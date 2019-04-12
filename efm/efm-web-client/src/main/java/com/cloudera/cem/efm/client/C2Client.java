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
 *
 *  This file incorporates works covered by the following copyright and permission notice:
 *
 *    Apache NiFi - MiNiFi
 *    Copyright 2014-2018 The Apache Software Foundation
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.cloudera.cem.efm.client;

import java.io.Closeable;

/**
 * A client for interacting with the REST API of a C2 Server's C2 Protocol
 */
public interface C2Client extends Closeable {

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.AgentClass}
     */
    AgentClassClient getAgentClassClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.Agent}
     */
    AgentClient getAgentClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.AgentManifest}
     */
    AgentManifestClient getAgentManifestClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.C2Heartbeat} and {@link com.cloudera.cem.efm.model.C2OperationAck}
     */
    ProtocolClient getProtocolClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.Operation}
     */
    OperationClient getOperationClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.FlowSummary}
     */
    FlowClient getFlowClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.FlowMapping}
     */
    FlowMappingClient getFlowMappingClient();

    /**
     * @return a client for interacting with {@link com.cloudera.cem.efm.model.Event}
     */
    EventClient getEventClient();

    /**
     * @return a client for obtaining configuration information about the C2 server
     */
    C2ConfigurationClient getC2ConfigurationClient();

    /**
     * The builder interface that implementations should provide for obtaining the client.
     */
    interface Builder {

        Builder config(C2ClientConfig clientConfig);

        C2ClientConfig getConfig();

        C2Client build();

    }

}
