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

import com.cloudera.cem.efm.service.agent.AgentService;
import com.cloudera.cem.efm.model.AgentManifest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Component
@Path("agent-manifests")
@Api(value = "Agent Manifests", description = "Register and manage agent manifest definitions")
public class AgentManifestResource extends ApplicationResource {

    private AgentService agentService;

    @Autowired
    public AgentManifestResource(AgentService agentService) {
        this.agentService = agentService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Upload an agent manifest",
            response = AgentManifest.class)
    public Response createAgentManifest(
            @QueryParam("class")
            @ApiParam("Optionally, a class label to associate with the manifest being uploaded")
                    String className,
            @ApiParam
                    AgentManifest agentManifest) {

        final AgentManifest createdAgentManifest = agentService.createAgentManifest(agentManifest);
        return Response
                .created(getUri("agent-manifests", createdAgentManifest.getIdentifier()))
                .entity(agentManifest)
                .build();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            // TODO, introduce pagination to get all agent manifests
            value = "Get all agent manifests. " + BETA_INDICATION,
            response = AgentManifest.class,
            responseContainer = "List")
    public Response getAgentManifests(
            @QueryParam("class")
            @ApiParam("Optionally, filter the results to match a class label")
                    String className) {

        final List<AgentManifest> agentManifests;
        if (className != null) {
            agentManifests = agentService.getAgentManifests(className);
        } else {
            agentManifests = agentService.getAgentManifests();
        }
        return Response.ok(agentManifests).build();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(
            value = "Get the agent manifest specified by the id",
            response = AgentManifest.class)
    public Response getAgentManifest(
            @PathParam("id")
            @ApiParam
                    String id) {

        final Optional<AgentManifest> agentManifestOptional = agentService.getAgentManifest(id);
        if (!agentManifestOptional.isPresent()) {
            throw new NotFoundException("No agent manifest with matching id " + id);
        }
        return Response.ok(agentManifestOptional.get()).build();

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @ApiOperation(
            value = "Delete the agent manifest specified by id",
            response = AgentManifest.class)
    public Response deleteAgentManifest(
            @PathParam("id")
            @ApiParam
                    String id) {

        final AgentManifest deletedAgentManifest = agentService.deleteAgentManifest(id);
        return Response.ok(deletedAgentManifest).build();
    }

}
