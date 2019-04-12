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
package com.cloudera.cem.efm.client;

import com.cloudera.cem.efm.model.Flow;
import com.cloudera.cem.efm.model.FlowSnapshot;
import com.cloudera.cem.efm.model.FlowSummaries;
import com.cloudera.cem.efm.model.FlowSummary;

import java.io.IOException;

/**
 * A client for interacting with {@link com.cloudera.cem.efm.model.FlowSummary}.
 */
public interface FlowClient {

    /**
     * Creates a Flow from the provided YAML content.
     *
     * @param yaml the YAML content of the flow as a {@link String}
     * @return the FlowSummary for the created flow
     */
    FlowSummary createFlow(final String yaml) throws C2Exception, IOException;

    /**
     * Creates a flow from the provided FlowSnapshot.
     *
     * @param flowSnapshot the FlowSnapshot for the flow to create
     * @return the FlowSummary for the created flow
     */
    FlowSummary createFlow(final FlowSnapshot flowSnapshot) throws C2Exception, IOException;

    /**
     * Provides all {@link com.cloudera.cem.efm.model.FlowSummary}
     *
     * @return a listing of all {@link com.cloudera.cem.efm.model.FlowSummary}s
     */
    FlowSummaries getFlowListing() throws C2Exception, IOException;

    /**
     * Retrieves the YAML content of the flow by identifier.
     *
     * @param identifier identifier of the flow to retrieve
     * @return String YAML content of the flow with the specified identifier
     */
    String getFlowYaml(final String identifier) throws C2Exception, IOException;

    /**
     * Retrieves the full Flow object by identifier.
     *
     * @param identifier identifier of the flow to retrieve
     * @return Flow the Flow with the given identifier
     */
    Flow getFlow(final String identifier) throws C2Exception, IOException;

}
