/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.web.api;

import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentInfo;
import com.cloudera.cem.efm.service.agent.AgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Component
@Path("agents")
@Api(value = "Agents", description = "Register and manage MiNiFi agents")
public class AgentResource extends ApplicationResource {

    private final AgentService agentService;

    @Autowired
    public AgentResource(AgentService agentService) {
        this.agentService = agentService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Register a MiNiFi agent with this C2 server",
            notes = "This can also be done with a heartbeat, which will register a MiNiFi agent the first time it is seen in a heartbeat.",
            response = AgentInfo.class
    )
    public Response createAgent(
            @ApiParam("The metadata of the agent to register")
                    AgentInfo agentInfo) {
        throw new NotSupportedException("This method is not yet implemented for agent creation. Agents are only registered by sending a heartbeat over the C2 protocol.");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get all MiNiFi agents known to this C2 Server. " + BETA_INDICATION,
            response = Agent.class,
            responseContainer = "List")
    public Response getAgents() {
        List<Agent> agents = agentService.getAgents();
        return Response.ok(agents).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(
            value = "Retrieve info for a MiNiFi agent registered with this C2 server",
            response = Agent.class
    )
    public Response getAgent(
            @ApiParam("The identifier of the agent to retrieve")
            @PathParam("id")
                    String identifier) {

        Optional<Agent> agentOptional = agentService.getAgent(identifier);
        if (!agentOptional.isPresent()) {
            throw new NotFoundException("Agent not found with id " + identifier);
        }
        return Response.ok(agentOptional.get()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response replaceAgent(
            @ApiParam("The identifier of the agent")
            @PathParam("id")
                    String identifier,
            @ApiParam(value = "The current metadata of the agent to overwrite any info currently associated with the agent identifier", required = true)
                    AgentInfo agentInfo) {
        throw new NotSupportedException("This method is not yet implemented for this resource. Agents are updated each time they send a heartbeat over the C2 protocol.");
    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(
            value = "Delete an agent registered with this C2 server",
            response = Agent.class
    )
    public Response deleteAgent(
            @ApiParam("The identifier of the agent to delete")
            @PathParam("id")
                    String identifier) {

        Agent deletedAgent = agentService.deleteAgent(identifier);
        return Response.ok(deletedAgent).build();
    }


}
