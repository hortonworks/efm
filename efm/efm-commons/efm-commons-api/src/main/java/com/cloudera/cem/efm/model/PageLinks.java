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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApiModel
public class PageLinks extends Links {

    public static final String REL_FIRST = "first";
    public static final String REL_PREVIOUS = "prev";
    public static final String REL_NEXT = "next";
    public static final String REL_LAST = "last";
    public static final String REL_NEWER = "new";

    protected static final Set<String> KNOWN_RELS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(REL_FIRST, REL_PREVIOUS, REL_NEXT, REL_LAST, REL_NEWER)));

    protected static final Set<String> ALL_KNOWN_RELS;
    static {
        ALL_KNOWN_RELS =
                Collections.unmodifiableSet(
                        Stream.concat(Links.KNOWN_RELS.stream(), KNOWN_RELS.stream()).collect(Collectors.toSet()));
    }

    @Override
    protected Set<String> getKnownRels() {
        return ALL_KNOWN_RELS;
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to the previous page",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getPrev() {
        return getLink(REL_PREVIOUS);
    }

    public void setPrev(Link prevLink) {
        setLink(REL_PREVIOUS, prevLink);
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to the next page",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getNext() {
        return getLink(REL_NEXT);
    }

    public void setNext(Link nextLink) {
        setLink(REL_NEXT, nextLink);
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to the first page",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getFirst() {
        return getLink(REL_FIRST);
    }

    public void setFirst(Link firstLink) {
        setLink(REL_FIRST, firstLink);
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to the last page",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getLast() {
        return getLink(REL_LAST);
    }

    public void setLast(Link lastLink) {
        setLink(REL_LAST, lastLink);
    }

    @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
    @ApiModelProperty(value = "Link to newer results, in the case that the resource type supports it",
            dataType = "com.cloudera.cem.efm.model.SimpleLink")
    public Link getNew() {
        return getLink(REL_NEWER);
    }

    public void setNew(Link newLink) {
        setLink(REL_NEWER, newLink);
    }
}
