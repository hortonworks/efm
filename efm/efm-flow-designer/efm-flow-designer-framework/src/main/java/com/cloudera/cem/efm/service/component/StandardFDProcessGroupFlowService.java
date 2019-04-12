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
package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.extension.ControllerServiceDefinition;
import com.cloudera.cem.efm.model.extension.ProcessorDefinition;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlow;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlowContent;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.service.validation.Validator;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.ControllerServiceValidationContext;
import com.cloudera.cem.efm.validation.ProcessorValidationContext;
import org.apache.nifi.registry.flow.Position;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDProcessGroupFlowService implements FDProcessGroupFlowService {

    public static final String ROOT_GROUP_ID = "root";

    private final FDFlowManager flowManager;
    private final FDProcessorService processorService;
    private final FDControllerServiceService controllerServiceService;
    private final ValidationService validationService;
    private final ControllerServiceLookup serviceLookup;

    @Autowired
    public StandardFDProcessGroupFlowService(final FDFlowManager flowManager,
                                             final FDProcessorService processorService,
                                             final FDControllerServiceService controllerServiceService,
                                             final ValidationService validationService,
                                             final ControllerServiceLookup serviceLookup) {
        this.flowManager = flowManager;
        this.processorService = processorService;
        this.controllerServiceService = controllerServiceService;
        this.validationService = validationService;
        this.serviceLookup = serviceLookup;
    }

    @Override
    public FDProcessGroupFlow get(final String flowId, final String processGroupId, final boolean includeChildren) {
        // Locate the flow where the process group is located
        final Optional<FDFlow> flowOptional = flowManager.getFlow(flowId);
        if (!flowOptional.isPresent()) {
            throw new ResourceNotFoundException("A flow does not exist with the given id");
        }

        final FDFlow flow = flowOptional.get();
        final FDFlowMetadata flowMetadata = flow.getFlowMetadata();
        final VersionedProcessGroup flowContents = flow.getFlowContent();
        final FDVersionInfo versionInfo = flow.getVersionInfo();
        final BigInteger localFlowRevision = flow.getLocalFlowRevision();

        // Locate the requested process group with in the flow, or throw an exception
        final VersionedProcessGroup versionedProcessGroup = ROOT_GROUP_ID.equals(processGroupId)
                ? flowContents : ComponentUtils.getProcessGroupOrNotFound(processGroupId, flowContents);

        // Convert the VersionedProcessGroup to an FDProcessGroupFlowContent object
        final FDProcessGroupFlowContent processGroupFlowConfig = createProcessGroupFlowConfig(flow, versionedProcessGroup, includeChildren);

        // Create the overall process group flow to wrap metadata and content
        final FDProcessGroupFlow processGroupFlow = new FDProcessGroupFlow();
        processGroupFlow.setFlowContent(processGroupFlowConfig);
        processGroupFlow.setFlowMetadata(flowMetadata);
        processGroupFlow.setVersionInfo(versionInfo);
        processGroupFlow.setLocalFlowRevision(localFlowRevision);
        return  processGroupFlow;
    }

    private FDProcessGroupFlowContent createProcessGroupFlowConfig(
            final FDFlow flow, final VersionedProcessGroup source, final boolean includeChildren) {

        final String agentClass = flow.getFlowMetadata().getAgentClass();
        final FDProcessGroupFlowContent destination = new FDProcessGroupFlowContent();

        // Copy over base fields from VersionedComponents...

        destination.setIdentifier(source.getIdentifier());
        destination.setGroupIdentifier(source.getGroupIdentifier());
        destination.setName(source.getName());
        destination.setComments(source.getComments());

        if (source.getPosition() != null) {
            destination.setPosition(new Position(source.getPosition().getX(), source.getPosition().getY()));
        } else {
            destination.setPosition(new Position(0, 0));
        }

        // Determine all the ancestor controller services between (and including) the current group and the root group
        // We do this once for the whole group rather doing it over and over for each component in the group
        final Set<VersionedControllerService> ancestorControllerServices = ComponentUtils.getAncestorControllerServices(
                source.getGroupIdentifier(), flow.getFlowContent());

        // Map the sets of components into the types needed for VersionedProcessGroup...
        final Set<FDProcessor> processors = source.getProcessors().stream()
                .map(p -> {
                    final ProcessorDefinition definition = processorService.getComponentDefinition(agentClass, p);

                    final FDProcessor processor = new FDProcessor();
                    processor.setComponentConfiguration(p);
                    processor.setComponentDefinition(definition);

                    final String flowId = flow.getFlowMetadata().getIdentifier();
                    final ProcessorValidationContext validationContext = new ProcessorValidationContext(processor.getComponentDefinition());
                    final Validator validator = () -> validationContext.validate(processor.getComponentConfiguration(), source, flowId, serviceLookup);
                    final Collection<String> validationErrors = validationService.getValidationErrors(p.getIdentifier(), validator);
                    processor.setValidationErrors(validationErrors);

                    ComponentUtils.populateControllerServicePropertyDescriptors(definition, ancestorControllerServices);
                    return processor;
                })
                .collect(Collectors.toSet());
        destination.setProcessors(processors);

        final Set<FDRemoteProcessGroup> remoteProcessGroups = source.getRemoteProcessGroups().stream()
                .map(p -> {
                    final FDRemoteProcessGroup remoteProcessGroup = new FDRemoteProcessGroup();
                    remoteProcessGroup.setComponentConfiguration(p);
                    return remoteProcessGroup;
                })
                .collect(Collectors.toSet());
        destination.setRemoteProcessGroups(remoteProcessGroups);

        final Set<FDConnection> connections = source.getConnections().stream()
                .map(p -> {
                    final FDConnection connection = new FDConnection();
                    connection.setComponentConfiguration(p);
                    return connection;
                })
                .collect(Collectors.toSet());
        destination.setConnections(connections);

        final Set<FDFunnel> funnels = source.getFunnels().stream()
                .map(p -> {
                    final FDFunnel funnel = new FDFunnel();
                    funnel.setComponentConfiguration(p);
                    return funnel;
                })
                .collect(Collectors.toSet());
        destination.setFunnels(funnels);

        final Set<FDControllerService> controllerServices = source.getControllerServices().stream()
                .map(p -> {
                    final ControllerServiceDefinition definition = controllerServiceService.getComponentDefinition(agentClass, p);

                    final FDControllerService controllerService = new FDControllerService();
                    controllerService.setComponentConfiguration(p);
                    controllerService.setComponentDefinition(definition);

                    final String flowId = flow.getFlowMetadata().getIdentifier();
                    final ControllerServiceValidationContext validationContext = new ControllerServiceValidationContext(definition);
                    final Validator validator = () -> validationContext.validate(controllerService.getComponentConfiguration(), source, flowId, serviceLookup);
                    final Collection<String> validationErrors = validationService.getValidationErrors(p.getIdentifier(), validator);
                    controllerService.setValidationErrors(validationErrors);

                    ComponentUtils.populateControllerServicePropertyDescriptors(definition, ancestorControllerServices);
                    return controllerService;
                })
                .collect(Collectors.toSet());
        destination.setControllerServices(controllerServices);

        // Recursively map the process groups...

        if (includeChildren) {
            final Set<FDProcessGroupFlowContent> processGroups = source.getProcessGroups().stream()
                    .map(p -> createProcessGroupFlowConfig(flow, p, includeChildren))
                    .collect(Collectors.toSet());
            destination.setProcessGroups(processGroups);
        } else {
            destination.setProcessGroups(new HashSet<>());
        }

        return destination;
    }
}
