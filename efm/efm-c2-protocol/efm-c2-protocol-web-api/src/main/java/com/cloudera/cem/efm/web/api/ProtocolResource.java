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

import com.cloudera.cem.efm.model.C2Heartbeat;
import com.cloudera.cem.efm.model.C2HeartbeatResponse;
import com.cloudera.cem.efm.model.C2OperationAck;
import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import com.cloudera.cem.efm.service.c2protocol.HeartbeatContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

@Component
@Path("c2-protocol")
@Api(value = "C2 Protocol", description = "An HTTP RESTful implementation of the MiNiFi C2 protocol.")
public class ProtocolResource extends ApplicationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolResource.class);

    private C2ProtocolService c2ProtocolService;

    @Autowired
    public ProtocolResource(C2ProtocolService c2ProtocolService) {
        this.c2ProtocolService = c2ProtocolService;
    }

    @POST
    @Path("/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "An endpoint for a MiNiFi Agent to send a heartbeat to the C2 server",
            response = C2HeartbeatResponse.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400) })
    public Response heartbeat(
            @ApiParam(required = true)
                    C2Heartbeat heartbeat) {

        final HeartbeatContext heartbeatContext = HeartbeatContext.builder()
                .baseUri(getBaseUri())
                .contentLength(httpServletRequest.getHeader(CONTENT_LENGTH))
                .build();

        final C2HeartbeatResponse heartbeatResponse = c2ProtocolService.processHeartbeat(heartbeat, heartbeatContext);
        return Response.ok(heartbeatResponse).build();

    }

    @POST
    @Path("/acknowledge")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "An endpoint for a MiNiFi Agent to send an operation acknowledgement to the C2 server"
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400) })
    public Response acknowledge(
            @ApiParam(required = true)
                    C2OperationAck operationAck) {

        c2ProtocolService.processOperationAck(operationAck);
        return Response.ok().build();

    }

}
