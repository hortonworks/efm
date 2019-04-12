/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.client.impl.jersey;

import com.cloudera.cem.efm.client.EventClient;
import com.cloudera.cem.efm.client.C2Exception;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.QueryParameters;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JerseyEventClient extends AbstractJerseyClient implements EventClient {

    private static final String EVENTS_TARGET_PATH = "/events";

    private final WebTarget eventTarget;

    public JerseyEventClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyEventClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.eventTarget = baseTarget.path(EVENTS_TARGET_PATH);
    }

    @Override
    public ListContainer<Event> getEvents() throws C2Exception, IOException {
        return executeAction("Error retrieving events", () -> {
            final ListContainer<Event> events = getRequestBuilder(eventTarget).get(ListContainer.class);
            return events;
        });
    }

    @Override
    public ListContainer<Event> getEvents(QueryParameters queryParameters) throws C2Exception, IOException {
        if (queryParameters == null) {
            throw new IllegalArgumentException("query parameters cannot be null");
        }

        return executeAction("Error retrieving events", () -> {
            WebTarget target = addQueryParamsToTarget(eventTarget, queryParameters);

            final ListContainer<Event> events = getRequestBuilder(target).get(ListContainer.class);
            return events;
        });
    }

    @Override
    public Event getEvent(String eventId) throws C2Exception, IOException {
        if (StringUtils.isBlank(eventId)) {
            throw new IllegalArgumentException("Event ID cannot be blank");
        }

        return executeAction("Error retrieving event", () -> {
            final WebTarget target = eventTarget
                    .path("/{id}")
                    .resolveTemplate("id", eventId);

            return getRequestBuilder(target).get(Event.class);
        });
    }

}
