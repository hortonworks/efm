package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.model.component.FDComponent;
import org.apache.nifi.registry.flow.VersionedComponent;

/**
 * Base interface for a service that provides CRUD operations for a type of component.
 *
 * @param <VC> the type of VersionedComponent this service operates on
 * @param <FDC> the type of FDComponent this service operates on
 */
public interface FDComponentService<VC extends VersionedComponent, FDC extends FDComponent<VC>> {

    /**
     * Creates a component in the given flow and process group.
     *
     * @param flowId the flow id
     * @param processGroupId the process group id
     * @param requestComponentConfig the requested component configuration
     * @return the created component
     */
    FDC create(String flowId, String processGroupId, VC requestComponentConfig);

    /**
     * Retrieves the component with the given id in the given flow.
     *
     * @param flowId the flow id
     * @param componentId the component id
     * @return the requested component
     */
    FDC get(String flowId, String componentId);

    /**
     * Updates the component in the given flow based on the provided configuration.
     *
     * @param flowId the flow id
     * @param requestComponentConfig the updates to apply
     * @return the updated component
     */
    FDC update(String flowId, VC requestComponentConfig);

    /**
     * Deletes the component in the given flow with the given component id.
     *
     * @param flowId the flow id
     * @param componentId the component id
     */
    void delete(String flowId, String componentId);

}
