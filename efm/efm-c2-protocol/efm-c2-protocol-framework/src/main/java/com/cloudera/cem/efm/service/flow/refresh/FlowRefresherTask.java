/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.flow.refresh;

import com.cloudera.cem.efm.annotation.ConditionalOnNiFiRegistry;
import com.cloudera.cem.efm.db.entity.AgentClassEntity;
import com.cloudera.cem.efm.db.entity.FlowEntity;
import com.cloudera.cem.efm.db.entity.FlowMappingEntity;
import com.cloudera.cem.efm.db.repository.AgentClassRepository;
import com.cloudera.cem.efm.db.repository.FlowMappingRepository;
import com.cloudera.cem.efm.db.repository.FlowRepository;
import com.cloudera.cem.efm.model.FlowFormat;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.service.flow.mapping.FlowMapper;
import com.cloudera.cem.efm.service.flow.mapping.FlowMapperException;
import com.cloudera.cem.efm.service.flow.retrieval.FlowRetrievalException;
import com.cloudera.cem.efm.service.flow.retrieval.FlowRetrievalService;
import com.cloudera.cem.efm.service.flow.serialize.Serializer;
import com.cloudera.cem.efm.service.flow.serialize.SerializerException;
import com.cloudera.cem.efm.service.flow.transform.FlowTransformException;
import com.cloudera.cem.efm.service.flow.transform.FlowTransformService;
import org.apache.nifi.minifi.commons.schema.ConfigSchema;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains the core logic for comparing cached versions of flows stored in the C2 server's DB, against the
 * latest versions available from the configured NiFi registry.
 *
 * This task will be scheduled by the FlowRefresher and will only be present when the @NiFiRegistryProfile is enabled.
 */
