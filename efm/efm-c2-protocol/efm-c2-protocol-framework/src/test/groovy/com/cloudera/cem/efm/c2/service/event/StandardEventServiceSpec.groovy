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
package com.cloudera.cem.efm.service.event

import com.cloudera.cem.efm.db.entity.EventEntity
import com.cloudera.cem.efm.db.repository.EventRepository
import com.cloudera.cem.efm.exception.ResourceNotFoundException
import com.cloudera.cem.efm.mapper.OptionalModelMapper
import com.cloudera.cem.efm.model.Event
import com.cloudera.cem.efm.model.EventSeverity
import com.cloudera.cem.efm.model.ResourceReference
import com.cloudera.cem.efm.service.config.ServerDetailsProvider
import com.cloudera.cem.efm.service.SpecUtil
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import javax.validation.Validation
import javax.validation.Validator

class StandardEventServiceSpec extends Specification {

    static OptionalModelMapper modelMapper
    static Validator validator
    ServerDetailsProvider serverDetailsProvider = Mock(ServerDetailsProvider)
    EventRepository eventRepository
    EventService eventService

    def setupSpec() {
        validator = Validation.buildDefaultValidatorFactory().getValidator()
        modelMapper = SpecUtil.buildOptionalModelMapper()
    }

    def setup() {
        eventRepository = Mock(EventRepository)

        serverDetailsProvider.getHostnameOrAddress() >> "localhost"

        eventService = new StandardEventService(
                eventRepository,
                serverDetailsProvider,
                Optional.empty(),
                validator,
                modelMapper)
    }

    def "create event"() {

        when: "arg is null"
        eventService.createEvent(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "arg is invalid"
        eventService.createEvent(new Event())

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "valid event is created"
        eventRepository.existsById("event1") >> false
        eventRepository.findById("event1") >> Optional.empty()
        eventRepository.save(_ as EventEntity) >> { EventEntity e -> return e }
        def created = eventService.createEvent(new Event([eventType : "Background", eventSource : new ResourceReference("Agent", "1"), level: EventSeverity.INFO, message: "Message"]))

        then: "created event is returned"
        with(created) {
            id != null
            level == EventSeverity.INFO
            message == "Message"
        }


        when: "event is created with existing id"
        eventRepository.existsById(_ as String) >> true
        eventRepository.findById(_ as String) >> Optional.of(new EventEntity([id: "event2", level: EventSeverity.INFO, message: "Message"]))
        eventService.createEvent(new Event([eventType : "Background", eventSource : new ResourceReference("Agent", "1"), level: EventSeverity.INFO, message: "Message"]))

        then: "exception is thrown"
        thrown IllegalStateException

    }

    def "get events"() {

        setup:
        EventEntity event1 = new EventEntity([id: "event1", level: EventSeverity.INFO, message: "Event One"])
        EventEntity event2 = new EventEntity([id: "event2", level: EventSeverity.INFO, message: "Event Two"])
        eventRepository.findAll(null, _ as Pageable) >> new PageImpl<EventEntity>([event1, event2])

        when:
        def events = eventService.getEvents(null)

        then:
        with(events) {
            events.getElements().size() == 2
            events.getElements().get(0).id == "event1"
            events.getPage().getNumber() == 0
            events.getPage().getSize() == 2
            events.getPage().getTotalElements() == 2
            events.getPage().getTotalPages() == 1
            events.getLinks() == null
        }

    }

    def "get event"() {

        when: "event does not exist"
        eventRepository.findById("event1") >> Optional.empty()
        def event1 = eventService.getEvent("event1")

        then: "empty optional is returned"
        !event1.isPresent()


        when: "event exists"
        eventRepository.findById("event2") >> Optional.of(new EventEntity([id: "event2", level: EventSeverity.INFO, message: "Event Two"]))
        def event2 = eventService.getEvent("event2")

        then: "event is returned"
        event2.isPresent()
        with(event2.get()) {
            id == "event2"
        }

    }

    def "delete event"() {

        when: "event does not exist"
        eventRepository.findById("event1") >> Optional.empty()
        eventService.deleteEvent("event1")

        then:
        thrown ResourceNotFoundException


        when: "event exists"
        eventRepository.findById("event2") >> Optional.of(new EventEntity([id: "event2", level: EventSeverity.INFO, message: "Event Two"]))
        def deleted = eventService.deleteEvent("event2")

        then:
        with(deleted) {
            id == "event2"
        }

    }

    def "tag event"() {

        when: "event does not exist"
        eventRepository.findById("event1") >> Optional.empty()
        eventService.tagEvent("event1", "key1", "value1")

        then:
        thrown ResourceNotFoundException


        when: "event exists"
        eventRepository.findById("event2") >> Optional.of(new EventEntity([id: "event2", level: EventSeverity.INFO, message: "Event Two"]))
        eventRepository.save(_ as EventEntity) >> { EventEntity e -> return e }
        def updated = eventService.tagEvent("event2", "key1", "value1")

        then:
        with(updated) {
            id == "event2"
            tags.size() == 1
            tags.get("key1") == "value1"
        }


        when: "tag value null"
        def tags = new HashMap<String, String>()
        tags.put("key1", "value1")
        tags.put("key2", "value2")
        eventRepository.findById("event3") >> Optional.of(new EventEntity([id: "event3", level: EventSeverity.INFO, message: "Event Two", tags: tags]))
        eventRepository.save(_ as EventEntity) >> { EventEntity e -> return e }
        def cleared = eventService.tagEvent("event3", "key1", null)

        then:
        with(cleared) {
            id == "event3"
            tags.size() == 1
            tags.containsKey("key2")
            !tags.containsKey("key1")
        }

    }

}
