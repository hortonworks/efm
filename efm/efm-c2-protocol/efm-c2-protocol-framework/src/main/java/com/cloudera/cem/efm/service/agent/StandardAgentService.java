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
package com.cloudera.cem.efm.service.agent;

import com.cloudera.cem.efm.db.entity.AgentClassEntity;
import com.cloudera.cem.efm.db.entity.AgentEntity;
import com.cloudera.cem.efm.db.entity.AgentManifestEntity;
import com.cloudera.cem.efm.db.projection.IdAndTimestamp;
import com.cloudera.cem.efm.db.repository.AgentClassRepository;
import com.cloudera.cem.efm.db.repository.AgentManifestRepository;
import com.cloudera.cem.efm.db.repository.AgentRepository;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class StandardAgentService extends BaseService implements AgentService {

    private static final Logger logger = LoggerFactory.getLogger(StandardAgentService.class);

    private final AgentManifestRepository agentManifestRepository;
    private final AgentClassRepository agentClassRepository;
    private final AgentRepository agentRepository;

    @Autowired
    public StandardAgentService(
            final AgentManifestRepository agentManifestRepository,
            final AgentClassRepository agentClassRepository,
            final AgentRepository agentRepository,
            final Validator validator,
            final OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.agentManifestRepository = agentManifestRepository;
        this.agentClassRepository = agentClassRepository;
        this.agentRepository = agentRepository;
    }

    //**********************************
    //***  Agent Class CRUD methods  ***
    //**********************************

    @Override
    public List<AgentClass> getAgentClasses() {
        final List<AgentClass> agentClasses = new ArrayList<>();
        agentClassRepository.findAll().forEach(entity -> {
            final AgentClass agentClass = modelMapper.map(entity, AgentClass.class);
            agentClasses.add(agentClass);
        });
        return agentClasses;
    }

    @Override
    public AgentClass createAgentClass(AgentClass agentClass) {
        validate(agentClass, "Cannot create agent class");

        if (agentClassRepository.existsById(agentClass.getName())) {
            throw new IllegalStateException(
                    String.format("Agent class already exists with name='%s'", agentClass.getName()));
        }
        final AgentClassEntity entity = modelMapper.map(agentClass, AgentClassEntity.class);
        return saveAgentClassEntity(entity);
    }

    @Override
    public Optional<AgentClass> getAgentClass(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        final Optional<AgentClassEntity> entityOptional = agentClassRepository.findById(name);
        return modelMapper.mapOptional(entityOptional, AgentClass.class);
    }

    @Override
    public AgentClass updateAgentClass(AgentClass agentClass) {
        validate(agentClass, "Cannot update agent class");
        final Optional<AgentClassEntity> entityOptional = agentClassRepository.findById(agentClass.getName());
        final AgentClassEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Agent class not found with name " + agentClass.getName()));
        modelMapper.map(agentClass, entity);
        return saveAgentClassEntity(entity);
    }

    @Override
    public AgentClass deleteAgentClass(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        final Optional<AgentClassEntity> entityOptional = agentClassRepository.findById(name);
        final AgentClassEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Agent class not found with name " + name));
        agentClassRepository.deleteById(name);
        final AgentClass deletedAgentClass = modelMapper.map(entity, AgentClass.class);
        return deletedAgentClass;
    }

    private AgentClass saveAgentClassEntity(AgentClassEntity agentClassEntity) {
        if (agentClassEntity.getAgentManifests() != null) {
            agentClassEntity.getAgentManifests().forEach(agentManifestId -> {
                if (!agentManifestRepository.existsById(agentManifestId)) {
                    throw new IllegalStateException("Cannot persist agent class. " +
                            "It references a nonexistent agent manifest id " + agentManifestId);
                }
            });
        }
        final AgentClassEntity savedEntity = agentClassRepository.save(agentClassEntity);
        final AgentClass savedAgentClass = modelMapper.map(savedEntity, AgentClass.class);
        return savedAgentClass;
    }


    //*************************************
    //***  Agent Manifest CRUD methods  ***
    //*************************************

    @Override
    public AgentManifest createAgentManifest(AgentManifest agentManifest) {
        validate(agentManifest, "Could not create agent manifest");
        if (agentManifest.getIdentifier() == null) {
            agentManifest.setIdentifier(UUID.randomUUID().toString());
        }

        if (agentManifestRepository.existsById(agentManifest.getIdentifier())) {
            throw new IllegalStateException(
                    String.format("Agent manifest already exists with id='%s'", agentManifest.getIdentifier()));
        }
        final AgentManifestEntity entity = modelMapper.map(agentManifest, AgentManifestEntity.class);
        final AgentManifestEntity savedEntity = agentManifestRepository.save(entity);
        final AgentManifest savedAgentManifest = modelMapper.map(savedEntity, AgentManifest.class);
        return savedAgentManifest;
    }

    @Override
    public List<AgentManifest> getAgentManifests() {
        final List<AgentManifest> agentManifests = new ArrayList<>();
        agentManifestRepository.findAll().forEach(entity -> {
            final AgentManifest agentManifest = modelMapper.map(entity, AgentManifest.class);
            agentManifests.add(agentManifest);
        });
        return agentManifests;
    }

    @Override
    public List<AgentManifest> getAgentManifests(String agentClassName) {
        if (agentClassName == null) {
            throw new IllegalArgumentException("Agent class name cannot be null");
        }

        final List<AgentManifest> agentManifests = new ArrayList<>();
        Optional<AgentClassEntity> agentClassEntityOptional = agentClassRepository.findById(agentClassName);
        if (agentClassEntityOptional.isPresent()) {
            Set<String> manifestIds = agentClassEntityOptional.get().getAgentManifests();
            if (manifestIds != null && !manifestIds.isEmpty()) {
                Iterable<AgentManifestEntity> retrievedAgentManifests = agentManifestRepository.findAllById(manifestIds);
                retrievedAgentManifests.forEach(entity -> {
                    final AgentManifest agentManifest = modelMapper.map(entity, AgentManifest.class);
                    agentManifests.add(agentManifest);
                });
            }
        }
        return agentManifests;
    }

    @Override
    public Optional<AgentManifest> getAgentManifest(String manifestId) {
        if (manifestId == null) {
            throw new IllegalArgumentException("Agent manifest id must not be null");
        }

        final Optional<AgentManifestEntity> entityOptional = agentManifestRepository.findById(manifestId);
        return modelMapper.mapOptional(entityOptional, AgentManifest.class);
    }

    @Override
    public AgentManifest deleteAgentManifest(String manifestId) {
        if (manifestId == null) {
            throw new IllegalArgumentException("Agent manifest id must not be null");
        }

        final Optional<AgentManifestEntity> entityOptional = agentManifestRepository.findById(manifestId);
        final AgentManifestEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Agent manifest not found with id " + manifestId));
        agentManifestRepository.deleteById(manifestId);
        final AgentManifest deletedAgentManifest = modelMapper.map(entity, AgentManifest.class);
        return deletedAgentManifest;
    }


    //****************************
    //***  Agent CRUD methods  ***
    //****************************

    @Override
    public Agent createAgent(Agent agent) {
        validate(agent, "Cannot create agent");
        if (agentRepository.existsById(agent.getIdentifier())) {
            throw new IllegalStateException(
                    String.format("Agent already exists with id '%s'", agent.getIdentifier()));
        }
        final AgentEntity entity = modelMapper.map(agent, AgentEntity.class);
        final AgentEntity savedEntity = agentRepository.save(entity);
        final Agent savedAgent = modelMapper.map(savedEntity, Agent.class);
        return savedAgent;
    }

    @Override
    public List<Agent> getAgents() {
        final List<Agent> agents = new ArrayList<>();
        agentRepository.findAll().forEach(entity -> {
            final Agent agent = modelMapper.map(entity, Agent.class);
            agents.add(agent);
        });
        return agents;
    }

    @Override
    public List<Agent> getAgents(String agentClassName) {
        if (agentClassName == null) {
            throw new IllegalArgumentException("agentClassName cannot be null");
        }

        final List<Agent> agents = new ArrayList<>();
        agentRepository.findAllByAgentClass(agentClassName).forEach(entity -> {
            final Agent agent = modelMapper.map(entity, Agent.class);
            agents.add(agent);
        });
        return agents;

    }

    @Override
    public Optional<Agent> getAgent(String agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent id must not be null");
        }

        final Optional<AgentEntity> entityOptional = agentRepository.findById(agentId);
        return modelMapper.mapOptional(entityOptional, Agent.class);
    }

    @Override
    public Agent updateAgent(Agent agent) {
        validate(agent, "Cannot update agent");

        final Optional<AgentEntity> entityOptional = agentRepository.findById(agent.getIdentifier());
        final AgentEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Agent not found with is " + agent.getIdentifier()));
        final Date existingFirstSeenDate = entity.getFirstSeen();
        modelMapper.map(agent, entity);
        if (existingFirstSeenDate != null) {
            entity.setFirstSeen(existingFirstSeenDate);  // firstSeen timestamp, once set, is immutable
        }
        final AgentEntity updatedEntity = agentRepository.save(entity);
        final Agent updatedAgent = modelMapper.map(updatedEntity, Agent.class);
        return updatedAgent;
    }

    @Override
    public Agent deleteAgent(String agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("agentId cannot be null");
        }

        final Optional<AgentEntity> entityOptional = agentRepository.findById(agentId);
        final AgentEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Agent not found with id " + agentId));
        agentRepository.deleteById(agentId);
        final Agent deletedAgent = modelMapper.map(entity, Agent.class);
        return deletedAgent;
    }

    //****************************
    //***  Additional methods  ***
    //****************************


    @Override
    public Map<String, Instant> getAgentClassLastSeens() {
        Map<String, Instant> lastSeenTimestamps = new HashMap<>((int) agentClassRepository.count());
        agentRepository.findAllAgentClassLastSeens().forEach((lastSeen) -> {
            lastSeenTimestamps.put(lastSeen.getId(), lastSeen.getTimestamp().toInstant());
        });
        return lastSeenTimestamps;
    }

    @Override
    public Optional<Instant> getAgentClassLastSeen(String agentClass) {
        if (agentClass == null) {
            throw new IllegalArgumentException("Agent class must not be null");
        }
        final Optional<IdAndTimestamp> agentClassLastSeenOptional = agentRepository.findAgentClassLastSeenById(agentClass);
        return agentClassLastSeenOptional.map(agentLastSeen -> agentLastSeen.getTimestamp().toInstant());
    }

    @Override
    public Map<String, Instant> getAgentManifestLastSeens() {
        Map<String, Instant> lastSeenTimestamps = new HashMap<>((int) agentManifestRepository.count());
        agentRepository.findAllAgentManifestLastSeens().forEach((lastSeen) -> {
            lastSeenTimestamps.put(lastSeen.getId(), lastSeen.getTimestamp().toInstant());
        });
        return lastSeenTimestamps;
    }

    @Override
    public Optional<Instant> getAgentManifestLastSeen(String agentManifestId) {
        if (agentManifestId == null) {
            throw new IllegalArgumentException("Agent manifest id must not be null");
        }
        final Optional<IdAndTimestamp> optional = agentRepository.findAgentManifestLastSeenById(agentManifestId);
        return optional.map(agentLastSeen -> agentLastSeen.getTimestamp().toInstant());
    }

    @Override
    public Map<String, Instant> getAgentLastSeens() {
        Map<String, Instant> lastSeenTimestamps = new HashMap<>((int) agentRepository.count());
        agentRepository.findAllAgentLastSeens().forEach((agentLastSeen) -> {
           lastSeenTimestamps.put(agentLastSeen.getId(), agentLastSeen.getTimestamp().toInstant());
        });
        return lastSeenTimestamps;
    }

    @Override
    public Optional<Instant> getAgentLastSeen(String agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent id must not be null");
        }
        final Optional<IdAndTimestamp> agentLastSeenOptional = agentRepository.findAgentLastSeenById(agentId);
        return agentLastSeenOptional.map(agentLastSeen -> agentLastSeen.getTimestamp().toInstant());
    }
}
