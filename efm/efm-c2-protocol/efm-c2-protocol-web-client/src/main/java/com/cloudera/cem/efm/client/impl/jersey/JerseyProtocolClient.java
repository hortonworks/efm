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

import com.cloudera.cem.efm.client.ProtocolClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.C2HeartbeatResponse;
import com.cloudera.cem.efm.model.C2OperationAck;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JerseyProtocolClient extends AbstractJerseyClient implements ProtocolClient {

    private static final String C2_PROTOCOL_TARGET_PATH = "/c2-protocol";

    private final WebTarget c2ProtocolTarget;

    public JerseyProtocolClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyProtocolClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.c2ProtocolTarget = baseTarget.path(C2_PROTOCOL_TARGET_PATH);
    }

    @Override
    public C2HeartbeatResponse heartbeat(C2Heartbeat c2Heartbeat) throws C2Exception, IOException {
        if (c2Heartbeat == null) {
            throw new IllegalArgumentException("Heartbeat cannot be null.");
        }

        return executeAction("Error sending heartbeat", () -> {
            final WebTarget target = c2ProtocolTarget
                    .path("/heartbeat");

            return getRequestBuilder(target).put(Entity.entity(c2Heartbeat, MediaType.APPLICATION_JSON),
                    C2HeartbeatResponse.class);
        });
    }

    @Override
    public boolean acknowledge(C2OperationAck operationAck) throws C2Exception, IOException {
        if (operationAck == null) {
            throw new IllegalArgumentException("Operation Acknowledgement cannot be null.");
        }

        return executeAction("Unable to send operation acknowledgement.", () -> {
            final WebTarget target = c2ProtocolTarget
                    .path("/acknowledge");

            return getRequestBuilder(target).put(Entity.entity(operationAck, MediaType.APPLICATION_JSON),
                    Boolean.class);
        });
    }

}
