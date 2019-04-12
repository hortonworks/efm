/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.web.link;

import com.cloudera.cem.efm.model.Agent;
import com.cloudera.cem.efm.model.AgentClass;
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.ListContainer;
import com.cloudera.cem.efm.model.ResourceReference;
import com.cloudera.cem.efm.web.dashboard.MetricsDashboardContext;
import com.cloudera.cem.efm.web.dashboard.MetricsDashboardUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Link;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cloudera.cem.efm.model.Event.EventLinks.REL_AGENT_CLASS;
import static com.cloudera.cem.efm.model.Event.EventLinks.REL_DETAIL;
import static com.cloudera.cem.efm.model.Event.EventLinks.REL_SOURCE;
import static com.cloudera.cem.efm.model.Links.REL_SELF;

@Service
public class HttpLinkService implements LinkService {

    private final Map<String, LinkSupplier> linkSuppliersByType = new HashMap<>();
    private final MetricsDashboardUrlService metricsDashboardUrlService;

    // TODO, in the future, this flag could be configured via a property value
    private final boolean populateAbsoluteLinks = true;

    @Autowired
    HttpLinkService(
            List<LinkSupplier> linkSuppliers,
            MetricsDashboardUrlService metricsDashboardUrlService) {
        this.metricsDashboardUrlService = metricsDashboardUrlService;
        linkSuppliers.forEach((supplier) -> {
            final LinkSupplier previouslyMappedValue = linkSuppliersByType.putIfAbsent(supplier.getType().toLowerCase(), supplier);
            if (previouslyMappedValue != null) {
                throw new IllegalStateException("Unable to initialize class. Multiple " + LinkSupplier.class.getSimpleName() + "s found for type '" + supplier.getType() + "'");
            }
        });
    }

    @Override
    public void populateLinks(Object object, URI baseUri) {
        if (object instanceof ListContainer) {
            populateListContainerLinks((ListContainer<?>)object, baseUri);
        } else if (object instanceof Event) {
            populateEventLinks((Event)object, baseUri);
        }
    }

    private void populateListContainerLinks(ListContainer<?> listContainer, URI baseUri) {
        if (((ListContainer) listContainer).getElements() != null) {
            ((ListContainer<?>) listContainer).getElements().forEach(object -> populateLinks(object, baseUri));
        }
    }

    private void populateEventLinks(Event event, URI baseUri) {

        Event.EventLinks links = new Event.EventLinks();

        links.setSelf(generateRelLink(new ResourceReference(Event.class.getSimpleName(), event.getId()), REL_SELF, baseUri));
        links.setDetail(generateRelLink(event.getEventDetail(), REL_DETAIL, baseUri));

        //links.setSource(generateRelLink(event.getEventSource(), REL_SOURCE, baseUri));
        final ResourceReference eventSource = event.getEventSource();
        if (eventSource != null) {
            final String sourceId = eventSource.getId();
            if (sourceId != null) {

                // TODO the if/else block below for source link generation can be refactored and improved
                // First, We could improve the context if we had access to the full event context rather than just source
                //   for example, for a heartbeat, we have access to the agent, agentClass, agentManifest, flow, etc.
                //   so we could probably make this more dynamic by having something like getBestAvailableDashboard(EventContext)
                // Second, now that we are generating links to something other than REST API resource types, we should probably
                //   move away from DTO.class.getSimpleName() as the convention to something more robust

                final URI sourceUri;
                if (AgentClass.class.getSimpleName().equals(eventSource.getType())) {
                    final Optional<URI> optionalDashboard = metricsDashboardUrlService.getAgentClassDashboardUrl(MetricsDashboardContext.fromAgentClass(sourceId));
                    sourceUri = optionalDashboard.orElse(null);
                } else if (Agent.class.getSimpleName().equals(eventSource.getType())) {
                    final Optional<URI> optionalDashboard = metricsDashboardUrlService.getAgentDashboardUrl(MetricsDashboardContext.fromAgent(sourceId));
                    sourceUri = optionalDashboard.orElse(null);
                } else if (Flow.class.getSimpleName().equals(eventSource.getType())) {
                    final Optional<URI> optionalDashboard = metricsDashboardUrlService.getFlowDashboardUrl(MetricsDashboardContext.fromFlow(sourceId));
                    sourceUri = optionalDashboard.orElse(null);
                } else {
                    sourceUri = null;
                }

                if (sourceUri != null) {
                    Link sourceDashboardLink = Link.fromUri(sourceUri).rel(REL_SOURCE).title(sourceId).build();
                    links.setSource(sourceDashboardLink);
                }
            }
        }

        final String agentClass = event.getAgentClass();
        if (agentClass != null) {
            final Optional<URI> optionalDashboard = metricsDashboardUrlService.getAgentClassDashboardUrl(MetricsDashboardContext.fromAgentClass(agentClass));
            if (optionalDashboard.isPresent()) {
                Link agentClassDashboardLink = Link.fromUri(optionalDashboard.get()).rel(REL_AGENT_CLASS).title(agentClass).build();
                links.setAgentClass(agentClassDashboardLink);
            }
        }

        event.setLinks(links);

    }

    private Link generateRelLink(ResourceReference resourceReference, String rel, URI baseUri) {
        if (resourceReference == null) {
            return null;
        }

        final String lowerCaseResourceType = resourceReference.getType().toLowerCase();
        final LinkSupplier linkSupplier = linkSuppliersByType.get(lowerCaseResourceType);

        if (linkSupplier == null) {
            return null;
        }

        final Link.Builder linkBuilder = linkSupplier.generateLinkBuilder(resourceReference).rel(rel);

        if (baseUri != null && populateAbsoluteLinks) {
            linkBuilder.baseUri(baseUri);
        }

        final Object[] uriBuilderArgs = generateIdChain(resourceReference);

        return linkBuilder.build(uriBuilderArgs);
    }

    private Object[] generateIdChain(ResourceReference resourceReference) {
        final List<String> ids = generateIdChainHelper(resourceReference, new ArrayList<>());
        return ids.toArray();
    }

    private List<String> generateIdChainHelper(ResourceReference resourceReference, List<String> childrenIds) {
        if (resourceReference == null) {
            return childrenIds;
        } else {
            childrenIds.add(0, resourceReference.getId());
            return generateIdChainHelper(resourceReference.getParent(), childrenIds);
        }
    }

}