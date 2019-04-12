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
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class TestStandardFDProcessorService extends BaseFDServiceTest {

    private FDProcessorService processorService;

    @Override
    public void setupService() {
        processorService = new StandardFDProcessorService(flowManager, extensionManagers, Mockito.mock(ValidationService.class));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateProcessorWhenFlowDoesNotExist() {
        final String flowId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());
        processorService.create(flowId, "pgId", new VersionedProcessor());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateProcessorWhenProcessGroupDoesNotExist() {
        final String flowId = "1";
        final String processGroupId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));
        processorService.create(flowId, processGroupId, new VersionedProcessor());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateProcessorWhenExtensionManagerDoesNotExist() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.empty());

        processorService.create(flowId, processGroupId, new VersionedProcessor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWhenManifestProcessorDoesNotExist() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenThrow(new IllegalArgumentException("Bundle does not exist"));

        processorService.create(flowId, processGroupId, requestProcessor);
    }

    @Test
    public void testCreateProcessorSuccess() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();

        final FDProcessor resultProcessor = new FDProcessor();
        resultProcessor.setComponentConfiguration(requestProcessor);
        resultProcessor.setComponentDefinition(new ProcessorDefinition());

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenReturn(resultProcessor);

        assertEquals(3, group2.getProcessors().size());

        final FDProcessor createdProcessor = processorService.create(flowId, processGroupId, requestProcessor);

        assertNotNull(createdProcessor);
        assertNotNull(createdProcessor.getComponentConfiguration());
        assertNotNull(createdProcessor.getComponentDefinition());
        assertEquals("GenerateFlowFile", createdProcessor.getComponentConfiguration().getName());
        assertEquals(4, group2.getProcessors().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWithInvalidSchedulingStrategy() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();
        requestProcessor.setSchedulingStrategy("INVALID");

        final FDProcessor resultProcessor = new FDProcessor();
        resultProcessor.setComponentConfiguration(requestProcessor);
        resultProcessor.setComponentDefinition(new ProcessorDefinition());

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenReturn(resultProcessor);

        processorService.create(flowId, processGroupId, requestProcessor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWithInvalidSchedulingPeriod() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();
        requestProcessor.setSchedulingPeriod("INVALID");

        final FDProcessor resultProcessor = new FDProcessor();
        resultProcessor.setComponentConfiguration(requestProcessor);
        resultProcessor.setComponentDefinition(new ProcessorDefinition());

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenReturn(resultProcessor);

        processorService.create(flowId, processGroupId, requestProcessor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWithInvalidPenaltyDuration() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();
        requestProcessor.setPenaltyDuration("INVALID");

        final FDProcessor resultProcessor = new FDProcessor();
        resultProcessor.setComponentConfiguration(requestProcessor);
        resultProcessor.setComponentDefinition(new ProcessorDefinition());

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenReturn(resultProcessor);

        processorService.create(flowId, processGroupId, requestProcessor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWithInvalidYieldDuration() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final VersionedProcessor requestProcessor = createVersionedProcessor();
        requestProcessor.setYieldDuration("INVALID");

        final FDProcessor resultProcessor = new FDProcessor();
        resultProcessor.setComponentConfiguration(requestProcessor);
        resultProcessor.setComponentDefinition(new ProcessorDefinition());

        when(extensionManager.createProcessor(
                requestProcessor.getType(),
                requestProcessor.getBundle()))
                .thenReturn(resultProcessor);

        processorService.create(flowId, processGroupId, requestProcessor);
    }

    private VersionedProcessor createVersionedProcessor() {
        final Bundle bundle = new Bundle();
        bundle.setGroup("org.apache.nifi.minifi");
        bundle.setArtifact("minifi-system");
        bundle.setVersion("0.6.0");

        final VersionedProcessor requestProcessor = new VersionedProcessor();
        requestProcessor.setType("org.apache.nifi.minifi.processors.GenerateFlowFile");
        requestProcessor.setBundle(bundle);
        return requestProcessor;
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetProcessorWhenFlowDoesNotExist() {
        final String flowId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());
        processorService.get(flowId, "procId");
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetProcessorWhenProcessorDoesNotExist() {
        final String flowId = "1";
        final String processorId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));
        processorService.get(flowId, processorId);
    }

    @Test
    public void testGetProcessorSuccess() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        when(extensionManager.getProcessorDefinition(group2Processor1.getType(), group2Processor1.getBundle()))
                .thenReturn(new ProcessorDefinition());

        final FDProcessor retrievedProcessor = processorService.get(flowId, group2Processor1.getIdentifier());
        assertNotNull(retrievedProcessor);
        assertNotNull(retrievedProcessor.getComponentConfiguration());
        assertNotNull(retrievedProcessor.getComponentDefinition());
        assertEquals(group2Processor1.getIdentifier(), retrievedProcessor.getComponentConfiguration().getIdentifier());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testUpdateProcessorWhenFlowDoesNotExist() {
        final String flowId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());
        processorService.update(flowId, group2Processor1);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testUpdateProcessorWhenProcessorDoesNotExist() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        final VersionedProcessor requestProcessor = new VersionedProcessor();
        requestProcessor.setIdentifier("DOES-NOT-EXIST");

        processorService.update(flow.getFlowMetadata().getIdentifier(), requestProcessor);
    }

    @Test
    public void testUpdateProcessorSuccess() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        when(extensionManager.getProcessorDefinition(group2Processor1.getType(), group2Processor1.getBundle()))
                .thenReturn(new ProcessorDefinition());

        final String updatedName = "NEW NAME";

        final VersionedProcessor requestProcessor = new VersionedProcessor();
        requestProcessor.setIdentifier(group2Processor1.getIdentifier());
        requestProcessor.setName(updatedName);

        assertNotEquals(updatedName, group2Processor1.getName());

        final FDProcessor updatedProcessor = processorService.update(flow.getFlowMetadata().getIdentifier(), requestProcessor);
        assertNotNull(updatedProcessor);
        assertNotNull(updatedProcessor.getComponentConfiguration());
        assertNotNull(updatedProcessor.getComponentDefinition());

        assertEquals(updatedName, group2Processor1.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateProcessorInvalidSchedulingStrategy() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        when(extensionManager.getProcessorDefinition(group2Processor1.getType(), group2Processor1.getBundle()))
                .thenReturn(new ProcessorDefinition());

        final String updatedSchedulingStrategy = "INVALID";

        final VersionedProcessor requestProcessor = new VersionedProcessor();
        requestProcessor.setIdentifier(group2Processor1.getIdentifier());
        requestProcessor.setSchedulingStrategy(updatedSchedulingStrategy);

        processorService.update(flow.getFlowMetadata().getIdentifier(), requestProcessor);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteProcessorWhenFlowDoesNotExist() {
        final String flowId = "DOES-NOT-EXIST";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());
        processorService.delete(flowId, group2Processor1.getIdentifier());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteProcessorWhenProcessGroupDoesNotExist() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));
        group2Processor1.setGroupIdentifier("does-not-exist");
        processorService.delete(flow.getFlowMetadata().getIdentifier(), group2Processor1.getIdentifier());
    }

    @Test
    public void testDeleteProcessorWithNoConnections() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        assertEquals(3, group2.getProcessors().size());
        assertEquals(1, group2.getConnections().size());

        processorService.delete(
                flow.getFlowMetadata().getIdentifier(),
                group2Processor1.getIdentifier());

        // Verify one processor was removed and the connection was removed
        assertEquals(2, group2.getProcessors().size());
        assertEquals(1, group2.getConnections().size());
    }

    @Test
    public void testDeleteProcessorWhenSourceOfConnection() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        assertEquals(3, group2.getProcessors().size());

        final String processorId = group2Processor2.getIdentifier();

        // Verify there is one connection and the source is the component we are going to delete
        assertEquals(1, group2.getConnections().size());
        assertEquals(processorId, group2.getConnections().stream().findFirst().get().getSource().getId());

        processorService.delete(flow.getFlowMetadata().getIdentifier(), processorId);

        // Verify one processor was removed and the connection was removed
        assertEquals(2, group2.getProcessors().size());
        assertEquals(0, group2.getConnections().size());
    }

    @Test
    public void testDeleteProcessorWhenDestinationOfConnection() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        assertEquals(3, group2.getProcessors().size());

        final String processorId = group2Processor3.getIdentifier();

        // Verify there is one connection and the source is the component we are going to delete
        assertEquals(1, group2.getConnections().size());
        assertEquals(processorId, group2.getConnections().stream().findFirst().get().getDestination().getId());

        processorService.delete(flow.getFlowMetadata().getIdentifier(), processorId);

        // Verify one processor was removed and the connection was removed
        assertEquals(2, group2.getProcessors().size());
        assertEquals(0, group2.getConnections().size());
    }
}
