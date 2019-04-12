/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel
public class Operation extends C2Operation {

    @NotBlank
    private String targetAgentId;
    @NotNull
    private OperationState state = OperationState.NEW;
    private String createdBy;
    private Long created;
    private Long updated;

    @ApiModelProperty("The identifier of the agent to which the operation applies")
    public String getTargetAgentId() {
        return targetAgentId;
    }

    public void setTargetAgentId(String targetAgentId) {
        this.targetAgentId = targetAgentId;
    }

    @ApiModelProperty(
            value = "The current state of the operation",
            readOnly = true)
    public OperationState getState() {
        return state;
    }

    public void setState(OperationState state) {
        this.state = state;
    }

    @ApiModelProperty(value = "The verified identity of the C2 server client that created the operation",
            readOnly = true,
            notes = "This field is set by the server when an operation request is submitted to identify the origin. " +
                    "When the C2 instance is secured, this is the client principal identity (e.g., certificate DN). " +
                    "When the C2 instances is unsecured, this will be 'anonymous' as client identity can not be authenticated.")
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @ApiModelProperty("The time (in milliseconds since Epoch) that this operation was created")
    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @ApiModelProperty("The time (in milliseconds since Epoch) that this operation was last updated")
    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

}
