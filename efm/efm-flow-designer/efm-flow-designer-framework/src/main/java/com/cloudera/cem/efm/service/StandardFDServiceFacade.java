package com.cloudera.cem.efm.service;

import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.component.FDComponent;
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.component.FDRevision;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlow;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlowContent;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.revision.FlowModification;
import com.cloudera.cem.efm.revision.InvalidRevisionException;
import com.cloudera.cem.efm.revision.Revision;
import com.cloudera.cem.efm.revision.RevisionClaim;
import com.cloudera.cem.efm.revision.RevisionManager;
import com.cloudera.cem.efm.revision.RevisionUpdate;
import com.cloudera.cem.efm.revision.StandardRevisionClaim;
import com.cloudera.cem.efm.revision.StandardRevisionUpdate;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.NiFiUserUtils;
import com.cloudera.cem.efm.service.component.FDComponentService;
import com.cloudera.cem.efm.service.component.FDConnectionService;
import com.cloudera.cem.efm.service.component.FDControllerServiceService;
import com.cloudera.cem.efm.service.component.FDFunnelService;
import com.cloudera.cem.efm.service.component.FDProcessGroupFlowService;
import com.cloudera.cem.efm.service.component.FDProcessorService;
import com.cloudera.cem.efm.service.component.FDRemoteProcessGroupService;
import com.cloudera.cem.efm.service.flow.FDFlowService;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.Bundle;
import org.apache.nifi.registry.flow.VersionedComponent;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
class StandardFDServiceFacade implements FDServiceFacade {

    private final FDProcessorService processorService;
    private final FDControllerServiceService controllerServiceService;
    private final FDConnectionService connectionService;
    private final FDFunnelService funnelService;
    private final FDRemoteProcessGroupService remoteProcessGroupService;
    private final FDProcessGroupFlowService processGroupFlowService;
    private final FDFlowService flowService;
    private final RevisionManager revisionManager;

    /**
     * These locks are used for operations that are global and not specific to an existing flow.
     *
     * For example, creating a flow would be guarded by global write lock.
     */
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock globalReadLock = globalLock.readLock();
    private final Lock globalWriteLock = globalLock.writeLock();

    /**
     * These locks guard operations against individual flows.
     */
    private final Map<String,ReadWriteLock> flowLocks = new HashMap<>();

    @Autowired
    public StandardFDServiceFacade(
            final FDProcessorService processorService,
            final FDControllerServiceService controllerServiceService,
            final FDConnectionService connectionService,
            final FDFunnelService funnelService,
            final FDRemoteProcessGroupService remoteProcessGroupService,
            final FDProcessGroupFlowService processGroupFlowService,
            final FDFlowService flowService,
            final RevisionManager revisionManager) {
        this.processorService = processorService;
        this.controllerServiceService = controllerServiceService;
        this.connectionService = connectionService;
        this.funnelService = funnelService;
        this.remoteProcessGroupService = remoteProcessGroupService;
        this.processGroupFlowService = processGroupFlowService;
        this.flowService = flowService;
        this.revisionManager = revisionManager;
    }

    // ------------- Flow Methods -----------------

