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

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public enum FlowFormat {

    YAML_V2_TYPE(Values.YAML_V2),

    FLOW_SNAPSHOT_JSON_V1_TYPE(Values.FLOW_SNAPSHOT_JSON_V1);

    public final String headerValue;

    FlowFormat(final String headerValue) {
        this.headerValue = headerValue;
        Objects.requireNonNull(this.headerValue);
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public static FlowFormat fromHeaderValue(final String headerValue) {
        if (StringUtils.isBlank(headerValue)) {
            throw new IllegalArgumentException("Header value cannot be null or blank");
        }

        for (FlowFormat flowFormat : values()) {
            if (flowFormat.getHeaderValue().equals(headerValue.toLowerCase())) {
                return flowFormat;
            }
        }

        throw new IllegalArgumentException("Unknown header value: " + headerValue);
    }

    /**
     * The string values of the header for reference from REST resource methods that need a string.
     */
    public static class Values {

        public static final String YAML_V2 = "application/vnd.minifi-c2+yaml;version=2";

        public static final String FLOW_SNAPSHOT_JSON_V1 = "application/vnd.minifi-c2+json;version=1";

        /**
         * NOTE - THIS LIST BE UPDATED WHEN ADDING VALUES ABOVE
         */
        public static final String ALL =
                YAML_V2 + ", " +
                FLOW_SNAPSHOT_JSON_V1;

    }
}
