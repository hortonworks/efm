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
package com.cloudera.cem.efm.db.repository;

import com.google.common.collect.Lists;
import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.db.entity.EventEntity;
import com.cloudera.cem.efm.db.query.EventSpecifications;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.FilterParameter;
import org.assertj.core.util.IterableUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;


public class TestEventRepository extends DatabaseTest {

    // See insertData.sql
    private static final long PREPOPULATED_EVENT_COUNT = 4;

    @Autowired
    private EventRepository repository;

    @Test
    public void testCount() {
        // Arrange
        // See insertData.sql

        // Act
        final long actualCount = repository.count();

        // Assert
        assertEquals(PREPOPULATED_EVENT_COUNT, actualCount);
    }

    @Test
    public void testFindAll() {
        // Arrange
        // See insertData.sql

        // Act
        final Iterable<EventEntity> entities = repository.findAll();

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(PREPOPULATED_EVENT_COUNT, entitiesList.size());
    }

    @Test
    public void testFindAllSort() {
        // Arrange
        // See insertData.sql
        final Sort sort = Sort.by(Sort.Direction.DESC, "created");

        // Act
        final Iterable<EventEntity> entities = repository.findAll(sort);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(4, entitiesList.size());
        assertEquals("event-4", entitiesList.get(0).getId());
        assertEquals("event-3", entitiesList.get(1).getId());
        assertEquals("event-2", entitiesList.get(2).getId());
        assertEquals("event-1", entitiesList.get(3).getId());
    }

    @Test
    public void testFindAllPageable() {
        // Arrange
        // See insertData.sql
        final int pageSize = 1;
        final Pageable pageable = PageRequest.of(0, pageSize);

        // Act
        final Iterable<EventEntity> entities = repository.findAll(pageable);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(pageSize, entitiesList.size());
        assertEquals("event-1", entitiesList.get(0).getId());
    }

