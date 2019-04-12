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
package com.cloudera.cem.efm.web.dashboard

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.PathSegment
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

class DefaultMetricsDashboardUrlProviderSpec extends Specification {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMetricsDashboardUrlProviderSpec.class)

    def urlProvidersGetThrowsExceptionUnderCertainConditions() {

        given:
        def dashboardUrls = new HashMap<String, String>();
        dashboardUrls.put("test-dashboard", "http://host:8080/dashboard?agent={agentId}")
        def properties = new MetricsDashboardUrlProperties()
        properties.setUrl(dashboardUrls)
        def urlProvider = new DefaultMetricsDashboardUrlProvider(properties)

        when: "dashboard name is null"
        urlProvider.get(null,null)

        then:
        thrown(IllegalArgumentException)

        when: "template placeholder value is not supplied"
        urlProvider.get("test-dashboard", null)

        then:
        thrown(MetricsDashboardUrlProviderException.class)

        when: "template placeholder value is not supplied"
        urlProvider.get("test-dashboard", ["agentClass": "class1"])

        then:
        thrown(MetricsDashboardUrlProviderException.class)

        when: "template placeholder value is supplied"
        urlProvider.get("test-dashboard", ["agentId": "agent1"])

        then:
        notThrown(MetricsDashboardUrlProviderException.class)

    }

    def urlProviderGetUrlRules() {

        given:
        def uriInfoBaseUrl = mockUriInfo(URI.create("https://proxyhost:8443/proxycontext"))
        def testDashboardName = "test-dashboard";
        def dashboardUrls = new HashMap<String, String>();
        dashboardUrls.put(testDashboardName, dashboardUrl)
        def properties = new MetricsDashboardUrlProperties()
        properties.setBaseUrl(baseUrl)
        properties.setUrl(dashboardUrls)
        def urlProvider = new DefaultMetricsDashboardUrlProvider(properties)
        urlProvider.setUriInfo(uriInfoBaseUrl)


        when:
        final resolvedUrl = urlProvider.get(testDashboardName, placeholderValues)

        then:
        logger.debug("expect: {}", expectedUrl)
        logger.debug("actual: {}", resolvedUrl)
        (resolvedUrl == null && expectedUrl == null) || resolvedUrl.toString() == expectedUrl


        where:

        baseUrl             | dashboardUrl                  | placeholderValues    | expectedUrl

        /* Null or blank dashboard url results in null uri returned */
        "http://host"       | null                          | null                 | null
        "http://host"       | ""                            | null                 | null
        null                | null                          | null                 | null
        null                | ""                            | null                 | null

        /* Base + relative url works regardless of trailing-starting-slashes */
        "http://host"       | "dashboard"                   | null                 | "http://host/dashboard"
        "http://host"       | "/dashboard"                  | null                 | "http://host/dashboard"
        "http://host/"      | "dashboard"                   | null                 | "http://host/dashboard"
        "http://host/"      | "/dashboard"                  | null                 | "http://host/dashboard"

        /* Base + relative url works regardless of trailing-starting-slashes when port is also present */
        "http://host:8080"  | "dashboard"                   | null                 | "http://host:8080/dashboard"
        "http://host:8080"  | "/dashboard"                  | null                 | "http://host:8080/dashboard"
        "http://host:8080/" | "dashboard"                   | null                 | "http://host:8080/dashboard"
        "http://host:8080/" | "/dashboard"                  | null                 | "http://host:8080/dashboard"

        /* Template placeholders get replaced using map of key values */
        "http://host"       | "dashboard/agent/{id}"        | ["id": "a1"]         | "http://host/dashboard/agent/a1"
        "http://host"       | "dashboard#agent-{id}"        | ["id": "a1"]         | "http://host/dashboard#agent-a1"
        "http://host"       | "dashboard?agent={id}"        | ["id": "a1"]         | "http://host/dashboard?agent=a1"
        "http://host"       | "/d?agent={a}&class={c}"      | ["a": "1", "c": "2"] | "http://host/d?agent=1&class=2"
        "http://host/{id}"  | "dashboard"                   | ["id": "a1"]         | "http://host/a1/dashboard"

        /* Template placeholder values get encoded correctly based on location in uri */
        "http://host"       | "/agent/{i}/dashboard"        | ["i":"_ _/_%_+_=_:"] | "http://host/agent/_%20_%2F_%25_+_=_:/dashboard"
        "http://host"       | "/dashboard?agent={i}"        | ["i":"_ _/_%_+_=_:"] | "http://host/dashboard?agent=_+_%2F_%25_%2B_%3D_%3A"
        "http://host"       | "/dashboard#agent-{i}"        | ["i":"_ _/_%_+_=_:"] | "http://host/dashboard#agent-_%20_%2F_%25_+_=_%3A"

        /* Absolute url overrides configured base url */
        "http://host"       | "http://host2:8080/dashboard" | null                 | "http://host2:8080/dashboard"

        /* Relative url uses base uri from request context uriInfo when no base url is configured */
        null                | "/dashboard"                  | null                 | "https://proxyhost:8443/proxycontext/dashboard"
        null                | "../grafana/dashboard"        | ["id": "a1"]         | "https://proxyhost:8443/proxycontext/../grafana/dashboard"
        null                | "?agent={a}&class={c}"        | ["a": "1", "c": "2"] | "https://proxyhost:8443/proxycontext?agent=1&class=2"
        null                | "#agent-{id}"                 | ["id": "a1"]         | "https://proxyhost:8443/proxycontext#agent-a1"
        null                | "/path?id={id}#frag"          | ["id": "a1"]         | "https://proxyhost:8443/proxycontext/path?id=a1#frag"

        /* Absolute url does not use request context uriInfo */
        null                | "http://host/dashboard"       | null                 | "http://host/dashboard"
        null                | "http://host:8080/dashboard"  | null                 | "http://host:8080/dashboard"
        null                | "https://host/dashboard"      | null                 | "https://host/dashboard"
        null                | "https://host:8080/dashboard" | null                 | "https://host:8080/dashboard"

    }

    def UriInfo mockUriInfo(URI uri) {
        return new UriInfo() {
            @Override
            String getPath() {
                return uri.getPath()
            }

            @Override
            String getPath(boolean decode) {
                return uri.getPath()
            }

            @Override
            List<PathSegment> getPathSegments() {
                throw new RuntimeException("method not implemented")
            }

            @Override
            List<PathSegment> getPathSegments(boolean decode) {
                throw new RuntimeException("method not implemented")
            }

            @Override
            URI getRequestUri() {
                return uri
            }

            @Override
            UriBuilder getRequestUriBuilder() {
                return UriBuilder.fromUri(uri)
            }

            @Override
            URI getAbsolutePath() {
                return uri
            }

            @Override
            UriBuilder getAbsolutePathBuilder() {
                return UriBuilder.fromUri(uri)
            }

            @Override
            URI getBaseUri() {
                return uri
            }

            @Override
            UriBuilder getBaseUriBuilder() {
                return UriBuilder.fromUri(uri)
            }

            @Override
            MultivaluedMap<String, String> getPathParameters() {
                throw new RuntimeException("method not implemented")
            }

            @Override
            MultivaluedMap<String, String> getPathParameters(boolean decode) {
                throw new RuntimeException("method not implemented")
            }

            @Override
            MultivaluedMap<String, String> getQueryParameters() {
                throw new RuntimeException("method not implemented")
            }

            @Override
            MultivaluedMap<String, String> getQueryParameters(boolean decode) {
                throw new RuntimeException("method not implemented")
            }

            @Override
            List<String> getMatchedURIs() {
                throw new RuntimeException("method not implemented")
            }

            @Override
            List<String> getMatchedURIs(boolean decode) {
                throw new RuntimeException("method not implemented")
            }

            @Override
            List<Object> getMatchedResources() {
                throw new RuntimeException("method not implemented")
            }

            @Override
            URI resolve(URI uri1) {
                throw new RuntimeException("method not implemented")
            }

            @Override
            URI relativize(URI uri1) {
                throw new RuntimeException("method not implemented")
            }
        }
    }


}