    @Override
    public FDFlowMetadata getFlowMetadata(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> {
            return flowService.getFlowMetadata(fdFlowId);
        });
    }

    @Override
    public FDFlow getFlow(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> {
            return flowService.getFlow(fdFlowId);
        });
    }

    @Override
    public FDFlowMetadata deleteFlow(String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowWriteLock(fdFlowId, () -> {
            return flowService.deleteFlow(fdFlowId);
        });
    }

    @Override
    public List<FDFlowEvent> getFlowEvents(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> {
            return flowService.getFlowEvents(fdFlowId);
        });
    }

    @Override
    public FDFlowMetadata createFlow(final String agentClass, final NiFiUser user) {
        if (StringUtils.isBlank(agentClass)) {
            throw new IllegalArgumentException("Agent class cannot be null or blank");
        }

        return withGlobalWriteLock(() -> {
            return flowService.createFlow(agentClass, user);
        });
    }

    @Override
    public List<FDFlowMetadata> getAvailableFlows() {
        return withGlobalReadLock(() -> {
            return flowService.getAvailableFlows();
        });
    }

    @Override
    public List<FDFlowSummary> getFlowSummaries() {
        return withGlobalReadLock(() -> {
            return flowService.getFlowSummaries();
        });
    }

    @Override
    public FDVersionInfo publishFlow(final String fdFlowId, final FDFlowPublishMetadata publishMetadata) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (publishMetadata == null) {
            throw new IllegalArgumentException("Publish metadata cannot be null");
        }

        return withFlowWriteLock(fdFlowId, () -> {
            return flowService.publishFlow(fdFlowId, publishMetadata);
        });
    }

    @Override
    public FDVersionInfo revertFlowToLastPublishedState(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowWriteLock(fdFlowId, () -> {
            return flowService.revertFlowToLastPublishedState(fdFlowId);
        });
    }

    @Override
    public FDVersionInfo revertFlowToFlowRevision(final String fdFlowId, final BigInteger flowRevision) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (flowRevision == null) {
            throw new IllegalArgumentException("Flow revision cannot be null");
        }

        return withFlowWriteLock(fdFlowId, () -> {
            return flowService.revertFlowToFlowRevision(fdFlowId, flowRevision);
        });
    }

    @Override
    public FDVersionInfo getFlowVersionInfo(String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> {
            return flowService.getFlowVersionInfo(fdFlowId);
        });
    }

    @Override
    public ELSpecification getELSpecification(String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> {
            return flowService.getELSpecification(fdFlowId);
        });
    }

    // ------------- Process Group Flow Methods -----------------

    @Override
    public FDProcessGroupFlow getProcessGroup(final String flowId, final String processGroupId, final boolean includeChildren) {
        validateGetRequest(flowId, processGroupId);

        return withFlowReadLock(flowId, () -> {
            final FDProcessGroupFlow processGroupFlow = processGroupFlowService.get(flowId, processGroupId, includeChildren);

            final FDProcessGroupFlowContent flowContent = processGroupFlow.getFlowContent();
            populateRevisions(flowContent);

            return processGroupFlow;
        });
    }

    private void populateRevisions(final FDProcessGroupFlowContent processGroup) {
        final Revision processGroupRevision = revisionManager.getRevision(processGroup.getIdentifier());
        final FDRevision fdProcessGroupRevision = createFDRevision(processGroupRevision);
        processGroup.setRevision(fdProcessGroupRevision);

        processGroup.getProcessors().stream().forEach(p -> {
            final Revision revision = revisionManager.getRevision(p.getComponentConfiguration().getIdentifier());
            p.setRevision(createFDRevision(revision));
        });

        processGroup.getControllerServices().stream().forEach(p -> {
            final Revision revision = revisionManager.getRevision(p.getComponentConfiguration().getIdentifier());
            p.setRevision(createFDRevision(revision));
        });

        processGroup.getFunnels().stream().forEach(p -> {
            final Revision revision = revisionManager.getRevision(p.getComponentConfiguration().getIdentifier());
            p.setRevision(createFDRevision(revision));
        });

        processGroup.getConnections().stream().forEach(p -> {
            final Revision revision = revisionManager.getRevision(p.getComponentConfiguration().getIdentifier());
            p.setRevision(createFDRevision(revision));
        });

        processGroup.getRemoteProcessGroups().stream().forEach(p -> {
            final Revision revision = revisionManager.getRevision(p.getComponentConfiguration().getIdentifier());
            p.setRevision(createFDRevision(revision));
        });

        if (processGroup.getProcessGroups() != null) {
            processGroup.getProcessGroups().stream().forEach(p -> {
                populateRevisions(p);
            });
        }
    }


    // ------------- Processor Methods -----------------

    @Override
    public FDComponentTypes getProcessorTypes(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> processorService.getProcessorTypes(fdFlowId));
    }

    @Override
    public FDProcessor createProcessor(final String flowId, final String processGroupId, final FDProcessor requestProcessor) {
        validateCreateRequest(flowId, processGroupId, requestProcessor);

        final VersionedProcessor requestComponentConfig = requestProcessor.getComponentConfiguration();
        validateTypeAndBundle(requestComponentConfig.getType(), requestComponentConfig.getBundle());

        return createComponent(flowId, processGroupId, requestProcessor, processorService);
    }

    @Override
    public FDProcessor getProcessor(final String flowId, final String processorId) {
        validateGetRequest(flowId, processorId);
        return getComponent(flowId, processorId, processorService);
    }

    @Override
    public FDProcessor updateProcessor(final String flowId, final String processorId, final FDProcessor requestProcessor) {
        validateUpdateRequest(flowId, processorId, requestProcessor);
        return updateComponent(flowId, requestProcessor, processorService);
    }

    @Override
    public FDProcessor deleteProcessor(final String flowId, final String processorId, final FDRevision requestRevision) {
        validateDeleteRequest(flowId, processorId, requestRevision);
        return deleteComponent(flowId, processorId, requestRevision, processorService);
    }

    @Override
    public FDPropertyDescriptor getProcessorPropertyDescriptor(final String flowId, final String processorId, final String propertyName) {
        validateGetRequest(flowId, processorId);

        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("Property name cannot be blank or null");
        }

        return withFlowReadLock(flowId, () -> processorService.getPropertyDescriptor(flowId, processorId, propertyName));
    }

    // ------------- Controller Service Methods -----------------

    @Override
    public FDComponentTypes getControllerServiceTypes(final String fdFlowId) {
        if (StringUtils.isBlank(fdFlowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        return withFlowReadLock(fdFlowId, () -> controllerServiceService.getControllerServiceTypes(fdFlowId));
    }

    @Override
    public FDControllerService createControllerService(final String flowId, final String processGroupId,
                                                       final FDControllerService requestControllerService) {

        validateCreateRequest(flowId, processGroupId, requestControllerService);

        final VersionedControllerService requestComponentConfig = requestControllerService.getComponentConfiguration();
        validateTypeAndBundle(requestComponentConfig.getType(), requestComponentConfig.getBundle());

        return createComponent(flowId, processGroupId, requestControllerService, controllerServiceService);
    }

    @Override
    public FDControllerService getControllerService(final String flowId, final String controllerServiceId) {
        validateGetRequest(flowId, controllerServiceId);
        return getComponent(flowId, controllerServiceId, controllerServiceService);
    }

    @Override
    public Set<FDControllerService> getControllerServices(final String flowId, final String processGroupId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (StringUtils.isBlank(processGroupId)) {
            throw new IllegalArgumentException("Process group id cannot be null or blank");
        }

        return withFlowReadLock(flowId, () -> {
            final Set<FDControllerService> controllerServices = controllerServiceService.getForProcessGroup(flowId, processGroupId);
            controllerServices.stream().forEach(cs -> {
                final String componentId = cs.getComponentConfiguration().getIdentifier();
                final Revision revision = revisionManager.getRevision(componentId);
                final FDRevision fdRevision = createFDRevision(revision);
                cs.setRevision(fdRevision);
            });
            return controllerServices;
        });
    }

    @Override
    public FDControllerService updateControllerService(final String flowId, final String controllerServiceId,
                                                       final FDControllerService requestControllerService) {
        validateUpdateRequest(flowId, controllerServiceId, requestControllerService);
        return updateComponent(flowId, requestControllerService, controllerServiceService);
    }

    @Override
    public FDControllerService deleteControllerService(final String flowId, final String controllerServiceId, final FDRevision requestRevision) {
        validateDeleteRequest(flowId, controllerServiceId, requestRevision);
        return deleteComponent(flowId, controllerServiceId, requestRevision, controllerServiceService);
    }

    @Override
    public FDPropertyDescriptor getControllerServicePropertyDescriptor(final String flowId, final String controllerServiceId, final String propertyName) {
        validateGetRequest(flowId, controllerServiceId);

        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("Property name cannot be blank or null");
        }

        return withFlowReadLock(flowId, () -> controllerServiceService.getPropertyDescriptor(flowId, controllerServiceId, propertyName));
    }

    // ------------- Connection Methods -----------------

    @Override
    public FDConnection createConnection(final String flowId, final String processGroupId, final FDConnection requestConnection) {
        validateCreateRequest(flowId, processGroupId, requestConnection);
        return createComponent(flowId, processGroupId, requestConnection, connectionService);
    }

    @Override
    public FDConnection getConnection(final String flowId, final String connectionId) {
        validateGetRequest(flowId, connectionId);
        return getComponent(flowId, connectionId, connectionService);
    }

    @Override
    public FDConnection updateConnection(final String flowId, final String connectionId, final FDConnection requestConnection) {
        validateUpdateRequest(flowId, connectionId, requestConnection);
        return updateComponent(flowId, requestConnection, connectionService);
    }

    @Override
    public FDConnection deleteConnection(final String flowId, final String connectionId, final FDRevision requestRevision) {
        validateDeleteRequest(flowId, connectionId, requestRevision);
        return deleteComponent(flowId, connectionId, requestRevision, connectionService);
    }

    // ------------- Funnel Methods -----------------

    @Override
    public FDFunnel createFunnel(final String flowId, final String processGroupId, final FDFunnel requestFunnel) {
        validateCreateRequest(flowId, processGroupId, requestFunnel);
        return createComponent(flowId, processGroupId, requestFunnel, funnelService);
    }

    @Override
    public FDFunnel getFunnel(final String flowId, final String funnelId) {
        validateGetRequest(flowId, funnelId);
        return getComponent(flowId, funnelId, funnelService);
    }

    @Override
    public FDFunnel updateFunnel(final String flowId, final String funnelId, final FDFunnel requestFunnel) {
        validateUpdateRequest(flowId, funnelId, requestFunnel);
        return updateComponent(flowId, requestFunnel, funnelService);
    }

    @Override
    public FDFunnel deleteFunnel(final String flowId, final String funnelId, final FDRevision requestRevision) {
        validateDeleteRequest(flowId, funnelId, requestRevision);
        return deleteComponent(flowId, funnelId, requestRevision, funnelService);
    }

    // ------------- RPG Methods -----------------

    @Override
    public FDRemoteProcessGroup createRemoteProcessGroup(final String flowId, final String processGroupId,
                                                         final FDRemoteProcessGroup requestRemoteProcessGroup) {
        validateCreateRequest(flowId, processGroupId, requestRemoteProcessGroup);
        return createComponent(flowId, processGroupId, requestRemoteProcessGroup, remoteProcessGroupService);
    }

    @Override
    public FDRemoteProcessGroup getRemoteProcessGroup(final String flowId, final String remoteProcessGroupId) {
        validateGetRequest(flowId, remoteProcessGroupId);
        return getComponent(flowId, remoteProcessGroupId, remoteProcessGroupService);
    }

    @Override
    public FDRemoteProcessGroup updateRemoteProcessGroup(final String flowId, final String remoteProcessGroupId,
                                                         final FDRemoteProcessGroup requestRemoteProcessGroup) {
        validateUpdateRequest(flowId, remoteProcessGroupId, requestRemoteProcessGroup);
        return updateComponent(flowId, requestRemoteProcessGroup, remoteProcessGroupService);
    }

    @Override
    public FDRemoteProcessGroup deleteRemoteProcessGroup(final String flowId, final String remoteProcessGroupId,
                                                         final FDRevision requestRevision) {
        validateDeleteRequest(flowId, remoteProcessGroupId, requestRevision);
        return deleteComponent(flowId, remoteProcessGroupId, requestRevision, remoteProcessGroupService);
    }

    // ------------- Generic Component CRUD Methods -----------------

    private <V extends VersionedComponent, F extends FDComponent<V>> F createComponent(
            final String flowId,
            final String processGroupId,
            final F requestComponent,
            final FDComponentService<V,F> componentService) {

        // Generate an id so we can create a revision
        final V requestComponentConfig = requestComponent.getComponentConfiguration();
        requestComponentConfig.setIdentifier(UUID.randomUUID().toString());

        final Revision revision = getRevision(requestComponent, requestComponentConfig.getIdentifier());

        return withFlowWriteLock(flowId, revision, () -> {
            final NiFiUser user = NiFiUserUtils.getNiFiUser();
            final RevisionClaim claim = new StandardRevisionClaim(revision);

            final RevisionUpdate<F> revisionUpdate = revisionManager.updateRevision(claim, user, () -> {
                final F createdComponent = componentService.create(flowId, processGroupId, requestComponentConfig);
                final FlowModification lastMod = new FlowModification(revision.incrementRevision(revision.getClientId()), user.getIdentity());
                return new StandardRevisionUpdate<>(createdComponent, lastMod);
            });

            final FDRevision resultRevision = createFDRevision(revisionUpdate.getLastModification());
            final F resultComponent = revisionUpdate.getComponent();
            resultComponent.setRevision(resultRevision);
            return resultComponent;
        });
    }

    private <V extends VersionedComponent, F extends FDComponent<V>> F getComponent(
            final String flowId,
            final String componentId,
            final FDComponentService<V,F> componentService) {
        return withFlowReadLock(flowId, () -> {
            final F resultComponent = componentService.get(flowId, componentId);
            final Revision revision = revisionManager.getRevision(componentId);
            final FDRevision fdRevision = createFDRevision(revision);
            resultComponent.setRevision(fdRevision);
            return resultComponent;
        });
    }

    private <V extends VersionedComponent, F extends FDComponent<V>> F updateComponent(
            final String flowId,
            final F requestComponent,
            final FDComponentService<V,F> componentService) {

        final V requestComponentConfig = requestComponent.getComponentConfiguration();
        final String componentId = requestComponentConfig.getIdentifier();
        final Revision revision = getRevision(requestComponent, componentId);

        return withFlowWriteLock(flowId, revision, () -> {
            final NiFiUser user = NiFiUserUtils.getNiFiUser();
            final RevisionClaim claim = new StandardRevisionClaim(revision);

            // Want to verify the flow and component exist before calling updateRevision, maybe a better way to do this
            final F existingComponent = componentService.get(flowId, componentId);

            final RevisionUpdate<F> revisionUpdate = revisionManager.updateRevision(claim, user, () -> {
                final F updatedComponent = componentService.update(flowId, requestComponentConfig);
                final Revision updatedRevision = revisionManager.getRevision(revision.getComponentId()).incrementRevision(revision.getClientId());
                final FlowModification lastMod = new FlowModification(updatedRevision, user.getIdentity());
                return new StandardRevisionUpdate<>(updatedComponent, lastMod);
            });

            final FDRevision resultRevision = createFDRevision(revisionUpdate.getLastModification());
            final F resultComponent = revisionUpdate.getComponent();
            resultComponent.setRevision(resultRevision);
            return resultComponent;
        });
    }

    private <V extends VersionedComponent, F extends FDComponent<V>> F deleteComponent(
            final String flowId,
            final String componentId,
            final FDRevision requestRevision,
            final FDComponentService<V,F> componentService) {

        final Revision revision = getRevision(requestRevision, componentId);

        return withFlowWriteLock(flowId, revision, () -> {
            final NiFiUser user = NiFiUserUtils.getNiFiUser();
            final RevisionClaim claim = new StandardRevisionClaim(revision);

            // Want to verify the flow and component exist before calling deleteRvision
            final F existingComponent = componentService.get(flowId, componentId);

            final F deletedComponent = revisionManager.deleteRevision(claim, user, () -> {
                componentService.delete(flowId, componentId);
                return existingComponent;
            });

            deletedComponent.setRevision(requestRevision);
            return deletedComponent;
        });

    }

    // ------------- Helper Methods -----------------

    private void validateCreateRequest(final String flowId, final String processGroupId, final FDComponent<?> requestComponent) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (StringUtils.isBlank(processGroupId)) {
            throw new IllegalArgumentException("Process group id cannot be null or blank");
        }

        if (requestComponent == null || requestComponent.getComponentConfiguration() == null) {
            throw new IllegalArgumentException("Component details must be specified.");
        }

        if (requestComponent.getRevision() == null || (requestComponent.getRevision().getVersion() == null || requestComponent.getRevision().getVersion() != 0)) {
            throw new IllegalArgumentException("A revision of 0 must be specified when creating a new component.");
        }

        final VersionedComponent requestComponentConfig = requestComponent.getComponentConfiguration();
        if (requestComponentConfig.getIdentifier() != null) {
            throw new IllegalArgumentException("Component ID cannot be specified.");
        }

        if (requestComponentConfig.getGroupIdentifier() != null && !requestComponentConfig.getGroupIdentifier().equals(processGroupId)) {
            throw new IllegalArgumentException("Cannot specify a group id in the component configuration different from the group where component is being added");
        }
    }

    private void validateTypeAndBundle(final String type, final Bundle requestBundle) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("The type of component to create must be specified.");
        }

        if (requestBundle == null
                || StringUtils.isBlank(requestBundle.getGroup())
                || StringUtils.isBlank(requestBundle.getArtifact())
                || StringUtils.isBlank(requestBundle.getVersion())) {
            throw new IllegalArgumentException("Bundle information is required");
        }
    }

    private void validateGetRequest(final String flowId, final String componentId) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (StringUtils.isBlank(componentId)) {
            throw new IllegalArgumentException("Component id cannot be null or blank");
        }
    }

    private void validateUpdateRequest(final String flowId, final String componentId, final FDComponent<?> requestComponent) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (StringUtils.isBlank(componentId)) {
            throw new IllegalArgumentException("Component id cannot be null or blank");
        }

        if (requestComponent == null || requestComponent.getComponentConfiguration() == null) {
            throw new IllegalArgumentException("Component details must be specified.");
        }

        if (requestComponent.getRevision() == null) {
            throw new IllegalArgumentException("Revision must be specified.");
        }

        final VersionedComponent requestComponentConfig = requestComponent.getComponentConfiguration();
        if (requestComponentConfig.getIdentifier() == null) {
            requestComponentConfig.setIdentifier(componentId);
        }

        if (!componentId.equals(requestComponentConfig.getIdentifier())) {
            throw new IllegalArgumentException("Component id in path must match component id in component configuration entity");
        }
    }

    private void validateDeleteRequest(final String flowId, final String componentId, final FDRevision requestRevision) {
        if (StringUtils.isBlank(flowId)) {
            throw new IllegalArgumentException("Flow id cannot be null or blank");
        }

        if (StringUtils.isBlank(componentId)) {
            throw new IllegalArgumentException("Component id cannot be null or blank");
        }

        if (requestRevision == null) {
            throw new IllegalArgumentException("Revision cannot be null");
        }
    }

    private Revision getRevision(final FDRevision revision, final String componentId) {
        return new Revision(revision.getVersion(), revision.getClientId(), componentId);
    }

    private Revision getRevision(final FDComponent entity, final String componentId) {
        return getRevision(entity.getRevision(), componentId);
    }

    private FDRevision createFDRevision(final FlowModification lastMod) {
        final Revision revision = lastMod.getRevision();

        final FDRevision fdRevision = createFDRevision(revision);
        fdRevision.setLastModifier(lastMod.getLastModifier());
        return fdRevision;
    }

    private FDRevision createFDRevision(final Revision revision) {
        final FDRevision fdRevision = new FDRevision();
        fdRevision.setVersion(revision.getVersion());
        fdRevision.setClientId(revision.getClientId());
        return fdRevision;
    }

    public void verifyRevision(final Revision revision) throws InvalidRevisionException {
        final Revision curRevision = revisionManager.getRevision(revision.getComponentId());
        if (revision.equals(curRevision)) {
            return;
        }

        throw new InvalidRevisionException(revision + " is not the most up-to-date revision. This component appears to have been modified");
    }

    public void verifyRevisions(final Set<Revision> revisions) {
        for (final Revision revision : revisions) {
            verifyRevision(revision);
        }
    }

    private <R> R withGlobalReadLock(FDServiceFacadeAction<R> action) {
        globalReadLock.lock();
        try {
            return action.execute();
        } finally {
            globalReadLock.unlock();
        }
    }

    private <R> R withGlobalWriteLock(FDServiceFacadeAction<R> action) {
        globalWriteLock.lock();
        try {
            return action.execute();
        } finally {
            globalWriteLock.unlock();
        }
    }

    private <R> R withFlowReadLock(String flowId, FDServiceFacadeAction<R> action) {
        final ReadWriteLock flowLock = getFlowLock(flowId);
        flowLock.readLock().lock();
        try {
            return action.execute();
        } finally {
            flowLock.readLock().unlock();
        }
    }

    private <R> R withFlowWriteLock(String flowId, Revision revision, FDServiceFacadeAction<R> action) {
        final ReadWriteLock flowLock = getFlowLock(flowId);
        flowLock.writeLock().lock();
        try {
            verifyRevision(revision);
            return action.execute();
        } finally {
            flowLock.writeLock().unlock();
        }
    }

    private <R> R withFlowWriteLock(String flowId, FDServiceFacadeAction<R> action) {
        final ReadWriteLock flowLock = getFlowLock(flowId);
        flowLock.writeLock().lock();
        try {
            return action.execute();
        } finally {
            flowLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the lock instance for a flow, or lazily creates one.
     *
     * NOTE: This method does not actually lock anything, it just retrieves the lock instance and the caller must
     * call lock and unlock appropriately.
     *
     * @param flowId the flow id
     * @return the ReadWriteLock for the flow id
     */
    private ReadWriteLock getFlowLock(final String flowId) {
        ReadWriteLock flowLock = flowLocks.get(flowId);
        if (flowLock == null) {
            synchronized (this) {
                flowLock = flowLocks.get(flowId);
                if (flowLock == null) {
                    flowLock = new ReentrantReadWriteLock();
                    flowLocks.put(flowId, flowLock);
                }
            }
        }
        return flowLock;
    }

    /**
     * An internal action in this service.
     *
     * @param <R> the return type of the action
     */
    private interface FDServiceFacadeAction<R> {

        R execute();

    }

}
