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
package com.cloudera.cem.efm.client.impl.jersey;

import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.client.OperationClient;
import com.cloudera.cem.efm.model.Operation;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyOperationClient extends AbstractJerseyClient implements OperationClient {

    private static final String C2_PROTOCOL_TARGET_PATH = "/operations";

    private final WebTarget operationTarget;

    public JerseyOperationClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyOperationClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.operationTarget = baseTarget.path(C2_PROTOCOL_TARGET_PATH);
    }


    @Override
    public Operation createOperation(Operation operation) throws C2Exception, IOException {
        if (operation == null) {
            throw new IllegalArgumentException("Missing the required parameter 'operation' when calling createOperation");
        }

        return executeAction("Error creating agent class", () ->
                getRequestBuilder(operationTarget)
                        .post(
                                Entity.entity(operation, MediaType.APPLICATION_JSON),
                                Operation.class
                        )
        );
    }

    @Override
    public List<Operation> getOperations() throws C2Exception, IOException {
        return executeAction("Error retrieving pending operations", () -> {
            final Operation[] operations = getRequestBuilder(operationTarget).get(Operation[].class);
            return operations == null ? Collections.emptyList() : Arrays.asList(operations);
        });
    }

    @Override
    public Operation getOperation(String id) throws C2Exception, IOException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Pending Operation id cannot be blank");
        }

        return executeAction("Error retrieving pending operation", () -> {
            final WebTarget target = operationTarget
                    .path("/{id}")
                    .resolveTemplate("id", id);

            return getRequestBuilder(target).get(Operation.class);
        });
    }

    @Override
    public Operation deleteOperation(String id) throws C2Exception, IOException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Pending Operation id cannot be blank");
        }

        return executeAction("Error deleting pending operation", () -> {
            final WebTarget target = operationTarget
                    .path("/{id}")
                    .resolveTemplate("id", id);

            return getRequestBuilder(target).delete(Operation.class);
        });
    }
}
