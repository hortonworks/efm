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
package com.cloudera.cem.efm.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.BuildInfo;
import com.cloudera.cem.efm.model.ELOperation;
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.extension.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TestStaticELSpecificationProvider {

    private AgentService agentService;
    private ELSpecificationProvider specificationProvider;

    @Before
    public void setup() {
        final ELSpecificationProperties properties = new ELSpecificationProperties();
        properties.setDir("src/test/resources/specs");

        agentService = Mockito.mock(AgentService.class);
        specificationProvider = new StaticELSpecificationProvider(properties, agentService, new ObjectMapper());
        ((StaticELSpecificationProvider) specificationProvider).loadSpecifications();
    }

    private AgentManifest createAgentManifest(String version, String agentType) {
        final BuildInfo buildInfo = new BuildInfo();
        buildInfo.setVersion(version);

        final Bundle elBundle = new Bundle();
        elBundle.setArtifact(StaticELSpecificationProvider.MINIFI_CPP_EL_BUNDLE_ARTIFACT);

        final AgentManifest defaultManifest = new AgentManifest();
        defaultManifest.setAgentType(agentType);
        defaultManifest.setBuildInfo(buildInfo);
        defaultManifest.setBundles(Collections.singletonList(elBundle));
        return defaultManifest;
    }

    @Test
    public void testGetCppELSpecificationExactVersionExists() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.5.0", "cpp");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-cpp-0.5.0", specification.get().getSpecificationKey());

        final Map<String, ELOperation> operations = specification.get().getOperations();
        assertNotNull(operations);
        assertEquals(1, operations.size());

        final ELOperation operation = operations.get("isNull");
        assertNotNull(operation);
        assertEquals("isNull", operation.getName());
    }

    @Test
    public void testGetCppELSpecificationExactVersionWhenNoELBundle() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.5.0", "cpp");
        defaultManifest.setBundles(Collections.emptyList());

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("missing-el-bundle", specification.get().getSpecificationKey());

        final Map<String, ELOperation> operations = specification.get().getOperations();
        assertNotNull(operations);
        assertEquals(0, operations.size());
    }

    @Test
    public void testGetJavaELSpecificationExactVersionExists() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.6.0", "minifi-java");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-java-0.6.0", specification.get().getSpecificationKey());
    }

    @Test
    public void testGetCppELSpecificationWithAgentTypeVariation() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.6.0", "minifi-cpp");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-cpp-0.6.0", specification.get().getSpecificationKey());
    }

    @Test
    public void testGetJavaELSpecificationWithAgentTypeVariation() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.6.0", "java");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-java-0.6.0", specification.get().getSpecificationKey());
    }

    @Test
    public void testGetJavaELSpecificationWithSnapshotVersion() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("0.6.0-SNAPSHOT", "minifi-java");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-java-0.6.0", specification.get().getSpecificationKey());
    }

    @Test
    public void testGetELSpecWhenVersionDoesNotExistAndMultipleCompatible() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("X.Y.Z", "cpp");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertFalse(specification.isPresent());
    }

    @Test
    public void testGetELSpecWhenVersionDoesNotExistAndSingleCompatible() {
        final String agentClass = "default";
        final AgentManifest defaultManifest = createAgentManifest("X.Y.Z", "minifi-java");

        when(agentService.getAgentManifests(agentClass)).thenReturn(Collections.singletonList(defaultManifest));

        final Optional<ELSpecification> specification = specificationProvider.getELSpecification(agentClass);
        assertTrue(specification.isPresent());
        assertEquals("minifi-java-0.6.0", specification.get().getSpecificationKey());
    }
}
