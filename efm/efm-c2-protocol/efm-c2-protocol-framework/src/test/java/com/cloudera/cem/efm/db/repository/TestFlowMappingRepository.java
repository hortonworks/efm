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
import com.cloudera.cem.efm.db.entity.FlowMappingEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.test.context.transaction.TestTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestFlowMappingRepository extends DatabaseTest {

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS z";

    @Autowired
    private PagingAndSortingRepository<FlowMappingEntity,String> flowMappingRepository;

    @Autowired
    private PagingAndSortingRepository<FlowEntity,String> flowRepository;

    @Test
    public void testGetAll() {
        final Iterable<FlowMappingEntity> entities = flowMappingRepository.findAll();

        int count = 0;
        for (FlowMappingEntity entity : entities) {
            Assert.assertNotNull(entity.getFlowEntity());
            count++;
        }

        Assert.assertEquals(1, count);
    }

    @Test
    public void testGetById() {
        final Optional<FlowMappingEntity> result = flowMappingRepository.findById("Class A");
        assertTrue(result.isPresent());

        final FlowMappingEntity flowMapping = result.get();
        assertEquals("Class A", flowMapping.getAgentClass());

        assertNotNull(flowMapping.getFlowEntity());
        assertEquals("1", flowMapping.getFlowEntity().getId());

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(flowMapping.getCreated()));
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(flowMapping.getUpdated()));
    }

    @Test
    public void testSave() {
        final Optional<FlowEntity> flowResult = flowRepository.findById("2");
        assertTrue(flowResult.isPresent());

        final FlowEntity flow = flowResult.get();

        final FlowMappingEntity flowMapping = new FlowMappingEntity();
        flowMapping.setAgentClass("Class B");
        flowMapping.setFlowEntity(flow);

        flowMappingRepository.save(flowMapping);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowMappingEntity> savedResult = flowMappingRepository.findById(flowMapping.getAgentClass());
        assertTrue(savedResult.isPresent());

        final FlowMappingEntity savedFlowMapping = savedResult.get();
        assertEquals(flowMapping.getAgentClass(), savedFlowMapping.getAgentClass());
    }

    @Test
    public void testUpdate() {
        final Optional<FlowMappingEntity> result = flowMappingRepository.findById("Class A");
        assertTrue(result.isPresent());

        // verify it is mapped to the first flow
        final FlowMappingEntity flowMapping = result.get();
        assertNotNull(flowMapping.getFlowEntity());
        assertEquals("1", flowMapping.getFlowEntity().getId());

        // save the original dates so we can compare them later
        final Date originalCreateDate = flowMapping.getCreated();
        final Date originalUpdateDate = flowMapping.getUpdated();

        // retrieve the second flow
        final Optional<FlowEntity> secondFlowResult = flowRepository.findById("2");
        assertTrue(secondFlowResult.isPresent());

        // change the mapped flow
        final FlowEntity secondFlow = secondFlowResult.get();
        flowMapping.setFlowEntity(secondFlow);

        flowMappingRepository.save(flowMapping);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowMappingEntity> updatedResult = flowMappingRepository.findById("Class A");
        assertTrue(updatedResult.isPresent());

        // verify it is mapped to the second flow
        final FlowMappingEntity updatedFlowMapping = updatedResult.get();
        assertNotNull(updatedFlowMapping.getFlowEntity());
        assertEquals(secondFlow.getId(), updatedFlowMapping.getFlowEntity().getId());

        // verify create date didn't change, but update date did change
        assertEquals(originalCreateDate, updatedFlowMapping.getCreated());
        assertNotEquals(originalUpdateDate, updatedFlowMapping.getUpdated());
    }

    @Test
    public void testDeleteById() {
        final String flowMappingId = "Class A";
        final Optional<FlowMappingEntity> result = flowMappingRepository.findById(flowMappingId);
        assertTrue(result.isPresent());

        flowMappingRepository.deleteById(flowMappingId);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowMappingEntity> deletedResult = flowMappingRepository.findById(flowMappingId);
        assertFalse(deletedResult.isPresent());
    }

    @Test
    public void testDeleteByEntity() {
        final String flowMappingId = "Class A";
        final Optional<FlowMappingEntity> result = flowMappingRepository.findById(flowMappingId);
        assertTrue(result.isPresent());

        flowMappingRepository.delete(result.get());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        final Optional<FlowMappingEntity> deletedResult = flowMappingRepository.findById(flowMappingId);
        assertFalse(deletedResult.isPresent());
    }
}
