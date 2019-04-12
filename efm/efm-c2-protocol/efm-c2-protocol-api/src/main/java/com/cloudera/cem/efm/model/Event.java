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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApiModel
public class Event {

    public static final String UNKNOWN_SOURCE_ID = "Unknown";
    public static final int EVENT_TYPE_MAX_SIZE = 200;
    public static final int MESSAGE_MAX_SIZE = 8000;

    private String id;

    @NotNull
    private EventSeverity level;

    @NotNull
    @Size(max = EVENT_TYPE_MAX_SIZE)
    private String eventType;

    @NotBlank
    @Size(max = MESSAGE_MAX_SIZE)
    private String message;

    private Long created;

    @NotNull
    private ResourceReference eventSource;

    private ResourceReference eventDetail;

    private String agentClass;

    private Map<String, String> tags;

    private EventLinks links;

    public Event() {
    }

    private Event(Builder builder) {
        this.id = builder.id;
        this.level = builder.level;
        this.eventType = builder.eventType;
        this.message = builder.message;
        this.created = builder.created;
        this.eventSource = builder.eventSource;
        this.eventDetail = builder.eventDetail;
        this.agentClass = builder.agentClass;
        this.tags = builder.tags;
        this.links = builder.links;
    }

    @ApiModelProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty
    public EventSeverity getLevel() {
        return level;
    }

    public void setLevel(EventSeverity level) {
        this.level = level;
    }

    @ApiModelProperty
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @ApiModelProperty
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @ApiModelProperty
    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @ApiModelProperty
    public ResourceReference getEventSource() {
        return eventSource;
    }

    public void setEventSource(ResourceReference eventSource) {
        this.eventSource = eventSource;
    }

    @XmlTransient  // clients consume this information via the links field
    public ResourceReference getEventDetail() {
        return eventDetail;
    }

    public void setEventDetail(ResourceReference eventDetail) {
        this.eventDetail = eventDetail;
    }

    @ApiModelProperty
    public String getAgentClass() {
        return agentClass;
    }

    public void setAgentClass(String agentClass) {
        this.agentClass = agentClass;
    }

    @ApiModelProperty
    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void putTag(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Tag key and value must not be null.");
        }
        if (tags == null) {
            tags = new HashMap<>();
        }
        tags.put(key, value);
    }

    @XmlTransient
    public String getTag(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        if (tags != null) {
            return tags.get(key);
        } else {
            return null;
        }
    }

    @ApiModelProperty
    public EventLinks getLinks() {
        return links;
    }

    public void setLinks(EventLinks links) {
        this.links = links;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Field {

        public static final String ID = "id";
        public static final String CREATED = "created";
        public static final String SEVERITY_LEVEL = "level";
        public static final String EVENT_TYPE = "eventType";
        public static final String MESSAGE = "message";
        public static final String SOURCE_TYPE = "eventSourceType";
        public static final String SOURCE_ID = "eventSourceId";
        public static final String AGENT_CLASS = "agentClass";

        public static final String[] FIELDS = {
                ID,
                CREATED,
                SEVERITY_LEVEL,
                EVENT_TYPE,
                MESSAGE,
                SOURCE_TYPE,
                SOURCE_ID,
                AGENT_CLASS,
        };

        public static Fields allFields() {
            return new Fields(FIELDS);
        }
    }


    @ApiModel
    public static class EventLinks extends Links {

        public static final String REL_SOURCE = "source";
        public static final String REL_DETAIL = "details";
        public static final String REL_AGENT_CLASS = "agentClass";

        protected static final Set<String> KNOWN_RELS = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(REL_SOURCE, REL_DETAIL, REL_AGENT_CLASS)));

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
        @ApiModelProperty(value = "Link to the source of the event, if applicable",
                dataType = "com.cloudera.cem.efm.model.SimpleLink")
        public Link getSource() {
            return getLink(REL_SOURCE);
        }

        public void setSource(Link sourceLink) {
            setLink(REL_SOURCE, sourceLink);
        }

        @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
        @ApiModelProperty(value = "Link to the event details, if applicable",
                dataType = "com.cloudera.cem.efm.model.SimpleLink")
        public Link getDetail() {
            return getLink(REL_DETAIL);
        }

        public void setDetail(Link detailLink) {
            setLink(REL_DETAIL, detailLink);
        }

        @XmlJavaTypeAdapter(SimpleLink.Adapter.class)
        @ApiModelProperty(value = "Link to the event details, if applicable",
                dataType = "com.cloudera.cem.efm.model.SimpleLink")
        public Link getAgentClass() {
            return getLink(REL_AGENT_CLASS);
        }

        public void setAgentClass(Link agentClassLink) {
            setLink(REL_AGENT_CLASS, agentClassLink);
        }
    }

    public static class Builder {

        private String id;
        private EventSeverity level;
        private String eventType;
        private String message;
        private Long created;
        private ResourceReference eventSource;
        private ResourceReference eventDetail;
        private String agentClass;
        private Map<String, String> tags = new HashMap<>();
        private EventLinks links;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder level(EventSeverity level) {
            this.level = level;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder created(Long created) {
            this.created = created;
            return this;
        }

        public Builder eventSource(ResourceReference eventSource) {
            this.eventSource = eventSource;
            return this;
        }

        public Builder eventSource(String type, String id) {
            if (StringUtils.isBlank(type)) {
                throw new IllegalArgumentException("type cannot be blank");
            }
            this.eventSource = new ResourceReference(type, StringUtils.isBlank(id) ? UNKNOWN_SOURCE_ID : id);
            return this;
        }

        public Builder eventDetail(ResourceReference eventDetail) {
            this.eventDetail = eventDetail;
            return this;
        }

        public Builder eventDetail(String type, String id) {
            if (!(StringUtils.isBlank(type) && StringUtils.isBlank(id))) {
                this.eventDetail = new ResourceReference(type, id);
            }
            return this;
        }

        public Builder agentClass(String agentClass) {
            this.agentClass = agentClass;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder tag(String key, String value) {
            if (this.tags == null) {
                tags = new HashMap<>();
            }
            this.tags.put(key, value);
            return this;
        }

        public Builder links(EventLinks links) {
            this.links = links;
            return this;
        }

        public Event build() {
            return new Event(this);
        }
    }

}
