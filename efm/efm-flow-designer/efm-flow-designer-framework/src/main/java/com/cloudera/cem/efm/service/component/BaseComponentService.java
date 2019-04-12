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

import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.model.component.FDComponent;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.service.validation.Validator;
import com.cloudera.cem.efm.validation.ComponentValidator;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for component services that are not extension types (i.e. funnels, connections, RPGs, etc.).
 */
public abstract class BaseComponentService<VC extends VersionedComponent, FDC extends FDComponent<VC>> {

    protected FDFlowManager flowManager;
    private final ValidationService validationService;

    @Autowired
    public BaseComponentService(final FDFlowManager flowManager, final ValidationService validationService) {
        this.flowManager = flowManager;
        this.validationService = validationService;
    }

    protected ValidationService getValidationService() {
        return validationService;
    }

    /**
     * Sub-classes implement the ability to instantiate the specific type of FDComponent given the config.
     *
     * @param componentConfig the component config
     * @return the FDComponent
     */
    protected abstract FDC instantiateComponent(VC componentConfig);

    /**
     * Creates a component in the given flow.
     *
     * @param flowId the flow where the component is being created
     * @param processGroupId the process group where the component is being created
     * @param requestComponentType the type of the component, or null
     * @param requestComponentConfig the requested component config
     * @param createComponent a supplier that creates the component instance
     * @param getComponentsFunction a function that given a process group will return the destination set for the new component
     * @param componentValidator an optional component validator, can be null to perform no validation
     *
     * @return the created component
     */
    protected FDC createComponent(
            final String flowId,
            final String processGroupId,
            final String requestComponentType,
            final VC requestComponentConfig,
            final Supplier<FDC> createComponent,
            final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
            final ComponentValidator<FDC> componentValidator) {

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();

        final VersionedProcessGroup destinationProcessGroup = ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContent);
        return createComponent(currentFlow, destinationProcessGroup, requestComponentType, requestComponentConfig, createComponent, getComponentsFunction, componentValidator);
    }

    /**
     * Creates a component in the given flow and process group.
     *
     * @param flow the flow where the component is being added
     * @param destinationProcessGroup the process group where component is being added
     * @param requestComponentType the type of component, or null
     * @param requestComponentConfig the requested component config
     * @param createComponent a supplier that creates the component instance
     * @param getComponentsFunction a function that given a process group will return the destination set for the new component
     * @param componentValidator an optional component validator, can be null to perform no validation
     *
     * @return the created component
     */
    protected FDC createComponent(
            final FDFlow flow,
            final VersionedProcessGroup destinationProcessGroup,
            final String requestComponentType,
            final VC requestComponentConfig,
            final Supplier<FDC> createComponent,
            final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
            final ComponentValidator<FDC> componentValidator) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        final FDC createdComponent = createComponent.get();

        final VC createdComponentConfig = createdComponent.getComponentConfiguration();
        createdComponentConfig.setIdentifier(requestComponentConfig.getIdentifier());
        createdComponentConfig.setGroupIdentifier(destinationProcessGroup.getIdentifier());

        // Add the component to the requested process group
        final Set<VC> components = getComponentsFunction.apply(destinationProcessGroup);
        components.add(createdComponentConfig);

        // Overlay any incoming config on top of the created component
        configureComponent(requestComponentConfig, createdComponentConfig);

        // Publish an event that the component was added
        ComponentUtils.publishComponentAddedEvent(flowManager, flowMetadata.getIdentifier(), requestComponentConfig.getIdentifier(), flowContent);

        if (componentValidator != null) {
            final Validator validator = () -> getValidationErrors(createdComponent, componentValidator, flowContent, flowMetadata.getIdentifier());
            createdComponent.setValidationErrors(validationService.getValidationErrors(createdComponentConfig.getIdentifier(), validator));
        }

        return createdComponent;
    }

    /**
     * Retrieves the FDComponent with the given id in the given flow.
     *
     * @param flowId the flow id
     * @param componentId the component id
     * @param getComponentsFunction the function to apply to a process group to locate instances of the type of component
     * @param componentValidator an optional component validator, can be null to perform no validation
     *
     * @return the FDComponent instance
     */
    protected FDC getComponent(final String flowId, final String componentId,
                               final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                               final ComponentValidator<FDC> componentValidator) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDFlowMetadata flowMetadata = currentFlow.getFlowMetadata();
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();

        final VC componentConfig = ComponentUtils.getComponentOrNotFound(componentId, flowContent, getComponentsFunction);

        final FDC component = instantiateComponent(componentConfig);
        if (componentValidator != null) {
            component.setValidationErrors(getValidationErrors(component, componentValidator, flowContent, flowMetadata.getIdentifier()));
        }
        return component;
    }


    /**
     * Retrieves the FDComponent instances in the given processor group.
     *
     * @param flowId the flow id
     * @param processGroupId the process group id
     * @param getComponentsFunction the function to apply on the process group to search for components
     * @param componentValidator an optional component validator, can be null to perform no validation
     *
     * @return the components in the given process group
     */
    protected Set<FDC> getComponentsForProcessGroup(final String flowId, final String processGroupId,
                                                    final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                                                    final ComponentValidator<FDC> componentValidator) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDFlowMetadata flowMetadata = currentFlow.getFlowMetadata();
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();
        final VersionedProcessGroup processGroup = ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContent);

        final Set<FDC> services = new LinkedHashSet<>();
        for (final VC componentConfig : getComponentsFunction.apply(processGroup)) {
            final FDC component = instantiateComponent(componentConfig);
            if (componentValidator != null) {
                component.setValidationErrors(getValidationErrors(component, componentValidator, flowContent, flowMetadata.getIdentifier()));
            }
            services.add(component);
        }

        return services;
    }

    /**
     * Updates the given extension component in the given flow.
     *
     * @param flowId the flow id
     * @param requestComponentConfig the component configuration with the updates to apply
     * @param getComponentsFunction the function to apply to a process group to locate instances of the type of component
     * @param componentValidator an optional component validator, can be null to perform no validation
     *
     * @return the updated FDExtensionComponent instance
     */
    protected FDC updateComponent(final String flowId, final VC requestComponentConfig,
                                  final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                                  final ComponentValidator<FDC> componentValidator) {

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();

        final VC resultComponentConfig = ComponentUtils.getComponentOrNotFound(
                requestComponentConfig.getIdentifier(), flowContent, getComponentsFunction);

        return updateComponent(currentFlow, requestComponentConfig, resultComponentConfig, componentValidator);
    }

    protected FDC updateComponent(final FDFlow flow, final VC requestComponentConfig, final VC resultComponentConfig,
                                  final ComponentValidator<FDC> componentValidator) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        // Overlay any incoming config on top of the created component
        configureComponent(requestComponentConfig, resultComponentConfig);

        // Publish a flow event to capture the change
        ComponentUtils.publishComponentModifiedEvent(flowManager, flowMetadata.getIdentifier(), requestComponentConfig.getIdentifier(), flowContent);

        final FDC component = instantiateComponent(resultComponentConfig);
        if (componentValidator != null) {
            component.setValidationErrors(getValidationErrors(component, componentValidator, flowContent, flowMetadata.getIdentifier()));
        }
        return component;
    }

    /**
     * Removes the component with the given id from the given flow.
     *
     * @param flowId the flow id
     * @param componentId the component id
     * @param getComponentsFunction the function that given a process group returns the set of components we are looking for
     */
    protected void deleteComponent(final String flowId, final String componentId,
                                   final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction) {

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDFlowMetadata flowMetadata = currentFlow.getFlowMetadata();
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();

        final VC removedComponent = ComponentUtils.removeComponent(componentId, flowContent, getComponentsFunction);
        ComponentUtils.publishComponentRemovedEvent(flowManager, flowMetadata.getIdentifier(), componentId, flowContent);
    }

    /**
     * Removes the given component from the given flow.
     *
     * @param flow the flow to remove the component from
     * @param componentConfig the component config specifying the component to remove
     * @param getComponentsFunction the function that given a process group returns the set of components we are looking for
     */
    protected void deleteComponent(final FDFlow flow, final VC componentConfig,
                                   final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        final VC removedComponent = ComponentUtils.removeComponent(componentConfig, flowContent, getComponentsFunction);
        ComponentUtils.publishComponentRemovedEvent(flowManager, flowMetadata.getIdentifier(), componentConfig.getIdentifier(), flowContent);
    }

    /**
     * Applies the configuration from the request component to the result component.
     *
     * @param requestComponent request component
     * @param resultComponent result component
     */
    protected void configureComponent(final VC requestComponent, final VC resultComponent) {
        // Update VersionedComponent fields...
        ComponentUtils.configureComponent(requestComponent, resultComponent);

        // Delegate to sub-classes for specifics
        configureComponentSpecifics(requestComponent, resultComponent);
    }

    /**
     * Sub-classes override to implement component specific configuration.
     *
     * @param requestComponent the request component
     * @param resultComponent the result component
     */
    protected abstract void configureComponentSpecifics(final VC requestComponent, final VC resultComponent);

    /**
     * Helper to retrieve the flow with the given id.
     *
     * @param flowId the flow id
     * @return the flow, will throw exception if not found
     */
    protected FDFlow getFlowOrNotFound(final String flowId) {
        final Optional<FDFlow> currentFlow = flowManager.getFlow(flowId);
        if (!currentFlow.isPresent()) {
            throw new ResourceNotFoundException("A flow does not exist with the given id");
        }

        return currentFlow.get();
    }

    private Collection<String> getValidationErrors(final FDC component, final ComponentValidator<FDC> componentValidator,
                                                   final VersionedProcessGroup group, final String flowId) {
        final VersionedComponent configuration = component.getComponentConfiguration();
        final String id = configuration.getIdentifier();

        return validationService.getValidationErrors(id, () -> componentValidator.getValidationErrors(component, group, flowId, null));
    }

}
