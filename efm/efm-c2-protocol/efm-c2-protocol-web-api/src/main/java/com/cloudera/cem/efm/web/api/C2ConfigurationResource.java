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

import com.cloudera.cem.efm.model.C2ConfigurationInfo;
import com.cloudera.cem.efm.model.NiFiRegistryInfo;
import com.cloudera.cem.efm.service.config.C2ConfigurationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("c2-configuration")
@Api(value = "C2 Configuration", description = "Retrieve information about the configuration of the C2 server")
public class C2ConfigurationResource extends ApplicationResource {

    private C2ConfigurationService configurationService;

    @Autowired
    public C2ConfigurationResource(final C2ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get general information about the configuration of the C2 server",
            response = C2ConfigurationInfo.class
    )
    public Response getC2ConfigurationInfo() {
        final C2ConfigurationInfo c2ConfigurationInfo = configurationService.getC2ConfigurationInfo();
        return Response.ok(c2ConfigurationInfo).build();
    }

    @GET
    @Path("/nifi-registry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get information about the NiFi Registry that the C2 server is configured with",
            response = NiFiRegistryInfo.class
    )
    public Response getNiFiRegistryInfo() {
        final NiFiRegistryInfo niFiRegistryInfo = configurationService.getNiFiRegistryInfo();
        return Response.ok(niFiRegistryInfo).build();
    }

}
