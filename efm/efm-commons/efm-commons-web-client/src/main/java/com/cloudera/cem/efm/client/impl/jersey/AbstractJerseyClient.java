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
 *
 *    This file incorporates works covered by the following copyright and permission notice:
 *
 *    Apache NiFi - Registry
 *    Copyright 2014-2018 The Apache Software Foundation
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.cloudera.cem.efm.client.impl.jersey;

import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.FilterParameter;
import com.cloudera.cem.efm.model.QueryParameters;
import com.cloudera.cem.efm.model.SortParameter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractJerseyClient {

    private final Map<String, String> headers;

    public AbstractJerseyClient(final Map<String, String> headers) {
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(headers));
    }

    protected Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Creates a new Invocation.Builder for the given WebTarget with the headers added to the builder.
     *
     * @param webTarget the target for the request
     * @return the builder for the target with the headers added
     */
    protected Invocation.Builder getRequestBuilder(final WebTarget webTarget) {
        final Invocation.Builder requestBuilder = webTarget.request();
        headers.entrySet().stream().forEach(e -> requestBuilder.header(e.getKey(), e.getValue()));
        return requestBuilder;
    }

    /**
     * Executes the given action and returns the result.
     *
     * @param action       the action to execute
     * @param errorMessage the message to use if a NiFiRegistryException is thrown
     * @param <T>          the return type of the action
     * @return the result of the action
     * @throws C2Exception if any exception other than IOException is encountered
     * @throws IOException if an I/O error occurs communicating with the registry
     */
    protected <T> T executeAction(final String errorMessage, final MiNiFiC2Action<T> action) throws C2Exception, IOException {
        try {
            return action.execute();
        } catch (final Exception e) {
            final Throwable ioeCause = getIOExceptionCause(e);

            if (ioeCause == null) {
                final StringBuilder errorMessageBuilder = new StringBuilder(errorMessage);

                // see if we have a WebApplicationException, and if so add the response body to the error message
                if (e instanceof WebApplicationException) {
                    final Response response = ((WebApplicationException) e).getResponse();
                    final String responseBody = response.readEntity(String.class);
                    errorMessageBuilder.append(": ").append(responseBody);
                }

                throw new C2Exception(errorMessageBuilder.toString(), e);
            } else {
                throw (IOException) ioeCause;
            }
        }
    }


    /**
     * An action to execute with the given return type.
     *
     * @param <T> the return type of the action
     */
    protected interface MiNiFiC2Action<T> {

        T execute();

    }

    /**
     * @param e an exception that was encountered interacting with the registry
     * @return the IOException that caused this exception, or null if the an IOException did not cause this exception
     */
    protected Throwable getIOExceptionCause(final Throwable e) {
        if (e == null) {
            return null;
        }

        if (e instanceof IOException) {
            return e;
        }

        return getIOExceptionCause(e.getCause());
    }

    /**
     * Adds QueryParameters to WebTarget in a standard fashion
     *
     * @param queryParameters the paging and sorting parameters for the target
     * @param target the target to which to add query parameters.
     */
    protected WebTarget addQueryParamsToTarget(final WebTarget target, final QueryParameters queryParameters) {

        WebTarget modifiedTarget = target;

        final List<SortParameter> sorts = queryParameters.getSortParameters();
        final Integer pageNum = queryParameters.getPageNum();
        final Integer numRows = queryParameters.getNumRows();
        final List<FilterParameter> filters = queryParameters.getFilterParameters();

        if (sorts != null) {
            for (final SortParameter sortParam : sorts) {
                modifiedTarget = modifiedTarget.queryParam("sort", sortParam.toString());
            }
        }

        if (pageNum != null) {
            modifiedTarget = modifiedTarget.queryParam("pageNum", pageNum.toString());
        }

        if (numRows != null) {
            modifiedTarget = modifiedTarget.queryParam("rows", numRows.toString());
        }

        if (filters != null) {
            for (final FilterParameter filterParam : filters) {
                modifiedTarget = modifiedTarget.queryParam("filter", filterParam.toString());
            }
        }

        return modifiedTarget;

    }

}
