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
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class TestStandardFDConnectionService extends BaseFDServiceTest {

    private VersionedConnection connectionConfig;
    private FDConnectionService connectionService;

    @Override
    public void setupService() {
        final ConnectableComponent source = ComponentUtils.createConnectableComponent(group2Processor1);
        final ConnectableComponent dest = ComponentUtils.createConnectableComponent(group2Processor2);

        connectionConfig = new VersionedConnection();
        connectionConfig.setIdentifier(UUID.randomUUID().toString());
        connectionConfig.setSource(source);
        connectionConfig.setDestination(dest);
        connectionConfig.setSelectedRelationships(Collections.singleton("success"));

        connectionService = new StandardFDConnectionService(flowManager, Mockito.mock(ValidationService.class));
    }

    @Test
    public void testCreateConnectionSameGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final FDConnection createdConnection = connectionService.create(flowId, groupId, connectionConfig);
        assertNotNull(createdConnection);
        assertNotNull(createdConnection.getComponentConfiguration());
        assertEquals(connectionConfig.getIdentifier(), createdConnection.getComponentConfiguration().getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionInvalidBackPressureDataSize() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        connectionConfig.setBackPressureDataSizeThreshold("INVALID");
        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionInvalidFlowFileExpiration() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        connectionConfig.setFlowFileExpiration("INVALID");
        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionNoSourceRelationships() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        connectionConfig.setSelectedRelationships(Collections.emptySet());
        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateConnectionSameGroupFlowNotFound() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateConnectionSameGroupConnectionGroupNotFound() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = "DOES-NOT-EXIST";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionSourceGroupNotFound() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        connectionConfig.getSource().setGroupId("DOES-NOT-EXIST");
        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConnectionDestGroupNotFound() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        connectionConfig.getDestination().setGroupId("DOES-NOT-EXIST");
        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateConnectionWhenSourceGroupNotChildOfConnectionGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the source an output port in process group 1 which is not a child group of the connection group
        final ConnectableComponent newSource = ComponentUtils.createConnectableComponent(group1OutputPort);
        connectionConfig.setSource(newSource);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateConnectionWhenSourceGroupIsChildButSourceNotOutputPort() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the source a processor in group 3 which is a child group of the connection group
        final ConnectableComponent newSource = ComponentUtils.createConnectableComponent(group3Processor1);
        connectionConfig.setSource(newSource);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test
    public void testCreateConnectionFromOutputPortInChildGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the source the output port of group 3 which is in the same group as the destination
        final ConnectableComponent newSource = ComponentUtils.createConnectableComponent(group3OutputPort);
        connectionConfig.setSource(newSource);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateConnectionWhenDestGroupNotChildOfConnectionGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the dest an input port in process group 1 which is not a child group of the connection group
        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group1InputPort);
        connectionConfig.setDestination(newDest);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateConnectionWhenDestGroupIsChildButDestNotInputPort() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the dest an input port in process group 1 which is not a child group of the connection group
        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group3OutputPort);
        connectionConfig.setDestination(newDest);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test
    public void testCreateConnectionToInputPortInChildGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        final String groupId = group2.getIdentifier();

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // Make the dest the input port of group 3 which is in the same group as the source
        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group3InputPort);
        connectionConfig.setDestination(newDest);

        connectionService.create(flowId, groupId, connectionConfig);
    }

    @Test
    public void testUpdateToValidDestination() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group2Processor1);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setSource(connectionG2P2toG2P3.getSource());
        requestConnection.setDestination(newDest);

        final FDConnection updatedConnection = connectionService.update(flowId, requestConnection);
        assertNotNull(updatedConnection);

    }

    @Test
    public void testUpdateToValidDestinationWhenNoSourceProvided() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group2Processor1);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setDestination(newDest);

        final FDConnection updatedConnection = connectionService.update(flowId, requestConnection);
        assertNotNull(updatedConnection);

    }

    @Test(expected = IllegalStateException.class)
    public void testUpdatedWhenSourceChanges() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newSource = ComponentUtils.createConnectableComponent(group2Processor1);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setSource(newSource);
        requestConnection.setDestination(connectionG2P2toG2P3.getDestination());

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testUpdateWhenConnectionDoesNotExist() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group2Processor1);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier("DOES-NOT-EXIST");
        requestConnection.setSource(connectionG2P2toG2P3.getSource());
        requestConnection.setDestination(newDest);

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWhenDestinationGroupDoesNotExist() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group2Processor1);
        newDest.setGroupId("DOES-NOT-EXIST");

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setSource(connectionG2P2toG2P3.getSource());
        requestConnection.setDestination(newDest);

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWhenDestinationComponentNotInDestinationGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group1Processor1);
        newDest.setGroupId(group2.getIdentifier());

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setSource(connectionG2P2toG2P3.getSource());
        requestConnection.setDestination(newDest);

        connectionService.update(flowId, requestConnection);
    }

    @Test
    public void testUpdateDestToInputPortInChildGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group3InputPort);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setDestination(newDest);

        final FDConnection updatedConnection = connectionService.update(flowId, requestConnection);
        assertNotNull(updatedConnection);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateDestToProcessorInChildGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group3Processor1);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setDestination(newDest);

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateDestToInputPortInNonChildGroup() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final ConnectableComponent newDest = ComponentUtils.createConnectableComponent(group1InputPort);

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setDestination(newDest);

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithInvalidBackPressureDataSize() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setBackPressureDataSizeThreshold("INVALID");

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithInvalidFlowFileExpiration() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setFlowFileExpiration("INVALID");

        connectionService.update(flowId, requestConnection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithEmptySelectedRelationships() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedConnection requestConnection = new VersionedConnection();
        requestConnection.setIdentifier(connectionG2P2toG2P3.getIdentifier());
        requestConnection.setSelectedRelationships(Collections.emptySet());

        connectionService.update(flowId, requestConnection);
    }

}
