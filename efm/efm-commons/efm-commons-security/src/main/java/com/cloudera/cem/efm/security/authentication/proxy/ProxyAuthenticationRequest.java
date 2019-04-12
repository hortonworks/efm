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

import org.apache.nifi.registry.security.authentication.AuthenticationRequest;

import java.security.cert.X509Certificate;

public class ProxyAuthenticationRequest extends AuthenticationRequest {

    public ProxyAuthenticationRequest(String username, ProxyCredentials credentials) {
        super(username, credentials, null);
    }

    @Override
    public ProxyCredentials getCredentials() {
        return (ProxyCredentials)super.getCredentials();
    }

    @Override
    public void setCredentials(Object credentials) {
        if (!(credentials instanceof ProxyCredentials)) {
            throw new IllegalArgumentException("Proxy Authentication Request credentials must be instance of " + ProxyCredentials.class.getSimpleName());
        }
        super.setCredentials(credentials);
    }

    public static class ProxyCredentials {
        private String clientIp;
        private X509Certificate clientCertificate;

        public ProxyCredentials() {
        }

        public ProxyCredentials(String clientIp, X509Certificate clientCertificate) {
            this.clientIp = clientIp;
            this.clientCertificate = clientCertificate;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public X509Certificate getClientCertificate() {
            return clientCertificate;
        }

        public void setClientCertificate(X509Certificate clientCertificate) {
            this.clientCertificate = clientCertificate;
        }
    }


}
