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
package com.cloudera.cem.efm.web.api.unsecure;

import com.google.common.collect.Sets;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.component.FDRevision;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.extension.Relationship;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlow;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlowContent;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.model.flow.FDVersionInfoResult;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.web.api.FDFlowResource;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ConnectableComponent;
import org.apache.nifi.registry.flow.ConnectableComponentType;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.jdbc.Sql;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:db/clearDB.sql"})
public class UnsecureFlowDesignerIT extends UnsecureITBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsecureFlowDesignerIT.class);

    private static final String DEV_MANIFEST_DIR = "dev/manifests";
    private static final String DEFAULT_MANIFEST = DEV_MANIFEST_DIR + "/default-manifest.json";

    private static final String TEST_AGENT_CLASS = "TEST AGENT CLASS";

    private static final Duration MAX_AGENT_CLASS_MONITOR_WAIT_TIME = Duration.ofSeconds(5);

    private WebTarget baseTarget;
    private WebTarget designerTarget;
    private WebTarget designerFlowsTarget;

    private Bundle systemBundle;

    @Before
    public void setup() {
        baseTarget = client.target(createURL(""));
        designerTarget = baseTarget.path("designer");
        designerFlowsTarget = designerTarget.path("flows");

        systemBundle = new Bundle();
        systemBundle.setGroup("org.apache.nifi.minifi");
        systemBundle.setArtifact("minifi-system");
        systemBundle.setVersion("0.6.0");
    }

    // TODO, this test required on FlowDesignerIT.sql, which is not compatible with postgres or mysql
    // so for now just skip this test when running with database integration profiles.
    // In the future, we could re-write the tests data to be populated in a db-agnostic or db-aware manner.
    @IfProfileValue(name="current.database.is.h2", value="true")
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:db/clearDB.sql", "classpath:db/FlowDesignerIT.sql"})
    public void testGetFlowDesignerFlows() {
        final String expectedFlowId = "1";
        final String expectedAgentClass = "Class A";
        final String expectedRootGroupId = "root-group-1";

        // Request the list of flows as flow metadata objects
        final ListContainer<FDFlowMetadata> flowMetadataListContainer = designerFlowsTarget.request()
                .get(new GenericType<ListContainer<FDFlowMetadata>>() {});

        assertNotNull(flowMetadataListContainer);
        assertNotNull(flowMetadataListContainer.getElements());
        assertTrue(flowMetadataListContainer.getElements().size() >= 2);

        // Request the list of flows as flow summary objects
        final ListContainer<FDFlowSummary> flowSummaryListContainer = designerFlowsTarget
                .path("/summaries")
                .request()
                .get(new GenericType<ListContainer<FDFlowSummary>>() {});

        assertNotNull(flowSummaryListContainer);
        assertNotNull(flowSummaryListContainer.getElements());
        assertTrue(flowSummaryListContainer.getElements().size() >= 2);

        final FDFlowSummary flowSummary = flowSummaryListContainer.getElements().get(0);
        assertNotNull(flowSummary);
        assertNotNull(flowSummary.getIdentifier());
        assertNotNull(flowSummary.getAgentClass());
        assertNotNull(flowSummary.getRootProcessGroupIdentifier());
        assertNotNull(flowSummary.getCreated());
        assertNotNull(flowSummary.getLastModified());
        assertNotNull(flowSummary.getLastModifiedBy());
        assertNull(flowSummary.getVersionInfo());

        // Verify the "Class A" flow exists with the correct values
        final FDFlowMetadata classAFlowMetadata = flowMetadataListContainer.getElements().stream()
                .filter(flow -> flow.getIdentifier().equals(expectedFlowId))
                .findFirst().orElse(null);
        assertNotNull(classAFlowMetadata);
        assertEquals(expectedFlowId, classAFlowMetadata.getIdentifier());
        assertEquals(expectedAgentClass, classAFlowMetadata.getAgentClass());
        assertEquals(expectedRootGroupId, classAFlowMetadata.getRootProcessGroupIdentifier());

        // Request the full flow for "Class A" using the id from the metadata above
        final FDFlow classAFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", classAFlowMetadata.getIdentifier())
                .request()
                .get(FDFlow.class);

        assertNotNull(classAFlow);
        assertNotNull(classAFlow.getFlowMetadata());
        assertNotNull(classAFlow.getFlowContent());
        assertNull(classAFlow.getVersionInfo());

        assertEquals(expectedFlowId, classAFlow.getFlowMetadata().getIdentifier());
        assertEquals(expectedAgentClass, classAFlow.getFlowMetadata().getAgentClass());
        assertEquals(expectedRootGroupId, classAFlow.getFlowMetadata().getRootProcessGroupIdentifier());
        assertEquals(expectedRootGroupId, classAFlow.getFlowContent().getIdentifier());
    }

    @Test
    public void testFlowTypes() throws InterruptedException, IOException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDComponentTypes processorTypes = designerFlowsTarget
                .path("{flowId}/types/processors")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDComponentTypes.class);

        assertNotNull(processorTypes);
        assertEquals(flowId, processorTypes.getFlowId());
        assertNotNull(processorTypes.getComponentTypes());
        assertEquals(29, processorTypes.getComponentTypes().size());

        final FDComponentTypes controllerServiceTypes = designerFlowsTarget
                .path("{flowId}/types/controller-services")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDComponentTypes.class);

        assertNotNull(controllerServiceTypes);
        assertEquals(flowId, controllerServiceTypes.getFlowId());
        assertNotNull(controllerServiceTypes.getComponentTypes());
        assertEquals(4, controllerServiceTypes.getComponentTypes().size());
    }

    @Test
    public void testELSpecification() throws InterruptedException, IOException {
        final String flowId = createAgentManifestClassAndFlow();

        final ELSpecification specification = designerFlowsTarget
                .path("{flowId}/expression-language-spec")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(ELSpecification.class);

        assertNotNull(specification);
        assertNotNull(specification.getOperations());
        assertTrue(specification.getOperations().size() > 1);
    }

    @Test
    public void testProcessorCRUDOperations() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);
        assertNotNull(testClassFlow.getFlowMetadata());
        assertNotNull(testClassFlow.getFlowContent());

        final FDVersionInfo versionInfo = testClassFlow.getVersionInfo();
        assertNull(versionInfo);

        assertNotNull(testClassFlow.getLocalFlowRevision());
        assertEquals(new BigInteger("0"), testClassFlow.getLocalFlowRevision());

        // Verify no processors
        assertEquals(0, testClassFlow.getFlowContent().getProcessors().size());

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        final Bundle requestBundle = new Bundle();
        requestBundle.setGroup("org.apache.nifi.minifi");
        requestBundle.setArtifact("minifi-system");
        requestBundle.setVersion("0.6.0");

        final VersionedProcessor requestComponent = new VersionedProcessor();
        requestComponent.setName("New Processor");
        requestComponent.setType("org.apache.nifi.minifi.processors.GenerateFlowFile");
        requestComponent.setBundle(requestBundle);

        final String requestClientId = "IT-test";

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        final FDProcessor requestProcessor = new FDProcessor();
        requestProcessor.setComponentConfiguration(requestComponent);
        requestProcessor.setRevision(requestRevision);

        // Perform the create and verify response

        final FDProcessor createdProcessor = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/processors")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(requestProcessor, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        assertNotNull(createdProcessor);
        assertNotNull(createdProcessor.getUri());

        assertNotNull(createdProcessor.getRevision());
        assertEquals(Long.valueOf(1), createdProcessor.getRevision().getVersion());
        assertEquals(requestClientId, createdProcessor.getRevision().getClientId());
        assertEquals("anonymous", createdProcessor.getRevision().getLastModifier());

        assertNotNull(createdProcessor.getComponentConfiguration());
        assertNotNull(createdProcessor.getComponentConfiguration().getIdentifier());

        assertNotNull(createdProcessor.getComponentDefinition());
        assertNotNull(createdProcessor.getComponentDefinition().getTypeDescription());

        // Verify the flow now has the processor

        final FDFlow testClassFlowUpdated = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowUpdated);
        assertNotNull(testClassFlowUpdated.getFlowContent());
        assertEquals(1, testClassFlowUpdated.getFlowContent().getProcessors().size());

        assertNull(testClassFlow.getVersionInfo());
        assertNotNull(testClassFlow.getLocalFlowRevision());
        assertEquals(new BigInteger("1"), testClassFlowUpdated.getLocalFlowRevision());

        final VersionedProcessor flowProcessor = testClassFlowUpdated.getFlowContent().getProcessors().stream().findFirst().get();
        assertNotNull(flowProcessor);
        assertEquals(requestProcessor.getComponentConfiguration().getName(), flowProcessor.getName());
        assertEquals(requestProcessor.getComponentConfiguration().getType(), flowProcessor.getType());
        assertEquals(requestProcessor.getComponentConfiguration().getBundle(), flowProcessor.getBundle());

        // Verify we can retrieve the processor by id

        final FDProcessor retrievedProcessor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", flowProcessor.getIdentifier())
                .request()
                .get(FDProcessor.class);

        assertNotNull(retrievedProcessor);
        assertNotNull(retrievedProcessor.getComponentConfiguration());
        assertEquals(flowProcessor.getIdentifier(), retrievedProcessor.getComponentConfiguration().getIdentifier());
        assertNotNull(retrievedProcessor.getComponentDefinition());
        assertNotNull(retrievedProcessor.getRevision());

        // Update the processor properties

        final String propertyName = "File Size";
        final String originalPropertyValue = "1 kB";

        assertNotNull(retrievedProcessor.getComponentConfiguration().getProperties());
        assertEquals(4, retrievedProcessor.getComponentConfiguration().getProperties().size());
        assertEquals(originalPropertyValue, retrievedProcessor.getComponentConfiguration().getProperties().get(propertyName));

        // Clear the properties and then add just the one property we want to update
        final String updatedPropertyValue = "10 kB";
        retrievedProcessor.getComponentConfiguration().getProperties().clear();
        retrievedProcessor.getComponentConfiguration().getProperties().put(propertyName, updatedPropertyValue);

        // Update processor and verify response

        final FDProcessor updatedProcessor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", flowProcessor.getIdentifier())
                .request()
                .put(Entity.entity(retrievedProcessor, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        assertNotNull(updatedProcessor);
        assertNotNull(updatedProcessor.getComponentConfiguration());

        // Make sure that even though we only submitted 1 property value above, we still have 4 here
        final Map<String,String> updatedProcessorProperties = updatedProcessor.getComponentConfiguration().getProperties();
        assertEquals(4, updatedProcessorProperties.size());
        assertEquals(updatedPropertyValue, updatedProcessorProperties.get(propertyName));

        // Verify we have a revision and that the updated version is greater than the previous
        final FDRevision updatedRevision = updatedProcessor.getRevision();
        assertNotNull(updatedRevision);
        assertTrue(updatedRevision.getVersion().longValue() > retrievedProcessor.getRevision().getVersion().longValue());

        // Delete processor and verify response

        final FDProcessor deletedProcessor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", flowProcessor.getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, updatedRevision.getClientId())
                .queryParam(FDFlowResource.VERSION, updatedRevision.getVersion())
                .request()
                .delete(FDProcessor.class);

        assertNotNull(deletedProcessor);
        assertNotNull(deletedProcessor.getComponentConfiguration());
        assertEquals(flowProcessor.getIdentifier(), deletedProcessor.getComponentConfiguration().getIdentifier());

        // Retrieve the flow again and make sure the processor is gone

        final FDFlow testClassFlowAfterDeletedProcessor = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowAfterDeletedProcessor);
        assertNotNull(testClassFlowAfterDeletedProcessor.getFlowContent());
        assertEquals(0, testClassFlowAfterDeletedProcessor.getFlowContent().getProcessors().size());

        // Retrieve flow events and verify 4 events exist - (create flow, create proc, modify proc, delete proc)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(4, flowEvents.getElements().size());

        BigInteger prevEventRevision = new BigInteger("-1");
        for (FDFlowEvent flowEvent : flowEvents.getElements()) {
            assertTrue(flowEvent.getFlowRevision().compareTo(prevEventRevision) > 0);
            assertNotNull(flowEvent.getRegistryUrl());
            assertNotNull(flowEvent.getRegistryBucketId());
            assertNotNull(flowEvent.getRegistryFlowId());
            assertNotNull(flowEvent.getRegistryVersion());
            prevEventRevision = flowEvent.getFlowRevision();
        }
    }

    @Test
    public void testControllerServiceCRUDOperations() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);
        assertNotNull(testClassFlow.getFlowMetadata());
        assertNotNull(testClassFlow.getFlowContent());

        assertNull(testClassFlow.getVersionInfo());
        assertNotNull(testClassFlow.getLocalFlowRevision());
        assertEquals(new BigInteger("0"), testClassFlow.getLocalFlowRevision());

        // Verify no controller services
        assertEquals(0, testClassFlow.getFlowContent().getControllerServices().size());

        final String requestClientId = "IT-test";
        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        final String csName = "My SSL Context Service";
        final String csType = "org.apache.nifi.minifi.controllers.SSLContextService";

        final FDControllerService createdService = createService(requestClientId, flowId,
                rootProcessGroupId, csName, csType, systemBundle);

        assertNotNull(createdService);
        assertNotNull(createdService.getUri());

        assertNotNull(createdService.getRevision());
        assertEquals(Long.valueOf(1), createdService.getRevision().getVersion());
        assertEquals(requestClientId, createdService.getRevision().getClientId());
        assertEquals("anonymous", createdService.getRevision().getLastModifier());

        assertNotNull(createdService.getComponentConfiguration());
        assertNotNull(createdService.getComponentConfiguration().getIdentifier());

        assertNotNull(createdService.getComponentDefinition());

        // Verify the flow now has the service

        final FDFlow testClassFlowUpdated = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(1, testClassFlowUpdated.getFlowContent().getControllerServices().size());

        final VersionedControllerService flowService = testClassFlowUpdated.getFlowContent().getControllerServices().stream().findFirst().get();
        assertNotNull(flowService);
        assertEquals(csName, flowService.getName());
        assertEquals(csType, flowService.getType());
        assertEquals(systemBundle, flowService.getBundle());

        // Verify we can retrieve the service by id

        final FDControllerService retrievedService = designerFlowsTarget
                .path("/{flowId}/controller-services/{controllerServiceId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("controllerServiceId", flowService.getIdentifier())
                .request()
                .get(FDControllerService.class);

        assertNotNull(retrievedService);
        assertNotNull(retrievedService.getComponentConfiguration());
        assertEquals(flowService.getIdentifier(), retrievedService.getComponentConfiguration().getIdentifier());
        assertNotNull(retrievedService.getComponentDefinition());
        assertNotNull(retrievedService.getRevision());

        // Verify getting the list of services for the process group

        final ListContainer<FDControllerService> retrievedServices = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/controller-services")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .get(new GenericType<ListContainer<FDControllerService>>() {});

        assertNotNull(retrievedServices);
        assertNotNull(retrievedServices.getElements());
        assertEquals(1, retrievedServices.getElements().size());
        assertNotNull(retrievedServices.getElements().get(0).getRevision());

        // Update the service properties

        assertNotNull(retrievedService.getComponentConfiguration().getProperties());
        assertEquals(4, retrievedService.getComponentConfiguration().getProperties().size());

        final String propertyName = "Client Certificate";
        final String propertyValue = "/tmp/test-user.jks";
        retrievedService.getComponentConfiguration().getProperties().put(propertyName, propertyValue);

        // Update the service and verify the response

        final FDControllerService updatedService = designerFlowsTarget
                .path("/{flowId}/controller-services/{controllerServiceId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("controllerServiceId", flowService.getIdentifier())
                .request()
                .put(Entity.entity(retrievedService, MediaType.APPLICATION_JSON_TYPE), FDControllerService.class);

        assertNotNull(updatedService);
        assertNotNull(updatedService.getComponentConfiguration());

        final Map<String,String> updatedServiceProperties = updatedService.getComponentConfiguration().getProperties();
        assertEquals(4, updatedServiceProperties.size());
        assertEquals(propertyValue, updatedServiceProperties.get(propertyName));

        // Verify we have a revision and that the updated version is greater than the previous
        final FDRevision updatedRevision = updatedService.getRevision();
        assertNotNull(updatedRevision);
        assertTrue(updatedRevision.getVersion().longValue() > retrievedService.getRevision().getVersion().longValue());

        // Delete service and verify response

        final FDControllerService deletedService = designerFlowsTarget
                .path("/{flowId}/controller-services/{controllerServiceId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("controllerServiceId", flowService.getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, updatedRevision.getClientId())
                .queryParam(FDFlowResource.VERSION, updatedRevision.getVersion())
                .request()
                .delete(FDControllerService.class);

        assertNotNull(deletedService);
        assertNotNull(deletedService.getComponentConfiguration());
        assertEquals(flowService.getIdentifier(), deletedService.getComponentConfiguration().getIdentifier());

        // Retrieve the flow again and make sure the processor is gone

        final FDFlow testClassFlowAfterDeletedService = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowAfterDeletedService);
        assertNotNull(testClassFlowAfterDeletedService.getFlowContent());
        assertEquals(0, testClassFlowAfterDeletedService.getFlowContent().getControllerServices().size());

        // Retrieve flow events and verify 4 events exist - (create flow, create proc, modify proc, delete proc)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(4, flowEvents.getElements().size());
    }

    @Test
    public void testConnectionCRUDOperations() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);

        // Verify no connections
        assertEquals(0, testClassFlow.getFlowContent().getConnections().size());

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        // Create two processor so we can connect them

        final String requestClientId = "IT-test";

        final FDProcessor createdProcessor1 = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 1" );
        assertNotNull(createdProcessor1);

        final FDProcessor createdProcessor2 = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 2" );
        assertNotNull(createdProcessor2);

        final FDProcessor createdProcessor3 = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 3" );
        assertNotNull(createdProcessor3);

        // Create a connection

        final String flowFileExpiration = "10 seconds";
        final String backPressureDataSizeThreshold = "1000 B";

        final VersionedConnection requestComponent = new VersionedConnection();
        requestComponent.setName("My Connection");
        requestComponent.setFlowFileExpiration(flowFileExpiration);
        requestComponent.setBackPressureDataSizeThreshold(backPressureDataSizeThreshold);
        requestComponent.setSelectedRelationships(Collections.singleton("success"));

        final ConnectableComponent source = new ConnectableComponent();
        source.setId(createdProcessor1.getComponentConfiguration().getIdentifier());
        source.setGroupId(rootProcessGroupId);
        source.setType(ConnectableComponentType.PROCESSOR);
        requestComponent.setSource(source);

        final ConnectableComponent dest = new ConnectableComponent();
        dest.setId(createdProcessor2.getComponentConfiguration().getIdentifier());
        dest.setGroupId(rootProcessGroupId);
        dest.setType(ConnectableComponentType.PROCESSOR);
        requestComponent.setDestination(dest);

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        final FDConnection requestConnection = new FDConnection();
        requestConnection.setComponentConfiguration(requestComponent);
        requestConnection.setRevision(requestRevision);

        // Perform the create and verify response

        final FDConnection createdConnection = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/connections")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(requestConnection, MediaType.APPLICATION_JSON_TYPE), FDConnection.class);

        assertNotNull(createdConnection);
        assertNotNull(createdConnection.getUri());

        assertNotNull(createdConnection.getRevision());
        assertEquals(Long.valueOf(1), createdConnection.getRevision().getVersion());
        assertEquals(requestClientId, createdConnection.getRevision().getClientId());
        assertEquals("anonymous", createdConnection.getRevision().getLastModifier());

        assertNotNull(createdConnection.getComponentConfiguration());
        assertNotNull(createdConnection.getComponentConfiguration().getIdentifier());

        // Verify the flow now has the connection

        final FDFlow testClassFlowUpdated = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(1, testClassFlowUpdated.getFlowContent().getConnections().size());

        final VersionedConnection flowConnection = testClassFlowUpdated.getFlowContent().getConnections().stream().findFirst().get();
        assertNotNull(flowConnection);
        assertEquals(requestConnection.getComponentConfiguration().getName(), flowConnection.getName());

        // Verify we can retrieve by id

        final FDConnection retrievedConnection = designerFlowsTarget
                .path("/{flowId}/connections/{connectionId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("connectionId", flowConnection.getIdentifier())
                .request()
                .get(FDConnection.class);

        assertNotNull(retrievedConnection);
        assertNotNull(retrievedConnection.getComponentConfiguration());
        assertEquals(flowConnection.getIdentifier(), retrievedConnection.getComponentConfiguration().getIdentifier());
        assertNotNull(retrievedConnection.getRevision());

        final ConnectableComponent retrievedConnectionSource = retrievedConnection.getComponentConfiguration().getSource();
        assertNotNull(retrievedConnectionSource);
        assertEquals(createdProcessor1.getComponentConfiguration().getIdentifier(), retrievedConnectionSource.getId());
        assertEquals(rootProcessGroupId, retrievedConnectionSource.getGroupId());
        assertEquals(ConnectableComponentType.PROCESSOR, retrievedConnectionSource.getType());

        final ConnectableComponent retrievedConnectionDest = retrievedConnection.getComponentConfiguration().getDestination();
        assertNotNull(retrievedConnectionDest);
        assertEquals(createdProcessor2.getComponentConfiguration().getIdentifier(), retrievedConnectionDest.getId());
        assertEquals(rootProcessGroupId, retrievedConnectionDest.getGroupId());
        assertEquals(ConnectableComponentType.PROCESSOR, retrievedConnectionDest.getType());

        // Update the configuration

        assertEquals(flowFileExpiration,retrievedConnection.getComponentConfiguration().getFlowFileExpiration());
        assertEquals(backPressureDataSizeThreshold, retrievedConnection.getComponentConfiguration().getBackPressureDataSizeThreshold());

        retrievedConnection.getComponentConfiguration().setFlowFileExpiration("2 seconds");
        retrievedConnection.getComponentConfiguration().setBackPressureDataSizeThreshold("1 MB");

        final ConnectableComponent newDest = new ConnectableComponent();
        newDest.setId(createdProcessor3.getComponentConfiguration().getIdentifier());
        newDest.setGroupId(rootProcessGroupId);
        newDest.setType(ConnectableComponentType.PROCESSOR);
        retrievedConnection.getComponentConfiguration().setDestination(newDest);

        // Update and verify the response

        final FDConnection updatedConnection = designerFlowsTarget
                .path("/{flowId}/connections/{connectionId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("connectionId", flowConnection.getIdentifier())
                .request()
                .put(Entity.entity(retrievedConnection, MediaType.APPLICATION_JSON_TYPE), FDConnection.class);

        assertNotNull(updatedConnection);
        assertNotNull(updatedConnection.getComponentConfiguration());

        assertEquals(retrievedConnection.getComponentConfiguration().getFlowFileExpiration(),
                updatedConnection.getComponentConfiguration().getFlowFileExpiration());
        assertEquals(retrievedConnection.getComponentConfiguration().getBackPressureDataSizeThreshold(),
                updatedConnection.getComponentConfiguration().getBackPressureDataSizeThreshold());

        final ConnectableComponent updatedConnectionDest = updatedConnection.getComponentConfiguration().getDestination();
        assertNotNull(updatedConnectionDest);
        assertEquals(createdProcessor3.getComponentConfiguration().getIdentifier(), updatedConnectionDest.getId());
        assertEquals(rootProcessGroupId, updatedConnectionDest.getGroupId());
        assertEquals(ConnectableComponentType.PROCESSOR, updatedConnectionDest.getType());


        // Verify we have a revision and that the updated version is greater than the previous
        final FDRevision updatedRevision = updatedConnection.getRevision();
        assertNotNull(updatedRevision);
        assertTrue(updatedRevision.getVersion().longValue() > retrievedConnection.getRevision().getVersion().longValue());

        // Delete and verify response

        final FDConnection deletedConnection = designerFlowsTarget
                .path("/{flowId}/connections/{connectionId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("connectionId", flowConnection.getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, updatedRevision.getClientId())
                .queryParam(FDFlowResource.VERSION, updatedRevision.getVersion())
                .request()
                .delete(FDConnection.class);

        assertNotNull(deletedConnection);
        assertNotNull(deletedConnection.getComponentConfiguration());
        assertEquals(flowConnection.getIdentifier(), deletedConnection.getComponentConfiguration().getIdentifier());

        // Retrieve the flow again and make sure the processor is gone

        final FDFlow testClassFlowAfterDelete = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowAfterDelete);
        assertNotNull(testClassFlowAfterDelete.getFlowContent());
        assertEquals(0, testClassFlowAfterDelete.getFlowContent().getConnections().size());

        // Retrieve flow events and verify 4 events exist - (create flow, create proc1, create proc2, create proc3, create conn, modify conn, delete conn)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(7, flowEvents.getElements().size());
    }

    @Test
    public void testFunnelCRUDOperations() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);
        assertNotNull(testClassFlow.getFlowMetadata());
        assertNotNull(testClassFlow.getFlowContent());

        // Verify no funnels
        assertEquals(0, testClassFlow.getFlowContent().getFunnels().size());

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        final String requestClientId = "IT-test";

        final VersionedFunnel requestComponentConfig = new VersionedFunnel();

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        final FDFunnel requestComponent = new FDFunnel();
        requestComponent.setComponentConfiguration(requestComponentConfig);
        requestComponent.setRevision(requestRevision);

        // Perform the create and verify response

        final FDFunnel createdFunnel = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/funnels")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(requestComponent, MediaType.APPLICATION_JSON_TYPE), FDFunnel.class);

        assertNotNull(createdFunnel);
        assertNotNull(createdFunnel.getUri());

        assertNotNull(createdFunnel.getRevision());
        assertEquals(Long.valueOf(1), createdFunnel.getRevision().getVersion());
        assertEquals(requestClientId, createdFunnel.getRevision().getClientId());
        assertEquals("anonymous", createdFunnel.getRevision().getLastModifier());

        assertNotNull(createdFunnel.getComponentConfiguration());
        assertNotNull(createdFunnel.getComponentConfiguration().getIdentifier());

        // Verify the flow now has the funnel

        final FDFlow testClassFlowUpdated = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(1, testClassFlowUpdated.getFlowContent().getFunnels().size());

        final VersionedFunnel flowFunnel = testClassFlowUpdated.getFlowContent().getFunnels().stream().findFirst().get();
        assertNotNull(flowFunnel);

        // Verify we can retrieve by id

        final FDFunnel retrievedFunnel = designerFlowsTarget
                .path("/{flowId}/funnels/{funnelId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("funnelId", flowFunnel.getIdentifier())
                .request()
                .get(FDFunnel.class);

        assertNotNull(retrievedFunnel);
        assertNotNull(retrievedFunnel.getComponentConfiguration());
        assertEquals(flowFunnel.getIdentifier(), retrievedFunnel.getComponentConfiguration().getIdentifier());
        assertNotNull(retrievedFunnel.getRevision());

        // Update the funnel and verify the response

        retrievedFunnel.getComponentConfiguration().setName("NEW NAME");

        final FDFunnel updatedFunnel = designerFlowsTarget
                .path("/{flowId}/funnels/{funnelId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("funnelId", flowFunnel.getIdentifier())
                .request()
                .put(Entity.entity(retrievedFunnel, MediaType.APPLICATION_JSON_TYPE), FDFunnel.class);

        assertNotNull(updatedFunnel);
        assertNotNull(updatedFunnel.getComponentConfiguration());
        assertEquals(retrievedFunnel.getComponentConfiguration().getName(), updatedFunnel.getComponentConfiguration().getName());

        // Verify we have a revision and that the updated version is greater than the previous
        final FDRevision updatedRevision = updatedFunnel.getRevision();
        assertNotNull(updatedRevision);
        assertTrue(updatedRevision.getVersion().longValue() > retrievedFunnel.getRevision().getVersion().longValue());

        // Delete and verify response

        final FDFunnel deletedFunnel = designerFlowsTarget
                .path("/{flowId}/funnels/{funnelId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("funnelId", flowFunnel.getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, updatedRevision.getClientId())
                .queryParam(FDFlowResource.VERSION, updatedRevision.getVersion())
                .request()
                .delete(FDFunnel.class);

        assertNotNull(deletedFunnel);
        assertNotNull(deletedFunnel.getComponentConfiguration());
        assertEquals(flowFunnel.getIdentifier(), deletedFunnel.getComponentConfiguration().getIdentifier());

        // Retrieve the flow again and make sure the processor is gone

        final FDFlow testClassFlowAfterDelete = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowAfterDelete);
        assertNotNull(testClassFlowAfterDelete.getFlowContent());
        assertEquals(0, testClassFlowAfterDelete.getFlowContent().getFunnels().size());

        // Retrieve flow events and verify 4 events exist - (create flow, create funnel, modify funnel, delete funnel)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(4, flowEvents.getElements().size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testRemoteProcessGroupCRUDOperations() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);
        assertNotNull(testClassFlow.getFlowMetadata());
        assertNotNull(testClassFlow.getFlowContent());

        // Verify no funnels
        assertEquals(0, testClassFlow.getFlowContent().getRemoteProcessGroups().size());

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        final String requestClientId = "IT-test";

        final VersionedRemoteProcessGroup requestComponentConfig = new VersionedRemoteProcessGroup();
        requestComponentConfig.setTargetUris("http://host1,http://host2");

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        final FDRemoteProcessGroup requestComponent = new FDRemoteProcessGroup();
        requestComponent.setComponentConfiguration(requestComponentConfig);
        requestComponent.setRevision(requestRevision);

        // Perform the create and verify response

        final FDRemoteProcessGroup createdRpg = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/remote-process-groups")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(requestComponent, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(createdRpg);
        assertNotNull(createdRpg.getUri());

        assertNotNull(createdRpg.getRevision());
        assertEquals(Long.valueOf(1), createdRpg.getRevision().getVersion());
        assertEquals(requestClientId, createdRpg.getRevision().getClientId());
        assertEquals("anonymous", createdRpg.getRevision().getLastModifier());

        assertNotNull(createdRpg.getComponentConfiguration());
        assertNotNull(createdRpg.getComponentConfiguration().getIdentifier());
        assertEquals(requestComponentConfig.getTargetUris(), createdRpg.getComponentConfiguration().getTargetUris());
        assertEquals("http://host1", createdRpg.getComponentConfiguration().getTargetUri());

        // Verify the flow now has the component

        final FDFlow testClassFlowUpdated = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(1, testClassFlowUpdated.getFlowContent().getRemoteProcessGroups().size());

        final VersionedRemoteProcessGroup flowRpg = testClassFlowUpdated.getFlowContent().getRemoteProcessGroups().stream().findFirst().get();
        assertNotNull(flowRpg);

        // Verify we can retrieve by id

        final FDRemoteProcessGroup retrievedRpg = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", flowRpg.getIdentifier())
                .request()
                .get(FDRemoteProcessGroup.class);

        assertNotNull(retrievedRpg);
        assertNotNull(retrievedRpg.getComponentConfiguration());
        assertEquals(flowRpg.getIdentifier(), retrievedRpg.getComponentConfiguration().getIdentifier());
        assertNotNull(retrievedRpg.getRevision());
        assertNull(retrievedRpg.getComponentConfiguration().getProxyPort());

        // Update and verify the response

        retrievedRpg.getComponentConfiguration().setTargetUris("http://host3,http://host2,http://host1");
        retrievedRpg.getComponentConfiguration().setProxyPort(80);

        final FDRemoteProcessGroup updatedRpg = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", flowRpg.getIdentifier())
                .request()
                .put(Entity.entity(retrievedRpg, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(updatedRpg);
        assertNotNull(updatedRpg.getComponentConfiguration());
        assertEquals(retrievedRpg.getComponentConfiguration().getTargetUris(), updatedRpg.getComponentConfiguration().getTargetUris());
        assertEquals("http://host3", updatedRpg.getComponentConfiguration().getTargetUri());
        assertEquals(80, updatedRpg.getComponentConfiguration().getProxyPort().intValue());

        // Verify we have a revision and that the updated version is greater than the previous
        final FDRevision updatedRevision = updatedRpg.getRevision();
        assertNotNull(updatedRevision);
        assertTrue(updatedRevision.getVersion().longValue() > retrievedRpg.getRevision().getVersion().longValue());

        // Try to set the proxy port to null, should stay set at 80

        updatedRpg.getComponentConfiguration().setProxyPort(null);

        final FDRemoteProcessGroup updatedRpg2 = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", flowRpg.getIdentifier())
                .request()
                .put(Entity.entity(updatedRpg, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(updatedRpg2);
        assertNotNull(updatedRpg2.getComponentConfiguration());
        assertEquals(80, updatedRpg2.getComponentConfiguration().getProxyPort().intValue());

        // Try to set the proxy port to null, should stay set at 80

        updatedRpg2.getComponentConfiguration().setProxyPort(-1);

        final FDRemoteProcessGroup updatedRpg3 = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", flowRpg.getIdentifier())
                .request()
                .put(Entity.entity(updatedRpg2, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(updatedRpg3);
        assertNotNull(updatedRpg3.getComponentConfiguration());
        assertNull(updatedRpg3.getComponentConfiguration().getProxyPort());

        // Delete and verify response

        final FDRemoteProcessGroup deletedRpg = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", flowRpg.getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, updatedRpg3.getRevision().getClientId())
                .queryParam(FDFlowResource.VERSION, updatedRpg3.getRevision().getVersion())
                .request()
                .delete(FDRemoteProcessGroup.class);

        assertNotNull(deletedRpg);
        assertNotNull(deletedRpg.getComponentConfiguration());
        assertEquals(flowRpg.getIdentifier(), deletedRpg.getComponentConfiguration().getIdentifier());

        // Retrieve the flow again and make sure the component is gone

        final FDFlow testClassFlowAfterDelete = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlowAfterDelete);
        assertNotNull(testClassFlowAfterDelete.getFlowContent());
        assertEquals(0, testClassFlowAfterDelete.getFlowContent().getRemoteProcessGroups().size());

        // Retrieve flow events and verify 4 events exist - (create flow, create rpg, modify rpg, delete rpg)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(6, flowEvents.getElements().size());
    }

    @Test
    public void testConnectionsToRPGPorts() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);
        assertNotNull(testClassFlow.getFlowMetadata());
        assertNotNull(testClassFlow.getFlowContent());

        final String requestClientId = "IT-test";
        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        // Create a GenerateFlowFile processor...
        final FDProcessor createdGenerateFlowFileProcessor = createGenerateFlowFileProcessor(
                requestClientId, flowId, rootProcessGroupId, "New Processor");

        assertNotNull(createdGenerateFlowFileProcessor);
        assertNotNull(createdGenerateFlowFileProcessor.getUri());

        // Create a Remote Process Group...

        final VersionedRemoteProcessGroup remoteProcessGroupConfig = new VersionedRemoteProcessGroup();
        remoteProcessGroupConfig.setTargetUris("http://localhost:8080/nifi");

        final FDRemoteProcessGroup remoteProcessGroup = new FDRemoteProcessGroup();
        remoteProcessGroup.setComponentConfiguration(remoteProcessGroupConfig);
        remoteProcessGroup.setRevision(requestRevision);

        final FDRemoteProcessGroup createdRemoteProcessGroup = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/remote-process-groups")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(remoteProcessGroup, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(createdRemoteProcessGroup);
        assertNotNull(createdRemoteProcessGroup.getUri());

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
        connectionDest.setId(UUID.randomUUID().toString());
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

        assertNotNull(createdConnection);
        assertNotNull(createdConnection.getUri());

        // Retrieve the flow and verify the contents...

        final FDFlow retrievedFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        // Verify the flow has the expected processor
        assertEquals(1, retrievedFlow.getFlowContent().getProcessors().size());

        final VersionedProcessor flowProcessor = retrievedFlow.getFlowContent().getProcessors().stream().findFirst().get();
        assertNotNull(flowProcessor);
        assertEquals(createdGenerateFlowFileProcessor.getComponentConfiguration().getIdentifier(), flowProcessor.getIdentifier());

        // Verify the flow has the RPG with the specified input port
        assertEquals(1, retrievedFlow.getFlowContent().getRemoteProcessGroups().size());

        final VersionedRemoteProcessGroup flowRpg = retrievedFlow.getFlowContent().getRemoteProcessGroups().stream().findFirst().get();
        assertNotNull(flowRpg);
        assertEquals(createdRemoteProcessGroup.getComponentConfiguration().getIdentifier(), flowRpg.getIdentifier());

        assertNotNull(flowRpg.getInputPorts());
        assertEquals(1, flowRpg.getInputPorts().size());

        // Verify the flow has the connection between the processor and RPG
        assertEquals(1, retrievedFlow.getFlowContent().getConnections().size());

        final VersionedConnection flowConnection = retrievedFlow.getFlowContent().getConnections().stream().findFirst().get();
        assertNotNull(flowConnection);
        assertEquals(createdConnection.getComponentConfiguration().getIdentifier(), flowConnection.getIdentifier());

        final ConnectableComponent flowConnectionSource = flowConnection.getSource();
        assertEquals(connectionSource.getId(), flowConnectionSource.getId());
        assertEquals(connectionSource.getType(), flowConnectionSource.getType());
        assertEquals(connectionSource.getGroupId(), flowConnectionSource.getGroupId());

        final ConnectableComponent flowConnectionDestination = flowConnection.getDestination();
        assertEquals(connectionDest.getId(), flowConnectionDestination.getId());
        assertEquals(connectionDest.getType(), flowConnectionDestination.getType());
        assertEquals(connectionDest.getGroupId(), flowConnectionDestination.getGroupId());

        // Create a second RPG so we can update the connection's destination to the new RPG

        final VersionedRemoteProcessGroup remoteProcessGroupConfig2 = new VersionedRemoteProcessGroup();
        remoteProcessGroupConfig2.setTargetUris("http://localhost:8080/nifi");

        final FDRemoteProcessGroup remoteProcessGroup2 = new FDRemoteProcessGroup();
        remoteProcessGroup2.setComponentConfiguration(remoteProcessGroupConfig2);
        remoteProcessGroup2.setRevision(requestRevision);

        final FDRemoteProcessGroup createdRemoteProcessGroup2 = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/remote-process-groups")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(remoteProcessGroup2, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        assertNotNull(createdRemoteProcessGroup2);
        assertNotNull(createdRemoteProcessGroup2.getUri());

        // Update the connection's destination

        final ConnectableComponent connectionDest2 = new ConnectableComponent();
        connectionDest2.setId(UUID.randomUUID().toString());
        connectionDest2.setGroupId(createdRemoteProcessGroup2.getComponentConfiguration().getIdentifier());
        connectionDest2.setType(ConnectableComponentType.REMOTE_INPUT_PORT);

        createdConnection.getComponentConfiguration().setDestination(connectionDest2);

        final FDConnection updatedConnection = designerFlowsTarget
                .path("/{flowId}/connections/{connectionId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("connectionId", createdConnection.getComponentConfiguration().getIdentifier())
                .request()
                .put(Entity.entity(createdConnection, MediaType.APPLICATION_JSON_TYPE), FDConnection.class);

        assertNotNull(updatedConnection);
        assertNotNull(updatedConnection.getUri());

        // Retrieve the flow and verify the updates...

        final FDFlow retrievedFlowUpdatedConnection = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(retrievedFlowUpdatedConnection);
        assertEquals(1, retrievedFlowUpdatedConnection.getFlowContent().getProcessors().size());
        assertEquals(2, retrievedFlowUpdatedConnection.getFlowContent().getRemoteProcessGroups().size());
        assertEquals(1, retrievedFlowUpdatedConnection.getFlowContent().getConnections().size());

        // Verify the original RPG no longer has any input ports
        final VersionedRemoteProcessGroup retrievedOriginalRpg = retrievedFlowUpdatedConnection.getFlowContent().getRemoteProcessGroups()
                .stream()
                .filter(rpg -> rpg.getIdentifier().equals(createdRemoteProcessGroup.getComponentConfiguration().getIdentifier()))
                .findFirst()
                .orElse(null);
        assertNotNull(retrievedOriginalRpg);
        assertEquals(0, retrievedOriginalRpg.getInputPorts().size());
        assertEquals(0, retrievedOriginalRpg.getOutputPorts().size());

        // Verify the second RPG now has the input port
        final VersionedRemoteProcessGroup retrievedSecondRpg = retrievedFlowUpdatedConnection.getFlowContent().getRemoteProcessGroups()
                .stream()
                .filter(rpg -> rpg.getIdentifier().equals(createdRemoteProcessGroup2.getComponentConfiguration().getIdentifier()))
                .findFirst()
                .orElse(null);
        assertNotNull(retrievedSecondRpg);
        assertEquals(1, retrievedSecondRpg.getInputPorts().size());
        assertEquals(0, retrievedSecondRpg.getOutputPorts().size());

        // Delete the RPG and ensure the connection gets removed...

        final FDRemoteProcessGroup deletedRpg = designerFlowsTarget
                .path("/{flowId}/remote-process-groups/{rpgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("rpgId", createdRemoteProcessGroup2.getComponentConfiguration().getIdentifier())
                .queryParam(FDFlowResource.CLIENT_ID, createdRemoteProcessGroup2.getRevision().getClientId())
                .queryParam(FDFlowResource.VERSION, createdRemoteProcessGroup2.getRevision().getVersion())
                .request()
                .delete(FDRemoteProcessGroup.class);
        assertNotNull(deletedRpg);

        final FDFlow retrievedFlowDeletedRpg = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(retrievedFlowDeletedRpg);
        assertEquals(1, retrievedFlowDeletedRpg.getFlowContent().getProcessors().size());
        assertEquals(1, retrievedFlowDeletedRpg.getFlowContent().getRemoteProcessGroups().size());
        assertEquals(0, retrievedFlowDeletedRpg.getFlowContent().getConnections().size());
    }

    @Test
    public void testGetProcessGroupFlowContent() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        // Retrieve the initial process group flow...
        final FDProcessGroupFlow processGroupFlow = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", "root")
                .request()
                .get(FDProcessGroupFlow.class);

        assertNotNull(processGroupFlow);
        assertNotNull(processGroupFlow.getFlowMetadata());
        assertEquals(flowId, processGroupFlow.getFlowMetadata().getIdentifier());
        assertNotNull(processGroupFlow.getLocalFlowRevision());

        final FDVersionInfo versionInfo = processGroupFlow.getVersionInfo();
        assertNull(versionInfo);

        // Verify the contents are completely empty...
        final FDProcessGroupFlowContent processGroupFlowContent = processGroupFlow.getFlowContent();
        assertNotNull(processGroupFlowContent);
        assertEquals(0, processGroupFlowContent.getProcessGroups().size());
        assertEquals(0, processGroupFlowContent.getProcessors().size());
        assertEquals(0, processGroupFlowContent.getControllerServices().size());
        assertEquals(0, processGroupFlowContent.getConnections().size());
        assertEquals(0, processGroupFlowContent.getRemoteProcessGroups().size());
        assertEquals(0, processGroupFlowContent.getFunnels().size());

        // Create a processor...
        final String clientId = "IT-test";
        final String rootGroupId = processGroupFlow.getFlowMetadata().getRootProcessGroupIdentifier();

        final FDProcessor createdProcessor = createGenerateFlowFileProcessor(clientId, flowId, rootGroupId, "Test Process Group Flow Content");
        assertNotNull(createdProcessor);

        // Retrieve the process group flow again to verify it includes the processor...
        final FDProcessGroupFlow processGroupFlowWithProcessor = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", "root")
                .request()
                .get(FDProcessGroupFlow.class);

        assertNotNull(processGroupFlowWithProcessor);

        final FDProcessGroupFlowContent processGroupFlowContentWithProcessor = processGroupFlowWithProcessor.getFlowContent();
        assertEquals(0, processGroupFlowContentWithProcessor.getProcessGroups().size());
        assertEquals(1, processGroupFlowContentWithProcessor.getProcessors().size());
        assertEquals(0, processGroupFlowContentWithProcessor.getControllerServices().size());
        assertEquals(0, processGroupFlowContentWithProcessor.getConnections().size());
        assertEquals(0, processGroupFlowContentWithProcessor.getRemoteProcessGroups().size());
        assertEquals(0, processGroupFlowContentWithProcessor.getFunnels().size());

        final FDProcessor flowProcessor = processGroupFlowContentWithProcessor.getProcessors().stream().findFirst().get();
        assertNotNull(flowProcessor.getRevision());
        assertNotNull(flowProcessor.getUri());
        assertEquals(createdProcessor.getComponentConfiguration().getIdentifier(), flowProcessor.getComponentConfiguration().getIdentifier());
    }

    @Test
    public void testPropertyAndRelationshipOrder() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);

        final String clientId = "IT-test";
        final String rootGroupId = testClassFlow.getFlowMetadata().getRootProcessGroupIdentifier();

        // Create a RouteOnAttribute processor...
        final String routeOnAttributeName = "RouteOnAttribute";
        final String routeOnAttributeType = "org.apache.nifi.minifi.processors.RouteOnAttribute";

        final FDProcessor createdRouteOnAttribute = createProcessor(clientId, flowId, rootGroupId,
                routeOnAttributeName, routeOnAttributeType, systemBundle);

        assertNotNull(createdRouteOnAttribute);
        assertNotNull(createdRouteOnAttribute.getComponentDefinition());

        // Should have 0 properties to start off
        final Map<String,PropertyDescriptor> propertyDescriptors = createdRouteOnAttribute.getComponentDefinition().getPropertyDescriptors();
        assertNotNull(propertyDescriptors);
        assertEquals(0, propertyDescriptors.size());

        final List<Relationship> relationships = createdRouteOnAttribute.getComponentDefinition().getSupportedRelationships();
        assertNotNull(relationships);
        assertEquals(0, relationships.size());

        final String routeOnAttributeId = createdRouteOnAttribute.getComponentConfiguration().getIdentifier();

        // Add a dynamic property named "C"
        final FDPropertyDescriptor descriptorC = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .resolveTemplate("propertyName", "C")
                .request()
                .get(FDPropertyDescriptor.class);
        assertNotNull(descriptorC);

        createdRouteOnAttribute.getComponentConfiguration().getProperties().put(
                descriptorC.getPropertyDescriptor().getName(), descriptorC.getPropertyDescriptor().getName());

        final FDProcessor updatedRouteOnAttribute1 = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .put(Entity.entity(createdRouteOnAttribute, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        // Add a dynamic property named "B"
        final FDPropertyDescriptor descriptorB = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .resolveTemplate("propertyName", "B")
                .request()
                .get(FDPropertyDescriptor.class);
        assertNotNull(descriptorB);

        updatedRouteOnAttribute1.getComponentConfiguration().getProperties().put(
                descriptorB.getPropertyDescriptor().getName(), descriptorB.getPropertyDescriptor().getName());

        final FDProcessor updatedRouteOnAttribute2 = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .put(Entity.entity(updatedRouteOnAttribute1, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        // Add a dynamic property named "A"
        final FDPropertyDescriptor descriptorA = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .resolveTemplate("propertyName", "A")
                .request()
                .get(FDPropertyDescriptor.class);
        assertNotNull(descriptorA);

        updatedRouteOnAttribute2.getComponentConfiguration().getProperties().put(
                descriptorA.getPropertyDescriptor().getName(), descriptorA.getPropertyDescriptor().getName());

        final FDProcessor updatedRouteOnAttribute3 = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .put(Entity.entity(updatedRouteOnAttribute2, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        // Retrieve the processor by id and verify the ordering of the properties and relationships
        final FDProcessor retrievedRouteOnAttribute = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .get(FDProcessor.class);

        assertNotNull(retrievedRouteOnAttribute);

        final ProcessorDefinition retrievedDefinition = retrievedRouteOnAttribute.getComponentDefinition();
        assertNotNull(retrievedDefinition);

        final Map<String,PropertyDescriptor> retrievedDescriptors = retrievedDefinition.getPropertyDescriptors();
        assertNotNull(retrievedDescriptors);
        assertEquals(3, retrievedDescriptors.size());

        int count = 0;
        for (Map.Entry<String,PropertyDescriptor> entry : retrievedDescriptors.entrySet()) {
            if (count == 0) {
                assertEquals(descriptorA.getPropertyDescriptor().getName(), entry.getKey());
            } else if (count == 1) {
                assertEquals(descriptorB.getPropertyDescriptor().getName(), entry.getKey());
            } else if (count == 2) {
                assertEquals(descriptorC.getPropertyDescriptor().getName(), entry.getKey());
            }
            count++;
        }

        final List<Relationship> retrievedRelationships = retrievedDefinition.getSupportedRelationships();
        assertNotNull(retrievedRelationships);
        assertEquals(3, retrievedRelationships.size());
        assertEquals(descriptorA.getPropertyDescriptor().getName(), retrievedRelationships.get(0).getName());
        assertEquals(descriptorB.getPropertyDescriptor().getName(), retrievedRelationships.get(1).getName());
        assertEquals(descriptorC.getPropertyDescriptor().getName(), retrievedRelationships.get(2).getName());
    }

    @Test
    public void testGetPropertyDescriptors() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);

        final String clientId = "IT-test";
        final String rootGroupId = testClassFlow.getFlowMetadata().getRootProcessGroupIdentifier();

        // Create a processor that has required properties with default values
        final FDProcessor createdGetFileProc = createProcessor(clientId, flowId, rootGroupId, "GetFile",
                "org.apache.nifi.minifi.processors.GetFile", systemBundle);
        assertNotNull(createdGetFileProc);

        // Verify the input dir starts as the default value
        final String inputDirPropName = "Input Directory";
        assertEquals(".", createdGetFileProc.getComponentConfiguration().getProperties().get(inputDirPropName));

        // Issue an update to change the input dir
        final String createdGetFileProcId = createdGetFileProc.getComponentConfiguration().getIdentifier();
        createdGetFileProc.getComponentConfiguration().getProperties().put(inputDirPropName, "/tmp");

        final FDProcessor updatedGetFileProc = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", createdGetFileProcId)
                .request()
                .put(Entity.entity(createdGetFileProc, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        // Verify the updated works
        assertEquals("/tmp", updatedGetFileProc.getComponentConfiguration().getProperties().get(inputDirPropName));

        // Issue an update to set the value of input dir to null
        updatedGetFileProc.getComponentConfiguration().getProperties().put(inputDirPropName, null);

        final FDProcessor updatedGetFileProc2 = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", createdGetFileProcId)
                .request()
                .put(Entity.entity(updatedGetFileProc, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        // Verify the value is set back to the default
        assertEquals(".", updatedGetFileProc2.getComponentConfiguration().getProperties().get(inputDirPropName));

        // Create a processor that does not support dynamic properties...
        final FDProcessor createdGenerateFlowFile = createGenerateFlowFileProcessor(
                clientId, flowId, rootGroupId, "GenerateFlowFile");
        assertNotNull(createdGenerateFlowFile);

        // Verify we can retrieve an existing property descriptor...
        final String batchSizeName = "Batch Size";
        final String generateFlowFileId = createdGenerateFlowFile.getComponentConfiguration().getIdentifier();

        final FDPropertyDescriptor batchSizeDescriptor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", generateFlowFileId)
                .resolveTemplate("propertyName", batchSizeName)
                .request()
                .get(FDPropertyDescriptor.class);

        assertNotNull(batchSizeDescriptor);
        assertNotNull(batchSizeDescriptor.getPropertyDescriptor());
        assertEquals(batchSizeName, batchSizeDescriptor.getPropertyDescriptor().getName());
        assertEquals("INTEGER_VALIDATOR",batchSizeDescriptor.getPropertyDescriptor().getValidator());

        final String fileSizeName = "File Size";

        final FDPropertyDescriptor fileSizeDescriptor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", generateFlowFileId)
                .resolveTemplate("propertyName", fileSizeName)
                .request()
                .get(FDPropertyDescriptor.class);

        assertNotNull(fileSizeDescriptor);
        assertNotNull(fileSizeDescriptor.getPropertyDescriptor());
        assertEquals(fileSizeName, fileSizeDescriptor.getPropertyDescriptor().getName());
        assertEquals("DATA_SIZE_VALIDATOR",fileSizeDescriptor.getPropertyDescriptor().getValidator());


        final String dataFormatName = "Data Format";

        final FDPropertyDescriptor dataFormatDescriptor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", generateFlowFileId)
                .resolveTemplate("propertyName", dataFormatName)
                .request()
                .get(FDPropertyDescriptor.class);

        assertNotNull(dataFormatDescriptor);
        assertNotNull(dataFormatDescriptor.getPropertyDescriptor());
        assertEquals(dataFormatName, dataFormatDescriptor.getPropertyDescriptor().getName());
        // one is not set for this
        assertEquals("VALID",dataFormatDescriptor.getPropertyDescriptor().getValidator());
        // ... but we do have allowable values
        assertEquals(2,dataFormatDescriptor.getPropertyDescriptor().getAllowableValues().size());
        Set<String> allowableValues = Sets.newHashSet("Text","Binary");
        assertFalse(dataFormatDescriptor.getPropertyDescriptor().getAllowableValues().stream().filter(e-> !allowableValues.contains( e.getValue())).findAny().isPresent());
        try {
            designerFlowsTarget
                    .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                    .resolveTemplate("flowId", flowId)
                    .resolveTemplate("processorId", generateFlowFileId)
                    .resolveTemplate("propertyName", "DOES-NOT-EXIST")
                    .request()
                    .get(FDPropertyDescriptor.class);
            fail("Should have thrown ResourceNotFoundException");
        } catch (NotFoundException e) {
            // Expected
        }

        // Create a processor that does support dynamic properties...
        final String routeOnAttributeType = "org.apache.nifi.minifi.processors.RouteOnAttribute";
        final FDProcessor createdRouteOnAttribute = createProcessor(clientId, flowId, rootGroupId,
                "RouteOnAttribute", routeOnAttributeType, systemBundle);
        assertNotNull(createdRouteOnAttribute);
        assertNotNull(createdRouteOnAttribute.getComponentDefinition());

        // Should start with no relationships
        assertNotNull(createdRouteOnAttribute.getComponentDefinition().getSupportedRelationships());
        assertEquals(0, createdRouteOnAttribute.getComponentDefinition().getSupportedRelationships().size());

        final String fooName = "FOO";
        final String routeOnAttributeId = createdRouteOnAttribute.getComponentConfiguration().getIdentifier();

        // Verify we can ask for any property name and get back a descriptor...
        final FDPropertyDescriptor fooDescriptor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .resolveTemplate("propertyName", fooName)
                .request()
                .get(FDPropertyDescriptor.class);

        assertNotNull(fooDescriptor);
        assertNotNull(fooDescriptor.getPropertyDescriptor());
        assertEquals(fooName, fooDescriptor.getPropertyDescriptor().getName());
        assertTrue(fooDescriptor.getPropertyDescriptor().isDynamic());
        assertNotNull(fooDescriptor.getPropertyDescriptor().getExpressionLanguageScope());

        // Issue an update to submit a value for the dynamic property
        createdRouteOnAttribute.getComponentConfiguration().getProperties().put(fooName, "");

        final FDProcessor updatedRouteOnAttribute = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .put(Entity.entity(createdRouteOnAttribute, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        assertNotNull(updatedRouteOnAttribute);
        assertNotNull(updatedRouteOnAttribute.getComponentDefinition());
        assertNotNull(updatedRouteOnAttribute.getComponentDefinition().getPropertyDescriptors());
        assertTrue(updatedRouteOnAttribute.getComponentDefinition().getPropertyDescriptors().containsKey(fooName));

        final PropertyDescriptor routeOnAttributePropFromUpdatedProc =
                updatedRouteOnAttribute.getComponentDefinition().getPropertyDescriptors().get(fooName);
        assertTrue(routeOnAttributePropFromUpdatedProc.isDynamic());
        assertNotNull(routeOnAttributePropFromUpdatedProc.getExpressionLanguageScope());

        // Updated processor should have a dynamic relationship populated
        assertNotNull(updatedRouteOnAttribute.getComponentDefinition().getSupportedRelationships());
        assertEquals(1, updatedRouteOnAttribute.getComponentDefinition().getSupportedRelationships().size());
        assertEquals(fooName, updatedRouteOnAttribute.getComponentDefinition().getSupportedRelationships().get(0).getName());

        // Retrieve the component and verify the component definition has a descriptor for the dynamic property

        final FDProcessor retrievedRouteOnAttribute = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .get(FDProcessor.class);

        assertNotNull(retrievedRouteOnAttribute);
        assertNotNull(retrievedRouteOnAttribute.getComponentDefinition());
        assertNotNull(retrievedRouteOnAttribute.getComponentDefinition().getPropertyDescriptors());

        // Updated processor should have a dynamic relationship populated
        assertNotNull(retrievedRouteOnAttribute.getComponentDefinition().getSupportedRelationships());
        assertEquals(1, retrievedRouteOnAttribute.getComponentDefinition().getSupportedRelationships().size());
        assertEquals(fooName, retrievedRouteOnAttribute.getComponentDefinition().getSupportedRelationships().get(0).getName());

        final ProcessorDefinition routeOnAttributeDefinition = retrievedRouteOnAttribute.getComponentDefinition();
        assertTrue(routeOnAttributeDefinition.getPropertyDescriptors().containsKey(fooName));

        final PropertyDescriptor routeOnAttributeProp = routeOnAttributeDefinition.getPropertyDescriptors().get(fooName);
        assertTrue(routeOnAttributeProp.isDynamic());
        assertNotNull(routeOnAttributeProp.getExpressionLanguageScope());

        assertEquals("", retrievedRouteOnAttribute.getComponentConfiguration().getProperties().get(fooName));

        // Retrieve the flow and verify the component definition in the flow also has the descriptor for the dynamic property
        final FDProcessGroupFlow pgFlowAfterProcUpdates = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", "root")
                .request()
                .get(FDProcessGroupFlow.class);

        assertNotNull(pgFlowAfterProcUpdates);
        assertNotNull(pgFlowAfterProcUpdates.getFlowContent());
        assertNotNull(pgFlowAfterProcUpdates.getFlowContent().getProcessors());

        final FDProcessor routeOnAttributeFromPGFlow = pgFlowAfterProcUpdates.getFlowContent().getProcessors().stream()
                .filter(p -> p.getComponentConfiguration().getIdentifier().equals(routeOnAttributeId))
                .findFirst().orElse(null);
        assertNotNull(routeOnAttributeFromPGFlow);

        final ProcessorDefinition routeOnAttributeFromPGFlowDefinition = routeOnAttributeFromPGFlow.getComponentDefinition();
        assertTrue(routeOnAttributeFromPGFlowDefinition.getPropertyDescriptors().containsKey(fooName));

        final PropertyDescriptor routeOnAttributeFromPGFlowProp = routeOnAttributeFromPGFlowDefinition.getPropertyDescriptors().get(fooName);
        assertTrue(routeOnAttributeFromPGFlowProp.isDynamic());
        assertNotNull(routeOnAttributeFromPGFlowProp.getExpressionLanguageScope());

        // Issue an update to set the value of the dynamic property to null
        retrievedRouteOnAttribute.getComponentConfiguration().getProperties().put(fooName, null);

        final FDProcessor updatedRouteOnAttribute2 = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", routeOnAttributeId)
                .request()
                .put(Entity.entity(retrievedRouteOnAttribute, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        assertNotNull(updatedRouteOnAttribute2);
        assertNotNull(updatedRouteOnAttribute2.getComponentDefinition());
        assertNotNull(updatedRouteOnAttribute2.getComponentDefinition().getPropertyDescriptors());
        assertFalse(updatedRouteOnAttribute2.getComponentDefinition().getPropertyDescriptors().containsKey(fooName));
        assertFalse(updatedRouteOnAttribute2.getComponentConfiguration().getProperties().containsKey(fooName));


        // Create a controller service, currently minifi-cpp does not support dynamic properties in controller services

        final String csType = "org.apache.nifi.minifi.controllers.SSLContextService";

        final FDControllerService createdService = createService(clientId, flowId,
                rootGroupId, "SSL Context Service", csType, systemBundle);

        // Verify we can retrieve an existing property...

        final String clientCertName = "Client Certificate";
        final String serviceId = createdService.getComponentConfiguration().getIdentifier();

        final FDPropertyDescriptor clientCertDescriptor = designerFlowsTarget
                .path("/{flowId}/controller-services/{controllerServiceId}/descriptors/{propertyName}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("controllerServiceId", serviceId)
                .resolveTemplate("propertyName", clientCertName)
                .request()
                .get(FDPropertyDescriptor.class);

        assertNotNull(clientCertDescriptor);
        assertNotNull(clientCertDescriptor.getPropertyDescriptor());
        assertEquals(clientCertName, clientCertDescriptor.getPropertyDescriptor().getName());

        // Verify that retrieving a non-existent property throws an exception
        try {
            designerFlowsTarget
                    .path("/{flowId}/controller-services/{controllerServiceId}/descriptors/{propertyName}")
                    .resolveTemplate("flowId", flowId)
                    .resolveTemplate("controllerServiceId", serviceId)
                    .resolveTemplate("propertyName", "DOES-NOT-EXIST")
                    .request()
                    .get(FDPropertyDescriptor.class);
            fail("Should have thrown an exception");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    @Test
    public void testPopulatingControllerServiceProperties() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertNotNull(testClassFlow);

        final String clientId = "IT-test";
        final String rootGroupId = testClassFlow.getFlowMetadata().getRootProcessGroupIdentifier();

        // Create an invoke http processor...
        final String invokeHttpType = "org.apache.nifi.minifi.processors.InvokeHTTP";

        final Bundle httpBundle = new Bundle();
        httpBundle.setGroup("org.apache.nifi.minifi");
        httpBundle.setArtifact("minifi-http-curl");
        httpBundle.setVersion("0.6.0");

        final FDProcessor invokeHttpProcessor = createProcessor(clientId,flowId, rootGroupId, "InvokeHttp", invokeHttpType, httpBundle);
        assertNotNull(invokeHttpProcessor);

        // Verify there are no available SSL Context Services...
        final PropertyDescriptor sslContextServiceProperty = invokeHttpProcessor.getComponentDefinition()
                .getPropertyDescriptors().values().stream()
                .filter(p -> p.getName().equals("SSL Context Service"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have found property for SSL Context Service"));
        assertNotNull(sslContextServiceProperty);
        assertEquals(0, sslContextServiceProperty.getAllowableValues().size());

        // Create SSL Context Services...
        final String sslContextServiceType = "org.apache.nifi.minifi.controllers.SSLContextService";

        final FDControllerService sslContextService1 = createService(clientId, flowId, rootGroupId,
                "SSL Context Service 1", sslContextServiceType, systemBundle);
        assertNotNull(sslContextService1);

        final FDControllerService sslContextService2 = createService(clientId, flowId, rootGroupId,
                "SSL Context Service 2", sslContextServiceType, systemBundle);
        assertNotNull(sslContextService2);

        // Create a different type of service...

        final String updatePolicyServiceType = "org.apache.nifi.minifi.controllers.UpdatePolicyControllerService";

        final FDControllerService policyService = createService(clientId, flowId, rootGroupId,
                "Policy Service", updatePolicyServiceType, systemBundle);
        assertNotNull(policyService);

        // Retrieve the processor again now that we have available services...

        final String invokeHttpIdentifier = invokeHttpProcessor.getComponentConfiguration().getIdentifier();

        final FDProcessor retrievedInvokeHttpProcessor = designerFlowsTarget
                .path("/{flowId}/processors/{processorId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", invokeHttpIdentifier)
                .request()
                .get(FDProcessor.class);

        assertNotNull(retrievedInvokeHttpProcessor);

        // Verify the two SSL Context Services are now available as allowable values...

        final PropertyDescriptor retrievedSslContextServiceProperty = retrievedInvokeHttpProcessor.getComponentDefinition()
                .getPropertyDescriptors().values().stream()
                .filter(p -> p.getName().equals("SSL Context Service"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have found property for SSL Context Service"));
        assertNotNull(retrievedSslContextServiceProperty);

        final List<PropertyAllowableValue> allowableValues = retrievedSslContextServiceProperty.getAllowableValues();
        assertEquals(2, allowableValues.size());

        final String sslContextService1Identifier = sslContextService1.getComponentConfiguration().getIdentifier();
        assertNotNull(allowableValues.stream().filter(a -> a.getValue().equals(sslContextService1Identifier)).findFirst().orElse(null));

        final String sslContextService2Identifier = sslContextService2.getComponentConfiguration().getIdentifier();
        assertNotNull(allowableValues.stream().filter(a -> a.getValue().equals(sslContextService2Identifier)).findFirst().orElse(null));

        // Retrieve the process group content...

        final FDProcessGroupFlow processGroupFlow = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", "root")
                .request()
                .get(FDProcessGroupFlow.class);

        assertNotNull(processGroupFlow);
        assertNotNull(processGroupFlow.getFlowContent());

        // Verify the processor in the flow has allowable values populated for the controller service...

        final Set<FDProcessor> flowProcessors = processGroupFlow.getFlowContent().getProcessors();
        assertNotNull(flowProcessors);
        assertEquals(1, flowProcessors.size());

        final FDProcessor flowProcessor = flowProcessors.stream().findFirst().get();

        final PropertyDescriptor flowSslContextServiceProperty = flowProcessor.getComponentDefinition()
                .getPropertyDescriptors().values().stream()
                .filter(p -> p.getName().equals("SSL Context Service"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Should have found property for SSL Context Service"));
        assertNotNull(flowSslContextServiceProperty);

        final List<PropertyAllowableValue> flowAllowableValues = flowSslContextServiceProperty.getAllowableValues();
        assertEquals(2, flowAllowableValues.size());
    }

    @Test
    public void testPublishAndRevertFlow() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);
        assertNotNull(testClassFlow);

        // Verify the flow starts out with null version info...
        final FDVersionInfo versionInfoInitial = testClassFlow.getVersionInfo();
        assertNull(versionInfoInitial);

        final FDVersionInfoResult versionInfoResult = designerFlowsTarget
                .path("/{flowId}/version-info")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDVersionInfoResult.class);
        assertNotNull(versionInfoResult);
        assertNull(versionInfoResult.getVersionInfo());

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();
        final String requestClientId = "IT-test";

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        // Create a processor...

        final FDProcessor createdProcessor = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 1" );
        assertNotNull(createdProcessor);

        // Create a remote process group...

        final VersionedRemoteProcessGroup remoteProcessGroupConfig = new VersionedRemoteProcessGroup();
        remoteProcessGroupConfig.setTargetUris("http://localhost:8080/nifi");

        final FDRemoteProcessGroup remoteProcessGroup = new FDRemoteProcessGroup();
        remoteProcessGroup.setComponentConfiguration(remoteProcessGroupConfig);
        remoteProcessGroup.setRevision(requestRevision);

        final FDRemoteProcessGroup createdRemoteProcessGroup = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/remote-process-groups")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", rootProcessGroupId)
                .request()
                .post(Entity.entity(remoteProcessGroup, MediaType.APPLICATION_JSON_TYPE), FDRemoteProcessGroup.class);

        // Create a connection between the processor and RPG...

        final VersionedConnection connectionConfig = new VersionedConnection();
        connectionConfig.setName("My Connection");
        connectionConfig.setFlowFileExpiration("60 seconds");
        connectionConfig.setBackPressureDataSizeThreshold("10000 B");
        connectionConfig.setSelectedRelationships(Collections.singleton("success"));

        final ConnectableComponent connectionSource = new ConnectableComponent();
        connectionSource.setId(createdProcessor.getComponentConfiguration().getIdentifier());
        connectionSource.setGroupId(rootProcessGroupId);
        connectionSource.setType(ConnectableComponentType.PROCESSOR);
        connectionConfig.setSource(connectionSource);

        final ConnectableComponent connectionDest = new ConnectableComponent();
        connectionDest.setId(UUID.randomUUID().toString());
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
        assertNotNull(createdConnection);

        // Retrieve flow events and verify 4 events exist - (create flow, create proc, modify proc, delete proc)

        final ListContainer<FDFlowEvent> flowEvents = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEvents);
        assertNotNull(flowEvents.getElements());
        assertEquals(4, flowEvents.getElements().size());

        BigInteger prevEventRevision = new BigInteger("-1");
        for (FDFlowEvent flowEvent : flowEvents.getElements()) {
            assertTrue(flowEvent.getFlowRevision().compareTo(prevEventRevision) > 0);
            assertFalse(flowEvent.getEventType() == FDFlowEventType.FLOW_PUBLISHED);
            prevEventRevision = flowEvent.getFlowRevision();
        }

        // Publish the flow...
        final FDFlowPublishMetadata publishMetadata = new FDFlowPublishMetadata();
        publishMetadata.setComments("Publishing integration test flow");

        final FDVersionInfo versionInfo = designerFlowsTarget
                .path("/{flowId}/publish")
                .resolveTemplate("flowId", flowId)
                .request()
                .post(Entity.entity(publishMetadata, MediaType.APPLICATION_JSON_TYPE), FDVersionInfo.class);
        assertNotNull(versionInfo);

        // Check the events again and verify there is only the publish event...

        final ListContainer<FDFlowEvent> flowEventsAfterPublish = designerFlowsTarget
                .path("/{flowId}/events")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(new GenericType<ListContainer<FDFlowEvent>>() {});

        assertNotNull(flowEventsAfterPublish);
        assertNotNull(flowEventsAfterPublish.getElements());
        assertEquals(1, flowEventsAfterPublish.getElements().size());

        final FDFlowEvent publishEvent = flowEventsAfterPublish.getElements().get(0);
        assertEquals(FDFlowEventType.FLOW_PUBLISHED, publishEvent.getEventType());
        assertEquals("http://localhost:18080", publishEvent.getRegistryUrl());
        assertEquals("A", publishEvent.getRegistryBucketId());
        assertEquals("B", publishEvent.getRegistryFlowId());
        assertEquals(1, publishEvent.getRegistryVersion().intValue());
        assertEquals(4, publishEvent.getFlowRevision().intValue());

        // Check the flow and make sure it has the same info

        final FDFlow flowAfterPublish = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(4, flowAfterPublish.getLocalFlowRevision().intValue());

        final FDVersionInfo versionInfoAfterPublish = flowAfterPublish.getVersionInfo();
        assertNotNull(versionInfoAfterPublish);
        assertEquals("http://localhost:18080", versionInfoAfterPublish.getRegistryUrl());
        assertEquals("A", versionInfoAfterPublish.getRegistryBucketId());
        assertEquals("B", versionInfoAfterPublish.getRegistryFlowId());
        assertEquals(1, versionInfoAfterPublish.getRegistryVersion().intValue());
        assertFalse(versionInfoAfterPublish.isDirty());

        // Add another processor

        final FDProcessor processorAfterPublish = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 1" );
        assertNotNull(processorAfterPublish);

        // Verify the revision incremented and processor exists
        final FDFlow flowAfterAddingProcessor = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(5, flowAfterAddingProcessor.getLocalFlowRevision().intValue());
        assertNotNull(
                flowAfterAddingProcessor.getFlowContent().getProcessors().stream()
                        .filter(p -> p.getIdentifier().equals(processorAfterPublish.getComponentConfiguration().getIdentifier()))
                        .findFirst().orElse(null)
        );

        // Verify the version info returns as dirty
        final FDVersionInfoResult versionInfoAfterAddProc = designerFlowsTarget
                .path("/{flowId}/version-info")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDVersionInfoResult.class);
        assertNotNull(versionInfoAfterAddProc);
        assertNotNull(versionInfoAfterAddProc.getVersionInfo());
        assertTrue(versionInfoAfterAddProc.getVersionInfo().isDirty());

        // Perform the revert to previous revision

        final FDVersionInfo revertVersionInfo = designerFlowsTarget
                .path("/{flowId}/revert")
                .resolveTemplate("flowId", flowId)
                .request()
                .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), FDVersionInfo.class);
        assertNotNull(revertVersionInfo);

        // Verify the revision went back after reverting and processor not longer exists
        final FDFlow flowAfterRevert = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);

        assertEquals(4, flowAfterRevert.getLocalFlowRevision().intValue());
        assertNull(
                flowAfterRevert.getFlowContent().getProcessors().stream()
                        .filter(p -> p.getIdentifier().equals(processorAfterPublish.getComponentConfiguration().getIdentifier()))
                        .findFirst().orElse(null)
        );
    }

    @Test
    @Ignore // Don't want to run this as part of every build, but leaving here for manual testing when needed
    public void testConcurrentRequests() throws IOException, InterruptedException {
        final String flowId = createAgentManifestClassAndFlow();

        final FDFlow testClassFlow = designerFlowsTarget
                .path("/{flowId}")
                .resolveTemplate("flowId", flowId)
                .request()
                .get(FDFlow.class);
        assertNotNull(testClassFlow);

        final String rootProcessGroupId = testClassFlow.getFlowContent().getIdentifier();
        final String requestClientId = "IT-test";

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(requestClientId);

        // Create a processor...

        final FDProcessor createdProcessor = createGenerateFlowFileProcessor(requestClientId, flowId, rootProcessGroupId, "Processor 1" );
        assertNotNull(createdProcessor);

        final String processorId = createdProcessor.getComponentConfiguration().getIdentifier();
        final String processorName = createdProcessor.getComponentConfiguration().getName();
        final String processorType = createdProcessor.getComponentConfiguration().getType();
        final Bundle processorBundle = createdProcessor.getComponentConfiguration().getBundle();

        // Create two updater runnables...

        final int executions = 500;

        final ProcessorUpdater updater1 = new ProcessorUpdater();
        updater1.setClientId(requestClientId);
        updater1.setFlowId(flowId);
        updater1.setProcessorId(processorId);
        updater1.setProcessorName(processorName);
        updater1.setProcessorType(processorType);
        updater1.setProcessorBundle(processorBundle);
        updater1.setDesignerFlowsTarget(designerFlowsTarget);
        updater1.setUpdaterName("Updater #1");
        updater1.setExecutions(executions);

        final ProcessorUpdater updater2 = new ProcessorUpdater();
        updater2.setClientId(requestClientId);
        updater2.setFlowId(flowId);
        updater2.setProcessorId(processorId);
        updater2.setProcessorName(processorName);
        updater2.setProcessorType(processorType);
        updater2.setProcessorBundle(processorBundle);
        updater2.setDesignerFlowsTarget(designerFlowsTarget);
        updater2.setUpdaterName("Updater #2");
        updater2.setExecutions(executions);

        // Start two threads to execute the updaters concurrently...

        final Thread thread1 = new Thread(updater1);
        thread1.setDaemon(true);
        thread1.start();

        final Thread thread2 = new Thread(updater2);
        thread2.setDaemon(true);
        thread2.start();

        // Wait for the threads to finish...

        thread1.join();
        thread2.join();

        // Fail if either of the updaters caught an exception

        if (updater1.exception != null) {
            updater1.exception.printStackTrace();
            fail(updater1.updaterName + " failed with: " + updater1.exception.getMessage());
        }

        if (updater2.exception != null) {
            updater2.exception.printStackTrace();
            fail(updater2.updaterName + " failed with: " + updater2.exception.getMessage());
        }
    }

    public static class ProcessorUpdater implements Runnable {

        private String clientId;
        private String flowId;
        private String processorId;
        private String processorName;
        private String processorType;
        private Bundle processorBundle;
        private WebTarget designerFlowsTarget;

        private String updaterName;
        private int executions = 50;

        private Exception exception;

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public void setFlowId(String flowId) {
            this.flowId = flowId;
        }

        public void setProcessorId(String processorId) {
            this.processorId = processorId;
        }

        public void setProcessorName(String processorName) {
            this.processorName = processorName;
        }

        public void setProcessorType(String processorType) {
            this.processorType = processorType;
        }

        public void setProcessorBundle(Bundle processorBundle) {
            this.processorBundle = processorBundle;
        }

        public void setDesignerFlowsTarget(WebTarget designerFlowsTarget) {
            this.designerFlowsTarget = designerFlowsTarget;
        }

        public void setUpdaterName(String updaterName) {
            this.updaterName = updaterName;
        }

        public void setExecutions(int executions) {
            this.executions = executions;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < executions; i++) {
                    final FDProcessor requestProcessor = getRequestProcessor(clientId, processorName, processorType, processorBundle);
                    requestProcessor.getComponentConfiguration().setIdentifier(processorId);

                    final FDProcessor updatedProcessor = designerFlowsTarget
                            .path("/{flowId}/processors/{processorId}")
                            .resolveTemplate("flowId", flowId)
                            .resolveTemplate("processorId", processorId)
                            .request()
                            .put(Entity.entity(requestProcessor, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);
                    System.out.println("*** " + updaterName + " - " + i);
                }
            } catch (Exception e) {
                exception = e;
                return;
            }
        }
    }

    private String createAgentManifestClassAndFlow() throws IOException, InterruptedException {
        // Create an Agent-Manifest
        final InputStream defaultManifestIn = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_MANIFEST);
        final AgentManifest defaultManifest = mapper.readValue(defaultManifestIn, AgentManifest.class);

        final AgentManifest createdManifest = baseTarget
                .path("agent-manifests")
                .request()
                .post(Entity.entity(defaultManifest, MediaType.APPLICATION_JSON), AgentManifest.class);

        // Create an Agent-Class and assign it the created manifest
        final AgentClass testAgentClass = new AgentClass();
        testAgentClass.setName(TEST_AGENT_CLASS + " " + UUID.randomUUID().toString());
        testAgentClass.setDescription("The agent class for " + TEST_AGENT_CLASS);
        testAgentClass.setAgentManifests(Collections.singleton(createdManifest.getIdentifier()));

        baseTarget
                .path("agent-classes")
                .request()
                .post(Entity.entity(testAgentClass, MediaType.APPLICATION_JSON), AgentClass.class);

        // Give the agent-class monitor enough time to create a flow
        LOGGER.info("Created test flow, waiting for AgentClassMonitor to detect class and create flow...");
        FDFlowMetadata testAgentClassFlow = null;
        Instant start = Instant.now();
        Duration waitTime = Duration.ZERO;
        while (waitTime.compareTo(MAX_AGENT_CLASS_MONITOR_WAIT_TIME) < 0) {
            Thread.sleep(250);
            final ListContainer<FDFlowMetadata> flowMetadataListContainer = designerFlowsTarget.request()
                    .get(new GenericType<ListContainer<FDFlowMetadata>>() {});

            testAgentClassFlow = flowMetadataListContainer.getElements().stream()
                    .filter(flow -> flow.getAgentClass().equals(testAgentClass.getName()))
                    .findFirst().orElse(null);

            waitTime = Duration.between(start, Instant.now());

            if (testAgentClassFlow != null) {
                break;
            }
        }
        if (testAgentClassFlow != null) {
            LOGGER.info("Detection took {}ms", waitTime.toMillis());
        } else {
            LOGGER.warn("Detection failed after waiting {}ms! Aborting test", waitTime.toMillis());
            fail("Flow for created class is null");
        }

        return testAgentClassFlow.getIdentifier();
    }

    private FDProcessor createGenerateFlowFileProcessor(final String clientId, final String flowId,
                                                        final String processGroupId, final String processorName) {
        final String type = "org.apache.nifi.minifi.processors.GenerateFlowFile";
        return createProcessor(clientId, flowId, processGroupId, processorName, type, systemBundle);
    }

    private FDProcessor createProcessor(final String clientId, final String flowId, final String processGroupId,
                                        final String processorName, final String type, final Bundle bundle) {
        final FDProcessor requestProcessor = getRequestProcessor(clientId, processorName, type, bundle);

        final FDProcessor createdProcessor = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/processors")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", processGroupId)
                .request()
                .post(Entity.entity(requestProcessor, MediaType.APPLICATION_JSON_TYPE), FDProcessor.class);

        return createdProcessor;
    }

    private static FDProcessor getRequestProcessor(String clientId, String processorName, String type, Bundle bundle) {
        final VersionedProcessor requestComponent = new VersionedProcessor();
        requestComponent.setName(processorName);
        requestComponent.setType(type);
        requestComponent.setBundle(bundle);

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(clientId);

        final FDProcessor requestProcessor = new FDProcessor();
        requestProcessor.setComponentConfiguration(requestComponent);
        requestProcessor.setRevision(requestRevision);
        return requestProcessor;
    }

    private FDControllerService createService(final String clientId, final String flowId, final String processGroupId,
                                              final String serviceName, final String type, final Bundle bundle) {
        final VersionedControllerService requestComponent = new VersionedControllerService();
        requestComponent.setName(serviceName);
        requestComponent.setType(type);
        requestComponent.setBundle(bundle);

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(0L);
        requestRevision.setClientId(clientId);

        final FDControllerService requestService = new FDControllerService();
        requestService.setComponentConfiguration(requestComponent);
        requestService.setRevision(requestRevision);

        // Perform the create and verify response

        final FDControllerService createdService = designerFlowsTarget
                .path("/{flowId}/process-groups/{pgId}/controller-services")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", processGroupId)
                .request()
                .post(Entity.entity(requestService, MediaType.APPLICATION_JSON_TYPE), FDControllerService.class);

        return createdService;
    }
}
