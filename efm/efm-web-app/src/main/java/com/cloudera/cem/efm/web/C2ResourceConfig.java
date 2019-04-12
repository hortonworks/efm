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
 *
 * This file incorporates works covered by the following copyright and permission notice:
 *
 *     Apache NiFi - MiNiFi
 *     Copyright 2014-2018 The Apache Software Foundation
 *
 *     Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.cloudera.cem.efm.web;

import com.cloudera.cem.efm.web.api.ApplicationResource;
import com.cloudera.cem.efm.web.mapper.C2JsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.List;

/**
 * This is the main Jersey configuration for the C2 REST API web application.
 */
@Configuration
@ApplicationPath("/api")
public class C2ResourceConfig extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(C2ResourceConfig.class);

    public C2ResourceConfig(
            @Context ServletContext servletContext,
            @Autowired List<ApplicationResource> resourceEndpoints,
            @Autowired List<ExceptionMapper> exceptionMappers) {
        // register filters
        register(HttpMethodOverrideFilter.class);

        // registry Jackson Object Mapper Resolver
        register(C2JsonProvider.class);

        // register the exception mappers & jackson object mapper resolver
        for (ExceptionMapper mapper : exceptionMappers) {
            logger.info("Registering {}", mapper.getClass().getName());
            register(mapper.getClass());
        }

        // register endpoints
        for (ApplicationResource resource : resourceEndpoints) {
            logger.info("Registering {}", resource.getClass().getName());
            register(resource.getClass());
        }

        // include bean validation errors in response
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        // This is necessary for Kerberos auth via SPNEGO to work correctly when responding
        // "401 Unauthorized" with a "WWW-Authenticate: Negotiate" header value.
        // If this value needs to be changed, Kerberos authentication needs to move to filter chain
        // so it can directly set the HttpServletResponse instead of indirectly through a JAX-RS Response
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);

    }

}