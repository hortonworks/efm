/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationResource.class);

    static final String BETA_INDICATION = "[BETA]";

    @Context
    protected HttpServletRequest httpServletRequest;

    @Context
    private UriInfo uriInfo;

    protected URI getUri(final String... path) {
        final UriBuilder uriBuilder = getUriBuilder(path);
        return uriBuilder != null ? uriBuilder.build() : null;
    }

    protected UriBuilder getUriBuilder(final String... path) {
        final UriBuilder uriBuilder = getBaseUriBuilder();
        return uriBuilder != null ? uriBuilder.segment(path) : null;
    }

    protected URI getBaseUri() {
        // Forwarded Headers are expected to have been applied as part of servlet filter chain
        return uriInfo.getBaseUri();
    }

    protected UriBuilder getBaseUriBuilder() {
        // Forwarded Headers are expected to have been applied as part of servlet filter chain
        return uriInfo.getBaseUriBuilder();
    }

    /**
     * Generates a 201 Created response with the specified content.
     *
     * @param uri    The URI
     * @param entity entity
     * @return The response to be built
     */
    protected Response.ResponseBuilder generateCreatedResponse(final URI uri, final Object entity) {
        // generate the response builder
        return Response.created(uri).entity(entity);
    }
}
