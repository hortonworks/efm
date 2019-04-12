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
import com.cloudera.cem.efm.client.FlowClient;
import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.FlowFormat;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.model.FlowSummaries;
import com.cloudera.cem.efm.model.FlowSummary;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JerseyFlowClient extends AbstractJerseyClient implements FlowClient {

    private static final String FLOWS_TARGET_PATH = "/flows";

    private final WebTarget flowsTarget;

    public JerseyFlowClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyFlowClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.flowsTarget = baseTarget.path(FLOWS_TARGET_PATH);
    }

    @Override
    public FlowSummary createFlow(String yaml) throws C2Exception, IOException {
        if (StringUtils.isBlank(yaml)) {
            throw new IllegalArgumentException("Missing the required parameter 'flowContent' when calling createFlow.  Cannot be null or empty.");
        }

        return executeAction("Error creating flow", () ->
                getRequestBuilder(flowsTarget)
                        .post(
                                Entity.entity(yaml, FlowFormat.YAML_V2_TYPE.getHeaderValue()),
                                FlowSummary.class
                        )
        );
    }

    @Override
    public FlowSummary createFlow(FlowSnapshot flowSnapshot) throws C2Exception, IOException {
        if (flowSnapshot == null){
            throw new IllegalArgumentException("FlowSnapshot cannot be null or empty");
        }

        return executeAction("Error creating flow", () ->
                getRequestBuilder(flowsTarget)
                        .post(
                                Entity.entity(flowSnapshot, FlowFormat.FLOW_SNAPSHOT_JSON_V1_TYPE.getHeaderValue()),
                                FlowSummary.class
                        )
        );
    }

    @Override
    public FlowSummaries getFlowListing() throws C2Exception, IOException {
        return executeAction("Error retrieving flows", () -> getRequestBuilder(flowsTarget).get(FlowSummaries.class));
    }

    @Override
    public String getFlowYaml(String identifier) throws C2Exception, IOException {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be blank");
        }

        return executeAction("Error retrieving flow with identifier: " + identifier, () -> {
            final WebTarget target = flowsTarget
                    .path("/{id}/content")
                    .resolveTemplate("id", identifier);

            return getRequestBuilder(target)
                    .accept(FlowFormat.YAML_V2_TYPE.getHeaderValue())
                    .get(String.class);
        });
    }

    @Override
    public Flow getFlow(String identifier) throws C2Exception, IOException {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("Flow identifier cannot be blank");
        }

        return executeAction("Error retrieving flow with identifier: " + identifier, () -> {
            final WebTarget target = flowsTarget
                    .path("/{id}")
                    .resolveTemplate("id", identifier);

            return getRequestBuilder(target)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(Flow.class);
        });
    }

}
