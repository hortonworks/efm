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
package com.cloudera.cem.efm.model;

/**
 * Parameters to be passed into service layer for methods that require sorting and paging and filtering.
 */
public class EventQueryParameters extends QueryParameters {

    public static final EventQueryParameters EMPTY_PARAMETERS = new EventQueryParameters.Builder().build();

    private final EventSeverity minimumSeverityLevel;
    private final Long createdAfter;
    private final Long createdBefore;
    private final String sourceType;
    private final String sourceId;

    protected EventQueryParameters(Builder builder) {
        super(builder);
        minimumSeverityLevel = builder.minimumSeverityLevel;
        createdAfter = builder.createdAfter;
        createdBefore = builder.createdBefore;
        sourceType = builder.sourceType;
        sourceId = builder.sourceId;

        if (createdAfter != null) {
            if (createdAfter < 0) {
                throw new IllegalArgumentException("createdAfter cannot be negative");
            }
        }

        if (createdBefore != null) {
            if (createdBefore < 0) {
                throw new IllegalArgumentException("createdBefore cannot be negative");
            }

            if (createdAfter != null && createdAfter.compareTo(createdBefore) > 0) {
                throw new IllegalArgumentException("createdAfter time must be less than createdBefore time");
            }
        }

    }

    public EventSeverity getMinimumSeverityLevel() {
        return minimumSeverityLevel;
    }

    public Long getCreatedAfter() {
        return createdAfter;
    }

    public Long getCreatedBefore() {
        return createdBefore;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    /**
     * Builder for EventQueryParameters.
     */
    public static class Builder extends QueryParameters.Builder {

        private EventSeverity minimumSeverityLevel;
        private Long createdAfter;
        private Long createdBefore;
        private String sourceType;
        private String sourceId;

        public void minimumSeverityLevel(EventSeverity minimumSeverityLevel) {
            this.minimumSeverityLevel = minimumSeverityLevel;
        }

        public void createdAfter(Long createdAfter) {
            this.createdAfter = createdAfter;
        }

        public void createdBefore(Long createdBefore) {
            this.createdBefore = createdBefore;
        }

        public void sourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public void sourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public EventQueryParameters build() {
            return new EventQueryParameters(this);
        }
    }


}
