/*
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

import com.cloudera.cem.efm.model.CurrentUser;
import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.NiFiUserUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("access")
@Api(value = "Access", description = "Check the access status of the current user")
public class AccessResource extends ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(AccessResource.class);

    /**
     * Gets the current client's identity and other attributes.
     *
     * @return An object describing the current client identity, as determined by the server, and it's permissions.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Returns the current client's authenticated identity. " + BETA_INDICATION,
            response = CurrentUser.class
    )
    public Response getAccessStatus() {
        final NiFiUser user = NiFiUserUtils.getNiFiUser();
        if (user == null) {
            // Not expected to happen unless the server has been misconfigured.
            throw new WebApplicationException(new Throwable("Unable to access details for current user."));
        }
        final CurrentUser currentUser = new CurrentUser();
        currentUser.setIdentity(user.getIdentity());
        currentUser.setAnonymous(user.isAnonymous());
        return Response.ok(currentUser).build();
    }


}
