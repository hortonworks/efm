/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.client.impl.jersey;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.cloudera.cem.efm.client.AgentClassClient;
import com.cloudera.cem.efm.client.AgentClient;
import com.cloudera.cem.efm.client.AgentManifestClient;
import com.cloudera.cem.efm.client.C2ConfigurationClient;
import com.cloudera.cem.efm.client.ProtocolClient;
import com.cloudera.cem.efm.client.EventClient;
import com.cloudera.cem.efm.client.FlowClient;
import com.cloudera.cem.efm.client.FlowMappingClient;
import com.cloudera.cem.efm.client.C2Client;
import com.cloudera.cem.efm.client.C2ClientConfig;
import com.cloudera.cem.efm.client.OperationClient;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;

/**
 * A NiFiRegistryClient that uses Jersey Client.
 */
public class JerseyC2Client implements C2Client {

    public static final String DEFAULT_C2_CONTEXT = "/efm/api";
    static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    static final int DEFAULT_READ_TIMEOUT = 10000;

    private final Client client;
    private final WebTarget baseTarget;

    private final AgentClassClient agentClassClient;
    private final AgentClient agentClient;
    private final ProtocolClient c2ProtocolClient;
    private final OperationClient operationClient;
    private final AgentManifestClient manifestClient;
    private final FlowClient flowClient;
    private final FlowMappingClient flowMappingClient;
    private final EventClient eventClient;
    private final C2ConfigurationClient c2ConfigurationClient;


    private JerseyC2Client(final C2Client.Builder builder) {
        final C2ClientConfig c2ClientConfig = builder.getConfig();
        if (c2ClientConfig == null) {
            throw new IllegalArgumentException("C2ClientConfig cannot be null");
        }

        String baseUrl = c2ClientConfig.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Base URL cannot be blank");
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        final String contextPath = c2ClientConfig.getContextPathOptional().orElse(DEFAULT_C2_CONTEXT);
        if (!baseUrl.endsWith(contextPath)) {
            baseUrl = baseUrl + contextPath;
        }

        try {
            new URI(baseUrl);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid base URL: " + e.getMessage(), e);
        }

        final SSLContext sslContext = c2ClientConfig.getSslContext();
        final HostnameVerifier hostnameVerifier = c2ClientConfig.getHostnameVerifier();

        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        if (sslContext != null) {
            clientBuilder.sslContext(sslContext);
        }
        if (hostnameVerifier != null) {
            clientBuilder.hostnameVerifier(hostnameVerifier);
        }

        final int connectTimeout = c2ClientConfig.getConnectTimeout() == null ? DEFAULT_CONNECT_TIMEOUT : c2ClientConfig.getConnectTimeout();
        final int readTimeout = c2ClientConfig.getReadTimeout() == null ? DEFAULT_READ_TIMEOUT : c2ClientConfig.getReadTimeout();

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
        clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeout);
        clientConfig.register(jacksonJaxbJsonProvider());
        clientBuilder.withConfig(clientConfig);
        this.client = clientBuilder.build();

        this.baseTarget = client.target(baseUrl);
        this.agentClient = new JerseyAgentClient(baseTarget);
        this.agentClassClient = new JerseyAgentClassClient(baseTarget);
        this.manifestClient = new JerseyAgentManifestClient(baseTarget);
        this.c2ProtocolClient = new JerseyProtocolClient(baseTarget);
        this.operationClient = new JerseyOperationClient(baseTarget);
        this.flowClient = new JerseyFlowClient(baseTarget);
        this.flowMappingClient = new JerseyFlowMappingClient(baseTarget);
        this.eventClient = new JerseyEventClient(baseTarget);
        this.c2ConfigurationClient = new JerseyC2ConfigurationClient(baseTarget);

    }

    @Override
    public AgentClient getAgentClient() {
        return this.agentClient;
    }

    @Override
    public AgentClassClient getAgentClassClient() {
        return this.agentClassClient;
    }


    public AgentManifestClient getAgentManifestClient() {
        return manifestClient;
    }

    @Override
    public ProtocolClient getProtocolClient() {
        return this.c2ProtocolClient;
    }

    @Override
    public OperationClient getOperationClient() {
        return this.operationClient;
    }

    @Override
    public FlowClient getFlowClient() {
        return this.flowClient;
    }

    @Override
    public FlowMappingClient getFlowMappingClient() {
        return this.flowMappingClient;
    }

    @Override
    public EventClient getEventClient() {
        return this.eventClient;
    }

    @Override
    public C2ConfigurationClient getC2ConfigurationClient() {
        return this.c2ConfigurationClient;
    }

    @Override
    public void close() {
        if (this.client != null) {
            try {
                this.client.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Builder for creating a JerseyNiFiRegistryClient.
     */
    public static class Builder implements C2Client.Builder {

        private C2ClientConfig clientConfig;

        @Override
        public Builder config(final C2ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        @Override
        public C2ClientConfig getConfig() {
            return clientConfig;
        }

        @Override
        public JerseyC2Client build() {
            return new JerseyC2Client(this);
        }

    }

    private static JacksonJaxbJsonProvider jacksonJaxbJsonProvider() {
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(mapper.getTypeFactory()));
        // Ignore unknown properties so that deployed client remain compatible with future versions of C2 that add new fields
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);

        jacksonJaxbJsonProvider.setMapper(mapper);
        return jacksonJaxbJsonProvider;
    }
}