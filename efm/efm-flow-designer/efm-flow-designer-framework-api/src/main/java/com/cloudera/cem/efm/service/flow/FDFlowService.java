package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.security.NiFiUser;

import java.math.BigInteger;
import java.util.List;

/**
 * Service for flow operations.
 */
public interface FDFlowService {

    /**
     * Retrieves the flow with the given id.
     *
     * @param fdFlowId the flow id
     * @return the flow with the given id
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDFlow getFlow(String fdFlowId);

    /**
     * Retrieves the flow events for the flow with the given id.
     *
     * @param flowId the flow id
     * @return the list of events for the flow
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    List<FDFlowEvent> getFlowEvents(String flowId);

    /**
     * Retrieves the flow metadata for the given flow id.
     *
     * @param fdFlowId the flow id
     * @return the flow metadata
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDFlowMetadata getFlowMetadata(String fdFlowId);

    /**
     * Creates a flow for the given agent class.
     *
     * @param agentClass the agent class
     * @param user the user at the time of creating the flow
     * @return the metadata about the created flow
     */
    FDFlowMetadata createFlow(String agentClass, NiFiUser user);

    /**
     * Retrieves the metadata about the flows that exist.
     *
     * @return the list of flow metadata
     */
    List<FDFlowMetadata> getAvailableFlows();

    /**
     * Retrieves the list of flow summaries.
     *
     * @return flow summaries
     */
    List<FDFlowSummary> getFlowSummaries();

    /**
     * Publishes the flow with the given id.
     *
     * @param fdFlowId the flow id
     * @param publishMetadata the metadata for the publish event
     * @return the resulting version info after publishing
     * @throws FlowPublisherException if an error occurs publishing
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDVersionInfo publishFlow(String fdFlowId, FDFlowPublishMetadata publishMetadata)
            throws FlowPublisherException;

    /**
     * Reverts the flow to its last published stated, reverting any local changes that have been made.
     *
     * @param fdFlowId the flow id
     * @return the version info after reverting
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDVersionInfo revertFlowToLastPublishedState(String fdFlowId);

    /**
     * Reverts the flow to the specific revision, reverting any changes that have been made since the revision.
     *
     * @param fdFlowId the flow id
     * @param flowRevision the revision to revert to
     * @return the version info after reverting
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDVersionInfo revertFlowToFlowRevision(String fdFlowId, BigInteger flowRevision);

    /**
     * Retrieves the version info for the flow with the given id.
     *
     * @param fdFlowId the flow id
     * @return the version info
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDVersionInfo getFlowVersionInfo(String fdFlowId);

    /**
     * Retrieves the expression language specification for the given flow.
     *
     * @param fdFlowId the flow id
     * @return the EL spec
     */
    ELSpecification getELSpecification(String fdFlowId);

    /**
     * Deletes the flow with the given id.
     *
     * @param fdFlowId the flow id
     * @return the metadata of the deleted flow
     * @throws com.cloudera.cem.efm.exception.ResourceNotFoundException if the flow does not exist
     */
    FDFlowMetadata deleteFlow(String fdFlowId);

}
