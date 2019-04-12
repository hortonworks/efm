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
package com.cloudera.cem.efm.service.event;


import com.cloudera.cem.efm.db.entity.EventEntity;
import com.cloudera.cem.efm.db.query.EventSpecifications;
import com.cloudera.cem.efm.db.query.PageableFactory;
import com.cloudera.cem.efm.db.repository.EventRepository;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.PageMetadata;
import com.cloudera.cem.efm.model.QueryParameters;
import com.cloudera.cem.efm.model.ResourceReference;
import com.cloudera.cem.efm.service.BaseService;
import com.cloudera.cem.efm.service.config.ServerDetailsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StandardEventService extends BaseService implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(StandardEventService.class);

    private final EventRepository eventRepository;
    private final PageableFactory pageableFactory;

    // For events originating at this C2 Server instance, provide a default event source reference that will be applied in createEvent
    private ResourceReference defaultEventSource;

    @Autowired
    public StandardEventService(
            final EventRepository eventRepository,
            final ServerDetailsProvider serverDetailsProvider,
            final Optional<PageableFactory> pageableFactory,
            final Validator validator,
            final OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.eventRepository = eventRepository;
        this.pageableFactory = pageableFactory.orElse(
                PageableFactory.configure()
                        .defaultSortSupplier(() -> Sort.by(Sort.Direction.DESC, Event.Field.CREATED))
                        .build());

        this.defaultEventSource = new ResourceReference("Server", serverDetailsProvider.getHostnameOrAddress());
    }

    public ResourceReference getDefaultEventSource() {
        return defaultEventSource;
    }

    public void setDefaultEventSource(ResourceReference defaultEventSource) {
        this.defaultEventSource = defaultEventSource;
    }

    @Override
    public Event createEvent(Event event) {
        if(event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        event.setId(UUID.randomUUID().toString());

        if (event.getEventSource() == null) {
            event.setEventSource(defaultEventSource);
        }

        validate(event, "Cannot create event");
        if (eventRepository.existsById(event.getId())) {
            throw new IllegalStateException(String.format("Event already exists with id='%s", event.getId()));
        }
        final EventEntity entity = modelMapper.map(event, EventEntity.class);
        final EventEntity savedEntity = eventRepository.save(entity);
        final Event savedEvent = modelMapper.map(savedEntity, Event.class);
        return savedEvent;
    }

    @Override
    public ListContainer<Event> getEvents(final QueryParameters queryParameters) {

        Specification<EventEntity> specs = EventSpecifications.fromQueryParameters(queryParameters);
        Pageable pageable = pageableFactory.pageableFromQueryParameters(queryParameters);

        Page<EventEntity> eventEntityPage = eventRepository.findAll(specs, pageable);

        // TODO, write a converter for Page<Entity>,ListContainer<DTO> so that this pattern is reusable
        ListContainer<Event> eventsListContainer = new ListContainer<>();

        List<Event> events = eventEntityPage.getContent().stream()
                .map(eventEntity -> modelMapper.map(eventEntity, Event.class))
                .collect(Collectors.toList());
        eventsListContainer.setElements(events);

        final PageMetadata pageMetadata = new PageMetadata(
                eventEntityPage.getNumberOfElements(),
                eventEntityPage.getNumber(),
                eventEntityPage.getTotalElements(),
                eventEntityPage.getTotalPages());
        eventsListContainer.setPage(pageMetadata);

        eventsListContainer.setLinks(null);  // this is set in web layer

        return eventsListContainer;
    }

    @Override
    public Optional<Event> getEvent(String eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }
        final Optional<EventEntity> eventEntityOptional = eventRepository.findById(eventId);
        final Optional<Event> eventOptional = modelMapper.mapOptional(eventEntityOptional, Event.class);
        return eventOptional;
    }

    @Override
    public Event deleteEvent(String eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }

        final Optional<EventEntity> entityOptional = eventRepository.findById(eventId);
        if (!entityOptional.isPresent()) {
            throw new ResourceNotFoundException("Event not found with id " + eventId);
        }
        eventRepository.deleteById(eventId);

        final Event deletedEvent = modelMapper.map(entityOptional.get(), Event.class);
        return deletedEvent;
    }

    @Override
    public Event tagEvent(String eventId, String tagKey, String tagValue) {

        if (eventId == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }
        if (tagKey == null) {
            throw new IllegalArgumentException("Tag key cannot be null");
        }

        // Update event
        final Optional<EventEntity> entityOptional = eventRepository.findById(eventId);
        final EventEntity entityToBeModified = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        logger.debug("Event tagged. eventId={}, tagKey={}, tagValue={}", eventId, tagKey, tagValue);
        Map<String, String> tags = entityToBeModified.getTags();
        if (tags == null) {
            tags = new HashMap<>();
        }
        if (tagValue != null) {
            tags.put(tagKey, tagValue);
        } else {
            tags.remove(tagKey);
        }
        entityToBeModified.setTags(tags);


        // Save everything
        final EventEntity updatedEventEntity = eventRepository.save(entityToBeModified);

        final Event updatedEvent = modelMapper.map(updatedEventEntity, Event.class);
        return updatedEvent;
    }

    /**
     * This is here temporarily as a helper method for generating development test data.
     * It will be removed prior to release.
     *
     * @param random (optional) a Random instance to use.
     * @return a random EventEntity object
     */
    private EventEntity generateRandomEventEntity(Random random) {
        if (random == null) {
            random = new Random();
        }

        final String[] randomMessages = {
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                "Netus et malesuada fames ac turpis.",
                "Mauris vitae ultricies leo integer malesuada nunc.",
                "Laoreet non curabitur gravida arcu ac tortor dignissim.",
                "Pellentesque massa placerat duis ultricies lacus sed turpis tincidunt id."
        };
        final EventSeverity[] severityLevels = EventSeverity.values();
        final String[] eventTypes = { "Heartbeat", "Flow Update", "Background", "Agent Status", "C2 Server Status" };
        final String[] agentClasses = { "classA", "classB", "classC" };

        final EventEntity eventEntity = new EventEntity();
        eventEntity.setId(UUID.randomUUID().toString());
        eventEntity.setEventSourceType(Math.random() < 0.5 ? "Agent" : "Server");
        if (eventEntity.getEventSourceType().equals("Agent")) {
            eventEntity.setEventSourceId(UUID.randomUUID().toString());
        } else {
            eventEntity.setEventSourceId("localhost");
        }
        eventEntity.setEventDetailType(Math.random() < 0.5 ? "C2Heartbeat" : null);
        if (eventEntity.getEventDetailType() != null) {
            eventEntity.setEventDetailId(UUID.randomUUID().toString());
        }
        eventEntity.setLevel(severityLevels[random.nextInt(severityLevels.length-1)]);
        eventEntity.setEventType(eventTypes[random.nextInt(eventTypes.length-1)]);
        eventEntity.setMessage(randomMessages[random.nextInt(randomMessages.length-1)]);

        if (eventEntity.getEventSourceType().equals("Agent")) {
            final String agentClass = agentClasses[random.nextInt(agentClasses.length - 1)];
            eventEntity.setAgentClass(agentClass);

            Map<String, String> tags = new HashMap<>();
            tags.put("agentClass", agentClass);
            eventEntity.setTags(tags);
        }

        return eventEntity;
    }

    int generateAndSaveRandomEventEntities(int bound) {
        final Random random = new Random();
        final int randomEventCount = random.nextInt(bound);
        List<EventEntity> randomEventEntities = new ArrayList<>(randomEventCount);
        for (int i = 0; i < randomEventCount; i++) {
            randomEventEntities.add(generateRandomEventEntity(random));
        }
        eventRepository.saveAll(randomEventEntities);
        return randomEventCount;
    }

}
