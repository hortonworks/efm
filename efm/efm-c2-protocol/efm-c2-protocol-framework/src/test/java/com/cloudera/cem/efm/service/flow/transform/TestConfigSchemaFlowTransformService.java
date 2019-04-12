/**
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
package com.cloudera.cem.efm.service.flow.transform;

import org.apache.nifi.minifi.commons.schema.ConfigSchema;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedPropertyDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TestConfigSchemaFlowTransformService {

    @Test
    public void testTransform() throws FlowTransformException {
        final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
        snapshotMetadata.setBucketIdentifier("bucket 1");
        snapshotMetadata.setFlowIdentifier("flow 2");
        snapshotMetadata.setVersion(1);
        snapshotMetadata.setComments("This is snapshot #1");

        final VersionedProcessGroup root = new VersionedProcessGroup();
        root.setIdentifier("root");
        root.setName("Root");

        final Map<String,String> props = new HashMap<>();
        props.put("Property 1", "Value 1");

        final Map<String, VersionedPropertyDescriptor> propDescriptors = new HashMap<>();

        final VersionedPropertyDescriptor propDescriptor1 = new VersionedPropertyDescriptor();
        propDescriptor1.setName("Property 1");
        propDescriptors.put(propDescriptor1.getName(), propDescriptor1);

        final VersionedProcessor proc1 = new VersionedProcessor();
        proc1.setIdentifier("p1");
        proc1.setName("Processor 1");
        proc1.setProperties(props);
        proc1.setPropertyDescriptors(propDescriptors);

        root.getProcessors().add(proc1);

        final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
        snapshot.setSnapshotMetadata(snapshotMetadata);
        snapshot.setFlowContents(root);

        // test the transform
        final FlowTransformService<ConfigSchema> transformService = new ConfigSchemaFlowTransformService();
        final ConfigSchema configSchema = transformService.transform(snapshot);

        Assert.assertNotNull(configSchema);
        Assert.assertNotNull(configSchema.getProcessGroupSchema());
        Assert.assertNotNull(configSchema.getProcessGroupSchema().getProcessors());
        Assert.assertEquals(1, configSchema.getProcessGroupSchema().getProcessors().size());
    }

}
