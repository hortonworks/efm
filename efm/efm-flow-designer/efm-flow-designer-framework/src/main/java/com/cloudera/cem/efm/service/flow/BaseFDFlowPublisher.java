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

import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.NiFiUserUtils;

import java.util.Optional;

/**
 * Base class for implementations of FDFlowPublisher.
 */
public abstract class BaseFDFlowPublisher implements FDFlowPublisher {

    protected FDFlowManager flowManager;

    public BaseFDFlowPublisher(final FDFlowManager flowManager) {
        this.flowManager = flowManager;
    }

    @Override
    public FDVersionInfo publish(final String flowId, final FDFlowPublishMetadata flowPublishMetadata) throws FlowPublisherException {
        final Optional<FDFlow> flowOptional = flowManager.getFlow(flowId);
        if (!flowOptional.isPresent()) {
            throw new ResourceNotFoundException("A flow does not exist with the given id");
        }

        final FDFlow flow = flowOptional.get();
        final String agentClass = flow.getFlowMetadata().getAgentClass();

        // Perform the actual publishing...
        final FDVersionInfo versionInfo = publishFlow(flow, flowPublishMetadata);

        final NiFiUser user = NiFiUserUtils.getNiFiUser();

        // Record an event in the DB to capture the act of publishing the flow
        final FDFlowEvent event = new FDFlowEvent();
        event.setEventType(FDFlowEventType.FLOW_PUBLISHED);
        event.setEventDescription(FDFlowEventType.FLOW_PUBLISHED.getDescription());
        event.setComponentId(flow.getFlowMetadata().getRootProcessGroupIdentifier());
        event.setFlowIdentifier(flowId);
        event.setRegistryUrl(versionInfo.getRegistryUrl());
        event.setRegistryBucketId(versionInfo.getRegistryBucketId());
        event.setRegistryFlowId(versionInfo.getRegistryFlowId());
        event.setRegistryVersion(versionInfo.getRegistryVersion());
        event.setLastPublished(versionInfo.getLastPublished());
        event.setLastPublishedUserIdentity(user.getIdentity());

        flowManager.addFlowEvent(event, flow.getFlowContent(), user);

        // Remove previous flow events except the latest event
        flowManager.retainPublishEvents(flowId);

        return versionInfo;
    }

    /**
     * Sub-classes can provide the actual details of where to publish.
     *
     * @param flow the flow to publish
     * @param flowPublishMetadata the metadata about publishing
     * @return the version info from publishing
     */
    protected abstract FDVersionInfo publishFlow(final FDFlow flow, final FDFlowPublishMetadata flowPublishMetadata);

}
