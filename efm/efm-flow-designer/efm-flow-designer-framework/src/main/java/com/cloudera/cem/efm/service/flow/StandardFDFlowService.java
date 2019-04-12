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
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.service.agent.ELSpecificationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDFlowService implements FDFlowService {

    private final FDFlowManager flowManager;
    private final FDFlowPublisher flowPublisher;
    private final ELSpecificationProvider elSpecificationProvider;

    @Autowired
    public StandardFDFlowService(final FDFlowManager flowManager,
                                 final FDFlowPublisher flowPublisher,
                                 final ELSpecificationProvider elSpecificationProvider) {
        this.flowManager = flowManager;
        this.flowPublisher = flowPublisher;
        this.elSpecificationProvider = elSpecificationProvider;
    }

    @Override
    public FDFlow getFlow(final String fdFlowId) {
        final Optional<FDFlow> flow = flowManager.getFlow(fdFlowId);
        if (!flow.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the given id");
        }
        return flow.get();
    }

    @Override
    public List<FDFlowEvent> getFlowEvents(final String fdFlowId) {
        return flowManager.getFlowEvents(fdFlowId);
    }

    @Override
    public FDFlowMetadata getFlowMetadata(final String fdFlowId) {
        final Optional<FDFlowMetadata> flowMetadata = flowManager.getFlowMetadata(fdFlowId);
        if (!flowMetadata.isPresent()) {
            throw new ResourceNotFoundException("No flow exists with the given id");
        }
        return flowMetadata.get();
    }

    @Override
    public FDFlowMetadata createFlow(final String agentClass, final NiFiUser user) {
        return flowManager.createFlow(agentClass, user);
    }

    @Override
    public List<FDFlowMetadata> getAvailableFlows() {
        return flowManager.getAvailableFlows();
    }

    @Override
    public List<FDFlowSummary> getFlowSummaries() {
        return flowManager.getFlowSummaries();
    }

    @Override
    public FDVersionInfo publishFlow(final String fdFlowId, final FDFlowPublishMetadata publishMetadata)
            throws FlowPublisherException {
        return flowPublisher.publish(fdFlowId, publishMetadata);
    }

    @Override
    public FDVersionInfo revertFlowToLastPublishedState(final String fdFlowId) {
        // determine the last publish event
        final Optional<FDFlowEvent> lastPublishEvent = flowManager.getLatestPublishFlowEvent(fdFlowId);
        if (!lastPublishEvent.isPresent()) {
            throw new IllegalStateException("Cannot revert to last published event because flow has never been published");
        }

        // revert the db to the last published event
        flowManager.revertToFlowRevision(fdFlowId, lastPublishEvent.get().getFlowRevision());

        // retrieve the flow which should pull fresh from the db and populate the cache
        return getFdVersionInfo(fdFlowId);
    }

    @Override
    public FDVersionInfo revertFlowToFlowRevision(final String fdFlowId, final BigInteger flowRevision) {
        // revert the db to the specified event
        flowManager.revertToFlowRevision(fdFlowId, flowRevision);

        // retrieve the flow which should pull fresh from the db and populate the cache
        return getFdVersionInfo(fdFlowId);
    }

    @Override
    public FDVersionInfo getFlowVersionInfo(final String fdFlowId) {
        return getFdVersionInfo(fdFlowId);
    }

    private FDVersionInfo getFdVersionInfo(String fdFlowId) {
        final Optional<FDFlow> flow = flowManager.getFlow(fdFlowId);
        if (!flow.isPresent()) {
            throw new ResourceNotFoundException("A flow does not exist with the specified id");
        }

        return flow.get().getVersionInfo();
    }

    @Override
    public ELSpecification getELSpecification(String fdFlowId) {
        final FDFlowMetadata flowMetadata = getFlowMetadata(fdFlowId);

        final Optional<ELSpecification> elSpecification = elSpecificationProvider.getELSpecification(flowMetadata.getAgentClass());
        if (!elSpecification.isPresent()) {
            throw new ResourceNotFoundException("No EL specification found for the given agent class");
        }

        return elSpecification.get();
    }

    @Override
    public FDFlowMetadata deleteFlow(final String fdFlowId) {
        return flowManager.deleteFlow(fdFlowId);
    }
}
