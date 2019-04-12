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
import com.cloudera.cem.efm.db.entity.AgentClassEntity;
import com.cloudera.cem.efm.db.entity.AgentEntity;
import com.cloudera.cem.efm.db.entity.AgentManifestEntity;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.AgentStatus;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Condition;
import org.modelmapper.Converter;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
public class AgentConverterFactory {

    private ObjectMapper jacksonObjectMapper;

    public AgentConverterFactory() {
        this(new ObjectMapper());
    }

    @Autowired
    public AgentConverterFactory(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }


    /****************************
     ***  Agent PropertyMaps  ***
     ****************************/

    @Bean
    public PropertyMap<Agent, AgentEntity> agentToEntityMap() {
        return new PropertyMap<Agent, AgentEntity>() {
            @Override
            protected void configure() {
                map().setId(source.getIdentifier());
                using(agentStatusToStringConverter()).map(source.getStatus()).setAgentStatusContent(null);
                // other fields are mapped implicitly based on names
            }
        };
    }

    @Bean
    public PropertyMap<AgentEntity, Agent> entityToAgentMap() {
        return new PropertyMap<AgentEntity, Agent>() {
            @Override
            protected void configure() {
                map().setIdentifier(source.getId());
                using(stringToAgentStatusConverter()).map(source.getAgentStatusContent()).setStatus(null);
                // other fields are mapped implicitly based on names
            }
        };
    }


    /*********************************
     ***  AgentClass PropertyMaps  ***
     *********************************/

    @Bean
    public static PropertyMap<AgentClass, AgentClassEntity> agentClassToEntityMap() {
        return new PropertyMap<AgentClass, AgentClassEntity>() {
            @Override
            protected void configure() {
                map().setId(source.getName());
            }
        };
    }

    @Bean
    public static PropertyMap<AgentClassEntity, AgentClass> entityToAgentClassMap() {
        return new PropertyMap<AgentClassEntity, AgentClass>() {
            @Override
            protected void configure() {
                map().setName(source.getId());

                // if the entity set of agent manifest ids is empty, do not set the destination
                // so that this field is omitted when serializing to json (we serialize not-null by default)
                when((Condition<Set, Set>) context ->
                    context.getSource() != null && !context.getSource().isEmpty())
                        .map().setAgentManifests(source.getAgentManifests());
            }
        };
    }


    /**********************************
     ***  AgentManifest Converters  ***
     **********************************/

    @Bean
    public Converter<AgentManifest, AgentManifestEntity> agentManifestToEntityConverter() {
        return new AbstractConverter<AgentManifest, AgentManifestEntity>() {
            @Override
            protected AgentManifestEntity convert(AgentManifest source) {
                if (source == null) {
                    return null;
                }
                final String serializedAgentManifest;
                try {
                    serializedAgentManifest = jacksonObjectMapper.writeValueAsString(source);
                } catch (JsonProcessingException e) {
                    throw new MappingExceptionBuilder().addError(e).build();
                }
                final AgentManifestEntity entity = new AgentManifestEntity();
                entity.setId(source.getIdentifier());
                entity.setContent(serializedAgentManifest);
                return entity;
            }
        };
    }

    @Bean
    public Converter<AgentManifest, String> agentManifestToStringConverter() {
        return new AbstractConverter<AgentManifest, String>() {
            @Override
            protected String convert(AgentManifest source) {
                return mapAgentManifestToString(source);
            }
        };
    }

    @Bean
    public Converter<AgentManifestEntity, AgentManifest> entityToAgentManifestConverter() {
        return new AbstractConverter<AgentManifestEntity, AgentManifest>() {
            @Override
            protected AgentManifest convert(AgentManifestEntity source) {
                if (source == null) {
                    return null;
                }
                final AgentManifest agentManifest;
                if (source.getContent() != null) {
                    agentManifest = mapStringToAgentManifest(source.getContent());
                } else {
                    agentManifest = new AgentManifest();
                }
                agentManifest.setIdentifier(source.getId());
                return agentManifest;
            }
        };
    }

    @Bean
    public Converter<String, AgentManifest> stringToAgentManifestConverter() {
        return new AbstractConverter<String, AgentManifest>() {
            @Override
            protected AgentManifest convert(String source) {
                return mapStringToAgentManifest(source);
            }
        };
    }


    /********************************
     ***  AgentStatus Converters  ***
     ********************************/

    @Bean
    public Converter<AgentStatus, String> agentStatusToStringConverter() {
        return new AbstractConverter<AgentStatus, String>() {
            @Override
            protected String convert(AgentStatus source) {
                if (source == null) {
                    return null;
                }

                try {
                    return jacksonObjectMapper.writeValueAsString(source);
                } catch (JsonProcessingException e) {
                    throw new MappingExceptionBuilder().addError(e).build();
                }
            }
        };
    }

    @Bean
    public Converter<String, AgentStatus> stringToAgentStatusConverter() {
        return new AbstractConverter<String, AgentStatus>() {
            @Override
            protected AgentStatus convert(String source) {
                if (source == null) {
                    return null;
                }

                try {
                    return jacksonObjectMapper.readValue(source, AgentStatus.class);
                } catch (IOException e) {
                    throw new MappingExceptionBuilder().addError(e).build();
                }
            }
        };
    }


    /***********************
     ***  Helper Methods ***
     ***********************/

    private String mapAgentManifestToString(AgentManifest source) {
        if (source == null) {
            return null;
        }
        try {
            return jacksonObjectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new MappingExceptionBuilder().addError(e).build();
        }
    }

    private AgentManifest mapStringToAgentManifest(String source) {
        if (source == null) {
            return null;
        }
        try {
            return jacksonObjectMapper.readValue(source, AgentManifest.class);
        } catch (IOException e) {
            throw new MappingExceptionBuilder().addError(e).build();
        }
    }

    /**
     * Returns all converters this factory is capable of building.
     * Mainly used for test suites.
     */
    public static List<Converter<?,?>> allConverters() {
        AgentConverterFactory factory = new AgentConverterFactory();
        return Arrays.asList(
                // AgentManifest Converters
                factory.agentManifestToEntityConverter(),
                factory.entityToAgentManifestConverter(),
                factory.agentManifestToStringConverter(),
                factory.stringToAgentManifestConverter(),
                // AgentStatus Converters
                factory.agentStatusToStringConverter(),
                factory.stringToAgentStatusConverter()
        );
    }

    /**
     * Returns all property maps this factory is capable of building.
     * Mainly used for test suites.
     */
    public static List<PropertyMap<?,?>> allMaps() {
        AgentConverterFactory factory = new AgentConverterFactory();
        return Arrays.asList(
                // AgentClass PropertyMaps
                agentClassToEntityMap(),
                entityToAgentClassMap(),
                // Agent PropertyMapps
                factory.agentToEntityMap(),
                factory.entityToAgentMap()
        );
    }

}
