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
package com.cloudera.cem.efm.service.operation;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.cloudera.cem.efm.db.entity.OperationEntity;
import com.cloudera.cem.efm.db.repository.OperationRepository;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.Operation;
import com.cloudera.cem.efm.model.OperationState;
import com.cloudera.cem.efm.service.BaseService;
import com.cloudera.cem.efm.service.event.EventService;
import com.cloudera.cem.efm.service.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class StandardOperationService extends BaseService implements OperationService {

    private static final Logger logger = LoggerFactory.getLogger(StandardOperationService.class);

    private static final String OPERATION_RESOURCE_TYPE = Operation.class.getSimpleName();

    private final OperationRepository operationRepository;
    private final EventService eventService;

    @Autowired
    public StandardOperationService(
            final OperationRepository operationRepository,
            final EventService eventService,
            final Validator validator,
            final OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.operationRepository = operationRepository;
        this.eventService = eventService;
    }


    //********************************
    //***  Operation CRUD methods  ***
    //********************************

    @Override
    public Operation createOperation(Operation operation) {
        validate(operation, "Cannot create operation");

        operation.setIdentifier(UUID.randomUUID().toString());

        // Verify operation dependencies
        if (operation.getDependencies() != null) {
            for (String opId : operation.getDependencies()) {
                if (!operationRepository.existsById(opId)) {
                    throw new IllegalArgumentException("Could not create operation due to invalid dependency operation id '" + opId + "'");
                }
            }
        }

        // Note: By requiring that operation ids in the dependency list must already exist, and
        // by making operation dependencies immutable (that is, there is no updateOperation(...)
        // method in this service, only operation state can change), we are ensuring the
        // dependency tree will always be a DAG. If these factors change in the future, every
        // save of an operation we will need to check for cyclic dependency graphs.

        OperationEntity operationEntity = modelMapper.map(operation, OperationEntity.class);
        operationEntity = operationRepository.save(operationEntity);
        final Operation savedOperation = modelMapper.map(operationEntity, Operation.class);

        eventService.createEvent(Event.builder()
                .level(EventSeverity.INFO)
                .eventType(EventType.OPERATION_CREATED)
                .message("C2 operation created: " + operationToEventMessageString(savedOperation))
                .eventDetail(OPERATION_RESOURCE_TYPE, savedOperation.getIdentifier())
                .eventSource("Server", "server-id")
                .build());

        return savedOperation;
    }

    @Override
    public List<Operation> getOperations() {
        final List<Operation> operations = new ArrayList<>();
        operationRepository.findAll().forEach(operationEntity -> {
            final Operation operation = modelMapper.map(operationEntity, Operation.class);
            operations.add(operation);
        });
        return operations;
    }

    @Override
    public List<Operation> getOperationsByAgent(String agentId) {
        logger.trace("Retrieving operations for agent {}", agentId);
        if (agentId == null) {
            throw new IllegalArgumentException("agentId cannot be null");
        }
        final List<Operation> operations = new ArrayList<>();
        operationRepository.findByTargetAgentId(agentId).forEach(operationEntity -> {
            final Operation operation = modelMapper.map(operationEntity, Operation.class);
            operations.add(operation);
        });
        return operations;
    }

    @Override
    public Optional<Operation> getOperation(String operationId) {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId cannot be null");
        }
        final Optional<OperationEntity> entityOptional = operationRepository.findById(operationId);
        final Optional<Operation> operationOptional = modelMapper.mapOptional(entityOptional, Operation.class);
        return operationOptional;
    }

    @Override
    public Operation deleteOperation(String operationId) {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId cannot be null");
        }

        final Optional<OperationEntity> entityOptional = operationRepository.findById(operationId);
        final OperationEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Operation not found with id " + operationId));
        operationRepository.deleteById(operationId);
        final Operation deletedOperation = modelMapper.map(entity, Operation.class);
        return deletedOperation;
    }

    //******************************
    //***  Higher level methods  ***
    //******************************

    @Override
    public Operation updateOperationState(final String operationId, final OperationState state) {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }

        // Update operation
        final Optional<OperationEntity> entityOptional = operationRepository.findById(operationId);
        OperationEntity updatedOperationEntity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Operation not found with id " + operationId));
        final OperationState fromState = updatedOperationEntity.getState();
        logger.debug("C2 operation state transition for operationId={}, fromState={}, toState={}", operationId, fromState, state);
        updatedOperationEntity.setState(state);

        // Update dependent operations
        final List<OperationEntity> updatedDependentOperationEntities = new ArrayList<>();
        if (state == OperationState.CANCELLED || state == OperationState.FAILED) {
            final Graph<OperationEntity> dependentsGraph = buildDependentGraph(updatedOperationEntity);
            dependentsGraph.nodes().forEach(dependentOperationEntity -> {
                if (!operationId.equals(dependentOperationEntity.getId())) {
                    dependentOperationEntity.setState(OperationState.CANCELLED);
                    updatedDependentOperationEntities.add(dependentOperationEntity);
                }
            });
        }

        // Save everything
        updatedOperationEntity = operationRepository.save(updatedOperationEntity);
        operationRepository.saveAll(updatedDependentOperationEntities);

        final Operation updatedOperation = modelMapper.map(updatedOperationEntity, Operation.class);

        eventService.createEvent(Event.builder()
                .level(EventSeverity.INFO)
                .eventType(EventType.OPERATION_UPDATED)
                .message("C2 operation state changed from " + fromState + " to " + state + ": " + operationToEventMessageString(updatedOperation))
                .eventDetail(OPERATION_RESOURCE_TYPE, updatedOperation.getIdentifier())
                .build());

        return updatedOperation;
    }

    @Override
    public List<C2Operation> getC2OperationsListForAgent(String agentId, int maxBatchSize) {
        // Start by getting the list of queued operations targeting the specified agent
        final List<OperationEntity> entitiesToConsider = new ArrayList<>();
        operationRepository.findByTargetAgentId(agentId).forEach(operationEntity -> {
            if (OperationState.QUEUED == operationEntity.getState()) {
                entitiesToConsider.add(operationEntity);
            }
        });

        // Build a graph based on the dependency relationships.
        // Note, this wil potentially pull in operations targeting other agents, or dependencies that are not queued.
        Graph<OperationEntity> dependencyGraph = buildDependencyGraph(entitiesToConsider, true);

        // Sort the dependency graph into a serial execution order that takes dependencies into account
        final List<OperationEntity> entitiesToConsiderInOrder = GraphUtil.reverseTopologicalSort(dependencyGraph);

        // Now walk the list and start building a list of operations that should be sent to the agent
        final List<OperationEntity> vettedListForAgent = new ArrayList<>();
        final Set<String> idsInVettedListForAgent = new HashSet<>();
        for (int i = 0; i < entitiesToConsiderInOrder.size() && i < maxBatchSize; i++) {
            final OperationEntity candidateOperation = entitiesToConsiderInOrder.get(i);

            if (!agentId.equals(candidateOperation.getTargetAgentId())) {
                continue;
            }
            if (candidateOperation.getState() != OperationState.QUEUED) {
                continue;
            }

            if (allDependenciesSatisfied(dependencyGraph, candidateOperation, idsInVettedListForAgent)) {

                // Based on "allDependenciesSatisfied" logic, we can assume any dependencies
                // of the candidate operation that are not in the vetted list are complete,
                // and can therefore be removed from what we send to the agent
                // (modifications not to be saved back to the repository though)
                candidateOperation.getDependencies().retainAll(idsInVettedListForAgent);

                vettedListForAgent.add(candidateOperation);
                idsInVettedListForAgent.add(candidateOperation.getId());
            }
        }

        // Map the final list of entities to C2Operation DTO objects and return to caller
        final List<C2Operation> c2OperationsForAgent = new ArrayList<>();
        vettedListForAgent.forEach(operationEntity -> {
            final C2Operation c2Operation = modelMapper.map(operationEntity, C2Operation.class);
            c2OperationsForAgent.add(c2Operation);
        });
        return c2OperationsForAgent;
    }

    private boolean allDependenciesSatisfied(Graph<OperationEntity> dependencyGraph, OperationEntity node, Set<String> dependencyOperationIdsToIgnore) {
        return dependencyGraph.successors(node).stream().allMatch(dependency ->
                (dependencyOperationIdsToIgnore != null && dependencyOperationIdsToIgnore.contains(dependency.getId()))
                        || dependency.getState() == OperationState.DONE
        );
    }


    //*****************************************
    //***  Dependency graph helper methods  ***
    //*****************************************

    Graph<OperationEntity> buildDependencyGraph(OperationEntity operationEntity, boolean stopAtCompletedDependencies) {
        return buildDependencyGraph(Collections.singletonList(operationEntity), stopAtCompletedDependencies);
    }

    Graph<OperationEntity> buildDependencyGraph(Iterable<OperationEntity> operationEntities, boolean stopAtCompletedDependencies) {

        MutableGraph<OperationEntity> dependencyGraph =
                GraphBuilder
                        .directed()
                        .allowsSelfLoops(false)
                        .nodeOrder(ElementOrder.sorted(new OperationEntityCreatedComparator()))
                        .build();

        operationEntities.forEach(entity -> addNodeAndDependencies(dependencyGraph, entity, stopAtCompletedDependencies));

        return dependencyGraph;
    }

    private void addNodeAndDependencies(MutableGraph<OperationEntity> dependencyGraph, OperationEntity node, boolean stopAtCompletedDependencies) {

        final Set<String> nodeDependencies = node.getDependencies();
        if (CollectionUtils.isEmpty(nodeDependencies)) {
            // base case
            dependencyGraph.addNode(node);
        } else {
            // recursive case
            nodeDependencies.stream()
                    .map(dependencyId ->
                            operationRepository.findById(dependencyId)
                                    .<IllegalStateException>orElseThrow(() ->
                                            new IllegalStateException("Could not build operation dependency graph because no operation exists with id " + dependencyId)))
                    .forEach(dependencyEntity -> {
                        dependencyGraph.putEdge(node, dependencyEntity);
                        if (!stopAtCompletedDependencies || dependencyEntity.getState() != OperationState.DONE) {
                            addNodeAndDependencies(dependencyGraph, dependencyEntity, stopAtCompletedDependencies);
                        }
                    });
        }
    }

    Graph<OperationEntity> buildDependentGraph(OperationEntity operationEntity) {
        return buildDependentGraph(Collections.singletonList(operationEntity));
    }

    Graph<OperationEntity> buildDependentGraph(Iterable<OperationEntity> operationEntities) {

        MutableGraph<OperationEntity> dependencyGraph =
                GraphBuilder
                        .directed()
                        .allowsSelfLoops(false)
                        .nodeOrder(ElementOrder.sorted(new OperationEntityCreatedComparator()))
                        .build();

        operationEntities.forEach(entity -> addNodeAndDependents(dependencyGraph, entity));

        return dependencyGraph;
    }

    private void addNodeAndDependents(MutableGraph<OperationEntity> dependencyGraph, OperationEntity node) {

        final Iterable<OperationEntity> nodeDependents = operationRepository.findByDependenciesContains(node.getId());
        if (!nodeDependents.iterator().hasNext()) {
            // base case
            dependencyGraph.addNode(node);
        } else {
            // recursive case
            nodeDependents.forEach(dependentEntity -> {
                dependencyGraph.putEdge(dependentEntity, node);
                addNodeAndDependents(dependencyGraph, dependentEntity);
            });
        }

    }

    private static String operationToEventMessageString(Operation op) {
        final StringBuilder builder = new StringBuilder();
        builder.append(op.getOperation().toString());
        builder.append(" ");
        if (op.getOperand() != null) {
            builder.append(op.getOperand());
            builder.append(" ");
        }
        final Map<String, String> args = op.getArgs();
        if (args != null && !args.isEmpty()) {
            for (String argKey : args.keySet()) {
                builder.append(argKey);
                builder.append("=");
                builder.append(args.get(argKey));
                builder.append(" ");
            }
        }
        builder.append(" agentId=");
        builder.append(op.getTargetAgentId());
        return builder.toString();
    }

}
