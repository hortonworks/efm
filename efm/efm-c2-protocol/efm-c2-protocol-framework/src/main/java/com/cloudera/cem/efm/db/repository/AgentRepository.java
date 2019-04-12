/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.entity.AgentEntity;
import com.cloudera.cem.efm.db.projection.IdAndTimestamp;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface AgentRepository extends PagingAndSortingRepository<AgentEntity, String> {

    Iterable<AgentEntity> findAllByAgentClass(String agentClassName);

    /**
     * Retrieve all agent last seen timestamps.
     *
     * @return all agent last seen timestamps as IdAndTimestamp projections
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.id, a.lastSeen) FROM AgentEntity a")
    Iterable<IdAndTimestamp> findAllAgentLastSeens();

    /**
     * Retrieve a given agent's last seen timestamp.
     *
     * @return an IdAndTimestamp instance, or empty Optional if the specified agent id is not found
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.id, a.lastSeen) FROM AgentEntity a WHERE a.id = ?1")
    Optional<IdAndTimestamp> findAgentLastSeenById(String agentId);

    /**
     * Retrieve all agent class last seen timestamps.
     *
     * @return all agent class last seen timestamps as IdAndTimestamp projections
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.agentClass, MAX(a.lastSeen)) FROM AgentEntity a GROUP BY a.agentClass")
    Iterable<IdAndTimestamp> findAllAgentClassLastSeens();

    /**
     * Retrieve an agent class's last seen timestamp.
     *
     * @return a IdAndTimestamp instance, or empty Optional if the specified agent class is not found
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.agentClass, MAX(a.lastSeen)) FROM AgentEntity a WHERE a.agentClass = ?1 GROUP BY a.agentClass")
    Optional<IdAndTimestamp> findAgentClassLastSeenById(String agentClass);

    /**
     * Retrieve all agent manifest last seen timestamps.
     *
     * @return all agent manifest last seen timestamps as IdAndTimestamp projections
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.agentManifestId, MAX(a.lastSeen)) FROM AgentEntity a GROUP BY a.agentManifestId")
    Iterable<IdAndTimestamp> findAllAgentManifestLastSeens();

    /**
     * Retrieve an agent manifests's last seen timestamp.
     *
     * @return a IdAndTimestamp instance, or empty Optional if the specified agent manifest is not found
     */
    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(a.agentManifestId, MAX(a.lastSeen)) FROM AgentEntity a WHERE a.agentManifestId = ?1 GROUP BY a.agentManifestId")
    Optional<IdAndTimestamp> findAgentManifestLastSeenById(String agentManifestId);

}
