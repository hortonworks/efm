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
package com.cloudera.cem.efm.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudera.cem.efm.db.entity.HeartbeatEntity;
import com.cloudera.cem.efm.model.C2Heartbeat;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class HeartbeatConverterFactory {

    private ObjectMapper jacksonObjectMapper;

    public HeartbeatConverterFactory() {
        jacksonObjectMapper = new ObjectMapper();
    }

    @Autowired
    public HeartbeatConverterFactory(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    @Bean
    public Converter<C2Heartbeat, HeartbeatEntity> c2HeartbeatToEntityConverter() {
        return new AbstractConverter<C2Heartbeat, HeartbeatEntity>() {
            @Override
            protected HeartbeatEntity convert(C2Heartbeat source) {
                final String serializedC2Heartbeat;
                try {
                    serializedC2Heartbeat = jacksonObjectMapper.writeValueAsString(source);
                } catch (JsonProcessingException e) {
                    throw new MappingExceptionBuilder().addError(e).build();
                }
                final HeartbeatEntity entity = new HeartbeatEntity();
                entity.setId(source.getIdentifier());
                entity.setDeviceId(source.getDeviceId());
                entity.setAgentManifestId(source.getAgentManifestId());
                entity.setAgentClass(source.getAgentClass());
                entity.setAgentId(source.getAgentId());
                entity.setFlowId(source.getFlowId());
                entity.setContent(serializedC2Heartbeat);
                return entity;
            }
        };
    }

    @Bean
    public Converter<HeartbeatEntity, C2Heartbeat> entityToC2HeartbeatConverter() {
        return new AbstractConverter<HeartbeatEntity, C2Heartbeat>() {
            @Override
            protected C2Heartbeat convert(HeartbeatEntity source) {
                final C2Heartbeat heartbeat;
                if (source.getContent() != null) {
                    try {
                        heartbeat = jacksonObjectMapper.readValue(source.getContent(), C2Heartbeat.class);
                    } catch (IOException e) {
                        throw new MappingExceptionBuilder().addError(e).build();
                    }
                } else {
                    heartbeat = new C2Heartbeat();
                }
                heartbeat.setIdentifier(source.getId());
                heartbeat.setCreated(source.getCreated().getTime());
                return heartbeat;
            }
        };
    }

    /**
     * Returns all converters this factory is capable of building.
     * Mainly used for test suites.
     */
    public static List<Converter<?,?>> allConverters() {
        HeartbeatConverterFactory factory = new HeartbeatConverterFactory();
        List<Converter<?,?>> converters = Arrays.asList(
                factory.c2HeartbeatToEntityConverter(),
                factory.entityToC2HeartbeatConverter()
        );
        return converters;
    }

}
