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

import com.cloudera.cem.efm.client.AgentClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentInfo;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyAgentClient extends AbstractJerseyClient implements AgentClient {

    private static final String AGENT_TARGET_PATH = "/agents";

    private final WebTarget agentTarget;

    public JerseyAgentClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyAgentClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.agentTarget = baseTarget.path(AGENT_TARGET_PATH);
    }

    @Override
    public Agent createAgent(AgentInfo agentInfo) throws C2Exception, IOException {
        if (agentInfo == null) {
            throw new IllegalArgumentException("Missing the required parameter 'agentInfo' when calling createAgent");
        }

        return executeAction("Error creating agent", () ->
                getRequestBuilder(agentTarget)
                        .post(
                                Entity.entity(agentInfo, MediaType.APPLICATION_JSON),
                                Agent.class
                        )
        );
    }

    @Override
    public List<Agent> getAgents() throws C2Exception, IOException {
        return executeAction("Error retrieving agents", () -> {
            final Agent[] agents = getRequestBuilder(agentTarget).get(Agent[].class);
            return agents == null ? Collections.emptyList() : Arrays.asList(agents);
        });
    }

    @Override
    public Agent getAgent(String id) throws C2Exception, IOException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Agent idcannot be blank");
        }

        return executeAction("Error retrieving agent", () -> {
            final WebTarget target = agentTarget
                    .path("/{id}")
                    .resolveTemplate("id", id);

            return getRequestBuilder(target).get(Agent.class);
        });
    }

    @Override
    public Agent replaceAgent(String id, AgentInfo replacementAgent) throws C2Exception, IOException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Agent id cannot be blank");
        }

        return executeAction("Error updating agent", () -> {
            final WebTarget target = agentTarget
                    .path("/{id}")
                    .resolveTemplate("id", id);

            return getRequestBuilder(target).put(Entity.entity(id, MediaType.APPLICATION_JSON),
                    Agent.class);
        });
    }

    @Override
    public Agent deleteAgent(String id) throws C2Exception, IOException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Agent id cannot be blank");
        }

        return executeAction("Error deleting agent", () -> {
            final WebTarget target = agentTarget
                    .path("/{id}")
                    .resolveTemplate("id", id);

            return getRequestBuilder(target).delete(Agent.class);
        });
    }
}
