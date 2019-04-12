/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
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
package com.cloudera.cem.efm.service.c2protocol;

import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.metrics.HeartbeatMetrics;
import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentInfo;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.C2HeartbeatResponse;
import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.C2OperationAck;
import com.cloudera.cem.efm.model.Device;
import com.cloudera.cem.efm.model.DeviceInfo;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.FlowInfo;
import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.Operation;
import com.cloudera.cem.efm.model.OperationState;
import com.cloudera.cem.efm.model.OperationType;
import com.cloudera.cem.efm.service.BaseService;
import com.cloudera.cem.efm.service.agent.AgentService;
import com.cloudera.cem.efm.service.device.DeviceService;
import com.cloudera.cem.efm.service.event.EventService;
import com.cloudera.cem.efm.service.event.EventType;
import com.cloudera.cem.efm.service.flow.C2FlowService;
import com.cloudera.cem.efm.service.heartbeat.HeartbeatService;
import com.cloudera.cem.efm.service.operation.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Validator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StandardC2ProtocolService extends BaseService implements C2ProtocolService {

    private static final Logger logger = LoggerFactory.getLogger(StandardC2ProtocolService.class);

    private static final String HEARTBEAT_RESOURCE_TYPE = C2Heartbeat.class.getSimpleName();

    private AgentService agentService;
    private DeviceService deviceService;
    private OperationService operationService;
    private C2FlowService flowService;
    private EventService eventService;
    private HeartbeatService heartbeatService;
    private HeartbeatMetrics heartbeatMetrics;

    @Autowired
    public StandardC2ProtocolService(
            AgentService agentService,
            DeviceService deviceService,
            OperationService operationService,
            C2FlowService flowService,
            EventService eventService,
            HeartbeatService heartbeatService,
            HeartbeatMetrics heartbeatMetrics,
            Validator validator,
            OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.agentService = agentService;
        this.deviceService = deviceService;
        this.operationService = operationService;
        this.eventService = eventService;
        this.flowService = flowService;
        this.heartbeatService = heartbeatService;
        this.heartbeatMetrics = heartbeatMetrics;
    }

    @Override
    public C2HeartbeatResponse processHeartbeat(C2Heartbeat heartbeat, HeartbeatContext context) {

        if (heartbeat == null) {
            throw new IllegalArgumentException("Heartbeat cannot be null");
        }

        try {
            // ID is assigned here.
            heartbeat = heartbeatService.createHeartbeat(heartbeat);
        } catch (Exception e) {
            logger.warn("Encountered exception while trying to persist heartbeat", e);
        }

        logger.debug("Heartbeat persisted: {}", heartbeat.toString());

        if (heartbeatMetrics != null) {
            heartbeatMetrics.record(heartbeat, context);
        }

        publishHeartbeatEvent(heartbeat);

        processHeartbeatDeviceInfo(heartbeat);
        processHeartbeatAgentInfo(heartbeat);
        processHeartbeatForFlowUpdate(heartbeat, context);

        C2HeartbeatResponse response = new C2HeartbeatResponse();
        List<C2Operation> requestedOperations = new ArrayList<>(getQueuedC2Operations(heartbeat));

        if (!requestedOperations.isEmpty()) {
            response.setRequestedOperations(requestedOperations);
            for (C2Operation c2op : requestedOperations) {
                try {
                    operationService.updateOperationState(c2op.getIdentifier(), OperationState.DEPLOYED);
                } catch (Exception e) {
                    logger.warn("Encountered exception while updating operation state for operationId=" + c2op.getIdentifier(), e);
                }
            }
        }

        return response;
    }

    @Override
    public void processOperationAck(C2OperationAck operationAck) {

        try {
            // TODO, add operation status (eg success/failed) to operationAck. For now, assume ack indicates successful execution.
            operationService.updateOperationState(operationAck.getOperationId(), OperationState.DONE);
        } catch (Exception e) {
            logger.warn("Encountered exception while processing operation ack", e);
        }

    }

    private void publishHeartbeatEvent(C2Heartbeat heartbeat) {
        try {
            final Event.Builder builder = Event.builder()
                    .level(EventSeverity.DEBUG)
                    .eventType(EventType.HEARTBEAT_RECEIVED)
                    .message("Heartbeat received.")
                    .eventSource(Agent.class.getSimpleName(), heartbeat.getAgentId())
                    .eventDetail(HEARTBEAT_RESOURCE_TYPE, heartbeat.getIdentifier())
                    .agentClass(heartbeat.getAgentClass());
            eventService.createEvent(builder.build());
        } catch (Exception e) {
            logger.warn("Encountered exception while trying to create heartbeat event", e);
        }
    }

    private void processHeartbeatDeviceInfo(C2Heartbeat heartbeat) {
        try {
            final String deviceIdentifier;
            final DeviceInfo deviceInfo = heartbeat.getDeviceInfo();
            if (deviceInfo != null) {
                deviceIdentifier = deviceInfo.getIdentifier();

                if (deviceIdentifier == null) {
                    logger.info("Could not register device without identifier: {} ", deviceInfo);
                    return;
                }

                logger.debug("Creating/updating device info for deviceId={}", deviceIdentifier);
                Optional<Device> existingDevice = deviceService.getDevice(deviceIdentifier);
                boolean deviceExists = (existingDevice.isPresent());
                Device device = existingDevice.orElse(new Device());
                if (!deviceExists) {
                    device.setIdentifier(deviceIdentifier);
                    device.setFirstSeen(heartbeat.getCreated());
                }
                device.setLastSeen(heartbeat.getCreated());
                if (deviceInfo.getSystemInfo() != null) {
                    device.setSystemInfo(deviceInfo.getSystemInfo());
                }
                if (deviceInfo.getNetworkInfo() != null) {
                    device.setNetworkInfo(deviceInfo.getNetworkInfo());
                }

                if (!deviceExists) {
                    deviceService.createDevice(device);
                } else {
                    deviceService.updateDevice(device);
                }
            }
        } catch (Exception e) {
            logger.warn("Encountered exception while trying to update device info", e);
        }
    }

    private void processHeartbeatAgentInfo(C2Heartbeat heartbeat) {
        try {
            final String agentIdentifier;
            final long heartbeatTimestamp = getHeartbeatTimestamp(heartbeat);
            final AgentInfo agentInfo = heartbeat.getAgentInfo();
            if (agentInfo != null) {
                agentIdentifier = agentInfo.getIdentifier();

                if (agentIdentifier == null) {
                    logger.warn("Could not register agent without identifier: {} ", agentInfo);
                    return;
                }
                logger.debug("Creating/updating agent info for agentId={}", agentIdentifier);

                // Create agent manifest if this is the first time we've seen it
                String agentManifestIdentifier = null;
                if (agentInfo.getAgentManifest() != null) {
                    agentManifestIdentifier = agentInfo.getAgentManifest().getIdentifier();

                    // Note, a client-set agent manifest identifier is required so that we don't create infinite manifests
                    // Alternatively, we need some way of deterministically generating a manifest id from the manifest contents,
                    // So that two heartbeats with a matching agent manifest do not register separate manifests
                    if (agentManifestIdentifier != null) {
                        // check if this identifier has been seen, which is expected for the vast majority of interactions
                        if (!agentService.getAgentManifest(agentManifestIdentifier).isPresent()) {
                            // synchronization caters to the scenario where several instances of a new build report near simultaneously
                            // (e.g. a CM tool deploy and start) such that several concurrent requests may lead to a uniqueness constraint violation
                            synchronized (this) {
                                // ensure that the identifier is still unknown
                                if (!agentService.getAgentManifest(agentManifestIdentifier).isPresent()) {
                                    agentService.createAgentManifest(agentInfo.getAgentManifest());
                                    // Also note, C2 Agent Manifests are immutable once registered by design.
                                    // Changing the C2 Server's notion of an Agent Manifest request requires
                                    // generating a new ID, which is why we do not attempt manifest updates here.
                                }
                            }
                        }
                    }
                }

                // Create agent class if this is the first time we've seen it
                final String agentClassName = agentInfo.getAgentClass();
                if (agentClassName != null) {
                    Optional<AgentClass> optionalAgentClass = agentService.getAgentClass(agentClassName);
                    if (optionalAgentClass.isPresent()) {
                        if (agentManifestIdentifier != null) {
                            final AgentClass existingAgentClass = optionalAgentClass.get();
                            Set<String> agentClassManifests = existingAgentClass.getAgentManifests();
                            if (CollectionUtils.isEmpty(agentClassManifests)) {
                                if (agentClassManifests == null) {
                                    agentClassManifests = new HashSet<>();
                                }
                                agentClassManifests.add(agentManifestIdentifier);
                                agentService.updateAgentClass(existingAgentClass);
                            } else if (!agentClassManifests.contains(agentManifestIdentifier)) {
                                // TODO add support for one-to-many class-to-manifests
                                // Well, this is a bit of a problem at the moment. Eventually, we want to support one-to-many class-to-manifests,
                                // but that makes flow designing for a class difficult. So for now, the first manifest "assigned" to a class is the only
                                // one it gets. This agent reported in with a class that already has a different agent manifest (using manifestId), so
                                // log something to make this apparent to the user.
                                logger.warn("Received heartbeat for agent class '{}' that does not match expected agent manifest id. Expected '{}', but received '{}'. " +
                                                "The effect is that flow updates may not work as expected for this agent: {}",
                                        agentClassName, agentClassManifests.stream().findFirst().orElse(null), agentManifestIdentifier, agentInfo);
                            }
                        }
                    } else {
                        AgentClass newAgentClass = new AgentClass();
                        newAgentClass.setName(agentClassName);
                        newAgentClass.setAgentManifests(agentManifestIdentifier != null ? new HashSet<>(Collections.singleton(agentManifestIdentifier)) : null);
                        agentService.createAgentClass(newAgentClass);
                    }
                }

                // Create (aka register) or update the agent
                Optional<Agent> existingAgent = agentService.getAgent(agentIdentifier);
                boolean agentExists = existingAgent.isPresent();
                final Agent agent = existingAgent.orElse(new Agent());
                if (!agentExists) {
                    agent.setIdentifier(agentIdentifier);
                    agent.setFirstSeen(heartbeatTimestamp);
                }
                agent.setLastSeen(heartbeatTimestamp);
                if (agentClassName != null) {
                    agent.setAgentClass(agentInfo.getAgentClass());
                }
                if (agentManifestIdentifier != null) {
                    agent.setAgentManifestId(agentManifestIdentifier);
                }
                if (agentInfo.getStatus() != null) {
                    agent.setStatus(agentInfo.getStatus());
                }

                if (!agentExists) {
                    agentService.createAgent(agent);
                } else {
                    agentService.updateAgent(agent);
                }
            }
        } catch (Exception e) {
            logger.warn("Encountered exception while trying to update agent info", e);
        }
    }

    private void processHeartbeatForFlowUpdate(final C2Heartbeat heartbeat, final HeartbeatContext context) {
        final AgentInfo agentInfo = heartbeat.getAgentInfo();
        if (agentInfo == null) {
            return;
        }

        final String agentIdentifier = heartbeat.getAgentInfo().getIdentifier();

        Optional<Agent> existingAgent = agentService.getAgent(agentIdentifier);

        if (!existingAgent.isPresent()){
            logger.debug("Returning since {} did not provide us an existing agent",agentIdentifier);
            return;
        }

        final Optional<FlowMapping> flowMapping = flowService.getFlowMapping(existingAgent.get().getAgentClass());
        if (!flowMapping.isPresent()) {
            return; // no mapping for the given class so nothing to do
        }

        final FlowInfo flowInfo = heartbeat.getFlowInfo();
        final String agentFlowId = flowInfo == null ? null : flowInfo.getFlowId();
        final String flowMappingFlowId = flowMapping.get().getFlowId();

        if (!flowMappingFlowId.equals(agentFlowId)) {
            final Operation updateConfiguration = new Operation();
            updateConfiguration.setOperation(OperationType.UPDATE);
            updateConfiguration.setOperand("configuration");

            final String flowLocation = getFlowUri(context.getBaseUri(), flowMappingFlowId);
            logger.debug("### Responding to agent {} with new flow location at {}", new Object[] {agentIdentifier, flowLocation});

            final Map<String,String> args = new HashMap<>();
            args.put("location", flowLocation);
            args.put("persist", "true");
            updateConfiguration.setArgs(args);

            updateConfiguration.setState(OperationState.QUEUED);
            updateConfiguration.setTargetAgentId(agentIdentifier);

            operationService.createOperation(updateConfiguration);
        }

    }

    private long getHeartbeatTimestamp(final C2Heartbeat heartbeat) {

        Long heartbeatCreatedTimestamp = heartbeat.getCreated();
        if (heartbeatCreatedTimestamp != null && heartbeatCreatedTimestamp <= System.currentTimeMillis()) {
            return heartbeatCreatedTimestamp;
        } else {
            return System.currentTimeMillis();
        }

    }

    private String getFlowUri(final URI baseUri, final String flowId) {
        final String relativeFlowPath = "flows/" + flowId + "/content";
        return baseUri != null
                ? baseUri.toString() + relativeFlowPath
                : relativeFlowPath;
    }

    private List<C2Operation> getQueuedC2Operations(C2Heartbeat heartbeat) {
        if (heartbeat.getAgentInfo() != null) {
            final String agentId = heartbeat.getAgentInfo().getIdentifier();
            if (agentId != null) {
                return operationService.getC2OperationsListForAgent(agentId);
            }
        }

        return Collections.emptyList();
    }

}
