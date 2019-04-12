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
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.db.entity.AgentEntity;
import com.cloudera.cem.efm.db.entity.AgentManifestEntity;
import org.assertj.core.util.IterableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestAgentRepository extends DatabaseTest {

    // See insertData.sql
    private static final long PREPOPULATED_AGENT_COUNT = 1;

    @Autowired
    private AgentRepository repository;
    @Autowired
    private AgentManifestRepository manifestRepository;

    @Test
    public void testCount() {
        // Arrange
        // See insertData.sql

        // Act
        final long actualCount = repository.count();

        // Assert
        assertEquals(PREPOPULATED_AGENT_COUNT, actualCount);
    }

    @Test
    public void testFindById() {
        // Arrange
        // See insertData.sql
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Act "when finding a missing id"
        final Optional<AgentEntity> missingEntity = repository.findById("nonexistent-id");

        // Assert "empty optional is returned"
        assertFalse(missingEntity.isPresent());

        // Act "when finding a pre-populated id"
        final Optional<AgentEntity> entityOptional = repository.findById("agent-1");

        // Assert "pre-populated entity is returned with all fields correct"
        assertTrue(entityOptional.isPresent());
        AgentEntity entity = entityOptional.get();
        assertEquals("agent-1", entity.getId());
        assertEquals("Agent One", entity.getName());
        assertEquals("Class A", entity.getAgentClass());
        assertEquals("SHOULD BE JSON", entity.getAgentStatusContent());
        assertEquals("2018-04-11 12:52:00.000 UTC", dateFormat.format(entity.getFirstSeen()));
        assertEquals("2018-04-11 12:53:00.000 UTC", dateFormat.format(entity.getLastSeen()));
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(entity.getCreated()));
        assertEquals("2018-04-11 12:54:00.000 UTC", dateFormat.format(entity.getUpdated()));
        assertEquals("agent-manifest-1", entity.getAgentManifestId());
    }

    @Test
    public void testFindAllByAgentClass() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<AgentEntity> agentEntities = repository.findAllByAgentClass("Class A");

        // Assert
        assertTrue(agentEntities != null);
        List<AgentEntity> agentEntityList = IterableUtil.nonNullElementsIn(agentEntities);
        assertEquals(1, agentEntityList.size());
        assertEquals("agent-1", agentEntityList.get(0).getId());

    }

    @Test
    public void testSave() {
        // Arrange
        // See insertData.sql
        final Optional <AgentManifestEntity> manifestEntityOptional = manifestRepository.findById("agent-manifest-1");
        assertTrue(manifestEntityOptional.isPresent());
        Date testStartTime = new Date(System.currentTimeMillis());
        final AgentEntity entity = new AgentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName("Test Agent");
        entity.setAgentClass("Class A");
        entity.setAgentManifestId("agent-manifest-1");
        entity.setAgentStatusContent("AGENT STATUS JSON");

        // Act
        final AgentEntity savedEntity = repository.save(entity);

        // Assert
        assertNotNull(savedEntity.getCreated());
        assertTrue(savedEntity.getCreated().after(testStartTime));
        assertNotNull(savedEntity.getUpdated());
        assertEquals(savedEntity.getCreated(), savedEntity.getUpdated());
        assertEquals(PREPOPULATED_AGENT_COUNT + 1, repository.count());
    }

    @Test
    public void testDeleteById() {
        // Arrange
        // See insertData.sql

        // Act
        repository.deleteById("agent-1");

        // Assert
        assertFalse(repository.findById("agent-1").isPresent());
    }

}
