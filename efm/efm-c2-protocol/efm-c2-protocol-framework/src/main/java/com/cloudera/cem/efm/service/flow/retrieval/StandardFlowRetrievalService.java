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
package com.cloudera.cem.efm.service.flow.retrieval;

import com.cloudera.cem.efm.annotation.ConditionalOnNiFiRegistry;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.service.flow.client.C2NiFiRegistryClient;
import org.apache.commons.lang3.Validate;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Standard implementation of FlowRetrievalService.
 *
 * NOTE: Currently there is an assumption that a given C2 server only interacts with a single NiFi Registry instance, so
 * even though the FlowMapping instances coming from the FlowMapper have registry URL, it is assumed for now that this
 * URL will always be the same and thus we can avoid maintaining multiple registry clients for now.
 */
@Service
@ConditionalOnNiFiRegistry
public class StandardFlowRetrievalService implements FlowRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardFlowRetrievalService.class);

    private final C2NiFiRegistryClient clientFactory;

    @Autowired
    public StandardFlowRetrievalService(final C2NiFiRegistryClient clientFactory) {
        this.clientFactory = clientFactory;
        Validate.notNull(this.clientFactory);
    }

    @Override
    public List<VersionedFlowSnapshotMetadata> getVersions(final FlowUri flowUri)
            throws IOException, FlowRetrievalException {
        if (flowUri == null) {
            throw new IllegalArgumentException("FlowUri cannot be null");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting flow versions for {}", new Object[] {flowUri});
        }

        final NiFiRegistryClient client = clientFactory.getClient();
        try {
            return client.getFlowSnapshotClient().getSnapshotMetadata(flowUri.getBucketId(), flowUri.getFlowId());
        } catch (IOException ioe) {
            throw ioe;
        } catch (NiFiRegistryException nre) {
            throw new FlowRetrievalException("Error retrieving flow versions for " + flowUri + ": " + nre.getMessage(), nre);
        }
    }

    @Override
    public VersionedFlowSnapshot getFlow(final FlowUri flowUri, final int version)
            throws IOException, FlowRetrievalException {

        if (flowUri == null) {
            throw new IllegalArgumentException("FlowUri cannot be null");
        }

        if (version < 1) {
            throw new IllegalArgumentException("Version must be greater than or equal to 1");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting flow for {} with version {}", new Object[] {flowUri, Integer.valueOf(version)});
        }

        final NiFiRegistryClient client = clientFactory.getClient();
        try {
            return client.getFlowSnapshotClient().get(flowUri.getBucketId(), flowUri.getFlowId(), version);
        } catch (IOException ioe) {
            throw ioe;
        } catch (NiFiRegistryException nre) {
            throw new FlowRetrievalException("Error retrieving flow for " + flowUri + ": " + nre.getMessage(), nre);
        }
    }

    @Override
    public VersionedFlowSnapshot getLatestFlow(final FlowUri flowUri)
            throws IOException, FlowRetrievalException {

        if (flowUri == null) {
            throw new IllegalArgumentException("FlowUri cannot be null");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting latest flow for {}", new Object[] {flowUri});
        }

        final NiFiRegistryClient client = clientFactory.getClient();
        try {
            return client.getFlowSnapshotClient().getLatest(flowUri.getBucketId(), flowUri.getFlowId());
        } catch (IOException ioe) {
            throw ioe;
        } catch (NiFiRegistryException nre) {
            throw new FlowRetrievalException("Error retrieving latest flow for " + flowUri + ": " + nre.getMessage(), nre);
        }
    }


}
