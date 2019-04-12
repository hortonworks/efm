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
package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.annotation.ConditionalOnNiFiRegistry;
import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.model.FlowSummary;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.model.NiFiRegistryInfo;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.registry.FDNiFiRegistryClient;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.NiFiUserUtils;
import com.cloudera.cem.efm.service.config.C2ConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;
import org.apache.nifi.registry.client.NiFiRegistryException;
import org.apache.nifi.registry.flow.VersionedFlow;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The NiFi Registry instance will be determined by taking the agent class of the flow and looking at the flow mappings
 * in C2 server. If none exist then flow designer will use the NiFi Registry URL and bucket id that C2 server is
 * configured with in order to create a new flow.
 */
@Service
@ConditionalOnNiFiRegistry
public class NiFiRegistryFDFlowPublisher extends BaseFDFlowPublisher {

    private final C2FlowService c2FlowService;
    private final C2ConfigurationService c2ConfigurationService;
    private final FDNiFiRegistryClient registryClient;

    @Autowired
    public NiFiRegistryFDFlowPublisher(final C2FlowService c2FlowService,
                                       final C2ConfigurationService c2ConfigurationService,
                                       final FDNiFiRegistryClient registryClient,
                                       final FDFlowManager flowManager) {
        super(flowManager);
        this.c2FlowService = c2FlowService;
        this.c2ConfigurationService = c2ConfigurationService;
        this.registryClient = registryClient;
    }

    @Override
    protected FDVersionInfo publishFlow(final FDFlow flow, final FDFlowPublishMetadata flowPublishMetadata) {
        final String agentClass = flow.getFlowMetadata().getAgentClass();
        final Optional<FlowMapping> c2FlowMapping = c2FlowService.getFlowMapping(agentClass);
        if (c2FlowMapping.isPresent()) {
            return publishWithExistingFlow(flow, flowPublishMetadata, c2FlowMapping.get());
        } else {
            return publishNewFlow(flow, flowPublishMetadata);
        }
    }

    private FDVersionInfo publishWithExistingFlow(final FDFlow fdFlow, final FDFlowPublishMetadata flowPublishMetadata, final FlowMapping c2FlowMapping)
            throws FlowPublisherException {

        final FDFlowMetadata flowMetadata = fdFlow.getFlowMetadata();
        final VersionedProcessGroup flowContents = fdFlow.getFlowContent();

        // Get the C2 FlowSummary for the flow id in the C2 FlowMapping
        final Optional<FlowSummary> c2FlowSummary = c2FlowService.getFlowSummary(c2FlowMapping.getFlowId());
        if (!c2FlowSummary.isPresent()) {
            throw new FlowPublisherException("C2 Flow Summary does not exist for agent class '" + flowMetadata.getAgentClass() + "'");
        }

        final String registryUrl = c2FlowSummary.get().getRegistryUrl();
        final String registryFlowId = c2FlowSummary.get().getRegistryFlowId();

        try {
            // Retrieve the latest version of the flow from registry
            final NiFiUser user = NiFiUserUtils.getNiFiUser();
            final FlowSnapshotClient snapshotClient = registryClient.getFlowSnapshotClient(registryUrl, user);
            final VersionedFlowSnapshotMetadata latestMetadata = snapshotClient.getLatestMetadata(registryFlowId);

            // Create the next version and save to registry
            final VersionedFlowSnapshotMetadata nextSnapshotMetadata = new VersionedFlowSnapshotMetadata();
            nextSnapshotMetadata.setFlowIdentifier(latestMetadata.getFlowIdentifier());
            nextSnapshotMetadata.setBucketIdentifier(latestMetadata.getBucketIdentifier());
            nextSnapshotMetadata.setVersion(latestMetadata.getVersion() + 1);

            if (!StringUtils.isBlank(flowPublishMetadata.getComments())) {
                nextSnapshotMetadata.setComments(flowPublishMetadata.getComments());
            }

            final VersionedFlowSnapshot nextSnapshot = new VersionedFlowSnapshot();
            nextSnapshot.setSnapshotMetadata(nextSnapshotMetadata);
            nextSnapshot.setFlowContents(flowContents);

            final VersionedFlowSnapshot createdSnapshot = snapshotClient.create(nextSnapshot);

            // Create the version info to return in the response
            final FDVersionInfo versionInfo = new FDVersionInfo();
            versionInfo.setRegistryUrl(registryUrl);
            versionInfo.setRegistryBucketId(createdSnapshot.getSnapshotMetadata().getBucketIdentifier());
            versionInfo.setRegistryFlowId(createdSnapshot.getSnapshotMetadata().getFlowIdentifier());
            versionInfo.setRegistryVersion(createdSnapshot.getSnapshotMetadata().getVersion());
            versionInfo.setLastPublished(createdSnapshot.getSnapshotMetadata().getTimestamp());
            versionInfo.setLastPublishedBy(user.getIdentity());
            versionInfo.setDirty(false);
            return versionInfo;

        } catch (NiFiRegistryException | IOException e) {
            throw new FlowPublisherException("Unable to publish flow to NiFi Registry due to: " + e.getMessage(), e);
        }
    }

