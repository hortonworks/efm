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
package com.cloudera.cem.efm.registry;

import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.util.NiFiRegistryUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StandardFDNiFiRegistryClient implements FDNiFiRegistryClient {

    private ServerProperties serverProperties;
    private Map<String, NiFiRegistryClient> clients = new ConcurrentHashMap<>();

    @Autowired
    public StandardFDNiFiRegistryClient(final ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public BucketClient getBucketClient(final String baseUrl, final NiFiUser user) {
        final String identity = getIdentity(user);
        final NiFiRegistryClient registryClient = getRegistryClient(baseUrl);
        final BucketClient bucketClient = identity == null ? registryClient.getBucketClient() : registryClient.getBucketClient(identity);
        return bucketClient;
    }

    @Override
    public FlowSnapshotClient getFlowSnapshotClient(final String baseUrl, final NiFiUser user) {
        final String identity = getIdentity(user);
        final NiFiRegistryClient registryClient = getRegistryClient(baseUrl);
        final FlowSnapshotClient snapshotClient = identity == null ? registryClient.getFlowSnapshotClient() : registryClient.getFlowSnapshotClient(identity);
        return snapshotClient;
    }

    @Override
    public FlowClient getFlowClient(final String baseUrl, final NiFiUser user) {
        final String identity = getIdentity(user);
        final NiFiRegistryClient registryClient = getRegistryClient(baseUrl);
        final FlowClient flowClient = identity == null ? registryClient.getFlowClient() : registryClient.getFlowClient(identity);
        return flowClient;
    }

    private synchronized NiFiRegistryClient getRegistryClient(final String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("The base URL for NiFi Registry cannot be null or blank");
        }

        final NiFiRegistryClient client = clients.computeIfAbsent(baseUrl, (key) -> {
            return NiFiRegistryUtil.createClient(key, serverProperties.getSsl());
        });

        return client;
    }

    private String getIdentity(final NiFiUser user) {
        return (user == null || user.isAnonymous()) ? null : user.getIdentity();
    }
}