@Service
@ConditionalOnNiFiRegistry
public class FlowRefresherTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRefresherTask.class);

    private final AgentClassRepository agentClassRepository;
    private final FlowMapper flowMapper;
    private final FlowRetrievalService flowRetrievalService;
    private final FlowTransformService<ConfigSchema> flowTransformService;
    private final Serializer<ConfigSchema> configSchemaSerializer;
    private final FlowRepository flowRepository;
    private final FlowMappingRepository flowMappingRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public FlowRefresherTask(
            final AgentClassRepository agentClassRepository,
            final FlowMapper flowMapper,
            final FlowRetrievalService flowRetrievalService,
            final FlowTransformService<ConfigSchema> flowTransformService,
            final Serializer<ConfigSchema> configSchemaSerializer,
            final FlowRepository flowRepository,
            final FlowMappingRepository flowMappingRepository,
            final TransactionTemplate transactionTemplate) {
        this.agentClassRepository = agentClassRepository;
        this.flowMapper = flowMapper;
        this.flowRetrievalService = flowRetrievalService;
        this.flowTransformService = flowTransformService;
        this.configSchemaSerializer = configSchemaSerializer;
        this.flowRepository = flowRepository;
        this.flowMappingRepository = flowMappingRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run() {
        LOGGER.debug("******************************************************************************");
        LOGGER.debug("Beginning C2 flow refresher task...");
        LOGGER.debug("******************************************************************************");

        final AtomicInteger totalCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicInteger refreshedCount = new AtomicInteger(0);

        for (final AgentClassEntity agentClassEntity : agentClassRepository.findAll()) {
            final String agentClassName = agentClassEntity.getId();
            totalCount.incrementAndGet();

            // perform a transaction per-class so that an error in one class doesn't prevent refreshing others
            // and catch throwable inside the action otherwise the exception is thrown out of run and this thread dies
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        LOGGER.debug("Performing flow refresh for agent class '{}'", new Object[]{agentClassName});
                        refreshAgentClass(agentClassName, refreshedCount);
                    } catch (Throwable t) {
                        LOGGER.error("Error refreshing flow for '" + agentClassName + "'", t);
                        errorCount.incrementAndGet();
                        status.setRollbackOnly();
                    }
                }
            });
        }

        LOGGER.debug("******************************************************************************");
        LOGGER.debug("C2 flow refresh complete!");
        LOGGER.debug("{} agent classes processed, {} were refreshed, and {} errors occurred", new Object[]{totalCount, refreshedCount, errorCount});
        LOGGER.debug("******************************************************************************");
    }

    // visible for testing
    void refreshAgentClass(final String agentClassName, final AtomicInteger refreshedCount)
            throws FlowMapperException, IOException, FlowRetrievalException, SerializerException, FlowTransformException {

        // Determine the uri from NiFi Registry that maps to this class
        final Optional<FlowUri> optionalFlowUri = flowMapper.getFlowMapping(agentClassName);
        if (!optionalFlowUri.isPresent()) {
            LOGGER.debug("No flow mapping present for '{}'", new Object[] {agentClassName});
            return;
        }

        final FlowUri flowUri = optionalFlowUri.get();
        LOGGER.debug("Found flow {} mapped to '{}'", new Object[]{flowUri.getFlowId(), agentClassName});

        // Get the latest version available for the uri we got above
        final VersionedFlowSnapshot latestFlow = flowRetrievalService.getLatestFlow(flowUri);
        if (latestFlow == null) {
            LOGGER.debug("No flow versions exist for '{}'", new Object[] {agentClassName});
            return;
        }

        // Query the database to see if we anything currently mapped for the given agent class
        final Optional<FlowMappingEntity> optionalFlowMapping = flowMappingRepository.findById(agentClassName);
        if (optionalFlowMapping.isPresent()) {
            final FlowMappingEntity existingFlowMapping = optionalFlowMapping.get();

            final String existingRegistryUrl = existingFlowMapping.getFlowEntity().getRegistryUrl();
            final String existingBucketId = existingFlowMapping.getFlowEntity().getRegistryBucketId();
            final String existingFlowId = existingFlowMapping.getFlowEntity().getRegistryFlowId();

            if (flowUri.getRegistryUrl().equals(existingRegistryUrl)
                    && flowUri.getBucketId().equals(existingBucketId)
                    && flowUri.getFlowId().equals(existingFlowId)) {

                final FlowEntity existingFlow = existingFlowMapping.getFlowEntity();
                if (latestFlow.getSnapshotMetadata().getVersion() > existingFlow.getRegistryFlowVersion()) {
                    updateFlowMapping(existingFlowMapping, flowUri, latestFlow);
                    refreshedCount.incrementAndGet();
                } else {
                    LOGGER.debug("'{}' already mapped to latest version, nothing to do...", new Object[]{agentClassName});
                }

            } else {
                // Existing mapping doesn't match the flow uri we have so we have to update the flow mapping to point
                // to a new flow, this could happen if the C2 server is restarted to point at a different NiFi registry
                updateFlowMapping(existingFlowMapping, flowUri, latestFlow);
                refreshedCount.incrementAndGet();
            }

        } else {
            // No mapping present yet so we need to create a new one
            createNewFlowMapping(agentClassName, flowUri, latestFlow);
            refreshedCount.incrementAndGet();
        }
    }

    private void updateFlowMapping(final FlowMappingEntity existingFlowMapping, final FlowUri flowUri, final VersionedFlowSnapshot latestFlow)
            throws SerializerException, FlowTransformException, IOException {

        LOGGER.debug("Updating flow mapping for '{}' with flow uri '{}'", new Object[] {existingFlowMapping.getAgentClass(), flowUri});

        // save a new flow to the database
        final FlowEntity flowEntity = createNewFlow(flowUri, latestFlow);
        flowRepository.save(flowEntity);

        LOGGER.debug("Created new flow entity with id {}", new Object[] {flowEntity.getId()});

        // update the existing mapping to point to the new flow
        existingFlowMapping.setFlowEntity(flowEntity);
        flowMappingRepository.save(existingFlowMapping);
    }

    private void createNewFlowMapping(final String agentClassName, final FlowUri flowUri, final VersionedFlowSnapshot latestFlow)
            throws FlowTransformException, IOException, SerializerException {

        LOGGER.debug("Creating new flow mapping for '{}' with flow uri '{}'", new Object[] {agentClassName, flowUri});

        final FlowEntity flowEntity = createNewFlow(flowUri, latestFlow);
        flowRepository.save(flowEntity);

        LOGGER.debug("Created new flow entity with id {}", new Object[] {flowEntity.getId()});

        final FlowMappingEntity flowMappingEntity = new FlowMappingEntity();
        flowMappingEntity.setAgentClass(agentClassName);
        flowMappingEntity.setFlowEntity(flowEntity);
        flowMappingRepository.save(flowMappingEntity);
    }

    private FlowEntity createNewFlow(final FlowUri flowUri, final VersionedFlowSnapshot latestFlow)
            throws FlowTransformException, IOException, SerializerException {

        final ConfigSchema configSchema = flowTransformService.transform(latestFlow);

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
        flowEntity.setRegistryFlowVersion(latestFlow.getSnapshotMetadata().getVersion());
        flowEntity.setFlowContent(serializedConfigSchema);
        flowEntity.setFlowFormat(FlowFormat.YAML_V2_TYPE);
        return flowEntity;
    }

}
