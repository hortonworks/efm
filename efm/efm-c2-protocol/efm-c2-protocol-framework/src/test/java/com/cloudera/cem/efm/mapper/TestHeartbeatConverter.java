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

import com.cloudera.cem.efm.db.entity.HeartbeatEntity;
import com.cloudera.cem.efm.model.AgentInfo;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.DeviceInfo;
import com.cloudera.cem.efm.model.FlowInfo;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestHeartbeatConverter extends ModelMapperTest {

    @Test
    public void testConvertHeartbeatToEntity() throws Exception{
        // Arrange;
        C2Heartbeat heartbeat = new C2Heartbeat();
        heartbeat.setIdentifier("heartbeat-1");

        AgentInfo agentInfo = new AgentInfo();
        agentInfo.setIdentifier("agent-1");
        heartbeat.setAgentInfo(agentInfo);

        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setIdentifier("device-1");
        heartbeat.setDeviceInfo(deviceInfo);

        FlowInfo flowInfo = new FlowInfo();
        flowInfo.setFlowId("flow-1");
        heartbeat.setFlowInfo(flowInfo);

        String expectedHeartbeatEntityContent = "{\n" +
                "  \"agentInfo\": { \"identifier\": \"agent-1\" },\n" +
                "  \"deviceInfo\": { \"identifier\": \"device-1\" },\n" +
                "  \"flowInfo\": { \"flowId\": \"flow-1\" }\n" +
                "}";


        // Act
        HeartbeatEntity heartbeatEntity = modelMapper.map(heartbeat, HeartbeatEntity.class);

        // Assert
        assertNotNull(heartbeatEntity);
        assertEquals(heartbeat.getIdentifier(), heartbeatEntity.getId());
        assertEquals(heartbeat.getDeviceInfo().getIdentifier(), heartbeatEntity.getDeviceId());
        assertEquals(heartbeat.getAgentInfo().getIdentifier(), heartbeatEntity.getAgentId());
        JSONAssert.assertEquals(expectedHeartbeatEntityContent, heartbeatEntity.getContent(), false);

    }

    @Test
    public void testConvertEntityToHeartbeat() throws Exception{
        // Arrange;
        String heartbeatEntityContent = "{\n" +
                "  \"agentInfo\": { \"identifier\": \"agent-1\" },\n" +
                "  \"deviceInfo\": { \"identifier\": \"device-1\" },\n" +
                "  \"flowInfo\": { \"flowId\": \"flow-1\" }\n" +
                "}";

        HeartbeatEntity heartbeatEntity = new HeartbeatEntity();
        heartbeatEntity.setId("heartbeat-1");
        heartbeatEntity.setAgentId("agent-1");
        heartbeatEntity.setDeviceId("device-1");
        heartbeatEntity.setContent(heartbeatEntityContent);
        heartbeatEntity.prePersist();  // set created field

        // Act
        C2Heartbeat heartbeat = modelMapper.map(heartbeatEntity, C2Heartbeat.class);

        // Assert
        assertNotNull(heartbeat);
        assertEquals(heartbeatEntity.getId(), heartbeat.getIdentifier());
        assertEquals(heartbeatEntity.getCreated(), new Date(heartbeat.getCreated()));
        assertNotNull(heartbeat.getAgentInfo());
        assertEquals(heartbeatEntity.getAgentId(), heartbeat.getAgentInfo().getIdentifier());
        assertNotNull(heartbeat.getDeviceInfo());
        assertEquals(heartbeatEntity.getDeviceId(), heartbeat.getDeviceInfo().getIdentifier());
        assertNotNull(heartbeat.getFlowInfo());
        assertEquals("flow-1", heartbeat.getFlowInfo().getFlowId());

    }

}
