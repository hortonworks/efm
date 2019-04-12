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

import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.FlowFormat;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.service.flow.C2FlowService;
import com.cloudera.cem.efm.model.FlowSummary;
import com.cloudera.cem.efm.model.FlowSummaries;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@Path("flows")
@Api(value = "Flows", description = "Creation and retrieval of flows")
public class FlowResource extends ApplicationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowResource.class);

    private C2FlowService flowService;

    @Autowired
    public FlowResource(final C2FlowService flowService) {
        this.flowService = flowService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            // TODO, introduce pagination to get all flow summaries
            value = "Gets all flow summaries. " + BETA_INDICATION,
            response = FlowSummaries.class
    )
    public Response getAllFlowSummaries(@Context final UriInfo uriInfo) {
        final List<FlowSummary> summaries = flowService.getFlowSummaries();
        summaries.stream().forEach(fs -> populateUri(fs, uriInfo));

        final FlowSummaries flowSummaries = new FlowSummaries();
        flowSummaries.setFlows(summaries);
        return Response.status(Response.Status.OK).entity(flowSummaries).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get a flow by id, the response will contain a JSON document with fields for all of the flow's metadata as well as the raw content of the flow embedded in a field",
            response = Flow.class
    )
    public Response getFlow(
            @Context
                final UriInfo uriInfo,
            @PathParam("id")
            @ApiParam("The id of a flow to retrieve")
                final String flowId) {

        final Optional<Flow> flow = flowService.getFlow(flowId);
        if (!flow.isPresent()){
            throw new ResourceNotFoundException("A flow does not exist with the given id");
        }

        populateUri(flow.get(), uriInfo);
        return Response.ok().entity(flow.get()).build();
    }

    @GET
    @Path("{id}/content")
    @Produces(FlowFormat.Values.YAML_V2)
    @ApiOperation(
            value = "Get a flow by id as a YAML formatted flow definition",
            response = String.class
    )
    public Response getFlowContentAsYaml(
            @PathParam("id")
            @ApiParam("The id of a flow to retrieve")
                final String flowId) {

        final StreamingOutput stream = (output) -> {
            flowService.getFlowContent(flowId, FlowFormat.YAML_V2_TYPE, output);
        };

        return Response.ok().entity(stream).build();
    }

    @POST
    @Consumes(FlowFormat.Values.YAML_V2)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a flow from a YAML flow definition. " + BETA_INDICATION,
            response = FlowSummary.class
    )
    public Response createFlowFromYaml(
            @Context
                final UriInfo uriInfo,
            @ApiParam(value = "The YAML content of the flow", required = true)
                final String flowContent) {

        final FlowSummary createdFlow = flowService.createFlow(flowContent);
        populateUri(createdFlow, uriInfo);
        return Response.created(createdFlow.getUri()).entity(createdFlow).build();
    }

    @POST
    @Consumes(FlowFormat.Values.FLOW_SNAPSHOT_JSON_V1)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a C2 flow from a VersionedFlowSnapshot from NiFi Registry. " + BETA_INDICATION,
            response = FlowSummary.class
    )
    public Response createFlowFromFlowSnapshot(
            @Context
                final UriInfo uriInfo,
            @ApiParam(value = "The flow snapshot containing the flow URI and VersionedFlowSnapshot", required = true)
                final FlowSnapshot flowSnapshot
            ) throws IOException {

        final FlowSummary createdFlow = flowService.createFlow(flowSnapshot);
        populateUri(createdFlow, uriInfo);
        return Response.created(createdFlow.getUri()).entity(createdFlow).build();
    }

    private void populateUri(final FlowSummary flowSummary, final UriInfo uriInfo) {
        final URI uri = uriInfo.getBaseUriBuilder()
                .path("flows/{id}")
                .resolveTemplate("id", flowSummary.getId())
                .build();
        flowSummary.setUri(uri);
    }

}
