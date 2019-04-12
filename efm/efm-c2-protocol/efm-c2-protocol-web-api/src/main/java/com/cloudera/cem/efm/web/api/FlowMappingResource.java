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
package com.cloudera.cem.efm.web.api;

import com.cloudera.cem.efm.model.FlowMapping;
import com.cloudera.cem.efm.model.FlowMappings;
import com.cloudera.cem.efm.service.flow.C2FlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@Path("flow-mappings")
@Api(value = "Flow Mappings",
        description = "Creating and retrieval of flow mappings. These are BETA endpoints as flow mappings are expected to be internally managed by the server.")
public class FlowMappingResource extends ApplicationResource {

    private C2FlowService flowService;

    @Autowired
    public FlowMappingResource(final C2FlowService flowService) {
        this.flowService = flowService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            // TODO, introduce pagination to get all flow mappings
            value = "Gets all flow mappings. " + BETA_INDICATION,
            response = FlowMappings.class
    )
    public Response getAllFlowMappings(@Context final UriInfo uriInfo) {
        final List<FlowMapping> allFlowMappings = flowService.getFlowMappings();
        allFlowMappings.stream().forEach(fm -> populateFlowUri(fm, uriInfo));

        final FlowMappings flowMappings = new FlowMappings();
        flowMappings.setFlowMappings(allFlowMappings);

        return Response.ok(flowMappings).build();
    }

    @GET
    @Path("{agentClassName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets a flow mapping for a given agent class. " + BETA_INDICATION,
            response = FlowMapping.class
    )
    public Response getFlowMapping(
            @Context
            final UriInfo uriInfo,
            @PathParam("agentClassName")
            @ApiParam("The name of the class to retrieve")
            final String agentClassName) {

        final Optional<FlowMapping> result = flowService.getFlowMapping(agentClassName);
        if (!result.isPresent()) {
            throw new NotFoundException("No flow mapping exists for agent class '" + agentClassName + "'");
        }

        final FlowMapping flowMapping = result.get();
        populateFlowUri(flowMapping, uriInfo);
        return Response.ok(flowMapping).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a flow mapping. " + BETA_INDICATION
    )
    public Response createFlowMapping(
            @Context
            final UriInfo uriInfo,
            @ApiParam(value = "The flow mapping to create", required = true)
            final FlowMapping flowMapping) {

        final FlowMapping createdFlowMapping = flowService.createFlowMapping(flowMapping);

        final URI flowMappingUri = uriInfo.getRequestUriBuilder()
                .path("{name}")
                .resolveTemplate("name", createdFlowMapping.getAgentClass())
                .build();

        return Response.created(flowMappingUri).build();
    }

    @PUT
    @Path("{agentClassName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates a flow mapping. " + BETA_INDICATION,
            response = FlowMapping.class
    )
    public Response updateFlowMapping(
            @Context
                final UriInfo uriInfo,
            @PathParam("agentClassName")
            @ApiParam("The name of the class to update the mapping for")
                final String agentClassName,
            @ApiParam(value = "The flow mapping to update", required = true)
                final FlowMapping flowMapping) {

        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be null or blank");
        }

        if (flowMapping.getAgentClass() != null && !agentClassName.equals(flowMapping.getAgentClass())) {
            throw new IllegalArgumentException("Agent class in path param must match agent class in body");
        } else {
            flowMapping.setAgentClass(agentClassName);
        }

        final FlowMapping updatedFlowMapping = flowService.updateFlowMapping(flowMapping);
        return Response.ok(updatedFlowMapping).build();
    }

    @DELETE
    @Path("{agentClassName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes a flow mapping. " + BETA_INDICATION,
            response = FlowMapping.class
    )
    public Response deleteFlowMapping(
            @Context
            final UriInfo uriInfo,
            @PathParam("agentClassName")
            @ApiParam("The name of the class to delete the mapping for")
                final String agentClassName) {

        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be null or blank");
        }

        final FlowMapping deletedFlowMapping = flowService.deleteFlowMapping(agentClassName);
        populateFlowUri(deletedFlowMapping, uriInfo);

        return Response.ok(deletedFlowMapping).build();
    }

    private void populateFlowUri(final FlowMapping flowMapping, final UriInfo uriInfo) {
        final URI flowUri = uriInfo.getBaseUriBuilder()
                .path("flows/{id}")
                .resolveTemplate("id", flowMapping.getFlowId())
                .build();

        flowMapping.setFlowUri(flowUri);
    }

}
