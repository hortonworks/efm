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
package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.db.entity.FlowEntity;
import com.cloudera.cem.efm.db.entity.FlowMappingEntity;
import com.cloudera.cem.efm.db.repository.FlowMappingRepository;
import com.cloudera.cem.efm.db.repository.FlowRepository;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.FlowFormat;
import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.model.FlowSummary;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.service.BaseService;
import com.cloudera.cem.efm.service.agent.AgentInfoException;
import com.cloudera.cem.efm.service.flow.serialize.ConfigSchemaSerializer;
import com.cloudera.cem.efm.service.flow.transform.FlowTransformService;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.minifi.commons.schema.ConfigSchema;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class StandardC2FlowService extends BaseService implements C2FlowService {

    private static final Logger logger = LoggerFactory.getLogger(StandardC2FlowService.class);

    private final FlowRepository flowRepository;
    private final FlowMappingRepository flowMappingRepository;
    private final FlowTransformService<ConfigSchema> flowTransformService;
    private final ConfigSchemaSerializer configSchemaSerializer;

    @Autowired
    public StandardC2FlowService(
            final FlowRepository flowRepository,
            final FlowMappingRepository flowMappingRepository,
            final FlowTransformService<ConfigSchema> flowTransformService,
            final ConfigSchemaSerializer configSchemaSerializer,
            final Validator validator,
            final OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.flowRepository = flowRepository;
        this.flowMappingRepository = flowMappingRepository;
        this.flowTransformService = flowTransformService;
        this.configSchemaSerializer = configSchemaSerializer;
    }

    //***********************************
    //***  Flow CRUD methods  ***
    //***********************************

    @Override
    public List<FlowSummary> getFlowSummaries() {
        final List<FlowSummary> summaries = new ArrayList<>();
        for (final FlowRepository.FlowSummary dbSummary : flowRepository.findByIdIsNotNull()) {
            final FlowSummary summary = getFlowSummary(dbSummary);
            summaries.add(summary);
        }
        return summaries;
    }

    @Override
    public Optional<FlowSummary> getFlowSummary(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FlowRepository.FlowSummary> dbFlowSummary = flowRepository.findByIdAndRegistryUrlNotNull(flowId);
        if (dbFlowSummary.isPresent()) {
            final FlowSummary flowSummary = getFlowSummary(dbFlowSummary.get());
            return Optional.of(flowSummary);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Flow> getFlow(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            return Optional.empty();
        }

        final Flow flow = new Flow();
        flow.setId(flowEntity.get().getId());
        flow.setRegistryUrl(flowEntity.get().getRegistryUrl());
        flow.setRegistryBucketId(flowEntity.get().getRegistryBucketId());
        flow.setRegistryFlowId(flowEntity.get().getRegistryFlowId());
        flow.setRegistryFlowVersion(flowEntity.get().getRegistryFlowVersion());
        flow.setFlowFormat(flowEntity.get().getFlowFormat());
        flow.setFlowContent(flowEntity.get().getFlowContent());
        flow.setCreated(flowEntity.get().getCreated());
        flow.setUpdated(flowEntity.get().getUpdated());

        return Optional.of(flow);
    }

    @Override
    public Optional<String> getFlowContent(final String flowId, final FlowFormat flowFormat) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            return Optional.empty();
        }

        if (flowFormat == FlowFormat.YAML_V2_TYPE) {
            return Optional.of(flowEntity.get().getFlowContent());
        } else {
            // We don't really have this case right now, but there may be more formats in the future
            throw new IllegalArgumentException("Unsupported flow format: " + flowFormat);
        }
    }

    @Override
    public void getFlowContent(final String flowId, final FlowFormat flowFormat, final OutputStream out) throws IOException {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (flowFormat == null) {
            throw new IllegalArgumentException("Flow format cannot be null");
        }

        if (out == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }

        final Optional<FlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("A flow does not exist for the given id");
        }

        final String flowContent = flowEntity.get().getFlowContent();
        if (flowFormat == FlowFormat.YAML_V2_TYPE) {
            out.write(flowContent.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } else {
            // We don't really have this case right now, but there may be more formats in the future
            throw new IllegalArgumentException("Unsupported flow format: " + flowFormat);
        }
    }

    @Override
    public FlowSummary createFlow(final String flowContent) {
        if (StringUtils.isBlank(flowContent)) {
            throw new IllegalArgumentException("Flow content cannot be null or blank");
        }

        // validates that the flow content is at least valid YAML
        try (final InputStream in = new ByteArrayInputStream(flowContent.getBytes(StandardCharsets.UTF_8))) {
            configSchemaSerializer.deserialize(in);
        } catch (Exception e) {
            throw new BadRequestException("Flow content must be valid YAML", e);
        }

        final FlowEntity flowEntity = new FlowEntity();
        flowEntity.setId(UUID.randomUUID().toString());
        flowEntity.setFlowContent(flowContent);
        flowEntity.setFlowFormat(FlowFormat.YAML_V2_TYPE);

        final FlowEntity savedEntity = flowRepository.save(flowEntity);
        return getFlowSummary(savedEntity);
    }

    @Override
    public FlowSummary createFlow(final FlowSnapshot flowSnapshot) throws IOException {
        if (flowSnapshot == null) {
            throw new IllegalArgumentException("Flow snapshot cannot be null");
        }

        final FlowUri flowUri = flowSnapshot.getFlowUri();
        if (flowUri == null) {
            throw new IllegalArgumentException("Flow URI cannot be null");
        }

        final VersionedFlowSnapshot versionedFlowSnapshot = flowSnapshot.getVersionedFlowSnapshot();
        if (versionedFlowSnapshot == null) {
            throw new IllegalArgumentException("Versioned flow snapshot cannot be null");
        }

        final VersionedFlowSnapshotMetadata versionedFlowSnapshotMetadata = versionedFlowSnapshot.getSnapshotMetadata();
        if (versionedFlowSnapshotMetadata == null) {
            throw new IllegalArgumentException("Versioned flow snapshot metadata cannot be null");
        }

        final ConfigSchema configSchema = flowTransformService.transform(versionedFlowSnapshot);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        configSchemaSerializer.serialize(configSchema, out);
        out.flush();

        // TODO figure out if it is safe to use UTF-8 since the serializer doesn't specify
        final String serializedConfigSchema = new String(out.toByteArray(), StandardCharsets.UTF_8);

        final FlowEntity flowEntity = new FlowEntity();
        flowEntity.setId(UUID.randomUUID().toString());
        flowEntity.setRegistryUrl(flowUri.getRegistryUrl());
        flowEntity.setRegistryBucketId(flowUri.getBucketId());
        flowEntity.setRegistryFlowId(flowUri.getFlowId());
        flowEntity.setRegistryFlowVersion(versionedFlowSnapshot.getSnapshotMetadata().getVersion());
        flowEntity.setFlowContent(serializedConfigSchema);
        flowEntity.setFlowFormat(FlowFormat.YAML_V2_TYPE);

        final FlowEntity savedEntity = flowRepository.save(flowEntity);
        return getFlowSummary(savedEntity);
    }

    private FlowSummary getFlowSummary(FlowRepository.FlowSummary dbSummary) {
        final FlowSummary summary = new FlowSummary();
        summary.setId(dbSummary.getId());
        summary.setRegistryUrl(dbSummary.getRegistryUrl());
        summary.setRegistryBucketId(dbSummary.getRegistryBucketId());
        summary.setRegistryFlowId(dbSummary.getRegistryFlowId());
        summary.setRegistryFlowVersion(dbSummary.getRegistryFlowVersion());
        summary.setFlowFormat(dbSummary.getFlowFormat());
        summary.setCreated(dbSummary.getCreated());
        summary.setUpdated(dbSummary.getUpdated());
        return summary;
    }


    //***********************************
    //***  FlowMapping CRUD methods  ***
    //***********************************


    @Override
    public FlowMapping createFlowMapping(final FlowMapping flowMapping) {
        if (flowMapping == null) {
            throw new IllegalArgumentException("Flow mapping cannot be null");
        }

        validate(flowMapping, "Cannot create flow mapping");

        final Optional<FlowEntity> optionalFlow = flowRepository.findById(flowMapping.getFlowId());
        if (!optionalFlow.isPresent()) {
            throw new NotFoundException("No flow exists with id " + flowMapping.getFlowId());
        }

        final FlowEntity flowEntity = optionalFlow.get();

        final FlowMappingEntity flowMappingEntity = new FlowMappingEntity();
        flowMappingEntity.setAgentClass(flowMapping.getAgentClass());
        flowMappingEntity.setFlowEntity(flowEntity);

        flowMappingRepository.save(flowMappingEntity);
        return flowMapping;
    }

    @Override
    public List<FlowMapping> getFlowMappings() {
        final List<FlowMapping> flowMappings = new ArrayList<>();

        for (FlowMappingEntity entity : flowMappingRepository.findAll()) {
            final FlowMapping flowMapping = createFlowMapping(entity);
            flowMappings.add(flowMapping);
        }

        return flowMappings;
    }

    @Override
    public Optional<FlowMapping> getFlowMapping(final String agentClass) {
        if (StringUtils.isBlank(agentClass)) {
            throw new AgentInfoException("Agent class cannot be null or blank");
        }

        final Optional<FlowMappingEntity> optionalFlowMapping = flowMappingRepository.findById(agentClass);
        if (optionalFlowMapping.isPresent()) {
            final FlowMapping flowMapping = createFlowMapping(optionalFlowMapping.get());
            return Optional.of(flowMapping);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FlowMapping updateFlowMapping(final FlowMapping flowMapping) {
        if (flowMapping == null) {
            throw new IllegalStateException("Flow mapping cannot be null");
        }

        validate(flowMapping, "Cannot update flow mapping");

        final Optional<FlowMappingEntity> optionalFlowMapping = flowMappingRepository.findById(flowMapping.getAgentClass());
        if (!optionalFlowMapping.isPresent()) {
            throw new NotFoundException("No flow mapping exists for agent class '" + flowMapping.getAgentClass() + "'");
        }

        final Optional<FlowEntity> optionalFlow = flowRepository.findById(flowMapping.getFlowId());
        if (!optionalFlow.isPresent()) {
            throw new NotFoundException("No flow exists with id '" + flowMapping.getFlowId() + "'");
        }

        final FlowEntity flowEntity = optionalFlow.get();
        final FlowMappingEntity existingFlowMapping = optionalFlowMapping.get();
        existingFlowMapping.setFlowEntity(flowEntity);
        flowMappingRepository.save(existingFlowMapping);

        return flowMapping;
    }

    @Override
    public FlowMapping deleteFlowMapping(final String agentClass) {
        if (StringUtils.isBlank(agentClass)) {
            throw new IllegalArgumentException("Agent class cannot be null or blank");
        }

        final Optional<FlowMappingEntity> optionalFlowMapping = flowMappingRepository.findById(agentClass);
        if (!optionalFlowMapping.isPresent()) {
            throw new NotFoundException("No flow mapping exists for agent class '" + agentClass + "'");
        }

        final FlowMappingEntity existingFlowMapping = optionalFlowMapping.get();
        final FlowMapping deletedFlowMapping = createFlowMapping(existingFlowMapping);
        flowMappingRepository.delete(existingFlowMapping);
        return deletedFlowMapping;
    }

    private FlowMapping createFlowMapping(final FlowMappingEntity entity) {
        final FlowMapping flowMapping = new FlowMapping();
        flowMapping.setAgentClass(entity.getAgentClass());
        flowMapping.setFlowId(entity.getFlowEntity().getId());
        return flowMapping;
    }

}
