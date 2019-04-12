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
package com.cloudera.cem.efm.coap.payload;

import com.cloudera.cem.efm.model.AgentInfo;
import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.ComponentStatus;
import com.cloudera.cem.efm.model.DeviceInfo;
import com.cloudera.cem.efm.model.FlowInfo;
import com.cloudera.cem.efm.model.FlowQueueStatus;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.model.OperationType;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Purpose and Design: Heart beat payloads are receiving payloads that return a
 * C2 heartbeat as the raw payload. This enables us to be the transformation point
 * from CoAP to C2Heartbeat POJOs ( but they're swaggerfied!).
 */
public class HeartbeatPayload extends ReceivingPayload<C2Heartbeat> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatPayload.class);

    protected C2Heartbeat heartbeat = null;
    public HeartbeatPayload(CoapExchange packet) throws ProtocolException {
        super(packet);
        if (payloadType != OperationType.HEARTBEAT){
            throw new ProtocolException("Expected a heartbeat.");
        }
    }


    @Override
    public C2Heartbeat getRawPayload() throws IOException {
        synchronized (this) {
            if (null == heartbeat) {
                parseHeartbeat();
            }
            return heartbeat;
        }
    }

    /**
     * Parses heartbeat based on the known version
     * @throws IOException if the payload cannot be read
     */
    protected synchronized void parseHeartbeat() throws IOException {
        heartbeat = new C2Heartbeat();
        // we have parsed the version and operation type.
        // so let's begin parsing the heartbeat itself.
        if (0 == version) {
            heartbeat.setIdentifier(UUID.randomUUID().toString());
            parseDeviceInfo(heartbeat);
            parseAgentInfo(heartbeat);
            final boolean hasFlowInfo = inputStream.readBoolean();
            if (hasFlowInfo) {
                parseFlowInfo(heartbeat);
            }
        }
    }

    /**
     * Parses the device info entities.
     * @param heartbeat heartbeat reference.
     * @throws IOException if the payload cannot be read
     */
    private synchronized void parseDeviceInfo(final C2Heartbeat heartbeat) throws IOException {
        final DeviceInfo deviceInfo = new DeviceInfo();
        final String deviceIdentifier = parseString();
        deviceInfo.setIdentifier(deviceIdentifier);
        heartbeat.setDeviceInfo(deviceInfo);
    }

    /**
     * Parses the agent info entities.
     * @param heartbeat heartbeat reference.
     * @throws IOException if the payload cannot be read
     */
    private synchronized void parseAgentInfo(final C2Heartbeat heartbeat) throws IOException {
        final AgentInfo agentInfo = new AgentInfo();
        final String agentIdentifier = parseString();
        agentInfo.setIdentifier(agentIdentifier);
        heartbeat.setAgentInfo(agentInfo);
    }

    private synchronized void parseFlowInfo(final C2Heartbeat heartbeat) throws IOException {
        final FlowInfo flowInfo = new FlowInfo();
        final FlowUri flowUri = new FlowUri();
        /**
         * Getters aren't immutable in status, but we should not make it a practice
         * of not using setters
         */
        Map<String, ComponentStatus> components = new HashMap<>();
        Map<String, FlowQueueStatus> queues = new HashMap<>();

        int size = inputStream.readUnsignedShort();
        /**
         * Can't be run in parallel so IntStream forEach has no major
         * performance/readability benefit, plus exception management is simpler.
         */
        for(int i = 0; i < size; i++){
            final String label = parseString();
            final ComponentStatus compStatus = new ComponentStatus();
            compStatus.setRunning(inputStream.readBoolean());
            components.put(label,compStatus);
        }
        // read queues

        size = inputStream.readUnsignedShort();
        /**
         * Again, IntStream forEach seems to degrade readability here and
         * forces an unchecked exception, and these need to be read in order so
         * no benefit to parallelizing it.
         */
        for(int i = 0; i < size; i++){
            final FlowQueueStatus queueStatus = new FlowQueueStatus();
            final String label = parseString();
            queueStatus.setDataSize( inputStream.readLong() );
            queueStatus.setDataSizeMax( inputStream.readLong() );
            queueStatus.setSize( inputStream.readLong() );
            queueStatus.setSizeMax( inputStream.readLong() );
            queues.put(label,queueStatus);
        }
        flowInfo.setQueues(queues);
        flowInfo.setComponents(components);
        // read flow URI information
        flowUri.setBucketId(parseString());
        flowUri.setFlowId(parseString());
        flowInfo.setVersionedFlowSnapshotURI(flowUri);
        flowInfo.setFlowId( flowUri.getFlowId());

        heartbeat.setFlowInfo(flowInfo);
    }

}

