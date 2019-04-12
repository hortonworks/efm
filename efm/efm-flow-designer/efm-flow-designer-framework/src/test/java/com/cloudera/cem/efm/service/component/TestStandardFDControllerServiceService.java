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
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class TestStandardFDControllerServiceService extends BaseFDServiceTest {

    private FDControllerServiceService controllerServiceService;

    @Override
    public void setupService() {
        controllerServiceService = new StandardFDControllerServiceService(flowManager, extensionManagers, Mockito.mock(ValidationService.class));
    }

    @Test
    public void testCreateControllerServiceSuccess() {
        final String flowId = "1";
        final String processGroupId = "child-level-2";

        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        final Bundle bundle = new Bundle();
        bundle.setGroup("org.apache.nifi.minifi");
        bundle.setArtifact("minifi-system");
        bundle.setVersion("0.6.0");

        final VersionedControllerService requestService = new VersionedControllerService();
        requestService.setType("org.apache.nifi.minifi.controllers.SSLContextService");
        requestService.setBundle(bundle);

        final FDControllerService resultService = new FDControllerService();
        resultService.setComponentConfiguration(requestService);
        resultService.setComponentDefinition(new ControllerServiceDefinition());

        when(extensionManager.createControllerService(
                requestService.getType(),
                requestService.getBundle()))
                .thenReturn(resultService);

        assertEquals(1, group2.getControllerServices().size());

        final FDControllerService createdService = controllerServiceService.create(
                flowId, processGroupId, requestService);

        assertNotNull(createdService);
        assertNotNull(createdService.getComponentConfiguration());
        assertNotNull(createdService.getComponentDefinition());
        assertEquals("SSLContextService", createdService.getComponentConfiguration().getName());
        assertEquals(2, group2.getControllerServices().size());
    }

    @Test
    public void testGetControllerServiceSuccess() {
        final String flowId = "1";
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        when(extensionManager.getControllerServiceDefinition(existingService.getType(), existingService.getBundle()))
                .thenReturn(new ControllerServiceDefinition());

        final FDControllerService retrievedService = controllerServiceService.get(flowId, existingService.getIdentifier());
        assertNotNull(retrievedService);
        assertNotNull(retrievedService.getComponentConfiguration());
        assertNotNull(retrievedService.getComponentDefinition());
        assertEquals(existingService.getIdentifier(), retrievedService.getComponentConfiguration().getIdentifier());
    }

    @Test
    public void testUpdateControllerServiceSuccess() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));

        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass()))
                .thenReturn(Optional.of(extensionManager));

        when(extensionManager.getControllerServiceDefinition(existingService.getType(), existingService.getBundle()))
                .thenReturn(new ControllerServiceDefinition());

        final String updatedName = "NEW NAME";

        final VersionedControllerService requestService = new VersionedControllerService();
        requestService.setIdentifier(existingService.getIdentifier());
        requestService.setName(updatedName);

        assertNotEquals(updatedName, existingService.getName());

        final FDControllerService updatedService = controllerServiceService.update(
                flow.getFlowMetadata().getIdentifier(), requestService);
        assertNotNull(updatedService);
        assertNotNull(updatedService.getComponentConfiguration());
        assertNotNull(updatedService.getComponentDefinition());

        assertEquals(updatedName, existingService.getName());
    }

    @Test
    public void testDeleteControllerServiceSuccess() {
        when(flowManager.getFlow(flow.getFlowMetadata().getIdentifier())).thenReturn(Optional.of(flow));
        when(extensionManagers.getExtensionManager(flow.getFlowMetadata().getAgentClass())).thenReturn(Optional.of(extensionManager));

        assertEquals(1, group2.getControllerServices().size());

        controllerServiceService.delete(
                flow.getFlowMetadata().getIdentifier(),
                existingService.getIdentifier());

        assertEquals(0, group2.getControllerServices().size());
    }

}
