/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */

package com.cloudera.cem.efm.validation;

import com.cloudera.cem.efm.model.extension.InputRequirement;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.Relationship;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessorValidationContext extends PropertyValidationContext {
    private final ProcessorDefinition definition;

    public ProcessorValidationContext(final ProcessorDefinition processorDefinition) {
        super(processorDefinition.getPropertyDescriptors(), Boolean.TRUE.equals(processorDefinition.getSupportsDynamicProperties()));
        this.definition = processorDefinition;
    }

    public Collection<String> validate(final VersionedProcessor configuration, final VersionedProcessGroup flow,
                                       final String flowId, final ControllerServiceLookup controllerServiceDefinitionLookup) {
        final List<String> validationErrors = new ArrayList<>();

        validationErrors.addAll(validateProperties(configuration.getProperties(), flow, flowId, controllerServiceDefinitionLookup));
        validationErrors.addAll(validateRelationships(configuration, flow));
        validationErrors.addAll(validateInputRequirement(configuration, flow));

        return validationErrors;
    }

    private Collection<String> validateInputRequirement(final VersionedProcessor processor, final VersionedProcessGroup flow) {
        final InputRequirement inputRequirement = definition.getInputRequirement();
        if (inputRequirement == null || inputRequirement == InputRequirement.INPUT_ALLOWED) {
            return Collections.emptyList();
        }

        final boolean hasIncoming = hasIncomingConnection(processor, flow, false);

        if (inputRequirement == InputRequirement.INPUT_REQUIRED) {
            if (!hasIncoming) {
                return Collections.singleton("Component must have an incoming Connection");
            }
        } else if (inputRequirement == InputRequirement.INPUT_FORBIDDEN) {
            if (hasIncoming) {
                return Collections.singleton("Component is not allowed to have an incoming Connection");
            }
        }

        return Collections.emptyList();
    }

    private Collection<String> validateRelationships(final VersionedProcessor processor, final VersionedProcessGroup flow) {
        // Determine the relationships that must be auto-terminated or connected
        final Set<String> definedRelationships = getDefinedRelationships(processor.getProperties().keySet());

        // Maintain a Set of all Relationships that have not been connected or auto-terminated
        final Set<String> unconnectedRelationships = new HashSet<>(definedRelationships);
        if (processor.getAutoTerminatedRelationships() != null) {
            unconnectedRelationships.removeAll(processor.getAutoTerminatedRelationships());
        }

        if (unconnectedRelationships.isEmpty()) {
            return Collections.emptyList();
        }

        // For each outbound connection for this Processor, remove any Relationships that are included in the connection
        // from our set of unconnected relationships (i.e., mark them as connected).
        final Set<VersionedConnection> outboundConnections = getOutboundConnections(processor, flow);
        outboundConnections.stream()
            .flatMap(connection -> connection.getSelectedRelationships().stream())
            .forEach(unconnectedRelationships::remove);

        if (unconnectedRelationships.isEmpty()) {
            return Collections.emptyList();
        }

        // Add a validation error for any Relationship that has been defined but not connected
        final List<String> validationErrors = new ArrayList<>();
        unconnectedRelationships.stream()
            .map(rel -> String.format("The Relationship '%s' has not been auto-terminated and no Connection exists for this Relationship", rel))
            .forEach(validationErrors::add);

        // Add a validation error for any Relationship that has been connected but is not defined
        outboundConnections.stream()
            .flatMap(connection -> connection.getSelectedRelationships().stream())
            .filter(relationship -> !definedRelationships.contains(relationship))
            .distinct()
            .map(relationship -> String.format("An outbound Connection has a Relationship '%s' but this Processor no longer has a Relationship by that name", relationship))
            .forEach(validationErrors::add);

        return validationErrors;
    }

    private Set<String> getDefinedRelationships(final Set<String> propertyNames) {
        final Set<String> supportedRelationshipNames = definition.getSupportedRelationships().stream()
            .map(Relationship::getName)
            .collect(Collectors.toSet());

        final boolean supportsDynamic = Boolean.TRUE.equals(definition.getSupportsDynamicRelationships());
        if (!supportsDynamic) {
            return supportedRelationshipNames;
        }

        // Determine the set of all Relationships that must have an outgoing connection
        final Set<String> definedRelationships = new HashSet<>();
        definedRelationships.addAll(supportedRelationshipNames);

        // If a Processor supports dynamic relationships, then any 'dynamic' property equates to a dynamic relationship.
        // I.e., any property that is not in the Property Descriptors provided by the Processor Definition corresponds to a dynamic relationship.
        final Set<String> supportedPropertyNames = definition.getPropertyDescriptors().keySet();
        for (final String propertyName : propertyNames) {
            if (!supportedPropertyNames.contains(propertyName)) {
                definedRelationships.add(propertyName);
            }
        }

        return definedRelationships;
    }

    /**
     * Returns the Set of all Connections whose source is the given Processor
     * @param processor the Processor for which outbound Connections should be determined
     * @param group the Process Group that the Processor lives in
     * @return the Set of all Connections for which the given Processor is the source
     */
    private Set<VersionedConnection> getOutboundConnections(final VersionedProcessor processor, final VersionedProcessGroup group) {
        return group.getConnections().stream()
            .filter(connection -> connection.getSource().getId().equals(processor.getIdentifier()))
            .collect(Collectors.toSet());
    }

    private boolean hasIncomingConnection(final VersionedProcessor processor, final VersionedProcessGroup group, final boolean includeSelfLoops) {
        final String processorId = processor.getIdentifier();

        for (final VersionedConnection connection : group.getConnections()) {
            if (!connection.getDestination().getId().equals(processorId)) {
                continue;
            }

            if (!includeSelfLoops && connection.getSource().getId().equals(processorId)) {
                continue;
            }

            return true;
        }

        return false;
    }
}
