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
import com.cloudera.cem.efm.db.entity.FDFlowEntity;
import com.cloudera.cem.efm.db.entity.FDFlowEventEntity;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestFDFlowEventRepository extends DatabaseTest {

    @Autowired
    private FDFlowRepository flowRepository;

    @Autowired
    private FDFlowEventRepository flowEventRepository;

    @Test
    public void testFindByFlowIdOrderByFlowRevisionAsc() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        final List<FDFlowEventRepository.FlowEventWithoutContent> events = new ArrayList<>();
        flowEventRepository.findByFlowOrderByFlowRevisionAsc(flowEntity.get()).forEach(e -> events.add(e));
        assertEquals(3, events.size());

        BigInteger prevRevision = new BigInteger("-1");
        for (FDFlowEventRepository.FlowEventWithoutContent event : events) {
            assertEquals("2", event.getFlow().getId());

            // ensure that the revisions are returned in order
            assertEquals(1, event.getFlowRevision().compareTo(prevRevision));
            prevRevision = event.getFlowRevision();
        }
    }

    @Test
    public void testFindFirstByFlowOrderByFlowRevisionDesc() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        final Optional<FDFlowEventEntity> latestFlowEventEntity = flowEventRepository.findFirstByFlowOrderByFlowRevisionDesc(flowEntity.get());
        assertTrue(latestFlowEventEntity.isPresent());
        assertEquals("2", latestFlowEventEntity.get().getFlow().getId());
        assertEquals("4", latestFlowEventEntity.get().getId());
        assertEquals(new BigInteger("3"), latestFlowEventEntity.get().getFlowRevision());
    }

    @Test
    public void testFindFirstByFlowAndEventTypeOrderByFlowRevisionDesc() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        final Optional<FDFlowEventEntity> latestFlowEventEntity = flowEventRepository.findFirstByFlowAndEventTypeOrderByFlowRevisionDesc(
                flowEntity.get(), FDFlowEventType.COMPONENT_ADDED);
        assertTrue(latestFlowEventEntity.isPresent());
        assertEquals("2", latestFlowEventEntity.get().getFlow().getId());
        assertEquals("2", latestFlowEventEntity.get().getId());
        assertEquals(new BigInteger("2"), latestFlowEventEntity.get().getFlowRevision());
    }

    @Test
    public void testDeleteByFlowAndIdNot() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        // Should start with 4 events
        final List<String> ids = new ArrayList<>();
        flowEventRepository.findAll().forEach(e -> ids.add(e.getId()));
        assertEquals(4, ids.size());

        // Flow #2 has three events, so this should delete the first 2
        flowEventRepository.deleteByFlowAndIdNot(flowEntity.get(), "4");

        // Should have 2 total events now
        ids.clear();
        assertEquals(0, ids.size());
        flowEventRepository.findAll().forEach(e -> ids.add(e.getId()));
        assertEquals(2, ids.size());

        // Events 1 and 4 should be left
        assertTrue(flowEventRepository.findById("1").isPresent());
        assertTrue(flowEventRepository.findById("4").isPresent());

        // Events 2 and 3 should be deleted
        assertFalse(flowEventRepository.findById("2").isPresent());
        assertFalse(flowEventRepository.findById("3").isPresent());
    }

    @Test
    public void testDeleteByFlowAndEventTypeNot() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        // Should start with 4 events
        final List<String> ids = new ArrayList<>();
        flowEventRepository.findAll().forEach(e -> ids.add(e.getId()));
        assertEquals(4, ids.size());

        // Flow #2 has three events, so this should delete the first 2
        flowEventRepository.deleteByFlowAndEventTypeNot(flowEntity.get(), FDFlowEventType.FLOW_PUBLISHED);

        // Should have 2 total events now
        ids.clear();
        assertEquals(0, ids.size());
        flowEventRepository.findAll().forEach(e -> ids.add(e.getId()));
        assertEquals(2, ids.size());

        // Events 1 and 4 should be left
        assertTrue(flowEventRepository.findById("1").isPresent());
        assertTrue(flowEventRepository.findById("4").isPresent());

        // Events 2 and 3 should be deleted
        assertFalse(flowEventRepository.findById("2").isPresent());
        assertFalse(flowEventRepository.findById("3").isPresent());
    }

    @Test
    public void testCountByFlow() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        assertEquals(Long.valueOf(3), flowEventRepository.countByFlow(flowEntity.get()));
    }

    @Test
    public void testFindByFlowAndFlowRevision() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        final Optional<FDFlowEventEntity> flowEventEntity = flowEventRepository.findByFlowAndFlowRevision(flowEntity.get(), new BigInteger("3"));
        assertTrue(flowEventEntity.isPresent());
        assertEquals("3", flowEventEntity.get().getFlowRevision().toString());
        assertEquals("4", flowEventEntity.get().getId());
    }

    @Test
    public void testDeleteByFlowAndFlowRevisionGreaterThan() {
        final Optional<FDFlowEntity> flowEntity = flowRepository.findById("2");
        assertTrue(flowEntity.isPresent());

        // Should start with 3 events
        final List<String> ids = new ArrayList<>();
        flowEventRepository.findByFlowOrderByFlowRevisionAsc(flowEntity.get()).forEach(e -> ids.add(e.getId()));
        assertEquals(3, ids.size());

        // Delete the events after the event with revision 1
        flowEventRepository.deleteByFlowAndFlowRevisionGreaterThan(flowEntity.get(), new BigInteger("1"));

        // Should have 2 total events now
        ids.clear();
        assertEquals(0, ids.size());
        flowEventRepository.findByFlowOrderByFlowRevisionAsc(flowEntity.get()).forEach(e -> ids.add(e.getId()));
        assertEquals(1, ids.size());

        // Events 1 and 3 should be left
        assertTrue(flowEventRepository.findById("1").isPresent());
        assertTrue(flowEventRepository.findById("3").isPresent());

        // Events 2 and 4 should be deleted
        assertFalse(flowEventRepository.findById("2").isPresent());
        assertFalse(flowEventRepository.findById("4").isPresent());
    }
}
