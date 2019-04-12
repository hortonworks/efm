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
package com.cloudera.cem.efm.coap.resources;

import com.cloudera.cem.efm.coap.payload.CoapResponse;
import com.cloudera.cem.efm.coap.payload.ReceivingPayload;
import com.cloudera.cem.efm.coap.service.CoapService;
import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.apache.commons.lang3.Validate;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Purpose and Design: Encapsulates receiving payload capabilities with a type
 * that extends the Receiving payload. Sole purpose is to avoid code duplication
 * since the receiving types generally perform much of the same operation.
 */
public abstract class PostResource<T extends ReceivingPayload> extends CoapResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostResource.class);

    private final C2ProtocolService service;
    protected final CoapService coapServicer;

    public PostResource(final String resource, final C2ProtocolService service, final CoapService servicer){
        super(resource);
        Validate.notNull(service, "C2 protocol service must be valid");
        this.service = service;
        this.coapServicer = servicer;
    }

    /**
     * Creates responses for different endoints.
     * @param packet CoAP packet.
     * @return optional CoapResponse.
     * @throws IOException error in I/O
     * @throws ProtocolException Protocol error produced within this package.
     */
    protected abstract Optional<CoapResponse> createResponse(final CoapExchange packet) throws IOException, ProtocolException;

    @Override
    public void handlePOST(CoapExchange exchange) {
        final byte [] payload = exchange.getRequestPayload();
        try {
            LOGGER.trace("received message payload size: {}", payload.length);
            Optional<CoapResponse> response = createResponse(exchange);
            if ( response.isPresent() ){
                Optional<byte[]> reponsePacket = response.get().getCoapPacket();
                if (reponsePacket.isPresent()){
                    exchange.respond(CoAP.ResponseCode.CONTENT,reponsePacket.get());
                    return;
                } else{
                    LOGGER.debug("Response produced, but no return packet");
                    exchange.respond(CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE);
                    return;
                }
            } else{
                LOGGER.debug("Payload Servicing failed for payload identified by {}",exchange.getRequestText());
            }

        } catch (ProtocolException | IOException e) {
            LOGGER.error("Protocol Exception occurred while receiving packet",e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            return;
        }
        // the CoAP response codes are limited by the spec and are application dependent. What is implemented
        // here ( in Java-CoAP ) isn't implemented across other libraries.
        exchange.respond(CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE);
    }
}