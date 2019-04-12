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
package com.cloudera.cem.efm.coap.service;

import com.google.common.annotations.VisibleForTesting;
import com.cloudera.cem.efm.coap.payload.AcknowledgementPayload;
import com.cloudera.cem.efm.coap.payload.CoapResponse;
import com.cloudera.cem.efm.coap.payload.HeartbeatPayload;
import com.cloudera.cem.efm.coap.payload.HeartbeatResponse;
import com.cloudera.cem.efm.coap.resources.AcknowledgementResource;
import com.cloudera.cem.efm.coap.resources.HeartbeatResource;
import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import com.cloudera.cem.efm.service.c2protocol.HeartbeatContext;
import com.cloudera.cem.efm.service.protocol.PayloadService;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Purpose: CoAP Service is the functional payload service that interacts with the protocol service.
 * Design: Simple extension of PayloadService.
 *
 */
@Service
public class CoapService extends PayloadService implements AutoCloseable{

    private static final Logger LOGGER = LoggerFactory.getLogger(CoapService.class);

    private static final String LOCALHOST = "localhost";

    public static final String ANY = "any";

    public static final String ANY_IP = "0.0.0.0";

    private final CoapServer server;

    private final ScheduledExecutorService coapExecutor;

    private final CoapProperties coapProperties;


    /**
     * Constructor for the coap service.
     * @param service the underlying C2ProtocolService that will process the business logic for heartbeat
     */
    @Autowired
    public CoapService(final C2ProtocolService service,final CoapProperties coapProperties) throws ProtocolException {
        super(service);

        this.coapProperties = coapProperties;

        final String host = Optional.of(coapProperties.getHost()).orElse("localhost");
        final int port = coapProperties.getPort();

        if (port > 0){
            LOGGER.info("Server port running on port {}",port);
            // retrieve the number of threads for our executor.
            final int threads = coapProperties.getThreads() > 0 ? coapProperties.getThreads() : 1;
            coapExecutor = Executors.newScheduledThreadPool(threads);
            // let the NumberFormatException propagate.
            server = new CoapServer();
            server.setExecutor(coapExecutor);
            bindToInterfaces(server,host,port);
            initializeHandlers();
            server.start();
        } else {
            LOGGER.info("Server not needed as host is configured to be {} and port is {}",host,port);
            coapExecutor = null;
            server = null;
        }
    }

    private void bindToInterfaces(final CoapServer server, final String host, final int port) throws ProtocolException {
        boolean isAny = false;
        if (ANY.equalsIgnoreCase(host) || ANY_IP.equals(host)){
            isAny = true;
        }
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            if (addr instanceof Inet4Address) {
                if (isAny || addr.getHostName().equalsIgnoreCase(host) || (host.equalsIgnoreCase(LOCALHOST) && addr.isLoopbackAddress())) {
                    InetSocketAddress bindToAddress = new InetSocketAddress(addr, port);
                    server.addEndpoint(new CoapEndpoint.CoapEndpointBuilder().setInetSocketAddress(bindToAddress).build());
                }
            }
        }
        if (server.getEndpoints().isEmpty()){
            throw new ProtocolException("Must define a valid endpoint");
        }
    }

    /**
     * Services Acknowledgement payloads. These contain information corresponding with what will be sent to clients
     * that are consumers of this service.
     * @param payload acknowledgement payload
     * @return CoAP response, which is optional
     * @throws ProtocolException if any error occurs while servicing the payload
     */
    public Optional<CoapResponse> servicePayload(final AcknowledgementPayload payload) throws ProtocolException {
        if (null == server)
            throw new ProtocolException("Call invalid when service is not properly configured");
        try {
            service.processOperationAck(payload.getRawPayload());
        } catch (IOException e) {
            throw new ProtocolException(e);
        }
        // no response is necessary.
        return Optional.empty();
    }


    /**
     * Services heartbeat payloads, whose responses will be sent to the client
     * @param payload heartbeat payload
     * @return Optional CoAP response.
     * @throws ProtocolException if any error occurs while servicing the payload
     */
    public Optional<CoapResponse> servicePayload(HeartbeatPayload payload) throws ProtocolException {
        if (null == server)
            throw new ProtocolException("Call invalid when service is not properly configured");
        LOGGER.trace("Received heartbeat");
        try {
            HeartbeatContext context = HeartbeatContext.empty();
            Optional<URI> uri = getURI();
            if (uri.isPresent()){
                context = HeartbeatContext.builder().baseUri(uri.get()).build();
            }
            return Optional.of(new HeartbeatResponse(payload.getMajorVersion(),  service.processHeartbeat(payload.getRawPayload(), context)));
        } catch (IOException e) {
            throw new ProtocolException(e);
        }
    }


    private void initializeHandlers(){
        if (null != server){
            server.add(new HeartbeatResource(service,this));
            server.add(new AcknowledgementResource(service,this));
        }
    }

    @VisibleForTesting
    public int getPort(){
        if (null != server){
            for(Endpoint endpoint : server.getEndpoints()) {
                return endpoint.getAddress().getPort();
            }
        }
        return -1;
    }

    @Override
    public void close() {
        if (null != server){
            server.stop();
        }
    }
}
