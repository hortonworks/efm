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
package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowFormat;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.StandardNiFiUser;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestDatabaseFDFlowManager extends DatabaseTest {

    @Autowired
    private FDFlowManager flowManager;

    @Test
    public void testGetFlowWhenExists() {
        final Optional<FDFlow> flowOptional = flowManager.getFlow("2");
        assertTrue(flowOptional.isPresent());

        final FDFlow flow = flowOptional.get();

        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        assertEquals("2", flowMetadata.getIdentifier());
        assertEquals("Class 2", flowMetadata.getAgentClass());
        assertNotNull(flowMetadata.getCreated());
        assertNotNull(flowMetadata.getUpdated());

        final VersionedProcessGroup flowContent = flow.getFlowContent();
        assertNotNull(flowContent);
        assertEquals("root-group-2", flowContent.getIdentifier());
        assertEquals("root", flowContent.getName());

        // Verify the content was based off the latest revision
        assertNull(flow.getVersionInfo());
        assertNotNull(flow.getLocalFlowRevision());
        assertEquals(3, flow.getLocalFlowRevision().intValue());
    }

    @Test
    public void testGetFlowWhenDoesNotExist() {
        final Optional<FDFlow> flowOptional = flowManager.getFlow("DOES-NOT-EXIST");
        assertFalse(flowOptional.isPresent());
    }

    @Test
    public void testGetFlowMetadataWhenExists() {
        final Optional<FDFlowMetadata> flowMetadataOptional = flowManager.getFlowMetadata("2");
        assertTrue(flowMetadataOptional.isPresent());

        final FDFlowMetadata flowMetadata = flowMetadataOptional.get();
        assertEquals("2", flowMetadata.getIdentifier());
        assertEquals("Class 2", flowMetadata.getAgentClass());
        assertNotNull(flowMetadata.getCreated());
        assertNotNull(flowMetadata.getUpdated());
    }

    @Test
    public void testGetFlowMetadataWhenDoesNotExist() {
        final Optional<FDFlowMetadata> flowMetadataOptional = flowManager.getFlowMetadata("DOES-NOT-EXIST");
        assertFalse(flowMetadataOptional.isPresent());
    }

    @Test
    public void testCreateFlow() {
        final String agentClassFoo = "Class FOO";
        final NiFiUser user = new StandardNiFiUser.Builder().identity("system").build();
        final FDFlowMetadata flowMetadata = flowManager.createFlow(agentClassFoo, user);
        entityManager.flush();

        assertEquals(agentClassFoo, flowMetadata.getAgentClass());
        assertNotNull(flowMetadata.getIdentifier());
        assertNotNull(flowMetadata.getCreated());
        assertNotNull(flowMetadata.getUpdated());

        final Optional<FDFlow> flowOptional = flowManager.getFlow(flowMetadata.getIdentifier());
        assertTrue(flowOptional.isPresent());

        final FDFlow flow = flowOptional.get();
        assertEquals(flowMetadata.getIdentifier(), flow.getFlowMetadata().getIdentifier());
        assertNotNull(flow.getFlowContent());
        assertNotNull(flow.getFlowContent().getIdentifier());
        assertNotNull(flow.getFlowContent().getName());
    }

    @Test
    public void testGetAvailableFlows() {
        final List<FDFlowMetadata> flows = flowManager.getAvailableFlows();
        assertEquals(2, flows.size());
    }

    @Test
    public void testDeleteFlowWhenExists() {
        final Optional<FDFlow> flowOptional = flowManager.getFlow("2");
        assertTrue(flowOptional.isPresent());

        final FDFlow flow = flowOptional.get();

        final FDFlowMetadata deletedFlowMetadata = flowManager.deleteFlow(flow.getFlowMetadata().getIdentifier());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        assertNotNull(deletedFlowMetadata);
        assertEquals(flow.getFlowMetadata().getIdentifier(), deletedFlowMetadata.getIdentifier());

        final Optional<FDFlow> deletedFlowOptional = flowManager.getFlow("2");
        assertFalse(deletedFlowOptional.isPresent());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testDeleteFlowWhenDoesNotExist() {
        flowManager.deleteFlow("DOES-NOT-EXIST");
    }

    @Test
    public void testGetFlowEventsWhenFlowExists() {
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        BigInteger prevRevision = new BigInteger("-1");
        for (final FDFlowEvent flowEvent : flowEvents) {
            assertEquals(flowId, flowEvent.getFlowIdentifier());
            assertNotNull(flowEvent.getIdentifier());
            assertNotNull(flowEvent.getFlowRevision());
            assertNotNull(flowEvent.getComponentId());
            assertNotNull(flowEvent.getCreated());
            assertNotNull(flowEvent.getUpdated());
            assertNotNull(flowEvent.getUserIdentity());
            assertNotNull(flowEvent.getEventType());
            assertNotNull(flowEvent.getEventDescription());
            assertNotNull(flowEvent.getFlowFormat());

            // Verify event revisions are in increasing order
            assertEquals(1, flowEvent.getFlowRevision().compareTo(prevRevision));
            prevRevision = flowEvent.getFlowRevision();
        }
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetFlowEventsWhenFlowDoesNotExist() {
        flowManager.getFlowEvents("DOES-NOT-EXIST");
    }

    @Test
    public void testGetLatestFlowEventWhenFlowExists() {
        final String flowId = "2";
        final Optional<FDFlowEvent> flowEventOptional = flowManager.getLatestFlowEvent(flowId);
        assertTrue(flowEventOptional.isPresent());

        final FDFlowEvent flowEvent = flowEventOptional.get();
        assertEquals("4", flowEvent.getIdentifier());
        assertEquals(3, flowEvent.getFlowRevision().intValue());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetLatestFlowWhenFlowDoesNotExist() {
        flowManager.getLatestFlowEvent("DOES-NOT-EXIST");
    }

    @Test
    public void testGetFlowEventWhenExists() throws ParseException {
        final String flowEventId = "4";
        final Optional<FDFlowEvent> flowEventOptional = flowManager.getFlowEvent(flowEventId);
        assertTrue(flowEventOptional.isPresent());

        final FDFlowEvent flowEvent = flowEventOptional.get();
        assertEquals(flowEventId, flowEvent.getIdentifier());
        assertEquals("2", flowEvent.getFlowIdentifier());
        assertEquals(new BigInteger("3"), flowEvent.getFlowRevision());
        Assert.assertEquals(FDFlowEventType.FLOW_PUBLISHED, flowEvent.getEventType());
        assertEquals("Created root process group", flowEvent.getEventDescription());
        Assert.assertEquals(FDFlowFormat.JACKSON_JSON_V1, flowEvent.getFlowFormat());
        assertEquals("root-group-2", flowEvent.getComponentId());
        assertEquals("system", flowEvent.getUserIdentity());

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
        assertEquals(dateFormat.parse("2018-07-26 12:54:00.000 UTC").getTime(), flowEvent.getCreated().longValue());
        assertEquals(dateFormat.parse("2018-07-26 12:54:00.000 UTC").getTime(), flowEvent.getUpdated().longValue());
    }

    @Test
    public void testGetFlowEventWhenDoesNotExist() {
        final Optional<FDFlowEvent> flowEventOptional = flowManager.getFlowEvent("DOES-NOT-EXIST");
        assertFalse(flowEventOptional.isPresent());
    }

    @Test
    public void testAddFlowEventWhenValid() {
        final FDFlowEvent flowEvent = new FDFlowEvent();
        flowEvent.setFlowIdentifier("2");
        flowEvent.setComponentId("proc-1");
        flowEvent.setEventType(FDFlowEventType.COMPONENT_ADDED);
        flowEvent.setEventDescription("Added TailFile processor");
        flowEvent.setUserIdentity("test-user");

        final VersionedProcessGroup pg = new VersionedProcessGroup();
        pg.setIdentifier("root-group-2");
        pg.setName("root");

        final NiFiUser user = new StandardNiFiUser.Builder().identity("system").build();

        final FDFlowEvent createdEvent = flowManager.addFlowEvent(flowEvent, pg, user);

        // Verify fields that were filled in for us
        assertNotNull(createdEvent.getIdentifier());
        assertNotNull(createdEvent.getFlowRevision());
        assertEquals(new BigInteger("4"), createdEvent.getFlowRevision());
        assertNotNull(createdEvent.getCreated());
        assertNotNull(createdEvent.getUpdated());
        assertEquals(DatabaseFDFlowManager.CURRENT_FLOW_FORMAT, createdEvent.getFlowFormat());

        // Verify fields we passed in
        assertEquals(flowEvent.getFlowIdentifier(), createdEvent.getFlowIdentifier());
        assertEquals(flowEvent.getEventType(), createdEvent.getEventType());
        assertEquals(flowEvent.getEventDescription(), createdEvent.getEventDescription());
        assertEquals(flowEvent.getComponentId(), createdEvent.getComponentId());
        assertEquals(flowEvent.getUserIdentity(), createdEvent.getUserIdentity());

        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(createdEvent.getFlowIdentifier());
        assertEquals(4, flowEvents.size());

        FDFlowEvent lastEvent = null;
        for (FDFlowEvent event : flowEvents) {
            lastEvent = event;
        }
        assertEquals(new BigInteger("4"), lastEvent.getFlowRevision());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddFlowEventAndTryToChangeRootGroupId() {
        final FDFlowEvent flowEvent = new FDFlowEvent();
        flowEvent.setFlowIdentifier("2");
        flowEvent.setComponentId("proc-1");
        flowEvent.setEventType(FDFlowEventType.COMPONENT_ADDED);
        flowEvent.setEventDescription("Added TailFile processor");
        flowEvent.setUserIdentity("test-user");

        final VersionedProcessGroup pg = new VersionedProcessGroup();
        pg.setIdentifier(UUID.randomUUID().toString()); // SET A DIFFERENT ID HERE WHICH ISN'T ALLOWED
        pg.setName("root");

        final NiFiUser user = new StandardNiFiUser.Builder().identity("system").build();

        flowManager.addFlowEvent(flowEvent, pg, user);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddFlowEventWhenMissingFlowId() {
        final FDFlowEvent flowEvent = new FDFlowEvent();
        // flowEvent.setFlowIdentifier("2"); LEAVE THIS OUT ON PURPOSE
        flowEvent.setComponentId("proc-1");
        flowEvent.setEventType(FDFlowEventType.COMPONENT_ADDED);
        flowEvent.setEventDescription("Added TailFile processor");
        flowEvent.setUserIdentity("test-user");

        final VersionedProcessGroup pg = new VersionedProcessGroup();
        pg.setIdentifier(UUID.randomUUID().toString());
        pg.setName("NEW ROOT GROUP");

        final NiFiUser user = new StandardNiFiUser.Builder().identity("system").build();

        flowManager.addFlowEvent(flowEvent, pg, user);
    }

    @Test
    public void testDeleteFlowEvent() {
        // Verify that we start with 3 events for flow #2
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        final FDFlowEvent flowEvent2 = flowManager.deleteFlowEvent("2");
        assertNotNull(flowEvent2);

        final FDFlowEvent flowEvent3 = flowManager.deleteFlowEvent("3");
        assertNotNull(flowEvent3);

        try {
            flowManager.deleteFlowEvent("4");
            Assert.fail("Should have thrown exception");
        } catch (Exception e) {

        }
    }

    @Test
    public void testPublishFlowWhenExists() {
        // Verify that we start with 3 events for flow #2
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        // Publish the flow which should remove all events except latest
        flowManager.retainPublishEvents(flowId);

        final List<FDFlowEvent> updatedFlowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(1, updatedFlowEvents.size());

        final FDFlowEvent flowEvent = updatedFlowEvents.get(0);
        assertEquals("4", flowEvent.getIdentifier());
        assertEquals(3, flowEvent.getFlowRevision().intValue());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testPublishFlowWhenDoesNotExist() {
        flowManager.retainPublishEvents("DOES-NOT-EXIST");
    }

    @Test
    public void testRevertFlow() {
        // Verify that we start with 3 events for flow #2
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        // Verify the third event is revision 3
        final BigInteger revision3 = new BigInteger("3");
        assertEquals(revision3, flowEvents.get(2).getFlowRevision());

        // Revert to first event
        final BigInteger revision1 = new BigInteger("1");
        flowManager.revertToFlowRevision("2", revision1);

        final List<FDFlowEvent> flowEvents2 = flowManager.getFlowEvents(flowId);
        assertEquals(1, flowEvents2.size());

        // Verify the third event is revision 3
        assertEquals(revision1, flowEvents.get(0).getFlowRevision());
    }

    @Test
    public void testRevertFlowToSameAsCurrent() {
        // Verify that we start with 3 events for flow #2
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        // Verify the third event is revision 3
        final BigInteger revision3 = new BigInteger("3");
        assertEquals(revision3, flowEvents.get(2).getFlowRevision());

        // Revert to same revision we are already at
        flowManager.revertToFlowRevision("2", revision3);

        final List<FDFlowEvent> flowEvents2 = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents2.size());

        // Verify the third event is revision 3
        assertEquals(revision3, flowEvents.get(2).getFlowRevision());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testRevertFlowToRevisionGreaterThanCurrent() {
        // Verify that we start with 3 events for flow #2
        final String flowId = "2";
        final List<FDFlowEvent> flowEvents = flowManager.getFlowEvents(flowId);
        assertEquals(3, flowEvents.size());

        // Verify the third event is revision 3
        final BigInteger revision3 = new BigInteger("3");
        assertEquals(revision3, flowEvents.get(2).getFlowRevision());

        // Revert to same revision we are already at
        flowManager.revertToFlowRevision("2", new BigInteger("999"));
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testRevertFlowWhenFlowDoesNotExist() {
        flowManager.revertToFlowRevision("DOES-NOT-EXIST", new BigInteger("1"));
    }

    @Test
    public void testGetLatestPublishEvent() {
        final Optional<FDFlowEvent> publishEvent = flowManager.getLatestPublishFlowEvent("2");
        assertTrue(publishEvent.isPresent());
        assertEquals("4", publishEvent.get().getIdentifier());
    }

    @Test
    public void testGetLatestPublishEventWhenNotPublished() {
        final Optional<FDFlowEvent> publishEvent = flowManager.getLatestPublishFlowEvent("1");
        assertFalse(publishEvent.isPresent());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testGetLatestPublishEventWhenFlowDoesNotExist() {
        flowManager.getLatestPublishFlowEvent("DOES-NOT-EXIST");
    }
}
