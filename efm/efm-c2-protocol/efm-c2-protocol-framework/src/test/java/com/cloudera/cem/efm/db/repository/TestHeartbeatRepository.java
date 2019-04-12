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
import com.cloudera.cem.efm.db.entity.HeartbeatEntity;
import com.cloudera.cem.efm.db.projection.IdAndNumber;
import com.cloudera.cem.efm.db.projection.IdAndTimestamp;
import org.assertj.core.util.IterableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestHeartbeatRepository extends DatabaseTest {

    // See insertData.sql
    private static final long PREPOPULATED_HEARTBEAT_COUNT = 6;
    private static final long PREPOPULATED_AGENT1_HEARTBEAT_COUNT = 2;
    private static final long PREPOPULATED_NOAGENT_HEARTBEAT_COUNT = 3;
    private static final long PREPOPULATED_DEVICE1_HEARTBEAT_COUNT = 2;
    private static final Instant HB_1_TIME = new Calendar.Builder()
            .setTimeZone(TimeZone.getTimeZone("GMT"))
            .setDate(2018, Calendar.APRIL, 11)
                .setTimeOfDay(12, 51,00)
                .build().toInstant();

    @Autowired
    private HeartbeatRepository repository;

    @Test
    public void testCount() {
        // Arrange
        // See insertData.sql

        // Act
        final long actualCount = repository.count();

        // Assert
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT, actualCount);
    }

    @Test
    public void testSave() {
        // Arrange
        Date testStartTime = new Date(System.currentTimeMillis());
        final HeartbeatEntity entity = new HeartbeatEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDeviceId("test-device-1");
        entity.setAgentId("test-agent-1");
        entity.setContent("SHOULD BE JSON");

        // Act
        final HeartbeatEntity savedEntity = repository.save(entity);

        // Assert
        assertNotNull(savedEntity.getCreated());
        assertTrue(savedEntity.getCreated().after(testStartTime));
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT + 1, repository.count());
    }

    @Test
    public void testFindById() {
        // Arrange
        // See insertData.sql
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Act "when finding missing operation id"
        final Optional<HeartbeatEntity> missingEntity = repository.findById("nonexistent-id");

        // Assert "empty optional is returned"
        assertFalse(missingEntity.isPresent());

        // Act "when finding pre-populated operation id"
        final Optional<HeartbeatEntity> entityOptional = repository.findById("heartbeat-1");

        // Assert "pre-populated operation entity is returned with all fields correct"
        assertTrue(entityOptional.isPresent());
        HeartbeatEntity entity = entityOptional.get();
        assertEquals("heartbeat-1", entity.getId());
        assertEquals("device-1", entity.getDeviceId());
        assertEquals("agent-1", entity.getAgentId());
        assertEquals("agent-manifest-1", entity.getAgentManifestId());
        assertEquals("Class A", entity.getAgentClass());
        assertEquals("SHOULD BE JSON", entity.getContent());
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(entity.getCreated()));
    }

    @Test
    public void testFindByDeviceId() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<HeartbeatEntity> entities = repository.findByDeviceId("device-1");

        // Assert
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_DEVICE1_HEARTBEAT_COUNT, entitiesList.size());
        assertEquals("heartbeat-1", entitiesList.get(0).getId());
        assertEquals("heartbeat-2", entitiesList.get(1).getId());
    }

    @Test
    public void testFindByAgentId() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<HeartbeatEntity> entities = repository.findByAgentId("agent-1");

        // Assert
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_AGENT1_HEARTBEAT_COUNT, entitiesList.size());
        assertEquals("heartbeat-1", entitiesList.get(0).getId());
        assertEquals("heartbeat-3", entitiesList.get(1).getId());
    }

    @Test
    public void testFindByAgentIdNull() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<HeartbeatEntity> entities = repository.findByAgentId(null);

        // Assert
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_NOAGENT_HEARTBEAT_COUNT, entitiesList.size());
        assertEquals("heartbeat-4", entitiesList.get(0).getId());
        assertEquals("heartbeat-5", entitiesList.get(1).getId());
        assertEquals("heartbeat-6", entitiesList.get(2).getId());
    }

    @Test
    public void testDelete() {
        // Arrange
        // See insertData.sql

        // Act
        repository.deleteById("heartbeat-1");

        // Assert
        assertFalse(repository.findById("heartbeat-1").isPresent());
    }

    @Test
    public void testDeleteByCreatedBefore() {
        // Arrange
        // See insertData.sql
        final Date ageOffDate = Date.from(HB_1_TIME.plusSeconds(1));

        // Act
        repository.deleteByCreatedBefore(ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT - 1, entitiesList.size());
        assertEquals("heartbeat-2", entitiesList.get(0).getId());
    }

    @Test
    public void testDeleteByAgentIdEqualsAndCreatedBefore() {
        // Arrange
        // See insertData.sql
        Date ageOffDate = Date.from(HB_1_TIME.plus(Duration.ofSeconds(61)));

        // Act
        repository.deleteByAgentIdEqualsAndCreatedBefore("agent-1", ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT - 1, entitiesList.size());
        assertEquals("heartbeat-2", entitiesList.get(0).getId());
    }

    @Test
    public void testDeleteByAgentIsNullAndCreatedBefore() {
        // Arrange
        // See insertData.sql
        Date ageOffDate = Date.from(HB_1_TIME.plus(Duration.ofMinutes(5)));

        // Act
        repository.deleteByAgentIdIsNullAndCreatedBefore(ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT - 2, entitiesList.size());
    }

    @Test
    public void testDeleteAllContent() {
        // Arrange
        // See insertData.sql

        // Act
        repository.deleteAllContent();

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT, entitiesList.size());
        for (HeartbeatEntity hb : entitiesList) {
            assertNull(hb.getContent());
        }
    }

    @Test
    public void testDeleteContentByCreatedBefore() {
        // Arrange
        // See insertData.sql
        Date ageOffDate = Date.from(HB_1_TIME.plus(Duration.ofSeconds(30)));

        // Act
        repository.deleteContentByCreatedBefore(ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT, entitiesList.size());
        for (HeartbeatEntity hb : entitiesList) {
            if (hb.getCreated().before(ageOffDate)) {
                assertNull(hb.getContent());
            } else {
                assertNotNull(hb.getContent());
            }
        }
    }

    @Test
    public void testDeleteContentByAgentIdEqualsAndCreatedBefore() {
        // Arrange
        // See insertData.sql
        Date ageOffDate = Date.from(HB_1_TIME.plus(Duration.ofSeconds(61)));

        // Act
        repository.deleteContentByAgentIdEqualsAndCreatedBefore("agent-1", ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT, entitiesList.size());
        assertNull(repository.findById("heartbeat-1").get().getContent());
    }

    @Test
    public void testDeletContenteByAgentIsNullAndCreatedBefore() {
        // Arrange
        // See insertData.sql
        Date ageOffDate = Date.from(HB_1_TIME.plus(Duration.ofMinutes(5)));

        // Act
        repository.deleteContentByAgentIdIsNullAndCreatedBefore(ageOffDate);

        // Assert
        final Iterable<HeartbeatEntity> entities = repository.findAll();
        final List<HeartbeatEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_HEARTBEAT_COUNT, entitiesList.size());
        for (HeartbeatEntity hb : entitiesList) {
            if (hb.getAgentId() == null && hb.getCreated().before(ageOffDate)) {
                assertNull(hb.getContent());
            } else {
                assertNotNull(hb.getContent());
            }
        }
    }

    @Test
    public void testFindHeartbeatTimestampsByAgentId() {
        // Arrange
        // See insertData.sql
        final Date hb3Time = Date.from(HB_1_TIME.plusSeconds(120));
        List<IdAndTimestamp> expectedResults = new ArrayList<>();
        expectedResults.add(new IdAndTimestamp("heartbeat-3", hb3Time));
        expectedResults.add(new IdAndTimestamp("heartbeat-1", Date.from(HB_1_TIME)));

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatTimestampsByAgentId("agent-1", PageRequest.of(0, 10));

        // Assert
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testFindHeartbeatTimestampsByAgentIdNull() {
        // Arrange
        // See insertData.sql

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatTimestampsByAgentId(null, PageRequest.of(0, 10));

        // Assert
        assertEquals(Collections.emptyList(), actualResults);
    }

    @Test
    public void testFindHeartbeatTimestampsByAgentIdIsNull() {
        // Arrange
        // See insertData.sql
        final Date hb4Time = Date.from(HB_1_TIME.plus(Duration.ofMinutes(3)));
        final Date hb5Time = Date.from(HB_1_TIME.plus(Duration.ofMinutes(4)));
        final Date hb6Time = Date.from(HB_1_TIME.plus(Duration.ofMinutes(5)));
        List<IdAndTimestamp> expectedResults = new ArrayList<>();
        expectedResults.add(new IdAndTimestamp("heartbeat-6", hb6Time));
        expectedResults.add(new IdAndTimestamp("heartbeat-5", hb5Time));
        expectedResults.add(new IdAndTimestamp("heartbeat-4", hb4Time));

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatTimestampsByAgentIdIsNull(PageRequest.of(0, 10));

        // Assert
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testFindHeartbeatContentTimestampsByAgentId() {
        // Arrange
        // See insertData.sql
        final Date hb1Time = Date.from(HB_1_TIME);
        List<IdAndTimestamp> expectedResults = Collections.singletonList(new IdAndTimestamp("heartbeat-1", hb1Time));
        // Clear content from HB3
        HeartbeatEntity hb3 = repository.findById("heartbeat-3").get();
        hb3.setContent(null);
        repository.save(hb3);

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatContentTimestampsByAgentId("agent-1", PageRequest.of(0, 10));

        // Assert
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testFindHeartbeatContentTimestampsByAgentIdNull() {
        // Arrange
        // See insertData.sql

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatContentTimestampsByAgentId(null, PageRequest.of(0, 10));

        // Assert
        assertEquals(Collections.emptyList(), actualResults);
    }

    @Test
    public void testFindHeartbeatContentTimestampsByAgentIdIsNull() {
        // Arrange
        // See insertData.sql
        final Date hb4Time = Date.from(HB_1_TIME.plus(Duration.ofMinutes(3)));
        final Date hb5Time = Date.from(HB_1_TIME.plus(Duration.ofMinutes(4)));
        List<IdAndTimestamp> expectedResults = new ArrayList<>();
        expectedResults.add(new IdAndTimestamp("heartbeat-5", hb5Time));
        expectedResults.add(new IdAndTimestamp("heartbeat-4", hb4Time));
        // Clear content from HB6
        HeartbeatEntity hb6 = repository.findById("heartbeat-6").get();
        hb6.setContent(null);
        repository.save(hb6);

        // Act
        List<IdAndTimestamp> actualResults = repository.findHeartbeatContentTimestampsByAgentIdIsNull(PageRequest.of(0, 10));

        // Assert
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testFindHeartbeatCountsByAgentId() {
        // Arrange
        // See insertData.sql
        List<IdAndNumber> expectedResults = new ArrayList<>();
        expectedResults.add(new IdAndNumber(null, 3L));
        expectedResults.add(new IdAndNumber("agent-1", 2L));
        expectedResults.add(new IdAndNumber("agent-2", 1L));

        // Act
        List<IdAndNumber> actualResults = IterableUtil.nonNullElementsIn(repository.findHeartbeatCountsByAgentId());

        // Assert
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testFindHeartbeatContentCountsByAgentId() {
        // Arrange
        // See insertData.sql
        List<IdAndNumber> expectedResults = new ArrayList<>();
        expectedResults.add(new IdAndNumber(null, 3L));
        expectedResults.add(new IdAndNumber("agent-1", 2L));
        // Clear content from HB2
        HeartbeatEntity hb2 = repository.findById("heartbeat-2").get();
        hb2.setContent(null);
        repository.save(hb2);

        // Act
        List<IdAndNumber> actualResults = IterableUtil.nonNullElementsIn(repository.findHeartbeatContentCountsByAgentId());

        // Assert
        assertEquals(expectedResults, actualResults);
    }

}
