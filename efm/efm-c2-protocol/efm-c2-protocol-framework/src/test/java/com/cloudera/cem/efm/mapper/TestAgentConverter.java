/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.mapper;

import com.cloudera.cem.efm.db.entity.AgentClassEntity;
import com.cloudera.cem.efm.db.entity.AgentEntity;
import com.cloudera.cem.efm.db.entity.AgentManifestEntity;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.AgentRepositories;
import com.cloudera.cem.efm.model.AgentRepositoryStatus;
import com.cloudera.cem.efm.model.AgentStatus;
import com.cloudera.cem.efm.model.BuildInfo;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestAgentConverter extends ModelMapperTest {

    @Test
    public void testAgentClassToEntityConverter() {
        // Arrange
        final AgentClass agentClass = new AgentClass();
        agentClass.setName("Class A");
        agentClass.setDescription("A test class");
        agentClass.setAgentManifests(new HashSet<>());
        agentClass.getAgentManifests().add("agent-manifest-1");
        agentClass.getAgentManifests().add("agent-manifest-2");

        // Act
        final AgentClassEntity mapResult = modelMapper.map(agentClass, AgentClassEntity.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(mapResult.getId(), agentClass.getName());
        assertEquals(mapResult.getDescription(), agentClass.getDescription());
        assertEquals(mapResult.getAgentManifests(), agentClass.getAgentManifests());
    }

    @Test
    public void testEntityToAgentClassConverter() {
        // Arrange
        final AgentClassEntity entity = new AgentClassEntity();
        entity.setId("Class A");
        entity.setDescription("A test class");
        entity.setAgentManifests(new HashSet<>());
        entity.getAgentManifests().add("agent-manifest-1");
        entity.getAgentManifests().add("agent-manifest-2");

        // Act
        final AgentClass mapResult = modelMapper.map(entity, AgentClass.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(entity.getId(), mapResult.getName());
        assertEquals(entity.getDescription(), mapResult.getDescription());
        assertEquals(entity.getAgentManifests(), mapResult.getAgentManifests());
    }

    @Test
    public void testPartialEntityToAgentClassConverter() {
        // Arrange
        final AgentClassEntity entity = new AgentClassEntity();
        entity.setId("Class A");
        entity.setDescription("A test class");
        // intentionally no agent manifests set

        // Act
        final AgentClass mapResult = modelMapper.map(entity, AgentClass.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(entity.getId(), mapResult.getName());
        assertEquals(entity.getDescription(), mapResult.getDescription());
        assertNull(mapResult.getAgentManifests());
    }

    @Test
    public void testAgentManifestToEntityConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentManifest, String> source = arrangeTestAgentManifest();

        // Act
        final AgentManifestEntity mapResult = modelMapper.map(source.getObject(), AgentManifestEntity.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(source.getObject().getIdentifier(), mapResult.getId());
        source.assertSerializationEquals(mapResult.getContent());
        assertNull(mapResult.getCreated());
        assertNull(mapResult.getUpdated());
    }

    @Test
    public void testAgentManifestToStringConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentManifest, String> source = arrangeTestAgentManifest();

        // Act
        final String mapResult = modelMapper.map(source.getObject(), String.class);

        // Assert
        source.assertSerializationEquals(mapResult);
    }

    @Test
    public void testEntityToAgentManifestConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentManifest, String> source = arrangeTestAgentManifest();

        // Act
        final AgentManifest mapResult = modelMapper.map(source.getSerialization(), AgentManifest.class);

        // Assert
        source.assertObjectEquals(mapResult);
    }

    @Test
    public void testStringToAgentManifestConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentManifest, String> source = arrangeTestAgentManifest();

        // Act
        final AgentManifest mapResult = modelMapper.map(source.getSerialization(), AgentManifest.class);

        // Assert
        source.assertObjectEquals(mapResult);
    }

    @Test
    public void testAgentStatusToStringConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentStatus, String> source = arrangeTestAgentStatus();

        // Act
        final String mapResult = modelMapper.map(source.getObject(), String.class);

        // Assert
        source.assertSerializationEquals(mapResult);
    }

    @Test
    public void testStringToAgentStatusConverter() throws Exception {
        // Arrange
        final SerializableTestObject<AgentStatus, String> source = arrangeTestAgentStatus();

        // Act
        final AgentStatus mapResult = modelMapper.map(source.getSerialization(), AgentStatus.class);

        // Assert
        source.assertObjectEquals(mapResult);
    }

    @Test
    public void testAgentToEntityMap() throws Exception {
        // Arrange
        final SerializableTestObject<AgentStatus, String> agentStatus = arrangeTestAgentStatus();
        final Agent agent = new Agent();
        agent.setIdentifier("agent-1");
        agent.setName("Agent One");
        agent.setAgentClass("Class A");
        agent.setFirstSeen(1L);
        agent.setLastSeen(2L);
        agent.setAgentManifestId("agent-manifest-1");
        agent.setStatus(agentStatus.getObject());

        // Act
        final AgentEntity mapResult = modelMapper.map(agent, AgentEntity.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(agent.getIdentifier(), mapResult.getId());
        assertEquals(agent.getName(), mapResult.getName());
        assertEquals(agent.getAgentClass(), mapResult.getAgentClass());
        assertEquals(new Date(agent.getFirstSeen()), mapResult.getFirstSeen());
        assertEquals(new Date(agent.getLastSeen()), mapResult.getLastSeen());
        assertEquals(agent.getAgentManifestId(), mapResult.getAgentManifestId());
        agentStatus.assertSerializationEquals(mapResult.getAgentStatusContent());
    }

    @Test
    public void testPartialAgentToEntityMap() throws Exception {
        // Arrange
        final Agent agent = new Agent();
        agent.setIdentifier("agent-1");

        // Act
        final AgentEntity mapResult = modelMapper.map(agent, AgentEntity.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(agent.getIdentifier(), mapResult.getId());
        assertNull(mapResult.getAgentStatusContent());
    }

    @Test
    public void testEntityToAgentMap() throws Exception {
        // Arrange
        final SerializableTestObject<AgentManifest, String> agentManifest = arrangeTestAgentManifest();
        final SerializableTestObject<AgentStatus, String> agentStatus = arrangeTestAgentStatus();
        final AgentEntity agentEntity = new AgentEntity();
        agentEntity.setId("agent-1");
        agentEntity.setName("Agent One");
        agentEntity.setAgentClass("Class A");
        agentEntity.setFirstSeen(new Date(1L));
        agentEntity.setLastSeen(new Date(2L));
        agentEntity.setAgentManifestId("agent-manifest-1");
        agentEntity.setAgentStatusContent(agentStatus.getSerialization());

        // Act
        final Agent mapResult = modelMapper.map(agentEntity, Agent.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(agentEntity.getId(), mapResult.getIdentifier());
        assertEquals(agentEntity.getName(), mapResult.getName());
        assertEquals(agentEntity.getAgentClass(), mapResult.getAgentClass());
        assertEquals(agentEntity.getFirstSeen(), new Date(mapResult.getFirstSeen()));
        assertEquals(agentEntity.getLastSeen(), new Date(mapResult.getLastSeen()));
        assertEquals(agentEntity.getAgentManifestId(), mapResult.getAgentManifestId());
        agentStatus.assertObjectEquals(mapResult.getStatus());
    }

    @Test
    public void testPartialEntityToAgentMap() throws Exception {
        // Arrange
        final AgentEntity agentEntity = new AgentEntity();
        agentEntity.setId("agent-1");

        // Act
        final Agent mapResult = modelMapper.map(agentEntity, Agent.class);

        // Assert
        assertNotNull(mapResult);
        assertEquals(agentEntity.getId(), mapResult.getIdentifier());
        assertNull(mapResult.getStatus());
    }


    // Helper methods

    private static SerializableTestObject<AgentManifest, String> arrangeTestAgentManifest() {
        final AgentManifest agentManifest = new AgentManifest();
        agentManifest.setIdentifier("agent-manifest-1");
        agentManifest.setAgentType("minifi-cpp");
        agentManifest.setBuildInfo(new BuildInfo());
        agentManifest.getBuildInfo().setVersion("0.5");
        agentManifest.getBuildInfo().setTimestamp(1L);

        final String agentManifestSerialization = "{\n" +
                "  \"identifier\": \"agent-manifest-1\",\n" +
                "  \"agentType\": \"minifi-cpp\",\n" +
                "  \"buildInfo\": {\n" +
                "    \"version\": \"0.5\",\n" +
                "    \"timestamp\": 1  \n" +
                "  }\n" +
                "}";

        final SerializableTestObjectAssertions<AgentManifest, String> assertions = new SerializableTestObjectAssertions<AgentManifest, String>() {
            @Override
            public void assertObjectEquals(SerializableTestObject<AgentManifest, String> expected, AgentManifest actualObject)
                    throws Exception {
                assertNotNull(actualObject);
                final AgentManifest expectedObject = expected.getObject();
                assertEquals(expectedObject.getIdentifier(), actualObject.getIdentifier());
                assertEquals(expectedObject.getAgentType(), actualObject.getAgentType());
                assertNotNull(actualObject.getBuildInfo());
                assertEquals(expectedObject.getBuildInfo().getVersion(), actualObject.getBuildInfo().getVersion());
                assertEquals(expectedObject.getBuildInfo().getTimestamp(), actualObject.getBuildInfo().getTimestamp());
            }

            @Override
            public void assertSerializationEquals(SerializableTestObject<AgentManifest, String> expected, String actualSerialization)
                    throws Exception {
                assertNotNull(actualSerialization);
                JSONAssert.assertEquals(expected.getSerialization(), actualSerialization, false);
            }
        };

        return new SerializableTestObject<>(agentManifest, agentManifestSerialization, assertions);
    }

    private static SerializableTestObject<AgentStatus, String> arrangeTestAgentStatus() {
        final AgentStatus agentStatus = new AgentStatus();
        agentStatus.setUptime(1L);
        agentStatus.setRepositories(new AgentRepositories());
        AgentRepositoryStatus flowFileRepositoryStatus = new AgentRepositoryStatus();
        flowFileRepositoryStatus.setSize(5L);
        agentStatus.getRepositories().setFlowfile(flowFileRepositoryStatus);

        final String agentStatusSerialization = "{\n" +
                "  \"uptime\": 1,\n" +
                "  \"repositories\": {\n" +
                "    \"flowfile\": {\n" +
                "      \"size\": 5\n" +
                "    }\n" +
                "  }\n" +
                "}";

        final SerializableTestObjectAssertions<AgentStatus, String> assertions = new SerializableTestObjectAssertions<AgentStatus, String>() {
            @Override
            public void assertObjectEquals(SerializableTestObject<AgentStatus, String> expected, AgentStatus actualObject) throws Exception {
                assertNotNull(actualObject);
                final AgentStatus expectedObject = expected.getObject();
                assertEquals(expectedObject.getUptime(), actualObject.getUptime());
                assertNotNull(actualObject.getRepositories());
                assertNotNull(actualObject.getRepositories().getFlowfile());
                assertEquals(expectedObject.getRepositories().getFlowfile().getSize(), actualObject.getRepositories().getFlowfile().getSize());
            }

            @Override
            public void assertSerializationEquals(SerializableTestObject<AgentStatus, String> expected, String actualSerialization) throws Exception {
                assertNotNull(actualSerialization);
                JSONAssert.assertEquals(expected.getSerialization(), actualSerialization, false);
            }
        };

        return new SerializableTestObject<>(agentStatus, agentStatusSerialization, assertions);
    }

}
