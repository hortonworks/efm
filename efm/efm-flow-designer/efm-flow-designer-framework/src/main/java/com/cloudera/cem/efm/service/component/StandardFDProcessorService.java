/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.extension.Relationship;
import com.cloudera.cem.efm.model.extension.SchedulingStrategy;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.service.extension.FDExtensionManager;
import com.cloudera.cem.efm.service.extension.FDExtensionManagers;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.ProcessorValidationContext;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Standard implementation of FDProcessorService.
 */
@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDProcessorService extends BaseExtensionComponentService<VersionedProcessor, ProcessorDefinition, FDProcessor> implements FDProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardFDProcessorService.class);
    private ConcurrentMap<String, Collection<String>> validationErrorCache = new ConcurrentHashMap<>();

    @Autowired
    public StandardFDProcessorService(final FDFlowManager flowManager, final FDExtensionManagers extensionManagers, final ValidationService validationService) {
        super(flowManager, extensionManagers, validationService);
    }

    @Override
    public FDComponentTypes getProcessorTypes(final String flowId) {
        return getExtensionTypes(flowId, FDExtensionManager::getProcessorTypes);
    }

    @Override
    public FDPropertyDescriptor getPropertyDescriptor(final String flowId, final String processorId, final String propertyName) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);

        final VersionedProcessor componentConfig = ComponentUtils.getComponentOrNotFound(
                processorId, currentFlow.getFlowContent(), VersionedProcessGroup::getProcessors);

        final String type = componentConfig.getType();
        final Bundle bundle = componentConfig.getBundle();
        final String componentGroupId = componentConfig.getGroupIdentifier();

        return getExtensionComponentPropertyDescriptor(currentFlow, componentGroupId, type, bundle, propertyName);
    }

    @Override
    protected Optional<PropertyDescriptor> getPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName) {
        return extensionManager.getProcessorPropertyDescriptor(type, bundle, propertyName);
    }

    @Override
    protected PropertyDescriptor createDynamicPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName) {
        return extensionManager.createDynamicProcessorPropertyDescriptor(type, bundle, propertyName);
    }

    @Override
    public FDProcessor create(final String flowId, final String processGroupId, final VersionedProcessor requestComponentConfig) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        validateRequestValues(requestComponentConfig);

        final String requestType = requestComponentConfig.getType();
        final Bundle requestBundle = requestComponentConfig.getBundle();

        final FDProcessor createdComponent = createExtensionComponent(
                currentFlow,
                processGroupId,
                requestType,
                requestComponentConfig,
                (em) -> em.createProcessor(requestType, requestBundle),
                VersionedProcessGroup::getProcessors,
                this::validate
        );

        ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, processGroupId, createdComponent.getComponentDefinition());
        return createdComponent;
    }

    private Collection<String> validate(final FDProcessor processor, final VersionedProcessGroup group, final String flowId, final ControllerServiceLookup serviceLookup) {
        final ProcessorValidationContext validationContext = new ProcessorValidationContext(processor.getComponentDefinition());
        return validationContext.validate(processor.getComponentConfiguration(), group, flowId, serviceLookup);
    }

    @Override
    public FDProcessor get(final String flowId, final String processorId) {
        final FDFlow flow = getFlowOrNotFound(flowId);
        final FDProcessor processor = getExtensionComponent(flow, processorId, VersionedProcessGroup::getProcessors, this::validate);
        final String groupId = processor.getComponentConfiguration().getGroupIdentifier();
        ComponentUtils.populateControllerServicePropertyDescriptors(flow, groupId, processor.getComponentDefinition());
        return processor;
    }

    @Override
    public FDProcessor update(final String flowId, final VersionedProcessor requestComponentConfig) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        validateRequestValues(requestComponentConfig);

        final FDProcessor processor = updateExtensionComponent(currentFlow, requestComponentConfig, VersionedProcessGroup::getProcessors, this::validate);
        final String groupId = processor.getComponentConfiguration().getGroupIdentifier();
        ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, groupId, processor.getComponentDefinition());
        return processor;
    }

    @Override
    public void delete(final String flowId, final String processorId) {
        deleteExtensionComponent(flowId, processorId, VersionedProcessGroup::getProcessors);
    }

    @Override
    public ProcessorDefinition getComponentDefinition(final String agentClass, final VersionedProcessor componentConfig) {
        final String type = componentConfig.getType();
        final Bundle bundle = componentConfig.getBundle();
        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(agentClass);

        final ProcessorDefinition processorDefinition = extensionManager.getProcessorDefinition(type, bundle);

        // if the config section has no properties we can short-circuit and return the definition as is
        final Map<String,String> componentProperties = componentConfig.getProperties();
        if (componentProperties == null || componentProperties.isEmpty()) {
            return processorDefinition;
        }

        // update the property values based on the definition, see comments on method for description
        // this must be done before calling populateDynamicProperties so that the value map is cleaned up first
        ComponentUtils.updatePropertyValues(processorDefinition, componentConfig.getProperties());

        // create property descriptors for any dynamic properties
        populateDynamicProperties(type, bundle, processorDefinition, componentProperties, extensionManager);

        // create relationships based on names of dynamic properties
        if (processorDefinition.getSupportsDynamicRelationships()
                && processorDefinition.getSupportsDynamicProperties()) {
            populateDynamicRelationships(processorDefinition);
        }

        return processorDefinition;
    }

    private void populateDynamicRelationships(final ProcessorDefinition processorDefinition) {
        final List<Relationship> relationships = new ArrayList<>(processorDefinition.getSupportedRelationships());
        final Map<String, PropertyDescriptor> propertyDescriptors = processorDefinition.getPropertyDescriptors();

        for (final PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
            if (propertyDescriptor.isDynamic()) {
                final Relationship dynamicRelationship = new Relationship();
                dynamicRelationship.setName(propertyDescriptor.getName());
                dynamicRelationship.setDescription("Dynamic relationship.");
                relationships.add(dynamicRelationship);
            }
        }

        processorDefinition.setSupportedRelationships(relationships);
    }


    @Override
    protected FDProcessor instantiateExtensionComponent(final VersionedProcessor componentConfig,
                                                        final ProcessorDefinition componentDefinition) {
        final FDProcessor component = new FDProcessor();
        component.setComponentConfiguration(componentConfig);
        component.setComponentDefinition(componentDefinition);
        return component;
    }

    @Override
    protected void configureComponentSpecifics(VersionedProcessor requestProcessor, VersionedProcessor resultProcessor) {
        // Update VersionedProcessor fields...
        final Map<String,String> style = requestProcessor.getStyle();
        if (ComponentUtils.isNotNull(style)) {
            resultProcessor.setStyle(style);
        }

        ComponentUtils.configureConfigurableComponent(requestProcessor, resultProcessor);

        final String annotationData = requestProcessor.getAnnotationData();
        if (ComponentUtils.isNotNull(annotationData)) {
            resultProcessor.setAnnotationData(annotationData);
        }

        final String schedulingPeriod = requestProcessor.getSchedulingPeriod();
        if (ComponentUtils.isNotNull(schedulingPeriod)) {
            resultProcessor.setSchedulingPeriod(schedulingPeriod);
        }

        final String schedulingStrategy = requestProcessor.getSchedulingStrategy();
        if (ComponentUtils.isNotNull(schedulingStrategy)) {
            resultProcessor.setSchedulingStrategy(schedulingStrategy);
        }

        final String executionNode = requestProcessor.getExecutionNode();
        if (ComponentUtils.isNotNull(executionNode)) {
            resultProcessor.setExecutionNode(executionNode);
        }

        final String penaltyDuration = requestProcessor.getPenaltyDuration();
        if (ComponentUtils.isNotNull(penaltyDuration)) {
            resultProcessor.setPenaltyDuration(penaltyDuration);
        }

        final String yieldDuration = requestProcessor.getYieldDuration();
        if (ComponentUtils.isNotNull(yieldDuration)) {
            resultProcessor.setYieldDuration(yieldDuration);
        }

        final String bulletinLevel = requestProcessor.getBulletinLevel();
        if (ComponentUtils.isNotNull(bulletinLevel)) {
            resultProcessor.setBulletinLevel(bulletinLevel);
        }

        final Long runDurationMillis = requestProcessor.getRunDurationMillis();
        if (ComponentUtils.isNotNull(runDurationMillis)) {
            resultProcessor.setRunDurationMillis(runDurationMillis);
        }

        final Integer concurrentTaskCount = requestProcessor.getConcurrentlySchedulableTaskCount();
        if (ComponentUtils.isNotNull(concurrentTaskCount)) {
            resultProcessor.setConcurrentlySchedulableTaskCount(concurrentTaskCount);
        }

        final Set<String> autoTerminatedRelationships = requestProcessor.getAutoTerminatedRelationships();
        if (ComponentUtils.isNotNull(autoTerminatedRelationships)) {
            resultProcessor.setAutoTerminatedRelationships(autoTerminatedRelationships);
        }
    }

    /**
     * Called before a create or update to validate any values that must be in a specific format before applying the values.
     *
     * @param requestComponent the configuration of the component from the incoming request
     */
    private void validateRequestValues(final VersionedProcessor requestComponent) {
        final String schedulingStrategy = requestComponent.getSchedulingStrategy();
        if (ComponentUtils.isNotNull(schedulingStrategy)) {
            try {
                SchedulingStrategy.valueOf(schedulingStrategy);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unknown scheduling strategy: " + schedulingStrategy);
            }
        }

        final String schedulingPeriod = requestComponent.getSchedulingPeriod();
        if (ComponentUtils.isNotNull(schedulingPeriod)) {
            // TODO if we support CRON scheduling we will have to conditionally validate this
            ComponentUtils.validateTimePeriodValue(schedulingPeriod);
        }

        final String penaltyDuration = requestComponent.getPenaltyDuration();
        if (ComponentUtils.isNotNull(penaltyDuration)) {
            ComponentUtils.validateTimePeriodValue(penaltyDuration);
        }

        final String yieldDuration = requestComponent.getYieldDuration();
        if (ComponentUtils.isNotNull(yieldDuration)) {
            ComponentUtils.validateTimePeriodValue(yieldDuration);
        }

        final Integer concurrentTasks = requestComponent.getConcurrentlySchedulableTaskCount();
        if (ComponentUtils.isNotNull(concurrentTasks) && concurrentTasks < 1) {
            throw new IllegalArgumentException("Concurrent Tasks must be greater than or equal to 1");
        }
    }

}
