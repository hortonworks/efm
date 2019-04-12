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
import com.cloudera.cem.efm.service.protocol.ProtocolException;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Defines reusable receiver capabilities
 *
 * Design extends CoapPayload, which contains basic Coap configuration and accessor functions
 * The resulting implementations will need to implement the function to get the raw payload
 * defined by the template.
 */
public abstract class ReceivingPayload<T> extends CoapPayload<T>{

    private static final Logger LOGGER = LoggerFactory.getLogger(CoapPayload.class);

    /**
     * Coap Packet reference.
     */
    private final CoapExchange packet;

    /**
     * Input stream.
     */
    final protected DataInputStream inputStream;

    /**
     * Constructor that accepts a CoapPacket as an input
     * @param packet coap packet
     * @throws ProtocolException if there is a problem constructing the payload from the CoapPacket
     */
    public ReceivingPayload(final CoapExchange packet) throws ProtocolException {
        super(0, OperationType.HEARTBEAT);

        this.packet=packet;

        inputStream = new DataInputStream( new ByteArrayInputStream(packet.getRequestPayload()) );

        try {
            version = inputStream.readShort();
            payloadType = OperationPayload.parseOperationType(inputStream.readByte());
        } catch (IOException e) {
            LOGGER.error("Exception while parsing operation type",e);
            throw new ProtocolException("IO Exception occurred in parsing payload");
        }
    }

    /**
     * Parses a string. Since readUTF may have a variadic size prefix, we will
     * simply read an unsigned short and then the bytes into a string to accommodate
     * the other languages that send data.
     * @return String
     * @throws IOException Report I/O Exceptions that occur during this process.
     */
    protected String parseString() throws IOException {
        final int size = inputStream.readUnsignedShort();
        final byte [] str = new byte[size];
        inputStream.readFully(str);
        return new String(str);
    }

}
