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

import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.service.extension.FDExtensionManager;
import com.cloudera.cem.efm.service.extension.FDExtensionManagers;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.PortType;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedPort;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.junit.Before;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Base class for service tests.
 */
public abstract class BaseFDServiceTest {

    protected FDFlowManager flowManager;
    protected FDExtensionManager extensionManager;
    protected FDExtensionManagers extensionManagers;

    protected FDFlow flow;
    protected VersionedProcessGroup flowContent;
    protected VersionedProcessGroup group1;
    protected VersionedProcessGroup group2;
    protected VersionedProcessGroup group3;

    protected VersionedProcessor group1Processor1;
    protected VersionedProcessor group2Processor1;
    protected VersionedProcessor group2Processor2;
    protected VersionedProcessor group2Processor3;
    protected VersionedProcessor group3Processor1;

    protected VersionedPort group1InputPort;
    protected VersionedPort group1OutputPort;

    protected VersionedPort group3InputPort;
    protected VersionedPort group3OutputPort;

    protected VersionedControllerService existingService;

    protected VersionedConnection connectionG2P2toG2P3;

    protected VersionedRemoteProcessGroup remoteProcessGroup;

    @Before
    public void setup() {
        flowManager = mock(FDFlowManager.class);
        extensionManager = mock(FDExtensionManager.class);
        extensionManagers = mock(FDExtensionManagers.class);

        final FDFlowMetadata flowMetadata = new FDFlowMetadata();
        flowMetadata.setIdentifier("1");
        flowMetadata.setRootProcessGroupIdentifier("1234-1234-1234-1234");
        flowMetadata.setAgentClass("Test Class");
        flowMetadata.setCreated(System.currentTimeMillis());
        flowMetadata.setUpdated(System.currentTimeMillis());

        flowContent = new VersionedProcessGroup();
        flowContent.setIdentifier("root-group");

        // group 1 is in the root group

        group1 = new VersionedProcessGroup();
        group1.setIdentifier("child-level-1");
        group1.setGroupIdentifier(flowContent.getIdentifier());
        flowContent.getProcessGroups().add(group1);

        // group 2 is in group 1

        group2 = new VersionedProcessGroup();
        group2.setIdentifier("child-level-2");
        group2.setGroupIdentifier(group1.getIdentifier());
        group1.getProcessGroups().add(group2);

        // group 3 is in group 2

        group3 = new VersionedProcessGroup();
        group3.setIdentifier("group3");
        group3.setGroupIdentifier(group2.getIdentifier());
        group2.getProcessGroups().add(group3);

        // Create the ports for group 1

        group1InputPort = new VersionedPort();
        group1InputPort.setIdentifier(UUID.randomUUID().toString());
        group1InputPort.setType(PortType.INPUT_PORT);
        group1InputPort.setGroupIdentifier(group1.getIdentifier());
        group1.getInputPorts().add(group1InputPort);

        group1OutputPort = new VersionedPort();
        group1OutputPort.setIdentifier(UUID.randomUUID().toString());
        group1OutputPort.setType(PortType.OUTPUT_PORT);
        group1OutputPort.setGroupIdentifier(group1.getIdentifier());
        group1.getOutputPorts().add(group1OutputPort);

        // Create the ports for group 3

        group3InputPort = new VersionedPort();
        group3InputPort.setIdentifier(UUID.randomUUID().toString());
        group3InputPort.setType(PortType.INPUT_PORT);
        group3InputPort.setGroupIdentifier(group3.getIdentifier());
        group3.getInputPorts().add(group3InputPort);

        group3OutputPort = new VersionedPort();
        group3OutputPort.setIdentifier(UUID.randomUUID().toString());
        group3OutputPort.setType(PortType.OUTPUT_PORT);
        group3OutputPort.setGroupIdentifier(group3.getIdentifier());
        group3.getOutputPorts().add(group3OutputPort);

        final Bundle bundle = new Bundle();
        bundle.setGroup("org.apache.nifi.minifi");
        bundle.setArtifact("minifi-system");
        bundle.setVersion("0.6.0");

        // create group 1 processor 1
        group1Processor1 = new VersionedProcessor();
        group1Processor1.setIdentifier(UUID.randomUUID().toString());
        group1Processor1.setName("GenerateFlowFile");
        group1Processor1.setType("org.apache.nifi.minifi.processors.GenerateFlowFile");
        group1Processor1.setBundle(bundle);

        group1.getProcessors().add(group1Processor1);
        group1Processor1.setGroupIdentifier(group1.getIdentifier());

        // create group 2 processor 1
        group2Processor1 = new VersionedProcessor();
        group2Processor1.setIdentifier(UUID.randomUUID().toString());
        group2Processor1.setName("GenerateFlowFile");
        group2Processor1.setType("org.apache.nifi.minifi.processors.GenerateFlowFile");
        group2Processor1.setBundle(bundle);

        group2.getProcessors().add(group2Processor1);
        group2Processor1.setGroupIdentifier(group2.getIdentifier());

        // create group 2 processor 2
        group2Processor2 = new VersionedProcessor();
        group2Processor2.setIdentifier(UUID.randomUUID().toString());
        group2Processor2.setName("UpdateAttribute");
        group2Processor2.setType("org.apache.nifi.minifi.processors.UpdateAttribute");
        group2Processor2.setBundle(bundle);

        group2.getProcessors().add(group2Processor2);
        group2Processor2.setGroupIdentifier(group2.getIdentifier());

        // create group 2 processor 3
        group2Processor3 = new VersionedProcessor();
        group2Processor3.setIdentifier(UUID.randomUUID().toString());
        group2Processor3.setName("UpdateAttribute");
        group2Processor3.setType("org.apache.nifi.minifi.processors.UpdateAttribute");
        group2Processor3.setBundle(bundle);

        group2.getProcessors().add(group2Processor3);
        group2Processor3.setGroupIdentifier(group2.getIdentifier());

        // create group 3 processor 1
        group3Processor1 = new VersionedProcessor();
        group3Processor1.setIdentifier(UUID.randomUUID().toString());
        group3Processor1.setName("UpdateAttribute");
        group3Processor1.setType("org.apache.nifi.minifi.processors.UpdateAttribute");
        group3Processor1.setBundle(bundle);

        group3.getProcessors().add(group3Processor1);
        group3Processor1.setGroupIdentifier(group3.getIdentifier());

        // create controller service and add it to group 2
        existingService = new VersionedControllerService();
        existingService.setIdentifier(UUID.randomUUID().toString());
        existingService.setName("SSL Context Service");
        existingService.setType("org.apache.nifi.minifi.controllers.SSLContextService");
        existingService.setBundle(bundle);

        group2.getControllerServices().add(existingService);
        existingService.setGroupIdentifier(group2.getIdentifier());

        // create connection from group2-processor2 to group2-processor3

        final ConnectableComponent source = ComponentUtils.createConnectableComponent(group2Processor2);
        final ConnectableComponent dest = ComponentUtils.createConnectableComponent(group2Processor3);

        connectionG2P2toG2P3 = new VersionedConnection();
        connectionG2P2toG2P3.setIdentifier(UUID.randomUUID().toString());
        connectionG2P2toG2P3.setSource(source);
        connectionG2P2toG2P3.setDestination(dest);

        group2.getConnections().add(connectionG2P2toG2P3);
        connectionG2P2toG2P3.setGroupIdentifier(group2.getIdentifier());

        // Create a remote process group at the root group
        remoteProcessGroup = new VersionedRemoteProcessGroup();
        remoteProcessGroup.setIdentifier(UUID.randomUUID().toString());
        remoteProcessGroup.setTargetUris("uri-1,uri-2");
        remoteProcessGroup.setTransportProtocol("RAW");
        remoteProcessGroup.setCommunicationsTimeout("30 seconds");
        remoteProcessGroup.setYieldDuration("10 seconds");
        remoteProcessGroup.setInputPorts(new HashSet<>());
        remoteProcessGroup.setOutputPorts(new HashSet<>());
        flowContent.getRemoteProcessGroups().add(remoteProcessGroup);

        // Create the flow
        final FDVersionInfo versionInfo = new FDVersionInfo();
        versionInfo.setRegistryUrl("http://localhost:18080");
        versionInfo.setRegistryBucketId("1234");
        versionInfo.setRegistryFlowId("1234");
        versionInfo.setRegistryVersion(1);

        flow = new FDFlow();
        flow.setFlowMetadata(flowMetadata);
        flow.setFlowContent(flowContent);
        flow.setVersionInfo(versionInfo);
        flow.setLocalFlowRevision(new BigInteger("1"));

        setupService();
    }

    /**
     * Will be called at the end of setup() so sub-classes can setup their respective service.
     */
    public abstract void setupService();

}
