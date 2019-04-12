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
package com.cloudera.cem.efm.coap;


import com.cloudera.cem.efm.coap.payload.OperationPayload;
import com.cloudera.cem.efm.coap.service.CoapProperties;
import com.cloudera.cem.efm.coap.service.CoapService;
import com.cloudera.cem.efm.model.C2HeartbeatResponse;
import com.cloudera.cem.efm.model.OperationType;
import com.cloudera.cem.efm.service.agent.AgentInfoException;
import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import com.cloudera.cem.efm.service.c2protocol.StandardC2ProtocolService;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Test the integration of these services. Sort of dog foods the payloads and expected responses.
 * Expectation is that protocol services being used are tested elsewhere, so we only need to concern
 * ourselves with integration.
 */
public class TestCoapIntegration {



    static AtomicInteger integer = new AtomicInteger(8787);

    public CoapService createService(final String hostname, C2ProtocolService c2protocolservice) throws ProtocolException {
        CoapService newService = null;
        do {
            if (c2protocolservice == null) {
                c2protocolservice = mock(StandardC2ProtocolService.class);
            }
            int port = integer.incrementAndGet();
            CoapProperties properties = new CoapProperties();
            properties.setHost(hostname);
            properties.setPort(port);
            newService = new CoapService(c2protocolservice, properties);
        }while(newService == null);
        return newService;
    }

    public CoapService createService(C2ProtocolService c2protocolservice) throws ProtocolException {
        return createService("localhost",c2protocolservice);
    }

    public CoapService createService() throws ProtocolException {
        return createService("localhost",null);
    }

    protected static class PacketBuilder {
        ByteArrayOutputStream bout;
        DataOutputStream stream;
        boolean hasFlowInfo = false;
        private PacketBuilder(short version, OperationType type) throws IOException, ProtocolException {
            bout = new ByteArrayOutputStream();
            stream = new DataOutputStream(bout);
            stream.writeShort(version);
            stream.writeByte(OperationPayload.parseOperationType(type));
        }

        public static PacketBuilder createBuilder(short version, OperationType type) throws IOException, ProtocolException {
            return new PacketBuilder(version,type);
        }

        private static void writeString(String str, DataOutputStream inputStream) throws IOException {
            int length = str.length();
            inputStream.writeShort((short) length);
            inputStream.write(str.getBytes(), 0, length);
        }

        public PacketBuilder addDeviceInfoIdentifier(String ident) throws IOException {
            writeString(ident,stream);
            return this;
        }

        public PacketBuilder addAgentInfoIdentifier(String ident) throws IOException {
            writeString(ident,stream);
            return this;
        }

        public PacketBuilder setNoFlowInfo() throws IOException {
            stream.writeByte(0);
            return this;
        }


        public byte [] build(){
            return bout.toByteArray();
        }
    }

    /**
     * Lack an operation type, so return 500
     */
    @Test
    public void testInvalidData() throws ProtocolException {
        try( CoapService service = createService() ) {
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            final byte [] heartbeat = { 0x00, 0x01 };
            CoapResponse response = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.INTERNAL_SERVER_ERROR == response.getCode());
        }
    }

    /**
     * Lack an operation type, so return 500
     */
    @Test
    public void testInvalidDataWrongPayloadType() throws ProtocolException {
        try( CoapService service = createService() ) {
            final byte [] heartbeat = { 0x00, 0x00, 0x00, 0x00, 0x00 };
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse response = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.INTERNAL_SERVER_ERROR == response.getCode());
        }
    }

    /**
     * Return an expected 500 since we don't have any data beyond the heartbeat
     */
    @Test
    public void testInvalidData2() throws ProtocolException {
        try( CoapService service = createService() ) {
            final byte [] heartbeat = { 0x00, 0x00, 0x01 };
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse response = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.INTERNAL_SERVER_ERROR == response.getCode());
        }
    }

       /**
     * Expect a 408 -- which in CoAP can mean that a
     */
    @Test
    public void testDataNoFailBut408() throws IOException {
        try( CoapService service = createService() ) {
            final byte [] heartbeat = PacketBuilder.createBuilder((short) 0,OperationType.HEARTBEAT).addDeviceInfoIdentifier("abcd").addAgentInfoIdentifier("abcd").setNoFlowInfo().build();
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse response = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE == response.getCode());
        } catch (ProtocolException e) {
            fail("received unexpected exception" + e);
        }
    }

    /**
     * Expect a 408 -- which in CoAP can mean that a
     */
    @Test
    public void testDataRegister() throws IOException {
        StandardC2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        when(c2protocolservice.processHeartbeat(anyObject(),any())).thenThrow(new AgentInfoException("register"));
        try( CoapService service = createService(c2protocolservice) ) {
            final byte [] heartbeat = PacketBuilder.createBuilder((short) 0,OperationType.HEARTBEAT).addDeviceInfoIdentifier("abcd").addAgentInfoIdentifier("abcd").setNoFlowInfo().build();
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse response = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.BAD_REQUEST == response.getCode());
            assertEquals("register",response.getResponseText());
        } catch (ProtocolException e) {
            fail("received unexpected exception" + e);
        }
    }

    /**
     * Expect a 205 with content
     */
    @Test
    public void testAck() throws IOException {
        StandardC2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        C2HeartbeatResponse response = new C2HeartbeatResponse();
        when(c2protocolservice.processHeartbeat(anyObject(),any())).thenReturn(response);
        try( CoapService service = createService(c2protocolservice) ) {
            final byte [] heartbeat = PacketBuilder.createBuilder((short) 0,OperationType.HEARTBEAT).addDeviceInfoIdentifier("abcd").addAgentInfoIdentifier("abcd").setNoFlowInfo().build();
            byte [] expectedResponse = { 0x00, 0x00, 0x00, 0x00};
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse coapReponse = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.CONTENT == coapReponse.getCode());
            byte [] responsePayload = coapReponse.getPayload();
            assertArrayEquals(expectedResponse,responsePayload);
        } catch (ProtocolException e) {
            fail("received unexpected exception" + e);
        }
    }

    /**
     * Expect a 205 with content since the CoAP server will bind to the local host.
     */
    @Test
    public void testInvalidHost() throws IOException {
        StandardC2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        C2HeartbeatResponse response = new C2HeartbeatResponse();
        when(c2protocolservice.processHeartbeat(anyObject(),any())).thenReturn(response);
        try( CoapService service = createService("google.com",c2protocolservice) ) {
            final byte [] heartbeat = PacketBuilder.createBuilder((short) 0,OperationType.HEARTBEAT).addDeviceInfoIdentifier("abcd").addAgentInfoIdentifier("abcd").setNoFlowInfo().build();
            byte [] expectedResponse = { 0x00, 0x00, 0x00, 0x00};
            CoapClient client = new CoapClient("coap://localhost:" + service.getPort() + "/heartbeat");
            CoapResponse coapReponse = client.post(heartbeat,0);
            assert(CoAP.ResponseCode.CONTENT == coapReponse.getCode());
            byte [] responsePayload = coapReponse.getPayload();
            assertArrayEquals(expectedResponse,responsePayload);
            fail("Invalid hostname should fail");
        } catch (ProtocolException e) {
            // expect this
        }
    }
}
