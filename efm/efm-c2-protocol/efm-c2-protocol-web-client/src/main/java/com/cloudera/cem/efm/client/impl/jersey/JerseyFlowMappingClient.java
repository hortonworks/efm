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

import com.cloudera.cem.efm.client.FlowMappingClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.FlowMappings;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class JerseyFlowMappingClient extends AbstractJerseyClient implements FlowMappingClient {

    private static final String FLOW_MAPPING_TARGET_PATH = "/flow-mappings";

    private final WebTarget flowMappingTarget;

    public JerseyFlowMappingClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyFlowMappingClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.flowMappingTarget = baseTarget.path(FLOW_MAPPING_TARGET_PATH);
    }

    @Override
    public FlowMappings getFlowMappingListing() throws C2Exception, IOException {
        return executeAction("Error retrieving flow mappings", () -> getRequestBuilder(flowMappingTarget).get(FlowMappings.class));
    }

    @Override
    public FlowMapping getFlowMapping(String agentClassName) throws C2Exception, IOException {
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error retrieving flow mapping for agent class, " + agentClassName, () -> {
            final WebTarget target = flowMappingTarget
                    .path("/{name}")
                    .resolveTemplate("name", agentClassName);

            return getRequestBuilder(target).get(FlowMapping.class);
        });
    }

    @Override
    public URI createFlowMapping(FlowMapping flowMapping) throws C2Exception, IOException {
        if (flowMapping == null) {
            throw new IllegalArgumentException("Missing the required parameter 'flowMapping' when calling createFlowMapping");
        }

        final String agentClassName = flowMapping.getAgentClass();
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error creating flow mapping", () ->
                getRequestBuilder(flowMappingTarget)
                        .post(Entity.entity(flowMapping, MediaType.APPLICATION_JSON))
                        .getLocation()
        );
    }

    @Override
    public FlowMapping updateFlowMapping(String agentClassName, FlowMapping flowMapping) throws C2Exception, IOException {
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        if (flowMapping == null) {
            throw new IllegalArgumentException("Flow Mapping cannot be null");
        }

        return executeAction("Error updating flow mapping for agent class, " + agentClassName, () -> {
            final WebTarget target = flowMappingTarget
                    .path("/{agentClassName}")
                    .resolveTemplate("agentClassName", agentClassName);

            return getRequestBuilder(target).put(Entity.entity(flowMapping, MediaType.APPLICATION_JSON),
                    FlowMapping.class);
        });
    }

    @Override
    public FlowMapping deleteFlowMapping(String agentClassName) throws C2Exception, IOException {
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error deleting flow mapping for agent class, " + agentClassName, () -> {
            final WebTarget target = flowMappingTarget
                    .path("/{agentClassName}")
                    .resolveTemplate("agentClassName", agentClassName);

            return getRequestBuilder(target).delete(FlowMapping.class);
        });
    }

}
