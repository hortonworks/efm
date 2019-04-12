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

import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.service.validation.ValidationService;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class TestStandardFDRemoteProcessGroupService extends BaseFDServiceTest {

    private FDRemoteProcessGroupService service;

    @Override
    public void setupService() {
        service = new StandardFDRemoteProcessGroupService(flowManager, Mockito.mock(ValidationService.class));
    }

    @Test
    public void testCreateSuccessWithValuesSpecified() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://host1,http://host2");
        requestConfig.setCommunicationsTimeout("30 seconds");
        requestConfig.setYieldDuration("10 seconds");
        requestConfig.setTransportProtocol("HTTP");

        final FDRemoteProcessGroup createdRemoteProcessGroup = service.create(flowId, group1.getIdentifier(), requestConfig);
        assertNotNull(createdRemoteProcessGroup);
        assertEquals(requestConfig.getTargetUris(), createdRemoteProcessGroup.getComponentConfiguration().getTargetUris());
        assertEquals(requestConfig.getCommunicationsTimeout(), createdRemoteProcessGroup.getComponentConfiguration().getCommunicationsTimeout());
        assertEquals(requestConfig.getYieldDuration(), createdRemoteProcessGroup.getComponentConfiguration().getYieldDuration());
        assertEquals(requestConfig.getTransportProtocol(), createdRemoteProcessGroup.getComponentConfiguration().getTransportProtocol());
    }

    @Test
    public void testCreateSuccessWithDefaults() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://host1,http://host2");

        final FDRemoteProcessGroup createdRemoteProcessGroup = service.create(flowId, group1.getIdentifier(), requestConfig);
        assertNotNull(createdRemoteProcessGroup);
        assertEquals(requestConfig.getTargetUris(), createdRemoteProcessGroup.getComponentConfiguration().getTargetUris());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSuccessWithInvalidUris() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("uri-1,uri-2");

        final FDRemoteProcessGroup createdRemoteProcessGroup = service.create(flowId, group1.getIdentifier(), requestConfig);
        assertNotNull(createdRemoteProcessGroup);
        assertEquals(requestConfig.getTargetUris(), createdRemoteProcessGroup.getComponentConfiguration().getTargetUris());
    }

    @Test
    public void testCreateSuccessWithTrimmedUris() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("   http://host1,   http://host2     ");

        final FDRemoteProcessGroup createdRemoteProcessGroup = service.create(flowId, group1.getIdentifier(), requestConfig);
        assertNotNull(createdRemoteProcessGroup);
        assertEquals("http://host1,http://host2", createdRemoteProcessGroup.getComponentConfiguration().getTargetUris());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithInvalidTransportProtocol() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("uri-1,uri-2");
        requestConfig.setTransportProtocol("INVALID");

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithInvalidYieldDuration() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("uri-1,uri-2");
        requestConfig.setYieldDuration("INVALID");

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithInvalidCommsTimeout() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("uri-1,uri-2");
        requestConfig.setCommunicationsTimeout("INVALID");

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithProxyPortOfZero() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://updated");
        requestConfig.setProxyPort(0);

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithProxyPortLessThanMin() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://updated");
        requestConfig.setProxyPort(-2);

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithProxyPortGreaterThanMsx() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://updated");
        requestConfig.setProxyPort(65536);

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test
    public void testCreateWithProxyPortOfNegativeOne() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://updated");
        requestConfig.setProxyPort(-1);

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test
    public void testCreateWithValidProxyPort() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setTargetUris("http://updated");
        requestConfig.setProxyPort(100);

        service.create(flowId, group1.getIdentifier(), requestConfig);
    }

    @Test
    public void testUpdateSuccess() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final String updatedTargetUris = "http://updated";
        final String updatedCommsTimeout = "1 seconds";
        final String updatedYieldDuration = "1 seconds";
        final String updatedTransportProtocol = "HTTP";

        assertNotEquals(updatedTargetUris, remoteProcessGroup.getTargetUris());
        assertNotEquals(updatedCommsTimeout, remoteProcessGroup.getCommunicationsTimeout());
        assertNotEquals(updatedYieldDuration, remoteProcessGroup.getYieldDuration());
        assertNotEquals(updatedTransportProtocol, remoteProcessGroup.getTransportProtocol());

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setIdentifier(remoteProcessGroup.getIdentifier());
        requestConfig.setTargetUris(updatedTargetUris);
        requestConfig.setCommunicationsTimeout(updatedCommsTimeout);
        requestConfig.setYieldDuration(updatedYieldDuration);
        requestConfig.setTransportProtocol(updatedTransportProtocol);

        final FDRemoteProcessGroup updatedRemoteProcessGroup = service.update(flowId, requestConfig);
        assertNotNull(updatedRemoteProcessGroup);
        assertEquals(requestConfig.getIdentifier(), updatedRemoteProcessGroup.getComponentConfiguration().getIdentifier());
        assertEquals(updatedTargetUris, updatedRemoteProcessGroup.getComponentConfiguration().getTargetUris());
        assertEquals(updatedCommsTimeout, updatedRemoteProcessGroup.getComponentConfiguration().getCommunicationsTimeout());
        assertEquals(updatedYieldDuration, updatedRemoteProcessGroup.getComponentConfiguration().getYieldDuration());
        assertEquals(updatedTransportProtocol, updatedRemoteProcessGroup.getComponentConfiguration().getTransportProtocol());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithInvalidTransportProtocol() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setIdentifier(remoteProcessGroup.getIdentifier());
        requestConfig.setTransportProtocol("INVALID");

        service.update(flowId, requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithInvalidYieldDuration() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setIdentifier(remoteProcessGroup.getIdentifier());
        requestConfig.setYieldDuration("INVALID");

        service.update(flowId, requestConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithInvalidCommsTimeout() {
        final String flowId = flow.getFlowMetadata().getIdentifier();
        when(flowManager.getFlow(flowId)).thenReturn(Optional.of(flow));

        final VersionedRemoteProcessGroup requestConfig = new VersionedRemoteProcessGroup();
        requestConfig.setIdentifier(remoteProcessGroup.getIdentifier());
        requestConfig.setCommunicationsTimeout("INVALID");

        service.update(flowId, requestConfig);
    }

}
