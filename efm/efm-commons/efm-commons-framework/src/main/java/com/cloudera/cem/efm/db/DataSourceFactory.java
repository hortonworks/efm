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
 *
 */
package com.cloudera.cem.efm.db;

import com.cloudera.cem.efm.profile.NotTestProfile;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@NotTestProfile  // disable this configuration class anytime tests are active, as tests use the in memory data source or provide their own.
public class DataSourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    static final String DEFAULT_DATABASE_PASSWORD = "efm";

    private final C2DatabaseProperties properties;

    private DataSource dataSource;

    @Autowired
    public DataSourceFactory(final C2DatabaseProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public DataSource getDataSource() {
        if (dataSource == null) {
            final String databaseUrl = properties.getUrl();
            if (StringUtils.isBlank(databaseUrl)) {
                throw new IllegalStateException(C2DatabaseProperties.URL + " is required");
            }

            final String databaseDriver = properties.getDriverClass();
            if (StringUtils.isBlank(databaseDriver)) {
                throw new IllegalStateException(C2DatabaseProperties.DRIVER + " is required");
            }

            final String databaseUsername = properties.getUsername();
            if (StringUtils.isBlank(databaseUsername)) {
                throw new IllegalStateException(C2DatabaseProperties.USERNAME + " is required");
            }

            String databasePassword = properties.getPassword();
            if (StringUtils.isBlank(databasePassword)) {
                databasePassword = DEFAULT_DATABASE_PASSWORD;
                LOGGER.warn("\n\n*************************************************************************************************************\n\n" +
                        "WARNING: Using default database password, please configure a password via " + C2DatabaseProperties.PASSWORD +
                        "\n\n*************************************************************************************************************\n\n");
            }

            dataSource = DataSourceBuilder
                    .create()
                    .url(databaseUrl)
                    .driverClassName(databaseDriver)
                    .username(databaseUsername)
                    .password(databasePassword)
                    .build();

            // We should end up with Hikari here b/c we exclude tomcat-jdbc and don't have commons-dbcp on the classpath
            if (dataSource instanceof HikariDataSource) {
                LOGGER.info("Setting maximum pool size on HikariDataSource to {}", new Object[]{properties.getMaxConnections()});
                ((HikariDataSource)dataSource).setMaximumPoolSize(properties.getMaxConnections());
            }
        }

        return dataSource;
    }
}
