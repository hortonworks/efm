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
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.profile.DevProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Populates development data for agent classes and manifests.
 */
@Service
@Transactional(rollbackFor = Exception.class)
@DevProfile
public class AgentManifestPopulator {

    private static final String DEV_MANIFEST_DIR = "dev/manifests";
    private static final String DEFAULT_MANIFEST = DEV_MANIFEST_DIR + "/default-manifest.json";

    private ObjectMapper objectMapper;
    private AgentService agentService;

    @Autowired
    public AgentManifestPopulator(final ObjectMapper objectMapper, final AgentService agentService) {
        this.objectMapper = objectMapper;
        this.agentService = agentService;
    }

    @PostConstruct
    public void populateDevData() throws IOException {
        final InputStream defaultManifestIn = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_MANIFEST);
        final AgentManifest defaultManifest = objectMapper.readValue(defaultManifestIn, AgentManifest.class);

        if (defaultManifest.getIdentifier() == null) {
            defaultManifest.setIdentifier("devMode-default-agentManifest");
        }

        final AgentManifest createdDefaultManifest = agentService.getAgentManifest(defaultManifest.getIdentifier())
                .orElseGet(() -> agentService.createAgentManifest(defaultManifest));

        if (!agentService.getAgentClass("Class A").isPresent()) {
            final AgentClass agentClassA = new AgentClass();
            agentClassA.setName("Class A");
            agentClassA.setDescription("Agent class A used for development");

            final Set<String> agentManifestIds = new HashSet<>();
            agentManifestIds.add(createdDefaultManifest.getIdentifier());
            agentClassA.setAgentManifests(agentManifestIds);

            agentService.createAgentClass(agentClassA);
        }

        if (!agentService.getAgentClass("Class B").isPresent()) {
            final AgentClass agentClassB = new AgentClass();
            agentClassB.setName("Class B");
            agentClassB.setDescription("Agent class B used for development");

            final Set<String> agentManifestIds = new HashSet<>();
            agentManifestIds.add(createdDefaultManifest.getIdentifier());
            agentClassB.setAgentManifests(agentManifestIds);

            agentService.createAgentClass(agentClassB);
        }

        if (!agentService.getAgentClass("Class C").isPresent()) {
            final AgentClass agentClassC = new AgentClass();
            agentClassC.setName("Class C");
            agentClassC.setDescription("Agent class C used for development");

            agentService.createAgentClass(agentClassC);
        }
    }

}
