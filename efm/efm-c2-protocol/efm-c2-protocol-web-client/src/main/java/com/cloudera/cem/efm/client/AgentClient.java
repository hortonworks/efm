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

import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentInfo;

import java.io.IOException;
import java.util.List;

/**
 * A client for interacting with {@link Agent}s.
 */
public interface AgentClient {

    /**
     * Creates an agent from the associated AgentInfo.
     *
     * @param agentInfo the {@link AgentInfo} to create
     * @return the agent with its associated identifier
     */
    Agent createAgent(final AgentInfo agentInfo) throws C2Exception, IOException;

    /**
     * Provides a listing of all Agents
     *
     * @return a listing of all agents
     */
    List<Agent> getAgents() throws C2Exception, IOException;

    /**
     * Gets the agent associated with the specified identifier
     *
     * @param id the id of the agent to retrieve
     * @return the agent with the associated name
     */
    Agent getAgent(final String id) throws C2Exception, IOException;

    /**
     * Updates an agent
     *
     * @param id               the id of the agent to update
     * @param replacementAgent the new {@link AgentInfo} to be associated with the specified id
     * @return the updated agent
     */
    Agent replaceAgent(final String id, final AgentInfo replacementAgent) throws C2Exception, IOException;

    /**
     * Deletes the agent with the specified id
     *
     * @param id of the agent to delete
     * @return the deleted agent class
     */
    Agent deleteAgent(final String id) throws C2Exception, IOException;

}
