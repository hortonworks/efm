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
package com.cloudera.cem.efm.security.authentication.proxy

import com.cloudera.cem.efm.security.authentication.proxy.*

import org.apache.nifi.registry.security.authentication.AuthenticationRequest
import org.apache.nifi.registry.security.authentication.exception.InvalidCredentialsException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor
import spock.lang.Specification

import java.security.Principal
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.time.Instant

class ProxyIdentityProviderSpec extends Specification {

    private static final String DEFAULT_HEADER_NAME = "x-webauth-user";
    private static final String DEFAULT_REQ_USER = "Alice";
    private static final String DEFAULT_REQ_IP = "1.1.1.1";

    def setup() {
    }

    def supports() {
        given:
        def proxyIdentityProvider  = genProxyIdentityProvider()

        expect:
        proxyIdentityProvider.supports(ProxyAuthenticationRequest.class)
        !proxyIdentityProvider.supports(AuthenticationRequest.class)
    }

    def disabled() {
        given:
        def proxyIdentityProvider = genProxyIdentityProvider(false)
        def request = mockHttpServletRequest()

        expect:
        proxyIdentityProvider.extractCredentials(request) == null
    }

    def extractCredentials() {

        when: "default settings, header value not present"
        def proxyIdentityProvider = genProxyIdentityProvider()
        def request = new MockHttpServletRequest()
        def credentials = proxyIdentityProvider.extractCredentials(request)

        then: "null is returned"
        credentials == null


        when: "default settings are used, header value present, no client cert"
        request = mockHttpServletRequest()
        credentials = proxyIdentityProvider.extractCredentials(request)
        def proxyCredentials = credentials.getCredentials()

        then:
        with(credentials) {
            getUsername() == DEFAULT_REQ_USER
            getCredentials() != null
        }
        proxyCredentials instanceof ProxyAuthenticationRequest.ProxyCredentials
        with((ProxyAuthenticationRequest.ProxyCredentials)proxyCredentials) {
            clientIp == DEFAULT_REQ_IP
            clientCertificate == null
        }


        when: "custom settings are used, header value present, client cert"
        proxyIdentityProvider = genProxyIdentityProvider(true, "x-user")
        request = mockHttpServletRequest("x-user", "Bob", "2.2.2.2", mockX509Certificate("CN=proxy, O=ACME"))
        credentials = proxyIdentityProvider.extractCredentials(request)
        proxyCredentials = credentials.getCredentials()

        then:
        with(credentials) {
            getUsername() == "Bob"
            getCredentials() != null
        }
        proxyCredentials instanceof ProxyAuthenticationRequest.ProxyCredentials
        with((ProxyAuthenticationRequest.ProxyCredentials)proxyCredentials) {
            clientIp == "2.2.2.2"
            clientCertificate != null
            clientCertificate.getSubjectDN().getName() == "CN=proxy, O=ACME"
        }

    }

    def authenticate() {

        expect: "provider does not authenticate non-supported authentication requests"
        genProxyIdentityProvider().authenticate(new AuthenticationRequest()) == null


        when: "no whitelists are set"
        def proxyIdentityProvider = genProxyIdentityProvider()
        def authRequest = genProxyAuthenticationRequest("Alice", "1.2.3.4", null)
        def authResponse = proxyIdentityProvider.authenticate(authRequest)

        then: "provider authenticates proxy user"
        with(authResponse) {
            authResponse.username == DEFAULT_REQ_USER
            authResponse.identity == DEFAULT_REQ_USER
            authResponse.issuer == ProxyIdentityProvider.class.getSimpleName()
        }

    }

    def authenticateWithIpWhitelist() {

        when: "request passes ip whitelist check"
        def proxyIdentityProvider = genProxyIdentityProvider(true, DEFAULT_HEADER_NAME, ["1.1.1.1", "1.1.1.2"], null)
        def authRequest = genProxyAuthenticationRequest("Bob", "1.1.1.2")
        def authResponse = proxyIdentityProvider.authenticate(authRequest)

        then: "provider authenticates proxy user"
        with(authResponse) {
            authResponse.username == "Bob"
            authResponse.identity == "Bob"
            authResponse.issuer == ProxyIdentityProvider.class.getSimpleName()
        }


        when: "request fails ip whitelist check"
        proxyIdentityProvider = genProxyIdentityProvider(true, DEFAULT_HEADER_NAME, ["1.1.1.1", "1.1.1.2"], null)
        authRequest = genProxyAuthenticationRequest("Bob", "2.2.2.2")
        proxyIdentityProvider.authenticate(authRequest)

        then: "exception is thrown"
        thrown(InvalidCredentialsException.class)

    }

