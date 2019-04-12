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
package com.cloudera.cem.efm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.Date;

@ApiModel
public class FlowSummary {

    private String id;

    private String registryUrl;

    private String registryBucketId;

    private String registryFlowId;

    private Integer registryFlowVersion;

    private FlowFormat flowFormat;

    private Date created;

    private Date updated;

    private URI uri;

    @ApiModelProperty(value = "A unique identifier of the flow", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The URL of the NiFi Registry this flow was retrieved from, or null if the flow came from direct upload")
    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    @ApiModelProperty(value = "The id of the NiFi Registry bucket this flow was retrieved from, or null if the flow came from direct upload")
    public String getRegistryBucketId() {
        return registryBucketId;
    }

    public void setRegistryBucketId(String registryBucketId) {
        this.registryBucketId = registryBucketId;
    }

    @ApiModelProperty(value = "The id of the NiFi Registry flow this flow was retrieved from, or null if the flow came from direct upload")
    public String getRegistryFlowId() {
        return registryFlowId;
    }

    public void setRegistryFlowId(String registryFlowId) {
        this.registryFlowId = registryFlowId;
    }

    @ApiModelProperty(value = "The version of the NiFi Registry flow this flow was retrieved from, or null if the flow came from direct upload")
    public Integer getRegistryFlowVersion() {
        return registryFlowVersion;
    }

    public void setRegistryFlowVersion(Integer registryFlowVersion) {
        this.registryFlowVersion = registryFlowVersion;
    }

    @ApiModelProperty(value = "The format of the flow indicating how the content should be interpreted when retrieving the flow content")
    public FlowFormat getFlowFormat() {
        return flowFormat;
    }

    public void setFlowFormat(FlowFormat flowFormat) {
        this.flowFormat = flowFormat;
    }

    @ApiModelProperty(value = "The date this flow was created in the C2 server")
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @ApiModelProperty(value = "The date this flow was updated in the C2 server")
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @ApiModelProperty(value = "The URI to retrieve this flow")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
