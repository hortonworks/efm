/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.security.authentication.proxy;

import com.cloudera.cem.efm.security.util.X509CertificateUtil;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = ProxyIdentityProviderProperties.PREFIX, name = "enabled")
public class ProxyIdentityProvider implements IdentityProvider {

    private static final Logger logger = LoggerFactory.getLogger(ProxyIdentityProvider.class);

    private static final String ISSUER = ProxyIdentityProvider.class.getSimpleName();
    private static final long EXPIRATION = TimeUnit.MILLISECONDS.convert(12, TimeUnit.HOURS);
    private static final String DEFAULT_HEADER_NAME = "x-webauth-user";

    private static final IdentityProviderUsage USAGE = new IdentityProviderUsage() {
        @Override
        public String getText() {
            return "The user identity must be passed in a header set by an authenticating proxy. " +
                    "The extracted header fields can be customized via the application properties.";
        }

        @Override
        public AuthType getAuthType() {
            return AuthType.UNKNOWN;
        }
    };

    private final ProxyIdentityProviderProperties properties;
    private X509PrincipalExtractor principalExtractor;

    @Autowired
    public ProxyIdentityProvider(ProxyIdentityProviderProperties properties, Optional<X509PrincipalExtractor> principalExtractor) {
        this.principalExtractor = principalExtractor.orElse(new SubjectDnX509PrincipalExtractor());
        this.properties = properties;
        checkForInsecureConfig();
    }

    @Override
    public IdentityProviderUsage getUsageInstructions() {
        return USAGE;
    }

    @Override
    public AuthenticationRequest extractCredentials(HttpServletRequest request) {

        // check if the proxy identity provider is enabled
        if (!properties.isEnabled() || request == null) {
            return null;
        }

        // get the principal out of the user token
        final String headerName = properties.getHeaderName() != null ? properties.getHeaderName(): DEFAULT_HEADER_NAME;
        final String userIdentity = request.getHeader(headerName);

        if (userIdentity == null) {
            return null;
        }

        final String clientIp = request.getRemoteAddr();
        final X509Certificate clientCertificate = X509CertificateUtil.extractClientCertificate(request);
        final ProxyAuthenticationRequest.ProxyCredentials credentials = new ProxyAuthenticationRequest.ProxyCredentials(clientIp, clientCertificate);

        return new ProxyAuthenticationRequest(userIdentity, credentials);
    }

    @Override
    public boolean supports(Class<? extends AuthenticationRequest> authenticationRequestClazz) {
        return ProxyAuthenticationRequest.class.isAssignableFrom(authenticationRequestClazz);
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) throws InvalidCredentialsException, IdentityAccessException {

        if (authenticationRequest == null || !(authenticationRequest instanceof ProxyAuthenticationRequest)) {
            return null;
        }
        final ProxyAuthenticationRequest proxyAuthenticationRequest = (ProxyAuthenticationRequest) authenticationRequest;

        final String identity = authenticationRequest.getUsername();
        if (identity == null) {
            return null;
        }

        // Validate client IP against whitelist (if configured)
        Collection<String> ipWhitelist = properties.getIpWhitelist();
        if (ipWhitelist != null && !ipWhitelist.isEmpty()) {
            final String clientIp = proxyAuthenticationRequest.getCredentials().getClientIp();
            if (clientIp == null || !ipWhitelist.contains(clientIp)) {
                final String message = String.format("Proxy SSO identity was provided (%s), but client IP address (%s) is not in trusted whitelist.", identity, clientIp);
                logger.warn(message);
                throw new InvalidCredentialsException(message);
            }
            logger.debug("Client IP address ({}) is a trusted proxy", clientIp);
        }

        // Validate client cert DN against whitelist (if configured)
        Collection<String> dnWhitelist = properties.getDnWhitelist();
        if (dnWhitelist != null && !dnWhitelist.isEmpty()) {
            final X509Certificate clientCert = proxyAuthenticationRequest.getCredentials().getClientCertificate();
            X509CertificateUtil.validateClientCertificateOrThrow(clientCert);
            final String clientCertDN = principalExtractor.extractPrincipal(clientCert).toString();
            if (clientCertDN == null || !dnWhitelist.contains(clientCertDN)) {
                final String message = String.format("Proxy SSO identity was provided (%s), but client certificate DN (%s) is not in trusted whitelist.", identity, clientCertDN);
                logger.warn(message);
                throw new InvalidCredentialsException(message);
            }
            logger.debug("Client DN ({}) is a trusted proxy.", clientCertDN);
        }

        logger.debug("Using user identity '{}' passed by proxy.", identity);

        final AuthenticationResponse authenticationResponse = new AuthenticationResponse(identity, identity, EXPIRATION, ISSUER);
        return authenticationResponse;
    }

    @Override
    public void onConfigured(IdentityProviderConfigurationContext identityProviderConfigurationContext) throws SecurityProviderCreationException {
        // Nothing to do
    }

    @Override
    public void preDestruction() throws SecurityProviderDestructionException {
        // Nothing to do
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }

    private void checkForInsecureConfig() {
        boolean proxyWhitelistEnabled =
                (properties.getIpWhitelist() != null
                        && !properties.getIpWhitelist().isEmpty())
                || (properties.getDnWhitelist() != null
                        && !properties.getDnWhitelist().isEmpty());

        if (this.properties.isEnabled() && !proxyWhitelistEnabled) {
            logger.warn("External Proxy Authentication is enabled without DN whitelist or IP whitelist settings. " +
                    "The service may be vulnerable to user impersonation if it is bound to a network other than localhost. " +
                    "Please verify your configuration.");
        }
    }
}
