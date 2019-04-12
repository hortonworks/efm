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
package com.cloudera.cem.efm.web.security;

import com.cloudera.cem.efm.security.authentication.proxy.ProxyIdentityProvider;
import com.cloudera.cem.efm.util.IdentityMapping;
import com.cloudera.cem.efm.web.security.authentication.AnonymousIdentityFilter;
import com.cloudera.cem.efm.web.security.authentication.IdentityAuthenticationProvider;
import com.cloudera.cem.efm.web.security.authentication.IdentityFilter;
import com.cloudera.cem.efm.web.security.authentication.knox.KnoxIdentityProvider;
import com.cloudera.cem.efm.web.security.authentication.x509.X509IdentityAuthenticationProvider;
import com.cloudera.cem.efm.web.security.authentication.x509.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * NiFi Registry Web Api Spring security
 */
@Component
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class C2SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(C2SecurityConfig.class);

    private ServerProperties serverProperties;

    private AnonymousIdentityFilter anonymousAuthenticationFilter = new AnonymousIdentityFilter();

    private final X509IdentityProvider x509IdentityProvider;
    private IdentityFilter x509AuthenticationFilter;
    private IdentityAuthenticationProvider x509AuthenticationProvider;

    private final KnoxIdentityProvider knoxIdentityProvider;
    private IdentityFilter knoxAuthenticationFilter;
    private IdentityAuthenticationProvider knoxAuthenticationProvider;

    private final ProxyIdentityProvider proxyIdentityProvider;
    private IdentityFilter proxyAuthenticationFilter;
    private IdentityAuthenticationProvider proxyAuthenticationProvider;

    @Autowired
    public C2SecurityConfig(
            ServerProperties properties,
            Optional<X509IdentityProvider> x509IdentityProvider,
            Optional<KnoxIdentityProvider> knoxIdentityProvider,
            Optional<ProxyIdentityProvider> proxyIdentityProvider) {
        super(true); // disable defaults
        this.serverProperties = properties;
        this.x509IdentityProvider = x509IdentityProvider.orElse(null);
        this.knoxIdentityProvider = knoxIdentityProvider.orElse(null);
        this.proxyIdentityProvider = proxyIdentityProvider.orElse(null);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .rememberMe().disable()
                .authorizeRequests()
                    .anyRequest().fullyAuthenticated()
                    .and()
                .exceptionHandling()
                    .authenticationEntryPoint(http401AuthenticationEntryPoint())
                    .and()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // Generic SSO Authenticating Proxy
        if (proxyIdentityProvider != null) {
            http.addFilterBefore(proxyAuthenticationFilter(), AnonymousAuthenticationFilter.class);
        }

        // x509
        if (x509IdentityProvider != null) {
            http.addFilterBefore(x509AuthenticationFilter(), AnonymousAuthenticationFilter.class);
        }

        // Knox
        if (knoxIdentityProvider != null) {
            http.addFilterBefore(knoxAuthenticationFilter(), AnonymousAuthenticationFilter.class);
        }

        if (serverProperties.getSsl() == null || !serverProperties.getSsl().isEnabled()) {
            // If we are running unsecured add an
            // anonymous authentication filter that will populate the
            // authenticated, anonymous user if no other user identity
            // is detected earlier in the Spring filter chain.
            http.anonymous().authenticationFilter(anonymousAuthenticationFilter);
        }
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        if (proxyIdentityProvider != null) {
            auth.authenticationProvider(proxyAuthenticationProvider());
        }
        if (x509IdentityProvider != null) {
            auth.authenticationProvider(x509AuthenticationProvider());
        }
        if (knoxIdentityProvider != null) {
            auth.authenticationProvider(knoxAuthenticationProvider());
        }
    }

    // ---- X509 ----

    private IdentityFilter x509AuthenticationFilter() {
        if (x509AuthenticationFilter == null) {
            x509AuthenticationFilter = new IdentityFilter(x509IdentityProvider);
        }
        return x509AuthenticationFilter;
    }

    private IdentityAuthenticationProvider x509AuthenticationProvider() {
        if (x509AuthenticationProvider == null) {
            x509AuthenticationProvider = new X509IdentityAuthenticationProvider(null, x509IdentityProvider, getIdentityMappings());
        }
        return x509AuthenticationProvider;
    }

    // ---- Knox ----

    private IdentityFilter knoxAuthenticationFilter() {
        if (knoxAuthenticationFilter == null) {
            knoxAuthenticationFilter = new IdentityFilter(knoxIdentityProvider);
        }
        return knoxAuthenticationFilter;
    }

    private IdentityAuthenticationProvider knoxAuthenticationProvider() {
        if (knoxAuthenticationProvider == null) {
            knoxAuthenticationProvider = new IdentityAuthenticationProvider(null, knoxIdentityProvider, getIdentityMappings());
        }
        return knoxAuthenticationProvider;
    }

    // ---- Generic Proxy ----

    private IdentityFilter proxyAuthenticationFilter() {
        if (proxyAuthenticationFilter == null && proxyIdentityProvider != null) {
            proxyAuthenticationFilter = new IdentityFilter(proxyIdentityProvider);
        }
        return proxyAuthenticationFilter;
    }

    private IdentityAuthenticationProvider proxyAuthenticationProvider() {
        if (proxyAuthenticationProvider == null && proxyIdentityProvider != null) {
            proxyAuthenticationProvider = new IdentityAuthenticationProvider(null, proxyIdentityProvider, getIdentityMappings());
        }
        return proxyAuthenticationProvider;
    }

    private List<IdentityMapping> getIdentityMappings() {
        // TODO, reintroduce refactored IdentityMapping capability that does not rely on C2Properties
        return Collections.emptyList();
    }

    private AuthenticationEntryPoint http401AuthenticationEntryPoint() {
        // This gets used for both secured and unsecured configurations. It will be called by Spring Security if a request makes it through the filter chain without being authenticated.
        // For unsecured, this should never be reached because the custom AnonymousAuthenticationFilter should always populate a fully-authenticated anonymous user
        // For secured, this will cause attempt to access any API endpoint (except those explicitly ignored) without providing credentials to return a 401 Unauthorized challenge
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException {
                logger.info("Client could not be authenticated due to: {} Returning 401 response.", authenticationException.toString());
                logger.debug("", authenticationException);
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("text/plain");
                    response.getWriter().println(String.format("%s Contact the system administrator.", authenticationException.getLocalizedMessage()));
                }
            }
        };
    }

}
