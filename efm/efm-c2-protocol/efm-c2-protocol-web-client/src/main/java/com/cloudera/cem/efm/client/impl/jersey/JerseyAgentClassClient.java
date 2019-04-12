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

import com.cloudera.cem.efm.client.AgentClassClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.AgentClass;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyAgentClassClient extends AbstractJerseyClient implements AgentClassClient {

    private static final String AGENT_CLASSES_TARGET_PATH = "/agent-classes";

    private final WebTarget agentClassTarget;

    public JerseyAgentClassClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyAgentClassClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.agentClassTarget = baseTarget.path(AGENT_CLASSES_TARGET_PATH);
    }

    @Override
    public AgentClass createAgentClass(final AgentClass agentClass) throws C2Exception, IOException {

        if (agentClass == null) {
            throw new IllegalArgumentException("Missing the required parameter 'agentClass' when calling createAgentClass");
        }

        final String agentClassName = agentClass.getName();
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error creating agent class", () ->
                getRequestBuilder(agentClassTarget)
                .post(
                        Entity.entity(agentClass, MediaType.APPLICATION_JSON),
                        AgentClass.class
                )
        );
    }

    @Override
    public List<AgentClass> getAgentClasses() throws C2Exception, IOException {
        return executeAction("Error retrieving agent classes", () -> {
            final AgentClass[] agentClasses = getRequestBuilder(agentClassTarget).get(AgentClass[].class);
            return agentClasses == null ? Collections.emptyList() : Arrays.asList(agentClasses);
        });
    }

    @Override
    public AgentClass getAgentClass(String className) throws C2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error retrieving agent class", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).get(AgentClass.class);
        });
    }

    @Override
    public AgentClass replaceAgentClass(String className, AgentClass replacementClass) throws C2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error updating agent class", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).put(Entity.entity(replacementClass, MediaType.APPLICATION_JSON),
                    AgentClass.class);
        });
    }

    @Override
    public AgentClass deleteAgentClass(String className) throws C2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Class name cannot be blank");
        }

        return executeAction("Error deleting flow", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).delete(AgentClass.class);
        });
    }
}
