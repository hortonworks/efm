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
package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.model.AgentManifest;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.security.StandardNiFiUser;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.service.FDServiceFacade;
import com.cloudera.cem.efm.service.agent.AgentService;
import com.cloudera.cem.efm.service.extension.FDExtensionManagers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * The runnable task that monitors agent-classes known to the C2 server and creates a local flow in flow designer for
 * any agent-class that doesn't already have one.
 */
@Service
public class AgentClassMonitorTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentClassMonitorTask.class);

    static final NiFiUser SYSTEM_USER = new StandardNiFiUser.Builder().identity("system").build();

    private AgentService agentService;
    private FDServiceFacade fdServiceFacade;
    private FDExtensionManagers fdExtensionManagers;

    @Autowired
    public AgentClassMonitorTask(final AgentService agentService,
                                 final FDServiceFacade fdServiceFacade,
                                 final FDExtensionManagers fdExtensionManagers) {
        this.agentService = agentService;
        this.fdServiceFacade = fdServiceFacade;
        this.fdExtensionManagers = fdExtensionManagers;
    }

    @Override
    public void run() {
        int createdFlows = 0;
        int skippedAgentClasses = 0;

        final List<FDFlowMetadata> allFlows = fdServiceFacade.getAvailableFlows();
        final List<AgentClass> agentClasses = agentService.getAgentClasses();

        LOGGER.debug("Comparing {} agent classes to {} flows...", new Object[]{agentClasses.size(), allFlows.size()});

        for (final AgentClass agentClass : agentClasses) {
            if (agentClass.getAgentManifests() == null || agentClass.getAgentManifests().isEmpty()) {
                LOGGER.debug("Found agent class '{}' with no manifests assigned, will skip creating a flow",
                        new Object[]{agentClass.getName()});
                skippedAgentClasses++;
                continue;
            }

            final Optional<FDFlowMetadata> agentFlow = allFlows.stream()
                    .filter(f -> f.getAgentClass().equals(agentClass.getName()))
                    .findFirst();
            if (!agentFlow.isPresent()) {
                try {
                    final FDFlowMetadata flowMetadata = fdServiceFacade.createFlow(agentClass.getName(), SYSTEM_USER);
                    createdFlows++;
                    LOGGER.debug("Created flow for {} with id {}", new Object[]{agentClass.getName(), flowMetadata.getIdentifier()});
                } catch (Exception e) {
                    LOGGER.error("Error creating flow for {}", new Object[]{agentClass.getName()}, e);
                }
            }
        }

        LOGGER.debug("Finished comparing agent classes to flows! {} flows were created, " +
                "{} agent classes had no manifests and were skipped", new Object[]{createdFlows, skippedAgentClasses});

        LOGGER.debug("Refreshing extension managers for agent classes...");

        fdExtensionManagers.loadExtensionManagers(agentClasses, (agentClass) -> {
            // NOTE: For now we are assuming only one agent manifest per class so we just take the first one
            final List<AgentManifest> agentManifests = agentService.getAgentManifests(agentClass.getName());
            return agentManifests.isEmpty() ? null : agentManifests.get(0);
        });

        LOGGER.debug("Done refreshing extension managers for agent classes!");
    }

}
