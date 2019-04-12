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
package com.cloudera.cem.efm.model.flow;

import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.component.FDRevision;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a process group using flow designer objects that wrap the registry objects.
 */
@ApiModel
public class FDProcessGroupFlowContent {

    private String identifier;
    private String groupId;
    private String name;
    private String comments;
    private Position position;

    private String uri;
    private FDRevision revision;

    private Set<FDProcessGroupFlowContent> processGroups = new HashSet<>();
    private Set<FDRemoteProcessGroup> remoteProcessGroups = new HashSet<>();
    private Set<FDProcessor> processors = new HashSet<>();
    private Set<FDConnection> connections = new HashSet<>();
    private Set<FDFunnel> funnels = new HashSet<>();
    private Set<FDControllerService> controllerServices = new HashSet<>();

    @ApiModelProperty("The component's unique identifier")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty("The ID of the Process Group that this component belongs to")
    public String getGroupIdentifier() {
        return groupId;
    }

    public void setGroupIdentifier(String groupId) {
        this.groupId = groupId;
    }

    @ApiModelProperty("The component's name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("The component's position on the graph")
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @ApiModelProperty("The user-supplied comments for the component")
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @ApiModelProperty(value = "The URI for future requests to the component.", readOnly = true)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(
            value = "The revision for this request/response. The revision is required for any mutable flow requests and is included in all responses."
    )
    public FDRevision getRevision() {
        return revision;
    }

    public void setRevision(FDRevision revision) {
        this.revision = revision;
    }


    @ApiModelProperty("The child Process Groups")
    public Set<FDProcessGroupFlowContent> getProcessGroups() {
        return processGroups;
    }

    public void setProcessGroups(Set<FDProcessGroupFlowContent> processGroups) {
        this.processGroups = processGroups;
    }

    @ApiModelProperty("The Remote Process Groups")
    public Set<FDRemoteProcessGroup> getRemoteProcessGroups() {
        return remoteProcessGroups;
    }

    public void setRemoteProcessGroups(Set<FDRemoteProcessGroup> remoteProcessGroups) {
        this.remoteProcessGroups = remoteProcessGroups;
    }

    @ApiModelProperty("The Processors")
    public Set<FDProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Set<FDProcessor> processors) {
        this.processors = processors;
    }

    @ApiModelProperty("The Connections")
    public Set<FDConnection> getConnections() {
        return connections;
    }

    public void setConnections(Set<FDConnection> connections) {
        this.connections = connections;
    }

    @ApiModelProperty("The Funnels")
    public Set<FDFunnel> getFunnels() {
        return funnels;
    }

    public void setFunnels(Set<FDFunnel> funnels) {
        this.funnels = funnels;
    }

    @ApiModelProperty("The Controller Services")
    public Set<FDControllerService> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(Set<FDControllerService> controllerServices) {
        this.controllerServices = controllerServices;
    }

    /**
     * Converts this process group to a VersionedProcessGroup.
     *
     * @return a VersionedProcessGroup with the same components as this process group
     */
    public VersionedProcessGroup toVersionedProcessGroup() {
        return toVersionedProcessGroup(this);
    }

    private static VersionedProcessGroup toVersionedProcessGroup(final FDProcessGroupFlowContent source) {
        final VersionedProcessGroup destination = new VersionedProcessGroup();

        // Copy over base fields from VersionedComponents...

        destination.setIdentifier(source.getIdentifier());
        destination.setGroupIdentifier(source.getGroupIdentifier());
        destination.setName(source.getName());
        destination.setComments(source.getComments());

        if (source.getPosition() != null) {
            destination.setPosition(new Position(source.getPosition().getX(), source.getPosition().getY()));
        } else {
            destination.setPosition(new Position(0, 0));
        }

        // Map the sets of components into the types needed for VersionedProcessGroup...

        final Set<VersionedProcessor> processors = source.getProcessors().stream()
                .map(p -> p.getComponentConfiguration())
                .collect(Collectors.toSet());
        destination.setProcessors(processors);

        final Set<VersionedRemoteProcessGroup> remoteProcessGroups = source.getRemoteProcessGroups().stream()
                .map(p -> p.getComponentConfiguration())
                .collect(Collectors.toSet());
        destination.setRemoteProcessGroups(remoteProcessGroups);

        final Set<VersionedConnection> connections = source.getConnections().stream()
                .map(p -> p.getComponentConfiguration())
                .collect(Collectors.toSet());
        destination.setConnections(connections);

        final Set<VersionedFunnel> funnels = source.getFunnels().stream()
                .map(p -> p.getComponentConfiguration())
                .collect(Collectors.toSet());
        destination.setFunnels(funnels);

        final Set<VersionedControllerService> controllerServices = source.getControllerServices().stream()
                .map(p -> p.getComponentConfiguration())
                .collect(Collectors.toSet());
        destination.setControllerServices(controllerServices);

        // Recursively map the process groups...

        final Set<VersionedProcessGroup> processGroups = source.getProcessGroups().stream()
                .map(p -> toVersionedProcessGroup(p))
                .collect(Collectors.toSet());
        destination.setProcessGroups(processGroups);

        return destination;
    }


}
