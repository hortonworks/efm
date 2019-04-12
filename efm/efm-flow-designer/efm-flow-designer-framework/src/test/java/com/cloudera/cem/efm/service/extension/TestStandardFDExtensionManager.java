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

import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.extension.Bundle;
import com.cloudera.cem.efm.model.extension.ComponentManifest;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.DefinedType;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.extension.Relationship;
import com.cloudera.cem.efm.model.types.FDComponentType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestStandardFDExtensionManager {

    private Bundle httpBundle;
    private Bundle systemBundle;
    private ProcessorDefinition invokeHttpProcessor;
    private ProcessorDefinition dynamicPropsProcessor;
    private ControllerServiceDefinition sslContextService;
    private ControllerServiceDefinition dynamicPropsService;
    private AgentManifest agentManifest;

    private FDExtensionManager extensionManager;

    @Before
    public void setup() {
        agentManifest = new AgentManifest();
        agentManifest.setIdentifier("12345");

        // Create InvokeHttp processor

        invokeHttpProcessor = new ProcessorDefinition();
        invokeHttpProcessor.setType("org.apache.nifi.minifi.processors.InvokeHTTP");

        final DefinedType sslContextServiceType = new DefinedType();
        sslContextServiceType.setType("org.apache.nifi.minifi.controllers.SSLContextService");
        sslContextServiceType.setGroup("org.apache.nifi.minifi");
        sslContextServiceType.setArtifact("minifi-system");

        final PropertyDescriptor sslContextServiceProperty = new PropertyDescriptor();
        sslContextServiceProperty.setName("SSL Context Service");
        sslContextServiceProperty.setDescription("The SSL Context Service used to provide client certificate information for TLS/SSL (https) connections.");
        sslContextServiceProperty.setRequired(false);
        sslContextServiceProperty.setTypeProvidedByValue(sslContextServiceType);

        final LinkedHashMap<String,PropertyDescriptor> invokeHttpProperties = new LinkedHashMap<>();
        invokeHttpProperties.put(sslContextServiceProperty.getName(), sslContextServiceProperty);

        invokeHttpProcessor.setPropertyDescriptors(invokeHttpProperties);
        invokeHttpProcessor.setSupportsDynamicProperties(false);

        final Relationship relSuccess = new Relationship();
        relSuccess.setName("Success");
        relSuccess.setDescription("Successful");

        final Relationship relFailure = new Relationship();
        relFailure.setName("Failure");
        relFailure.setDescription("Failure");

        final List<Relationship> supportedRelationships = new ArrayList<>();
        supportedRelationships.add(relSuccess);
        supportedRelationships.add(relFailure);

        invokeHttpProcessor.setSupportsDynamicRelationships(true);
        invokeHttpProcessor.setSupportedRelationships(supportedRelationships);

        // Create a ComponentManifest for the http bundle and add the processor to the manifest

        final ComponentManifest httpBundleComponentManifest = new ComponentManifest();
        httpBundleComponentManifest.setProcessors(Collections.singletonList(invokeHttpProcessor));

        httpBundle = new Bundle("org.apache.nifi.minifi", "minifi-http-curl", "0.6.0");
        httpBundle.setComponentManifest(httpBundleComponentManifest);

        // Create SSL Context Service

        sslContextService = new ControllerServiceDefinition();
        sslContextService.setType(sslContextServiceType.getType());
        sslContextService.setGroup(sslContextServiceType.getGroup());
        sslContextService.setArtifact(sslContextServiceType.getArtifact());
        sslContextService.setVersion("0.6.0");

        final PropertyDescriptor clientCert = new PropertyDescriptor();
        clientCert.setName("Client Certificate");
        clientCert.setDescription("The client certificate.");
        clientCert.setRequired(false);

        final LinkedHashMap<String,PropertyDescriptor> sslContextServiceProperties = new LinkedHashMap<>();
        sslContextServiceProperties.put(clientCert.getName(), clientCert);

        sslContextService.setPropertyDescriptors(sslContextServiceProperties);

        // Create the dynamic properties processor

        dynamicPropsProcessor = new ProcessorDefinition();
        dynamicPropsProcessor.setType("org.apache.nifi.minifi.processors.DynamicProperties");
        dynamicPropsProcessor.setSupportsDynamicProperties(true);

        // Create the dynamic properties service

        dynamicPropsService = new ControllerServiceDefinition();
        dynamicPropsService.setType("org.apache.nifi.minifi.processors.DynamicProperties");
        dynamicPropsService.setSupportsDynamicProperties(true);

        // Create a ComponentManifest for the system bundle and add the controller service to the manifest

        final ComponentManifest systemBundleComponentManifest = new ComponentManifest();
        systemBundleComponentManifest.setProcessors(Arrays.asList(dynamicPropsProcessor));
        systemBundleComponentManifest.setControllerServices(Arrays.asList(sslContextService, dynamicPropsService));

        systemBundle = new Bundle("org.apache.nifi.minifi", "minifi-system", "0.6.0");
        systemBundle.setComponentManifest(systemBundleComponentManifest);

        // Add all bundles to manifest and give manifest to extension manager

        final List<Bundle> bundles = new ArrayList<>();
        bundles.add(httpBundle);
        bundles.add(systemBundle);
        agentManifest.setBundles(bundles);

        extensionManager = new StandardFDExtensionManager(agentManifest);
    }

    @Test
    public void testGetProcessorTypes() {
        final Set<FDComponentType> processorTypes = extensionManager.getProcessorTypes();
        assertNotNull(processorTypes);
        assertEquals(2, processorTypes.size());

        final FDComponentType processorType = processorTypes.stream()
                .filter(p -> p.getType().equals(invokeHttpProcessor.getType()))
                .findFirst()
                .orElse(null);

        assertEquals(invokeHttpProcessor.getType(), processorType.getType());
        assertEquals(httpBundle.getGroup(), processorType.getGroup());
        assertEquals(httpBundle.getArtifact(), processorType.getArtifact());
        assertEquals(httpBundle.getVersion(), processorType.getVersion());
    }

    @Test
    public void testControllerServiceTypes() {
        final Set<FDComponentType> serviceTypes = extensionManager.getControllerServiceTypes();
        assertNotNull(serviceTypes);
        assertEquals(2, serviceTypes.size());

        final FDComponentType serviceType = serviceTypes.stream()
                .filter(s -> s.getType().equals(sslContextService.getType()))
                .findFirst()
                .orElse(null);

        assertEquals(sslContextService.getType(), serviceType.getType());
        assertEquals(systemBundle.getGroup(), serviceType.getGroup());
        assertEquals(systemBundle.getArtifact(), serviceType.getArtifact());
        assertEquals(systemBundle.getVersion(), serviceType.getVersion());
    }

    @Test
    public void testGetManifestProcessorWhenExists() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        final ProcessorDefinition processor = extensionManager.getProcessorDefinition(invokeHttpProcessor.getType(), bundle);
        assertNotNull(processor);
        assertEquals(invokeHttpProcessor.getType(), processor.getType());
        assertEquals(invokeHttpProcessor.getGroup(), processor.getGroup());
        assertEquals(invokeHttpProcessor.getArtifact(), processor.getArtifact());
        assertEquals(invokeHttpProcessor.getVersion(), processor.getVersion());
        assertEquals(invokeHttpProcessor.getPropertyDescriptors().size(), processor.getPropertyDescriptors().size());

        assertNotNull(invokeHttpProcessor.getSupportedRelationships());
        assertEquals(2, invokeHttpProcessor.getSupportedRelationships().size());
        assertEquals(true, invokeHttpProcessor.getSupportsDynamicRelationships());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetManifestProcessorWhenTypeDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        extensionManager.getProcessorDefinition("does-not-exist", bundle);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetManifestProcessorWhenBundleDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        bundle.setGroup("does-not-exist");
        extensionManager.getProcessorDefinition(invokeHttpProcessor.getType(), bundle);
    }

    @Test
    public void testCreateProcessorsWhenExists() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        final FDProcessor processor = extensionManager.createProcessor(invokeHttpProcessor.getType(), bundle);
        assertNotNull(processor);

        assertNotNull(processor.getComponentConfiguration());
        assertEquals(invokeHttpProcessor.getType(), processor.getComponentConfiguration().getType());
        assertEquals(bundle.getGroup(), processor.getComponentConfiguration().getBundle().getGroup());
        assertEquals(bundle.getArtifact(), processor.getComponentConfiguration().getBundle().getArtifact());
        assertEquals(bundle.getVersion(), processor.getComponentConfiguration().getBundle().getVersion());

        assertNotNull(processor.getComponentDefinition());
        assertEquals(invokeHttpProcessor.getType(), processor.getComponentDefinition().getType());
        assertEquals(invokeHttpProcessor.getGroup(), processor.getComponentDefinition().getGroup());
        assertEquals(invokeHttpProcessor.getArtifact(), processor.getComponentDefinition().getArtifact());
        assertEquals(invokeHttpProcessor.getVersion(), processor.getComponentDefinition().getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWhenTypeDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        extensionManager.createProcessor("does-not-exist", bundle);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateProcessorWhenBundleDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        bundle.setGroup("does-not-exist");
        extensionManager.createProcessor(invokeHttpProcessor.getType(), bundle);
    }

    @Test
    public void testGetManifestControllerServiceWhenExists() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        final ControllerServiceDefinition controllerService = extensionManager.getControllerServiceDefinition(sslContextService.getType(), bundle);
        assertNotNull(controllerService);
        assertEquals(sslContextService.getType(), controllerService.getType());
        assertEquals(sslContextService.getGroup(), controllerService.getGroup());
        assertEquals(sslContextService.getArtifact(), controllerService.getArtifact());
        assertEquals(sslContextService.getVersion(), controllerService.getVersion());
        assertEquals(sslContextService.getPropertyDescriptors().size(), controllerService.getPropertyDescriptors().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetManifestControllerServiceWhenTypeDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        extensionManager.getControllerServiceDefinition("does-not-exist", bundle);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetManifestControllerServiceWhenBundleDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        bundle.setGroup("does-not-exist");
        extensionManager.getControllerServiceDefinition(sslContextService.getType(), bundle);
    }

    @Test
    public void testCreateControllerServiceWhenExists() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        final FDControllerService controllerService = extensionManager.createControllerService(sslContextService.getType(), bundle);
        assertNotNull(controllerService);
        assertNotNull(controllerService.getComponentConfiguration());
        assertNotNull(controllerService.getComponentDefinition());
        assertEquals(sslContextService.getType(), controllerService.getComponentDefinition().getType());
        assertEquals(sslContextService.getGroup(), controllerService.getComponentDefinition().getGroup());
        assertEquals(sslContextService.getArtifact(), controllerService.getComponentDefinition().getArtifact());
        assertEquals(sslContextService.getVersion(), controllerService.getComponentDefinition().getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateControllerServiceWhenTypeDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        extensionManager.createControllerService("does-not-exist", bundle);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateControllerServiceWhenBundleDoesNotExist() {
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        bundle.setGroup("does-not-exist");
        extensionManager.createControllerService(sslContextService.getType(), bundle);
    }

    @Test
    public void testGetProcessorPropertyDescriptorWhenExists() {
        final String propertyName = "SSL Context Service";

        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        final Optional<PropertyDescriptor> propertyDescriptor = extensionManager.getProcessorPropertyDescriptor(
                invokeHttpProcessor.getType(), bundle, propertyName);
        assertTrue(propertyDescriptor.isPresent());
        assertEquals(propertyName, propertyDescriptor.get().getName());
    }

    @Test
    public void testGetProcessorPropertyDescriptorWhenDoesNotExist() {
        final String propertyName = "DOES NOT EXIST";

        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        final Optional<PropertyDescriptor> propertyDescriptor = extensionManager.getProcessorPropertyDescriptor(
                invokeHttpProcessor.getType(), bundle, propertyName);
        assertFalse(propertyDescriptor.isPresent());
    }

    @Test
    public void testCreateDynamicProcessorPropertyWhenSupported() {
        final String propertyName = "FOO";
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);

        final PropertyDescriptor dynamicProperty = extensionManager.createDynamicProcessorPropertyDescriptor(
                dynamicPropsProcessor.getType(), bundle, propertyName);
        assertNotNull(dynamicProperty);
        assertEquals(propertyName, dynamicProperty.getName());
        assertEquals(propertyName, dynamicProperty.getDisplayName());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateDynamicProcessorPropertyWhenNotSupported() {
        final String propertyName = "FOO";
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(httpBundle);
        extensionManager.createDynamicProcessorPropertyDescriptor(invokeHttpProcessor.getType(), bundle, propertyName);
    }

    @Test
    public void testGetControllerServicePropertyDescriptorWhenExists() {
        final String propertyName = "Client Certificate";

        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        final Optional<PropertyDescriptor> propertyDescriptor = extensionManager.getControllerServicePropertyDescriptor(
                sslContextService.getType(), bundle, propertyName);
        assertTrue(propertyDescriptor.isPresent());
        assertEquals(propertyName, propertyDescriptor.get().getName());
    }

    @Test
    public void testGetControllerServicePropertyDescriptorWhenDoesNotExist() {
        final String propertyName = "DOES NOT EXIST";

        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        final Optional<PropertyDescriptor> propertyDescriptor = extensionManager.getControllerServicePropertyDescriptor(
                sslContextService.getType(), bundle, propertyName);
        assertFalse(propertyDescriptor.isPresent());
    }

    @Test
    public void testCreateDynamicControllerServicePropertyWhenSupported() {
        final String propertyName = "FOO";
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);

        final PropertyDescriptor dynamicProperty = extensionManager.createDynamicControllerServicePropertyDescriptor(
                dynamicPropsService.getType(), bundle, propertyName);
        assertNotNull(dynamicProperty);
        assertEquals(propertyName, dynamicProperty.getName());
        assertEquals(propertyName, dynamicProperty.getDisplayName());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateDynamicControllerServicePropertyWhenNotSupported() {
        final String propertyName = "FOO";
        final org.apache.nifi.registry.flow.Bundle bundle = getRequestBundle(systemBundle);
        extensionManager.createDynamicControllerServicePropertyDescriptor(sslContextService.getType(), bundle, propertyName);
    }

    private org.apache.nifi.registry.flow.Bundle getRequestBundle(Bundle manifestBundle) {
        final org.apache.nifi.registry.flow.Bundle bundle = new org.apache.nifi.registry.flow.Bundle();
        bundle.setGroup(manifestBundle.getGroup());
        bundle.setArtifact(manifestBundle.getArtifact());
        bundle.setVersion(manifestBundle.getVersion());
        return bundle;
    }

}
