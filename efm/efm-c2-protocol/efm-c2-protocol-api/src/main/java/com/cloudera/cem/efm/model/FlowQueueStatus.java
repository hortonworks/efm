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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlTransient;

@ApiModel
public class FlowQueueStatus {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long size;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long sizeMax;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long dataSize;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long dataSizeMax;

    @ApiModelProperty(value = "The number of flow files in the queue", allowableValues = "range[0, 9223372036854775807]")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @ApiModelProperty(value = "The maximum number of flow files that the queue is configured to hold", allowableValues = "range[0, 9223372036854775807]")
    public Long getSizeMax() {
        return sizeMax;
    }

    public void setSizeMax(Long sizeMax) {
        this.sizeMax = sizeMax;
    }

    @ApiModelProperty(value = "The size (in Bytes) of all flow files in the queue", allowableValues = "range[0, 9223372036854775807]")
    public Long getDataSize() {
        return dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    @ApiModelProperty(value = "The maximum size (in Bytes) that the queue is configured to hold", allowableValues = "range[0, 9223372036854775807]")
    public Long getDataSizeMax() {
        return dataSizeMax;
    }

    public void setDataSizeMax(Long dataSizeMax) {
        this.dataSizeMax = dataSizeMax;
    }

    /**
     * If sizeMax is set, returns a decimal between [0, 1] indicating the ratio
     * of size to sizeMax.
     *
     * If size or sizeMax are null, this method return null.
     *
     * @return a decimal between [0, 1] representing the sizeMax utilization percentage
     */
    @XmlTransient
    @ApiModelProperty(hidden = true)
    public Double getSizeUtilization() {
        return size != null && sizeMax != null ? (double) size / (double) sizeMax : null;
    }

    /**
     * If dataSizeMax is set, returns a decimal between [0, 1] indicating the ratio
     * of dataSize to dataSizeMax.
     *
     * @return a decimal between [0, 1] representing the dataSizeMax utilization percentage
     */
    @XmlTransient
    @ApiModelProperty(hidden = true)
    public Double getDataSizeUtilization() {
        return dataSize != null && dataSizeMax != null ? (double) dataSize / (double) dataSizeMax : null;
    }

}
