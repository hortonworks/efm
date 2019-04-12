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
package com.cloudera.cem.efm.model.extension;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SchedulingDefaults {

    private SchedulingStrategy defaultSchedulingStrategy;
    private long defaultSchedulingPeriodMillis;
    private long penalizationPeriodMillis;
    private long yieldDurationMillis;
    private long defaultRunDurationNanos;
    private String defaultMaxConcurrentTasks;

    @ApiModelProperty("The name of the default scheduling strategy")
    public SchedulingStrategy getDefaultSchedulingStrategy() {
        return defaultSchedulingStrategy;
    }

    public void setDefaultSchedulingStrategy(SchedulingStrategy defaultSchedulingStrategy) {
        this.defaultSchedulingStrategy = defaultSchedulingStrategy;
    }

    @ApiModelProperty("The default scheduling period in milliseconds")
    public long getDefaultSchedulingPeriodMillis() {
        return defaultSchedulingPeriodMillis;
    }

    public void setDefaultSchedulingPeriodMillis(long defaultSchedulingPeriodMillis) {
        this.defaultSchedulingPeriodMillis = defaultSchedulingPeriodMillis;
    }

    @ApiModelProperty("The default penalization period in milliseconds")
    public long getPenalizationPeriodMillis() {
        return penalizationPeriodMillis;
    }

    public void setPenalizationPeriodMillis(long penalizationPeriodMillis) {
        this.penalizationPeriodMillis = penalizationPeriodMillis;
    }

    @ApiModelProperty("The default yield duration in milliseconds")
    public long getYieldDurationMillis() {
        return yieldDurationMillis;
    }

    public void setYieldDurationMillis(long yieldDurationMillis) {
        this.yieldDurationMillis = yieldDurationMillis;
    }

    @ApiModelProperty("The default run duration in nano-seconds")
    public long getDefaultRunDurationNanos() {
        return defaultRunDurationNanos;
    }

    public void setDefaultRunDurationNanos(long defaultRunDurationNanos) {
        this.defaultRunDurationNanos = defaultRunDurationNanos;
    }

    @ApiModelProperty("The default concurrent tasks")
    public String getDefaultMaxConcurrentTasks() {
        return defaultMaxConcurrentTasks;
    }

    public void setDefaultMaxConcurrentTasks(String defaultMaxConcurrentTasks) {
        this.defaultMaxConcurrentTasks = defaultMaxConcurrentTasks;
    }

}
