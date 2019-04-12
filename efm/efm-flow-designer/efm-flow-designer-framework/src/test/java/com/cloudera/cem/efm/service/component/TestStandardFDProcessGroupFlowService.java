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
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlow;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlowContent;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.StandardControllerServiceLookup;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class TestStandardFDProcessGroupFlowService extends BaseFDServiceTest {

    private ProcessorDefinition processorDefinition;
    private ControllerServiceDefinition controllerServiceDefinition;

    private FDProcessorService processorService;
    private FDControllerServiceService controllerServiceService;

    private FDProcessGroupFlowService processGroupFlowService;

    @Override
    public void setupService() {
        processorDefinition = new ProcessorDefinition();
        processorDefinition.setType("FOO-PROC");

        controllerServiceDefinition = new ControllerServiceDefinition();
        controllerServiceDefinition.setType("FOO-CS");

        processorService = Mockito.mock(FDProcessorService.class);
        controllerServiceService = Mockito.mock(FDControllerServiceService.class);

        final ValidationService validationService = Mockito.mock(ValidationService.class);
        final ControllerServiceLookup serviceLookup = new StandardControllerServiceLookup(flowManager, extensionManagers, validationService);
        processGroupFlowService = new StandardFDProcessGroupFlowService(flowManager, processorService, controllerServiceService, validationService, serviceLookup);
    }

    @Test
    public void testGetProcessGroupWithChildren() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // We just want to prove that the service is populating the component definitions, so just return anything here
        when(processorService.getComponentDefinition(any(String.class), any(VersionedProcessor.class))).thenReturn(processorDefinition);
        when(controllerServiceService.getComponentDefinition(any(String.class), any(VersionedControllerService.class))).thenReturn(controllerServiceDefinition);

        final FDProcessGroupFlow processGroupFlow = processGroupFlowService.get(flowId, flowContent.getIdentifier(), true);
        assertNotNull(processGroupFlow);

        final FDProcessGroupFlowContent processGroupFlowContent = processGroupFlow.getFlowContent();
        assertTrue(processGroupFlowContent.getProcessGroups().size() > 0);
        assertEquals(flowContent.getProcessGroups().size(), processGroupFlowContent.getProcessGroups().size());

        final int totalProcessors = getNumberOfProcessors(processGroupFlowContent);
        assertEquals(5, totalProcessors);

        final int totalServices = getNumberOfControllerServices(processGroupFlowContent);
        assertEquals(1, totalServices);
    }

    @Test
    public void testGetProcessGroupWithoutChildren() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final FDProcessGroupFlow processGroupFlow = processGroupFlowService.get(flowId, flowContent.getIdentifier(), false);
        assertNotNull(processGroupFlow);

        final FDProcessGroupFlowContent processGroupFlowContent = processGroupFlow.getFlowContent();
        assertEquals(0, processGroupFlowContent.getProcessGroups().size());
    }

    @Test
    public void testGetProcessGroupUsingRootAlias() {
        final String flowId = "1";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));
        when(processorService.getComponentDefinition(any(String.class), any(VersionedProcessor.class))).thenReturn(processorDefinition);
        when(controllerServiceService.getComponentDefinition(any(String.class), any(VersionedControllerService.class))).thenReturn(controllerServiceDefinition);

        final FDProcessGroupFlow processGroupFlow = processGroupFlowService.get(flowId, "root", true);
        assertNotNull(processGroupFlow);

        final FDProcessGroupFlowContent processGroupFlowContent = processGroupFlow.getFlowContent();
        assertTrue(processGroupFlowContent.getProcessGroups().size() > 0);
        assertEquals(flowContent.getProcessGroups().size(), processGroupFlowContent.getProcessGroups().size());
    }

    @Test
    public void testGetProcessGroupNonRootGroup() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        // We just want to prove that the service is populating the component definitions, so just return anything here
        when(processorService.getComponentDefinition(any(String.class), any(VersionedProcessor.class))).thenReturn(processorDefinition);
        when(controllerServiceService.getComponentDefinition(any(String.class), any(VersionedControllerService.class))).thenReturn(controllerServiceDefinition);

        final FDProcessGroupFlow processGroupFlow = processGroupFlowService.get(flowId, group3.getIdentifier(), true);
        assertNotNull(processGroupFlow);

        final FDProcessGroupFlowContent processGroupFlowContent = processGroupFlow.getFlowContent();
        assertEquals(0, processGroupFlowContent.getProcessGroups().size());
        assertEquals(group3.getProcessGroups().size(), processGroupFlowContent.getProcessGroups().size());

        final int totalProcessors = getNumberOfProcessors(processGroupFlowContent);
        assertEquals(1, totalProcessors);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetProcessGroupWhenFlowDoesNotExist() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.empty());
        processGroupFlowService.get(flowId, flowContent.getIdentifier(), true);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetProcessGroupWhenWhenProcessGroupDoesNotExist() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));
        processGroupFlowService.get(flowId, "DOES-NOT-EXIST", true);
    }

    private int getNumberOfProcessors(final FDProcessGroupFlowContent processGroupFlowContent) {
        final AtomicInteger count = new AtomicInteger(0);

        for (final FDProcessor processor : processGroupFlowContent.getProcessors()) {
            assertNotNull(processor.getComponentConfiguration());
            assertNotNull(processor.getComponentDefinition());
            count.incrementAndGet();
        }

        processGroupFlowContent.getProcessGroups().stream().forEach(pg -> count.addAndGet(getNumberOfProcessors(pg)));

        return count.get();
    }

    private int getNumberOfControllerServices(final FDProcessGroupFlowContent processGroupFlowContent) {
        final AtomicInteger count = new AtomicInteger(0);

        for (final FDControllerService service : processGroupFlowContent.getControllerServices()) {
            assertNotNull(service.getComponentConfiguration());
            assertNotNull(service.getComponentDefinition());
            count.incrementAndGet();
        }

        processGroupFlowContent.getProcessGroups().stream().forEach(pg -> count.addAndGet(getNumberOfControllerServices(pg)));

        return count.get();
    }

}
