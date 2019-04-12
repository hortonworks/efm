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
import org.apache.nifi.registry.flow.VersionedProcessGroup;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Objects;

@ApiModel
public class FDFlow {

    @Valid
    @NotNull
    private FDFlowMetadata flowMetadata;

    @Valid
    @NotNull
    private VersionedProcessGroup flowContent;

    @Valid
    private FDVersionInfo versionInfo;

    @NotNull
    private BigInteger localFlowRevision;

    @ApiModelProperty(value = "The flow metadata", readOnly = true)
    public FDFlowMetadata getFlowMetadata() {
        return flowMetadata;
    }

    public void setFlowMetadata(FDFlowMetadata flowMetadata) {
        this.flowMetadata = flowMetadata;
    }

    @ApiModelProperty(value = "The flow contents", readOnly = true)
    public VersionedProcessGroup getFlowContent() {
        return flowContent;
    }

    public void setFlowContent(VersionedProcessGroup flowContent) {
        this.flowContent = flowContent;
    }

    @ApiModelProperty(value = "The version information for the flow", readOnly = true)
    public FDVersionInfo getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(FDVersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    @ApiModelProperty(value = "The local revision number for the given flow used to order flow events per flow", readOnly = true)
    public BigInteger getLocalFlowRevision() {
        return localFlowRevision;
    }

    public void setLocalFlowRevision(BigInteger localFlowRevision) {
        this.localFlowRevision = localFlowRevision;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.flowMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof FDFlow)) {
            return false;
        }

        final FDFlow other = (FDFlow) obj;
        return Objects.equals(this.getFlowMetadata(), other.getFlowMetadata());
    }

}
