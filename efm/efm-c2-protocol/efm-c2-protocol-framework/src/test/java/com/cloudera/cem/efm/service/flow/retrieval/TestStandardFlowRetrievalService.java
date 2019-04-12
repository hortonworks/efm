/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.service.flow.retrieval;

import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.service.flow.FlowTestUtils;
import com.cloudera.cem.efm.service.flow.client.C2NiFiRegistryClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestStandardFlowRetrievalService {

    private C2NiFiRegistryClient clientFactory;
    private NiFiRegistryClient client;
    private FlowSnapshotClient flowSnapshotClient;

    private FlowRetrievalService flowRetrievalService;

    @Before
    public void setup() {
        flowSnapshotClient = mock(FlowSnapshotClient.class);

        client = mock(NiFiRegistryClient.class);
        when(client.getFlowSnapshotClient()).thenReturn(flowSnapshotClient);

        clientFactory = mock(C2NiFiRegistryClient.class);
        when(clientFactory.getClient()).thenReturn(client);

        flowRetrievalService = new StandardFlowRetrievalService(clientFactory);
    }

    @Test
    public void testRetrieveLatestWhenVersionsExist() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), 2);
        when(flowSnapshotClient.getLatest(flowUri.getBucketId(), flowUri.getFlowId())).thenReturn(flowSnapshot);

        final VersionedFlowSnapshot returnedFlowSnapshot = flowRetrievalService.getLatestFlow(flowUri);
        assertNotNull(returnedFlowSnapshot);
        assertEquals(flowSnapshot.getSnapshotMetadata().getBucketIdentifier(), returnedFlowSnapshot.getSnapshotMetadata().getBucketIdentifier());
        assertEquals(flowSnapshot.getSnapshotMetadata().getFlowIdentifier(), returnedFlowSnapshot.getSnapshotMetadata().getFlowIdentifier());
        assertEquals(flowSnapshot.getSnapshotMetadata().getVersion(), returnedFlowSnapshot.getSnapshotMetadata().getVersion());
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveLatestWhenNoVersions() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(flowSnapshotClient.getLatest(flowUri.getBucketId(), flowUri.getFlowId())).thenThrow(new NiFiRegistryException("No Versions"));
        flowRetrievalService.getLatestFlow(flowUri);
    }


    @Test
    public void testRetrieveSpecificWhenVersionsExist() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final int version = 2;
        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), version);
        when(flowSnapshotClient.get(flowUri.getBucketId(), flowUri.getFlowId(), version)).thenReturn(flowSnapshot);

        final VersionedFlowSnapshot returnedFlowSnapshot = flowRetrievalService.getFlow(flowUri, version);
        assertNotNull(returnedFlowSnapshot);
        assertEquals(flowSnapshot.getSnapshotMetadata().getBucketIdentifier(), returnedFlowSnapshot.getSnapshotMetadata().getBucketIdentifier());
        assertEquals(flowSnapshot.getSnapshotMetadata().getFlowIdentifier(), returnedFlowSnapshot.getSnapshotMetadata().getFlowIdentifier());
        assertEquals(flowSnapshot.getSnapshotMetadata().getVersion(), returnedFlowSnapshot.getSnapshotMetadata().getVersion());
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveSpecificWhenNoVersions() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final int version = 2;
        when(flowSnapshotClient.get(flowUri.getBucketId(), flowUri.getFlowId(), version)).thenThrow(new NiFiRegistryException("No versions"));

        flowRetrievalService.getFlow(flowUri, version);
    }

    @Test
    public void testRetrieveVersionsListWhenVersionsExist() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final VersionedFlowSnapshot flowSnapshot1 = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), 1);
        final VersionedFlowSnapshot flowSnapshot2 = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), 2);

        when(flowSnapshotClient.getSnapshotMetadata(flowUri.getBucketId(), flowUri.getFlowId()))
                .thenReturn(Arrays.asList(
                        flowSnapshot1.getSnapshotMetadata(),
                        flowSnapshot2.getSnapshotMetadata()));

        final List<VersionedFlowSnapshotMetadata> returnedVersions = flowRetrievalService.getVersions(flowUri);
        assertNotNull(returnedVersions);
        assertEquals(2, returnedVersions.size());
    }

    @Test
    public void testRetrieveVersionsListWhenNoVersions() throws IOException, FlowRetrievalException, NiFiRegistryException {
        final FlowUri flowUri = new FlowUri("http://localhost:18080", UUID.randomUUID().toString(), UUID.randomUUID().toString());

        when(flowSnapshotClient.getSnapshotMetadata(flowUri.getBucketId(), flowUri.getFlowId())).thenReturn(Collections.emptyList());

        final List<VersionedFlowSnapshotMetadata> returnedVersions = flowRetrievalService.getVersions(flowUri);
        assertNotNull(returnedVersions);
        assertEquals(0, returnedVersions.size());
    }

}
