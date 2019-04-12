/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.client;

import com.cloudera.cem.efm.model.Operation;

import java.io.IOException;
import java.util.List;

/**
 * A client for interacting with {@link Operation}s
 */
public interface OperationClient {

    /**
     * Creates an operation
     *
     * @param operation the operation to create
     * @return the created operation with its associated identifier
     */
    Operation createOperation(final Operation operation) throws C2Exception, IOException;

    /**
     * Gets all operations
     *
     * @return the operations
     */
    List<Operation> getOperations() throws C2Exception, IOException;

    /**
     * Gets an operation with the specified identifier
     *
     * @param id of the operation to retrieve
     * @return the operation
     */
    Operation getOperation(final String id) throws C2Exception, IOException;

    /**
     * Deletes an operation with the specified identifier
     *
     * @param id the identifier of the operation to delete
     * @return the deleted operation
     */
    Operation deleteOperation(final String id) throws C2Exception, IOException;

}
