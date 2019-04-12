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
import com.cloudera.cem.efm.properties.NifiRegistryProperties;
import com.cloudera.cem.efm.service.flow.client.C2NiFiRegistryClient;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

// NOTE: This test was meant to be run manually and not part of the build so we have the @Ignore annotation so
// it won't be execute by the failsafe plugin, temporarily remove the annotation to run manually.
@Ignore
public class StandardFlowRetrievalServiceIT {

    private static final String NIFI_REGISTRY_URL = "http://localhost:18080";

    private static final String CLASS_A = "Class A";
    private static final String CLASS_B = "Class B";

    private static StandardFlowRetrievalService flowRetrievalService;

    private static FlowUri classAUri;
    private static FlowUri classBUri;

    @BeforeClass
    public static void setup() throws IOException, NiFiRegistryException {
        final NifiRegistryProperties nrProperties = new NifiRegistryProperties();
        nrProperties.setUrl(NIFI_REGISTRY_URL);

        final C2NiFiRegistryClient c2NiFiRegistryClient = new C2NiFiRegistryClient(nrProperties, null);
        final NiFiRegistryClient niFiRegistryClient = c2NiFiRegistryClient.getClient();

        // Create a bucket to use for this test
        final Bucket bucket = new Bucket();
        bucket.setName("C2 Flow Retrieval IT - " + System.currentTimeMillis());

        // Set the id of the created bucket into the C2 properties before we create the FlowMapperFactory
        final Bucket createdBucket = niFiRegistryClient.getBucketClient().create(bucket);
        nrProperties.setBucketId(createdBucket.getIdentifier());

        flowRetrievalService = new StandardFlowRetrievalService(c2NiFiRegistryClient);

        // Create a flow for Class A
        final VersionedFlow classAFlow = new VersionedFlow();
        classAFlow.setBucketIdentifier(createdBucket.getIdentifier());
        classAFlow.setName(CLASS_A);

        final VersionedFlow createdClassAFlow = niFiRegistryClient.getFlowClient().create(classAFlow);
        classAUri = new FlowUri(NIFI_REGISTRY_URL, createdBucket.getIdentifier(), createdClassAFlow.getIdentifier());

        // Create a snapshot #1 for Class A
        final VersionedFlowSnapshot classASnapshot1 = createSnapshot(createdClassAFlow, 1);
        niFiRegistryClient.getFlowSnapshotClient().create(classASnapshot1);

        // Create a snapshot #2 for Class A
        final VersionedFlowSnapshot classASnapshot2 = createSnapshot(createdClassAFlow, 2);
        niFiRegistryClient.getFlowSnapshotClient().create(classASnapshot2);

        // Create a flow for Class B
        final VersionedFlow classBFlow = new VersionedFlow();
        classBFlow.setBucketIdentifier(createdBucket.getIdentifier());
        classBFlow.setName(CLASS_B);

        final VersionedFlow createdClassBFlow = niFiRegistryClient.getFlowClient().create(classBFlow);
        classBUri = new FlowUri(NIFI_REGISTRY_URL, createdBucket.getIdentifier(), createdClassBFlow.getIdentifier());

        // Don't create any versions for Class B so we can test what happens when no versions exist
    }

    @Test
    public void testRetrieveLatestWhenVersionsExist() throws IOException, FlowRetrievalException {
        final VersionedFlowSnapshot versionedFlowSnapshot = flowRetrievalService.getLatestFlow(classAUri);
        Assert.assertNotNull(versionedFlowSnapshot);
        Assert.assertEquals(2, versionedFlowSnapshot.getSnapshotMetadata().getVersion());
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveLatestWhenNoVersions() throws IOException, FlowRetrievalException {
        flowRetrievalService.getLatestFlow(classBUri);
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveLatestWhenNoFlowForClass() throws IOException, FlowRetrievalException {
        flowRetrievalService.getLatestFlow(new FlowUri(NIFI_REGISTRY_URL, "DOES-NOT-EXIST", "DOES-NOT-EXIST"));
    }

    @Test
    public void testRetrieveSpecificWhenVersionsExist() throws IOException, FlowRetrievalException {
        final VersionedFlowSnapshot versionedFlowSnapshot = flowRetrievalService.getFlow(classAUri, 1);
        Assert.assertNotNull(versionedFlowSnapshot);
        Assert.assertEquals(1, versionedFlowSnapshot.getSnapshotMetadata().getVersion());
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveSpecificWhenNoVersions() throws IOException, FlowRetrievalException {
        flowRetrievalService.getFlow(classBUri, 1);
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveSpecificWhenNoFlowForClass() throws IOException, FlowRetrievalException {
        flowRetrievalService.getFlow(new FlowUri(NIFI_REGISTRY_URL, "DOES-NOT-EXIST", "DOES-NOT-EXIST"), 1);
    }

    @Test
    public void testRetrieveVersionsListWhenVersionsExist() throws IOException, FlowRetrievalException {
        final List<VersionedFlowSnapshotMetadata> versions = flowRetrievalService.getVersions(classAUri);
        Assert.assertNotNull(versions);
        Assert.assertEquals(2, versions.size());
    }

    @Test
    public void testRetrieveVersionsListWhenNoVersions() throws IOException, FlowRetrievalException {
        final List<VersionedFlowSnapshotMetadata> versions = flowRetrievalService.getVersions(classBUri);
        Assert.assertNotNull(versions);
        Assert.assertEquals(0, versions.size());
    }

    @Test(expected = FlowRetrievalException.class)
    public void testRetrieveVersionsListWhenNoFlowForClass() throws IOException, FlowRetrievalException {
        flowRetrievalService.getVersions(new FlowUri(NIFI_REGISTRY_URL, "DOES-NOT-EXIST", "DOES-NOT-EXIST"));
    }

    private static VersionedFlowSnapshot createSnapshot(final VersionedFlow versionedFlow, int num) {
        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setBucketIdentifier(versionedFlow.getBucketIdentifier());
        snapshotMetadata.setFlowIdentifier(versionedFlow.getIdentifier());
        snapshotMetadata.setVersion(num);
        snapshotMetadata.setComments("This is snapshot #" + num);

        final VersionedProcessGroup rootProcessGroup = new VersionedProcessGroup();
        rootProcessGroup.setIdentifier("root-pg");
        rootProcessGroup.setName("Root Process Group");

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(rootProcessGroup);
        return snapshot;
    }


}
