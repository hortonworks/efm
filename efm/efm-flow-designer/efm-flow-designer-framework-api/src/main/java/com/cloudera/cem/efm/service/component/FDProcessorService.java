package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import org.apache.nifi.registry.flow.VersionedProcessor;

/**
 * Interface for a service that performs CRUD operations for processors.
 */
public interface FDProcessorService extends FDComponentService<VersionedProcessor, FDProcessor> {

    /**
     * Gets the available processor types for the given flow.
     *
     * @param flowId the flow id
     * @return the available processor types
     */
    FDComponentTypes getProcessorTypes(String flowId);

    /**
     * Gets the property descriptor with the given id, or creates one if it does not exist AND the processor supports dynamic properties.
     *
     * @param flowId the flow id
     * @param processorId the processor id
     * @param propertyName the propertyId
     * @return the property descriptor
     */
    FDPropertyDescriptor getPropertyDescriptor(String flowId, String processorId, String propertyName);

    /**
     * Get the processor definition for the given agent class and versioned processor.
     *
     * @param agentClass the agent class of the flow
     * @param componentConfig the versioned processor config
     * @return the processor definition
     */
    ProcessorDefinition getComponentDefinition(String agentClass, VersionedProcessor componentConfig);
}
