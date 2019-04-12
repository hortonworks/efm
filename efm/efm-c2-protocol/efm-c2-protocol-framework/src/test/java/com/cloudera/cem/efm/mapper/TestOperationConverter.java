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
package com.cloudera.cem.efm.mapper;

import com.cloudera.cem.efm.db.entity.OperationEntity;
import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.Operation;
import com.cloudera.cem.efm.model.OperationState;
import com.cloudera.cem.efm.model.OperationType;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestOperationConverter extends ModelMapperTest {

    @Test
    public void testMapOperationToEntity() {
        // Arrange
        final Operation operation = new Operation();
        operation.setIdentifier("operation-1");
        operation.setOperation(OperationType.CLEAR);
        operation.setOperand("connection");
        final Map<String, String> args = new HashMap<>();
        args.put("name", "connection-1");
        operation.setArgs(args);
        final Set<String> dependencies = new LinkedHashSet<>();
        dependencies.add("operation-0.1");
        dependencies.add("operation-0.2");
        operation.setDependencies(dependencies);
        operation.setTargetAgentId("agent-1");
        operation.setState(OperationState.READY);
        operation.setCreatedBy("anonymous");
        operation.setCreated(1L);
        operation.setUpdated(2L);

        // Act
        OperationEntity operationEntity = modelMapper.map(operation, OperationEntity.class);

        // Assert
        assertNotNull(operationEntity);
        assertEquals(operation.getIdentifier(), operationEntity.getId());
        assertEquals(operation.getOperation(), operationEntity.getOperation());
        assertEquals(operation.getOperand(), operationEntity.getOperand());
        assertEquals(operation.getArgs(), operationEntity.getArgs());
        assertEquals(operation.getDependencies(), operationEntity.getDependencies());
        assertEquals(operation.getTargetAgentId(), operationEntity.getTargetAgentId());
        assertEquals(operation.getState(), operationEntity.getState());
        assertEquals(operation.getCreatedBy(), operationEntity.getCreatedBy());
    }

    @Test
    public void testMapEntityToOperation() {
        // Arrange
        final OperationEntity operationEntity = arrangeTestOperationEntity();

        // Act
        Operation operation = modelMapper.map(operationEntity, Operation.class);

        // Assert
        assertNotNull(operationEntity);
        assertEquals(operationEntity.getId(), operation.getIdentifier());
        assertEquals(operationEntity.getOperation(), operation.getOperation());
        assertEquals(operationEntity.getOperand(), operation.getOperand());
        assertEquals(operationEntity.getArgs(), operation.getArgs());
        assertEquals(operationEntity.getDependencies(), operation.getDependencies());
        assertEquals(operationEntity.getTargetAgentId(), operation.getTargetAgentId());
        assertEquals(operationEntity.getState(), operation.getState());
        assertEquals(operationEntity.getCreatedBy(), operation.getCreatedBy());
        assertEquals(operationEntity.getCreated(), new Date(operation.getCreated()));
        assertEquals(operationEntity.getUpdated(), new Date(operation.getUpdated()));
    }

    @Test
    public void testMapPartialEntityToOperation() {
        // Arrange
        final OperationEntity operationEntity = arrangeTestOperationEntity();
        operationEntity.setDependencies(Collections.emptySet());  // intentionally empty set

        // Act
        Operation operation = modelMapper.map(operationEntity, Operation.class);

        // Assert
        assertNotNull(operationEntity);
        assertEquals(operationEntity.getId(), operation.getIdentifier());
        assertEquals(operationEntity.getOperation(), operation.getOperation());
        assertEquals(operationEntity.getOperand(), operation.getOperand());
        assertEquals(operationEntity.getArgs(), operation.getArgs());
        assertEquals(operationEntity.getTargetAgentId(), operation.getTargetAgentId());
        assertEquals(operationEntity.getState(), operation.getState());
        assertEquals(operationEntity.getCreatedBy(), operation.getCreatedBy());
        assertEquals(operationEntity.getCreated(), new Date(operation.getCreated()));
        assertEquals(operationEntity.getUpdated(), new Date(operation.getUpdated()));
        assertNull(operation.getDependencies());
    }

    @Test
    public void testMapEntityToC2Operation() {
        // Arrange
        final OperationEntity operationEntity = arrangeTestOperationEntity();

        // Act
        C2Operation c2Operation = modelMapper.map(operationEntity, C2Operation.class);

        // Assert
        assertNotNull(operationEntity);
        assertEquals(operationEntity.getId(), c2Operation.getIdentifier());
        assertEquals(operationEntity.getOperation(), c2Operation.getOperation());
        assertEquals(operationEntity.getOperand(), c2Operation.getOperand());
        assertEquals(operationEntity.getArgs(), c2Operation.getArgs());
        assertEquals(operationEntity.getDependencies(), c2Operation.getDependencies());
    }

    private static OperationEntity arrangeTestOperationEntity() {
        final OperationEntity operationEntity = new OperationEntity();
        operationEntity.setId("operation-1");
        operationEntity.setOperation(OperationType.CLEAR);
        operationEntity.setOperand("connection");
        final Map<String, String> args = new HashMap<>();
        args.put("name", "connection-1");
        operationEntity.setArgs(args);
        final Set<String> dependencies = new LinkedHashSet<>();
        dependencies.add("operation-0.1");
        dependencies.add("operation-0.2");
        operationEntity.setDependencies(dependencies);
        operationEntity.setTargetAgentId("agent-1");
        operationEntity.setState(OperationState.READY);
        operationEntity.setCreatedBy("anonymous");
        operationEntity.prePersist();  // to set audit timestamps

        return operationEntity;
    }

}
