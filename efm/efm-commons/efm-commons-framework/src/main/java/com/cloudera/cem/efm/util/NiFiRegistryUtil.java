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
package com.cloudera.cem.efm.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryClientConfig;
import org.apache.nifi.registry.client.impl.JerseyNiFiRegistryClient;
import org.apache.nifi.registry.security.util.KeystoreType;
import org.springframework.boot.web.server.Ssl;

public class NiFiRegistryUtil {

    public static NiFiRegistryClient createClient(final String baseUrl, final Ssl sslContext) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalStateException("Unable to create NiFi Registry Client because NiFi Registry URL was not provided");
        }

        final boolean secureUrl = baseUrl.startsWith("https");
        if (secureUrl && (sslContext == null  || StringUtils.isBlank(sslContext.getKeyStore()) || StringUtils.isBlank(sslContext.getTrustStore()))) {
            throw new IllegalStateException("Keystore and truststore must be provided when NiFi Registry URL is secure");
        }

        final NiFiRegistryClientConfig.Builder clientConfigBuilder = new NiFiRegistryClientConfig.Builder().baseUrl(baseUrl);

        if (secureUrl) {
            updateClientConfigBuilderTls(clientConfigBuilder, sslContext);
        }

        final NiFiRegistryClientConfig clientConfig = clientConfigBuilder.build();
        return new JerseyNiFiRegistryClient.Builder().config(clientConfig).build();
    }

    private static void updateClientConfigBuilderTls(NiFiRegistryClientConfig.Builder builder, Ssl sslContext) {

        if (sslContext != null) {

            final String keystore = sslContext.getKeyStore();
            final String keystoreType = sslContext.getKeyStoreType();
            final String keystorePasswd = sslContext.getKeyStorePassword();
            final String keyPasswd = sslContext.getKeyPassword();

            final String truststore = sslContext.getTrustStore();
            final String truststoreType = sslContext.getTrustStoreType();
            final String truststorePasswd = sslContext.getTrustStorePassword();

            if (!StringUtils.isBlank(keystore)) {
                builder.keystoreFilename(keystore);
            }
            if (!StringUtils.isBlank(keystoreType)) {
                builder.keystoreType(KeystoreType.valueOf(keystoreType.toUpperCase()));
            }
            if (!StringUtils.isBlank(keystorePasswd)) {
                builder.keystorePassword(keystorePasswd);
            }
            if (!StringUtils.isBlank(keyPasswd)) {
                builder.keyPassword(keyPasswd);
            }
            if (!StringUtils.isBlank(truststore)) {
                builder.truststoreFilename(truststore);
            }
            if (!StringUtils.isBlank(truststoreType)) {
                builder.truststoreType(KeystoreType.valueOf(truststoreType.toUpperCase()));
            }
            if (!StringUtils.isBlank(truststorePasswd)) {
                builder.truststorePassword(truststorePasswd);
            }
        }

    }

}
