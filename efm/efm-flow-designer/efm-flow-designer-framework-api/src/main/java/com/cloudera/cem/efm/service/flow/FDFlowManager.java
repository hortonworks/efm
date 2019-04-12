package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.security.NiFiUser;
import org.apache.nifi.registry.flow.VersionedProcessGroup;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * Manages the storage and retrieval of all flows known to the flow designer.
 */
public interface FDFlowManager {

    // ------------ Flow Methods ------------ //

    /**
     * Creates a flow designer flow for the given agent-class.
     *
     * This results in the creation of a flow id and an empty root process group with an id.
     *
     * @param agentClass the agent class to create a flow for
     * @param user the user creating the flow
     * @return the metadata for the created flow
     */
    FDFlowMetadata createFlow(String agentClass, NiFiUser user);

    /**
     * Retrieves the flow with the given id.
     *
     * @param flowId the flow id
     * @return the flow or an empty optional
     */
    Optional<FDFlow> getFlow(String flowId);

    /**
     * Retrieves the metadata for the given flow id.
     *
     * @param flowId the flow id
     * @return the flow metadata or an empty optional
     */
    Optional<FDFlowMetadata> getFlowMetadata(String flowId);

    /**
     * Retrieves the metadata for all flow designer flows.
     *
     * @return the list of metadata for all flows
     */
    List<FDFlowMetadata> getAvailableFlows();

    /**
     * Retrieves the flow summaries for all flow designer flows.
     *
     * @return the list of flow summaries for all flows
     */
    List<FDFlowSummary> getFlowSummaries();

    /**
     * Deletes the flow with the given id, including all events for the flow.
     *
     * @param flowId the id of the flow to delete
     * @return the metadata about the deleted flow
     */
    FDFlowMetadata deleteFlow(String flowId);


    // ------------ Flow Event Methods ------------ //

    /**
     * Retrieves the flow events for the given flow id.
     *
     * @param flowId the flow id
     * @return the list of flow events
     */
    List<FDFlowEvent> getFlowEvents(String flowId);

    /**
     * Retrieves the latest event for the given flow id.
     *
     * @param flowId the flow id
     * @return the latest event or empty optional if no events exist
     */
    Optional<FDFlowEvent> getLatestFlowEvent(String flowId);

    /**
     * Retrieves the latest publish event for the given flow id.
     *
     * @param flowId the flow id
     * @return the latest publish event, or empty optional if no publish event exists
     */
    Optional<FDFlowEvent> getLatestPublishFlowEvent(String flowId);

    /**
     * Retrieves the flow event with the given event id.
     *
     * @param flowEventId the id of the flow event
     * @return the flow event or empty optional if the event does not exist
     */
    Optional<FDFlowEvent> getFlowEvent(String flowEventId);

    /**
     * Creates a flow event for the given flow.
     *
     * @param flowEvent the flow event to create
     * @param flowContent the flow at the time of the event
     * @param user the user that triggered the event
     * @return the created flow event
     */
    FDFlowEvent addFlowEvent(FDFlowEvent flowEvent, VersionedProcessGroup flowContent, NiFiUser user);

    /**
     * Deletes the flow event with the given id.
     *
     * @param flowEventId the id of the event to delete
     * @return the deleted event
     */
    FDFlowEvent deleteFlowEvent(String flowEventId);

    /**
     * Deletes all flow events for the given flow id, except for the publish events.
     *
     * @param flowId the id of the flow
     */
    void retainPublishEvents(String flowId);

    /**
     * Reverts the given flow to the specified revision.
     *
     * @param flowId the id of the flow
     * @param flowRevision the flow revision to revert to
     */
    void revertToFlowRevision(String flowId, BigInteger flowRevision);

}
