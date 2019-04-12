/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *   (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *       LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *       FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *       DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *       TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *       UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.c2protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class HeartbeatContext {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatContext.class);

    private static final HeartbeatContext EMPTY = builder().build();

    private final URI baseUri;
    private final Long contentLength;

    HeartbeatContext(Builder builder) {
        this.baseUri = builder.baseUri;
        this.contentLength = builder.contentLength;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HeartbeatContext empty() {
        return EMPTY;
    }

    public static class Builder {

        private URI baseUri;
        private Long contentLength;

        private Builder() {
        }

        public Builder baseUri(URI baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder contentLength(String contentLength) {
            try {
                this.contentLength = Long.valueOf(contentLength);
            } catch (NumberFormatException e) {
                logger.debug("Could not parse content length string: " + contentLength, e);
            }
            return this;
        }

        public HeartbeatContext build() {
            return new HeartbeatContext(this);
        }
    }
}
