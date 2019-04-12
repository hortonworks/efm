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
package com.cloudera.cem.efm.web.security.authentication.knox;

import com.cloudera.cem.efm.web.security.authentication.exception.KnoxAuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class KnoxUtils {

    static final Logger LOGGER = LoggerFactory.getLogger(KnoxUtils.class);

    public static void sendRedirectToKnox(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          final KnoxAuthenticationException exception) throws IOException {

        final StringBuffer originalUrlBuilder = request.getRequestURL();

        final String queryString = request.getQueryString();
        if (!StringUtils.isBlank(queryString)) {
            originalUrlBuilder.append("?").append(queryString);
        }

        final String knoxUrl = exception.getKnoxUrl();
        final String originalUrl = originalUrlBuilder.toString();

        final URI redirectUrl = UriBuilder.fromUri(knoxUrl)
                .queryParam("originalUrl", originalUrl)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(exception.getMessage());
            LOGGER.debug("Redirecting to {}", new Object[]{redirectUrl.toString()});
        }

        // redirect to know for login...
        response.sendRedirect(redirectUrl.toString());
    }

}
