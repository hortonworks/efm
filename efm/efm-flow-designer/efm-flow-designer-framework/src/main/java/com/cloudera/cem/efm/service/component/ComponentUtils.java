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
import com.cloudera.cem.efm.model.extension.ConfigurableComponentDefinition;
import com.cloudera.cem.efm.model.extension.DefinedType;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.NiFiUserUtils;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processor.DataUnit;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.ConnectableComponentType;
import org.apache.nifi.registry.flow.ControllerServiceAPI;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConfigurableComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.util.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ComponentUtils {

    static final String MINIFI_CPP_TYPE_SEPARATOR = "::";

    /**
     * Helper to locate the process group when we want to throw a ResourceNotFoundException when the flow does not exist.
     *
     * @param processGroupId the process group id
     * @param startingGroup the group to start searching from
     * @return the process group, an exception will be thrown if it is not found
     */
    public static VersionedProcessGroup getProcessGroupOrNotFound(final String processGroupId, final VersionedProcessGroup startingGroup) {
        final Optional<VersionedProcessGroup> connectionGroup = ComponentUtils.locateProcessGroup(processGroupId, startingGroup);
        if (!connectionGroup.isPresent()) {
            throw new ResourceNotFoundException("Process group does not exist with the given id");
        }
        return connectionGroup.get();
    }

    /**
     * Helper to locate the process group when we want to throw a IllegalArgumentException when the flow does not exist.
     *
     * @param processGroupId the process group id
     * @param startingGroup the group to start searching from
     * @return the process group, an exception will be thrown if it is not found
     */
    public static VersionedProcessGroup getProcessGroupOrIllegalArgument(final String processGroupId, final VersionedProcessGroup startingGroup) {
        final Optional<VersionedProcessGroup> connectionGroup = ComponentUtils.locateProcessGroup(processGroupId, startingGroup);
        if (!connectionGroup.isPresent()) {
            throw new IllegalArgumentException("Process group does not exist with the given id");
        }
        return connectionGroup.get();
    }

    /**
     * Helper to locate a component when we want to throw a ResourceNotFoundException when the flow does not exist.
     *
     * @param componentId the component id
     * @param startingGroup the starting group
     * @param getComponentsFunction a function to apply on a process group to search for the type of component
     * @return the component
     */
    public static <V extends VersionedComponent> V getComponentOrNotFound(
            final String componentId,
            final VersionedProcessGroup startingGroup,
            final Function<VersionedProcessGroup, Set<V>> getComponentsFunction) {

        final Optional<V> component = ComponentUtils.locateComponent(componentId, startingGroup, getComponentsFunction);
        if (!component.isPresent()) {
            throw new ResourceNotFoundException("Component does not exist with the given id");
        }

        return component.get();
    }

    /**
     * Helper to locate a component when we want to throw a IllegalArgumentException when the flow does not exist.
     *
     * @param componentId the component id
     * @param startingGroup the starting group
     * @param getComponentsFunction a function to apply on a process group to search for the type of component
     * @return the component
     */
    public static <V extends VersionedComponent> V getComponentOrIllegalArgument(
            final String componentId,
            final VersionedProcessGroup startingGroup,
            final Function<VersionedProcessGroup, Set<V>> getComponentsFunction) {

        final Optional<V> component = ComponentUtils.locateComponent(componentId, startingGroup, getComponentsFunction);
        if (!component.isPresent()) {
            throw new ResourceNotFoundException("Component does not exist with the given id");
        }

        return component.get();
    }

    /**
     * Locates the given component by recursively searching the provided process group and it's children.
     *
     * @param componentId the component id to search for
     * @param startingGroup the process group to start searching from
     * @param getComponentsFunction a function that given a process group will return a set of components
     * @return the optional containing the component or null if one was not found
     */
    public static <V extends VersionedComponent> Optional<V> locateComponent(
            final String componentId,
            final VersionedProcessGroup startingGroup,
            final Function<VersionedProcessGroup, Set<V>> getComponentsFunction) {

        // Check components in current group, return if id matches
        final Set<V> components = getComponentsFunction.apply(startingGroup);
        if (components != null) {
            for (final V component : components) {
                if (component.getIdentifier().equals(componentId)) {
                    return Optional.of(component);
                }
            }
        }

        // None matched in current group, so recurse to children
        if (startingGroup.getProcessGroups() != null) {
            for (final VersionedProcessGroup childProcessGroup : startingGroup.getProcessGroups()) {
                final Optional<V> foundComponent = locateComponent(componentId, childProcessGroup, getComponentsFunction);
                if (foundComponent.isPresent()) {
                    return foundComponent;
                }
            }
        }

        // If made it here, then none matched
        return Optional.empty();
    }

    /**
     * Locates the process group with the given id starting with the provided process group and recursively searching.
     *
     * @param processGroupId the id of the process group to locate
     * @param startingGroup the starting group
     * @return the found group, or empty optional
     */
    public static Optional<VersionedProcessGroup> locateProcessGroup(final String processGroupId, final VersionedProcessGroup startingGroup) {
        if (startingGroup == null) {
            return Optional.empty();
        }

        if (startingGroup.getIdentifier().equals(processGroupId)) {
            return Optional.of(startingGroup);
        }

        if (startingGroup.getProcessGroups() != null) {
            for (VersionedProcessGroup childProcessGroup : startingGroup.getProcessGroups()) {
                final Optional<VersionedProcessGroup> foundGroup = locateProcessGroup(processGroupId, childProcessGroup);
                if (foundGroup.isPresent()) {
                    return foundGroup;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all ancestor controller services of the target group (including services in the target group) back to the parent group.
     *
     * @param targetGroupId the target group which should be a descendant of the parent group
     * @param parentGroup a parent group to start searching from
     * @return the ancestor controller services
     */
    public static Set<VersionedControllerService> getAncestorControllerServices(final String targetGroupId, final VersionedProcessGroup parentGroup) {
        final Set<VersionedControllerService> controllerServices = new LinkedHashSet<>();

        final boolean partOfDescendantChain = getAncestorControllerServices(targetGroupId, parentGroup, controllerServices);
        if (!partOfDescendantChain) {
            throw new IllegalStateException("The target process group is not a child of the starting group or any of its children");
        }

        return controllerServices;
    }

    /**
     * Helper method to recursively accumulate ancestor services.
     *
     * @param targetGroupId the target group which should be a descendant of the parent group
     * @param parentGroup a parent group to start searching from
     * @param controllerServices the set of accumulated services
     * @return true if any services were accumulated during the current call chain
     */
    private static boolean getAncestorControllerServices(final String targetGroupId, final VersionedProcessGroup parentGroup,
                                                         final Set<VersionedControllerService> controllerServices) {
        if (parentGroup == null) {
            return false;
        }

        // The group id of the root group is always null, so if asking for ancestors of the root group then
        // targetGroupId will be null and parentGroup will have a null group identifier
        final boolean targetIsRootGroup = (targetGroupId == null && parentGroup.getGroupIdentifier() == null);

        if (parentGroup.getIdentifier().equals(targetGroupId) || targetIsRootGroup) {
            controllerServices.addAll(parentGroup.getControllerServices());
            return true;
        }

        if (parentGroup.getProcessGroups() != null) {
            for (final VersionedProcessGroup childProcessGroup : parentGroup.getProcessGroups()) {
                final boolean partOfDescendantChain = getAncestorControllerServices(targetGroupId, childProcessGroup, controllerServices);
                if (partOfDescendantChain) {
                    controllerServices.addAll(parentGroup.getControllerServices());
                    return partOfDescendantChain;
                }
            }
        }

        return false;
    }

    /**
     * Gets the property descriptors that require controller service references.
     *
     * @param componentDefinition the component definition to get the property descriptors from
     * @return the property descriptors requiring controller services
     */
    public static Set<PropertyDescriptor> getControllerServicePropertyDescriptors(final ConfigurableComponentDefinition componentDefinition) {
        final Map<String,PropertyDescriptor> propertyDescriptors = componentDefinition.getPropertyDescriptors();
        if (propertyDescriptors == null) {
            return Collections.emptySet();
        }

        return propertyDescriptors.values().stream()
                .filter(p -> p.getTypeProvidedByValue() != null)
                .collect(Collectors.toSet());
    }

    /**
     * Populates the allowable values of any property descriptors in the given component that require a controller service.
     *
     * @param flow the flow containing the component
     * @param componentGroupId the id of the process group where the component is located
     * @param componentDefinition the component definition to populate
     */
    public static void populateControllerServicePropertyDescriptors(final FDFlow flow, final String componentGroupId,
                                                                    final ConfigurableComponentDefinition componentDefinition) {
        // Find all the property descriptors of the component that reference a controller service type
        final Set<PropertyDescriptor> controllerServiceProperties = ComponentUtils.getControllerServicePropertyDescriptors(componentDefinition);

        // If any properties reference controller services then we need populate their allowable values with the possible services
        if (!controllerServiceProperties.isEmpty()) {
            // Determine all the ancestor controller services between (and including) the group where the component is back to the root group
            final Set<VersionedControllerService> ancestorControllerServices = ComponentUtils.getAncestorControllerServices(
                    componentGroupId, flow.getFlowContent());

            populateControllerServicePropertyDescriptors(controllerServiceProperties, ancestorControllerServices);
        }
    }

    /**
     * Populates the allowable values of any property descriptors in the given component that require a controller service.
     *
     * @param componentDefinition the component definition to populate
     * @param ancestorControllerServices the ancestor controller services
     */
    public static void populateControllerServicePropertyDescriptors(final ConfigurableComponentDefinition componentDefinition,
                                                                    final Set<VersionedControllerService> ancestorControllerServices) {
        // Find all the property descriptors of the component that reference a controller service type
        final Set<PropertyDescriptor> controllerServiceProperties = ComponentUtils.getControllerServicePropertyDescriptors(componentDefinition);

        // If any properties reference controller services then we need populate their allowable values with the possible services
        if (!controllerServiceProperties.isEmpty()) {
            populateControllerServicePropertyDescriptors(controllerServiceProperties, ancestorControllerServices);
        }
    }


    /**
     * Populates the allowable values of the controller service properties based on the provided ancestor services.
     *
     * @param controllerServiceProperties the controller service property descriptors
     * @param ancestorControllerServices the ancestor services
     */
    public static void populateControllerServicePropertyDescriptors(final Set<PropertyDescriptor> controllerServiceProperties,
                                                                    final Set<VersionedControllerService> ancestorControllerServices) {

        // For each CS property, add an allowable value for any ancestor service that has the type required by the property
        for (final PropertyDescriptor propertyDescriptor : controllerServiceProperties) {
            final List<PropertyAllowableValue> allowableValues = new ArrayList<>();
            final DefinedType requiredType = propertyDescriptor.getTypeProvidedByValue();

            for (final VersionedControllerService controllerService : ancestorControllerServices) {

                // Check each ControllerServiceAPI provided by the CS and see if it matches the required type of the PropertyDescriptor
                final List<ControllerServiceAPI> serviceApis = controllerService.getControllerServiceApis();
                for (final ControllerServiceAPI serviceApi : serviceApis) {
                    final String serviceApiType = serviceApi.getType();
                    final String serviceApiGroup = serviceApi.getBundle().getGroup();
                    final String serviceApiArtifact = serviceApi.getBundle().getArtifact();
                    final String serviceApiVersion = serviceApi.getBundle().getVersion();

                    // NOTE: Currently the manifest doesn't specify a version in typeProvidedByValue so we have to conditionally
                    // use version when matching the info from the PropertyDescriptor against the provided ControllerServiceAPI
                    if (!StringUtils.isBlank(requiredType.getVersion())) {
                        if (serviceApiType.equals(requiredType.getType())
                                && serviceApiGroup.equals(requiredType.getGroup())
                                && serviceApiArtifact.equals(requiredType.getArtifact())
                                && serviceApiVersion.equals(requiredType.getVersion())) {
                            final PropertyAllowableValue allowableValue = new PropertyAllowableValue();
                            allowableValue.setValue(controllerService.getIdentifier());
                            allowableValue.setDisplayName(controllerService.getName());
                            allowableValues.add(allowableValue);
                        }
                    } else {
                        if (serviceApiType.equals(requiredType.getType())
                                && serviceApiGroup.equals(requiredType.getGroup())
                                && serviceApiArtifact.equals(requiredType.getArtifact())) {
                            final PropertyAllowableValue allowableValue = new PropertyAllowableValue();
                            allowableValue.setValue(controllerService.getIdentifier());
                            allowableValue.setDisplayName(controllerService.getName());
                            allowableValues.add(allowableValue);
                        }
                    }
                }
            }

            propertyDescriptor.setAllowableValues(allowableValues);
        }
    }

    /**
     * Update the map of property values based on the component definition and based on the value of the property
     * - if a property value is null and property is required, we need to reset it back to it's default
     * - if a property value is null and it is not required, we need to remove the entry for the value from the map
     *
     * @param componentDefinition the component definition
     * @param properties the property values of the component
     */
    public static void updatePropertyValues(final ConfigurableComponentDefinition componentDefinition,
                                            final Map<String,String> properties) {

        final Map<String,PropertyDescriptor> propertyDescriptors = componentDefinition.getPropertyDescriptors();
        if (propertyDescriptors == null) {
            return;
        }

        // first set any required props with null values back to their default
        for (final Map.Entry<String,PropertyDescriptor> entry : propertyDescriptors.entrySet()) {
            final String propName = entry.getKey();
            final PropertyDescriptor propertyDescriptor = entry.getValue();

            final String currValue = properties.get(propName);
            if (currValue == null) {
                if (propertyDescriptor.getRequired()) {
                    final String defaultValue = propertyDescriptor.getDefaultValue();
                    properties.put(propName, defaultValue);
                }
            }
        }

        // now remove entries for any dynamic properties that have null values, and any non-required properties with null values
        final Iterator<Map.Entry<String,String>> iter = properties.entrySet().iterator();
        while(iter.hasNext()) {
            final Map.Entry<String, String> entry = iter.next();

            final String propName = entry.getKey();
            final String propValue = entry.getValue();

            if (propValue == null) {
                final PropertyDescriptor propertyDescriptor = propertyDescriptors.get(propName);
                final boolean deletedDynamicProperty = (propertyDescriptor == null && componentDefinition.getSupportsDynamicProperties());
                if (deletedDynamicProperty) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Removes the component with the given id from the provided flow.
     *
     * @param componentId the id of the component to remove
     * @param flowContent the root process group of the flow
     * @param getComponentsFunction the function to apply on the process group to search for the component
     * @param <V> the type of VersionedComponent
     * @return the removed component
     */
    public static <V extends VersionedComponent> V removeComponent(
            final String componentId,
            final VersionedProcessGroup flowContent,
            final Function<VersionedProcessGroup, Set<V>> getComponentsFunction) {

        // Locate the component being removed
        final V componentConfig = getComponentOrNotFound(componentId, flowContent, getComponentsFunction);
        return removeComponent(componentConfig, flowContent, getComponentsFunction);
    }

    /**
     * Removes the given component from the provided flow.
     *
     * @param componentConfig the component config
     * @param flowContent the root process group of the flow
     * @param getComponentsFunction the function to apply on the process group to search for the component
     * @param <V> the type of VersionedComponent
     * @return the removed component
     */
    public static <V extends VersionedComponent> V removeComponent(
            final V componentConfig,
            final VersionedProcessGroup flowContent,
            final Function<VersionedProcessGroup, Set<V>> getComponentsFunction) {

        // Locate the process group containing the component
        final String containingProcessGroupId = componentConfig.getGroupIdentifier();
        final VersionedProcessGroup processGroup = getProcessGroupOrNotFound(containingProcessGroupId, flowContent);

        // Get the set of versioned components where the component being remove should be located
        final Set<V> versionedComponents = getComponentsFunction.apply(processGroup);

        // Remove the component from the set of components
        V removedComponent = null;
        final Iterator<V> iter = versionedComponents.iterator();
        while (iter.hasNext()) {
            final V versionedComponent = iter.next();
            if (versionedComponent.getIdentifier().equals(componentConfig.getIdentifier())) {
                iter.remove();
                removedComponent = versionedComponent;
                break;
            }
        }

        // This really shouldn't happen since it would indicate that we allowed the flow to get into an incorrect state
        // where the component specified a group id that existed, but the component wasn't added to that group
        if (removedComponent == null) {
            throw new IllegalStateException("The component to remove was not found in the process group that is expected to contain the component");
        }

        // Remove connections in the component's group where the source or dest is the component being removed
        removeComponentConnections(removedComponent.getIdentifier(), processGroup);

        // If the component's group has a parent group (i.e. its not the root group) then locate that parent group and
        // remove any connections involving the component being removed, this handles deleting a input/output port where
        // the connection lives in the group above where port lives

        final String componentGroupParentGroupId = processGroup.getGroupIdentifier();
        if (isNotNull(componentGroupParentGroupId)) {
            final VersionedProcessGroup componentGroupParentGroup = getProcessGroupOrIllegalArgument(componentGroupParentGroupId, flowContent);
            removeComponentConnections(removedComponent.getIdentifier(), componentGroupParentGroup);
        }

        return removedComponent;
    }

    /**
     * Remove any connections involving the given component within the given process group.
     *
     * @param componentId the id of a component
     * @param processGroup the process group to remove connections from
     */
    public static void removeComponentConnections(final String componentId, final VersionedProcessGroup processGroup) {
        removeComponentConnections(componentId, processGroup, false);
    }

    /**
     * Remove any connections involving the given component within the given process group, and will optionally search recusrively.
     *
     * @param componentId the id of a component
     * @param processGroup the process group to remove connections from
     * @param recursive whether or not to recurse into child groups
     */
    public static void removeComponentConnections(final String componentId, final VersionedProcessGroup processGroup, final boolean recursive) {
        if (processGroup == null || processGroup.getConnections() == null || processGroup.getConnections().isEmpty()) {
            return;
        }

        // Remove any connections in the current process group where the source or dest is the given component
        final Iterator<VersionedConnection> iter = processGroup.getConnections().iterator();
        while(iter.hasNext()) {
            final VersionedConnection connection = iter.next();
            if (connection.getSource().getId().equals(componentId) || connection.getDestination().getId().equals(componentId)) {
                iter.remove();
            }
        }

        // Recurse to any child groups
        if (recursive && processGroup.getProcessGroups() != null) {
            for (final VersionedProcessGroup childGroup : processGroup.getProcessGroups()) {
                removeComponentConnections(componentId, childGroup, recursive);
            }
        }
    }

    /**
     * Creates a ConnectableComponent from the given VersionedComponent, or throws an exception if the provided component
     * is not a connectable type.
     *
     * @param component the component
     * @return the connectable component
     */
    public static ConnectableComponent createConnectableComponent(final VersionedComponent component) {
        final ConnectableComponent connectableComponent = new ConnectableComponent();
        connectableComponent.setId(component.getIdentifier());
        connectableComponent.setGroupId(component.getGroupIdentifier());

        switch (component.getComponentType()) {
            case PROCESSOR:
                connectableComponent.setType(ConnectableComponentType.PROCESSOR);
                break;
            case INPUT_PORT:
                connectableComponent.setType(ConnectableComponentType.INPUT_PORT);
                break;
            case OUTPUT_PORT:
                connectableComponent.setType(ConnectableComponentType.OUTPUT_PORT);
                break;
            case REMOTE_INPUT_PORT:
                connectableComponent.setType(ConnectableComponentType.REMOTE_INPUT_PORT);
                break;
            case REMOTE_OUTPUT_PORT:
                connectableComponent.setType(ConnectableComponentType.REMOTE_OUTPUT_PORT);
                break;
            case FUNNEL:
                connectableComponent.setType(ConnectableComponentType.FUNNEL);
                break;
            default:
                throw new IllegalArgumentException("Component type is not a connectable type: " + component.getComponentType());
        }

        return connectableComponent;
    }

    /**
     * Determines the name for the given component.
     *
     * @param requestComponent the requested component
     * @param requestType the type of the requested component
     * @return the name to use
     */
    public static String getComponentName(final VersionedComponent requestComponent, final String requestType) {
        String componentName = requestComponent.getName();

        // Component provided a name so return that
        if (!StringUtils.isBlank(componentName)) {
            return componentName;
        }

        if (StringUtils.isBlank(requestType)) {
            return requestComponent.getComponentType().getTypeName();
        }

        // Determine if we have a CPP class name or Java class name
        if (requestType.contains(MINIFI_CPP_TYPE_SEPARATOR)) {
            final int lastSepIndex = requestType.lastIndexOf(MINIFI_CPP_TYPE_SEPARATOR);
            componentName = requestType.substring(lastSepIndex + MINIFI_CPP_TYPE_SEPARATOR.length());

            // If somehow the substring return null or empty, then just return requested type as the name
            if (StringUtils.isBlank(componentName)) {
                componentName = requestType;
            }

        } else {
            final int lastSepIndex = requestType.lastIndexOf(".");
            if (lastSepIndex < 0) {
                componentName = requestType;
            } else {
                componentName = requestType.substring(lastSepIndex + 1);
            }
        }

        return componentName;
    }

    /**
     * Applies the configuration from the request component to the result component.
     *
     * @param requestComponent request component
     * @param resultComponent result component
     */
    public static  <V extends VersionedComponent> void configureComponent(final V requestComponent, final V resultComponent) {
        // Update VersionedComponent fields...
        final String name = requestComponent.getName();
        if (isNotNull(name)) {
            resultComponent.setName(name);
        }

        final String comments = requestComponent.getComments();
        if (isNotNull(comments)) {
            resultComponent.setComments(comments);
        }

        final Position position = requestComponent.getPosition();
        if (isNotNull(position)) {
            resultComponent.setPosition(position);
        }

        // if the result component still has a null position then default to (0,0), should only happen on a create
        if (resultComponent.getPosition() == null) {
            resultComponent.setPosition(new Position(0,0));
        }

    }

    /**
     * Applies the configuration from the request component to the result component.
     *
     * @param requestComponent request component
     * @param resultComponent result component
     */
    public static <V extends VersionedConfigurableComponent> void configureConfigurableComponent(final V requestComponent, final V resultComponent) {
        final Map<String,String> requestPropertyValues = requestComponent.getProperties();
        if (ComponentUtils.isNotNull(requestPropertyValues)) {
            if (resultComponent.getProperties() == null) {
                resultComponent.setProperties(new HashMap<>());
            }
            final Map<String,String> resultPropertyValues = resultComponent.getProperties();
            requestPropertyValues.entrySet().stream().forEach(e -> resultPropertyValues.put(e.getKey(), e.getValue()));
        }
    }

    /**
     * Returns whether the specified object is not null.
     *
     * @param <T> type
     * @param object object
     * @return true if the specified object is not null
     */
    public static <T> boolean isNotNull(T object) {
        return object != null;
    }

    public static void validateTimePeriodValue(final String value) {
        FormatUtils.getTimeDuration(value, TimeUnit.MILLISECONDS);
    }

    public static String validateDataUnitValue(final String value) {
        final ParsedDataUnit parsedDataUnit = parseDataSize(value);
        return parsedDataUnit.getStringValue();
    }

    private static ParsedDataUnit parseDataSize(final String value) {
        if (value == null) {
            return null;
        }

        final Matcher matcher = DataUnit.DATA_SIZE_PATTERN.matcher(value.toUpperCase());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid data size: " + value);
        }

        final String sizeValue = matcher.group(1);
        final String unitValue = matcher.group(2);

        final DataUnit sourceUnit = DataUnit.valueOf(unitValue);
        return new ParsedDataUnit(sizeValue, sourceUnit);
    }

    private static class ParsedDataUnit {

        private final String sizeValue;
        private final DataUnit unitValue;

        public ParsedDataUnit(final String sizeValue, final DataUnit unitValue) {
            this.sizeValue = Objects.requireNonNull(sizeValue);
            this.unitValue = Objects.requireNonNull(unitValue);
        }

        public String getStringValue() {
            return sizeValue + " " + unitValue.name();
        }
    }

    // Helper methods for publishing events

    public static void publishComponentAddedEvent(final FDFlowManager flowManager, final String flowId,
                                                  final String componentId, final VersionedProcessGroup flowContent) {
        publishComponentEvent(
                flowManager,
                flowId,
                componentId,
                FDFlowEventType.COMPONENT_ADDED,
                FDFlowEventType.COMPONENT_ADDED.getDescription(),
                flowContent);
    }

    public static void publishComponentModifiedEvent(final FDFlowManager flowManager, final String flowId,
                                                     final String componentId, final VersionedProcessGroup flowContent) {
        publishComponentEvent(
                flowManager,
                flowId,
                componentId,
                FDFlowEventType.COMPONENT_MODIFIED,
                FDFlowEventType.COMPONENT_MODIFIED.getDescription(),
                flowContent);
    }

    public static void publishComponentRemovedEvent(final FDFlowManager flowManager, final String flowId,
                                                    final String componentId, final VersionedProcessGroup flowContent) {
        publishComponentEvent(
                flowManager,
                flowId,
                componentId,
                FDFlowEventType.COMPONENT_REMOVED,
                FDFlowEventType.COMPONENT_REMOVED.getDescription(),
                flowContent);
    }

    public static void publishComponentEvent(final FDFlowManager flowManager, final String flowId,
                                             final String componentId, final FDFlowEventType eventType,
                                             final String eventDescription, final VersionedProcessGroup flowContent) {
        final FDFlowEvent event = new FDFlowEvent();
        event.setEventType(eventType);
        event.setEventDescription(eventDescription);
        event.setFlowIdentifier(flowId);
        event.setComponentId(componentId);

        final NiFiUser user = NiFiUserUtils.getNiFiUser();
        flowManager.addFlowEvent(event, flowContent, user);
    }

}
