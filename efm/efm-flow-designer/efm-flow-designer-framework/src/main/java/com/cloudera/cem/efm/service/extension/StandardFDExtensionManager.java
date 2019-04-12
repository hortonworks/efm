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
import com.cloudera.cem.efm.model.BuildInfo;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.extension.ComponentManifest;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.DefinedType;
import com.cloudera.cem.efm.model.extension.ExpressionLanguageScope;
import com.cloudera.cem.efm.model.extension.ExtensionComponent;
import com.cloudera.cem.efm.model.extension.InputRequirement;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import com.cloudera.cem.efm.model.extension.Relationship;
import com.cloudera.cem.efm.model.extension.SchedulingDefaults;
import com.cloudera.cem.efm.model.extension.SchedulingStrategy;
import com.cloudera.cem.efm.model.types.FDComponentType;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.ControllerServiceAPI;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of FDExtensionManager.
 *
 * This class is not a spring bean since it will be managed by the FDExtensionManagers class.
 */
public class StandardFDExtensionManager implements FDExtensionManager {

    static final Logger LOGGER = LoggerFactory.getLogger(StandardFDExtensionManager.class);

    private AgentManifest agentManifest;
    private Set<FDComponentType> processors;
    private Set<FDComponentType> controllerServices;

    public StandardFDExtensionManager(final AgentManifest agentManifest) {
        this.agentManifest = agentManifest;
    }

    // ----------- Processor methods ------------------

    @Override
    public Set<FDComponentType> getProcessorTypes() {
        if (processors == null) {
            synchronized (this) {
                if (processors == null) {
                    processors = Collections.unmodifiableSet(
                            createSummaries(agentManifest, ComponentManifest::getProcessors));
                }
            }
        }
        return processors;
    }

    @Override
    public ProcessorDefinition getProcessorDefinition(final String type, final Bundle bundle) {
        final ProcessorDefinition manifestProcessor = locateManifestProcessor(type, bundle);
        return copyManifestProcessor(manifestProcessor);
    }

    @Override
    public FDProcessor createProcessor(final String type, final Bundle bundle) {
        final ProcessorDefinition manifestProcessor = locateManifestProcessor(type, bundle);
        return createProcessor(type, bundle, manifestProcessor);
    }

    @Override
    public Optional<PropertyDescriptor> getProcessorPropertyDescriptor(final String type, final Bundle bundle, final String propertyName) {
        final ProcessorDefinition manifestProcessor = locateManifestProcessor(type, bundle);

        final Map<String,PropertyDescriptor> propertyDescriptors = manifestProcessor.getPropertyDescriptors();
        if (propertyDescriptors == null || !propertyDescriptors.containsKey(propertyName)) {
            return Optional.empty();
        } else {
            return Optional.of(propertyDescriptors.get(propertyName));
        }
    }

    @Override
    public PropertyDescriptor createDynamicProcessorPropertyDescriptor(final String type, final Bundle bundle, final String propertyName) {
        final ProcessorDefinition manifestProcessor = locateManifestProcessor(type, bundle);

        if (!manifestProcessor.getSupportsDynamicProperties()) {
            throw new IllegalStateException("Processor does not support dynamic properties");
        }

        // NOTE - Eventually we should be getting the definition of the dynamic property from the manifest, but for now it is hard-coded

        final PropertyDescriptor propertyDescriptor = new PropertyDescriptor();
        propertyDescriptor.setName(propertyName);
        propertyDescriptor.setDisplayName(propertyName);
        propertyDescriptor.setExpressionLanguageScope(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES);
        propertyDescriptor.setDescription("Dynamic property");
        propertyDescriptor.setRequired(false);
        propertyDescriptor.setDynamic(true);
        return propertyDescriptor;
    }


    // ----------- Controller Service methods ------------------

    @Override
    public Set<FDComponentType> getControllerServiceTypes() {
        if (controllerServices == null) {
            synchronized (this) {
                if (controllerServices == null) {
                    controllerServices = Collections.unmodifiableSet(
                            createSummaries(agentManifest, ComponentManifest::getControllerServices));
                }
            }
        }
        return controllerServices;
    }

    @Override
    public ControllerServiceDefinition getControllerServiceDefinition(final String type, final Bundle bundle) {
        final ControllerServiceDefinition manifestControllerService = locateManifestControllerService(type, bundle);
        return copyManifestControllerService(manifestControllerService);
    }

