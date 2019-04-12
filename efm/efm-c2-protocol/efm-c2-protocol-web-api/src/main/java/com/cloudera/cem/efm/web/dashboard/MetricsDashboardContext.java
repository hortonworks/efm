/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the
 * terms of the Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with
 * a third party authorized to distribute this code.  If you do not have a written agreement with Cloudera
 * or with an authorized and properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS
 *      CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT,
 *      MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS
 *      FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA
 *      IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL
 *      DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF
 *      INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.web.dashboard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class MetricsDashboardContext {

    private final String agentId;
    private final String agentClass;
    private final String flowId;

    private MetricsDashboardContext(
            String agentId,
            String agentClass,
            String flowId) {
        this.agentId = agentId;
        this.agentClass = agentClass;
        this.flowId = flowId;
    }

    private MetricsDashboardContext(Builder builder) {
        this(builder.agentId, builder.agentClass, builder.flowId);
    }

    public static MetricsDashboardContext fromAgent(final String agentId) {
        return new MetricsDashboardContext(agentId, null, null);
    }

    public static MetricsDashboardContext fromAgentClass(final String agentClass) {
        return new MetricsDashboardContext(null, agentClass, null);
    }

    public static MetricsDashboardContext fromFlow(final String flowId) {
        return new MetricsDashboardContext(null, null, flowId);
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentClass() {
        return agentClass;
    }

    public String getFlowId() {
        return flowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MetricsDashboardContext that = (MetricsDashboardContext) o;

        return new EqualsBuilder()
                .append(agentId, that.agentId)
                .append(agentClass, that.agentClass)
                .append(flowId, that.flowId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(agentId)
                .append(agentClass)
                .append(flowId)
                .toHashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String agentId = null;
        private String agentClass = null;
        private String flowId = null;

        public String getAgentId() {
            return agentId;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public String getAgentClass() {
            return agentClass;
        }

        public Builder agentClass(String agentClass) {
            this.agentClass = agentClass;
            return this;
        }

        public String getFlowId() {
            return flowId;
        }

        public Builder flowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public MetricsDashboardContext build() {
            return new MetricsDashboardContext(this);
        }

    }

}
