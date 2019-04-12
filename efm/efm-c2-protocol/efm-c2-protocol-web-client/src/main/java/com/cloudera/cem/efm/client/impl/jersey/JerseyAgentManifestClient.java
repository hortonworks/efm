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

import com.cloudera.cem.efm.client.AgentManifestClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.AgentManifest;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyAgentManifestClient extends AbstractJerseyClient implements AgentManifestClient {

    private static final String AGENT_MANIFESTS_TARGET_PATH = "/agent-manifests";

    private final WebTarget agentManifestTarget;

    public JerseyAgentManifestClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyAgentManifestClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.agentManifestTarget = baseTarget.path(AGENT_MANIFESTS_TARGET_PATH);
    }

    public AgentManifest createAgentManifest(final AgentManifest agentManifest) throws C2Exception, IOException {
        return this.createAgentManifest(agentManifest, null);
    }


    @Override
    public AgentManifest createAgentManifest(final AgentManifest agentManifest, final String agentClass) throws C2Exception, IOException {
        if (agentManifest == null) {
            throw new IllegalArgumentException("Missing the required parameter 'agentManifest' when calling createAgentManifest");
        }

        final WebTarget manifestTarget = StringUtils.isNotBlank(agentClass) ? agentManifestTarget.queryParam("class", agentClass) : agentManifestTarget;

        return executeAction("Error creating agent manifest", () ->
                getRequestBuilder(manifestTarget)
                .post(
                        Entity.entity(agentManifest, MediaType.APPLICATION_JSON), AgentManifest.class
                ));
    }

    @Override
    public List<AgentManifest> getAgentManifests() throws C2Exception, IOException {
        return executeAction("Error creating agent class", () -> {
            final AgentManifest[] agentManifests = getRequestBuilder(agentManifestTarget).get(AgentManifest[].class);
            return agentManifests == null ? Collections.emptyList() : Arrays.asList(agentManifests);
        });
    }

    @Override
    public AgentManifest getAgentManifest(String className) throws C2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }


        return executeAction("Error retrieving agent class", () -> {
            final WebTarget target = agentManifestTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).get(AgentManifest.class);
        });
    }

    @Override
    public AgentManifest deleteAgentManifest(String className) throws C2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Manifest name cannot be blank");
        }

        return executeAction("Error deleting flow", () -> {
            final WebTarget target = agentManifestTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).delete(AgentManifest.class);
        });
    }
}
