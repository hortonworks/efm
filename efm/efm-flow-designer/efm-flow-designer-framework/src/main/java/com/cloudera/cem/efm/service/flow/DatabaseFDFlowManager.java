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
package com.cloudera.cem.efm.service.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudera.cem.efm.db.entity.FDFlowEntity;
import com.cloudera.cem.efm.db.entity.FDFlowEventEntity;
import com.cloudera.cem.efm.db.repository.FDFlowEventRepository;
import com.cloudera.cem.efm.db.repository.FDFlowRepository;
import com.cloudera.cem.efm.exception.FlowManagerException;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowFormat;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flow Manager implementation that stores flows in a relational database.
 */
@Service
public class DatabaseFDFlowManager extends BaseService implements FDFlowManager {

    /**
     * The name of the root process group when creating a new flow.
     */
    static final String ROOT_PROCESS_GROUP_NAME = "root";

    /**
     * The string to use for the NiFi Registry URL, bucket id, and flow id, when the local flow has not been published yet.
     */
    static final String UNPUBLISHED = "UNPUBLISHED";

    /**
     * The current flow format using for serializing flow contents of new flows and new events.
     */
    public static final FDFlowFormat CURRENT_FLOW_FORMAT = FDFlowFormat.JACKSON_JSON_V1;

    private final FDFlowRepository flowRepository;
    private final FDFlowEventRepository flowEventRepository;
    private final ObjectMapper objectMapper;

    private Map<String,FDFlow> flowsById = new ConcurrentHashMap<>();

    @Autowired
    public DatabaseFDFlowManager(final FDFlowRepository flowRepository,
                                 final FDFlowEventRepository flowEventRepository,
                                 final OptionalModelMapper modelMapper,
                                 final ObjectMapper objectMapper,
                                 final Validator validator) {
        super(validator, modelMapper);
        this.flowRepository = flowRepository;
        this.flowEventRepository = flowEventRepository;
        this.objectMapper = objectMapper;
    }

    // ----------------------- Flow Methods ----------------------- //

    @Override
    public Optional<FDFlowMetadata> getFlowMetadata(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            return Optional.empty();
        }

