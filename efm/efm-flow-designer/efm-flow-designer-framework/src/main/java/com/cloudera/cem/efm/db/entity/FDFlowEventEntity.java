/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.db.entity;

import com.cloudera.cem.efm.model.flow.FDFlowEventType;
import com.cloudera.cem.efm.model.flow.FDFlowFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "FD_FLOW_EVENT")
public class FDFlowEventEntity extends com.cloudera.cem.efm.db.entity.AuditableEntity<String> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "FLOW_ID")
    private FDFlowEntity flow;

    @Column(name = "FLOW_REVISION")
    private BigInteger flowRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE")
    private FDFlowEventType eventType;

    @Column(name = "EVENT_DESCRIPTION")
    private String eventDescription;

    @Column(name = "FLOW_CONTENT")
    private String flowContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "FLOW_CONTENT_FORMAT")
    private FDFlowFormat flowFormat;

    @Column(name = "COMPONENT_ID")
    private String componentId;

    @Column(name = "USER_IDENTITY")
    private String userIdentity;

    @Column(name = "NIFI_REGISTRY_URL")
    private String registryUrl;

    @Column(name = "NIFI_REGISTRY_BUCKET_ID")
    private String registryBucketId;

    @Column(name = "NIFI_REGISTRY_FLOW_ID")
    private String registryFlowId;

    @Column(name = "NIFI_REGISTRY_FLOW_VERSION")
    private Integer registryVersion;

    @Column(name = "LAST_PUBLISHED")
    private Date lastPublished;

    @Column(name = "LAST_PUBLISHED_USER_IDENTITY")
    private String lastPublishedUserIdentity;


    public FDFlowEventType getEventType() {
        return eventType;
    }

    public void setEventType(FDFlowEventType eventType) {
        this.eventType = eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getFlowContent() {
        return flowContent;
    }

    public void setFlowContent(String flowContent) {
        this.flowContent = flowContent;
    }

    public FDFlowFormat getFlowFormat() {
        return flowFormat;
    }

    public void setFlowFormat(FDFlowFormat flowFormat) {
        this.flowFormat = flowFormat;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public String getRegistryBucketId() {
        return registryBucketId;
    }

    public void setRegistryBucketId(String registryBucketId) {
        this.registryBucketId = registryBucketId;
    }

    public String getRegistryFlowId() {
        return registryFlowId;
    }

    public void setRegistryFlowId(String registryFlowId) {
        this.registryFlowId = registryFlowId;
    }

    public Integer getRegistryVersion() {
        return registryVersion;
    }

    public void setRegistryVersion(Integer registryVersion) {
        this.registryVersion = registryVersion;
    }

    public Date getLastPublished() {
        return lastPublished;
    }

    public void setLastPublished(Date lastPublished) {
        this.lastPublished = lastPublished;
    }

    public String getLastPublishedUserIdentity() {
        return lastPublishedUserIdentity;
    }

    public void setLastPublishedUserIdentity(String lastPublishedUserIdentity) {
        this.lastPublishedUserIdentity = lastPublishedUserIdentity;
    }

    public FDFlowEntity getFlow() {
        return flow;
    }

    public void setFlow(FDFlowEntity flow) {
        this.flow = flow;
    }

    public BigInteger getFlowRevision() {
        return flowRevision;
    }

    public void setFlowRevision(BigInteger flowRevision) {
        this.flowRevision = flowRevision;
    }

}
