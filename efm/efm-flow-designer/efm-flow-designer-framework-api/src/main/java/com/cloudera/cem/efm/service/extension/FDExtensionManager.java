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
package com.cloudera.cem.efm.service.extension;

import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.types.FDComponentType;
import org.apache.nifi.registry.flow.Bundle;

import java.util.Optional;
import java.util.Set;

/**
 * Extension manager for a given agent class. p
 *
 * Provides information about available component types from the agent manifest, and creates instances of requested component types.
 */
public interface FDExtensionManager {

    // ----- Processors ------

    /**
     * @return the types of processors available to the agent class
     */
    Set<FDComponentType> getProcessorTypes();

    /**
     * Retrieves a copy of the manifest processor with the given type and bundle.
     *
     * @param type the type of the processor
     * @param bundle the bundle of the processor
     * @return the manifest processor with the given type and bundle
     */
    ProcessorDefinition getProcessorDefinition(String type, Bundle bundle);

    /**
     * Creates a processor instance of the specific type and bundle.
     *
     * @param type the type of the processor
     * @param bundle the bundle of the processor
     * @return the FDProcessor instance containing the component definition and the initial component
     */
    FDProcessor createProcessor(String type, Bundle bundle);

    /**
     * Retrieves the property descriptor with the given name from the processor specified by the given type and bundle.
     *
     * @param type the type of the processor
     * @param bundle the bundle of the processor
     * @param propertyName the name of the property
     * @return an optional containing the property descriptor, or empty if doesn't exist
     */
    Optional<PropertyDescriptor> getProcessorPropertyDescriptor(String type, Bundle bundle, String propertyName);

    /**
     * Creates a dynamic property descriptor with the given name for the processor specified by the given type and bundle.
     *
     * @param type the type of the processor
     * @param bundle the bundle of the processor
     * @param propertyName the name of the property
     * @return the property descriptor
     * @throws IllegalStateException if called for a processor that does not support dynamic properties
     */
    PropertyDescriptor createDynamicProcessorPropertyDescriptor(String type, Bundle bundle, String propertyName);

    // ----- Controller Services ------

    /**
     * @return the types of controller services available to the agent class
     */
    Set<FDComponentType> getControllerServiceTypes();

    /**
     * Retrieves a copy of the controller service definition from the manifest for the given type and bundle.
     *
     * @param type the type of the controller service
     * @param bundle the bundle of the controller service
     * @return the manifest controller service with the given type and bundle
     */
    ControllerServiceDefinition getControllerServiceDefinition(String type, Bundle bundle);

    /**
     * Creates a controller service instance of the specific type and bundle.
     *
     * @param type the type of the controller service
     * @param bundle the bundle of the controller service
     * @return the FDControllerService instance
     */
    FDControllerService createControllerService(String type, Bundle bundle);

    /**
     * Retrieves the property descriptor with the given name from the controller service specified by the given type and bundle.
     *
     * @param type the type of the controller service
     * @param bundle the bundle of the controller service
     * @param propertyName the name of the property
     * @return an optional containing the property descriptor, or empty if doesn't exist
     */
    Optional<PropertyDescriptor> getControllerServicePropertyDescriptor(String type, Bundle bundle, String propertyName);

    /**
     * Creates a dynamic property descriptor with the given name for the controller service specified by the given type and bundle.
     *
     * @param type the type of the controller service
     * @param bundle the bundle of the controller service
     * @param propertyName the name of the property
     * @return the property descriptor
     * @throws IllegalStateException if called for a controller service that does not support dynamic properties
     */
    PropertyDescriptor createDynamicControllerServicePropertyDescriptor(String type, Bundle bundle, String propertyName);

}