    @Override
    public FDControllerService createControllerService(final String type, final Bundle bundle) {
        final ControllerServiceDefinition manifestControllerService = locateManifestControllerService(type, bundle);
        return createControllerService(type, bundle, manifestControllerService);
    }

    @Override
    public Optional<PropertyDescriptor> getControllerServicePropertyDescriptor(final String type, final Bundle bundle, final String propertyName) {
        final ControllerServiceDefinition serviceDefinition = locateManifestControllerService(type, bundle);

        final Map<String,PropertyDescriptor> propertyDescriptors = serviceDefinition.getPropertyDescriptors();
        if (propertyDescriptors == null || !propertyDescriptors.containsKey(propertyName)) {
            return Optional.empty();
        } else {
            return Optional.of(propertyDescriptors.get(propertyName));
        }
    }

    @Override
    public PropertyDescriptor createDynamicControllerServicePropertyDescriptor(final String type, final Bundle bundle, final String propertyName) {
        final ControllerServiceDefinition serviceDefinition = locateManifestControllerService(type, bundle);

        if (!serviceDefinition.getSupportsDynamicProperties()) {
            throw new IllegalStateException("Service does not support dynamic properties");
        }

        // NOTE - Eventually we should be getting the definition of the dynamic property from the manifest, but for now it is hard-coded

        final PropertyDescriptor propertyDescriptor = new PropertyDescriptor();
        propertyDescriptor.setName(propertyName);
        propertyDescriptor.setDisplayName(propertyName);
        propertyDescriptor.setExpressionLanguageScope(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES);
        propertyDescriptor.setDescription("Dynamic property");
        propertyDescriptor.setRequired(false);
        propertyDescriptor.setDynamic(true);
        return propertyDescriptor;
    }

    // ----------- Helper methods ------------------

