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
 *  This file incorporates works covered by the following copyright and permission notice:
 *
 *    Apache NiFi - MiNiFi
 *    Copyright 2014-2018 The Apache Software Foundation
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.cloudera.cem.efm.client;

import org.apache.nifi.security.util.KeyStoreUtils;
import org.apache.nifi.security.util.KeystoreType;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Optional;

/**
 * Configuration for a web client.
 */
public class C2ClientConfig {

    private final String baseUrl;
    private final Optional<String> contextPathOptional;
    private final SSLContext sslContext;
    private final String keystoreFilename;
    private final String keystorePass;
    private final String keyPass;
    private final KeystoreType keystoreType;
    private final String truststoreFilename;
    private final String truststorePass;
    private final KeystoreType truststoreType;
    private final HostnameVerifier hostnameVerifier;
    private final Integer readTimeout;
    private final Integer connectTimeout;


    private C2ClientConfig(final Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.contextPathOptional = Optional.ofNullable(builder.contextPath);
        this.sslContext = builder.sslContext;
        this.keystoreFilename = builder.keystoreFilename;
        this.keystorePass = builder.keystorePass;
        this.keyPass = builder.keyPass;
        this.keystoreType = builder.keystoreType;
        this.truststoreFilename = builder.truststoreFilename;
        this.truststorePass = builder.truststorePass;
        this.truststoreType = builder.truststoreType;
        this.hostnameVerifier = builder.hostnameVerifier;
        this.readTimeout = builder.readTimeout;
        this.connectTimeout = builder.connectTimeout;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Optional<String> getContextPathOptional() {
        return contextPathOptional;
    }

    public SSLContext getSslContext() {
        if (sslContext != null) {
            return sslContext;
        }

        final KeyManagerFactory keyManagerFactory;
        if (keystoreFilename != null && keystorePass != null && keystoreType != null) {
            try {
                // prepare the keystore
                final KeyStore keyStore = KeyStoreUtils.getKeyStore(keystoreType.name());
                try (final InputStream keyStoreStream = new FileInputStream(new File(keystoreFilename))) {
                    keyStore.load(keyStoreStream, keystorePass.toCharArray());
                }
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                if (keyPass == null) {
                    keyManagerFactory.init(keyStore, keystorePass.toCharArray());
                } else {
                    keyManagerFactory.init(keyStore, keyPass.toCharArray());
                }
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to load Keystore", e);
            }
        } else {
            keyManagerFactory = null;
        }

        final TrustManagerFactory trustManagerFactory;
        if (truststoreFilename != null && truststorePass != null && truststoreType != null) {
            try {
                // prepare the truststore
                final KeyStore trustStore = KeyStoreUtils.getTrustStore(truststoreType.name());
                try (final InputStream trustStoreStream = new FileInputStream(new File(truststoreFilename))) {
                    trustStore.load(trustStoreStream, truststorePass.toCharArray());
                }
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to load Truststore", e);
            }
        } else {
            trustManagerFactory = null;
        }

        if (keyManagerFactory != null || trustManagerFactory != null) {
            try {
                // initialize the ssl context
                KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
                TrustManager[] trustManagers = trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null;
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers, trustManagers, new SecureRandom());
                sslContext.getDefaultSSLParameters().setNeedClientAuth(true);

                return sslContext;
            } catch (final Exception e) {
                throw new IllegalStateException("Created keystore and truststore but failed to initialize SSLContext", e);
            }
        } else {
            return null;
        }
    }

    public String getKeystoreFilename() {
        return keystoreFilename;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public KeystoreType getKeystoreType() {
        return keystoreType;
    }

    public String getTruststoreFilename() {
        return truststoreFilename;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public KeystoreType getTruststoreType() {
        return truststoreType;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Builder for client configuration.
     */
    public static class Builder {

        private String baseUrl;
        private String contextPath;
        private SSLContext sslContext;
        private String keystoreFilename;
        private String keystorePass;
        private String keyPass;
        private KeystoreType keystoreType;
        private String truststoreFilename;
        private String truststorePass;
        private KeystoreType truststoreType;
        private HostnameVerifier hostnameVerifier;
        private Integer readTimeout;
        private Integer connectTimeout;

        public Builder baseUrl(final String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder contextPath(final String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder sslContext(final SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder keystoreFilename(final String keystoreFilename) {
            this.keystoreFilename = keystoreFilename;
            return this;
        }

        public Builder keystorePassword(final String keystorePass) {
            this.keystorePass = keystorePass;
            return this;
        }

        public Builder keyPassword(final String keyPass) {
            this.keyPass = keyPass;
            return this;
        }

        public Builder keystoreType(final KeystoreType keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        public Builder truststoreFilename(final String truststoreFilename) {
            this.truststoreFilename = truststoreFilename;
            return this;
        }

        public Builder truststorePassword(final String truststorePass) {
            this.truststorePass = truststorePass;
            return this;
        }

        public Builder truststoreType(final KeystoreType truststoreType) {
            this.truststoreType = truststoreType;
            return this;
        }

        public Builder hostnameVerifier(final HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder readTimeout(final Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder connectTimeout(final Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public C2ClientConfig build() {
            return new C2ClientConfig(this);
        }

    }
}