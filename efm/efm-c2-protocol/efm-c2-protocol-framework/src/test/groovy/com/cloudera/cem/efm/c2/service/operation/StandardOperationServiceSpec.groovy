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
package com.cloudera.cem.efm.service.operation

import com.google.common.graph.Graph
import com.cloudera.cem.efm.db.entity.OperationEntity
import com.cloudera.cem.efm.db.repository.OperationRepository
import com.cloudera.cem.efm.exception.ResourceNotFoundException
import com.cloudera.cem.efm.mapper.OptionalModelMapper
import com.cloudera.cem.efm.model.C2Operation
import com.cloudera.cem.efm.model.Operation
import com.cloudera.cem.efm.model.OperationState
import com.cloudera.cem.efm.model.OperationType
import com.cloudera.cem.efm.service.SpecUtil
import com.cloudera.cem.efm.service.event.EventService
import org.assertj.core.util.IterableUtil
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import javax.validation.Validation
import javax.validation.Validator

class StandardOperationServiceSpec extends Specification {

    static OptionalModelMapper modelMapper
    static Validator validator
    def eventService = Mock(EventService)
    OperationRepository operationRepository
    StandardOperationService operationService

    def setupSpec() {
        validator = Validation.buildDefaultValidatorFactory().getValidator()
        modelMapper = SpecUtil.buildOptionalModelMapper()
        TestableOperationEntity.addModelMapperMappings(modelMapper)
    }

    def setup() {
        operationRepository = Mock(OperationRepository)

        operationService = new StandardOperationService(
                operationRepository,
                eventService,
                validator,
                modelMapper)
    }

    //********************************
    //***  Operation CRUD methods  ***
    //********************************

    def "create operation"() {

        when: "arg is null"
        operationService.createOperation(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "arg is invalid"
        operationService.createOperation(new Operation())

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "operation is created with invalid dependency id"
        operationRepository.save(_ as OperationEntity) >> { OperationEntity e -> return e }
        operationRepository.existsById("missing-operation-id") >> false
        operationService.createOperation(
                new Operation([
                        operation: OperationType.DESCRIBE,
                        targetAgentId: "agent1",
                        state: OperationState.NEW,
                        dependencies: ["missing-operation-id"]
                ]))

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "valid operation is created"
        operationRepository.save(_ as OperationEntity) >> { OperationEntity e -> return e }
        operationRepository.existsById("operation-dependency") >> true
        def created = operationService.createOperation(
                new Operation([
                        operation: OperationType.DESCRIBE,
                        targetAgentId: "agent1",
                        state: OperationState.NEW,
                        dependencies: ["operation-dependency"]
                ]))

        then: "created operation is returned, generated id"
        with(created) {
            targetAgentId == "agent1"
            identifier != null
        }

    }

    def "get operations"() {

        setup:
        OperationEntity op1 = new OperationEntity([
                id: "operation1",
                operation: OperationType.DESCRIBE,
                targetAgentId: "agent1",
                state: OperationState.NEW
        ])
        OperationEntity op2 = new OperationEntity([
                id: "operation2",
                operation: OperationType.RESTART,
                targetAgentId: "agent2",
                state: OperationState.NEW
        ])
        operationRepository.findAll() >> [op1, op2]
        operationRepository.findByTargetAgentId(_ as String) >> [op2]


        when: "get all operations"
        def operations = operationService.getOperations()

        then:
        operations.size() == 2


        when: "get operations for agent2"
        operations = operationService.getOperationsByAgent("agent2")

        then:
        operations.size() == 1
        operations.get(0).identifier == "operation2"

    }

    def "get operation"() {

        when: "operation does not exist"
        operationRepository.findById("operation0") >> Optional.empty()
        def operation1 = operationService.getOperation("operation1")

        then: "empty optional is returned"
        !operation1.isPresent()


        when: "operation exists"
        operationRepository.findById("operation1") >> Optional.of(new OperationEntity([
                id: "operation1",
                operation: OperationType.DESCRIBE,
                targetAgentId: "agent1",
                state: OperationState.NEW
        ]))
        def operation2 = operationService.getOperation("operation1")

        then: "operation is returned"
        assert operation2.isPresent()
        with(operation2.get()) {
            identifier == "operation1"
            operation == OperationType.DESCRIBE
            targetAgentId == "agent1"
            state == OperationState.NEW
        }

    }

    def "delete operation"() {

        when: "operation does not exist"
        operationRepository.findById("operation0") >> Optional.empty()
        operationService.deleteOperation("operation0")

        then:
        thrown ResourceNotFoundException


        when: "operation exists"
        operationRepository.findById("operation1") >> Optional.of(new OperationEntity([id: "operation1"]))
        Operation deleted = operationService.deleteOperation("operation1")

        then:
        with(deleted) {
            identifier == "operation1"
        }

    }

    //******************************
    //***  Higher level methods  ***
    //******************************

    def "update operation state"() {

        when: "operation does not exist"
        operationRepository.findById("operation0") >> Optional.empty()
        operationService.updateOperationState("operation0", OperationState.DONE)

        then:
        thrown ResourceNotFoundException


        when: "operation exists"
        operationRepository.findById("operation1") >> Optional.of(new OperationEntity([id: "operation1", operation: OperationType.UPDATE]))
        operationRepository.save(_ as OperationEntity) >> { OperationEntity e -> return e }
        Operation updated = operationService.updateOperationState("operation1", OperationState.DONE)

        then:
        with(updated) {
            identifier == "operation1"
            state == OperationState.DONE
        }

    }

    def "update operation state to FAILED or CANCELLED"() {

        given: "operation2 dependent on operation1"
        def operation1 = new OperationEntity([id: "operation1", operation: OperationType.UPDATE, state: OperationState.DEPLOYED])
        def operation2 = new OperationEntity([id: "operation2", operation: OperationType.UPDATE, state: OperationState.READY, dependencies: ["operation1"]])
        operationRepository.findById("operation1") >> Optional.of(operation1)
        operationRepository.findById("operation2") >> Optional.of(operation2)
        operationRepository.findByDependenciesContains("operation1") >> [operation2]
        operationRepository.findByDependenciesContains("operation2") >> []
        operationRepository.save(_ as OperationEntity) >> { OperationEntity e -> return e }


        when: "operation1 fails"
        operationService.updateOperationState("operation1", OperationState.FAILED)

        then: "operation 2 is automatically cancelled"
        1 * operationRepository.saveAll([ new OperationEntity([id: "operation2", state: OperationState.CANCELLED, dependencies: ["operation1"]]) ])


        when: "operation1 is cancelled"
        operationService.updateOperationState("operation1", OperationState.CANCELLED)

        then: "operation 2 is automatically cancelled"
        1 * operationRepository.saveAll([ new OperationEntity([id: "operation2", state: OperationState.CANCELLED, dependencies: ["operation1"]]) ])

    }

    def "build dependency graph"() {

        given: "operations repository has 8 operations with some dependencies"
        def operations = setupOperationRepository()
        def op1 = operations['op1']
        def op2 = operations['op2']
        def op3 = operations['op3']
        def op4 = operations['op4']
        def op5 = operations['op5']
        def op6 = operations['op6']
        def op7 = operations['op7']
        def op8 = operations['op8']


        when: "graph is built for op5, not stopping at completed dependencies"
        Graph<OperationEntity> graph = operationService.buildDependencyGraph(op5, false)

        then: "correct graph for op1-op5 is returned"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 5
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op1, op2, op3, op4, op5]
        graph.edges().size() == 5
        graph.hasEdgeConnecting(op5, op4)
        graph.hasEdgeConnecting(op5, op3)
        graph.hasEdgeConnecting(op4, op2)
        graph.hasEdgeConnecting(op3, op2)
        graph.hasEdgeConnecting(op2, op1)


        when: "graph is built for op5, stopping at completed dependencies"
        graph = operationService.buildDependencyGraph(op5, true)

        then: "correct graph for op2-op5 is returned"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 4
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op2, op3, op4, op5]
        graph.edges().size() == 4
        graph.hasEdgeConnecting(op5, op4)
        graph.hasEdgeConnecting(op5, op3)
        graph.hasEdgeConnecting(op4, op2)
        graph.hasEdgeConnecting(op3, op2)


