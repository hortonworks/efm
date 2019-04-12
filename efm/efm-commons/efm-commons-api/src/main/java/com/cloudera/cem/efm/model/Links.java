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
package com.cloudera.cem.efm.model;

import io.swagger.annotations.ApiModelProperty;

import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Links {

    public static final String REL_SELF = "self";
    protected static final Set<String> KNOWN_RELS = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(REL_SELF)));

    protected final Map<String, Link> links;

    public Links() {
        this.links = new TreeMap<>();
    }

    @XmlTransient
    protected Set<String> getKnownRels() {
        return KNOWN_RELS;
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to this resource entity",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getSelf() {
        return getLink(REL_SELF);
    }

    public void setSelf(Link selfLink) {
        setLink(REL_SELF, selfLink);
    }

    @XmlJavaTypeAdapter(SimpleLink.ListAdapter.class)
    @ApiModelProperty(value = "List of other rel links that have been set",
            dataType = "List[com.cloudera.cem.efm.model.SimpleLink]")
    public List<Link> getOther() {
        final List<Link> otherLinks = new ArrayList<>();
        for (Link link : links.values()) {
            if (!relIsKnown(link.getRel())) {
                otherLinks.add(link);
            }
        }
        return otherLinks.isEmpty() ? null : otherLinks;
    }

    public void setOther(List<Link> otherLinks) {
        if (otherLinks != null) {
            for (Link link : otherLinks) {
                addLink(link);
            }
        }
    }

    @XmlTransient
    public Link getLink(String rel) {
        return links.get(rel);
    }

    protected void setLink(String relKey, Link link) {
        if (relKey == null) {
            throw new IllegalArgumentException("rel must not be null");
        }

        if (link == null) {
            links.remove(relKey);
            return;
        }

        if (!relKey.equals(link.getRel())) {
            throw new IllegalArgumentException("link rel expected to be '" + relKey + "', was '" + link.getRel() + "'");
        }

        links.put(relKey, link);
    }

    public void addLink(Link link) {
        if (link.getRel() == null) {
            throw new IllegalArgumentException("Link rel must be not null");
        }
        links.put(link.getRel(), link);
    }

    private boolean relIsKnown(String rel) {
        return (getKnownRels() != null && getKnownRels().contains(rel));
    }

}