    @Test
    public void testFindAllSpecification() {
        // Arrange
        // See insertData.sql
        final Specification<EventEntity> eventSourceAgent1 = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.get("eventSourceId"), "agent-1");
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(eventSourceAgent1);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(2, entitiesList.size());
        assertEquals("event-1", entitiesList.get(0).getId());
        assertEquals("event-2", entitiesList.get(1).getId());
    }

    @Test
    public void testFindAllSpecificationSort() {
        // Arrange
        // See insertData.sql
        final Sort sort = Sort.by(Sort.Direction.DESC, "created");
        final Date gteDate = new Calendar.Builder()
                .setTimeZone(TimeZone.getTimeZone("GMT"))
                .setDate(2018, Calendar.APRIL, 11)
                .setTimeOfDay(12, 53,0)
                .build()
                .getTime();
        final Specification<EventEntity> createGte = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("created"), gteDate);
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(createGte, sort);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(2, entitiesList.size());
        assertEquals("event-4", entitiesList.get(0).getId());
        assertEquals("event-3", entitiesList.get(1).getId());
    }

    @Test
    public void testFindAllSpecificationPageable() {
        // Arrange
        // See insertData.sql
        final Pageable pageable = PageRequest.of(0, 10);
        final Specification<EventEntity> eventSourceAgent2 = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.get("eventSourceId"), "agent-2");
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(eventSourceAgent2, pageable);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(1, entitiesList.size());
        assertEquals("event-3", entitiesList.get(0).getId());
    }

    @Test
    public void testFindAllWhereTagEqual() {
        // Arrange
        // See insertData.sql
        final Specification<EventEntity> tagFooEq30 = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                MapJoin<EventEntity, String,String> joinTags = root.joinMap("tags");
                return criteriaBuilder.and(
                        criteriaBuilder.equal(joinTags.key(), "foo"),
                        criteriaBuilder.equal(joinTags.value(), "30"));
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(tagFooEq30);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(1, entitiesList.size());
        assertEquals("event-3", entitiesList.get(0).getId());
    }

    @Test
    public void testFindAllWhereTagLike() {
        // Arrange
        // See insertData.sql
        final Specification<EventEntity> tagBarLike10 = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                MapJoin<EventEntity, String,String> joinTags = root.joinMap("tags");
                return criteriaBuilder.and(
                        criteriaBuilder.equal(joinTags.key(), "bar"),
                        criteriaBuilder.like(joinTags.value(), "%10%"));
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(tagBarLike10);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(1, entitiesList.size());
        assertEquals("event-2", entitiesList.get(0).getId());
    }

    @Test
    public void testFindAllWhereTagExists() {
        // Arrange
        // See insertData.sql
        final Specification<EventEntity> tagBazExists = new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                MapJoin<EventEntity, String,String> joinTags = root.joinMap("tags");
                return criteriaBuilder.equal(joinTags.key(), "baz");
            }
        };

        // Act
        final Iterable<EventEntity> entities = repository.findAll(tagBazExists);

        // Assert
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(1, entitiesList.size());
        assertEquals("event-3", entitiesList.get(0).getId());
    }

    @Test
    public void testDeleteByCreatedBefore() {
        // Arrange
        // See insertData.sql
        Calendar ageOffDate = new Calendar.Builder()
                .setTimeZone(TimeZone.getTimeZone("GMT"))
                .setDate(2018, Calendar.APRIL, 11)
                .setTimeOfDay(12, 52,30)
                .build();

        // Act
        repository.deleteByLevelAndCreatedBefore(EventSeverity.DEBUG, ageOffDate.getTime());

        // Assert
        final Iterable<EventEntity> entities = repository.findAll();
        final List<EventEntity> entitiesList = IterableUtil.nonNullElementsIn(entities);
        assertEquals(3, entitiesList.size());
        assertEquals("event-2", entitiesList.get(0).getId());
        assertEquals("event-3", entitiesList.get(1).getId());
        assertEquals("event-4", entitiesList.get(2).getId());
    }

    /*
     * The following method is really testing EventSpecifications logic
     * and not EventRepository. Ideally, we write unit tests
     * for Specifications and EventSpecifications, however that is
     * difficult given that all they return are lambda expressions
     * that require a JPA root, query, and builder object to exercise.
     *
     * So for now, adding test cases here that assume EventRepository
     * is working (as proved by test cases above) and therefore we
     * are really asserting that EventSpecifications.fromFilterParameter(...)
     * methods are creating the proper Specification classes.
     *
     * This could also be rewritten as an integration test that calls the
     * GET /c2/api/events?filter=... endpoint with different filter expressions.
     */

    interface EventSpecificationsTestHarness {
        void run(String filterExpression, List<EventEntity> expectedResult);

        default void run(String filterExpression, EventEntity... expectedResult) {
            run(filterExpression, Lists.newArrayList(expectedResult));
        }
    }

    @Test
    public void testSpecificationsFromFilterParameterExpressions() {

        // Arrange
        // See insertData.sql
        final Sort orderById = Sort.by(Sort.Direction.ASC, "id");
        long event2DateEpochMilli = new Calendar.Builder()
                .setTimeZone(TimeZone.getTimeZone("GMT"))
                .setDate(2018, Calendar.APRIL, 11)
                .setTimeOfDay(12, 52,00)
                .build()
                .toInstant()
                .toEpochMilli();
        final List<EventEntity> allEvents = IterableUtil.nonNullElementsIn(repository.findAll());
        final EventEntity event1 = allEvents.get(0);
        final EventEntity event2 = allEvents.get(1);
        final EventEntity event3 = allEvents.get(2);
        final EventEntity event4 = allEvents.get(3);

        // Test Harness
        EventSpecificationsTestHarness testHarness = (filterExpression, expectedResult) -> {
            FilterParameter filter = FilterParameter.fromString(filterExpression);
            Specification<EventEntity> eventSpecification = EventSpecifications.fromFilterParameter(filter);
            List<EventEntity> queryResult = Lists.newArrayList(repository.findAll(eventSpecification, orderById));
            assertEquals(expectedResult, queryResult);

            List<EventEntity> inverseExpectedResult = Lists.newArrayList(allEvents);
            inverseExpectedResult.removeAll(expectedResult);
            Specification<EventEntity> inverseSpecification = EventSpecifications.fromFilterParameter(filter.not());
            List<EventEntity> inverseQueryResult = Lists.newArrayList(repository.findAll(inverseSpecification, orderById));
            assertEquals(inverseExpectedResult, inverseQueryResult);
        };

        testHarness.run("id:like:event", allEvents);
        testHarness.run("id:eq:event-1", event1);
        testHarness.run("id:gt:event-3", event4);
        testHarness.run("id:gte:event-3", event3, event4);
        testHarness.run("id:lt:event-2", event1);
        testHarness.run("id:lte:event-2", event1, event2);

        testHarness.run("created:gt:" + event2DateEpochMilli, event3, event4);
        testHarness.run("created:gte:" + event2DateEpochMilli, event2, event3, event4);
        testHarness.run("created:lt:" + event2DateEpochMilli, event1);
        testHarness.run("created:lte:" + event2DateEpochMilli, event1, event2);

        testHarness.run("level:like:INF", event2);
        testHarness.run("level:eq:INFO", event2);
        testHarness.run("level:gt:INFO", event3, event4);
        testHarness.run("level:gte:INFO", event2, event3, event4);
        testHarness.run("level:lt:WARN", event1, event2);
        testHarness.run("level:lte:WARN", event1, event2, event3);

        testHarness.run("eventType:like:Status", event3, event4);
        testHarness.run("eventType:eq:Heartbeat", event1);

        testHarness.run("message:like:This is an", event2, event4);
        testHarness.run("message:eq:This is a debug event.", event1);

        testHarness.run("eventSourceType:like:Server", event4);
        testHarness.run("eventSourceType:eq:Agent", event1, event2, event3);

        testHarness.run("eventSourceId:like:localhost", event4);
        testHarness.run("eventSourceId:eq:agent-1", event1, event2);

        testHarness.run("agentClass:like:Class", event1, event2, event3);
        testHarness.run("agentClass:eq:Class A", event1, event2);
        testHarness.run("agentClass:gt:Class A", event3);
        testHarness.run("agentClass:gte:Class A", event1, event2, event3);
        testHarness.run("agentClass:lte:Class A", event1, event2);
        testHarness.run("agentClass:lt:Class A", Collections.emptyList());

    }

}
