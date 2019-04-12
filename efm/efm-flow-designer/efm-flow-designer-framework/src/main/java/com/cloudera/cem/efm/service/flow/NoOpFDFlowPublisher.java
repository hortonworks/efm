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
package com.cloudera.cem.efm.service.flow;

import com.cloudera.cem.efm.annotation.ConditionalOnNiFiRegistryDisabled;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import org.springframework.stereotype.Service;

/**
 * An implementation of FDFlowPublisher that is activated when NiFi Registry integration is disabled.
 */
@Service
@ConditionalOnNiFiRegistryDisabled
public class NoOpFDFlowPublisher extends BaseFDFlowPublisher {

    public NoOpFDFlowPublisher(final FDFlowManager flowManager) {
        super(flowManager);
    }

    @Override
    protected FDVersionInfo publishFlow(final FDFlow flow, final FDFlowPublishMetadata flowPublishMetadata) {
        final Integer flowVersion;
        if (flow.getVersionInfo() == null) {
            flowVersion = 1;
        } else {
            flowVersion = flow.getVersionInfo().getRegistryVersion() + 1;
        }

        final long currentTime = System.currentTimeMillis();

        final FDVersionInfo versionInfo = new FDVersionInfo();
        versionInfo.setRegistryUrl("http://localhost:18080");
        versionInfo.setRegistryBucketId("A");
        versionInfo.setRegistryFlowId("B");
        versionInfo.setRegistryVersion(flowVersion);
        versionInfo.setLastPublished(currentTime);
        versionInfo.setLastPublishedBy("anonymous");
        return versionInfo;
    }

}
