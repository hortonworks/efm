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

import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.Operation;
import com.cloudera.cem.efm.model.OperationState;

import java.util.List;
import java.util.Optional;

/**
 *  Standard CRUD method semantics apply to these methods. That is:
 *
 *    - getWidgets: return List of Widget,
 *                  or empty List if no Widgets exist
 *
 *    - getWidget(String): return Optional Widget with matching id,
 *                         or empty Optional if no Widget with matching id exists
 *
 *    - createWidget(Widget): create Widget and assign it a generated id,
 *                            return created widget (including any fields that got generated such as id or creation timestamp),
 *                            throw IllegalStateException if Widget with matching id already exists
 *                            throw IllegalArgumentException if Widget is not valid (e.g., missing required fields)
 *
 *    - updateWidget(Widget): update Widget with the id to match the incoming Widget
 *                            return updated Widget
 *                            throw IllegalArgumentException if Widget is not valid (e.g., missing required fields. Note, id is required when updating existing Widget)
 *                            throw ResourceNotFoundException if no Widget with matching id exists
 *
 *    - deleteWidget(String): delete Widget with id,
 *                            return Widget that was deleted,
 *                            throw ResourceNotFoundException if no Widget with matching id exists
 *
 *  Any invalid arguments (eg, null where required) will result in an IllegalArgumentException
 */
public interface OperationService {

    //***********************************
    //***  Operation CRUD methods  ***
    //***********************************

    Operation createOperation(Operation operation);
    List<Operation> getOperations();
    List<Operation> getOperationsByAgent(String agentId);
    Optional<Operation> getOperation(String operationId);
    Operation updateOperationState(String operationId, OperationState state);
    Operation deleteOperation(String operationId);

    //******************************
    //***  Higher level methods  ***
    //******************************

    /**
     * Returns a list of C2Operations to send to an agent.
     *
     * <p>efmOperations are included in the returned list if and only if they
     * meet the following criteria:
     *
     * <ul>
     *   <li>{@code state == OperationState.QUEUED}</li>
     *   <li>all dependencies are satisfied</li>
     * </ul>
     *
     * <p>If a QUEUED operation's dependency has not been completed, or was
     * cancelled or failed, the dependent operation will be excluded from the
     * list returned as it cannot be executed.
     *
     * <p>If a QUEUED operation's dependency has been completed, that
     * dependency will be omitted from the returned C2Operation as it is
     * has been satisfied and the agent does not need to track it. The
     * dependencies included in the returned list are guaranteed to only
     * refer to other C2Operations in the returned list.
     *
     * <p>efmOperations in the returned list will be ordered in the order that they
     * should be executed, taking dependencies into account. If operations have the
     * same (or no) dependencies, they will be ordered based on creation timestamp.
     *
     * @param agentId The agent for which to get a list of queued operation
     * @return a sorted list of queued operations, with operation dependencies taken into account
     *
     * @see #getC2OperationsListForAgent(String, int)
     */
    default List<C2Operation> getC2OperationsListForAgent(String agentId) {
        return getC2OperationsListForAgent(agentId, Integer.MAX_VALUE);
    }

    /**
     * Returns a list of C2Operations to send to an agent, limited to the number
     * specified by {@code maxBatchSize}.
     *
     * <p>The semantics of this method are identical to
     * {@link #getC2OperationsListForAgent(String)},
     * with that added logic that limits the size of the list returned.
     *
     * @param agentId The agent for which to get a list of queued operation
     * @param maxBatchSize The maximum number of C2Operations to return
     * @return a sorted list of queued operations, with operation dependencies taken into account
     *
     * @see #getC2OperationsListForAgent(String)
     */
    List<C2Operation> getC2OperationsListForAgent(String agentId, int maxBatchSize);



}