    public FDVersionInfo publishNewFlow(final FDFlow fdFlow, final FDFlowPublishMetadata flowPublishMetadata) throws FlowPublisherException {
        final NiFiUser user = NiFiUserUtils.getNiFiUser();
        final FDFlowMetadata flowMetadata = fdFlow.getFlowMetadata();
        final VersionedProcessGroup flowContents = fdFlow.getFlowContent();

        // Determine the NiFi Registry that the C2 server is configured with and create a client for it
        final NiFiRegistryInfo niFiRegistryInfo = c2ConfigurationService.getNiFiRegistryInfo();
        final String registryBaseUrl = niFiRegistryInfo.getBaseUrl();

        try {
            // If the C2 server only has a bucket name, then we need to determine the bucket id
            String bucketId = niFiRegistryInfo.getBucketId();
            if (StringUtils.isBlank(bucketId)) {
                final String bucketName = niFiRegistryInfo.getBucketName();
                final BucketClient bucketClient = registryClient.getBucketClient(registryBaseUrl, user);
                final List<Bucket> buckets = bucketClient.getAll();

                final Bucket matchingBucket = buckets.stream()
                        .filter(b -> b.getName().equals(bucketName))
                        .findFirst()
                        .orElseThrow(() -> new FlowPublisherException(
                                "Could not find NiFi Registry bucket with name '" + bucketName + "'"));

                bucketId = matchingBucket.getIdentifier();
            }

            // Create a new VersionedFlow for the agent class
            final VersionedFlow versionedFlow = new VersionedFlow();
            versionedFlow.setName(flowMetadata.getAgentClass());
            versionedFlow.setDescription("Created by MiNiFi C2 Flow Designer");
            versionedFlow.setBucketIdentifier(bucketId);

            // Create the new flow in NiFi Registry
            final FlowClient flowClient = registryClient.getFlowClient(registryBaseUrl, user);
            final VersionedFlow createdVersionedFlow = flowClient.create(versionedFlow);

            // Create the first snapshot/version in NiFi Registry
            final VersionedFlowSnapshotMetadata snapshotMetadata = new VersionedFlowSnapshotMetadata();
            snapshotMetadata.setBucketIdentifier(bucketId);
            snapshotMetadata.setFlowIdentifier(createdVersionedFlow.getIdentifier());
            snapshotMetadata.setVersion(1);

            if (!StringUtils.isBlank(flowPublishMetadata.getComments())) {
                snapshotMetadata.setComments(flowPublishMetadata.getComments());
            }

            final VersionedFlowSnapshot snapshot = new VersionedFlowSnapshot();
            snapshot.setSnapshotMetadata(snapshotMetadata);
            snapshot.setFlowContents(flowContents);

            final FlowSnapshotClient snapshotClient = registryClient.getFlowSnapshotClient(registryBaseUrl, user);
            final VersionedFlowSnapshot createdSnapshot = snapshotClient.create(snapshot);

            // Tell C2 about the flow which converts it to YAML and stores it in C2's flow table
            final FlowSnapshot c2FlowSnapshot = new FlowSnapshot();
            c2FlowSnapshot.setFlowUri(new FlowUri(registryBaseUrl, snapshotMetadata.getBucketIdentifier(), snapshotMetadata.getFlowIdentifier()));
            c2FlowSnapshot.setVersionedFlowSnapshot(snapshot);

            final FlowSummary createdC2Flow = c2FlowService.createFlow(c2FlowSnapshot);

            // Create a flow mapping in C2 from the agent class to the converted flow above
            final FlowMapping c2FlowMapping = new FlowMapping();
            c2FlowMapping.setAgentClass(fdFlow.getFlowMetadata().getAgentClass());
            c2FlowMapping.setFlowId(createdC2Flow.getId());
            c2FlowService.createFlowMapping(c2FlowMapping);

            // Create the version info to return in the response
            final FDVersionInfo versionInfo = new FDVersionInfo();
            versionInfo.setRegistryUrl(registryBaseUrl);
            versionInfo.setRegistryBucketId(createdSnapshot.getSnapshotMetadata().getBucketIdentifier());
            versionInfo.setRegistryFlowId(createdSnapshot.getSnapshotMetadata().getFlowIdentifier());
            versionInfo.setRegistryVersion(createdSnapshot.getSnapshotMetadata().getVersion());
            versionInfo.setLastPublished(createdSnapshot.getSnapshotMetadata().getTimestamp());
            versionInfo.setLastPublishedBy(user.getIdentity());
            versionInfo.setDirty(false);
            return versionInfo;

        } catch (NiFiRegistryException | IOException e) {
            throw new FlowPublisherException("Unable to publish flow to NiFi Registry due to: " + e.getMessage(), e);
        }
    }

}
