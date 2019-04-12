/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.entity.HeartbeatEntity;
import com.cloudera.cem.efm.db.projection.IdAndNumber;
import com.cloudera.cem.efm.db.projection.IdAndTimestamp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface HeartbeatRepository extends PagingAndSortingRepository<HeartbeatEntity, String> {

// Partial work towards Issue #526, commented out for now.
//    /**
//     * Projection for when we want to return everything except the heartbeat contents.
//     */
//    interface HeartbeatSummary {
//        String getId();
//        String getDevice();
//        String getAgentManifestId();
//        String getAgentClass();
//        String getAgentId();
//        String getFlowId();
//        Date getCreated();
//    }
//
//    Iterable<HeartbeatSummary> findAll(Specification<HeartbeatEntity> specification);
//
//    Iterable<HeartbeatSummary> findAll(Specification<HeartbeatEntity> specification, Sort sort);
//
//    Page<HeartbeatSummary> findAll(Specification<HeartbeatEntity> specification, Pageable pageable);

    Optional<HeartbeatEntity> findById(String heartbeatId);

    Iterable<HeartbeatEntity> findByDeviceId(String deviceId);

    Iterable<HeartbeatEntity> findByAgentId(String agentId);

    void deleteByCreatedBefore(Date date);

    void deleteByAgentIdEqualsAndCreatedBefore(String agentId, Date date);

    void deleteByAgentIdIsNullAndCreatedBefore(Date date);

    @Modifying
    @Query("UPDATE HeartbeatEntity h SET h.content = null")
    void deleteAllContent();

    @Modifying
    @Query("UPDATE HeartbeatEntity h SET h.content = null " +
            " WHERE h.created < :date AND h.content IS NOT NULL")
    void deleteContentByCreatedBefore(@Param("date") Date date);

    @Modifying
    @Query("UPDATE HeartbeatEntity h SET h.content = null " +
            " WHERE h.agentId = :agentId AND h.created < :date AND h.content IS NOT NULL")
    void deleteContentByAgentIdEqualsAndCreatedBefore(@Param("agentId") String agentId, @Param("date") Date date);

    @Modifying
    @Query("UPDATE HeartbeatEntity h SET h.content = null " +
            " WHERE h.agentId IS NULL AND h.created < :date AND h.content IS NOT NULL")
    void deleteContentByAgentIdIsNullAndCreatedBefore(@Param("date") Date date);

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(h.id, h.created) " +
            " FROM HeartbeatEntity h WHERE h.agentId = :agentId ORDER BY h.created DESC")
    List<IdAndTimestamp> findHeartbeatTimestampsByAgentId(@Param("agentId") String agentId, Pageable pageable);

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(h.id, h.created) " +
            " FROM HeartbeatEntity h WHERE h.agentId IS NULL ORDER BY h.created DESC")
    List<IdAndTimestamp> findHeartbeatTimestampsByAgentIdIsNull(Pageable pageable);

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(h.id, h.created) " +
            " FROM HeartbeatEntity h WHERE h.content IS NOT NULL AND h.agentId = :agentId ORDER BY h.created DESC")
    List<IdAndTimestamp> findHeartbeatContentTimestampsByAgentId(@Param("agentId") String agentId, Pageable pageable);

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndTimestamp(h.id, h.created) " +
            " FROM HeartbeatEntity h WHERE h.content IS NOT NULL AND h.agentId IS NULL ORDER BY h.created DESC")
    List<IdAndTimestamp> findHeartbeatContentTimestampsByAgentIdIsNull(Pageable pageable);

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndNumber(h.agentId, COUNT(h)) " +
            " FROM HeartbeatEntity h GROUP BY h.agentId ORDER BY COUNT(h) DESC")
    Iterable<IdAndNumber> findHeartbeatCountsByAgentId();

    @Query("SELECT new com.cloudera.cem.efm.db.projection.IdAndNumber(h.agentId, COUNT(h)) " +
            " FROM HeartbeatEntity h WHERE h.content IS NOT NULL GROUP BY h.agentId ORDER BY COUNT(h) DESC")
    Iterable<IdAndNumber> findHeartbeatContentCountsByAgentId();

}
