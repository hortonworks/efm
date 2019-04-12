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
package com.cloudera.cem.efm.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.cloudera.cem.efm.client.C2ClientConfig;
import com.cloudera.cem.efm.db.DatabaseProfileValueSource;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * A base class to simplify creating integration tests against an API application running with an embedded server and volatile DB.
 */
@ProfileValueSourceConfiguration(DatabaseProfileValueSource.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected Environment environment;

    @Autowired
    private ServerProperties serverProperties;

    /* OPTIONAL: Any subclass that extends this base class MAY provide or specify a @TestConfiguration that provides a
     * C2ClientConfig @Bean. The properties specified should correspond with the integration test cases in
     * the concrete subclass. See SecureFileIT for an example. */
    @Autowired(required = false)
    private C2ClientConfig clientConfig;

    /* This will be injected with the random port assigned to the embedded Jetty container. */
    @LocalServerPort
    private int port;

    /**
     * Subclasses can access this auto-configured JAX-RS client to communicate to the C2 Server
     */
    protected Client client;

    protected ObjectMapper mapper;

    @PostConstruct
    void initialize() {
        if (this.clientConfig != null) {
            this.client = createClientFromConfig(this.clientConfig);
        } else {
            this.client = ClientBuilder.newClient();
        }

        this.mapper = new ObjectMapper();
        this.mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        this.mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(mapper.getTypeFactory()));
        // Ignore unknown properties so that deployed client remain compatible with future versions of C2 server that add new fields
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Subclasses can utilize this method to build a URL that has the correct protocol, hostname, and port
     * for a given path.
     *
     * @param relativeResourcePath the path component of the resource you wish to access, relative to the
     *                             base API URL, where the base includes the servlet context path.
     *
     * @return a String containing the absolute URL of the resource.
     */
    protected String createURL(String relativeResourcePath) {
        if (relativeResourcePath == null) {
            throw new IllegalArgumentException("Resource path cannot be null");
        }

        final StringBuilder baseUriBuilder = new StringBuilder(createBaseURL()).append("/efm/api");

        if (!relativeResourcePath.startsWith("/")) {
            baseUriBuilder.append('/');
        }
        baseUriBuilder.append(relativeResourcePath);

        return baseUriBuilder.toString();
    }

    /**
     * Sub-classes can utilize this method to obtain the base-url for a client.
     *
     * @return a string containing the base url which includes the scheme, host, and port
     */
    protected String createBaseURL() {
        final boolean isSecure = serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled();
        final String protocolSchema = isSecure ? "https" : "http";

        final StringBuilder baseUriBuilder = new StringBuilder()
                .append(protocolSchema).append("://localhost:").append(port);

        return baseUriBuilder.toString();
    }

    protected C2ClientConfig createClientConfig(String baseUrl) {
        final C2ClientConfig.Builder builder = new C2ClientConfig.Builder();
        builder.baseUrl(baseUrl);

        if (this.clientConfig != null) {
            if (this.clientConfig.getSslContext() != null) {
                builder.sslContext(this.clientConfig.getSslContext());
            }

            if (this.clientConfig.getHostnameVerifier() != null) {
                builder.hostnameVerifier(this.clientConfig.getHostnameVerifier());
            }
        }

        return builder.build();
    }

    private Client createClientFromConfig(C2ClientConfig c2ClientConfig) {

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(jacksonJaxbJsonProvider());

        final ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(clientConfig);

        final SSLContext sslContext = c2ClientConfig.getSslContext();
        if (sslContext != null) {
            clientBuilder.sslContext(sslContext);
        }

        final HostnameVerifier hostnameVerifier = c2ClientConfig.getHostnameVerifier();
        if (hostnameVerifier != null) {
            clientBuilder.hostnameVerifier(hostnameVerifier);
        }

        return clientBuilder.build();
    }

    private JacksonJaxbJsonProvider jacksonJaxbJsonProvider() {
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();
        jacksonJaxbJsonProvider.setMapper(mapper);
        return jacksonJaxbJsonProvider;
    }

}
