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
package com.cloudera.cem.efm.model.flow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@ApiModel
public class FDFlowEvent {

    @NotBlank
    private String identifier;

    @NotBlank
    private String flowIdentifier;

    @NotNull
    private BigInteger flowRevision;

    @NotNull
    private FDFlowEventType eventType;

    @NotNull
    private String eventDescription;

    @NotNull
    private FDFlowFormat flowFormat;

    @NotBlank
    private String componentId;

    @NotBlank
    private String userIdentity;

    @NotBlank
    private String registryUrl;

    @NotBlank
    private String registryBucketId;

    @NotBlank
    private String registryFlowId;

    @NotNull
    private Integer registryVersion;

    private Long lastPublished;

    private String lastPublishedUserIdentity;

    @NotNull
    private Long created;

    @NotNull
    private Long updated;


    @ApiModelProperty(value = "The id of the flow event", readOnly = true)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty(value = "The id of the flow that the event is for")
    public String getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(String flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @ApiModelProperty(value = "The revision number for the given flow used to order flow events per flow", readOnly = true)
    public BigInteger getFlowRevision() {
        return flowRevision;
    }

    public void setFlowRevision(BigInteger flowRevision) {
        this.flowRevision = flowRevision;
    }

    @ApiModelProperty(value = "The type of the event")
    public FDFlowEventType getEventType() {
        return eventType;
    }

    public void setEventType(FDFlowEventType eventType) {
        this.eventType = eventType;
    }

    @ApiModelProperty(value = "The description of the event")
    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    @ApiModelProperty(value = "The format of the flow content field", readOnly = true)
    public FDFlowFormat getFlowFormat() {
        return flowFormat;
    }

    public void setFlowFormat(FDFlowFormat flowFormat) {
        this.flowFormat = flowFormat;
    }

    @ApiModelProperty(value = "The id of the component that this event is for")
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @ApiModelProperty(value = "The identity of the user that performed the action")
    public String getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }

    @ApiModelProperty(value = "The URL of the NiFi Registry instance that this flow was last published to")
    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    @ApiModelProperty(value = "The bucket id in the NiFi Registry instance that this flow was last published to")
    public String getRegistryBucketId() {
        return registryBucketId;
    }

    public void setRegistryBucketId(String registryBucketId) {
        this.registryBucketId = registryBucketId;
    }

    @ApiModelProperty(value = "The flow id in the NiFi Registry instance that this flow was last published to")
    public String getRegistryFlowId() {
        return registryFlowId;
    }

    public void setRegistryFlowId(String registryFlowId) {
        this.registryFlowId = registryFlowId;
    }

    @ApiModelProperty(value = "The version in the NiFi Registry instance that this flow was last published to")
    public Integer getRegistryVersion() {
        return registryVersion;
    }

    public void setRegistryVersion(Integer registryVersion) {
        this.registryVersion = registryVersion;
    }

    @ApiModelProperty(value = "The timestamp this flow was last published")
    public Long getLastPublished() {
        return lastPublished;
    }

    public void setLastPublished(Long lastPublished) {
        this.lastPublished = lastPublished;
    }

    @ApiModelProperty(value = "The identity of the user that performed the last publish")
    public String getLastPublishedUserIdentity() {
        return lastPublishedUserIdentity;
    }

    public void setLastPublishedUserIdentity(String lastPublishedUserIdentity) {
        this.lastPublishedUserIdentity = lastPublishedUserIdentity;
    }

    @ApiModelProperty(value = "The timestamp this event was created", readOnly = true)
    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @ApiModelProperty(value = "The timestamp this event was updated", readOnly = true)
    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

}
