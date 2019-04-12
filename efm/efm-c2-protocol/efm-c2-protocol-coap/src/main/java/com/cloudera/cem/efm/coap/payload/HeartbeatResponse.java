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

import com.cloudera.cem.efm.model.C2HeartbeatResponse;
import com.cloudera.cem.efm.model.C2Operation;
import com.cloudera.cem.efm.model.OperationType;
import com.cloudera.cem.efm.service.protocol.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HeartbeatResponse extends CoapPayload<byte[]> implements CoapResponse {

    private final C2HeartbeatResponse response;

    public HeartbeatResponse(final int version, final C2HeartbeatResponse response){
        super(version,OperationType.ACKNOWLEDGE);
        this.response = response;
    }

    @Override
    public byte[] getRawPayload() throws IOException, ProtocolException {
        final  ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final DataOutputStream dataOut = new DataOutputStream(byteStream);
        dataOut.writeShort(version);

        final List<C2Operation> operations = response.getRequestedOperations();

        if (null != operations) {
            dataOut.writeShort(operations.size());
            for (C2Operation operation : operations) {
                dataOut.writeByte(OperationPayload.parseOperationType(operation.getOperation()));
                // TODO: write UTF -- consider reducing identifier to binary representation
                dataOut.writeUTF(operation.getIdentifier());
                // TODO: consider reducing operand to a single byte.
                dataOut.writeUTF(operation.getOperand());
                final Map<String,String> arguments = operation.getArgs();
                dataOut.writeShort(arguments.size());
                // key followed by value. Other than that the order of each
                // key/value is unimportant.
                arguments.forEach( (key,value) ->{
                    try {
                        dataOut.writeUTF(key);
                        dataOut.writeUTF(value);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } else{
            dataOut.writeShort(0);
        }

        return byteStream.toByteArray();
    }

    @Override
    public Optional<byte[]> getCoapPacket()  throws IOException, ProtocolException {
        if (response != null){
            return Optional.of( getRawPayload() );
        }
        return Optional.empty();
    }
}
