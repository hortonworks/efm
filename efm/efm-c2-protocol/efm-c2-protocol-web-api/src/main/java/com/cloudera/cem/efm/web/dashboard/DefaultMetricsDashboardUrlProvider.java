/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the
 * terms of the Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with
 * a third party authorized to distribute this code.  If you do not have a written agreement with Cloudera
 * or with an authorized and properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS
 *      CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT,
 *      MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS
 *      FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA
 *      IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL
 *      DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF
 *      INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.web.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultMetricsDashboardUrlProvider implements MetricsDashboardUrlProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMetricsDashboardUrlProvider.class);

    private final UriBuilder baseUriBuilder;

    private final Map<String, UriBuilder> dashboardUriBuilders = new HashMap<>();

    @Context
    private UriInfo uriInfo;

    public DefaultMetricsDashboardUrlProvider(MetricsDashboardUrlProperties properties) {

        final String baseUrlString = properties.getBaseUrl();
        baseUriBuilder = baseUrlString != null ? UriBuilder.fromUri(baseUrlString): null;

        final Map<String, String> urls = properties.getUrl();
        if (urls != null && !urls.isEmpty()) {
            for (final String dashboardName : urls.keySet()) {
                final String dashboardUrl = urls.get(dashboardName);
                if (StringUtils.isBlank(dashboardUrl)) {
                    continue;
                }
                logger.info("Metrics dashboard loaded with url template: {}={}", dashboardName, dashboardUrl);
                try {
                    // create uri builder from uri template
                    dashboardUriBuilders.put(dashboardName, UriBuilder.fromUri(dashboardUrl));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Metrics Dashboard URL template is invalid due to: " + e.getLocalizedMessage(), e);
                }
            }
        }
    }

    @Override
    public URI get(final String dashboardName, final Map<String, String> placeholderReplacementValues) throws IllegalArgumentException, MetricsDashboardUrlProviderException {

        if (dashboardName == null) {
            throw new IllegalArgumentException("dashboard name must not be null");
        }

        final UriBuilder uriBuilder = dashboardUriBuilders.get(dashboardName);
        if (uriBuilder == null) {
            return null;
        }

        try {
            final URI dashboardUrl = buildUriFromBuilder(uriBuilder, placeholderReplacementValues);

            if (dashboardUrl == null || dashboardUrl.isAbsolute()) {
                return dashboardUrl;
            } else {
                final UriBuilder baseBuilder = this.baseUriBuilder != null ? this.baseUriBuilder : this.uriInfo.getBaseUriBuilder();
                if (baseBuilder == null) {
                    return dashboardUrl;
                }

                // Clone the base uri builder so we don't modify it, then add the relative uri parts to it
                final UriBuilder absoluteUriBuilder = baseBuilder.clone();
                absoluteUriBuilder.path(dashboardUrl.getRawPath());
                absoluteUriBuilder.replaceQuery(dashboardUrl.getRawQuery());
                absoluteUriBuilder.fragment(dashboardUrl.getRawFragment());
                final URI absoluteDashboardUrl = buildUriFromBuilder(absoluteUriBuilder, placeholderReplacementValues);
                return absoluteDashboardUrl;
            }
        } catch (Exception e) {
            throw new MetricsDashboardUrlProviderException("Failed to build dashboard url for '" + dashboardName + "' due to" + e.getLocalizedMessage(), e);
        }

    }

    /* package-visible uriInfo accessors for testing. outside of testing uriInfo is injected via javax.ws.rs provider */

    void setUriInfo(@Context UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    private static URI buildUriFromBuilder(final UriBuilder builder, final Map<String, String> placeholderReplacementValues) throws IllegalArgumentException {
        return placeholderReplacementValues != null ? builder.buildFromMap(placeholderReplacementValues, true) : builder.build();
    }

}
