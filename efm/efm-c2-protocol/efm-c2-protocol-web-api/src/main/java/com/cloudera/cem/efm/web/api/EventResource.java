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
package com.cloudera.cem.efm.web.api;

import com.cloudera.cem.efm.service.event.EventService;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.Fields;
import com.cloudera.cem.efm.model.FilterOperator;
import com.cloudera.cem.efm.model.FilterParameter;
import com.cloudera.cem.efm.model.Links;
import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.PageLinks;
import com.cloudera.cem.efm.model.QueryParameters;
import com.cloudera.cem.efm.model.SortParameter;
import com.cloudera.cem.efm.web.link.LinkService;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Path("events")
@Api(value = "Events", description = "Retrieval of events")
public class EventResource extends ApplicationResource {

    private final EventService eventService;
    private final LinkService linkService;

    @Autowired
    public EventResource(EventService eventService, LinkService linkService) {
        this.eventService = eventService;
        this.linkService = linkService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get events",
            response = ListContainer.class)
    public Response getEvents(
            @QueryParam("pageNum")
                    Integer pageNum,
            @QueryParam("rows")
                    Integer numRows,
            @QueryParam("sort")
                    List<String> sorts,
            @QueryParam("filter")
                    List<String> filters) {

        // TODO validation

        final List<SortParameter> sortParameters = sorts != null ? sorts.stream().map(SortParameter::fromString).collect(Collectors.toList()) : null;
        final List<FilterParameter> filterParameters = filters != null ? filters.stream().map(FilterParameter::fromString).collect(Collectors.toList()) : null;

        final List<FilterParameter> inferredFilterParameters = filterParameters != null ? new ArrayList<>(filterParameters) : new ArrayList<>();
        if (!containsPageDriftFilter(inferredFilterParameters)) {
            inferredFilterParameters.add(getDefaultPageDriftFilter());
        }

        final QueryParameters queryParameters = new QueryParameters.Builder()
                .pageNum(pageNum)
                .numRows(numRows)
                .addSorts(sortParameters)
                .addFilters(inferredFilterParameters)
                .build();

        final ListContainer<Event> eventsPage = eventService.getEvents(queryParameters);

        linkService.populateLinks(eventsPage, getBaseUri());

        // Populate PageLinks TODO, move this logic into a service
        final int currentPage = pageNum != null ? pageNum : 0;
        final long lastPage = eventsPage.getPage().getTotalPages()-1;

        final UriBuilder pageLinkUriBuilder = UriBuilder.fromResource(this.getClass());
        if (queryParameters.getSortParameters() != null && !queryParameters.getSortParameters().isEmpty()) {
            List<String> sortValues = queryParameters.getSortParameters().stream().map(SortParameter::toString).collect(Collectors.toList());
            sortValues.forEach((value) -> pageLinkUriBuilder.queryParam("sort", value));
        }
        final UriBuilder newerItemsLinkUriBuilder = pageLinkUriBuilder.clone();  // copy this for use later
        if (queryParameters.getFilterParameters() != null && !queryParameters.getFilterParameters().isEmpty()) {
            List<String> filterValues = queryParameters.getFilterParameters().stream().map(FilterParameter::toString).collect(Collectors.toList());
            filterValues.forEach((value) -> pageLinkUriBuilder.queryParam("filter", value));
        }
        if (numRows != null) {
            pageLinkUriBuilder.queryParam("rows", numRows);
        }
        pageLinkUriBuilder.queryParam("pageNum", "{page}");

        final PageLinks pageLinks = new PageLinks();
        pageLinks.setSelf(Link.fromUriBuilder(pageLinkUriBuilder).rel(Links.REL_SELF).build(currentPage));
        pageLinks.setFirst(Link.fromUriBuilder(pageLinkUriBuilder).rel(PageLinks.REL_FIRST).build(0));
        pageLinks.setLast(Link.fromUriBuilder(pageLinkUriBuilder).rel(PageLinks.REL_LAST).build(lastPage));
        if (currentPage > 0) {
            pageLinks.setPrev(Link.fromUriBuilder(pageLinkUriBuilder).rel(PageLinks.REL_PREVIOUS).build(currentPage-1));
        }
        if (currentPage < lastPage) {
            pageLinks.setNext(Link.fromUriBuilder(pageLinkUriBuilder).rel(PageLinks.REL_NEXT).build(currentPage+1));
        }

        // TODO this code for the newerLink is too clever and possibly brittle, and needs to be refactored into something maintainable and testable
        if (queryParameters.getFilterParameters() != null && !queryParameters.getFilterParameters().isEmpty()) {
            List<String> filterValues = queryParameters.getFilterParameters().stream()
                    .map((fp) -> isPageDriftFilter(fp) ? fp.not() : fp)
                    .map(FilterParameter::toString).collect(Collectors.toList());
            filterValues.forEach((value) -> newerItemsLinkUriBuilder.queryParam("filter", value));
        }
        // don't need page num or num rows for this link
        pageLinks.setNew(Link.fromUriBuilder(newerItemsLinkUriBuilder).rel(PageLinks.REL_NEWER).build());

        eventsPage.setLinks(pageLinks);

        return Response.ok(eventsPage).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get a specific event",
            response = Event.class
    )
    @ApiResponses({
            @ApiResponse(code = 404, message = HttpStatusMessages.MESSAGE_404) })
    public Response getEvent(
            @PathParam("id")
            @ApiParam(value = "The id of the event to retrieve")
                    String id) {
        final Optional<Event> eventOptional = eventService.getEvent(id);
        final Event event = eventOptional.orElseThrow(() -> new NotFoundException("No event with matching id '" + id + "'"));
        linkService.populateLinks(event, getBaseUri());
        return Response.ok(event).build();
    }

    @GET
    @Path("fields")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieves the available field names for searching or sorting events.",
            response = Fields.class
    )
    public Response getAvailableEventFields() {
        final Fields eventFields = Event.Field.allFields();
        return Response.status(Response.Status.OK).entity(eventFields).build();
    }

    // Helpers

    private FilterParameter getDefaultPageDriftFilter() {
        return new FilterParameter(Event.Field.CREATED, FilterOperator.LTE, String.valueOf(System.currentTimeMillis()));
    }

    private boolean containsPageDriftFilter(Collection<FilterParameter> filterParameters) {
        boolean containsPageDriftFilter = filterParameters == null || filterParameters.stream().anyMatch(this::isPageDriftFilter);
        return containsPageDriftFilter;
    }

    private boolean isPageDriftFilter(FilterParameter filterParameter) {
        if (filterParameter.getFieldName().equalsIgnoreCase(Event.Field.CREATED)) {
            if (filterParameter.getFilterOperator().equals(FilterOperator.LTE)
                    || filterParameter.getFilterOperator().equals(FilterOperator.LT)) {
                return true;
            }
            if (filterParameter.isNegated()
                    && (filterParameter.getFilterOperator().equals(FilterOperator.GTE)
                    || filterParameter.getFilterOperator().equals(FilterOperator.GT))) {
                return true;
            }
        }
        return false;
    }

}
