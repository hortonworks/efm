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

import com.cloudera.cem.efm.service.operation.OperationService;
import com.cloudera.cem.efm.model.Operation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
@Path("operations")
@Api(value = "Operations", description = "Submit and manage C2 operations targeting MiNiFi Agents")
public class OperationResource extends ApplicationResource {

    private final OperationService operationService;

    @Autowired
    public OperationResource(OperationService operationService) {
        this.operationService = operationService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Submit a request for a C2 operation targeting a MiNiFi agent",
            response = Operation.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400) })
    public Response createOperation(
            @ApiParam(value = "The requested operation", required = true)
                    Operation operation) {
        final Operation createdOperation = operationService.createOperation(operation);
        return Response
                .created(getUri("operations", createdOperation.getIdentifier()))
                .entity(createdOperation)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            // TODO, introduce pagination to get all operations
            value = "Get all operations. " + BETA_INDICATION,
            response = Operation.class,
            responseContainer = "List")
    public Response getOperations() {
        final List<Operation> operations = operationService.getOperations();
        return Response.ok(operations).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get a specific operation",
            response = Operation.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404) })
    public Response getOperation(
            @PathParam("id")
            @ApiParam(value = "The identifier of the operation to retrieve", required = true)
                    String identifier) {

        final Optional<Operation> operationRequestOptional = operationService.getOperation(identifier);
        if (!operationRequestOptional.isPresent()) {
            throw new NotFoundException("No operation with matching id '" + identifier + "'");
        }
        return Response.ok(operationRequestOptional.get()).build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            // TODO partial updates should use PATCH
            value = "Updates the state of an operation (other fields are ignored). " + BETA_INDICATION,
            response = Operation.class
    )
    @ApiResponses({
            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400),
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404)})
    public Response updateOperation(
            @PathParam("id")
            @ApiParam(value = "The identifier of the operation for which to update state.", required = true)
                    String identifier,
            @ApiParam(value = "An Operation object containing the new state.", required = true)
                    Operation operation) {

        if (operation == null || operation.getState() == null) {
            throw new IllegalArgumentException("Operation must contain a state field when calling update.");
        }

        final Operation updatedOperation = operationService.updateOperationState(identifier, operation.getState());
        return Response.ok(updatedOperation).build();
    }

    // TODO evaluate if we should allow operation deletions.
    // For now disabling it as operation id is a foreign key for other columns.
    // Clients should use updateOperation() to set operation state to CANCELLED instead
//    @DELETE
//    @Path("/{id}")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(
//            value = "Delete an operation",
//            response = Operation.class
//    )
//    @ApiResponses({
//            @ApiResponse(code = 400, message = HttpStatusMessages.MESSAGE_400) })
//    public Response deleteOperation(
//            @PathParam("id")
//            @ApiParam(value = "The identifier of the operation to delete", required = true)
//                    String identifier) {
//        final Operation deletedOperation = operationService.deleteOperation(identifier);
//        return Response.ok(deletedOperation).build();
//    }

    // TODO, add bulk operation creation endpoints. e.g., create an operation for all agents of a given class label

}
