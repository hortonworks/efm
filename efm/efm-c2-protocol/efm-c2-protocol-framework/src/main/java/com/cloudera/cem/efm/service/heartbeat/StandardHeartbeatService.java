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
package com.cloudera.cem.efm.service.heartbeat;

import com.cloudera.cem.efm.db.entity.HeartbeatEntity;
import com.cloudera.cem.efm.db.repository.HeartbeatRepository;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StandardHeartbeatService extends BaseService implements HeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(StandardHeartbeatService.class);

    private final HeartbeatRepository heartbeatRepository;

    private boolean persistenceDisabled;
    private boolean contentPersistenceDisabled;

    @Autowired
    public StandardHeartbeatService(
            HeartbeatRepository heartbeatRepository,
            HeartbeatProperties heartbeatProperties,
            Validator validator,
            OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.heartbeatRepository = heartbeatRepository;

        final HeartbeatProperties.Retention metadataRetention = heartbeatProperties.getMetadata();
        final HeartbeatProperties.Retention contentRetention = heartbeatProperties.getContent();
        this.persistenceDisabled = metadataRetention != null && metadataRetention.isZeroRetentionConfigured();
        this.contentPersistenceDisabled = contentRetention != null && contentRetention.isZeroRetentionConfigured();
    }

    @Override
    @Transactional
    public C2Heartbeat createHeartbeat(C2Heartbeat heartbeat) {
        if (heartbeat.getIdentifier() == null) {
            heartbeat.setIdentifier(UUID.randomUUID().toString());
        }
        if (heartbeat.getCreated() == null) {
            heartbeat.setCreated(System.currentTimeMillis());
        }

        validate(heartbeat, "Cannot create heartbeat");

        if (persistenceDisabled) {
            return heartbeat;
        }

        final HeartbeatEntity entity = modelMapper.map(heartbeat, HeartbeatEntity.class);

        if (contentPersistenceDisabled) {
            // TODO, create an alternative model mapper that prevents the content from being created in the first place
            entity.setContent(null);
        }

        final HeartbeatEntity savedEntity = heartbeatRepository.save(entity);
        final C2Heartbeat savedHeartbeat = modelMapper.map(savedEntity, C2Heartbeat.class);
        return savedHeartbeat;
    }

    @Override
    public List<C2Heartbeat> getHeartbeats() {
        return null;
    }

    @Override
    public Optional<C2Heartbeat> getHeartbeat(String heartbeatId) {
        if (heartbeatId == null) {
            throw new IllegalArgumentException("operationId cannot be null");
        }
        final Optional<HeartbeatEntity> entityOptional = heartbeatRepository.findById(heartbeatId);
        final Optional<C2Heartbeat> heartbeatOptional = modelMapper.mapOptional(entityOptional, C2Heartbeat.class);
        return heartbeatOptional;
    }
}
