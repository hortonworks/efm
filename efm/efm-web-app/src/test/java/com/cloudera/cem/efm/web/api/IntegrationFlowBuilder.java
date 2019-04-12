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
package com.cloudera.cem.efm.web.api;

import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.component.FDRevision;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.ConnectableComponentType;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Collections;

/**
 * Builds a flow with a GenerateFlowFile processor connected to an RPG with an input port to send back to NiFi.
 *
 * The intent is to run this against the docker environment after creating an remote input port in the main NiFi instance.
 *
 * The flow that is built will be published to registry and then the agent should automatically pick it up.
 */
public class IntegrationFlowBuilder {

    static final String DEFAULT_AGENT_CLASS_NAME = "default";

    private final String flowDesignerBaseUrl;
    private final String nifiRpgUrl;
    private final String nifiInputPortId;

    private final Client client;
    private final WebTarget baseTarget;
    private final WebTarget designerTarget;
    private final WebTarget designerFlowsTarget;

    public IntegrationFlowBuilder(final String flowDesignerBaseUrl, final String nifiRpgUrl, final String nifiInputPortId) {
        this.flowDesignerBaseUrl = flowDesignerBaseUrl;
        this.nifiRpgUrl = nifiRpgUrl;
        this.nifiInputPortId = nifiInputPortId;
        this.client = ClientBuilder.newClient();
        this.baseTarget = client.target(this.flowDesignerBaseUrl + "/efm/api");
        this.designerTarget = baseTarget.path("designer");
        this.designerFlowsTarget = designerTarget.path("flows");
    }

    public void run() {
        System.out.println("DFM URL = " + this.flowDesignerBaseUrl);
        System.out.println("NiFi Input Port = " + this.nifiInputPortId);
        System.out.println("Building flow...");

        final ListContainer<FDFlowMetadata> flowMetadataListContainer = designerFlowsTarget.request()
                .get(new GenericType<ListContainer<FDFlowMetadata>>() {});

        final FDFlowMetadata agentClassFlow = flowMetadataListContainer.getElements().stream()
                .filter(flow -> flow.getAgentClass().equals(DEFAULT_AGENT_CLASS_NAME))
                .findFirst()
                .orElse(null);

        if (agentClassFlow == null) {
            throw new IllegalStateException("Unable to find flow for agent class '" + DEFAULT_AGENT_CLASS_NAME + "'");
        }

        final String flowId = agentClassFlow.getIdentifier();
        final String rootProcessGroupId = agentClassFlow.getRootProcessGroupIdentifier();

        final String requestClientId = "IT-test";

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        // Create a GenerateFlowFile processor...

        final Bundle requestBundle = new Bundle();
        requestBundle.setGroup("org.apache.nifi.minifi");
        requestBundle.setArtifact("minifi-system");
        requestBundle.setVersion("0.6.0");

        final VersionedProcessor generateFlowFileConfig = new VersionedProcessor();
        generateFlowFileConfig.setName("New Processor");
        generateFlowFileConfig.setType("org.apache.nifi.minifi.processors.GenerateFlowFile");
        generateFlowFileConfig.setBundle(requestBundle);
        generateFlowFileConfig.setSchedulingPeriod("5 seconds");

        final FDProcessor generateFlowFileProcessor = new FDProcessor();
        generateFlowFileProcessor.setComponentConfiguration(generateFlowFileConfig);
        generateFlowFileProcessor.setRevision(requestRevision);

        final FDProcessor createdGenerateFlowFileProcessor = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/processors")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(generateFlowFileProcessor, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        System.out.println("Created processor with id " + createdGenerateFlowFileProcessor.getComponentConfiguration().getIdentifier());

        // Create a remote process group...

        final VersionedRemoteProcessGroup remoteProcessGroupConfig = new VersionedRemoteProcessGroup();
        remoteProcessGroupConfig.setTargetUris(nifiRpgUrl);

        final FDRemoteProcessGroup remoteProcessGroup = new FDRemoteProcessGroup();
        remoteProcessGroup.setComponentConfiguration(remoteProcessGroupConfig);
        remoteProcessGroup.setRevision(requestRevision);

        final FDRemoteProcessGroup createdRemoteProcessGroup = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/remote-process-groups")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(remoteProcessGroup, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        System.out.println("Created Remote Process Group with id " + createdRemoteProcessGroup.getComponentConfiguration().getIdentifier());

        // Create a connection between the processor and RPG...

        final VersionedConnection connectionConfig = new VersionedConnection();
        connectionConfig.setName("My Connection");
        connectionConfig.setFlowFileExpiration("60 seconds");
        connectionConfig.setBackPressureDataSizeThreshold("10000 B");
        connectionConfig.setSelectedRelationships(Collections.singleton("success"));

        final ConnectableComponent connectionSource = new ConnectableComponent();
        connectionSource.setId(createdGenerateFlowFileProcessor.getComponentConfiguration().getIdentifier());
        connectionSource.setGroupId(rootProcessGroupId);
        connectionSource.setType(ConnectableComponentType.PROCESSOR);
        connectionConfig.setSource(connectionSource);

        final ConnectableComponent connectionDest = new ConnectableComponent();
        connectionDest.setId(nifiInputPortId);
        connectionDest.setGroupId(createdRemoteProcessGroup.getComponentConfiguration().getIdentifier());
        connectionDest.setType(ConnectableComponentType.REMOTE_INPUT_PORT);
        connectionConfig.setDestination(connectionDest);

        final FDConnection connection = new FDConnection();
        connection.setComponentConfiguration(connectionConfig);
        connection.setRevision(requestRevision);

        final FDConnection createdConnection = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/connections")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(connection, MediaType.APPLICATION_JSON_TYPE), FDConnection.class);

        System.out.println("Created connection with id " + createdConnection.getComponentConfiguration().getIdentifier());

        // Publish the flow...

        final FDFlowPublishMetadata publishMetadata = new FDFlowPublishMetadata();
        publishMetadata.setComments("Publishing integration test flow");

        final FDVersionInfo versionInfo = designerFlowsTarget
                .path("/{flowId}/publish")
                .resolveTemplate("flowId", flowId)
                .request()
                .post(Entity.entity(publishMetadata, MediaType.APPLICATION_JSON_TYPE), FDVersionInfo.class);

        System.out.println("Flow published!");
        System.out.println("Version = " + versionInfo.getRegistryVersion());
    }

    public static void main(String[] args) {
        String flowDesignerBaseUrl = "http://localhost:10080";
        if (args.length >= 1 && !StringUtils.isBlank(args[0])) {
            flowDesignerBaseUrl = args[0];
        }

        String nifiRpgUrl = "http://f8afbbbcb048:8080/nifi";
        if (args.length >= 2 && !StringUtils.isBlank(args[1])) {
            nifiRpgUrl = args[1];
        }

        String nifiInputPortId = "367a1348-0166-1000-bc47-b091e8d145da";
        if (args.length >= 3 && !StringUtils.isBlank(args[2])) {
            nifiInputPortId = args[2];
        }

        final IntegrationFlowBuilder flowBuilder = new IntegrationFlowBuilder(flowDesignerBaseUrl, nifiRpgUrl, nifiInputPortId);
        flowBuilder.run();
    }

}
