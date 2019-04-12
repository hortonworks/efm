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

import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.AgentManifest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Creates extension managers from a list of agent classes.
 */
@Service
public class FDExtensionManagers {

    static final Logger LOGGER = LoggerFactory.getLogger(FDExtensionManagers.class);

    private final AtomicReference<Map<String,FDExtensionManager>> extensionManagersReference = new AtomicReference<>(new HashMap<>());

    public void loadExtensionManagers(final List<AgentClass> agentClasses, final Function<AgentClass,AgentManifest> agentManifestLookup) {
        final Map<String,FDExtensionManager> extensionManagers = new HashMap<>();

        for (final AgentClass agentClass : agentClasses) {
            final String agentClassName = agentClass.getName();
            try {
                final AgentManifest agentManifest = agentManifestLookup.apply(agentClass);
                if (agentManifest == null) {
                    LOGGER.error("Unable to create extension manager for '{}' because no manifest is available", new Object[]{agentClassName});
                } else {
                    final FDExtensionManager extensionManager = new StandardFDExtensionManager(agentManifest);
                    extensionManagers.put(agentClassName, extensionManager);
                    LOGGER.debug("Created extension manager for '{}'", new Object[]{agentClassName});
                }
            } catch (Throwable t) {
                LOGGER.error("Error creating extension manager for " + agentClassName, t);
            }
        }

        extensionManagersReference.set(extensionManagers);
    }

    public Optional<FDExtensionManager> getExtensionManager(final String agentClass) {
        if (StringUtils.isBlank(agentClass)) {
            throw new IllegalArgumentException("Agent class cannot be null or blank");
        }

        final Map<String,FDExtensionManager> extensionManagers = extensionManagersReference.get();

        final FDExtensionManager extensionManager = extensionManagers.get(agentClass);
        if (extensionManager == null) {
            return Optional.empty();
        } else {
            return Optional.of(extensionManager);
        }
    }

}
