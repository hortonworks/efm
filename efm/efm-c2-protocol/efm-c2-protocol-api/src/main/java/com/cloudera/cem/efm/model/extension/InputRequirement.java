/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
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

package com.cloudera.cem.efm.model.extension;

public enum InputRequirement {
    /**
     * This value is used to indicate that the Processor requires input from other Processors
     * in order to run. As a result, the Processor will not be valid if it does not have any
     * incoming connections.
     */
    INPUT_REQUIRED,

    /**
     * This value is used to indicate that the Processor will consume data from an incoming
     * connection but does not require an incoming connection in order to perform its task.
     * If the {@link InputRequirement} annotation is not present, this is the default value
     * that is used.
     */
    INPUT_ALLOWED,

    /**
     * This value is used to indicate that the Processor is a "Source Processor" and does
     * not accept incoming connections. Because the Processor does not pull FlowFiles from
     * an incoming connection, it can be very confusing for users who create incoming connections
     * to the Processor. As a result, this value can be used in order to clarify that incoming
     * connections will not be used. This prevents the user from even creating such a connection.
     */
    INPUT_FORBIDDEN;
}
