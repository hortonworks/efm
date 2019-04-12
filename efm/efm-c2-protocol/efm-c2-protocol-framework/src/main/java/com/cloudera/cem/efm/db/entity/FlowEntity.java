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


import com.cloudera.cem.efm.db.repository.FlowRepository;
import com.cloudera.cem.efm.model.FlowFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;


@Entity
@Table(name = "FLOW")
public class FlowEntity extends AuditableEntity<String> implements FlowRepository.FlowSummary {

    @Column(name = "REGISTRY_URL")
    private String registryUrl;

    @Column(name = "REGISTRY_BUCKET_ID")
    private String registryBucketId;

    @Column(name = "REGISTRY_FLOW_ID")
    private String registryFlowId;

    @Column(name = "REGISTRY_FLOW_VERSION")
    private Integer registryFlowVersion;

    @Column(name = "FLOW_CONTENT")
    private String flowContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "FLOW_CONTENT_FORMAT")
    private FlowFormat flowFormat;

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

    public Integer getRegistryFlowVersion() {
        return registryFlowVersion;
    }

    public void setRegistryFlowVersion(Integer registryFlowVersion) {
        this.registryFlowVersion = registryFlowVersion;
    }

    public String getFlowContent() {
        return flowContent;
    }

    public void setFlowContent(String flowContent) {
        this.flowContent = flowContent;
    }

    public FlowFormat getFlowFormat() {
        return flowFormat;
    }

    public void setFlowFormat(FlowFormat flowFormat) {
        this.flowFormat = flowFormat;
    }

}
