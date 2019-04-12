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

package com.cloudera.cem.efm.validation;

import org.apache.nifi.registry.flow.VersionedConnection;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedProcessGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FunnelValidationContext {

    public Collection<String> validate(final VersionedFunnel configuration, final VersionedProcessGroup flow) {
        final String id = configuration.getIdentifier();

        boolean incomingConnection = false;
        boolean outgoingConnection = false;
        for (final VersionedConnection connection : flow.getConnections()) {
            final String sourceId = connection.getSource().getId();
            final String destinationId = connection.getDestination().getId();

            // Ignore self loops
            if (sourceId.equals(destinationId)) {
                continue;
            }

            if (sourceId.equals(id)) { // if Funnel is the source of the connection, then the Funnel has an outgoing connection
                outgoingConnection = true;
            } else if (destinationId.equals(id)) { // If funnel is destination of connection, then it has an incoming connection
                incomingConnection = true;
            }
        }

        if (incomingConnection && outgoingConnection) {
            return Collections.emptyList();
        }

        final List<String> errors = new ArrayList<>();
        if (!incomingConnection) {
            errors.add("Funnel has no incoming Connections from any other component");
        }
        if (!outgoingConnection) {
            errors.add("Funnel has no outgoing Connection to any other component");
        }

        return errors;
    }
}
