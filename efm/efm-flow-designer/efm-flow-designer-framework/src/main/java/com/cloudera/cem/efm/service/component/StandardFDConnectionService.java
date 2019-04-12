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

import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.nifi.registry.flow.ComponentType;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.ConnectableComponentType;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedRemoteGroupPort;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static com.cloudera.cem.efm.service.component.ComponentUtils.isNotNull;

/**
 * Standard implementation of FDConnectionService.
 */
@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDConnectionService extends BaseComponentService<VersionedConnection, FDConnection> implements com.cloudera.cem.efm.service.component.FDConnectionService {

    /**
     * The default back-pressure data size threshold when creating a new connection.
     */
    public static final String DEFAULT_BACK_PRESSURE_DATA_SIZE_THRESHOLD = "10000 B";

    /**
     * The default back-pressure object size threshold when creating a new connection.
     */
    public static final long DEFAULT_BACK_PRESSURE_OBJECT_SIZE_THRESHOLD = 0;

    /**
     * The default flow file expiration when creating a new connection.
     */
    public static final String DEFAULT_FLOW_FILE_EXPIRATION = "60 seconds";

    /**
     * Map to look the function to use to find components in a process group based on the ConnectableComponentType.
     */
    private static Map<ConnectableComponentType, Function<VersionedProcessGroup, Set<? extends VersionedComponent>>> componentTypeFunctionMap;
    static {
        componentTypeFunctionMap = new HashMap<>();
        componentTypeFunctionMap.put(ConnectableComponentType.FUNNEL, VersionedProcessGroup::getFunnels);
        componentTypeFunctionMap.put(ConnectableComponentType.PROCESSOR, VersionedProcessGroup::getProcessors);
        componentTypeFunctionMap.put(ConnectableComponentType.INPUT_PORT, VersionedProcessGroup::getInputPorts);
        componentTypeFunctionMap.put(ConnectableComponentType.OUTPUT_PORT, VersionedProcessGroup::getOutputPorts);
        componentTypeFunctionMap.put(ConnectableComponentType.REMOTE_INPUT_PORT, VersionedProcessGroup::getInputPorts);
        componentTypeFunctionMap.put(ConnectableComponentType.REMOTE_OUTPUT_PORT, VersionedProcessGroup::getOutputPorts);
    }

    /**
     * @param flowManager the flow manager
     */
    public StandardFDConnectionService(final FDFlowManager flowManager, final ValidationService validationService) {
        super(flowManager, validationService);
    }

    @Override
    public FDConnection create(final String flowId, final String processGroupId, final VersionedConnection requestComponentConfig) {
        final ConnectableComponent sourceConnectable = requestComponentConfig.getSource();
        final ConnectableComponent destConnectable = requestComponentConfig.getDestination();

        if (sourceConnectable == null || destConnectable == null) {
            throw new IllegalArgumentException("Both source and destination must be specified");
        }

        if (sourceConnectable.getId() == null || destConnectable.getId() == null) {
            throw new IllegalArgumentException("Both source and destination ids must be specified");
        }

        if (sourceConnectable.getType() == null || destConnectable.getType() == null) {
            throw new IllegalArgumentException("Both source and destination types must be specified");
        }

        if (sourceConnectable.getType() == ConnectableComponentType.FUNNEL
                && destConnectable.getType() == ConnectableComponentType.FUNNEL
                && sourceConnectable.getId().equals(destConnectable.getId())) {
            throw new IllegalArgumentException("A funnel cannot be connected to itself");
        }

        if (sourceConnectable.getGroupId() == null) {
            sourceConnectable.setGroupId(processGroupId);
        }

        if (destConnectable.getGroupId() == null) {
            destConnectable.setGroupId(processGroupId);
        }

        // Ensure that processors specify the selected relationships
        if (sourceConnectable.getType() == ConnectableComponentType.PROCESSOR) {
            if (requestComponentConfig.getSelectedRelationships() == null || requestComponentConfig.getSelectedRelationships().isEmpty()) {
                throw new IllegalArgumentException("Selected relationships must be specified");
            }
        }

        validateRequestConfig(requestComponentConfig);

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContent = currentFlow.getFlowContent();
        final VersionedProcessGroup connectionGroup = ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContent);

        final String sourceId = sourceConnectable.getId();
        final String sourceGroupId = sourceConnectable.getGroupId();

        final String destId = destConnectable.getId();
        final String destGroupId = destConnectable.getGroupId();

        // VALIDATE SOURCE

        VersionedRemoteProcessGroup sourceRemoteProcessGroup = null;
        VersionedRemoteGroupPort newRemoteOutputPort = null;

        if (sourceConnectable.getType() == ConnectableComponentType.REMOTE_OUTPUT_PORT) {
            try {
                UUID.fromString(sourceId);
            } catch (Exception  e) {
                throw new IllegalArgumentException("The ID of a remote output port must be a UUID");
            }

            // Locate the remote process group with the group id specified in the source, or throw exception
            sourceRemoteProcessGroup = connectionGroup.getRemoteProcessGroups().stream()
                    .filter(rpg -> rpg.getIdentifier().equals(sourceGroupId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find the specified remote process group for connection source"));

            // Find the output port in the remote process group with the target id of the specified source
            final Optional<VersionedRemoteGroupPort> remoteGroupPort = sourceRemoteProcessGroup.getOutputPorts().stream()
                    .filter(p -> p.getTargetId().equals(sourceId)).findFirst();

            // If we couldn't find a remote output port with that id then create a new one, but don't add it to the RPG until later
            if (!remoteGroupPort.isPresent()) {
                newRemoteOutputPort = new VersionedRemoteGroupPort();
                newRemoteOutputPort.setIdentifier(UUID.randomUUID().toString());
                newRemoteOutputPort.setGroupIdentifier(connectionGroup.getIdentifier());
                newRemoteOutputPort.setRemoteGroupId(sourceRemoteProcessGroup.getIdentifier());
                newRemoteOutputPort.setTargetId(sourceId);
                newRemoteOutputPort.setName(sourceId);
                newRemoteOutputPort.setComponentType(ComponentType.REMOTE_OUTPUT_PORT);
            }

            // set the remote input port name to the id
            sourceConnectable.setName(sourceId);
        } else {
            // Validate source group exists and that the source connectable exists in the source group
            final VersionedProcessGroup sourceGroup = ComponentUtils.getProcessGroupOrIllegalArgument(sourceGroupId, flowContent);

            final boolean sourceGroupContainsSource = containsConnectable(sourceGroup, sourceConnectable);
            if (!sourceGroupContainsSource) {
                throw new IllegalStateException("Cannot add connection because the source component was not found in the source group");
            }

            // If the source is not located in the same group as the connection, then ensure the source group is a child group
            // of the connection group, and ensure the source is an output port
            if (!processGroupId.equals(sourceGroupId)) {
                if (sourceConnectable.getType() != ConnectableComponentType.OUTPUT_PORT) {
                    throw new IllegalStateException("Cannot add connection between " + sourceId + " and " + destId
                            + " because they are in different process groups and the source is not an output port");
                }

                boolean connectionGroupContainsSourceGroup = containsChildGroup(connectionGroup, sourceGroupId);
                if (!connectionGroupContainsSourceGroup) {
                    throw new IllegalStateException("Cannot add connection between " + sourceId + " and " + destId
                            + " because the source group is not a child group of the connection group");
                }
            }
        }


        // VALIDATE DESTINATION

        VersionedRemoteProcessGroup destRemoteProcessGroup = null;
        VersionedRemoteGroupPort newRemoteInputPort = null;

        if (destConnectable.getType() == ConnectableComponentType.REMOTE_INPUT_PORT) {
            try {
                UUID.fromString(destId);
            } catch (Exception  e) {
                throw new IllegalArgumentException("The ID of a remote input port must be a UUID");
            }

            // Locate the remote process group with the group id specified in the dest, or throw exception
            destRemoteProcessGroup = connectionGroup.getRemoteProcessGroups().stream()
                    .filter(rpg -> rpg.getIdentifier().equals(destGroupId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find the specified remote process group for connection destination"));

            // Find the input port in the remote process group with the target id of the specified dest
            final Optional<VersionedRemoteGroupPort> remoteGroupPort = destRemoteProcessGroup.getInputPorts().stream()
                    .filter(p -> p.getTargetId().equals(destId)).findFirst();

            // If we couldn't find a remote input port with that target id then create a new one, but don't add it to the RPG until later
            if (!remoteGroupPort.isPresent()) {
                newRemoteInputPort = new VersionedRemoteGroupPort();
                newRemoteInputPort.setIdentifier(UUID.randomUUID().toString());
                newRemoteInputPort.setGroupIdentifier(connectionGroup.getIdentifier());
                newRemoteInputPort.setRemoteGroupId(destRemoteProcessGroup.getIdentifier());
                newRemoteInputPort.setTargetId(destId);
                newRemoteInputPort.setName(destId);
                newRemoteInputPort.setComponentType(ComponentType.REMOTE_INPUT_PORT);
            }

            // set the remote output port name to the id
            destConnectable.setName(destId);
        } else {
            // Validate dest group exists and that the dest connectable exists in the source group
            final VersionedProcessGroup destGroup = ComponentUtils.getProcessGroupOrIllegalArgument(destGroupId, flowContent);

            final boolean destGroupContainsSource = containsConnectable(destGroup, destConnectable);
            if (!destGroupContainsSource) {
                throw new IllegalStateException("Cannot add connection because the destination component was not found in the destination group");
            }

            // If the dest is not located in the same group as the connection, then ensure the dest group is a child group
            // of the connection group, and ensure the dest is an input port
            if (!processGroupId.equals(destGroupId)) {
                if (destConnectable.getType() != ConnectableComponentType.INPUT_PORT) {
                    throw new IllegalStateException("Cannot add connection between " + sourceId + " and " + destId
                            + " because they are in different process groups and the destination is not an input port");
                }

                boolean connectionGroupContainsDestGroup = containsChildGroup(connectionGroup, destGroupId);
                if (!connectionGroupContainsDestGroup) {
                    throw new IllegalStateException("Cannot add connection between " + sourceId + " and " + destId
                            + " because the destination group is not a child group of the connection group");
                }
            }
        }

        // Now that both source and dest passed validation we can add them to the remote process group
        if (newRemoteOutputPort != null) {
            sourceRemoteProcessGroup.getOutputPorts().add(newRemoteOutputPort);
        }
        if (newRemoteInputPort != null) {
            destRemoteProcessGroup.getInputPorts().add(newRemoteInputPort);
        }

        // Continue on to normal create...

        final FDConnection created = createComponent(
                currentFlow,
                connectionGroup,
                null,
                requestComponentConfig,
                () -> {
                    final VersionedConnection connectionConfig = new VersionedConnection();
                    connectionConfig.setSource(sourceConnectable);
                    connectionConfig.setDestination(destConnectable);
                    connectionConfig.setSelectedRelationships(new HashSet<>());
                    connectionConfig.setName("");
                    connectionConfig.setBends(new ArrayList<>());
                    connectionConfig.setPrioritizers(new ArrayList<>());
                    connectionConfig.setzIndex(new Long(0));
                    connectionConfig.setLabelIndex(new Integer(1));
                    connectionConfig.setBackPressureDataSizeThreshold(DEFAULT_BACK_PRESSURE_DATA_SIZE_THRESHOLD);
                    connectionConfig.setBackPressureObjectThreshold(DEFAULT_BACK_PRESSURE_OBJECT_SIZE_THRESHOLD);
                    connectionConfig.setFlowFileExpiration(DEFAULT_FLOW_FILE_EXPIRATION);

                    final FDConnection connection = new FDConnection();
                    connection.setComponentConfiguration(connectionConfig);
                    return connection;
                    },
                VersionedProcessGroup::getConnections,
                null);

        getValidationService().clearValidationErrors(sourceConnectable.getId());
        getValidationService().clearValidationErrors(destConnectable.getId());

        return created;
    }

    @Override
    public FDConnection get(final String flowId, final String componentId) {
        return getComponent(flowId, componentId, VersionedProcessGroup::getConnections, null);
    }

    @Override
    public FDConnection update(final String flowId, final VersionedConnection requestComponentConfig) {
        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContents = currentFlow.getFlowContent();

        // locate the connection component and ensure it exists
        final String componentId = requestComponentConfig.getIdentifier();
        final VersionedConnection existingConnection = ComponentUtils.getComponentOrNotFound(
                componentId, flowContents, VersionedProcessGroup::getConnections);

        // locate the connection group and ensure it exists
        final String connectionGroupId = existingConnection.getGroupIdentifier();
        final VersionedProcessGroup connectionGroup = ComponentUtils.getProcessGroupOrIllegalArgument(connectionGroupId, flowContents);

        final ConnectableComponent requestSource = requestComponentConfig.getSource();
        final ConnectableComponent existingSource = existingConnection.getSource();

        // if a source was provided then ensure it is the same source as the existing connection
        if (isNotNull(requestSource) && isNotNull(requestSource.getId()) && !requestSource.getId().equals(existingSource.getId())) {
            throw new IllegalStateException("Connection with id " + existingConnection.getIdentifier() + " has conflicting source id");
        }

        // If a processor specifies a non-null set of selected relationships, then it must not be empty
        if (existingSource.getType() == ConnectableComponentType.PROCESSOR
                && isNotNull(requestComponentConfig.getSelectedRelationships())
                && requestComponentConfig.getSelectedRelationships().isEmpty()) {
            throw new IllegalArgumentException("Selected relationships cannot be empty");
        }

        // validate the common request fields for create/update
        validateRequestConfig(requestComponentConfig);

        // determine if the destination changed
        final ConnectableComponent proposedDestination = requestComponentConfig.getDestination();
        if (proposedDestination != null) {
            if (proposedDestination.getId() == null) {
                throw new IllegalArgumentException("Proposed destination must contain an id");
            }

            if (proposedDestination.getType() == null) {
                throw new IllegalArgumentException("Proposed destination must contain a type");
            }

            if (proposedDestination.getType() == ConnectableComponentType.FUNNEL
                    && proposedDestination.getId().equals(existingSource.getId())) {
                throw new IllegalArgumentException("Connecting a funnel to itself is not allowed");
            }

            final ConnectableComponent currentDestination = existingConnection.getDestination();

            // handle remote input port differently
            if (ConnectableComponentType.REMOTE_OUTPUT_PORT == proposedDestination.getType()) {
                throw new IllegalArgumentException("Destination cannot be a remote output port");
            } else if (ConnectableComponentType.REMOTE_INPUT_PORT == proposedDestination.getType()) {

                final String proposedDestinationId = proposedDestination.getId();
                final String proposedDestinationGroupId = proposedDestination.getGroupId();

                try {
                    UUID.fromString(proposedDestinationId);
                } catch (Exception  e) {
                    throw new IllegalArgumentException("The ID of a remote input port must be a UUID");
                }

                // the group id must be specified
                if (proposedDestinationGroupId == null) {
                    throw new IllegalArgumentException("When the destination is a remote input port its group id is required.");
                }

                // if the current destination is a remote input port
                boolean isDifferentRemoteProcessGroup = false;
                if (currentDestination.getType() == ConnectableComponentType.REMOTE_INPUT_PORT) {
                    if (!proposedDestinationGroupId.equals(currentDestination.getGroupId())) {
                        isDifferentRemoteProcessGroup = true;
                    }
                }

                // if the destination is changing or the previous destination was a different remote process group
                if (!proposedDestinationId.equals(currentDestination.getId()) || isDifferentRemoteProcessGroup) {
                    // locate the remote process group or throw exception if not found
                    final VersionedRemoteProcessGroup proposedRemoteProcessGroup = connectionGroup.getRemoteProcessGroups().stream()
                            .filter(rpg -> rpg.getIdentifier().equals(proposedDestinationGroupId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unable to find the specified remote process group for the destination"));

                    // locate an existing remote input port with a target id that matches the proposed destination id
                    final Optional<VersionedRemoteGroupPort> remoteInputPort = proposedRemoteProcessGroup.getInputPorts().stream()
                            .filter(p -> p.getTargetId().equals(proposedDestinationId))
                            .findFirst();

                    // if an existing port wasn't found then create one
                    final VersionedRemoteGroupPort newRemoteInputPort = new VersionedRemoteGroupPort();
                    if (!remoteInputPort.isPresent()) {
                        newRemoteInputPort.setIdentifier(UUID.randomUUID().toString());
                        newRemoteInputPort.setGroupIdentifier(connectionGroup.getIdentifier());
                        newRemoteInputPort.setRemoteGroupId(proposedDestinationGroupId);
                        newRemoteInputPort.setTargetId(proposedDestinationId);
                        newRemoteInputPort.setName(proposedDestinationId);
                        newRemoteInputPort.setComponentType(ComponentType.REMOTE_INPUT_PORT);
                    }

                    // determine if any other connections are still using the original remote input port, and if not remove it
                    if (!isDestStillUsed(existingConnection, currentDestination, connectionGroup)) {
                        removeRemoteInputPort(connectionGroup, currentDestination);
                    }

                    // add the port and update destination on the existing connection
                    proposedRemoteProcessGroup.getInputPorts().add(newRemoteInputPort);
                    proposedDestination.setName(proposedDestinationId);
                    existingConnection.setDestination(proposedDestination);
                }

            } else {

                // if there is a different destination id
                if (!proposedDestination.getId().equals(currentDestination.getId())) {
                    // if the destination connectable's group id has not been set, its inferred to be the current group
                    if (proposedDestination.getGroupId() == null) {
                        proposedDestination.setGroupId(existingConnection.getGroupIdentifier());
                    }

                    // locate the destination group and ensure it exists
                    final VersionedProcessGroup destinationGroup = ComponentUtils.getProcessGroupOrIllegalArgument(
                            proposedDestination.getGroupId(), currentFlow.getFlowContent());

                    // locate destination component in the destination group and ensure it exists
                    final Optional<VersionedComponent> newDestination = getVersionedComponentConnectable(
                            destinationGroup, proposedDestination);

                    if (!newDestination.isPresent()) {
                        throw new IllegalArgumentException("Unable to find the specified destination.");
                    }

                    // if the group of the new dest component is not the group the connection is in, then ensure
                    // the group is a child of the connection's group, and ensure the component is an input port

                    final String existingConnectionGroupId = existingConnection.getGroupIdentifier();
                    final String newDestinationGroupId = newDestination.get().getGroupIdentifier();

                    if (!existingConnectionGroupId.equals(newDestinationGroupId)) {
                        final ComponentType newDestinationComponentType = newDestination.get().getComponentType();
                        if (newDestinationComponentType != ComponentType.INPUT_PORT) {
                            throw new IllegalStateException("Cannot update connection because the destination is in " +
                                    "a different process group and is not an input port");
                        }

                        final boolean connectionGroupContainsDestGroup = containsChildGroup(connectionGroup, newDestinationGroupId);
                        if (!connectionGroupContainsDestGroup) {
                            throw new IllegalStateException("Cannot update connection because the new destination " +
                                    "group is not a child group of the connection group");
                        }
                    }

                    // update the existing connection's destination
                    existingConnection.setDestination(proposedDestination);
                }
            }
        }

        final FDConnection connection = updateComponent(currentFlow, requestComponentConfig, existingConnection, null);

        getValidationService().clearValidationErrors(connection.getComponentConfiguration().getSource().getId());
        getValidationService().clearValidationErrors(connection.getComponentConfiguration().getDestination().getId());

        return connection;
    }

    @Override
    public void delete(final String flowId, final String componentId) {

        final FDFlow currentFlow = getFlowOrNotFound(flowId);
        final VersionedProcessGroup flowContents = currentFlow.getFlowContent();

        // locate the connection component and ensure it exists
        final VersionedConnection existingConnection = ComponentUtils.getComponentOrNotFound(
                componentId, flowContents, VersionedProcessGroup::getConnections);

        final ConnectableComponent existingSource = existingConnection.getSource();
        final ConnectableComponent existingDestination = existingConnection.getDestination();

        // determine if we are deleting a connection that involves a remote port
        if (existingSource.getType() == ConnectableComponentType.REMOTE_OUTPUT_PORT
                || existingDestination.getType() == ConnectableComponentType.REMOTE_INPUT_PORT) {

            // locate the connection group and ensure it exists
            final String connectionGroupId = existingConnection.getGroupIdentifier();
            final VersionedProcessGroup connectionGroup = ComponentUtils.getProcessGroupOrIllegalArgument(connectionGroupId, flowContents);

            // handle the source being a remote output port...
            // determine if any other connections are still using the remote output port, and if not remove it
            if (existingSource.getType() == ConnectableComponentType.REMOTE_OUTPUT_PORT
                    && !isSourceStillUsed(existingConnection, existingSource, connectionGroup)) {
                removeRemoteOutputPort(connectionGroup, existingSource);
            }

            // handle the dest being a remote input port...
            // determine if any other connections are still using the remote input port, and if not remove it
            if (existingDestination.getType() == ConnectableComponentType.REMOTE_INPUT_PORT
                    && !isDestStillUsed(existingConnection, existingDestination, connectionGroup)) {
                removeRemoteInputPort(connectionGroup, existingDestination);
            }

        }

        deleteComponent(currentFlow, existingConnection, VersionedProcessGroup::getConnections);

        getValidationService().clearValidationErrors(existingConnection.getSource().getId());
        getValidationService().clearValidationErrors(existingConnection.getDestination().getId());

    }

    @Override
    protected FDConnection instantiateComponent(final VersionedConnection componentConfig) {
        final FDConnection connection = new FDConnection();
        connection.setComponentConfiguration(componentConfig);
        return connection;
    }

    @Override
    protected void configureComponentSpecifics(final VersionedConnection requestComponent,
                                               final VersionedConnection resultComponent) {

        final Integer labelIndex = requestComponent.getLabelIndex();
        if (isNotNull(labelIndex)) {
            resultComponent.setLabelIndex(labelIndex);
        }

        final Long zIndex = requestComponent.getzIndex();
        if (isNotNull(zIndex)) {
            resultComponent.setzIndex(zIndex);
        }

        final Set<String> selectedRels = requestComponent.getSelectedRelationships();
        if (isNotNull(selectedRels)) {
            resultComponent.setSelectedRelationships(selectedRels);
        }

        final Long backPressureObjectThreshold = requestComponent.getBackPressureObjectThreshold();
        if (isNotNull(backPressureObjectThreshold)) {
            resultComponent.setBackPressureObjectThreshold(backPressureObjectThreshold);
        }

        final String backPressureDataSizeThreshold = requestComponent.getBackPressureDataSizeThreshold();
        if (isNotNull(backPressureDataSizeThreshold)) {
            resultComponent.setBackPressureDataSizeThreshold(backPressureDataSizeThreshold);
        }

        final String flowFileExpiration = requestComponent.getFlowFileExpiration();
        if (isNotNull(flowFileExpiration)) {
            resultComponent.setFlowFileExpiration(flowFileExpiration);
        }

        final List<String> prioritizers = requestComponent.getPrioritizers();
        if (isNotNull(prioritizers)) {
            resultComponent.setPrioritizers(prioritizers);
        }

        final List<Position> bends = requestComponent.getBends();
        if (isNotNull(bends)) {
            resultComponent.setBends(bends);
        }
    }

    private boolean containsChildGroup(final VersionedProcessGroup parentGroup, final String childGroupId) {
        boolean containsChildGroup = false;
        for (final VersionedProcessGroup childGroup : parentGroup.getProcessGroups()) {
            if (childGroup.getIdentifier().equals(childGroupId)) {
                containsChildGroup = true;
                break;
            }
        }
        return containsChildGroup;
    }

    private boolean containsConnectable(final VersionedProcessGroup sourceGroup, final ConnectableComponent source) {
        return getVersionedComponentConnectable(sourceGroup, source).isPresent();
    }

    private Optional<VersionedComponent> getVersionedComponentConnectable(
            final VersionedProcessGroup sourceGroup, final ConnectableComponent source) {

        final Set<? extends VersionedComponent> components = componentTypeFunctionMap
                .get(source.getType())
                .apply(sourceGroup);

        for (final VersionedComponent component : components) {
            if (component.getIdentifier().equals(source.getId())) {
                return Optional.of(component);
            }
        }

        return Optional.empty();
    }

    private boolean isDestStillUsed(final VersionedConnection existingConnection,
                                    final ConnectableComponent existingDestination,
                                    final VersionedProcessGroup connectionGroup) {
        boolean destStillUsed = false;
        for (final VersionedConnection connection : connectionGroup.getConnections()) {
            if (!connection.getIdentifier().equals(existingConnection.getIdentifier())
                    && connection.getDestination().getId().equals(existingDestination.getId())) {
                destStillUsed = true;
                break;
            }
        }
        return destStillUsed;
    }

    private boolean isSourceStillUsed(final VersionedConnection existingConnection,
                                    final ConnectableComponent existingSource,
                                    final VersionedProcessGroup connectionGroup) {
        boolean sourceStillUsed = false;
        for (final VersionedConnection connection : connectionGroup.getConnections()) {
            if (!connection.getIdentifier().equals(existingConnection.getIdentifier())
                    && connection.getSource().getId().equals(existingSource.getId())) {
                sourceStillUsed = true;
                break;
            }
        }
        return sourceStillUsed;
    }

    private void removeRemoteInputPort(final VersionedProcessGroup connectionGroup, final ConnectableComponent remoteInputPort) {
        // locate the remote process group that is specified as the connectable's group id
        final Optional<VersionedRemoteProcessGroup> originalRemoteProcessGroup = connectionGroup.getRemoteProcessGroups().stream()
                .filter(rpg -> rpg.getIdentifier().equals(remoteInputPort.getGroupId()))
                .findFirst();

        // if the remote process group was found then attempt to remove the given remote input port with the matching target id
        if (originalRemoteProcessGroup.isPresent() && originalRemoteProcessGroup.get().getInputPorts() != null) {
            final Iterator<VersionedRemoteGroupPort> iter = originalRemoteProcessGroup.get().getInputPorts().iterator();
            while (iter.hasNext()) {
                final VersionedRemoteGroupPort p = iter.next();
                if (p.getTargetId().equals(remoteInputPort.getId())) {
                    iter.remove();
                }
            }
        }
    }

    private void removeRemoteOutputPort(VersionedProcessGroup connectionGroup, ConnectableComponent remoteOutputPort) {
        // locate the remote process group that is specified as the connectable's group id
        final Optional<VersionedRemoteProcessGroup> remoteProcessGroup = connectionGroup.getRemoteProcessGroups().stream()
                .filter(rpg -> rpg.getIdentifier().equals(remoteOutputPort.getGroupId()))
                .findFirst();

        // if the remote process group was found then attempt to remove the given remote output port with the matching target id
        if (remoteProcessGroup.isPresent() && remoteProcessGroup.get().getOutputPorts() != null) {
            final Iterator<VersionedRemoteGroupPort> iter = remoteProcessGroup.get().getOutputPorts().iterator();
            while (iter.hasNext()) {
                final VersionedRemoteGroupPort p = iter.next();
                if (p.getTargetId().equals(remoteOutputPort.getId())) {
                    iter.remove();
                }
            }
        }
    }

    private void validateRequestConfig(final VersionedConnection requestComponentConfig) {
        // Ensure back-pressure data size is the proper format
        final String backPressureDataSizeThreshold = requestComponentConfig.getBackPressureDataSizeThreshold();
        if (isNotNull(backPressureDataSizeThreshold)) {
            final String parsedValue = ComponentUtils.validateDataUnitValue(backPressureDataSizeThreshold);
            requestComponentConfig.setBackPressureDataSizeThreshold(parsedValue);
        }

        // Ensure flow file expiration is the proper format
        final String flowFileExpiration = requestComponentConfig.getFlowFileExpiration();
        if (isNotNull(flowFileExpiration)) {
            ComponentUtils.validateTimePeriodValue(flowFileExpiration);
        }

        // Ensure the back-pressure object threshold is non-negative
        final Long backPressureObjectThreshold = requestComponentConfig.getBackPressureObjectThreshold();
        if (isNotNull(backPressureObjectThreshold) && backPressureObjectThreshold < 0) {
            throw new IllegalArgumentException("Back Pressure Object Threshold must be a positive number");
        }

        // Ensure the label index is a positive number
        final Integer labelIndex = requestComponentConfig.getLabelIndex();
        if (isNotNull(labelIndex) && labelIndex < 0) {
            throw new IllegalArgumentException("Label index must be a positive number");
        }
    }

}
