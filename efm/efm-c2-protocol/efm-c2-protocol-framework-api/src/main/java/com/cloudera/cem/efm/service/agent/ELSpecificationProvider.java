package com.cloudera.cem.efm.service.agent;

import com.cloudera.cem.efm.model.ELSpecification;

import java.util.Optional;

/**
 * Service for obtaining the expression-language specification for a given agent class.
 */
public interface ELSpecificationProvider {

    /**
     * @param agentClass the agent class to obtain the specification for
     * @return an Optional containing the specification, or empty if one does not exist
     */
    Optional<ELSpecification> getELSpecification(String agentClass);

}
