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
package com.cloudera.cem.efm.service.c2protocol

import com.cloudera.cem.efm.model.*
import com.cloudera.cem.efm.service.SpecUtil
import com.cloudera.cem.efm.service.agent.AgentService
import com.cloudera.cem.efm.service.device.DeviceService
import com.cloudera.cem.efm.service.event.EventService
import com.cloudera.cem.efm.service.flow.C2FlowService
import com.cloudera.cem.efm.service.heartbeat.HeartbeatService
import com.cloudera.cem.efm.service.operation.OperationService
import spock.lang.Specification

class StandardC2ProtocolServiceSpec extends Specification {

    def agentService = Mock(AgentService)
    def deviceService = Mock(DeviceService)
    def operationService = Mock(OperationService)
    def flowService = Mock(C2FlowService)
    def eventService = Mock(EventService)
    def heartbeatService = Mock(HeartbeatService)
    C2ProtocolService c2ProtocolService

    def setup() {

        def modelMapper = SpecUtil.buildOptionalModelMapper()

        heartbeatService.createHeartbeat(_ as C2Heartbeat) >> { C2Heartbeat h ->
            h.setIdentifier(UUID.randomUUID().toString())
            if (h.getCreated() == null) {
                h.setCreated(System.currentTimeMillis())
            }
            return h
        }

        c2ProtocolService = new StandardC2ProtocolService(
                agentService,
                deviceService,
                operationService,
                flowService,
                eventService,
                heartbeatService,
                null,
                null,
                modelMapper)
    }

    def "process heartbeat"() {

        setup:
        C2Heartbeat heartbeat1 = createTestHeartbeat("agent1", "agentClass1")
        operationService.getC2OperationsListForAgent("agent1") >> Collections.emptyList()

        C2Heartbeat heartbeat1Manifest2 = createTestHeartbeat("agent1", "agentClass1", "new-agent-manifest-id")
        operationService.getC2OperationsListForAgent("agent1") >> Collections.emptyList()

        C2Heartbeat heartbeat2 = createTestHeartbeat("agent2", "agentClass2")
        operationService.getC2OperationsListForAgent("agent2") >> Collections.singletonList(createOperation("agent2", OperationState.QUEUED))

        agentService.getAgent(_ as String) >> Optional.empty()
        agentService.getAgentManifest(_ as String) >> Optional.empty()
        agentService.getAgentClass(_ as String) >> Optional.empty()
        deviceService.getDevice(_ as String) >> Optional.empty()
        flowService.getFlowMapping(_ as String) >> Optional.empty()


        when: "heartbeat is processed while no operations are queued"
        def hbResponse1 = c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "empty heartbeat response is generated"
        with(hbResponse1) {
            requestedOperations == null
        }


        when: "heartbeat is processed while operations are queued"
        def hbResponse2 = c2ProtocolService.processHeartbeat(heartbeat2, HeartbeatContext.empty())

        then: "heartbeat response is generated with requested operations"
        with(hbResponse2) {
            requestedOperations.size() == 1
            requestedOperations.get(0).operation == OperationType.DESCRIBE
        }


        when: "heartbeat contains new agent manifest"
        agentService.getAgentManifest(_ as String) >> Optional.empty()
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the agent manifest is registered"
        1 * agentService.createAgentManifest(_ as AgentManifest)


        when: "heartbeat contains existing agent manifest"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the agent manifest is not registered"
        1 * agentService.getAgentManifest(_ as String) >> { id -> Optional.of(new AgentManifest([identifier: id])) }
        0 * agentService.createAgentManifest(*_)


        when: "heartbeat contains new agent class"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the agent class is registered"
        1 * agentService.getAgentClass(_ as String) >> Optional.empty()
        1 * agentService.createAgentClass(_ as AgentClass)


        when: "heartbeat contains existing agent class, with existing agent manifest"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the existing agent class is not updated"
        1 * agentService.getAgentClass(_ as String) >> {name -> Optional.of(new AgentClass([name: name, agentManifests: ["test-agent-manifest-id"]])) }
        0 * agentService.createAgentClass(_ as AgentClass)
        0 * agentService.updateAgentClass(_ as AgentClass)


        when: "heartbeat contains existing agent class, with new agent manifest"
        c2ProtocolService.processHeartbeat(heartbeat1Manifest2, HeartbeatContext.empty())

        then: "the existing agent class is not updated"
        1 * agentService.getAgentClass(_ as String) >> {name -> Optional.of(new AgentClass([name: name, agentManifests: ["test-agent-manifest-id"]])) }
        0 * agentService.createAgentClass(_ as AgentClass)
        0 * agentService.updateAgentClass(_ as AgentClass)


        when: "heartbeat contains existing agent class with no manifests and agent manifest"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the existing agent class is updated"
        1 * agentService.getAgentClass(_ as String) >> {name -> Optional.of(new AgentClass([name: name, agentManifests: []])) }
        0 * agentService.createAgentClass(_ as AgentClass)
        1 * agentService.updateAgentClass(_ as AgentClass)


        when: "heartbeat contains new agent manifest"
        agentService.getAgentManifest(_ as String) >> Optional.empty()
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the agent manifest is registered"
        1 * agentService.createAgentManifest(_ as AgentManifest)


        when: "heartbeat contains new device"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the device is registered"
        1 * deviceService.getDevice(_ as String) >> Optional.empty()
        1 * deviceService.createDevice(_ as Device)


        when: "heartbeat contains existing device"
        c2ProtocolService.processHeartbeat(heartbeat1, HeartbeatContext.empty())

        then: "the device is not registered"
        1 * deviceService.getDevice(_ as String) >> { id -> Optional.of(new Device([identifier: id])) }
        0 * deviceService.createDevice(*_)

    }

    def "process ack"() {

        setup:
        C2OperationAck ack = new C2OperationAck([operationId: "operation1"])

        when: "operationAck is processed"
        c2ProtocolService.processOperationAck(ack)

        then: "operation state is updated"
        1 * operationService.updateOperationState("operation1", OperationState.DONE)

    }


    // --- Helper methods

    def createTestHeartbeat(String agentId, String agentClass) {
        return createTestHeartbeat(agentId, agentClass, "test-agent-manifest-id")
    }

    def createTestHeartbeat(String agentId, String agentClass, String agentManifestId) {
        return new C2Heartbeat([
                identifier: "test-heartbeat-id",
                created: 1514764800000L,
                deviceInfo: new DeviceInfo([
                        identifier: "test-device-id",
                ]),
                agentInfo: new AgentInfo([
                        identifier: agentId,
                        agentClass: agentClass,
                        agentManifest: new AgentManifest([
                                identifier: agentManifestId
                        ])
                ]),
                flowInfo: new FlowInfo([
                        flowId: "test-flow-id"
                ])
        ])
    }

    def createOperation(String targetAgentId, OperationState state) {
        return new Operation([
                identifier: "test-operation-id",
                operation: OperationType.DESCRIBE,
                targetAgentId: targetAgentId,
                state: state
        ])
    }

}
