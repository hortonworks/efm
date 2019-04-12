package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;

/**
 * Service that publishes flows from flow designer to a NiFi Registry instance.
 */
public interface FDFlowPublisher {

    /**
     * Publishes the flow with the given id to the appropriate NiFi Registry instance.
     *
     * @param flowId the id of the flow to publish
     * @param flowPublishMetadata the metadata about the publish action
     * @return the version info for the publish event
     * @throws FlowPublisherException if an error occurs publishing
     */
    FDVersionInfo publish(String flowId, FDFlowPublishMetadata flowPublishMetadata) throws FlowPublisherException;

}
