/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.entity.FDFlowEntity;
import com.cloudera.cem.efm.db.entity.FDFlowEventEntity;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowFormat;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

/**
 * Repository for accessing Flow Designer's flow events from the database.
 */
public interface FDFlowEventRepository extends PagingAndSortingRepository<FDFlowEventEntity,String> {

    /**
     * Used to select all columns of an event except the flow content for cases where we are returning a list.
     */
    interface FlowEventWithoutContent {
        String getId();
        FDFlowEventType getEventType();
        String getEventDescription();
        FDFlowFormat getFlowFormat();
        String getComponentId();
        String getUserIdentity();
        FDFlowEntity getFlow();
        BigInteger getFlowRevision();
        Date getCreated();
        Date getUpdated();
        String getRegistryUrl();
        String getRegistryBucketId();
        String getRegistryFlowId();
        Integer getRegistryVersion();
    }

    /**
     * Retrieves the flow event entities for the given flow id, ordered by flow revision ascending.
     * @param flow the flow
     * @return the list of flow events without the flow content on each event
     */
    Iterable<FlowEventWithoutContent> findByFlowOrderByFlowRevisionAsc(FDFlowEntity flow);

    /**
     * Retrieves the event for the given flow with the given revision.
     *
     * @param flow the flow
     * @param flowRevision the flow revision
     * @return the event entity or empty optional
     */
    Optional<FDFlowEventEntity> findByFlowAndFlowRevision(FDFlowEntity flow, BigInteger flowRevision);

    /**
     * Retrieves the latest flow event for the given flow id.
     *
     * @param flow the flow
     * @return the latest event
     */
    Optional<FDFlowEventEntity> findFirstByFlowOrderByFlowRevisionDesc(FDFlowEntity flow);

    /**
     * Retrieves the latest flow event for the given flow id with the given event type.
     *
     * @param flow the flow
     * @param eventType the type of event
     * @return the latest event with the given type for the given flow
     */
    Optional<FDFlowEventEntity> findFirstByFlowAndEventTypeOrderByFlowRevisionDesc(FDFlowEntity flow, FDFlowEventType eventType);

    /**
     * Deletes all events for the given flow, except the event with the id specified.
     *
     * Typically clients would first call findFirstByFlowOrderByFlowRevisionDesc to get the latest event id to keep.
     *
     * @param flow the flow the events are being deleted for
     * @param flowEventId the id of the event to keep for the given flow, typically the latest id.
     */
    void deleteByFlowAndIdNot(FDFlowEntity flow, String flowEventId);

    /**
     * Deletes all events for the given flow that do not have the specified event type.
     *
     * The primary use-case is to delete all events except the publish events.
     *
     * @param flow the flow the events are being deleted for
     * @param eventType the id of the event to keep for the given flow, typically the latest id.
     */
    void deleteByFlowAndEventTypeNot(FDFlowEntity flow, FDFlowEventType eventType);

    /**
     * Deletes events for a given flow where the revision of the event is greater than the passed in revision.
     *
     * @param flow the flow
     * @param flowRevision the revision to delete events after
     */
    void deleteByFlowAndFlowRevisionGreaterThan(FDFlowEntity flow, BigInteger flowRevision);

    /**
     * Returns the number of events for the given flow.
     *
     * @param flow the flow to count events for
     * @return the number of events for the flow
     */
    Long countByFlow(FDFlowEntity flow);

}
