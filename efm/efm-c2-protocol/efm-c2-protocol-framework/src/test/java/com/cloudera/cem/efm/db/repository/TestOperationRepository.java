/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.db.entity.OperationEntity;
import com.cloudera.cem.efm.model.OperationState;
import com.cloudera.cem.efm.model.OperationType;
import org.assertj.core.util.IterableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestOperationRepository extends DatabaseTest {

    // See insertData.sql
    private static final long PREPOPULATED_OPERATION_COUNT = 2;

    @Autowired
    private OperationRepository repository;

    @Test
    public void testCount() {
        // Arrange
        // See insertData.sql

        // Act
        final long actualCount = repository.count();

        // Assert
        assertEquals(PREPOPULATED_OPERATION_COUNT, actualCount);
    }

    @Test
    public void testFindById() {
        // Arrange
        // See insertData.sql
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Act "when finding missing operation id"
        final Optional<OperationEntity> missingEntity = repository.findById("nonexistent-operation-id");

        // Assert "empty optional is returned"
        assertFalse(missingEntity.isPresent());

        // Act "when finding pre-populated operation id"
        final Optional<OperationEntity> entityOptional = repository.findById("operation-1");

        // Assert "pre-populated operation entity is returned with all fields correct"
        assertTrue(entityOptional.isPresent());
        OperationEntity operationEntity = entityOptional.get();
        assertEquals("operation-1", operationEntity.getId());
        assertEquals(OperationType.CLEAR, operationEntity.getOperation());
        assertEquals("connection", operationEntity.getOperand());
        assertEquals(1, operationEntity.getArgs().size());
        assertEquals("connection-1", operationEntity.getArgs().get("name"));
        assertEquals(0, operationEntity.getDependencies().size());
        assertEquals("agent-1", operationEntity.getTargetAgentId());
        assertEquals(OperationState.READY, operationEntity.getState());
        assertEquals("admin", operationEntity.getCreatedBy());
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(operationEntity.getCreated()));
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(operationEntity.getUpdated()));
    }

    @Test
    public void testFindByTargetAgentId() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<OperationEntity> entities = repository.findByTargetAgentId("agent-1");

        // Assert
        final List<OperationEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(2, entitiesList.size());
        assertEquals("operation-1", entitiesList.get(0).getId());
        assertEquals("operation-2", entitiesList.get(1).getId());
        assertEquals(1, entitiesList.get(1).getDependencies().size());
        assertTrue(entitiesList.get(1).getDependencies().contains("operation-1"));
    }

    @Test
    public void testFindByDependenciesContains() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<OperationEntity> entities = repository.findByDependenciesContains("operation-1");

        // Assert
        final List<OperationEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(1, entitiesList.size());
        OperationEntity entity0 = entitiesList.get(0);
        assertEquals("operation-2", entity0.getId());
        assertEquals(1, entity0.getDependencies().size());
        assertTrue(entity0.getDependencies().contains("operation-1"));
    }

    @Test
    public void testSave() {
        // Arrange
        final OperationEntity entity = new OperationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedBy("anonymous");
        entity.setDependencies(new HashSet<>());
        entity.setState(OperationState.NEW);
        entity.setOperation(OperationType.CLEAR);
        entity.setTargetAgentId("test-agent-1");
        entity.setOperand("connection");
        entity.setArgs(new HashMap<>());
        entity.getArgs().put("name", "test-connection-1");

        // Act
        final OperationEntity savedEntity = repository.save(entity);

        // Assert
        assertNotNull(savedEntity.getCreated());
        assertNotNull(savedEntity.getUpdated());
        assertEquals(savedEntity.getCreated(), savedEntity.getUpdated());
        assertEquals(PREPOPULATED_OPERATION_COUNT + 1, repository.count());
    }

    @Test
    public void testSaveAll() {
        // Arrange
        final OperationEntity entity1 = new OperationEntity();
        entity1.setId(UUID.randomUUID().toString());
        entity1.setCreatedBy("anonymous");
        entity1.setDependencies(new HashSet<>());
        entity1.setState(OperationState.NEW);
        entity1.setOperation(OperationType.CLEAR);
        entity1.setTargetAgentId("test-agent-1");
        entity1.setOperand("connection");

        final OperationEntity entity2 = new OperationEntity();
        entity2.setId(UUID.randomUUID().toString());
        entity2.setCreatedBy("anonymous");
        entity2.setState(OperationState.NEW);
        entity2.setOperation(OperationType.DESCRIBE);
        entity2.setTargetAgentId("test-agent-1");
        entity2.setDependencies(new HashSet<>());
        entity2.getDependencies().add(entity1.getId());

        // Act
        final Iterable<OperationEntity> savedEntities = repository.saveAll(Arrays.asList(entity1, entity2));

        // Assert
        final List<OperationEntity> savedEntitiesList = IterableUtil.nonNullElementsIn(savedEntities);
        assertEquals(2, savedEntitiesList.size());
        assertEquals(PREPOPULATED_OPERATION_COUNT + 2, repository.count());
    }

    @Test
    public void testUpdate() {
        // Arrange
        final OperationEntity entity = new OperationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreatedBy("anonymous");
        entity.setDependencies(new HashSet<>());
        entity.setState(OperationState.NEW);
        entity.setOperation(OperationType.CLEAR);
        entity.setTargetAgentId("test-agent-1");
        entity.setOperand("connection");
        entity.setArgs(new HashMap<>());
        entity.getArgs().put("name", "test-connection-1");

        // Act
        final OperationEntity savedEntity = repository.save(entity);
        entityManager.flush();

        savedEntity.setState(OperationState.QUEUED);
        savedEntity.setArgs(new HashMap<>());
        savedEntity.getArgs().put("name", "test-connection-2");

        repository.save(savedEntity);
        entityManager.flush();

        final Optional<OperationEntity> updatedEntityOptional = repository.findById(entity.getId());

        // Assert
        assertTrue(updatedEntityOptional.isPresent());
        final OperationEntity updatedEntity = updatedEntityOptional.get();
        assertNotEquals(updatedEntity.getCreated(), updatedEntity.getUpdated());
        assertEquals(1, updatedEntity.getArgs().size());
        assertEquals("test-connection-2", updatedEntity.getArgs().get("name"));
        assertEquals(OperationState.QUEUED, updatedEntity.getState());
    }

}
