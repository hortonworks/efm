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
package com.cloudera.cem.efm.util;

import com.cloudera.cem.efm.security.NiFiUser;
import com.cloudera.cem.efm.security.StandardNiFiUser;
import com.cloudera.cem.efm.service.FDServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionSystemException;

import javax.annotation.PostConstruct;
import javax.persistence.EntityExistsException;

/**
 * Populates the flow designer's flow table in the DB.
 *
 * NOTE: This was previously a spring-bean that was enabled with the dev mode profile, but since we have the
 * AgentClassMonitor running that will auto-create the flows, we don't really need this, so currently not enabled.
 */
public class DevFlowPopulator {

    private static final Logger logger = LoggerFactory.getLogger(DevFlowPopulator.class);

    private FDServiceFacade fdServiceFacade;

    @Autowired
    public DevFlowPopulator(final FDServiceFacade fdServiceFacade) {
        this.fdServiceFacade = fdServiceFacade;
    }

    @PostConstruct
    public void populateFlows() {
        try {
            final NiFiUser user = new StandardNiFiUser.Builder().identity("test-user").build();
            fdServiceFacade.createFlow("Class A", user);
            fdServiceFacade.createFlow("Class B", user);
        } catch (EntityExistsException | TransactionSystemException e) {
            logger.debug("Unable to create dev flows due to exception. It is possible they already exist in the database.", e);
        }
    }

}
