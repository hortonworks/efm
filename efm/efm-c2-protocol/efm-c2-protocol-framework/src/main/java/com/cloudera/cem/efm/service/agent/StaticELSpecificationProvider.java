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
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.extension.Bundle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service  // TODO, should we move this to an AutoConfiguration Factory with a @ConditionalOnMissingBean so that it can be overwritten by an external jar?
public class StaticELSpecificationProvider implements ELSpecificationProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(StaticELSpecificationProvider.class);

    static final String MINIFI_CPP_TYPE = "minifi-cpp";
    static final String MINIFI_JAVA_TYPE = "minifi-java";
    static final Pattern SPEC_FILENAME_PATTERN = Pattern.compile("el-(" + MINIFI_CPP_TYPE + "|" + MINIFI_JAVA_TYPE + ")-(.+)\\.json");

    static final String MINIFI_CPP_EL_BUNDLE_ARTIFACT = "minifi-expression-language-extensions";

    private final String specificationsDir;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    private final SortedSet<String> specificationKeys = new TreeSet<>();
    private final Map<String, ELSpecification> specifications = new HashMap<>();

    @Autowired
    public StaticELSpecificationProvider(final ELSpecificationProperties properties, final AgentService agentService, final ObjectMapper objectMapper) {
        this.specificationsDir = properties.getDir();
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadSpecifications() {
        if (StringUtils.isBlank(specificationsDir)) {
            printWarning();
            return;
        }

        final File specsDir = new File(specificationsDir);
        if (!specsDir.exists() || !specsDir.canRead()) {
            printWarning();
            return;
        }

        LOGGER.info("Loading EL specifications from: {}", new Object[]{specsDir});
        for(final File specFile : specsDir.listFiles()) {
            final Matcher matcher = SPEC_FILENAME_PATTERN.matcher(specFile.getName());
            if (!matcher.matches()) {
                LOGGER.warn("Skipping EL specification file '{}' that does not match naming convention of 'el-<type>-<version>.json'", new Object[]{specFile.getName()});
                continue;
            }

            try {
                final String key = getKey(matcher.group(1), matcher.group(2));
                final ELSpecification specification = objectMapper.readValue(specFile, ELSpecification.class);
                specification.setSpecificationKey(key);

                specifications.put(key, specification);
                specificationKeys.add(key);

                LOGGER.info("Loaded EL specification for '{}'", new Object[]{key});
            } catch (Exception e) {
                LOGGER.error("Unable to read specification file {} due to: {}", new Object[]{specFile.getName(), e.getMessage()}, e);
            }
        }

        if (specifications.isEmpty()) {
            LOGGER.warn("***********************************************************************************************");
            LOGGER.warn("No EL specifications were loaded, this will prevent auto-completion of EL functions in the flow designer");
            LOGGER.warn("***********************************************************************************************");
        } else {
            LOGGER.info("Finished loading {} EL specifications", new Object[]{specifications.size()});
        }
    }

    private void printWarning() {
        LOGGER.warn("***********************************************************************************************");
        LOGGER.warn("EL specification directory does not exist, or is not readable, no specifications will be loaded");
        LOGGER.warn("***********************************************************************************************");
    }

    @Override
    public Optional<ELSpecification> getELSpecification(final String agentClass) {
        if (StringUtils.isBlank(agentClass)) {
            throw new IllegalArgumentException("AgentClass is required to obtain the EL specification");
        }

        final List<AgentManifest> agentManifests = agentService.getAgentManifests(agentClass);
        if (agentManifests.isEmpty()) {
            throw new IllegalArgumentException("No manifests exists for the given agent class");
        }

        final AgentManifest agentManifest = agentManifests.get(0);
        if (agentManifest.getAgentType() == null) {
            throw new IllegalArgumentException("AgentManifest AgentType is required to obtain the EL specification");
        }

        // determine the agent type for the key...
        String mappedType;
        final String agentType = agentManifest.getAgentType();
        if (agentType.equals(MINIFI_JAVA_TYPE) || agentType.equals("java")) {
            mappedType = MINIFI_JAVA_TYPE;
        } else if (agentType.equals(MINIFI_CPP_TYPE) || agentType.equals("cpp")) {
            mappedType = MINIFI_CPP_TYPE;
        } else {
            throw new IllegalArgumentException("Unexpected agent type '" + agentType + "'");
        }


        // determine the version for the key...
        String version = null;
        final BuildInfo buildInfo = agentManifest.getBuildInfo();
        if (buildInfo != null && buildInfo.getVersion() != null) {
            final String buildInfoVersion = buildInfo.getVersion();
            final int snapshotIndex = buildInfoVersion.indexOf("-SNAPSHOT");
            if (snapshotIndex > 0) {
                version = buildInfoVersion.substring(0, snapshotIndex);
            } else {
                version = buildInfoVersion;
            }
        }

        // if the type is CPP, check if the EL bundle is included, and if not then return an empty spec
        if (mappedType.equals(MINIFI_CPP_TYPE)) {
            boolean hasELBundle = false;
            if (agentManifest.getBundles() != null) {
                for (final Bundle bundle : agentManifest.getBundles()) {
                    if (MINIFI_CPP_EL_BUNDLE_ARTIFACT.equals(bundle.getArtifact())) {
                        hasELBundle = true;
                        break;
                    }
                }
            }

            if (!hasELBundle) {
                final ELSpecification emptySpec = new ELSpecification();
                emptySpec.setSpecificationKey("missing-el-bundle");
                emptySpec.setOperations(Collections.emptyMap());
                return Optional.of(emptySpec);
            }
        }

        // if version is null then we'll attempt to take the first EL spec using just the type, otherwise look for exact version
        if (version == null) {
            return getCompatibleELSpecification(mappedType);
        } else {
            final String key = getKey(mappedType, version);
            final ELSpecification exactSpecification = specifications.get(key);
            if (exactSpecification == null) {
                LOGGER.warn("No EL specification was found for {}, attempting to find compatible specification", new Object[]{key});
                return getCompatibleELSpecification(mappedType);
            } else {
                return Optional.of(exactSpecification);
            }
        }
    }

    private Optional<ELSpecification> getCompatibleELSpecification(final String mappedType) {
        final List<String> compatibleKeys = specificationKeys.stream()
                .filter(s -> s.startsWith(mappedType))
                .collect(Collectors.toList());

        if (compatibleKeys.isEmpty()) {
            return Optional.empty();
        } else if (compatibleKeys.size() == 1) {
            final String compatibleKey = compatibleKeys.get(0);
            return Optional.of(specifications.get(compatibleKey));
        } else {
            LOGGER.warn("Could not determine compatible EL specification because more than one specification " +
                    "was found for the agent type '{}', will return empty specification", new Object[]{mappedType});
            return Optional.empty();
        }
    }

    private String getKey(String agentType, String version) {
        return agentType + "-" + version;
    }
}
