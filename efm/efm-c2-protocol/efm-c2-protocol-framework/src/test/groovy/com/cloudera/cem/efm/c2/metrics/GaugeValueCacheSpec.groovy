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
package com.cloudera.cem.efm.metrics

import io.micrometer.core.instrument.Tags
import spock.lang.Specification

class GaugeValueCacheSpec extends Specification {

    private static final Double CACHE_MISS_SUPPLIER_VALUE = 1000.0
    private static final Double CACHED_VALUE = 500.0

    private C2MetricsConfiguration c2MetricsConfiguration = Stub(C2MetricsConfiguration)
    private GaugeValueCache.CacheMissSupplier cacheMissSupplier = Stub(GaugeValueCache.CacheMissSupplier)

    def allTagsEnabled() {

        setup:
        final Tags tags = (Tags.of("agentClass", "class-1", "agentId", "agent-1"))
        c2MetricsConfiguration.isTagEnabled(_ as String) >> true
        cacheMissSupplier.get(_ as Tags) >> CACHE_MISS_SUPPLIER_VALUE
        GaugeValueCache gaugeValueCache = new GaugeValueCache(c2MetricsConfiguration, cacheMissSupplier)
        gaugeValueCache.update(tags, CACHED_VALUE)


        when: "cache hit"
        Double gaugeValue = gaugeValueCache.getGaugeValue(Tags.of(tags))

        then: "cached value is returned"
        gaugeValue == CACHED_VALUE


        when: "cache miss"
        Double gaugeValue2 = gaugeValueCache.getGaugeValue(Tags.of("agentClass", "class-1", "agentId", "missing-id"))

        then: "cache miss supplier value is returned"
        gaugeValue2 == CACHE_MISS_SUPPLIER_VALUE

    }

    def someTagsDisabled() {

        setup:
        final Tags tags = (Tags.of("agentClass", "class-1", "agentId", "agent-1"))
        c2MetricsConfiguration.isTagEnabled("agentId") >> false
        c2MetricsConfiguration.isTagEnabled("agentClass") >> true
        cacheMissSupplier.get(_ as Tags) >> CACHE_MISS_SUPPLIER_VALUE
        GaugeValueCache gaugeValueCache = new GaugeValueCache(c2MetricsConfiguration, cacheMissSupplier)
        gaugeValueCache.update(tags, CACHED_VALUE)


        when: "cache hit on all (pre-filter) tags"
        Double gaugeValue = gaugeValueCache.getGaugeValue(tags)

        then: "cached value is returned"
        gaugeValue == CACHED_VALUE


        when: "cache hit on enabled (post-filter) tags"
        Double gaugeValue2 = gaugeValueCache.getGaugeValue(Tags.of("agentClass", "class-1"))

        then: "cached value is returned"
        gaugeValue2 == CACHED_VALUE


        when: "cache miss"
        Double gaugeValue3 = gaugeValueCache.getGaugeValue(Tags.of("agentId", "missing-id"))

        then: "cache miss supplier value is returned"
        gaugeValue3 == CACHE_MISS_SUPPLIER_VALUE

    }

    def someTagsDisabledWithUpdate() {

        setup:
        final Tags tags = (Tags.of("agentClass", "class-1", "agentId", "agent-1"))
        final Tags effectivelyEqualTags = (Tags.of("agentClass", "class-1", "agentId", "agent-2"))
        c2MetricsConfiguration.isTagEnabled("agentId") >> false
        c2MetricsConfiguration.isTagEnabled("agentClass") >> true
        cacheMissSupplier.get(_ as Tags) >> CACHE_MISS_SUPPLIER_VALUE
        GaugeValueCache gaugeValueCache = new GaugeValueCache(c2MetricsConfiguration, cacheMissSupplier)
        gaugeValueCache.update(tags, CACHED_VALUE)
        gaugeValueCache.update(Tags.of(effectivelyEqualTags), CACHED_VALUE + 1.0)


        when: "cache hit on all (pre-filter) tags"
        Double gaugeValue = gaugeValueCache.getGaugeValue(tags)

        then: "cached value is returned"
        gaugeValue == CACHED_VALUE + 1.0


        when: "cache hit on enabled (post-filter) tags"
        Double gaugeValue2 = gaugeValueCache.getGaugeValue(Tags.of("agentClass", "class-1"))

        then: "cached value is returned"
        gaugeValue2 == CACHED_VALUE + 1.0


        when: "cache miss"
        Double gaugeValue3 = gaugeValueCache.getGaugeValue(Tags.of("agentId", "missing-id"))

        then: "cache miss supplier value is returned"
        gaugeValue3 == CACHE_MISS_SUPPLIER_VALUE

    }

}
