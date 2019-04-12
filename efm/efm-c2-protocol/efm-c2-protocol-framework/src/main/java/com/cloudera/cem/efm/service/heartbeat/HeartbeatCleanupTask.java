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
package com.cloudera.cem.efm.service.heartbeat;

import com.cloudera.cem.efm.db.projection.IdAndNumber;
import com.cloudera.cem.efm.db.projection.IdAndTimestamp;
import com.cloudera.cem.efm.db.repository.HeartbeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class HeartbeatCleanupTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatCleanupTask.class);

    private final HeartbeatRepository heartbeatRepository;

    private final HeartbeatProperties.Retention metadataRetention;
    private final HeartbeatProperties.Retention contentRetention;

    @Autowired
    public HeartbeatCleanupTask(
            HeartbeatRepository heartbeatRepository,
            HeartbeatProperties heartbeatProperties) {
        this.heartbeatRepository = heartbeatRepository;
        this.metadataRetention = heartbeatProperties.getMetadata();
        this.contentRetention = heartbeatProperties.getContent();
    }

    @Transactional
    @Override
    public void run() {

        // entire records
        if (metadataRetention != null) {

            if (metadataRetention.getMaxAgeToKeep() != null) {
                try {
                    final long maxAgeToKeepMillis = metadataRetention.getMaxAgeToKeep().toMillis();
                    final Date oldestToKeep = Date.from(Instant.now().minusMillis(maxAgeToKeepMillis));
                    logger.debug("Deleting heartbeats from before '{}'", oldestToKeep);
                    heartbeatRepository.deleteByCreatedBefore(oldestToKeep);
                } catch (Throwable t) {
                    logger.warn("Unable to delete heartbeats due to: " + t.getLocalizedMessage(), t);
                }
            }


            if (metadataRetention.getMaxCountToKeep() != null) {
                try {
                    if (metadataRetention.getMaxCountToKeep().equals(0)) {
                        logger.debug("Deleting all heartbeats");
                        heartbeatRepository.deleteAll();
                    } else {
                        logger.debug("Deleting heartbeats older than most recent {} per agent", metadataRetention.getMaxCountToKeep());
                        for (IdAndNumber counts : heartbeatRepository.findHeartbeatCountsByAgentId()) {
                            if (metadataRetention.getMaxCountToKeep().compareTo(counts.getNumber().intValue()) < 0) {
                                final String agentId = counts.getId();
                                if (agentId != null) {
                                    final List<IdAndTimestamp> heartbeatTimes =
                                            heartbeatRepository.findHeartbeatTimestampsByAgentId(agentId, PageRequest.of(0, metadataRetention.getMaxCountToKeep()));
                                    if (heartbeatTimes.size() > 0) {
                                        final IdAndTimestamp lastToKeep = heartbeatTimes.get(heartbeatTimes.size() - 1);
                                        if (lastToKeep != null && lastToKeep.getTimestamp() != null) {
                                            heartbeatRepository.deleteByAgentIdEqualsAndCreatedBefore(agentId, lastToKeep.getTimestamp());
                                        }
                                    }
                                } else {
                                    final List<IdAndTimestamp> heartbeatTimes = heartbeatRepository.findHeartbeatTimestampsByAgentIdIsNull(PageRequest.of(0, metadataRetention.getMaxCountToKeep()));
                                    if (heartbeatTimes.size() > 0) {
                                        final IdAndTimestamp lastToKeep = heartbeatTimes.get(heartbeatTimes.size() - 1);
                                        if (lastToKeep != null && lastToKeep.getTimestamp() != null) {
                                            heartbeatRepository.deleteByAgentIdIsNullAndCreatedBefore(lastToKeep.getTimestamp());
                                        }
                                    }
                                }
                            } else {
                                // findHeartbeatCountsByAgentId() results are sorted by count desc
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("Unable to delete heartbeats due to: " + t.getLocalizedMessage(), t);
                }
            }
        }

        // content only
        if (contentRetention != null) {

            if (contentRetention.getMaxAgeToKeep() != null) {
                try {
                    final long contentMaxAgeToKeepMillis = contentRetention.getMaxAgeToKeep().toMillis();
                    final Date contentOldestToKeep = Date.from(Instant.now().minusMillis(contentMaxAgeToKeepMillis));
                    logger.debug("Deleting heartbeat content from before '{}'", contentOldestToKeep);
                    heartbeatRepository.deleteContentByCreatedBefore(contentOldestToKeep);
                } catch (Throwable t) {
                    logger.warn("Unable to delete heartbeat content due to: " + t.getLocalizedMessage(), t);
                }
            }

            if (contentRetention.getMaxCountToKeep() != null) {
                try {
                    if (contentRetention.getMaxCountToKeep().equals(0)) {
                        logger.debug("Deleting all heartbeat content", contentRetention.getMaxCountToKeep());
                        heartbeatRepository.deleteAllContent();
                    } else {
                        logger.debug("Deleting heartbeat content older than most recent {} per agent", contentRetention.getMaxCountToKeep());
                        for (IdAndNumber counts : heartbeatRepository.findHeartbeatContentCountsByAgentId()) {
                            if (contentRetention.getMaxCountToKeep().compareTo(counts.getNumber().intValue()) < 0) {
                                final String agentId = counts.getId();
                                if (agentId != null) {
                                    final List<IdAndTimestamp> heartbeatContentTimes =
                                            heartbeatRepository.findHeartbeatContentTimestampsByAgentId(agentId, PageRequest.of(0, contentRetention.getMaxCountToKeep()));
                                    if (heartbeatContentTimes.size() > 0) {
                                        final IdAndTimestamp lastToKeep = heartbeatContentTimes.get(heartbeatContentTimes.size()-1);
                                        if (lastToKeep != null && lastToKeep.getTimestamp() != null) {
                                            heartbeatRepository.deleteContentByAgentIdEqualsAndCreatedBefore(agentId, lastToKeep.getTimestamp());
                                        }
                                    }
                                } else {
                                    final List<IdAndTimestamp> heartbeatContentTimes =
                                            heartbeatRepository.findHeartbeatContentTimestampsByAgentIdIsNull(PageRequest.of(0, contentRetention.getMaxCountToKeep()));
                                    if (heartbeatContentTimes.size() > 0) {
                                        final IdAndTimestamp lastToKeep = heartbeatContentTimes.get(heartbeatContentTimes.size()-1);
                                        if (lastToKeep != null && lastToKeep.getTimestamp() != null) {
                                            heartbeatRepository.deleteContentByAgentIdIsNullAndCreatedBefore(lastToKeep.getTimestamp());
                                        }
                                    }
                                }
                            } else {
                                // findHeartbeatCountsByAgentId() results are sorted by count desc
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("Unable to delete heartbeat content due to: " + t.getLocalizedMessage(), t);
                }
            }
        }

    }
}
