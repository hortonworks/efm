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

import java.net.URI;
import java.util.Map;

public interface MetricsDashboardUrlProvider {

    /**
     * Gets a metrics dashboard url by name.
     *
     * The returned URL should be an absolute URL unless the implementation explicitly states otherwise.
     * Any configured template placeholders are resolved using the supplied placeholder replacement values.
     *
     * @param dashboardName the name of the dashboard to get
     * @param placeholderReplacementValues Optional, can be null.
     *                                     A map of placeholder key to placeholder value.
     *                                     For example, the placeholder key for {agentId} is agentId.
     *
     * @return An absolute url for the dashboard with template placeholders replaced with their values,
     *         or null if there is not configured url for the given dashboard name.
     *
     * @throws IllegalArgumentException if dashboardName is null
     * @throws MetricsDashboardUrlProviderException if the dashboard url cannot be resolved,
     *             for example if the the configured url has a template placeholder and the
     *             placeholder value is not supplied in the placeholderReplacementValues
     */
    URI get(String dashboardName, Map<String, String> placeholderReplacementValues)
            throws IllegalArgumentException, MetricsDashboardUrlProviderException;

}
