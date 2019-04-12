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
package com.cloudera.cem.efm.web.security.authentication.knox;

import com.cloudera.cem.efm.annotation.ConditionalOnKnox;
import com.cloudera.cem.efm.web.security.authentication.exception.KnoxAuthenticationException;
import com.nimbusds.jose.JOSEException;
import org.apache.nifi.registry.security.authentication.AuthenticationRequest;
import org.apache.nifi.registry.security.authentication.AuthenticationResponse;
import org.apache.nifi.registry.security.authentication.IdentityProvider;
import org.apache.nifi.registry.security.authentication.IdentityProviderConfigurationContext;
import org.apache.nifi.registry.security.authentication.IdentityProviderUsage;
import org.apache.nifi.registry.security.authentication.exception.IdentityAccessException;
import org.apache.nifi.registry.security.authentication.exception.InvalidCredentialsException;
import org.apache.nifi.registry.security.exception.SecurityProviderCreationException;
import org.apache.nifi.registry.security.exception.SecurityProviderDestructionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnKnox
public class KnoxIdentityProvider implements IdentityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnoxIdentityProvider.class);

    private static final String ISSUER = KnoxIdentityProvider.class.getSimpleName();

    private static final long EXPIRATION = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);

    private static final IdentityProviderUsage USAGE = new IdentityProviderUsage() {
        @Override
        public String getText() {
            return "The user credentials must be passed in a cookie with the value being a JWT that will be " +
                    "verified by this identity provider. The name of the cookie is specified in c2.properties " +
                    "via the c2.security.user.knox.cookieName property.";
        }

        @Override
        public AuthType getAuthType() {
            return AuthType.OTHER.httpAuthScheme("Apache-Knox-SSO");
        }
    };

    private final KnoxService knoxService;

    @Autowired
    public KnoxIdentityProvider(final KnoxService knoxService) {
        this.knoxService = knoxService;
    }

    @Override
    public IdentityProviderUsage getUsageInstructions() {
        return USAGE;
    }

    @Override
    public AuthenticationRequest extractCredentials(final HttpServletRequest request) {
        // only support knox login when running securely
        if (!request.isSecure()) {
            return null;
        }

        // ensure knox sso support is enabled
        if (!knoxService.isKnoxEnabled()) {
            return null;
        }

        // get the principal out of the user token
        final String knoxJwt = getJwtFromCookie(request, knoxService.getKnoxCookieName());

        // normally in NiFi/NiFi-Registry we would return null here to continue trying other forms of authentication,
        // but in this case, if knox is enabled we want to throw an exception so we'll be re-directed to the Knox login
        if (knoxJwt == null) {
            throw new KnoxAuthenticationException("Knox JWT was not found in the request", knoxService.getKnoxUrl());
        } else {
            // otherwise create the authentication request token
            return new AuthenticationRequest(null, knoxJwt, request.getRemoteAddr());
        }
    }

    private String getJwtFromCookie(final HttpServletRequest request, final String cookieName) {
        String jwt = null;

        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        return jwt;
    }

    @Override
    public AuthenticationResponse authenticate(final AuthenticationRequest authenticationRequest)
            throws InvalidCredentialsException, IdentityAccessException {
        if (authenticationRequest == null) {
            LOGGER.info("Cannot authenticate null authenticationRequest, returning null.");
            return null;
        }

        final Object credentials = authenticationRequest.getCredentials();
        if (credentials == null) {
            throw new KnoxAuthenticationException("Knox JWT not found in authenticationRequest credentials", knoxService.getKnoxUrl());
        }

        final String jwtAuthToken = credentials instanceof String ? (String) credentials : null;
        try {
            final String jwtPrincipal = knoxService.getAuthenticationFromToken(jwtAuthToken);
            return new AuthenticationResponse(jwtPrincipal, jwtPrincipal, EXPIRATION, ISSUER);
        } catch (ParseException | JOSEException e) {
            throw new KnoxAuthenticationException(e.getMessage(), e, knoxService.getKnoxUrl());
        }
    }

    @Override
    public void onConfigured(final IdentityProviderConfigurationContext identityProviderConfigurationContext)
            throws SecurityProviderCreationException {

    }

    @Override
    public void preDestruction() throws SecurityProviderDestructionException {

    }
}
