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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudera.cem.efm.client.AgentClassClient;
import com.cloudera.cem.efm.client.C2Client;
import com.cloudera.cem.efm.client.C2ConfigurationClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.client.FlowClient;
import com.cloudera.cem.efm.client.FlowMappingClient;
import com.cloudera.cem.efm.client.impl.jersey.JerseyC2Client;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentInfo;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.AgentStatus;
import com.cloudera.cem.efm.model.BuildInfo;
import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.FlowFormat;
import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.FlowMappings;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.model.FlowSummary;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.model.NiFiRegistryInfo;
import com.cloudera.cem.efm.model.extension.Bundle;
import com.cloudera.cem.efm.model.extension.ComponentManifest;
import com.cloudera.cem.efm.model.extension.ExpressionLanguageScope;
import com.cloudera.cem.efm.model.extension.InputRequirement;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.extension.SchedulingDefaults;
import com.cloudera.cem.efm.model.extension.SchedulingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UnsecureC2ClientIT extends UnsecureITBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsecureC2ClientIT.class);

    private C2Client c2Client;

    @Before
    public void setup() {
        final String baseUrl = createBaseURL();
        LOGGER.info("Using base url = " + baseUrl);

        c2Client = new JerseyC2Client.Builder()
                .config(createClientConfig(baseUrl))
                .build();
    }

    @After
    public void teardown() {
        try {
            c2Client.close();
        } catch (IOException e) {
            LOGGER.error("Error closing C2 client", e);
        }
    }

    /**
     * This integration test performs a full lifecycle test against an agent class
     */
    @Test
    public void testAgentClassLifecycle() throws C2Exception, IOException {
        final AgentClassClient agentClassClient = c2Client.getAgentClassClient();

        final AgentClass agentClass = new AgentClass();
        final String agentClassName = "Secret Agent Class";
        agentClass.setName(agentClassName);
        agentClass.setDescription("This class is super duper secret.");

        // Persist an agent class
        final AgentClass returnedAgentClass = agentClassClient.createAgentClass(agentClass);
        Assert.assertNotNull(returnedAgentClass);
        Assert.assertEquals(agentClassName, returnedAgentClass.getName());

        // Perform a bulk listing, where we should have only one
        Assert.assertEquals("Did not receive the 1 expected class", 1, agentClassClient.getAgentClasses().size());

        // Retrieve the class specifically by the class name
        final AgentClass retrievedClass = agentClassClient.getAgentClass(returnedAgentClass.getName());
        Assert.assertNotNull(retrievedClass);
        Assert.assertEquals(returnedAgentClass.getName(), retrievedClass.getName());

        // Update this instance with a new description
        final String newDescription = "This class is not so secret.";
        retrievedClass.setDescription(newDescription);
        final AgentClass updatedAgentClass = agentClassClient.replaceAgentClass(retrievedClass.getName(), retrievedClass);
        Assert.assertNotNull(updatedAgentClass);
        Assert.assertEquals(newDescription, updatedAgentClass.getDescription());

        // Provide a friend for the existing agent class
        final AgentClass anotherAgentClass = new AgentClass();
        anotherAgentClass.setName("Just another class");
        anotherAgentClass.setDescription("Late to the party");
        final AgentClass anotherPersistedClass = agentClassClient.createAgentClass(anotherAgentClass);
        Assert.assertNotNull(anotherPersistedClass);
        Assert.assertEquals(anotherAgentClass.getName(), anotherPersistedClass.getName());

        // Perform a bulk listing, where we should have two
        Assert.assertEquals("Did not receive the 2 expected classes", 2, agentClassClient.getAgentClasses().size());
    }

    @Test(expected = C2Exception.class)
    public void testDeleteAgentClassNonexistent() throws C2Exception, IOException {
        final AgentClass nonExistentClassDeletion = c2Client.getAgentClassClient().deleteAgentClass("No Such Class");
        Assert.fail("Delete action should not have successfully occurred.");
    }

    @Test
    public void testFlowClient() throws IOException, C2Exception {
        final FlowClient flowClient = c2Client.getFlowClient();

        // Verify no flows exist
        final int initialFlowCount = flowClient.getFlowListing().getFlows().size();
        assertEquals(0, initialFlowCount);

        final String testFlowYamlContent = getSampleYamlFlow();

        // Create a flow from YAML
        final FlowSummary createdFlowSummary = flowClient.createFlow(testFlowYamlContent);
        assertNotNull(createdFlowSummary);

        // Retrieve the YAML of the created flow and verify it equals the original YAML
        final String retrievedFlowYaml = flowClient.getFlowYaml(createdFlowSummary.getId());
        assertEquals(testFlowYamlContent, retrievedFlowYaml);

        // Retrieve the full flow object and verify the id, format, and content match expected
        final Flow retrievedFlow  = flowClient.getFlow(createdFlowSummary.getId());
        assertNotNull(retrievedFlow);
        assertEquals(createdFlowSummary.getId(), retrievedFlow.getId());
        assertEquals(createdFlowSummary.getUri(), retrievedFlow.getUri());
        assertEquals(createdFlowSummary.getFlowFormat(), retrievedFlow.getFlowFormat());
        assertEquals(testFlowYamlContent, retrievedFlow.getFlowContent());

        // Create a flow using the FlowSnapshot object instead of raw YAML
        final FlowUri flowUri = new FlowUri();
        flowUri.setRegistryUrl("http://localhost:12345");
        flowUri.setBucketId("1234-1234-1234-1234");
        flowUri.setFlowId("2345-2345-2345-23345");

        final VersionedProcessor versionedProcessor = new VersionedProcessor();
        versionedProcessor.setName("Test Processor");

        final VersionedProcessGroup versionedProcessGroup = new VersionedProcessGroup();
        versionedProcessGroup.setProcessors(Collections.singleton(versionedProcessor));

        final VersionedFlowSnapshotMetadata versionedFlowSnapshotMetadata = new VersionedFlowSnapshotMetadata();
        versionedFlowSnapshotMetadata.setVersion(2);

        final VersionedFlowSnapshot versionedFlowSnapshot = new VersionedFlowSnapshot();
        versionedFlowSnapshot.setFlowContents(versionedProcessGroup);
        versionedFlowSnapshot.setSnapshotMetadata(versionedFlowSnapshotMetadata);

        final FlowSnapshot flowSnapshot = new FlowSnapshot();
        flowSnapshot.setFlowUri(flowUri);
        flowSnapshot.setVersionedFlowSnapshot(versionedFlowSnapshot);

        final FlowSummary flowSnapshotSummary = flowClient.createFlow(flowSnapshot);
        assertNotNull(flowSnapshotSummary);
        assertEquals(FlowFormat.YAML_V2_TYPE, flowSnapshotSummary.getFlowFormat());
        assertEquals(flowUri.getRegistryUrl(), flowSnapshotSummary.getRegistryUrl());
        assertEquals(flowUri.getBucketId(), flowSnapshotSummary.getRegistryBucketId());
        assertEquals(flowUri.getFlowId(), flowSnapshotSummary.getRegistryFlowId());

        // Retrieve the flow created using the FlowSnapshot format and verify fields
        final Flow retrievedFlowFromSnapshot = flowClient.getFlow(flowSnapshotSummary.getId());
        assertNotNull(retrievedFlowFromSnapshot);
        assertNotNull(retrievedFlowFromSnapshot.getCreated());
        assertNotNull(retrievedFlowFromSnapshot.getUpdated());
        assertNotNull(retrievedFlowFromSnapshot.getFlowContent());
        assertEquals(flowSnapshotSummary.getId(), retrievedFlowFromSnapshot.getId());
        assertEquals(FlowFormat.YAML_V2_TYPE, retrievedFlowFromSnapshot.getFlowFormat());
        assertEquals(flowUri.getRegistryUrl(), retrievedFlowFromSnapshot.getRegistryUrl());
        assertEquals(flowUri.getBucketId(), retrievedFlowFromSnapshot.getRegistryBucketId());
        assertEquals(flowUri.getFlowId(), retrievedFlowFromSnapshot.getRegistryFlowId());
        assertEquals(versionedFlowSnapshotMetadata.getVersion(), retrievedFlowFromSnapshot.getRegistryFlowVersion().intValue());

        // Hard to verify that the returned YAML is the correct transform of the VersionedFlowSnapshot, just verify proc name is there
        assertTrue(retrievedFlowFromSnapshot.getFlowContent().contains(versionedProcessor.getName()));
    }

    @Test
    public void testFlowAndMappingLifecycle() throws C2Exception, IOException, URISyntaxException {
        final FlowClient flowClient = c2Client.getFlowClient();

        // Capture current state of flows if we are not working from a clean environment as we cannot currently delete
        final int initialFlowCount = flowClient.getFlowListing().getFlows().size();


        final String testFlowContent = getSampleYamlFlow();
        // Create and verify received URI
        final FlowSummary createdFlowSummary = flowClient.createFlow(testFlowContent);
        assertNotNull(createdFlowSummary);

        final URI createdFlowUri = createdFlowSummary.getUri();
        assertNotNull(createdFlowUri);


        // Verify flow at URI is that which was transmitted
        String flowAtUriContent;
        try (final Scanner scanner = new Scanner(createdFlowUri.toURL().openStream(), StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            flowAtUriContent = scanner.hasNext() ? scanner.next() : "";
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Flow flowAtUri = objectMapper.readValue(flowAtUriContent, Flow.class);
        Assert.assertEquals(testFlowContent, flowAtUri.getFlowContent());

        // Verify the flow exists in the listing
        final List<FlowSummary> flows = flowClient.getFlowListing().getFlows();
        final int updatedSize = flows.size();
        Assert.assertEquals(initialFlowCount + 1, updatedSize);

        Optional<FlowSummary> optionalSummary = getIdForFlowUri(flows, createdFlowUri);
        Assert.assertTrue("Flow listing did not contain created URI", optionalSummary.isPresent());


        /* Create a new mapping for this flow */
        final FlowSummary matchingSummary = optionalSummary.get();
        final FlowMappingClient mappingClient = c2Client.getFlowMappingClient();

        // Find our initial state for flow mappings
        final FlowMappings initialFlowMappings = mappingClient.getFlowMappingListing();
        int initialMappingCount = initialFlowMappings.getFlowMappings().size();

        // Create the agent class we will use for the mapping
        final AgentClass agentClass = new AgentClass();
        final String testClassName = "mapping-test-class";
        agentClass.setName(testClassName);
        agentClass.setDescription("This class is super duper secret.");

        final AgentClass returnedAgentClass = c2Client.getAgentClassClient().createAgentClass(agentClass);
        Assert.assertNotNull(returnedAgentClass);

        //  Create a sample flow mapping
        final FlowMapping testFlowMapping = new FlowMapping();
        testFlowMapping.setAgentClass(testClassName);
        testFlowMapping.setFlowId(matchingSummary.getId());
        testFlowMapping.setFlowUri(matchingSummary.getUri());

        // Persist this to the server and verify its creation
        final URI mappingUri = mappingClient.createFlowMapping(testFlowMapping);
        Assert.assertNotNull("Flow Mapping URI was not set", mappingUri);
        Assert.assertEquals("Incorrect number of flow mappings after creation", initialMappingCount + 1, mappingClient.getFlowMappingListing().getFlowMappings().size());
        Assert.assertEquals("Did not receive URI in anticipated format", testClassName, StringUtils.substringAfterLast(mappingUri.toString(), "/"));

        // Attempt retrieval of the flow mapping
        final FlowMapping retrievedFlowMapping = mappingClient.getFlowMapping(testClassName);
        Assert.assertEquals(testClassName, retrievedFlowMapping.getAgentClass());
        Assert.assertEquals(matchingSummary.getId(), retrievedFlowMapping.getFlowId());
        Assert.assertEquals(matchingSummary.getUri(), retrievedFlowMapping.getFlowUri());


        // Update the agent flow mapping with a new flow
        final FlowSummary newFlowSummary = flowClient.createFlow(testFlowContent);
        assertNotNull(newFlowSummary);
        final URI newFlowUri = newFlowSummary.getUri();
        final Optional<FlowSummary> optionalNewFlow = getIdForFlowUri(flowClient.getFlowListing().getFlows(), newFlowUri);
        Assert.assertTrue(optionalNewFlow.isPresent());

        final FlowSummary newFlow = optionalNewFlow.get();
        final FlowMapping newFlowMapping = new FlowMapping();
        newFlowMapping.setFlowUri(newFlow.getUri());
        newFlowMapping.setFlowId(newFlow.getId());
        newFlowMapping.setAgentClass(testClassName);
        FlowMapping flowMapping = mappingClient.updateFlowMapping(testClassName, newFlowMapping);

        // Verify the updated mapping
        final FlowMapping updatedMapping = mappingClient.getFlowMapping(testClassName);
        Assert.assertEquals(testClassName, updatedMapping.getAgentClass());
        Assert.assertEquals(newFlowMapping.getFlowId(), updatedMapping.getFlowId());
        Assert.assertEquals(newFlowMapping.getFlowUri(), updatedMapping.getFlowUri());

        // Delete the mapping
        final FlowMapping deletedFlowMapping = mappingClient.deleteFlowMapping(testClassName);
        Assert.assertEquals(testClassName, deletedFlowMapping.getAgentClass());
        Assert.assertEquals(newFlowMapping.getFlowId(), deletedFlowMapping.getFlowId());
        Assert.assertEquals(newFlowMapping.getFlowUri(), deletedFlowMapping.getFlowUri());
    }

    private static Optional<FlowSummary> getIdForFlowUri(final Collection<FlowSummary> flows, final URI createdFlowUri) {
        return flows.stream().filter(flow -> createdFlowUri.equals(createdFlowUri)).findFirst();
    }

    /* TODO: Add/update tests when Agent resource can support creation/deletion of instances */
    @Test(expected = C2Exception.class)
    public void testCreateAgent() throws C2Exception, IOException {
        final Agent createdAgent = c2Client.getAgentClient().createAgent(generateTestAgentInfo());
        Assert.fail("Should not have successfully created an agent");
    }

    private static AgentInfo generateTestAgentInfo() {
        final AgentInfo testAgentInfo = new AgentInfo();

        final AgentManifest testAgentManifest = new AgentManifest();
        testAgentManifest.setAgentType("Test Agent Type");

        final BuildInfo testBuildInfo = new BuildInfo();
        testBuildInfo.setCompiler("Apple LLVM version 9.0.0 (clang-900.0.39.2)");
        testBuildInfo.setCompilerFlags("-O3");
        testBuildInfo.setRevision("Rev 123");
        testBuildInfo.setTargetArch("MIPS32");
        testBuildInfo.setTimestamp(System.currentTimeMillis());
        testBuildInfo.setVersion("a.1.a");

        testAgentManifest.setBuildInfo(testBuildInfo);

        final SchedulingDefaults schedulingDefaults = new SchedulingDefaults();
        schedulingDefaults.setDefaultMaxConcurrentTasks("2");
        schedulingDefaults.setDefaultRunDurationNanos(10000);
        schedulingDefaults.setDefaultSchedulingPeriodMillis(1000);
        schedulingDefaults.setDefaultSchedulingStrategy(SchedulingStrategy.TIMER_DRIVEN);

        testAgentManifest.setSchedulingDefaults(schedulingDefaults);

        final Bundle testBundle = new Bundle();
        testBundle.setArtifact("my-artifact");
        testBundle.setGroup("my-group");
        testBundle.setVersion("likea");

        final ComponentManifest testComponentManifest = new ComponentManifest();
        ProcessorDefinition processor = new ProcessorDefinition();
        processor.setInputRequirement(InputRequirement.INPUT_FORBIDDEN);
        final LinkedHashMap<String, PropertyDescriptor> propertyDescriptorMap = new LinkedHashMap<>();
        final PropertyDescriptor testPropertyDescriptor = new PropertyDescriptor();
        testPropertyDescriptor.setDefaultValue("a-value");
        testPropertyDescriptor.setDescription("Here be a property");
        testPropertyDescriptor.setDisplayName("My Property");


        PropertyAllowableValue aValue = new PropertyAllowableValue();
        aValue.setDescription("This is a value.");
        aValue.setDisplayName("A Value");
        aValue.setValue("a-value");

        PropertyAllowableValue anotherValue = new PropertyAllowableValue();
        anotherValue.setDescription("This is another value.");
        anotherValue.setDisplayName("Another value");
        anotherValue.setValue("another-value");

        List<PropertyAllowableValue> propAllowableVals = Arrays.asList(aValue, anotherValue);
        testPropertyDescriptor.setAllowableValues(propAllowableVals);

        testPropertyDescriptor.setName("my-property");
        testPropertyDescriptor.setRequired(true);
        testPropertyDescriptor.setSensitive(false);
        testPropertyDescriptor.setExpressionLanguageScope(ExpressionLanguageScope.VARIABLE_REGISTRY);

        propertyDescriptorMap.put("Property", testPropertyDescriptor);

        processor.setPropertyDescriptors(propertyDescriptorMap);
        List<ProcessorDefinition> processors = new ArrayList<>();
        processors.add(processor);
        testComponentManifest.setProcessors(processors);
        testBundle.setComponentManifest(testComponentManifest);

        List<Bundle> testBundles = new ArrayList<>();
        testBundles.add(testBundle);
        testAgentManifest.setBundles(testBundles);
        testAgentManifest.setComponentManifest(testComponentManifest);
        testAgentManifest.setIdentifier("Agent Manifest Identifier");
        testAgentManifest.setVersion("867");


        testAgentInfo.setAgentClass("Agent Class");
        testAgentInfo.setAgentManifest(testAgentManifest);
        testAgentInfo.setIdentifier("Agent Identifier");

        AgentStatus agentStatus = new AgentStatus();
        agentStatus.setUptime(System.currentTimeMillis());

        testAgentInfo.setStatus(agentStatus);

        return testAgentInfo;
    }

    @Test
    public void testGetNiFiRegistryInfo() throws C2Exception, IOException {
        C2ConfigurationClient configClient = c2Client.getC2ConfigurationClient();
        NiFiRegistryInfo info = configClient.getNiFiRegistryInfo();
        Assert.assertNotNull(info);
    }

    private static String getSampleYamlFlow() throws IOException {
        return readTestFile("yaml/minifi-generate-flow-file-sink.yml");
    }

    private static String readTestFile(String resourcePath) throws IOException {
        try (final InputStream in = UnsecureC2ClientIT.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return convertStreamToString(in);
        }
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
