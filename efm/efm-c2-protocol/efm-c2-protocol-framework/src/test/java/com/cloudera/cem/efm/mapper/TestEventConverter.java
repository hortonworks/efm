/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
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
package com.cloudera.cem.efm.mapper;

import com.cloudera.cem.efm.db.entity.EventEntity;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.ResourceReference;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestEventConverter extends ModelMapperTest {

    @Test
    public void testMapEventToEntity() {
        // Arrange
        Event event = new Event();
        event.setId("test-event");
        event.setLevel(EventSeverity.INFO);
        event.setMessage("This is a test event message.");
        event.setEventType("Heartbeat");
        event.setEventSource(new ResourceReference(Agent.class.getSimpleName(), "test-agent"));
        event.setEventDetail(new ResourceReference(C2Heartbeat.class.getSimpleName(), "test-heartbeat"));
        event.putTag("agentClass", "test-class");

        // Act
        EventEntity eventEntity = modelMapper.map(event, EventEntity.class);

        // Assert
        assertCorrectMapping(event, eventEntity);
    }


    @Test
    public void testMapPartialEventToEntity() {
        // Arrange
        Event event = new Event();
        // Set only required fields
        event.setId("test-event");
        event.setLevel(EventSeverity.INFO);
        event.setMessage("This is a test event message.");

        // Act
        EventEntity eventEntity = modelMapper.map(event, EventEntity.class);

        // Assert
        assertCorrectMapping(event, eventEntity);
    }

    @Test
    public void testMapEntityToEvent() {
        // Arrange
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId("test-event");
        eventEntity.setLevel(EventSeverity.INFO);
        eventEntity.setMessage("This is a test event message.");
        eventEntity.setEventType("Heartbeat");
        eventEntity.setEventSourceType(Agent.class.getSimpleName());
        eventEntity.setEventSourceId("test-agent");
        eventEntity.setEventDetailType(C2Heartbeat.class.getSimpleName());
        eventEntity.setEventDetailId("test-heartbeat");
        Map<String, String> tags = new HashMap<>();
        tags.put("agentClass", "test-class");
        eventEntity.setTags(tags);
        eventEntity.prePersist();

        // Act
        Event event = modelMapper.map(eventEntity, Event.class);

        // Assert
        assertCorrectMapping(eventEntity, event);

    }

    @Test
    public void testMapPartialEntityToEvent() {
        // Arrange
        EventEntity eventEntity = new EventEntity();
        // Set only required fields
        eventEntity.setId("test-event");
        eventEntity.setLevel(EventSeverity.INFO);
        eventEntity.setMessage("This is a test event message.");
        eventEntity.prePersist();

        // Act
        Event event = modelMapper.map(eventEntity, Event.class);

        // Assert
        assertCorrectMapping(eventEntity, event);
    }

    private static void assertCorrectMapping(Event source, EventEntity mapResult) {
        assertEquals(source.getId(), mapResult.getId());
        assertEquals(source.getLevel(), mapResult.getLevel());
        assertEquals(source.getMessage(), mapResult.getMessage());

        assertEquals(source.getEventType(), mapResult.getEventType());
        assertEquals(source.getTags(), mapResult.getTags());

        if (source.getEventSource() != null) {
            assertEquals(source.getEventSource().getType(), mapResult.getEventSourceType());
            assertEquals(source.getEventSource().getId(), mapResult.getEventSourceId());
        } else {
            assertNull(mapResult.getEventSourceType());
            assertNull(mapResult.getEventSourceId());
        }

        if (source.getEventDetail() != null) {
            assertEquals(source.getEventDetail().getType(), mapResult.getEventDetailType());
            assertEquals(source.getEventDetail().getId(), mapResult.getEventDetailId());
        } else {
            assertNull(mapResult.getEventSourceType());
            assertNull(mapResult.getEventSourceId());
        }
    }

    private static void assertCorrectMapping(EventEntity source, Event mapResult) {
        assertEquals(source.getId(), mapResult.getId());
        assertEquals(source.getLevel(), mapResult.getLevel());
        assertEquals(source.getMessage(), mapResult.getMessage());

        assertEquals(source.getEventType(), mapResult.getEventType());

        if (source.getCreated() != null) {
            assertEquals(Long.valueOf(source.getCreated().getTime()), mapResult.getCreated());
        } else {
            assertNull(mapResult.getCreated());
        }

        if (source.getEventSourceType() != null) {
            assertEquals(source.getEventSourceType(), mapResult.getEventSource().getType());
            assertEquals(source.getEventSourceId(), mapResult.getEventSource().getId());
        } else {
            assertNull(mapResult.getEventSource());
        }

        if (source.getEventDetailType() != null) {
            assertEquals(source.getEventDetailType(), mapResult.getEventDetail().getType());
            assertEquals(source.getEventDetailId(), mapResult.getEventDetail().getId());
        } else {
            assertNull(mapResult.getEventDetail());
        }

        if (source.getTags() != null && !source.getTags().isEmpty()) {
            assertEquals(source.getTags(), mapResult.getTags());
        } else {
            assertNull(mapResult.getTags());
        }
    }

}
