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
package com.cloudera.cem.efm.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class GaugeValueCache {

    private static final Logger logger = LoggerFactory.getLogger(GaugeValueCache.class);
    private static final CacheMissSupplier NULL_CACHE_MISS_SUPPLIER = tags -> null;

    private final C2MetricsConfiguration metricsConfiguration;
    private final CacheMissSupplier cacheMissSupplier;
    private final ConcurrentMap<Set<Tag>, Double> internalCache = new ConcurrentHashMap<>();
    private final Map<Tag, Boolean> tagEnabledMap = new HashMap<>();

    GaugeValueCache(C2MetricsConfiguration metricsConfiguration) {
        this(metricsConfiguration, NULL_CACHE_MISS_SUPPLIER);
    }

    GaugeValueCache(C2MetricsConfiguration metricsConfiguration, CacheMissSupplier cacheMissSupplier) {
        this.metricsConfiguration = metricsConfiguration;
        this.cacheMissSupplier = cacheMissSupplier != null ? cacheMissSupplier : NULL_CACHE_MISS_SUPPLIER;
    }

    void update(Tags tags, Double cacheValue) {
        if (tags == null || cacheValue == null) {
            throw new IllegalArgumentException("Tags and cacheValue must both be not null");
        }
        final Set<Tag> filteredTags = filterDisabledTags(tags);
        internalCache.put(filteredTags, cacheValue);
    }

    Double getGaugeValue(Tags tags) {
        if (tags == null) {
            throw new IllegalArgumentException("Tags cannot be null");
        }

        final Set<Tag> filteredTags = filterDisabledTags(tags);

        Double currentGaugeValue = null;
        final Double cacheResult = internalCache.get(filteredTags);
        if (cacheResult != null) {
            currentGaugeValue = cacheResult;
        } else {
            final Double cacheMissSupplierResult = cacheMissSupplier.get(tags);
            if (cacheMissSupplierResult != null) {
                currentGaugeValue = cacheMissSupplierResult;
                internalCache.put(filteredTags, currentGaugeValue);
            }
        }
        return currentGaugeValue;
    }

    private Set<Tag> filterDisabledTags(Tags tags) {
        return tags.stream().filter(this::isTagEnabled).collect(Collectors.toSet());
    }

    private boolean isTagEnabled(Tag tag) {
        Boolean isTagEnabled = tagEnabledMap.get(tag);
        if (isTagEnabled == null) {
            isTagEnabled = metricsConfiguration.isTagEnabled(tag.getKey());
            tagEnabledMap.put(tag, isTagEnabled);
        }
        return isTagEnabled;
    }

    @FunctionalInterface
    interface CacheMissSupplier {
        Double get(Tags tags);
    }

}
