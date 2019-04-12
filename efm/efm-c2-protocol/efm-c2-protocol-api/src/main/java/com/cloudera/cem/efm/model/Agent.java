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
import javax.validation.constraints.Size;

@ApiModel
public class Agent {

    public static final int NAME_MAX_SIZE = 120;

    @NotBlank
    private String identifier;

    @Size(max = NAME_MAX_SIZE)
    private String name;

    @Size(max = AgentClass.NAME_MAX_SIZE)
    private String agentClass;
    private String agentManifestId;
    private AgentStatus status;
    private Long firstSeen;
    private Long lastSeen;

    @ApiModelProperty(
            value = "A unique identifier for the Agent",
            notes = "Usually set when the agent is provisioned and deployed",
            required = true)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @ApiModelProperty(
            value = "The class or category label of the agent, e.g., 'sensor-collector'",
            notes = "Usually set when the agent is provisioned and deployed")
    public String getAgentClass() {
        return agentClass;
    }

    public void setAgentClass(String agentClass) {
        this.agentClass = agentClass;
    }

    @ApiModelProperty("The id of the agent manifest that applies to this agent.")
    public String getAgentManifestId() {
        return agentManifestId;
    }

    public void setAgentManifestId(String agentManifestId) {
        this.agentManifestId = agentManifestId;
    }

    @ApiModelProperty("An optional human-friendly name or alias for the agent")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("A timestamp (milliseconds since Epoch) for the first time the agent was seen by this C2 server")
    public Long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Long firstSeen) {
        this.firstSeen = firstSeen;
    }

    @ApiModelProperty("A timestamp (milliseconds since Epoch) for the most recent time the was seen by this C2 server")
    public Long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @ApiModelProperty("A summary of the runtime status of the agent")
    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", agentClass='" + agentClass + '\'' +
                '}';
    }
}
