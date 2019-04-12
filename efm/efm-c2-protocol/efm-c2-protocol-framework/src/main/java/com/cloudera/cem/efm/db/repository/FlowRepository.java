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

import com.cloudera.cem.efm.db.entity.FlowEntity;
import com.cloudera.cem.efm.model.FlowFormat;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.Optional;

/**
 * Spring Data Repository for FlowEntity.
 */
public interface FlowRepository extends PagingAndSortingRepository<FlowEntity,String> {

    /**
     * Projection for when we want to return everything except the flow contents.
     */
    interface FlowSummary {
        String getId();
        String getRegistryUrl();
        String getRegistryBucketId();
        String getRegistryFlowId();
        Integer getRegistryFlowVersion();
        FlowFormat getFlowFormat();
        Date getCreated();
        Date getUpdated();
    }

    /**
     * Used to retrieve all flows without content using the FlowSummary projection.
     *
     * Needed to use "IdIsNotNull" to trick spring-data since we need a unique method name.
     *
     * @return all FlowSummary instances
     */
    Iterable<FlowSummary> findByIdIsNotNull();

    /**
     * Used to retrieve a FlowSummary by flow id.
     *
     * Needed to use "AndRegistryUrlNotNull" to trick spring-data since we need a unique method name.
     *
     * @param id the flow id
     * @return the optional FlowSummary with the given id, or an empty optional if one doesn't exist
     */
    Optional<FlowSummary> findByIdAndRegistryUrlNotNull(String id);

}
