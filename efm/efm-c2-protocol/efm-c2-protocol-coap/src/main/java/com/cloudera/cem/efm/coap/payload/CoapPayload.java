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
package com.cloudera.cem.efm.coap.payload;

import com.cloudera.cem.efm.model.OperationType;
import com.cloudera.cem.efm.service.protocol.Payload;
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Coap packets are defined as having the following structure.
 * 2 bytes for version
 * 1 byte for payload type
 *
 * TODO: Encapsulate additional payload information into the byte
 * currently representing payload type.
 */
public abstract class CoapPayload<T> implements Payload {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoapPayload.class);

    /**
     * Embedded payload operation type.
     */
    protected OperationType payloadType;

    /**
     * Version within the coap packet, always the first byte.
     */
    protected int version;


    /**
     * Constructor accepts the version and operation of this Packet.
     * @param version the version of the coap packet format
     * @param type the operation type
     */
    public CoapPayload(int version, OperationType type) {
        this.version = version;
        this.payloadType = type;
    }


    /**
     * Defines the transformation to get the raw payload defined by the generic
     * @return internal payload
     */
    public abstract T getRawPayload() throws IOException, ProtocolException;

    @Override
    public OperationType getPayloadType() {
        return payloadType;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return version;
    }

}