    private com.cloudera.cem.efm.model.extension.Bundle locateManifestBundle(String type, Bundle bundle) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("Processor type is required");
        }

        if (bundle == null || StringUtils.isBlank(bundle.getGroup()) || StringUtils.isBlank(bundle.getArtifact()) || StringUtils.isBlank(bundle.getVersion())) {
            throw new IllegalArgumentException("Bundle information is required");
        }

        final Optional<com.cloudera.cem.efm.model.extension.Bundle> manifestBundle = agentManifest.getBundles().stream()
                .filter(b -> b.getArtifact().equals(bundle.getArtifact())
                        && b.getGroup().equals(bundle.getGroup())
                        && b.getVersion().equals(bundle.getVersion()))
                .findFirst();

        if (!manifestBundle.isPresent()) {
            throw new IllegalArgumentException("A bundle does not exist with the given group, artifact, and version");
        }
        return manifestBundle.get();
    }

    private ControllerServiceDefinition locateManifestControllerService(final String type, final Bundle bundle) {
        final com.cloudera.cem.efm.model.extension.Bundle manifestBundle = locateManifestBundle(type, bundle);

        final Optional<ControllerServiceDefinition> manifestControllerService = manifestBundle.getComponentManifest().getControllerServices().stream()
                .filter(p -> p.getType().equals(type))
                .findFirst();

        if (!manifestControllerService.isPresent()) {
            throw new IllegalArgumentException("A processor with the given type does not exist in the bundle");
        }

        return manifestControllerService.get();
    }

    private ProcessorDefinition locateManifestProcessor(final String type, final Bundle bundle) {
        final com.cloudera.cem.efm.model.extension.Bundle manifestBundle = locateManifestBundle(type, bundle);

        final Optional<ProcessorDefinition> manifestProcessor = manifestBundle.getComponentManifest().getProcessors().stream()
                .filter(p -> p.getType().equals(type))
                .findFirst();

        if (!manifestProcessor.isPresent()) {
            throw new IllegalArgumentException("A processor with the given type does not exist in the bundle");
        }

        return manifestProcessor.get();
    }

    private FDControllerService createControllerService(final String type, final Bundle bundle, final ControllerServiceDefinition manifestControllerService) {
        final ControllerServiceDefinition componentDefinition = copyManifestControllerService(manifestControllerService);
        final VersionedControllerService componentConfiguration = createVersionedControllerService(type, bundle, componentDefinition);

        final FDControllerService controllerService = new FDControllerService();
        controllerService.setComponentDefinition(componentDefinition);
        controllerService.setComponentConfiguration(componentConfiguration);
        return controllerService;
    }

    private ControllerServiceDefinition copyManifestControllerService(final ControllerServiceDefinition manifestControllerService) {
        final ControllerServiceDefinition serviceCopy = new ControllerServiceDefinition();

        // Copy base fields from DefinedType
        copyDefinedTypeFields(manifestControllerService, serviceCopy);

        // Copy fields from ExtensionComponent
        copyExtensionComponentFields(manifestControllerService, serviceCopy);

        // Copy other fields from Controller Service
        final LinkedHashMap<String, PropertyDescriptor> procDescriptors = new LinkedHashMap<>();
        if (manifestControllerService.getPropertyDescriptors() != null) {
            for (final Map.Entry<String, PropertyDescriptor> entry : manifestControllerService.getPropertyDescriptors().entrySet()) {
                procDescriptors.put(entry.getKey(), copyPropertyDescriptor(entry.getValue()));
            }
        }
        serviceCopy.setPropertyDescriptors(procDescriptors);

        serviceCopy.setSupportsDynamicProperties(manifestControllerService.getSupportsDynamicProperties());

        return serviceCopy;
    }

    private VersionedControllerService createVersionedControllerService(final String type, final Bundle bundle, final ControllerServiceDefinition manifestControllerService) {
        final Bundle componentBundle = createVersionedBundle(bundle);

        final VersionedControllerService component = new VersionedControllerService();
        component.setBundle(componentBundle);
        component.setType(type);

        // Populate default values based on manifest

        final Map<String,String> propertyValues = new HashMap<>();
        final Map<String, VersionedPropertyDescriptor> propertyDescriptors = new HashMap<>();

        if (manifestControllerService.getPropertyDescriptors() != null) {
            for (final Map.Entry<String,PropertyDescriptor> manifestEntry : manifestControllerService.getPropertyDescriptors().entrySet()) {
                final PropertyDescriptor manifestDescriptor = manifestEntry.getValue();

                final VersionedPropertyDescriptor componentDescriptor = createVersionedPropertyDescriptor(manifestDescriptor);
                propertyDescriptors.put(componentDescriptor.getName(), componentDescriptor);

                final String defaultValue = manifestDescriptor.getDefaultValue();
                propertyValues.put(componentDescriptor.getName(), defaultValue);
            }
        }

        component.setProperties(propertyValues);
        component.setPropertyDescriptors(propertyDescriptors);

        final List<ControllerServiceAPI> controllerServiceApis = new ArrayList<>();

        // NOTE: if the manifest provided the API implementations then we'll use those, but if it didn't then we'll assume it
        // is minifi-cpp and we can make the API the actual implementation itself b/c it doesn't have the concept of API bundles

        final List<DefinedType> providedServiceApis = manifestControllerService.getProvidedApiImplementations();
        if (providedServiceApis != null && providedServiceApis.size() > 0) {
            for (final DefinedType providedApi : providedServiceApis) {
                final Bundle apiBundle = new Bundle();
                apiBundle.setGroup(providedApi.getGroup());
                apiBundle.setArtifact(providedApi.getArtifact());
                apiBundle.setVersion(providedApi.getVersion());

                final String apiType = providedApi.getType();

                final ControllerServiceAPI controllerServiceApi = new ControllerServiceAPI();
                controllerServiceApi.setBundle(apiBundle);
                controllerServiceApi.setType(apiType);

                controllerServiceApis.add(controllerServiceApi);
            }
        } else {
            final Bundle implBundle = new Bundle();
            implBundle.setGroup(bundle.getGroup());
            implBundle.setArtifact(bundle.getArtifact());
            implBundle.setVersion(bundle.getVersion());

            final ControllerServiceAPI controllerServiceApi = new ControllerServiceAPI();
            controllerServiceApi.setBundle(implBundle);
            controllerServiceApi.setType(type);

            controllerServiceApis.add(controllerServiceApi);
        }

        component.setControllerServiceApis(controllerServiceApis);
        return component;
    }


    private FDProcessor createProcessor(final String type, final Bundle bundle, final ProcessorDefinition manifestProcessor) {
        final ProcessorDefinition componentDefinition = copyManifestProcessor(manifestProcessor);
        final VersionedProcessor componentConfiguration = createVersionedProcessor(type, bundle, componentDefinition);

        final FDProcessor processor = new FDProcessor();
        processor.setComponentConfiguration(componentConfiguration);
        processor.setComponentDefinition(componentDefinition);
        return processor;
    }

    private VersionedProcessor createVersionedProcessor(final String type, final Bundle bundle, final ProcessorDefinition manifestProcessor) {
        final Bundle componentBundle = createVersionedBundle(bundle);

        final VersionedProcessor component = new VersionedProcessor();
        component.setType(type);
        component.setBundle(componentBundle);
        component.setPosition(new Position(0,0));
        component.setStyle(new HashMap<>());
        component.setAutoTerminatedRelationships(new HashSet<>());

        // Populate default values based on manifest

        final SchedulingDefaults schedulingDefaults = agentManifest.getSchedulingDefaults();
        if (schedulingDefaults != null) {
            final SchedulingStrategy schedulingStrategy = schedulingDefaults.getDefaultSchedulingStrategy();
            if (schedulingStrategy == null) {
                LOGGER.warn("Scheduling strategy was not specified in manifest, defaulting to TIMER_DRIVEN");
                component.setSchedulingStrategy(SchedulingStrategy.TIMER_DRIVEN.name());
            } else {
                component.setSchedulingStrategy(schedulingStrategy.name());
            }

            final String schedulingPeriodMillis = String.valueOf(schedulingDefaults.getDefaultSchedulingPeriodMillis());
            component.setSchedulingPeriod(schedulingPeriodMillis + " ms");

            final String defaultConcurrentTasks = Optional.ofNullable(schedulingDefaults.getDefaultMaxConcurrentTasks()).orElse("1");
            component.setConcurrentlySchedulableTaskCount(Integer.valueOf(defaultConcurrentTasks));

            long defaultRunDuration = schedulingDefaults.getDefaultRunDurationNanos();
            if (defaultRunDuration > 0) {
                defaultRunDuration  = defaultRunDuration / 1000;
            }
            component.setRunDurationMillis(defaultRunDuration);

            final String defaultYieldDurationMillis = String.valueOf(schedulingDefaults.getYieldDurationMillis());
            component.setYieldDuration(defaultYieldDurationMillis + " ms");

            final String defaultPenalizationMillis = String.valueOf(schedulingDefaults.getPenalizationPeriodMillis());
            component.setPenaltyDuration(defaultPenalizationMillis + " ms");
        }

        final Map<String,String> propertyValues = new HashMap<>();
        final Map<String, VersionedPropertyDescriptor> propertyDescriptors = new HashMap<>();

        if (manifestProcessor.getPropertyDescriptors() != null) {
            for (final Map.Entry<String,PropertyDescriptor> manifestEntry : manifestProcessor.getPropertyDescriptors().entrySet()) {
                final PropertyDescriptor manifestDescriptor = manifestEntry.getValue();

                final VersionedPropertyDescriptor componentDescriptor = createVersionedPropertyDescriptor(manifestDescriptor);
                propertyDescriptors.put(componentDescriptor.getName(), componentDescriptor);

                final String defaultValue = manifestDescriptor.getDefaultValue();
                propertyValues.put(componentDescriptor.getName(), defaultValue);
            }
        }

        component.setProperties(propertyValues);
        component.setPropertyDescriptors(propertyDescriptors);
        component.setComments("");

        return component;
    }

    private Bundle createVersionedBundle(final Bundle bundle) {
        final Bundle componentBundle = new Bundle();
        componentBundle.setGroup(bundle.getGroup());
        componentBundle.setArtifact(bundle.getArtifact());
        componentBundle.setVersion(bundle.getVersion());
        return componentBundle;
    }

    private VersionedPropertyDescriptor createVersionedPropertyDescriptor(final PropertyDescriptor manifestDescriptor) {
        final VersionedPropertyDescriptor propertyDescriptor = new VersionedPropertyDescriptor();
        propertyDescriptor.setName(manifestDescriptor.getName());
        propertyDescriptor.setDisplayName(manifestDescriptor.getDisplayName());
        propertyDescriptor.setSensitive(manifestDescriptor.getSensitive());
        propertyDescriptor.setIdentifiesControllerService(manifestDescriptor.getTypeProvidedByValue() != null);
        return propertyDescriptor;
    }

    private ProcessorDefinition copyManifestProcessor(final ProcessorDefinition manifestProcessor) {
        final ProcessorDefinition procCopy = new ProcessorDefinition();

        // Copy base fields from DefinedType
        copyDefinedTypeFields(manifestProcessor, procCopy);

        // Copy fields from ExtensionComponent
        copyExtensionComponentFields(manifestProcessor, procCopy);

        // Copy other fields from Processor
        final LinkedHashMap<String, PropertyDescriptor> procDescriptors = new LinkedHashMap<>();
        if (manifestProcessor.getPropertyDescriptors() != null) {
            for (final Map.Entry<String, PropertyDescriptor> entry : manifestProcessor.getPropertyDescriptors().entrySet()) {
                procDescriptors.put(entry.getKey(), copyPropertyDescriptor(entry.getValue()));
            }
        }
        procCopy.setPropertyDescriptors(procDescriptors);

        procCopy.setSupportsDynamicProperties(manifestProcessor.getSupportsDynamicProperties());
        procCopy.setSupportsDynamicRelationships(manifestProcessor.getSupportsDynamicRelationships());

        final List<Relationship> supportedRelationships = new ArrayList<>();
        if (manifestProcessor.getSupportedRelationships() != null) {
            manifestProcessor.getSupportedRelationships().stream().forEach(r -> supportedRelationships.add(copyRelationship(r)));
        }
        procCopy.setSupportedRelationships(supportedRelationships);

        if (manifestProcessor.getInputRequirement() == null) {
            procCopy.setInputRequirement(InputRequirement.INPUT_ALLOWED);
        } else {
            procCopy.setInputRequirement(manifestProcessor.getInputRequirement());
        }

        return procCopy;
    }

    private Relationship copyRelationship(Relationship sourceRelationship) {
        final Relationship relationship = new Relationship();
        relationship.setName(sourceRelationship.getName());
        relationship.setDescription(relationship.getDescription());
        return relationship;
    }

    private BuildInfo copyBuildInfo(BuildInfo manifestBuildInfo) {
        final BuildInfo procBuildInfo = new BuildInfo();
        procBuildInfo.setVersion(manifestBuildInfo.getVersion());
        procBuildInfo.setRevision(manifestBuildInfo.getRevision());
        procBuildInfo.setTimestamp(manifestBuildInfo.getTimestamp());
        procBuildInfo.setTargetArch(manifestBuildInfo.getTargetArch());
        procBuildInfo.setCompiler(manifestBuildInfo.getCompiler());
        procBuildInfo.setCompilerFlags(manifestBuildInfo.getCompilerFlags());
        return procBuildInfo;
    }

    private DefinedType copyDefinedType(final DefinedType sourceDefinedType) {
        final DefinedType destDefinedType = new DefinedType();
        destDefinedType.setGroup(sourceDefinedType.getGroup());
        destDefinedType.setArtifact(sourceDefinedType.getArtifact());
        destDefinedType.setVersion(sourceDefinedType.getVersion());
        destDefinedType.setType(sourceDefinedType.getType());
        destDefinedType.setTypeDescription(sourceDefinedType.getTypeDescription());
        return destDefinedType;
    }

    private PropertyDescriptor copyPropertyDescriptor(final PropertyDescriptor sourceDescriptor) {
        final PropertyDescriptor destDescriptor = new PropertyDescriptor();
        destDescriptor.setName(sourceDescriptor.getName());
        destDescriptor.setDisplayName(sourceDescriptor.getDisplayName());
        destDescriptor.setDescription(sourceDescriptor.getDescription());

        if (sourceDescriptor.getAllowableValues() == null) {
            destDescriptor.setAllowableValues(null);
        } else {
            final List<PropertyAllowableValue> allowableValues = sourceDescriptor.getAllowableValues().stream()
                .map(this::copyAllowableValue)
                .collect(Collectors.toList());
            destDescriptor.setAllowableValues(allowableValues);
        }

        destDescriptor.setDefaultValue(sourceDescriptor.getDefaultValue());
        destDescriptor.setValidator(sourceDescriptor.getValidator());
        destDescriptor.setRequired(sourceDescriptor.getRequired());
        destDescriptor.setSensitive(sourceDescriptor.getSensitive());
        destDescriptor.setExpressionLanguageScope(sourceDescriptor.getExpressionLanguageScope());

        if (sourceDescriptor.getTypeProvidedByValue() != null) {
            final DefinedType procTypeProvidedByValue = copyDefinedType(sourceDescriptor.getTypeProvidedByValue());
            destDescriptor.setTypeProvidedByValue(procTypeProvidedByValue);
        }

        return destDescriptor;
    }

    private PropertyAllowableValue copyAllowableValue(final PropertyAllowableValue sourceAllowableValue) {
        final PropertyAllowableValue destAllowableValue = new PropertyAllowableValue();
        destAllowableValue.setDescription(sourceAllowableValue.getDescription());
        destAllowableValue.setDisplayName(sourceAllowableValue.getDisplayName());
        destAllowableValue.setValue(sourceAllowableValue.getValue());
        return destAllowableValue;
    }

    private void copyDefinedTypeFields(final DefinedType source, final DefinedType dest) {
        dest.setGroup(source.getGroup());
        dest.setArtifact(source.getArtifact());
        dest.setVersion(source.getVersion());
        dest.setType(source.getType());
        dest.setTypeDescription(source.getTypeDescription());
    }

    private void copyExtensionComponentFields(final ExtensionComponent source, final ExtensionComponent dest) {
        // Copy BuildInfo
        final BuildInfo manifestBuildInfo = source.getBuildInfo();
        if (manifestBuildInfo != null) {
            final BuildInfo procBuildInfo = copyBuildInfo(manifestBuildInfo);
            dest.setBuildInfo(procBuildInfo);
        }

        // Copy provided API implementations
        final List<DefinedType> procProvidedApiImpls = new ArrayList<>();
        if (source.getProvidedApiImplementations() != null) {
            for (final DefinedType manifestProvidedApiImpl : source.getProvidedApiImplementations()) {
                final DefinedType procProvidedApiImpl = copyDefinedType(manifestProvidedApiImpl);
                procProvidedApiImpls.add(procProvidedApiImpl);
            }
        }
        dest.setProvidedApiImplementations(procProvidedApiImpls);

        // Copy other fields from ExtensionComponent
        dest.setDeprecated(source.getDeprecated());
        dest.setDeprecationReason(source.getDeprecationReason());

        final Set<String> procTags = new HashSet<>();
        if (source.getTags() != null) {
            procTags.addAll(source.getTags());
        }
        dest.setTags(procTags);
    }

    private Set<FDComponentType> createSummaries(final AgentManifest agentManifest,
                                                 final Function<ComponentManifest,List<? extends ExtensionComponent>> getComponents) {
        final Set<FDComponentType> components = new TreeSet<>();
        if (agentManifest == null) {
            return components;
        }

        final List<com.cloudera.cem.efm.model.extension.Bundle> bundles = agentManifest.getBundles();
        if (bundles == null) {
            return components;
        }

        for (com.cloudera.cem.efm.model.extension.Bundle bundle : agentManifest.getBundles()) {
            final ComponentManifest bundleManifest = bundle.getComponentManifest();
            if (bundleManifest == null) {
                continue;
            }

            final List<? extends ExtensionComponent> bundleComponents = getComponents.apply(bundleManifest);
            if (bundleComponents == null) {
                continue;
            }

            final Set<FDComponentType> componentTypes = bundleComponents.stream()
                    .map(p -> createComponentSummary(agentManifest, bundle, p))
                    .collect(Collectors.toSet());

            components.addAll(componentTypes);
        }

        return components;
    }

    private FDComponentType createComponentSummary(final AgentManifest agentManifest,
                                                   final com.cloudera.cem.efm.model.extension.Bundle bundle,
                                                   final ExtensionComponent component) {
        return new FDComponentType.Builder()
                .artifact(bundle.getArtifact())
                .group(bundle.getGroup())
                .version(bundle.getVersion())
                .type(component.getType())
                .agentManifestId(agentManifest.getIdentifier())
                .description(component.getTypeDescription())
                .tags(component.getTags())
                .build();
    }

}
