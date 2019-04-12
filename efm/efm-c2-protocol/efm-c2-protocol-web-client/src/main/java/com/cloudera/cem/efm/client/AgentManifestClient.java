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

import com.cloudera.cem.efm.model.AgentManifest;

import java.io.IOException;
import java.util.List;

/**
 * A client for interacting with {@link AgentManifest}s.
 */
public interface AgentManifestClient {

    /**
     * Creates the provided Agent Manifest and associates with the provided agent class
     *
     * @param agentManifest the {@link AgentManifest} to create
     * @param agentClass    the agentClass to associate with this manifest
     * @return the agent manifest with its associated identifier
     */
    AgentManifest createAgentManifest(final AgentManifest agentManifest, final String agentClass) throws C2Exception, IOException;

    /**
     * Creates the provided Agent Manifest.
     *
     * @param agentManifest the {@link AgentManifest} to create
     * @return the agent manifest with its associated identifier
     */
    AgentManifest createAgentManifest(final AgentManifest agentManifest) throws C2Exception, IOException;

    /**
     * Provides a listing of all agent manifests
     *
     * @return a listing of all agent manifests
     */
    List<AgentManifest> getAgentManifests() throws C2Exception, IOException;

    /**
     * Gets the agent manifest associated with the specified name
     *
     * @param manifestName the name of the agent manifest to retrieve
     * @return the agent manifest with the associated name
     */
    AgentManifest getAgentManifest(final String manifestName) throws C2Exception, IOException;

    /**
     * Deletes the agent manifest with the specified name
     *
     * @param manifestName of the agent class to delete
     * @return the deleted agent manifest
     */
    AgentManifest deleteAgentManifest(final String manifestName) throws C2Exception, IOException;

}
