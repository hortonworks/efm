/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simplified Link representation
 */
@ApiModel("Link")
public class SimpleLink {

    private String href;
    private String rel;
    private String title;
    private String type;
    private Map<String, String> params;

    private static final Set<String> SPECIAL_PARAMS = new HashSet<>(Arrays.asList(Link.REL, Link.TITLE, Link.TYPE));

    private SimpleLink() {
    }

    private SimpleLink(Link link) {
        if (link.getUri() != null) {
            href = link.getUri().toString();
        }
        if (href == null && link.getUriBuilder() != null) {
            href = link.getUriBuilder().build().toString();
        }

        if (href == null) {
            throw new IllegalArgumentException("href must not be null");
        }

        rel = link.getRel();
        title = link.getTitle();
        type = link.getType();

        final Map<String, String> extraParams = new HashMap<>();
        for (Map.Entry<String, String> e : link.getParams().entrySet()) {
            if (!SPECIAL_PARAMS.contains(e.getKey())) {
                extraParams.put(e.getKey(), e.getValue());
            }
        }
        if (!extraParams.isEmpty()) {
            this.params = extraParams;
        }
    }

    @ApiModelProperty
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        if (href == null) {
            throw new IllegalArgumentException("href must not be null");
        }
        this.href = href;
    }

    @ApiModelProperty
    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    @ApiModelProperty
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    /*
     * This is private as the call to Link.fromUri(...) has
     * a runtime dependency on Jersey, so it is only designed
     * to be called from the Adapter running in a web context
     *
     * TODO, revisit if this should be part of the model class or externalized given the runtime dependency issue.
     */
    private Link.Builder toLinkBuilder() {
        Link.Builder linkBuilder = Link.fromUri(href);
        if (rel != null) {
            linkBuilder.rel(rel);
        }
        if (title != null) {
            linkBuilder.title(title);
        }
        if (type != null) {
            linkBuilder.type(type);
        }
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                linkBuilder.param(e.getKey(), e.getValue());
            }
        }
        return linkBuilder;
    }

    public static class Adapter extends XmlAdapter<SimpleLink, Link> {

        /**
         * Convert a {@link SimpleLink} into a {@link Link}.
         *
         * @param simpleLink instance of type {@link SimpleLink}
         * @return mapped instance of type {@link Link}
         */
        @Override
        public Link unmarshal(SimpleLink simpleLink) {
            if (simpleLink == null) {
                return null;
            }

            Link.Builder lb = simpleLink.toLinkBuilder();
            return lb.build();
        }

        /**
         * Convert a {@link Link} into a {@link SimpleLink}.
         *
         * @param link instance of type {@link Link}.
         * @return mapped instance of type {@link SimpleLink}.
         */
        @Override
        public SimpleLink marshal(Link link) {
            if (link == null) {
                return null;
            }

            final SimpleLink sl = new SimpleLink(link);
            return sl;
        }
    }

    public static class ListAdapter extends XmlAdapter<List<SimpleLink>, List<Link>> {

        @Override
        public List<Link> unmarshal(List<SimpleLink> simpleLinks) {
            if (simpleLinks == null) {
                return null;
            }

            final SimpleLink.Adapter adapter = new SimpleLink.Adapter();
            final List<Link> links = new ArrayList<>();
            for (SimpleLink simpleLink : simpleLinks) {
                links.add(adapter.unmarshal(simpleLink));
            }
            return links;
        }

        @Override
        public List<SimpleLink> marshal(List<Link> links) {
            if (links == null) {
                return null;
            }

            final SimpleLink.Adapter adapter = new SimpleLink.Adapter();
            final List<SimpleLink> simpleLinks = new ArrayList<>();
            for (Link link : links) {
                simpleLinks.add(adapter.marshal(link));
            }
            return simpleLinks;

        }
    }

}