        final FDFlowMetadata flowMetadata = modelMapper.map(flowEntity.get(), FDFlowMetadata.class);
        return Optional.of(flowMetadata);
    }

    @Override
    public Optional<FDFlow> getFlow(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        // attempt to retrieve from cache
        FDFlow flow = flowsById.get(flowId);

        // if not cached then load fresh and put in cache
        if (flow == null) {
            final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
            if (!flowEntity.isPresent()) {
                return Optional.empty();
            }

            final Optional<FDFlowEventEntity> latestFlowEvent = flowEventRepository.findFirstByFlowOrderByFlowRevisionDesc(flowEntity.get());
            if (!latestFlowEvent.isPresent()) {
                throw new IllegalStateException("Could not find any flow content for the given flow");
            }

            final FDFlowMetadata flowMetadata = modelMapper.map(flowEntity.get(), FDFlowMetadata.class);
            final VersionedProcessGroup flowContent = deserialize(latestFlowEvent.get().getFlowContent());
            final FDVersionInfo versionInfo = createVersionInfo(latestFlowEvent.get());
            final BigInteger localFlowRevision = latestFlowEvent.get().getFlowRevision();

            flow = new FDFlow();
            flow.setFlowMetadata(flowMetadata);
            flow.setFlowContent(flowContent);
            flow.setVersionInfo(versionInfo);
            flow.setLocalFlowRevision(localFlowRevision);

            flowsById.put(flowId, flow);
        }

        return Optional.of(flow);
    }

    @Override
    public FDFlowMetadata createFlow(final String agentClass, final NiFiUser user) {
        if (StringUtils.isBlank(agentClass)) {
            throw new IllegalArgumentException("Agent class cannot be null or blank");
        }

        if (user == null || StringUtils.isBlank(user.getIdentity())) {
            throw new IllegalArgumentException("A user identity must be provided");
        }

        // Create the initial root process group for the flow
        final VersionedProcessGroup flowContent = new VersionedProcessGroup();
        flowContent.setIdentifier(UUID.randomUUID().toString());
        flowContent.setName(ROOT_PROCESS_GROUP_NAME);

        // Save the flow entity to the db
        final FDFlowEntity flowEntity = new FDFlowEntity();
        flowEntity.setId(UUID.randomUUID().toString());
        flowEntity.setAgentClass(agentClass);
        flowEntity.setRootProcessGroupId(flowContent.getIdentifier());

        final FDFlowEntity savedFlowEntity = flowRepository.save(flowEntity);

        // Create the flow event for the initial flow
        final FDFlowEventEntity flowEventEntity = new FDFlowEventEntity();
        flowEventEntity.setId(UUID.randomUUID().toString());
        flowEventEntity.setFlow(savedFlowEntity);
        flowEventEntity.setEventType(FDFlowEventType.COMPONENT_ADDED);
        flowEventEntity.setEventDescription("Created root process group");
        flowEventEntity.setComponentId(flowContent.getIdentifier());
        flowEventEntity.setFlowRevision(BigInteger.ZERO);
        flowEventEntity.setUserIdentity(user.getIdentity());

        // Set the initial NiFi Registry info
        flowEventEntity.setRegistryUrl(UNPUBLISHED);
        flowEventEntity.setRegistryBucketId(UNPUBLISHED);
        flowEventEntity.setRegistryFlowId(UNPUBLISHED);
        flowEventEntity.setRegistryVersion(0);
        flowEventEntity.setLastPublished(null);
        flowEventEntity.setLastPublishedUserIdentity(null);

        // Serialize the flow content and set it in the flow event
        final String serializedFlowContent = serializeFlowContent(flowContent);
        flowEventEntity.setFlowContent(serializedFlowContent);
        flowEventEntity.setFlowFormat(CURRENT_FLOW_FORMAT);

        // Save the initial flow event to the db
        flowEventRepository.save(flowEventEntity);

        return modelMapper.map(savedFlowEntity, FDFlowMetadata.class);
    }

    @Override
    public List<FDFlowMetadata> getAvailableFlows() {
        final List<FDFlowMetadata> flowMetadataList = new ArrayList<>();

        for (final FDFlowEntity flowEntity : flowRepository.findAll()) {
            final FDFlowMetadata flowMetadata = modelMapper.map(flowEntity, FDFlowMetadata.class);
            flowMetadataList.add(flowMetadata);
        }

        return flowMetadataList;
    }

    @Override
    public List<FDFlowSummary> getFlowSummaries() {
        final List<FDFlowSummary> flowSummaries = new ArrayList<>();

        for (final FDFlowEntity flowEntity : flowRepository.findAll()) {
            final String flowId = flowEntity.getId();

            final FDFlowSummary flowSummary = new FDFlowSummary();
            flowSummary.setIdentifier(flowId);
            flowSummary.setAgentClass(flowEntity.getAgentClass());
            flowSummary.setRootProcessGroupIdentifier(flowEntity.getRootProcessGroupId());
            flowSummary.setCreated(flowEntity.getCreated().getTime());

            final Optional<FDFlowEventEntity> latestFlowEventResult = getLatestFlowEventEntity(flowId);
            if (!latestFlowEventResult.isPresent()) {
                throw new ResourceNotFoundException("No flow events found for flow id: " + flowId);
            }

            final FDFlowEventEntity latestFlowEvent = latestFlowEventResult.get();
            flowSummary.setLastModified(latestFlowEvent.getCreated().getTime());
            flowSummary.setLastModifiedBy(latestFlowEvent.getUserIdentity());

            // Will be null if never published...
            final FDVersionInfo versionInfo = createVersionInfo(latestFlowEvent);
            flowSummary.setVersionInfo(versionInfo);

            flowSummaries.add(flowSummary);
        }

        return flowSummaries;
    }

    @Override
    public FDFlowMetadata deleteFlow(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        // delete from DB
        flowRepository.delete(flowEntity.get());

        // delete from cache
        if (flowsById.containsKey(flowId)) {
            flowsById.remove(flowId);
        }

        return modelMapper.map(flowEntity.get(), FDFlowMetadata.class);
    }

    // ----------------------- Flow Event Methods ----------------------- //

    @Override
    public List<FDFlowEvent> getFlowEvents(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        final List<FDFlowEvent> events = new ArrayList<>();
        flowEventRepository.findByFlowOrderByFlowRevisionAsc(flowEntity.get()).forEach(e -> {
            final FDFlowEvent flowEvent = modelMapper.map(e, FDFlowEvent.class);
            events.add(flowEvent);
        });
        return events;
    }

    @Override
    public Optional<FDFlowEvent> getLatestFlowEvent(final String flowId) {
        final Optional<FDFlowEventEntity> latestFlowEvent = getLatestFlowEventEntity(flowId);
        if (latestFlowEvent.isPresent()) {
            final FDFlowEvent flowEvent = modelMapper.map(latestFlowEvent.get(), FDFlowEvent.class);
            return Optional.of(flowEvent);
        } else {
            // Should always be at least one event per flow, so this really shouldn't happen
            return Optional.empty();
        }
    }

    private Optional<FDFlowEventEntity> getLatestFlowEventEntity(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        return flowEventRepository.findFirstByFlowOrderByFlowRevisionDesc(flowEntity.get());
    }

    @Override
    public Optional<FDFlowEvent> getLatestPublishFlowEvent(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        final Optional<FDFlowEventEntity> latestPublishEvent = flowEventRepository.findFirstByFlowAndEventTypeOrderByFlowRevisionDesc(
                flowEntity.get(), FDFlowEventType.FLOW_PUBLISHED);

        if (latestPublishEvent.isPresent()) {
            final FDFlowEvent flowEvent = modelMapper.map(latestPublishEvent.get(), FDFlowEvent.class);
            return Optional.of(flowEvent);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FDFlowEvent> getFlowEvent(final String flowEventId) {
        if (StringUtils.isBlank(flowEventId)) {
            throw new IllegalArgumentException("Flow event id cannot be null or blank");
        }

        final Optional<FDFlowEventEntity> flowEventEntity = flowEventRepository.findById(flowEventId);
        if (flowEventEntity.isPresent()) {
            final FDFlowEvent flowEvent = modelMapper.map(flowEventEntity.get(), FDFlowEvent.class);
            return Optional.of(flowEvent);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FDFlowEvent addFlowEvent(final FDFlowEvent flowEvent, final VersionedProcessGroup flowContent, final NiFiUser user) {
        if (flowEvent == null) {
            throw new IllegalArgumentException("Flow event cannot be null or blank");
        }

        if (StringUtils.isBlank(flowEvent.getFlowIdentifier())) {
            throw new IllegalArgumentException("Flow identifier of flow event cannot be null or blank");
        }

        if (flowContent == null) {
            throw new IllegalArgumentException("Flow content cannot be null or blank");
        }

        if (StringUtils.isBlank(flowContent.getIdentifier()) || StringUtils.isBlank(flowContent.getName())) {
            throw new IllegalArgumentException("Flow content must contain a root process group with an identifier and name");
        }

        if (user == null || StringUtils.isBlank(user.getIdentity())) {
            throw new IllegalArgumentException("A user identity must be provided");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowEvent.getFlowIdentifier());
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        final String previousRootGroupId = flowEntity.get().getRootProcessGroupId();
        final String incomingRootGroupId = flowContent.getIdentifier();
        if (!previousRootGroupId.equals(incomingRootGroupId)) {
            throw new IllegalArgumentException("Cannot change the id of the root process group");
        }

        final Optional<FDFlowEventEntity> latestFlowEvent = flowEventRepository.findFirstByFlowOrderByFlowRevisionDesc(flowEntity.get());
        if (!latestFlowEvent.isPresent()) {
            throw new IllegalStateException("Could not find any flow content for the given flow");
        }

        final BigInteger currRevision = latestFlowEvent.get().getFlowRevision();
        final BigInteger nextRevision = currRevision.add(BigInteger.ONE);

        flowEvent.setIdentifier(UUID.randomUUID().toString());
        flowEvent.setCreated(System.currentTimeMillis());
        flowEvent.setUpdated(System.currentTimeMillis());
        flowEvent.setFlowRevision(nextRevision);
        flowEvent.setFlowFormat(CURRENT_FLOW_FORMAT);
        flowEvent.setUserIdentity(user.getIdentity());

        // if not a publish event then carry forward the registry info from the previous event, otherwise use whatever was specified
        if (flowEvent.getEventType() != FDFlowEventType.FLOW_PUBLISHED) {
            flowEvent.setRegistryUrl(latestFlowEvent.get().getRegistryUrl());
            flowEvent.setRegistryBucketId(latestFlowEvent.get().getRegistryBucketId());
            flowEvent.setRegistryFlowId(latestFlowEvent.get().getRegistryFlowId());
            flowEvent.setRegistryVersion(latestFlowEvent.get().getRegistryVersion());

            final Date lastPublished = latestFlowEvent.get().getLastPublished();
            flowEvent.setLastPublished(lastPublished == null ? null : lastPublished.getTime());
            flowEvent.setLastPublishedUserIdentity(latestFlowEvent.get().getLastPublishedUserIdentity());
        }

        if (flowEvent.getEventType() == FDFlowEventType.FLOW_PUBLISHED && flowEvent.getLastPublished() == null) {
            throw new IllegalArgumentException("Publish event must contain a published date");
        }

        validate(flowEvent, "Cannot add flow event due to invalid event");

        final FDFlowEventEntity flowEventEntity = modelMapper.map(flowEvent, FDFlowEventEntity.class);
        flowEventEntity.setFlowContent(serializeFlowContent(flowContent));
        flowEventEntity.setFlow(flowEntity.get());

        final FDFlowEventEntity savedFlowEventEntity = flowEventRepository.save(flowEventEntity);

        // Update the cached flow with the latest version info
        final String flowId = flowEntity.get().getId();
        if (flowsById.containsKey(flowId)) {
            final FDVersionInfo versionInfo = createVersionInfo(savedFlowEventEntity);
            final FDFlow cachedFlow = flowsById.get(flowId);
            cachedFlow.setVersionInfo(versionInfo);
            cachedFlow.setLocalFlowRevision(savedFlowEventEntity.getFlowRevision());
        }

        return modelMapper.map(savedFlowEventEntity, FDFlowEvent.class);
    }

    @Override
    public FDFlowEvent deleteFlowEvent(final String flowEventId) {
        if (StringUtils.isBlank(flowEventId)) {
            throw new IllegalArgumentException("Flow event id cannot be null or blank");
        }

        final Optional<FDFlowEventEntity> flowEventEntity = flowEventRepository.findById(flowEventId);
        if (!flowEventEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow event exists for the given id");
        }

        if (flowEventEntity.get().getEventType() == FDFlowEventType.FLOW_PUBLISHED) {
            throw new IllegalArgumentException("Cannot delete event because deleting events of type "
                    + FDFlowEventType.FLOW_PUBLISHED + " is not allowed");
        }

        final FDFlowEntity flowEntity = flowEventEntity.get().getFlow();

        final Long numEventsForFlow = flowEventRepository.countByFlow(flowEntity);
        if (numEventsForFlow <= 1) {
            throw new IllegalStateException("Cannot delete event because there is only one event for the given flow");
        }

        flowEventRepository.delete(flowEventEntity.get());

        // we don't know if the event being deleted is the latest event, and if it is then the cached version of
        // the flow is no longer accurate because it was based on the deleted event
        if (flowsById.containsKey(flowEntity.getId())) {
            flowsById.remove(flowEntity.getId());
        }

        return modelMapper.map(flowEventEntity.get(), FDFlowEvent.class);
    }

    @Override
    public void retainPublishEvents(final String flowId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        final Optional<FDFlowEventEntity> latestPublishEvent = flowEventRepository.findFirstByFlowAndEventTypeOrderByFlowRevisionDesc(
                flowEntity.get(), FDFlowEventType.FLOW_PUBLISHED);
        if (!latestPublishEvent.isPresent()) {
            throw new IllegalArgumentException("Must have at least one publish event to retain");
        }

        // On a publish we remove the local history except for the publish events
        flowEventRepository.deleteByFlowAndEventTypeNot(flowEntity.get(), FDFlowEventType.FLOW_PUBLISHED);
    }

    @Override
    public void revertToFlowRevision(final String flowId, final BigInteger flowRevision) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (flowRevision == null) {
            throw new IllegalArgumentException("Flow revision cannot be null");
        }

        final Optional<FDFlowEntity> flowEntity = flowRepository.findById(flowId);
        if (!flowEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the provided flow id");
        }

        final Optional<FDFlowEventEntity> flowEventEntity = flowEventRepository.findByFlowAndFlowRevision(flowEntity.get(), flowRevision);
        if (!flowEventEntity.isPresent()) {
            throw new ResourceNotFoundException("No flow event exists with given event id for the given flow");
        }

        flowEventRepository.deleteByFlowAndFlowRevisionGreaterThan(flowEntity.get(), flowEventEntity.get().getFlowRevision());

        if (flowsById.containsKey(flowEntity.get().getId())) {
            flowsById.remove(flowEntity.get().getId());
        }
    }

    private String serializeFlowContent(final VersionedProcessGroup flowContent) {
        try {
            return objectMapper.writeValueAsString(flowContent);
        } catch (JsonProcessingException e) {
            throw new FlowManagerException("Unable to serialize flow content", e);
        }
    }

    private VersionedProcessGroup deserialize(String flowContent) {
        try {
            return objectMapper.readValue(flowContent, VersionedProcessGroup.class);
        } catch (IOException e) {
            throw new FlowManagerException("Unable to deserialize flow content", e);
        }
    }

    private FDVersionInfo createVersionInfo(final FDFlowEventEntity flowEvent) {
        // Only create version info if the flow has been published at least once
        final Date lastPublished = flowEvent.getLastPublished();
        if (lastPublished == null) {
            return null;
        } else {
            final FDVersionInfo versionInfo = new FDVersionInfo();
            versionInfo.setRegistryUrl(flowEvent.getRegistryUrl());
            versionInfo.setRegistryBucketId(flowEvent.getRegistryBucketId());
            versionInfo.setRegistryFlowId(flowEvent.getRegistryFlowId());
            versionInfo.setRegistryVersion(flowEvent.getRegistryVersion());
            versionInfo.setDirty(flowEvent.getEventType() != FDFlowEventType.FLOW_PUBLISHED);
            versionInfo.setLastPublished(lastPublished.getTime());
            versionInfo.setLastPublishedBy(flowEvent.getLastPublishedUserIdentity());
            return versionInfo;
        }
    }

}
