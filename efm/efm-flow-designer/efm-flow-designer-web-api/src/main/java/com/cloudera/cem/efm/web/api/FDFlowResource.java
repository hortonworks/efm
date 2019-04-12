/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.web.api;

import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.ListContainer;
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
import com.cloudera.cem.efm.model.flow.FDVersionInfoResult;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.service.FDServiceFacade;
import com.cloudera.cem.efm.web.api.request.ClientIdParameter;
import com.cloudera.cem.efm.web.api.request.LongParameter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedProcessor;
import org.apache.nifi.registry.flow.VersionedRemoteProcessGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Path("/designer")
@Api(value = "Flow Designer", description = "Design flows to deploy to agents.")
public class FDFlowResource extends FDBaseResource {

    public static final String VERSION = "version";
    public static final String CLIENT_ID = "clientId";

    @Autowired
    public FDFlowResource(final FDServiceFacade fdServiceFacade) {
        super(fdServiceFacade);
    }

    @GET
    @Path("/client-id")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Gets a client id to use with the designer endpoints",
            response = String.class
    )
    public Response getClientId() {
        return Response.ok(UUID.randomUUID().toString()).build();
    }

    @GET
    @Path("/flows")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the available flows known to the flow designer",
            response = ListContainer.class
    )
    public Response getFlows() {
        final List<FDFlowMetadata> flows = fdServiceFacade.getAvailableFlows();
        final ListContainer<FDFlowMetadata> listContainer = new ListContainer<>(flows);
        return Response.ok(listContainer).build();
    }

    @GET
    @Path("/flows/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the summaries of the available flows known to the flow designer",
            response = ListContainer.class
    )
    public Response getFlowSummaries() {
        final List<FDFlowSummary> flows = fdServiceFacade.getFlowSummaries();
        final ListContainer<FDFlowSummary> listContainer = new ListContainer<>(flows);
        return Response.ok(listContainer).build();
    }

    @GET
    @Path("/flows/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the flow with the given id",
            response = FDFlow.class
    )
    public Response getFlow(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId
    ) {
        final FDFlow flow = fdServiceFacade.getFlow(flowId);
        return Response.ok(flow).build();
    }

    @DELETE
    @Path("/flows/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the flow with the given id",
            response = FDFlowMetadata.class
    )
    public Response deleteFlow(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
            final String flowId
    ) {
        final FDFlowMetadata flow = fdServiceFacade.deleteFlow(flowId);
        return Response.ok(flow).build();
    }

    @GET
    @Path("/flows/{flowId}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the flow metadata for the flow with the given id",
            response = FDFlowMetadata.class
    )
    public Response getFlowMetadata(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId
    ) {
        final FDFlowMetadata flowMetadata = fdServiceFacade.getFlowMetadata(flowId);
        return Response.ok(flowMetadata).build();
    }

    @GET
    @Path("/flows/{flowId}/version-info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the version info for the flow with the given id",
            response = FDVersionInfoResult.class
    )
    public Response getFlowVersionInfo(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId
    ) {
        final FDVersionInfo versionInfo = fdServiceFacade.getFlowVersionInfo(flowId);

        final FDVersionInfoResult versionInfoResult = new FDVersionInfoResult();
        versionInfoResult.setVersionInfo(versionInfo);

        return Response.ok(versionInfoResult).build();
    }

    @GET
    @Path("/flows/{flowId}/expression-language-spec")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the expression language specification for the agent class of the given flow", response= ELSpecification.class)
    public Response getELSpecification(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
            final String flowId
    ) {
        final ELSpecification specification = fdServiceFacade.getELSpecification(flowId);
        return Response.ok(specification).build();
    }

    @GET
    @Path("/flows/{flowId}/types/processors")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the available processor types for the given flow",
            response = FDComponentTypes.class
    )
    public Response getProcessorTypes(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId
    ) {
        final FDComponentTypes processorTypes = fdServiceFacade.getProcessorTypes(flowId);
        return Response.ok(processorTypes).build();
    }

    @GET
    @Path("/flows/{flowId}/types/controller-services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the available controller service types for the given flow",
            response = FDComponentTypes.class
    )
    public Response getControllerServiceTypes(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId
    ) {
        final FDComponentTypes controllerServiceTypes = fdServiceFacade.getControllerServiceTypes(flowId);
        return Response.ok(controllerServiceTypes).build();
    }

    @POST
    @Path("/flows/{flowId}/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Publishes the current state of the flow to NiFi Registry",
            response = FDVersionInfo.class
    )
    public Response publishFlow(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @ApiParam(value = "The metadata for publishing the flow, such as comments", required = false)
                final FDFlowPublishMetadata publishMetadata
    ) throws FlowPublisherException {
        final FDVersionInfo versionInfo = fdServiceFacade.publishFlow(flowId, publishMetadata == null ? new FDFlowPublishMetadata() : publishMetadata);
        return Response.ok(versionInfo).build();
    }

    @POST
    @Path("/flows/{flowId}/revert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Reverts the current state of the flow to the last published version. If the flow has never been published, " +
                    "or has no changes to publish, then this will be a no-op.",
            response = FDVersionInfo.class
    )
    public Response revertFlow(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
            final String flowId) throws FlowPublisherException {
        final FDVersionInfo versionInfo = fdServiceFacade.revertFlowToLastPublishedState(flowId);
        return Response.ok(versionInfo).build();
    }

    @GET
    @Path("/flows/{flowId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets the flow events for the flow with the given id",
            response = ListContainer.class
    )
    public Response getFlowEvents(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
            final String flowId
    ) {
        final List<FDFlowEvent> flowEvents = fdServiceFacade.getFlowEvents(flowId);
        final ListContainer<FDFlowEvent> container = new ListContainer<>(flowEvents);
        return Response.ok(container).build();
    }

    // ---- Process group methods ----

    @GET
    @Path("/flows/{flowId}/process-groups/{pgId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the process group with the given id in the given flow. " +
                    "The alias of 'root' may be used to retrieve the root process group for the given flow.",
            response = FDProcessGroupFlow.class
    )
    public Response getProcessGroup(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @QueryParam("includeChildren")
                final boolean includeChildren
    ) {
        final FDProcessGroupFlow processGroupFlow = fdServiceFacade.getProcessGroup(flowId, processGroupId, includeChildren);
        populateUris(flowId, processGroupFlow.getFlowContent());
        return Response.ok(processGroupFlow).build();
    }


    // ---- Processor methods ----

    @POST
    @Path("/flows/{flowId}/process-groups/{pgId}/processors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a processor in the given process group",
            response = FDProcessor.class
    )
    public Response createProcessor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @ApiParam(value = "The processor configuration details.", required = true)
                final FDProcessor requestProcessor) {

        final FDProcessor createdProcessor = fdServiceFacade.createProcessor(flowId, processGroupId, requestProcessor);

        final URI uri = generateProcessorUri(flowId, createdProcessor.getComponentConfiguration());
        createdProcessor.setUri(uri.toString());

        return generateCreatedResponse(uri, createdProcessor).build();
    }

    @GET
    @Path("/flows/{flowId}/processors/{processorId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the processor with the given id in the given flow",
            response = FDProcessor.class
    )
    public Response getProcessor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("processorId")
            @ApiParam(name = "processorId", required = true)
                final String processorId
    ) {
        final FDProcessor processor = fdServiceFacade.getProcessor(flowId, processorId);

        final URI uri = generateProcessorUri(flowId, processor.getComponentConfiguration());
        processor.setUri(uri.toString());

        return Response.ok(processor).build();
    }

    @PUT
    @Path("/flows/{flowId}/processors/{processorId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates the processor with the given id in the given flow",
            response = FDProcessor.class
    )
    public Response updateProcessor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("processorId")
            @ApiParam(name = "processorId", required = true)
                final String processorId,
            @ApiParam(value = "The processor configuration details.", required = true)
                final FDProcessor requestProcessor) {

        final FDProcessor updatedProcessor = fdServiceFacade.updateProcessor(flowId, processorId, requestProcessor);

        final URI uri = generateProcessorUri(flowId, updatedProcessor.getComponentConfiguration());
        updatedProcessor.setUri(uri.toString());

        return Response.ok(updatedProcessor).build();
    }

    @DELETE
    @Path("/flows/{flowId}/processors/{processorId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the processor with the given id in the given flow",
            response = FDProcessor.class
    )
    public Response deleteProcessor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("processorId")
            @ApiParam(name = "processorId", required = true)
                final String processorId,
            @ApiParam(value = "The revision is used to verify the client is working with the latest version of the flow.")
            @QueryParam(VERSION)
                final LongParameter version,
            @ApiParam(value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.")
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY)
                final ClientIdParameter clientId) {

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(version == null ? null : version.getLong());
        requestRevision.setClientId(clientId.getClientId());

        final FDProcessor deletedProcessor = fdServiceFacade.deleteProcessor(flowId, processorId, requestRevision);

        final URI uri = generateProcessorUri(flowId, deletedProcessor.getComponentConfiguration());
        deletedProcessor.setUri(uri.toString());

        return Response.ok(deletedProcessor).build();
    }

    @GET
    @Path("/flows/{flowId}/processors/{processorId}/descriptors/{propertyName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the property descriptor with the given name from the given processor in the given flow",
            response = FDPropertyDescriptor.class
    )
    public Response getProcessorPropertyDescriptor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("processorId")
            @ApiParam(name = "processorId", required = true)
                final String processorId,
            @PathParam("propertyName")
            @ApiParam(name = "propertyName", required = true)
                final String propertyName
    ) {
        final FDPropertyDescriptor propertyDescriptor = fdServiceFacade.getProcessorPropertyDescriptor(flowId, processorId, propertyName);
        return Response.ok(propertyDescriptor).build();
    }


    // ---- Controller Service methods ----

    @POST
    @Path("/flows/{flowId}/process-groups/{pgId}/controller-services")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a controller service in the given process group",
            response = FDControllerService.class
    )
    public Response createControllerService(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @ApiParam(value = "The configuration details.", required = true)
                final FDControllerService requestControllerService) {

        final FDControllerService createdService = fdServiceFacade.createControllerService(flowId, processGroupId, requestControllerService);

        final URI uri = generateControllerServiceUri(flowId, createdService.getComponentConfiguration());
        createdService.setUri(uri.toString());

        return generateCreatedResponse(uri, createdService).build();
    }

    @GET
    @Path("/flows/{flowId}/controller-services/{controllerServiceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the controller service with the given id in the given flow",
            response = FDControllerService.class
    )
    public Response getControllerService(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("controllerServiceId")
            @ApiParam(name = "controllerServiceId", required = true)
                final String controllerServiceId
    ) {
        final FDControllerService service = fdServiceFacade.getControllerService(flowId, controllerServiceId);

        final URI uri = generateControllerServiceUri(flowId, service.getComponentConfiguration());
        service.setUri(uri.toString());

        return Response.ok(service).build();
    }

    @GET
    @Path("/flows/{flowId}/process-groups/{pgId}/controller-services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the controller service with the given id in the given flow",
            response = ListContainer.class
    )
    public Response getControllerServices(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String pgId
    ) {
        final Set<FDControllerService> services = fdServiceFacade.getControllerServices(flowId, pgId);

        for (final FDControllerService service : services) {
            final URI uri = generateControllerServiceUri(flowId, service.getComponentConfiguration());
            service.setUri(uri.toString());
        }

        final ListContainer<FDControllerService> container = new ListContainer<>(new ArrayList<>(services));
        return Response.ok(container).build();
    }

    @PUT
    @Path("/flows/{flowId}/controller-services/{controllerServiceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates the controller service with the given id in the given flow",
            response = FDControllerService.class
    )
    public Response updateControllerService(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("controllerServiceId")
            @ApiParam(name = "controllerServiceId", required = true)
                final String controllerServiceId,
            @ApiParam(value = "The controller service configuration details.", required = true)
                final FDControllerService requestControllerService) {

        final FDControllerService updatedControllerService = fdServiceFacade.updateControllerService(
                flowId, controllerServiceId, requestControllerService);

        final URI uri = generateControllerServiceUri(flowId, updatedControllerService.getComponentConfiguration());
        updatedControllerService.setUri(uri.toString());

        return Response.ok(updatedControllerService).build();
    }

    @DELETE
    @Path("/flows/{flowId}/controller-services/{controllerServiceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the controller service with the given id in the given flow",
            response = FDControllerService.class
    )
    public Response deleteControllerService(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("controllerServiceId")
            @ApiParam(name = "controllerServiceId", required = true)
                final String controllerServiceId,
            @ApiParam(value = "The revision is used to verify the client is working with the latest version of the flow.")
            @QueryParam(VERSION)
                final LongParameter version,
            @ApiParam(value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.")
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY)
                final ClientIdParameter clientId) {

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(version == null ? null : version.getLong());
        requestRevision.setClientId(clientId.getClientId());

        final FDControllerService deletedService = fdServiceFacade.deleteControllerService(flowId, controllerServiceId, requestRevision);

        final URI uri = generateControllerServiceUri(flowId, deletedService.getComponentConfiguration());
        deletedService.setUri(uri.toString());

        return Response.ok(deletedService).build();
    }

    @GET
    @Path("/flows/{flowId}/controller-services/{controllerServiceId}/descriptors/{propertyName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the property descriptor with the given name for the given service in the given flow",
            response = FDPropertyDescriptor.class
    )
    public Response getControllerServicePropertyDescriptor(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("controllerServiceId")
            @ApiParam(name = "controllerServiceId", required = true)
                final String controllerServiceId,
            @PathParam("propertyName")
            @ApiParam(name = "propertyName", required = true)
                final String propertyName
    ) {
        final FDPropertyDescriptor propertyDescriptor = fdServiceFacade.getControllerServicePropertyDescriptor(
                flowId, controllerServiceId, propertyName);

        return Response.ok(propertyDescriptor).build();
    }

    // ---- Connection methods ----

    @POST
    @Path("/flows/{flowId}/process-groups/{pgId}/connections")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a connection in the given process group",
            response = FDConnection.class
    )
    public Response createConnection(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @ApiParam(value = "The configuration details.", required = true)
                final FDConnection requestConnection) {

        final FDConnection createdConnection = fdServiceFacade.createConnection(flowId, processGroupId, requestConnection);

        final URI uri = generateConnectionUri(flowId, createdConnection.getComponentConfiguration());
        createdConnection.setUri(uri.toString());

        return generateCreatedResponse(uri, createdConnection).build();
    }

    @GET
    @Path("/flows/{flowId}/connections/{connectionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the connection with the given id in the given flow",
            response = FDConnection.class
    )
    public Response getConnection(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("connectionId")
            @ApiParam(name = "connectionId", required = true)
                final String connectionId) {

        final FDConnection connection = fdServiceFacade.getConnection(flowId, connectionId);

        final URI uri = generateConnectionUri(flowId, connection.getComponentConfiguration());
        connection.setUri(uri.toString());

        return Response.ok(connection).build();
    }

    @PUT
    @Path("/flows/{flowId}/connections/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates the connection with the given id in the given flow",
            response = FDConnection.class
    )
    public Response updateConnection(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("connectionId")
            @ApiParam(name = "connectionId", required = true)
                final String connectionId,
            @ApiParam(value = "The connection configuration details.", required = true)
                final FDConnection requestConnection) {

        final FDConnection updatedConnection = fdServiceFacade.updateConnection(flowId, connectionId, requestConnection);

        final URI uri = generateConnectionUri(flowId, updatedConnection.getComponentConfiguration());
        updatedConnection.setUri(uri.toString());

        return Response.ok(updatedConnection).build();
    }

    @DELETE
    @Path("/flows/{flowId}/connections/{connectionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the connection with the given id in the given flow",
            response = FDConnection.class
    )
    public Response deleteConnection(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("connectionId")
            @ApiParam(name = "connectionId", required = true)
                final String connectionId,
            @ApiParam(value = "The revision is used to verify the client is working with the latest version of the flow.")
            @QueryParam(VERSION)
                final LongParameter version,
            @ApiParam(value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.")
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY)
                final ClientIdParameter clientId) {

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(version == null ? null : version.getLong());
        requestRevision.setClientId(clientId.getClientId());

        final FDConnection deletedConnection = fdServiceFacade.deleteConnection(flowId, connectionId, requestRevision);

        final URI uri = generateConnectionUri(flowId, deletedConnection.getComponentConfiguration());
        deletedConnection.setUri(uri.toString());

        return Response.ok(deletedConnection).build();
    }

    // ---- Funnel methods ----

    @POST
    @Path("/flows/{flowId}/process-groups/{pgId}/funnels")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a funnel in the given process group",
            response = FDFunnel.class
    )
    public Response createFunnel(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @ApiParam(value = "The configuration details.", required = true)
                final FDFunnel requestFunnel) {

        final FDFunnel createdFunnel = fdServiceFacade.createFunnel(flowId, processGroupId, requestFunnel);

        final URI uri = generateFunnelUri(flowId, createdFunnel.getComponentConfiguration());
        createdFunnel.setUri(uri.toString());

        return generateCreatedResponse(uri, createdFunnel).build();
    }

    @GET
    @Path("/flows/{flowId}/funnels/{funnelId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the funnel with the given id in the given flow",
            response = FDFunnel.class
    )
    public Response getFunnel(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("funnelId")
            @ApiParam(name = "funnelId", required = true)
                final String funnelId) {

        final FDFunnel funnel = fdServiceFacade.getFunnel(flowId, funnelId);

        final URI uri = generateFunnelUri(flowId, funnel.getComponentConfiguration());
        funnel.setUri(uri.toString());

        return Response.ok(funnel).build();
    }

    @PUT
    @Path("/flows/{flowId}/funnels/{funnelId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates the funnel with the given id in the given flow",
            response = FDFunnel.class
    )
    public Response updateFunnel(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("funnelId")
            @ApiParam(name = "funnelId", required = true)
                final String funnelId,
            @ApiParam(value = "The funnel configuration details.", required = true)
                final FDFunnel requestFunnel) {

        final FDFunnel updatedFunnel = fdServiceFacade.updateFunnel(flowId, funnelId, requestFunnel);

        final URI uri = generateFunnelUri(flowId, updatedFunnel.getComponentConfiguration());
        updatedFunnel.setUri(uri.toString());

        return Response.ok(updatedFunnel).build();
    }

    @DELETE
    @Path("/flows/{flowId}/funnels/{funnelId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the funnel with the given id in the given flow",
            response = FDFunnel.class
    )
    public Response deleteFunnel(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("funnelId")
            @ApiParam(name = "funnelId", required = true)
                final String funnelId,
            @ApiParam(value = "The revision is used to verify the client is working with the latest version of the flow.")
            @QueryParam(VERSION)
                final LongParameter version,
            @ApiParam(value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.")
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY)
                final ClientIdParameter clientId) {

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(version == null ? null : version.getLong());
        requestRevision.setClientId(clientId.getClientId());

        final FDFunnel deletedFunnel = fdServiceFacade.deleteFunnel(flowId, funnelId, requestRevision);

        final URI uri = generateFunnelUri(flowId, deletedFunnel.getComponentConfiguration());
        deletedFunnel.setUri(uri.toString());

        return Response.ok(deletedFunnel).build();
    }

    // ---- Remote Process Group methods ----

    @POST
    @Path("/flows/{flowId}/process-groups/{pgId}/remote-process-groups")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a remote process group in the given process group",
            response = FDRemoteProcessGroup.class
    )
    public Response createRemoteProcessGroup(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("pgId")
            @ApiParam(name = "pgId", required = true)
                final String processGroupId,
            @ApiParam(value = "The configuration details.", required = true)
                final FDRemoteProcessGroup requestRemoteProcessGroup) {

        final FDRemoteProcessGroup createdRpg = fdServiceFacade.createRemoteProcessGroup(flowId, processGroupId, requestRemoteProcessGroup);

        final URI uri = generateRemoteProcessGroupUri(flowId, createdRpg.getComponentConfiguration());
        createdRpg.setUri(uri.toString());

        return generateCreatedResponse(uri, createdRpg).build();
    }

    @GET
    @Path("/flows/{flowId}/remote-process-groups/{remoteProcessGroupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the remote process group with the given id in the given flow",
            response = FDRemoteProcessGroup.class
    )
    public Response getRemoteProcessGroup(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("remoteProcessGroupId")
            @ApiParam(name = "remoteProcessGroupId", required = true)
                final String remoteProcessGroupId) {

        final FDRemoteProcessGroup rpg = fdServiceFacade.getRemoteProcessGroup(flowId, remoteProcessGroupId);

        final URI uri = generateRemoteProcessGroupUri(flowId, rpg.getComponentConfiguration());
        rpg.setUri(uri.toString());

        return Response.ok(rpg).build();
    }

    @PUT
    @Path("/flows/{flowId}/remote-process-groups/{remoteProcessGroupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates the remote process group with the given id in the given flow",
            response = FDRemoteProcessGroup.class
    )
    public Response updateRemoteProcessGroup(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("remoteProcessGroupId")
            @ApiParam(name = "remoteProcessGroupId", required = true)
                final String remoteProcessGroupId,
            @ApiParam(value = "The remoteProcessGroup configuration details.", required = true)
                final FDRemoteProcessGroup requestRemoteProcessGroup) {

        final FDRemoteProcessGroup updatedRpg = fdServiceFacade.updateRemoteProcessGroup(flowId, remoteProcessGroupId, requestRemoteProcessGroup);

        final URI uri = generateRemoteProcessGroupUri(flowId, updatedRpg.getComponentConfiguration());
        updatedRpg.setUri(uri.toString());

        return Response.ok(updatedRpg).build();
    }

    @DELETE
    @Path("/flows/{flowId}/remote-process-groups/{remoteProcessGroupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes the remote process group with the given id in the given flow",
            response = FDRemoteProcessGroup.class
    )
    public Response deleteRemoteProcessGroup(
            @PathParam("flowId")
            @ApiParam(name = "flowId", required = true)
                final String flowId,
            @PathParam("remoteProcessGroupId")
            @ApiParam(name = "remoteProcessGroupId", required = true)
                final String remoteProcessGroupId,
            @ApiParam(value = "The revision is used to verify the client is working with the latest version of the flow.")
            @QueryParam(VERSION)
                final LongParameter version,
            @ApiParam(value = "If the client id is not specified, new one will be generated. This value (whether specified or generated) is included in the response.")
            @QueryParam(CLIENT_ID) @DefaultValue(StringUtils.EMPTY)
                final ClientIdParameter clientId) {

        final FDRevision requestRevision = new FDRevision();
        requestRevision.setVersion(version == null ? null : version.getLong());
        requestRevision.setClientId(clientId.getClientId());

        final FDRemoteProcessGroup deletedRpg = fdServiceFacade.deleteRemoteProcessGroup(flowId, remoteProcessGroupId, requestRevision);

        final URI uri = generateRemoteProcessGroupUri(flowId, deletedRpg.getComponentConfiguration());
        deletedRpg.setUri(uri.toString());

        return Response.ok(deletedRpg).build();
    }

    // ---- URI Helper methods ----

    private URI generateProcessorUri(final String flowId, final VersionedProcessor processor) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getProcessor")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("processorId", processor.getIdentifier())
                .build();
    }

    private URI generateControllerServiceUri(final String flowId, final VersionedControllerService controllerService) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getControllerService")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("controllerServiceId", controllerService.getIdentifier())
                .build();
    }

    private URI generateConnectionUri(final String flowId, final VersionedConnection connection) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getConnection")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("connectionId", connection.getIdentifier())
                .build();
    }

    private URI generateFunnelUri(final String flowId, final VersionedFunnel funnel) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getFunnel")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("funnelId", funnel.getIdentifier())
                .build();
    }

    private URI generateRemoteProcessGroupUri(final String flowId, final VersionedRemoteProcessGroup remoteProcessGroup) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getRemoteProcessGroup")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("remoteProcessGroupId", remoteProcessGroup.getIdentifier())
                .build();
    }

    private URI generateProcessGroupUri(final String flowId, final FDProcessGroupFlowContent processGroup) {
        return getBaseUriBuilder()
                .path(FDFlowResource.class, "getProcessGroup")
                .resolveTemplate("flowId", flowId)
                .resolveTemplate("pgId", processGroup.getIdentifier())
                .build();
    }

    private void populateUris(final String flowId, final FDProcessGroupFlowContent processGroup) {
        final URI processGroupUri = generateProcessGroupUri(flowId, processGroup);
        processGroup.setUri(processGroupUri.toString());

        processGroup.getProcessors().stream().forEach(p ->
            p.setUri(generateProcessorUri(flowId, p.getComponentConfiguration()).toString())
        );

        processGroup.getControllerServices().stream().forEach(p ->
                p.setUri(generateControllerServiceUri(flowId, p.getComponentConfiguration()).toString())
        );

        processGroup.getFunnels().stream().forEach(p ->
                p.setUri(generateFunnelUri(flowId, p.getComponentConfiguration()).toString())
        );

        processGroup.getConnections().stream().forEach(p ->
                p.setUri(generateConnectionUri(flowId, p.getComponentConfiguration()).toString())
        );

        processGroup.getRemoteProcessGroups().stream().forEach(p ->
                p.setUri(generateRemoteProcessGroupUri(flowId, p.getComponentConfiguration()).toString())
        );

        // Recursively populate URIs in child process groups if they exist
        processGroup.getProcessGroups().stream().forEach(p -> populateUris(flowId, p));
    }

}
