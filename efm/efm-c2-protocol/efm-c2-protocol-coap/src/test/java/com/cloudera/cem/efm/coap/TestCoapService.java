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


import com.cloudera.cem.efm.coap.payload.AcknowledgementPayload;
import com.cloudera.cem.efm.coap.payload.HeartbeatPayload;
import com.cloudera.cem.efm.coap.service.CoapProperties;
import com.cloudera.cem.efm.coap.service.CoapService;
import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import com.cloudera.cem.efm.service.c2protocol.StandardC2ProtocolService;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/*
 * This test exists in the c2-protocol-web-api module instead
 * of c2-protocol-api because the Jax-RS Link.Builder
 * methods called for the SimpleLink -> Link conversion
 * have a runtime dependency on a Jersey RuntimeDelegate
 */
public class TestCoapService {


    @Test
    public void testValidPort() throws ProtocolException {

        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        properties.setPort(8882);
        try ( CoapService service = new CoapService(c2protocolservice,properties) ) {
            assertNotNull(service);
        }
    }

    @Test(expected = ProtocolException.class)
    public void testValidPortHostName() throws ProtocolException {

        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        properties.setPort(8383);
        properties.setHost("mrthesegfault");
        CoapService service = new CoapService(c2protocolservice,properties);
            assertNotNull(service);
    }

    @Test(expected = ProtocolException.class)
    public void testInvalidHostName() throws ProtocolException {
        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        properties.setPort(8383);
        properties.setHost("8797");
        CoapService service = new CoapService(c2protocolservice,properties);
        assertNotNull(service);
    }


    @Test
    public void testEmptyPort() throws ProtocolException {

        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        try ( CoapService service = new CoapService(c2protocolservice,properties) ) {
            assertNotNull(service);
        }
    }


    @Test(expected = ProtocolException.class)
    public void testEmptyPortThrowExceptionAck() throws ProtocolException, ProtocolException {

        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        try ( CoapService service = new CoapService(c2protocolservice,properties) ) {
            assertNotNull(service);
            AcknowledgementPayload payload = mock(AcknowledgementPayload.class);
            service.servicePayload(payload);
        }
    }

    @Test(expected = ProtocolException.class)
    public void testEmptyPortThrowExceptionHeartbeat() throws ProtocolException, ProtocolException {

        C2ProtocolService c2protocolservice = mock(StandardC2ProtocolService.class);
        CoapProperties properties = new CoapProperties();
        try ( CoapService service = new CoapService(c2protocolservice,properties) ) {
            assertNotNull(service);
            HeartbeatPayload payload = mock(HeartbeatPayload.class);
            service.servicePayload(payload);
        }
    }


}
