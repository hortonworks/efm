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
import com.cloudera.cem.efm.model.component.FDExtensionComponent;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.extension.ConfigurableComponentDefinition;
import com.cloudera.cem.efm.model.extension.ExtensionComponent;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.types.FDComponentType;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.service.extension.FDExtensionManager;
import com.cloudera.cem.efm.service.extension.FDExtensionManagers;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.validation.ComponentValidator;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.StandardControllerServiceLookup;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Base class for services that operate on components that are extension types (i.e. processors and controller services).
 *
 * @param <VC> the type of VersionedComponent the service operates on
 * @param <EC> the type of ExtensionComponent the service operates on
 * @param <FDC> the type of FDExtensionComponent the service operates on
 */
public abstract class BaseExtensionComponentService<VC extends VersionedComponent, EC extends ExtensionComponent, FDC extends FDExtensionComponent<VC, EC>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExtensionComponentService.class);

    protected FDFlowManager flowManager;
    protected FDExtensionManagers extensionManagers;

    private final ValidationService validationService;
    private final ControllerServiceLookup serviceLookup;

    /**
     * @param flowManager the flow manager
     * @param extensionManagers the extension managers
     */
    public BaseExtensionComponentService(final FDFlowManager flowManager, final FDExtensionManagers extensionManagers, final ValidationService validationService) {
        this.flowManager = flowManager;
        this.extensionManagers = extensionManagers;
        this.validationService = validationService;
        this.serviceLookup = new StandardControllerServiceLookup(flowManager, extensionManagers, validationService);
    }

    protected ValidationService getValidationService() {
        return validationService;
    }

    /**
     * Sub-classes implement the ability to retrieve the component definition for the given agent class and component config.
     *
     * @param agentClass the agent class
     * @param componentConfig the component configuration
     * @return the component definition as an ExtensionComponent
     */
    public abstract EC getComponentDefinition(String agentClass, VC componentConfig);

    /**
     * Sub-classes implement ability to instantiate an FDExtensionComponent given the config and definition.
     *
     * @param componentConfig the component config
     * @param componentDefinition the component definition
     * @return the FDExtensionComponent
     */
    protected abstract FDC instantiateExtensionComponent(VC componentConfig, EC componentDefinition);

    /**
     * Gets the available extension types for the given flow.
     *
     * @param flowId the flow id
     * @param getExtensionTypes a function to call on an FDExtensionManager to return the types of components
     * @return the set of available types
     */
    protected FDComponentTypes getExtensionTypes(final String flowId,
                                                 final Function<FDExtensionManager,Set<FDComponentType>> getExtensionTypes) {
        final Optional<FDFlowMetadata> flowMetadata = flowManager.getFlowMetadata(flowId);
        if (!flowMetadata.isPresent()) {
            throw new ResourceNotFoundException("No flow exists for the given flow id");
        }

        final String agentClass = flowMetadata.get().getAgentClass();
        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(agentClass);

        final Set<FDComponentType> types = getExtensionTypes.apply(extensionManager);

        final FDComponentTypes componentTypes = new FDComponentTypes();
        componentTypes.setFlowId(flowId);
        componentTypes.setComponentTypes(types);
        return componentTypes;
    }

    /**
     * Creates a component that require an extension manager.
     *
     * @param flow the flow where the component is being created
     * @param processGroupId the process group where the component is being created
     * @param requestComponentType the type of the component
     * @param requestComponentConfig the requested component config
     * @param createComponent a function that given an extension manager will create a component
     * @param getComponentsFunction a function that given a process group will return the destination set for the new component
     *
     * @return the created component
     */
    protected FDC createExtensionComponent(
        final FDFlow flow,
        final String processGroupId,
        final String requestComponentType,
        final VC requestComponentConfig,
        final Function<FDExtensionManager, FDC> createComponent,
        final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
        final ComponentValidator<FDC> validator) {


        final VersionedProcessGroup flowContent = flow.getFlowContent();

        final VersionedProcessGroup destinationProcessGroup = ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContent);

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final String agentClass = flowMetadata.getAgentClass();

        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(agentClass);
        final FDC createdComponent = createComponent.apply(extensionManager);

        final VC createdComponentConfig = createdComponent.getComponentConfiguration();
        createdComponentConfig.setIdentifier(requestComponentConfig.getIdentifier());
        createdComponentConfig.setGroupIdentifier(processGroupId);

        final String componentName = ComponentUtils.getComponentName(requestComponentConfig, requestComponentType);
        createdComponentConfig.setName(componentName);

        // Add the component to the requested process group
        final Set<VC> components = getComponentsFunction.apply(destinationProcessGroup);
        components.add(createdComponentConfig);

        // Overlay any incoming config on top of the created component
        configureComponent(requestComponentConfig, createdComponentConfig);

        // Fetch validation errors from cache or perform validation & cache the resets
        createdComponent.setValidationErrors(getValidationErrors(createdComponent, validator, flowContent, flowMetadata.getIdentifier()));

        // Publish an event that the component was added
        ComponentUtils.publishComponentAddedEvent(flowManager, flowMetadata.getIdentifier(), requestComponentConfig.getIdentifier(), flowContent);

        return createdComponent;
    }

    /**
     * Retrieves the FDExtensionComponent with the given id in the given flow.
     *
     * @param flow the flow containing the component
     * @param componentId the component id
     * @param getComponentsFunction the function to apply to a process group to locate instances of the type of component
     * @return the FDExtensionComponent instance
     */
    protected FDC getExtensionComponent(final FDFlow flow, final String componentId,
                                        final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                                        final ComponentValidator<FDC> validator) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        final VC componentConfig = ComponentUtils.getComponentOrNotFound(componentId, flowContent, getComponentsFunction);
        final EC componentDefinition = getComponentDefinition(flowMetadata.getAgentClass(), componentConfig);

        final FDC component = instantiateExtensionComponent(componentConfig, componentDefinition);
        component.setValidationErrors(getValidationErrors(component, validator, flowContent, flowMetadata.getIdentifier()));

        return component;
    }

    /**
     * Retrieves the FDExtensionComponent instances in the given processor group.
     *
     * @param flowId the flow id
     * @param processGroupId the process group id
     * @param getComponentsFunction the function to apply on the process group to search for components
     * @return the components in the given process group
     */
    protected Set<FDC> getExtensionComponentsForProcessGroup(final String flowId, final String processGroupId,
                                                             final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                                                             final ComponentValidator<FDC> validator) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDFlowMetadata flowMetadata = currentFlow.getFlowMetadata();
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();
        final String agentClass = flowMetadata.getAgentClass();

        final VersionedProcessGroup processGroup = ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContent);

        final Set<FDC> services = new LinkedHashSet<>();
        for (final VC componentConfig : getComponentsFunction.apply(processGroup)) {
            final EC componentDefinition = getComponentDefinition(agentClass, componentConfig);
            final FDC component = instantiateExtensionComponent(componentConfig, componentDefinition);
            component.setValidationErrors(getValidationErrors(component, validator, flowContent, flowMetadata.getIdentifier()));
            services.add(component);
        }

        return services;
    }

    /**
     * Updates the given extension component in the given flow.
     *
     * @param flow the flow containing the component
     * @param requestComponentConfig the component configuration with the updates to apply
     * @param getComponentsFunction the function to apply to a process group to locate instances of the type of component
     * @return the updated FDExtensionComponent instance
     */
    protected FDC updateExtensionComponent(final FDFlow flow, final VC requestComponentConfig,
                                           final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction,
                                           final ComponentValidator<FDC> validator) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        final VC resultServiceConfig = ComponentUtils.getComponentOrNotFound(
                requestComponentConfig.getIdentifier(), flowContent, getComponentsFunction);

        validationService.clearValidationErrors(requestComponentConfig.getIdentifier());

        // Overlay any incoming config on top of the created component
        configureComponent(requestComponentConfig, resultServiceConfig);

        final String agentClass = flowMetadata.getAgentClass();
        final EC componentDefinition = getComponentDefinition(agentClass, resultServiceConfig);

        // Publish a flow event to capture the change
        ComponentUtils.publishComponentModifiedEvent(flowManager, flowMetadata.getIdentifier(), requestComponentConfig.getIdentifier(), flowContent);

        final FDC instantiated = instantiateExtensionComponent(resultServiceConfig, componentDefinition);
        instantiated.setValidationErrors(getValidationErrors(instantiated, validator, flowContent, flowMetadata.getIdentifier()));

        return instantiated;
    }

    /**
     * Removes the component with the given id from the given flow.
     *
     * @param flowId the flow id
     * @param componentId the component id
     * @param getComponentsFunction the function that given a process group returns the set of components we are looking for
     */
    protected VC deleteExtensionComponent(final String flowId, final String componentId,
                                            final Function<VersionedProcessGroup, Set<VC>> getComponentsFunction) {

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final FDFlowMetadata flowMetadata = currentFlow.getFlowMetadata();
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();

        final VC removedComponent = ComponentUtils.removeComponent(componentId, flowContent, getComponentsFunction);

        // Publish a flow event to capture the change
        validationService.clearValidationErrors(componentId);
        ComponentUtils.publishComponentRemovedEvent(flowManager, flowMetadata.getIdentifier(), componentId, flowContent);
        return removedComponent;
    }

    /**
     * Gets the property descriptor with the specified property name for the given type of component.
     *
     * @param flow the flow where the component is
     * @param componentGroupId the process group where the component is
     * @param componentType the type of component
     * @param componentBundle the bundle of the component
     * @param propertyName the property name
     * @return the property descriptor
     */
    protected FDPropertyDescriptor getExtensionComponentPropertyDescriptor(
            final FDFlow flow,
            final String componentGroupId,
            final String componentType,
            final Bundle componentBundle,
            final String propertyName) {

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContent = flow.getFlowContent();

        // Get the extension manager for the given agent class...
        final FDExtensionManager extensionManager = getExtensionManagerOrIllegalState(flowMetadata.getAgentClass());

        PropertyDescriptor resultDescriptor;

        // See if there is a defined property with the given name, otherwise if the processor supports dynamic properties then create one
        final Optional<PropertyDescriptor> definedPropertyDescriptor = getPropertyDescriptorFromExtensionManager(
                extensionManager, componentType, componentBundle, propertyName);

        if (definedPropertyDescriptor.isPresent()) {
            resultDescriptor = definedPropertyDescriptor.get();
        } else {
            try {
                resultDescriptor = createDynamicPropertyDescriptorFromExtensionManager(
                        extensionManager, componentType, componentBundle, propertyName);
            } catch (IllegalStateException e) {
                resultDescriptor = null;
            }
        }

        if (resultDescriptor == null) {
            throw new ResourceNotFoundException("Property Descriptor does not exist with the specified name, " +
                    "and the component does not support dynamic properties");
        }

        // If the descriptor is for a controller service then populate the allowable values...
        if (resultDescriptor.getTypeProvidedByValue() != null) {
            final Set<VersionedControllerService> ancestorControllerServices = ComponentUtils.getAncestorControllerServices(
                    componentGroupId, flowContent);

            ComponentUtils.populateControllerServicePropertyDescriptors(
                    Collections.singleton(resultDescriptor),
                    ancestorControllerServices);
        }

        final FDPropertyDescriptor propertyDescriptor = new FDPropertyDescriptor();
        propertyDescriptor.setPropertyDescriptor(resultDescriptor);
        return propertyDescriptor;
    }

    /**
     * Sub-classes specify how to get the property descriptor from the given extension manager.
     *
     * @param extensionManager the extension manager
     * @param type the type of the component
     * @param bundle the bundle
     * @param propertyName the property name
     * @return an optional containing the located property descriptor, or empty if it doesn't exist
     */
    protected abstract Optional<PropertyDescriptor> getPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName);

    /**
     * Sub-classes specify how to create the dynamic property descriptor from the given extension manager.
     *
     * @param extensionManager the extension manager
     * @param type the type of the component
     * @param bundle the bundle
     * @param propertyName the property name
     * @return the created property descriptor
     */
    protected abstract PropertyDescriptor createDynamicPropertyDescriptorFromExtensionManager(
            final FDExtensionManager extensionManager, final String type,
            final Bundle bundle, final String propertyName);


    /**
     * Creates property descriptors in the component definition for any properties in the properties map that don't
     * already have an existing descriptor.
     *
     * @param componentType the component type
     * @param componentBundle the component bundle
     * @param componentDefinition the component definition
     * @param properties the current properties of the component
     * @param extensionManager the extension manager
     */
    protected void populateDynamicProperties(
            final String componentType,
            final Bundle componentBundle,
            final ConfigurableComponentDefinition componentDefinition,
            final Map<String,String> properties,
            final FDExtensionManager extensionManager) {

        // Since the map inside the processor definition is unmodifiable, make a copy of it so we can add more descriptors
        final LinkedHashMap<String,PropertyDescriptor> updatedDescriptors = componentDefinition.getPropertyDescriptors() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(componentDefinition.getPropertyDescriptors());

        // We need to sort the properties Map so that any dynamic properties get added to the updatedDescriptors in a consistent order
        final SortedMap<String,String> sortedProperties = new TreeMap<>(properties);

        // For each property name/value from the config, see if there is already a property descriptor for it
        for (final Map.Entry<String,String> entry : sortedProperties.entrySet()) {
            final String propertyName = entry.getKey();
            if (!updatedDescriptors.containsKey(propertyName)) {
                if (!componentDefinition.getSupportsDynamicProperties()) {
                    LOGGER.warn("Property descriptor does not exist for {} and component does not support " +
                            "dynamic properties, descriptor will not be created", new Object[]{propertyName});
                } else {
                    final PropertyDescriptor dynamicDescriptor = createDynamicPropertyDescriptorFromExtensionManager(
                            extensionManager, componentType, componentBundle, propertyName);
                    LOGGER.debug("Creating dynamic property descriptor for {}", new Object[]{propertyName});
                    updatedDescriptors.put(dynamicDescriptor.getName(), dynamicDescriptor);
                }
            }
        }

        // Set the updated map back into the definition
        componentDefinition.setPropertyDescriptors(updatedDescriptors);
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

    /**
     * Gets the extension manager for the given agent class, or throws IllegalStateException if it does not exist.
     *
     * @param agentClass the agent class
     * @return the extension manager
     */
    protected FDExtensionManager getExtensionManagerOrIllegalState(final String agentClass) {
        final Optional<FDExtensionManager> extensionManager = extensionManagers.getExtensionManager(agentClass);
        if (!extensionManager.isPresent()) {
            throw new IllegalStateException("Unable to retrieve component definitions because no agent manifest was found for the given agent class.");
        }
        return extensionManager.get();
    }

    private Collection<String> getValidationErrors(final FDC component, final ComponentValidator<FDC> validator,
                                                   final VersionedProcessGroup group, final String flowId) {
        final VersionedComponent configuration = component.getComponentConfiguration();
        final String id = configuration.getIdentifier();

        return validationService.getValidationErrors(id, () -> validator.getValidationErrors(component, group, flowId, serviceLookup));
    }

}