        when: "graph is built for op8"
        graph = operationService.buildDependencyGraph(op8, false)

        then: "correct graph for op6-op8 is returned, with node ordering based on timestamp"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 3
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op6, op8, op7]
        graph.edges().size() == 2
        graph.hasEdgeConnecting(op7, op6)
        graph.hasEdgeConnecting(op8, op7)


        when: "graph is built for op2 and op7"
        graph = operationService.buildDependencyGraph([op7, op2], false)

        then: "correct graph is returned for op1-op2, op6-op7"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 4
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op1, op2, op6, op7]
        graph.edges().size() == 2
        graph.hasEdgeConnecting(op2, op1)
        graph.hasEdgeConnecting(op7, op6)

    }

    def "build dependent graph"() {

        given: "operations repository has 8 operations with some dependencies"
        def operations = setupOperationRepository()
        def op1 = operations['op1']
        def op2 = operations['op2']
        def op3 = operations['op3']
        def op4 = operations['op4']
        def op5 = operations['op5']
        def op6 = operations['op6']
        def op7 = operations['op7']
        def op8 = operations['op8']

        when: "graph is built for op1"
        Graph<OperationEntity> graph = operationService.buildDependentGraph(op1)

        then: "correct graph for op1-op5 is returned"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 5
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op1, op2, op3, op4, op5]
        graph.edges().size() == 5
        graph.hasEdgeConnecting(op5, op4)
        graph.hasEdgeConnecting(op5, op3)
        graph.hasEdgeConnecting(op4, op2)
        graph.hasEdgeConnecting(op3, op2)
        graph.hasEdgeConnecting(op2, op1)


        when: "graph is built for op6"
        graph = operationService.buildDependentGraph(op6)

        then: "correct graph for op6-op8 is returned, with node ordering based on timestamp"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 3
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op6, op8, op7]
        graph.edges().size() == 2
        graph.hasEdgeConnecting(op7, op6)
        graph.hasEdgeConnecting(op8, op7)


        when: "graph is built for op2 and op7"
        graph = operationService.buildDependentGraph([op7, op2])

        then: "correct graph is returned for op2-op5, op7-op8"
        graph != null
        graph.isDirected()
        graph.nodes().size() == 6
        IterableUtil.nonNullElementsIn(graph.nodes()) == [op2, op3, op4, op5, op8, op7]
        graph.edges().size() == 5
        graph.hasEdgeConnecting(op3, op2)
        graph.hasEdgeConnecting(op4, op2)
        graph.hasEdgeConnecting(op5, op3)
        graph.hasEdgeConnecting(op5, op4)
        graph.hasEdgeConnecting(op8, op7)

    }


    def "getC2OperationsListForAgent"() {

        given: "operations repository has 8 operations with some dependencies"
        setupOperationRepository()
        List<C2Operation> agentOpQueue


        when:
        agentOpQueue = operationService.getC2OperationsListForAgent("agent0")

        then:
        agentOpQueue == []


        when:
        agentOpQueue = operationService.getC2OperationsListForAgent("agent1")

        then:
        agentOpQueue != null
        agentOpQueue.size() == 3
        agentOpQueue.get(0).getIdentifier() == "op3"
        agentOpQueue.get(0).getDependencies() == null || agentOpQueue.get(0).getDependencies().isEmpty()
        agentOpQueue.get(1).getIdentifier() == "op4"
        agentOpQueue.get(1).getDependencies() == null || agentOpQueue.get(1).getDependencies().isEmpty()
        agentOpQueue.get(2).getIdentifier() == "op5"
        agentOpQueue.get(2).getDependencies().size() == 2
        agentOpQueue.get(2).getDependencies().containsAll(["op3", "op4"])

        
        when: "maxBatchSize is used"
        agentOpQueue = operationService.getC2OperationsListForAgent("agent1", 2)

        then:
        agentOpQueue != null
        agentOpQueue.size() == 2
        agentOpQueue.get(0).getIdentifier() == "op3"
        agentOpQueue.get(0).getDependencies() == null || agentOpQueue.get(0).getDependencies().isEmpty()
        agentOpQueue.get(1).getIdentifier() == "op4"
        agentOpQueue.get(1).getDependencies() == null || agentOpQueue.get(1).getDependencies().isEmpty()

    }


    // Helper methods

    private Map<String, OperationEntity> setupOperationRepository() {
        def operations = new HashMap<String, OperationEntity>();
        createOperationEntityAndAddToMap(operations, "op1", "agent1", 1L, [], OperationState.DONE)
        createOperationEntityAndAddToMap(operations, "op2", "agent0", 2L, ["op1"], OperationState.DONE)
        createOperationEntityAndAddToMap(operations, "op3", "agent1", 3L, ["op2"], OperationState.QUEUED)
        createOperationEntityAndAddToMap(operations, "op4", "agent1", 4L, ["op2"], OperationState.QUEUED)
        createOperationEntityAndAddToMap(operations, "op5", "agent1", 5L, ["op3", "op4"], OperationState.QUEUED)
        createOperationEntityAndAddToMap(operations, "op6", "agent1", 6L, [], OperationState.NEW)
        createOperationEntityAndAddToMap(operations, "op7", "agent1", 7L, ["op6"], OperationState.NEW)
        // Simulate op8 was created after the system clock was changed to an earlier time
        createOperationEntityAndAddToMap(operations, "op8", "agent1", 6L, ["op7"], OperationState.NEW)

        operationRepository.existsById(_) >> { String id -> operations.containsKey(id) }
        operationRepository.findById(_) >> { String id -> Optional.ofNullable(operations.get(id)) }
        operationRepository.findAllById(_ as Iterable<String>) >> { Iterable<String> ids ->
            List<OperationEntity> matchedEntities = new ArrayList<>(ids.size())
            ids.forEach({
                def value = operations.get(it)
                if (value != null) {
                    matchedEntities.add(value)
                }
            })
            return matchedEntities
        }
        operationRepository.findByTargetAgentId(_ as String) >> { String id ->
            List<OperationEntity> matchedEntities = new ArrayList<>()
            operations.values().forEach({
                if (it.getTargetAgentId() != null && it.getTargetAgentId().equals(id)) {
                    matchedEntities.add(it)
                }
            })
            return matchedEntities
        }
        operationRepository.findByDependenciesContains(_) >> { String id ->
            List<OperationEntity> matchedEntities = new ArrayList<>()
            operations.values().forEach({
                if (it.getDependencies() != null && it.getDependencies().contains(id)) {
                    matchedEntities.add(it)
                }
            })
            return matchedEntities
        }

        return operations
    }

    private static OperationEntity createOperationEntityAndAddToMap(
            Map<String, OperationEntity> operationsMap,
            String id,
            String targetAgentId,
            long createdMillis,
            dependencies,
            OperationState state) {
        OperationEntity e = new TestableOperationEntity(
                id: id,
                targetAgentId: targetAgentId,
                dependencies: dependencies,
                created: new Date(createdMillis),
                state: state)
        operationsMap.put(id, e)
        return e
    }



}
