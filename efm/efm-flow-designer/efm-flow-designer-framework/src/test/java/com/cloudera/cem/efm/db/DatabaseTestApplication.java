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
package com.cloudera.cem.efm.db;

import com.cloudera.cem.efm.service.agent.AgentService;
import com.cloudera.cem.efm.service.agent.ELSpecificationProvider;
import com.cloudera.cem.efm.service.config.C2ConfigurationService;
import com.cloudera.cem.efm.service.flow.C2FlowService;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa.HibernateMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

/**
 * Sets up the application context for database repository tests.
 */
@SpringBootApplication(
        exclude = HibernateMetricsAutoConfiguration.class
)
@ComponentScan(
        // NOTE: This base package is purposely "com.cloudera.cem.efm" so that it can be shared
        // by the spring-data repository tests and the database flow manager tests. Since it is at the root package it finds
        // more beans then it would have been if it were nested inside the db package, and the additional beans depend on beans
        // from c2-protocol which are not available so we provide mocks of these below.
        basePackages = { "com.cloudera.cem.efm" },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        value = DataSourceFactory.class)
        })
@PropertySource("classpath:/conf/database-test.properties")
public class DatabaseTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseTestApplication.class, args);
    }

    // Provide mocks for beans normally provided by c2-protocol-framework at runtime

    @Bean
    public AgentService getAgentService() {
        return Mockito.mock(AgentService.class);
    }

    @Bean
    public C2FlowService getC2FlowService() {
        return Mockito.mock(C2FlowService.class);
    }

    @Bean
    public C2ConfigurationService getC2ConfigurationService() {
        return Mockito.mock(C2ConfigurationService.class);
    }

    @Bean
    public ELSpecificationProvider getELSpecificationProvider() {
        return Mockito.mock(ELSpecificationProvider.class);
    }
}
