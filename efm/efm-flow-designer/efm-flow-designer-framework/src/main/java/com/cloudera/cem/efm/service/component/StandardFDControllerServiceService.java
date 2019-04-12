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

import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.service.extension.FDExtensionManager;
import com.cloudera.cem.efm.service.extension.FDExtensionManagers;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.ControllerServiceValidationContext;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Standard implementation of FDControllerService.
 */
@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDControllerServiceService extends BaseExtensionComponentService<VersionedControllerService, ControllerServiceDefinition, FDControllerService>
        implements FDControllerServiceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardFDControllerServiceService.class);

    @Autowired
    public StandardFDControllerServiceService(final FDFlowManager flowManager, final FDExtensionManagers extensionManagers, final ValidationService validationService) {
        super(flowManager, extensionManagers, validationService);
    }

    @Override
    public FDComponentTypes getControllerServiceTypes(final String flowId) {
        return getExtensionTypes(flowId, FDExtensionManager::getControllerServiceTypes);
    }

    @Override
    public FDControllerService create(final String flowId, final String processGroupId, final VersionedControllerService requestComponentConfig) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);

        final String requestType = requestComponentConfig.getType();
        final Bundle requestBundle = requestComponentConfig.getBundle();

        final FDControllerService createdComponent = createExtensionComponent(
                currentFlow,
                processGroupId,
                requestType,
                requestComponentConfig,
                (em) -> em.createControllerService(requestType, requestBundle),
                VersionedProcessGroup::getControllerServices,
                this::validate
        );

        ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, processGroupId, createdComponent.getComponentDefinition());
        return createdComponent;
    }

    @Override
    public FDControllerService get(final String flowId, final String controllerServiceId) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDControllerService controllerService = getExtensionComponent(currentFlow, controllerServiceId, VersionedProcessGroup::getControllerServices, this::validate);
        final String groupId = controllerService.getComponentConfiguration().getGroupIdentifier();
        ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, groupId, controllerService.getComponentDefinition());
        return controllerService;
    }

    @Override
    public Set<FDControllerService> getForProcessGroup(final String flowId, final String processGroupId) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);

        final Set<FDControllerService> controllerServices = getExtensionComponentsForProcessGroup(
                flowId, processGroupId, VersionedProcessGroup::getControllerServices, this::validate);

        for (final FDControllerService controllerService : controllerServices) {
            ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, processGroupId, controllerService.getComponentDefinition());
        }

        return controllerServices;
    }

    @Override
    public FDPropertyDescriptor getPropertyDescriptor(final String flowId, final String controllerServiceId, final String propertyName) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);

        final VersionedControllerService componentConfig = ComponentUtils.getComponentOrNotFound(
                controllerServiceId, currentFlow.getFlowContent(), VersionedProcessGroup::getControllerServices);

        final String type = componentConfig.getType();
        final Bundle bundle = componentConfig.getBundle();
        final String componentGroupId = componentConfig.getGroupIdentifier();

        return getExtensionComponentPropertyDescriptor(currentFlow, componentGroupId, type, bundle, propertyName);
    }

    @Override
    protected Optional<PropertyDescriptor> getPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName) {
        return extensionManager.getControllerServicePropertyDescriptor(type, bundle, propertyName);
    }

    @Override
    protected PropertyDescriptor createDynamicPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName) {
        return extensionManager.createDynamicControllerServicePropertyDescriptor(type, bundle, propertyName);
    }

    @Override
    public FDControllerService update(final String flowId, final VersionedControllerService requestComponentConfig) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);

        final FDControllerService beforeUpdate = getExtensionComponent(currentFlow, requestComponentConfig.getIdentifier(), VersionedProcessGroup::getControllerServices, this::validate);
        final Collection<String> validationErrorsBeforeUpdate = beforeUpdate.getValidationErrors();
        final boolean serviceValidBefore = validationErrorsBeforeUpdate == null || validationErrorsBeforeUpdate.isEmpty();

        final FDControllerService updatedService = updateExtensionComponent(currentFlow, requestComponentConfig, VersionedProcessGroup::getControllerServices, this::validate);
        final Collection<String> validationErrors = updatedService.getValidationErrors();
        final boolean updatedServiceValid = validationErrors == null || validationErrors.isEmpty();

        // If the service's validity has changed, we need to re-validate any component that currently references this service, as that component may now
        // be valid or invalid as a result of referencing this service.
        if (serviceValidBefore != updatedServiceValid) {
            revalidateReferencingComponents(updatedService.getComponentConfiguration(), flowId);
        }

        final String groupId = updatedService.getComponentConfiguration().getGroupIdentifier();
        ComponentUtils.populateControllerServicePropertyDescriptors(currentFlow, groupId, updatedService.getComponentDefinition());
        return updatedService;
    }

    private void revalidateReferencingComponents(final VersionedControllerService serviceConfig, final String flowId) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();
        final VersionedProcessGroup group = ComponentUtils.getProcessGroupOrNotFound(serviceConfig.getGroupIdentifier(), flowContent);

        final String agentClass = currentFlow.getFlowMetadata().getAgentClass();
        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(agentClass);

        final ValidationService validationService = getValidationService();

        findReferencingComponents(serviceConfig.getIdentifier(), group, extensionManager).stream()
            .map(VersionedComponent::getIdentifier)
            .forEach(validationService::clearValidationErrors);
    }

    private Set<VersionedComponent> findReferencingComponents(final String controllerServiceId, final VersionedProcessGroup group, final FDExtensionManager extensionManager) {
        final Set<VersionedComponent> referencingComponents = new HashSet<>();

        for (final VersionedProcessor processor : group.getProcessors()) {
            ProcessorDefinition processorDefinition = null;

            final Map<String, String> properties = processor.getProperties();
            if (properties == null) {
                continue;
            }

            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                if (controllerServiceId.equals(entry.getValue())) {
                    if (processorDefinition == null) {
                        processorDefinition = extensionManager.getProcessorDefinition(processor.getType(), processor.getBundle());
                    }

                    final PropertyDescriptor descriptor = processorDefinition.getPropertyDescriptors().get(entry.getKey());
                    if (descriptor != null && descriptor.getTypeProvidedByValue() != null) {
                        // Property references a Controller Service, and it's configured with the ID of this service.
                        referencingComponents.add(processor);
                        break;
                    }
                }
            }
        }

        return referencingComponents;
    }

    @Override
    public void delete(final String flowId, final String controllerServiceId) {
        final VersionedControllerService removed = deleteExtensionComponent(flowId, controllerServiceId, VersionedProcessGroup::getControllerServices);
        revalidateReferencingComponents(removed, flowId);
    }

    @Override
    public ControllerServiceDefinition getComponentDefinition(final String agentClass, final VersionedControllerService componentConfig) {
        final String type = componentConfig.getType();
        final Bundle bundle = componentConfig.getBundle();
        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(agentClass);

        final ControllerServiceDefinition controllerServiceDefinition = extensionManager.getControllerServiceDefinition(type, bundle);

        // if the config section has no properties we can short-circuit and return the definition as is
        final Map<String,String> componentProperties = componentConfig.getProperties();
        if (componentProperties == null || componentProperties.isEmpty()) {
            return controllerServiceDefinition;
        }

        // update the property values based on the definition, see comments on method for description
        // this must be done before calling populateDynamicProperties so that the value map is cleaned up first
        ComponentUtils.updatePropertyValues(controllerServiceDefinition, componentConfig.getProperties());

        // create property descriptors for any dynamic properties
        populateDynamicProperties(type, bundle, controllerServiceDefinition, componentProperties, extensionManager);

        return controllerServiceDefinition;
    }

    @Override
    protected FDControllerService instantiateExtensionComponent(final VersionedControllerService componentConfig,
                                                                final ControllerServiceDefinition componentDefinition) {
        final FDControllerService extensionComponent = new FDControllerService();
        extensionComponent.setComponentConfiguration(componentConfig);
        extensionComponent.setComponentDefinition(componentDefinition);
        return extensionComponent;
    }

    @Override
    protected void configureComponentSpecifics(final VersionedControllerService requestService, final VersionedControllerService resultService) {
        // Configure Controller Service specific fields...
        final String annotationData = requestService.getAnnotationData();
        if (ComponentUtils.isNotNull(annotationData)) {
            resultService.setAnnotationData(requestService.getAnnotationData());
        }

        ComponentUtils.configureConfigurableComponent(requestService, resultService);
    }

    private Collection<String> validate(final FDControllerService service, final VersionedProcessGroup group,
                                        final String flowId, final ControllerServiceLookup serviceLookup) {
        final ControllerServiceValidationContext validationContext = new ControllerServiceValidationContext(service.getComponentDefinition());
        return validationContext.validate(service.getComponentConfiguration(), group, flowId, serviceLookup);
    }

}
