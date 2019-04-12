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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.cloudera.cem.efm.metrics.Name.Tag.C2_HOST;

@Configuration
public class C2MetricsConfiguration {

    private final C2MetricsProperties c2MetricsProperties;
    private final MetricsProperties metricsProperties;

    @Autowired
    public C2MetricsConfiguration(C2MetricsProperties c2MetricsProperties, MetricsProperties metricsProperties) {
        this.c2MetricsProperties = c2MetricsProperties;
        this.metricsProperties = metricsProperties;
    }

    @Value("${efm.server.address:}")
    private String hostname;

    @Bean
    public MeterRegistryCustomizer commonTagsCustomizer() {
        return new MeterRegistryCustomizer() {
            @Override
            public void customize(MeterRegistry registry) {
                if (!StringUtils.isEmpty(hostname)) {
                    registry.config().commonTags(C2_HOST, hostname);
                }
            }
        };
    }

    @Bean
    public MeterRegistryCustomizer ignoreTagsCustomizer() {
        return new MeterRegistryCustomizer() {
            @Override
            public void customize(MeterRegistry registry) {
                Map<String, Boolean> enabledMap = c2MetricsProperties.getEnableTag();
                if (enabledMap != null && !enabledMap.isEmpty()) {
                    final Set<String> disabledTags = new HashSet<>();
                    if (!enabledMap.getOrDefault("all", true)) {
                        disabledTags.addAll(Arrays.asList(Name.Tag.ALL));
                    }
                    for (Map.Entry<String, Boolean> entry: enabledMap.entrySet()) {
                        final String tagKey = entry.getKey();
                        final boolean isEnabled = entry.getValue() == null ? entry.getValue() : true;
                        if (! "all".equals(tagKey)) {
                            if (isEnabled) {
                                disabledTags.remove(tagKey);
                            } else {
                                disabledTags.add(tagKey);
                            }
                        }
                    }
                    if (disabledTags.size() > 0) {
                        final MeterFilter ignoreTagsFilter = MeterFilter.ignoreTags(disabledTags.toArray(new String[disabledTags.size()]));
                        registry.config().meterFilter(ignoreTagsFilter);
                    }
                }
            }
        };
    }

    @Bean
    public MeterRegistryCustomizer maxTagValuesCustomizer() {
        return new MeterRegistryCustomizer() {
            @Override
            public void customize(MeterRegistry registry) {
                final Map<String, Integer> rawMaxValuesForTag = c2MetricsProperties.getMaxTags();
                if (rawMaxValuesForTag != null && !rawMaxValuesForTag.isEmpty()) {
                    final Map<String, Integer> mergedMaxValuesForTag = new HashMap<>();
                    final Integer globalTagValueCap = rawMaxValuesForTag.get("all");
                    if (globalTagValueCap != null) {
                        Arrays.stream(Name.Tag.ALL).forEach(tag -> mergedMaxValuesForTag.put(tag, globalTagValueCap));
                    }
                    for (Map.Entry<String, Integer> entry: rawMaxValuesForTag.entrySet()) {
                        final String tagKey = entry.getKey();
                        final Integer maxValues = entry.getValue();
                        if (! "all".equals(tagKey)) {
                            if (maxValues != null) {
                                mergedMaxValuesForTag.put(tagKey, maxValues);
                            }
                        }
                    }

                    for (Map.Entry<String, Integer> tagMax : mergedMaxValuesForTag.entrySet()) {
                        final String tagName = tagMax.getKey();
                        final int maxTagValues = tagMax.getValue();
                        final MeterFilter denyFilter = new MaxTagValuesReachedDenyFilter(Name.Prefix.C2, tagName);
                        MeterFilter capTagValuesFilter = MeterFilter.maximumAllowableTags(Name.Prefix.C2, tagName, maxTagValues, denyFilter);
                        registry.config().meterFilter(capTagValuesFilter);
                    }
                }
            }
        };
    }



    public boolean isMeterEnabled(final String meterNameOrPrefix) {
        final Map<String, Boolean> enabledMeters = metricsProperties.getEnable();
        if (enabledMeters == null || enabledMeters.isEmpty()) {
            // individual meters are enabled by default unless explicitly disabled
            return true;
        }
        boolean enabled = enabledMeters.getOrDefault("all", true);
        for(final String prefix : Name.prefixesOf(meterNameOrPrefix)) {
            enabled = enabledMeters.getOrDefault(prefix, enabled);  // if the enabled flag is not set, it inherits from the parent, which was the previous value
        }
        return enabled;
    }

    public boolean isTagEnabled(final String tagName) {
        final Map<String, Boolean> enabledTags = c2MetricsProperties.getEnableTag();
        if (enabledTags == null || enabledTags.isEmpty()) {
            // individual tags are enabled by default unless explicitly disabled
            return true;
        }

        boolean enabled = enabledTags.getOrDefault("all", true);
        enabled = enabledTags.getOrDefault(tagName, enabled);
        return enabled;
    }



}
