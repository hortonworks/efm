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

import com.cloudera.cem.efm.model.AgentRepositories;
import com.cloudera.cem.efm.model.AgentRepositoryStatus;
import com.cloudera.cem.efm.model.AgentStatus;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.FlowQueueStatus;
import com.cloudera.cem.efm.service.c2protocol.HeartbeatContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HeartbeatMetrics {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatMetrics.class);

    private final MeterRegistry meterRegistry;
    private final GaugeValueCache lastSeenTimestampCache;

    @Autowired
    public HeartbeatMetrics(
            MeterRegistry meterRegistry,
            C2MetricsConfiguration metricsConfiguration) {

        this.meterRegistry = meterRegistry;
        this.lastSeenTimestampCache = new GaugeValueCache(metricsConfiguration);
    }

    public void record(final C2Heartbeat heartbeat, HeartbeatContext context) {
        if (heartbeat == null) {
            return;
        }
        try {

            final Tags tags = Tags.of(
                    Tag.of(Name.Tag.DEVICE_ID, safeValue(heartbeat.getDeviceId())),
                    Tag.of(Name.Tag.AGENT_ID, safeValue(heartbeat.getAgentId())),
                    Tag.of(Name.Tag.AGENT_CLASS, safeValue(heartbeat.getAgentClass())),
                    Tag.of(Name.Tag.AGENT_MANIFEST_ID, safeValue(heartbeat.getAgentManifestId())),
                    Tag.of(Name.Tag.FLOW_ID, safeValue(heartbeat.getFlowId()))
            );

            processLastSeenTimestamp(tags, heartbeat.getCreated());

            Counter.builder(Name.C2_HEARTBEAT_COUNT)
                    .description("number of heartbeats received")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();

            if (context != null && context.getContentLength() != null) {
                DistributionSummary.builder(Name.C2_HEARTBEAT_CONTENT_LENGTH)
                        .description("content length of the heartbeat payload body")
                        .tags(tags)
                        .register(meterRegistry)
                        .record(context.getContentLength());
            }

            // Conditional metrics based on content of heartbeat
            final AgentStatus agentStatus = heartbeat.getAgentInfo() != null ? heartbeat.getAgentInfo().getStatus() : null;
            final AgentRepositories repositories = agentStatus != null ? agentStatus.getRepositories() : null;
            final AgentRepositoryStatus flowfileRepoStatus = repositories != null ? repositories.getFlowfile() : null;
            final AgentRepositoryStatus provenanceRepoStatus = repositories != null ? repositories.getProvenance() : null;
            if (flowfileRepoStatus != null) {
                processAgentRepoStatusMetrics(Name.Prefix.C2_AGENT_STATUS_REPO_FLOWFILE, tags ,flowfileRepoStatus);
            }
            if (provenanceRepoStatus != null) {
                processAgentRepoStatusMetrics(Name.Prefix.C2_AGENT_STATUS_REPO_PROVENANCE, tags, provenanceRepoStatus);
            }

            final Map<String, FlowQueueStatus> queues = heartbeat.getFlowInfo() != null ? heartbeat.getFlowInfo().getQueues() : null;
            if (queues != null) {
                for (final Map.Entry<String, FlowQueueStatus> entry : queues.entrySet()) {
                    processFlowQueueStatusMetrics(Name.join(Name.Prefix.C2_FLOW_STATUS_QUEUE, entry.getKey()), tags, entry.getValue());
                }
            }

            // TODO, Future enhancement: extract metrics from top level metrics: { ... } field and coerce meter/value type using something like a schema registry
            // This will require some external configuration of metric name -> meter type and value type

        } catch (Exception e) {
            logger.warn("Encountered exception while trying to capture heartbeat metrics", e);
        }
    }

    private void processLastSeenTimestamp(final Tags metricTags, final Long epochMilli) {
        final Double secondsSinceEpoch = epochMilli != null ? epochMilli / 1000.0 : System.currentTimeMillis() / 1000.0;

        lastSeenTimestampCache.update(metricTags, secondsSinceEpoch);
        Gauge.builder(Name.C2_HEARTBEAT_LAST_SEEN_TIME, lastSeenTimestampCache, lastSeenTimestampCache -> lastSeenTimestampCache.getGaugeValue(metricTags))
                .description("Timestamp (seconds since epoch) of the most recent heartbeat seen by the C2 server")
                .tags(metricTags)
                .baseUnit("seconds")
                .register(meterRegistry);

    }

    private void processAgentRepoStatusMetrics(final String metricNamePrefix, final Tags metricTags, final AgentRepositoryStatus repositoryStatus) {
        if (repositoryStatus.getSize() != null) {
            DistributionSummary.builder(Name.join(metricNamePrefix, Name.Suffix.SIZE))
                    .description("number of items in the repository")
                    .tags(metricTags)
                    .register(meterRegistry)
                    .record(repositoryStatus.getSize());
        }
        if (repositoryStatus.getSize() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.SIZE), metricTags).record(repositoryStatus.getSize());
        }
    }

    private void processFlowQueueStatusMetrics(final String metricNamePrefix, final Tags metricTags, final FlowQueueStatus flowQueueStatus) {

        if (flowQueueStatus.getSize() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.SIZE), metricTags).record(flowQueueStatus.getSize());
        }
        if (flowQueueStatus.getSizeMax() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.SIZE_MAX), metricTags).record(flowQueueStatus.getSizeMax());
        }
        if (flowQueueStatus.getSizeUtilization() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.SIZE_USAGE), metricTags).record(flowQueueStatus.getSizeUtilization());
        }
        if (flowQueueStatus.getDataSize() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.DATA_SIZE), metricTags).record(flowQueueStatus.getDataSize());
        }
        if (flowQueueStatus.getDataSizeMax() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.DATA_SIZE_MAX), metricTags).record(flowQueueStatus.getDataSizeMax());
        }
        if (flowQueueStatus.getDataSizeUtilization() != null) {
            meterRegistry.summary(Name.join(metricNamePrefix, Name.Suffix.DATE_SIZE_USAGE), metricTags).record(flowQueueStatus.getDataSizeUtilization());
        }

    }

    private String safeValue(String tagValue) {
        return tagValue != null ? tagValue : Name.Tag.EMPTY_VALUE;
    }

}
