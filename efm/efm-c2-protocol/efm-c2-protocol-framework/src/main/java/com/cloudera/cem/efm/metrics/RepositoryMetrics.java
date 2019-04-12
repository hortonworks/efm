/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
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
package com.cloudera.cem.efm.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;
import io.micrometer.core.lang.NonNullApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;

@NonNullApi
@Component
@Qualifier(DelayedMeterRegistryConfigurer.METER_BINDER_QUALIFIER)
public class RepositoryMetrics implements MeterBinder {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryMetrics.class);

    private final Collection<CrudRepository> repositories;

    @Autowired
    public RepositoryMetrics(Collection<CrudRepository> crudRepositories) {
        this.repositories = crudRepositories;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        repositories.forEach((repository) -> {
            registerCrudRepository(registry, Name.Prefix.C2_REPO, repository);
        });
    }

    private void registerCrudRepository(MeterRegistry registry, String meterPrefix, CrudRepository repository) {

        try {
            final String repositoryInterfaceName = repository.getClass().getInterfaces()[0].getSimpleName();
            final String meterName = Name.join(meterPrefix, repositoryInterfaceName, Name.Suffix.SIZE);

            if (registry.find(meterName).meter() == null) {
                Gauge.builder(meterName, repository, CrudRepository::count)
                        .description("the number of persisted entities in a repository")
                        .register(registry);
            }
        } catch (Exception e) {
            logger.warn("Failed to bind meters for " + repository.toString() + "due to exception.", e);
        }

    }

}
