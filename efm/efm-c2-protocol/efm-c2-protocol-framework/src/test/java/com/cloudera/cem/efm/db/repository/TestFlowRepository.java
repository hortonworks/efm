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

import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.db.entity.FlowEntity;
import com.cloudera.cem.efm.model.FlowFormat;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestFlowRepository extends DatabaseTest {

    @Autowired
    private FlowRepository repository;

    @Test
    public void testGetAll() {
        assertEquals(2, getCount());
    }

    @Test
    public void testGetById() {
        final Optional<FlowEntity> result = repository.findById("1");
        assertTrue(result.isPresent());

        final FlowEntity entity = result.get();
        assertEquals("1", entity.getId());
        assertEquals("http://localhost:18080", entity.getRegistryUrl());
        assertEquals("Bucket1", entity.getRegistryBucketId());
        assertEquals("Flow1", entity.getRegistryFlowId());
        assertEquals(Integer.valueOf(1), entity.getRegistryFlowVersion());
        assertEquals("SHOULD BE YAML", entity.getFlowContent());
        assertEquals(FlowFormat.YAML_V2_TYPE, entity.getFlowFormat());

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(entity.getCreated()));
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(entity.getUpdated()));
    }

    @Test
    public void testSave() {
        final FlowEntity entity = new FlowEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRegistryUrl("https://localhost:18080");
        entity.setRegistryBucketId(UUID.randomUUID().toString());
        entity.setRegistryFlowId(UUID.randomUUID().toString());
        entity.setRegistryFlowVersion(1);
        entity.setFlowFormat(FlowFormat.YAML_V2_TYPE);

        final FlowEntity savedEntity = repository.save(entity);
        assertNotNull(savedEntity.getCreated());
        assertNotNull(savedEntity.getUpdated());
    }

    @Test
    public void testUpdate() {
        final Optional<FlowEntity> result = repository.findById("1");
        assertTrue(result.isPresent());

        final FlowEntity entity = result.get();

        // save the original dates so we can compare them later
        final Date originalCreateDate = entity.getCreated();
        final Date originalUpdateDate = entity.getUpdated();

        final String updatedRegistryUrl = "http://UPDATED:18080";
        assertNotEquals(updatedRegistryUrl, entity.getRegistryUrl());
        entity.setRegistryUrl(updatedRegistryUrl);

        repository.save(entity);
        entityManager.flush();  // so we can verify the effects of preUpdate

        final Optional<FlowEntity> updatedResult = repository.findById("1");
        assertTrue(updatedResult.isPresent());

        final FlowEntity updatedEntity = updatedResult.get();
        assertEquals(updatedRegistryUrl, updatedEntity.getRegistryUrl());
        assertEquals(originalCreateDate, updatedEntity.getCreated());
        assertNotEquals(originalUpdateDate, updatedEntity.getUpdated());
    }

    @Test
    public void testDeleteById() {
        final String flowId = "2";
        final Optional<FlowEntity> result = repository.findById(flowId);
        assertTrue(result.isPresent());

        repository.deleteById(flowId);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowEntity> deletedResult = repository.findById(flowId);
        assertFalse(deletedResult.isPresent());
    }

    @Test
    public void testDeleteEntity() {
        final String flowId = "2";
        final Optional<FlowEntity> result = repository.findById(flowId);
        assertTrue(result.isPresent());

        repository.delete(result.get());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowEntity> deletedResult = repository.findById(flowId);
        assertFalse(deletedResult.isPresent());
    }

    @Test
    public void testGetAllSummaries() {
        final Iterable<FlowRepository.FlowSummary> summaries = repository.findByIdIsNotNull();

        int count = 0;
        for (FlowRepository.FlowSummary summary : summaries) {
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testGetSummaryByIdWhenExists() {
        final Optional<FlowRepository.FlowSummary> summary = repository.findByIdAndRegistryUrlNotNull("1");
        assertTrue(summary.isPresent());
        assertEquals("1", summary.get().getId());
        assertEquals(FlowFormat.YAML_V2_TYPE, summary.get().getFlowFormat());
    }

    @Test
    public void testGetSummaryByIdWhenDoesNotExist() {
        final Optional<FlowRepository.FlowSummary> summary = repository.findByIdAndRegistryUrlNotNull("DOES-NOT-EXIST");
        assertFalse(summary.isPresent());
    }

    private int getCount() {
        final Iterable<FlowEntity> entities = repository.findAll();

        int count = 0;
        for (FlowEntity entity : entities) {
            count++;
        }

        return count;
    }
}
