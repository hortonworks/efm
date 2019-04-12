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

/**
 * Defines static methods for dealing with operation types in Coap. We could use EnumSet and
 * iterate all elements, but if ordering of those Enum ever changed we would have a discrepancy.
 * As a result, and for backwards compatibility, we will define the strict ordering here.
 */
public class OperationPayload {

    public static byte parseOperationType(OperationType type) throws ProtocolException{
        /**
         * We don't want to enforce ordering of the enum in OperationType. Each
         * service may have a different storage structure. Further, per 'Effective Java'
         * for maintainability and cross version compatibility we will rely on known values for
         * COAP.
         */
        switch(type){
            case ACKNOWLEDGE:
                return 0;
            case HEARTBEAT:
                return 1;
            case CLEAR:
                return 2;
            case DESCRIBE:
                return 3;
            case RESTART:
                return 4;
            case START:
                return 5;
            case UPDATE:
                return 6;
            case STOP:
                return 7;
        }
        throw new ProtocolException("Invalid payload type");
    }

    public static OperationType parseOperationType(final byte operation) throws ProtocolException {

        /**
         * We don't want to enforce ordering of the enum in OperationType. Each
         * service may have a different storage structure. Further, per 'Effective Java'
         * for maintainability and cross version compatibility we will rely on known values for
         * COAP.
         */
        switch(operation){
            case 0:
                return OperationType.ACKNOWLEDGE;
            case 1:
                return OperationType.HEARTBEAT;
            case 2:
                return OperationType.CLEAR;
            case 3:
                return OperationType.DESCRIBE;
            case 4:
                return OperationType.RESTART;
            case 5:
                return OperationType.START;
            case 6:
                return OperationType.STOP;
        }
        throw new ProtocolException("Invalid payload type");
    }
}