    def authenticateWithDnWhitelist() {

        given:
        def proxyIdentityProvider = genProxyIdentityProvider(true, DEFAULT_HEADER_NAME, null, ["CN=proxy1, O=ACME", "CN=proxy2, O=ACME"])
        proxyIdentityProvider.setPrincipalExtractor(new X509PrincipalExtractor() {
            @Override
            Object extractPrincipal(X509Certificate cert) {
                return cert.getSubjectDN().getName()
            }
        })

        when: "request passes dn whitelist check"
        def authRequest = genProxyAuthenticationRequest("Charlie", "1.1.1.1", mockX509Certificate("CN=proxy1, O=ACME"))
        def authResponse = proxyIdentityProvider.authenticate(authRequest)

        then: "provider authenticates proxy user"
        with(authResponse) {
            authResponse.username == "Charlie"
            authResponse.identity == "Charlie"
            authResponse.issuer == ProxyIdentityProvider.class.getSimpleName()
        }

        when: "request fails dn whitelist check"
        authRequest = genProxyAuthenticationRequest("Charlie", "1.1.1.1", mockX509Certificate("CN=proxy3, O=ACME"))
        proxyIdentityProvider.authenticate(authRequest)

        then: "exception is thrown"
        thrown InvalidCredentialsException


        when: "client cert is not yet valid"
        authRequest = genProxyAuthenticationRequest("Charlie", "1.1.1.1", mockX509Certificate("CN=proxy1, O=ACME", Instant.now().plusSeconds(60)))
        proxyIdentityProvider.authenticate(authRequest)

        then: "exception is thrown"
        thrown InvalidCredentialsException


        when: "client cert is expired"
        authRequest = genProxyAuthenticationRequest("Charlie", "1.1.1.1", mockX509Certificate("CN=proxy1, O=ACME", Instant.now().minusSeconds(60), Instant.now().minusSeconds(30)))
        proxyIdentityProvider.authenticate(authRequest)

        then: "exception is thrown"
        thrown InvalidCredentialsException

    }


    // Helpers

    def genProxyIdentityProvider(
            boolean enabled = true,
            String headerName = null,
            Collection<String> ipWhitelist = null,
            Collection<String> dnWhitelist = null) {
        def properties = new ProxyIdentityProviderProperties(enabled: enabled)
        if (headerName != null) {
            properties.setHeaderName(headerName)
        }
        if (ipWhitelist != null) {
            properties.setIpWhitelist(ipWhitelist)
        }
        if (dnWhitelist != null) {
            properties.setDnWhitelist(dnWhitelist)
        }
        return new ProxyIdentityProvider(properties, Optional.empty())
    }

    def genProxyAuthenticationRequest(
            String username = DEFAULT_REQ_USER,
            String clientIp = DEFAULT_REQ_IP,
            X509Certificate clientCert = null) {
        return new ProxyAuthenticationRequest(username, new ProxyAuthenticationRequest.ProxyCredentials(clientIp, clientCert))
    }

    def mockHttpServletRequest(
            String userHeaderName = DEFAULT_HEADER_NAME,
            String user = DEFAULT_REQ_USER,
            String ip = DEFAULT_REQ_IP,
            X509Certificate certificate = null) {
        def request = new MockHttpServletRequest()
        request.setRemoteAddr(ip)
        if (userHeaderName != null && user != null) {
            request.addHeader(userHeaderName, user)
        }
        if (certificate != null) {
            X509Certificate[] certs = new X509Certificate[1]
            certs[0] = certificate
            request.setAttribute("javax.servlet.request.X509Certificate", certs)
        }
        return request
    }

    def mockX509Certificate(
            String subjectDn,
            Instant startDate = Instant.now().minusSeconds(1),
            Instant endDate = Instant.now().plusSeconds(60)) {

        def cert = Mock(X509Certificate)
        cert.checkValidity() >> {
            if (Instant.now().isBefore(startDate)) {
                throw new CertificateNotYetValidException()
            } else if (Instant.now().isAfter(endDate)) {
                throw new CertificateExpiredException()
            }
        }
        cert.getSubjectDN() >> {
            return new Principal(){
                @Override
                String getName() {
                    return subjectDn
                }
            }
        }
        return cert
    }

}
