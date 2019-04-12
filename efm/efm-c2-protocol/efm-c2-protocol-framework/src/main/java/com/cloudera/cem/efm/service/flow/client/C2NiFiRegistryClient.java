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
package com.cloudera.cem.efm.service.flow.client;

import com.cloudera.cem.efm.annotation.ConditionalOnNiFiRegistry;
import com.cloudera.cem.efm.properties.NifiRegistryProperties;
import com.cloudera.cem.efm.util.NiFiRegistryUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Encapsulates a NiFiRegistryClient with the url and bucket id or name of the configured NiFi Registry instance.
 */
@Component
@ConditionalOnNiFiRegistry
public class C2NiFiRegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(C2NiFiRegistryClient.class);

    private final String registryUrl;
    private final String registryBucketId;
    private final String registryBucketName;
    private final NiFiRegistryClient client;


    @Autowired
    public C2NiFiRegistryClient(
            final NifiRegistryProperties nifiRegistryProperties,
            final ServerProperties serverProperties) {
        this (nifiRegistryProperties, serverProperties, null);
    }

    /** Allows overriding the default Client, primarily for testing purposes */
    C2NiFiRegistryClient(
            final NifiRegistryProperties nifiRegistryProperties,
            final ServerProperties serverProperties,
            final NiFiRegistryClient client) {

        final String rawBucketId = nifiRegistryProperties.getBucketId();
        final String rawBucketName = nifiRegistryProperties.getBucketName();

        registryBucketId = StringUtils.isBlank(rawBucketId) ? null : rawBucketId;
        registryBucketName = StringUtils.isBlank(rawBucketName) ? null : rawBucketName;

        if (registryBucketId == null && registryBucketName == null) {
            throw new IllegalStateException("For using NiFi Registry, you must set either bucket name or bucket id. Both properties are blank.");
        } else if (registryBucketId != null && registryBucketName != null) {
            throw new IllegalStateException("Properties for NiFi Registry bucket id and name are both set. Exactly one should be used.");
        }

        final Ssl sslContext = serverProperties != null ? serverProperties.getSsl() : null;

        this.registryUrl = nifiRegistryProperties.getUrl();
        this.client = client == null ? NiFiRegistryUtil.createClient(this.registryUrl, sslContext) : client;
        Validate.notNull(this.client);
    }

    public String getNiFiRegistryUrl() {
        return registryUrl;
    }

    public Optional<String> getNiFiRegistryBucketId() throws NiFiRegistryException, IOException {
        return Optional.ofNullable(registryBucketId);
    }

    public Optional<String> getNiFiRegistryBucketName() {
        return Optional.ofNullable(registryBucketName);
    }

    /**
     *  Return configured bucketId if set, and if bucketId
     *  is not set, uses a NiFi Registry client to lookup
     *  the bucketId for the configured name.
     *
     *  Returns empty optional if lookup by name results
     *  in no bucket.
     */
    public Optional<String> resolveNiFiRegistryBucketId() throws NiFiRegistryException, IOException {
        if (registryBucketId != null) {
            logger.debug("Using configured bucketId '{}'", registryBucketId);
            return Optional.of(registryBucketId);
        }
        Optional<String> bucketIdForName = getBucketIdByName();
        if (bucketIdForName.isPresent()) {
            logger.debug("Resolved bucketId={} for bucketName={}", bucketIdForName.get(), registryBucketName);
        } else {
            logger.debug("Could not determine bucketId. Bucket named '{}' not found.", registryBucketName);
        }
        return bucketIdForName;
    }

    public NiFiRegistryClient getClient() {
        return client;
    }

    private Optional<String> getBucketIdByName() throws NiFiRegistryException, IOException {
        if (registryBucketName == null) {
            return Optional.empty();
        }
        return client.getBucketClient().getAll().stream()
                .filter(b -> registryBucketName.equals(b.getName()))
                .map(Bucket::getIdentifier)
                .findFirst();
    }

}
