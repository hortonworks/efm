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
package com.cloudera.cem.efm;

import com.cloudera.cem.efm.service.config.ServerDetailsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa.HibernateMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.PrintStream;

/**
 * Main class for starting the C2 Web API as a Spring Boot application.
 *
 * This class is purposely in com.cloudera.cem.efm as that is the common base
 * package across other modules. Spring Boot will use the package of this class to
 * automatically scan for beans/config/entities/etc. and would otherwise require
 * configuring custom packages to scan in several different places.
 */
@SpringBootApplication(exclude = HibernateMetricsAutoConfiguration.class)
@PropertySource("classpath:config/default.properties")
public class C2Application {

    private static final Logger logger = LoggerFactory.getLogger(C2Application.class);

    public static void main(String[] args) {
        final SpringApplication app = new SpringApplication(C2Application.class);
        // Add app customizations here
        // Note that default properties are set via a @PropertySource (instead of using app.setDefaultProperties())
        // so that the same defaults get applied when running our integration tests that use SpringBootTest.
        // This is necessary because SpringBootTest creates and runs the application without running this main() method.
        app.setBanner(new C2Application.EfmBanner());
        app.run(args);
    }

    @Component
    class OnReadyLogger implements ApplicationListener<ApplicationReadyEvent> {

        private ServerDetailsProvider serverDetailsProvider;

        @Autowired
        public OnReadyLogger(ServerDetailsProvider serverDetailsProvider) {
            this.serverDetailsProvider = serverDetailsProvider;
        }

        @Override
        public void onApplicationEvent(final ApplicationReadyEvent event) {
            ApplicationContext context = event.getApplicationContext();
            if (context != null) {
                String baseUri = serverDetailsProvider.getBaseUriBestGuess();
                if (baseUri != null) {
                    logger.info("The Edge Flow Manager has started. Services available at the following URLs:");
                    logger.info(">>> Access User Interface: {}", baseUri + "/ui");
                    logger.info(">>> Base URL for REST API: {}", baseUri + "/api");
                    logger.info(">>> Swagger REST API docs: {}", baseUri + "/swagger");
                    logger.info(">>> Status and management: {}", baseUri + "/actuator");
                }
            }
        }

    }

    static class EfmBanner implements Banner {

        static final String[] BANNER_LINES = { "",
                "  ______    ______   __    __     ",
                " /\\  ___\\  /\\  ___\\ /\\ '-./  \\    ",
                " \\ \\  __\\  \\ \\  __\\ \\ \\ \\-./\\ \\   ",
                "  \\ \\_____\\ \\ \\_\\    \\ \\_\\ \\ \\_\\  ",
                "   \\/_____/  \\/_/     \\/_/  \\/_/  ",
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>",
                "Cloudera | CEM | Edge Flow Manager",
                "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
        };

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            try {

                final String[] bannerLines = BANNER_LINES;
                final int lastLineIdx = BANNER_LINES.length-1;
                final String lastLine = bannerLines[lastLineIdx];
                final int lastLineLength = lastLine.length();

                final String applicationVersion = getApplicationVersion(sourceClass);
                if (applicationVersion != null && !applicationVersion.isEmpty()) {

                    final String formattedAppVersion = " v" + applicationVersion;
                    final int formattedAppVersionLength = formattedAppVersion.length();
                    if (lastLineLength > formattedAppVersionLength) {
                        // Right align the application version over top the last line
                        final String lastLineUpdatedWithVersion = lastLine.substring(0, lastLineLength - formattedAppVersionLength) + formattedAppVersion;
                        bannerLines[lastLineIdx] = lastLineUpdatedWithVersion;
                    } else {
                        // Left align the application version as the last line
                        bannerLines[lastLineIdx] = formattedAppVersion;
                    }
                }

                final String banner = String.join("\n", bannerLines) + "\n";
                out.println(banner);
            } catch (Exception e) {
                logger.debug("Banner not printable: " + e.getMessage(), e);
            }

        }

        protected String getApplicationVersion(Class<?> sourceClass) {
            final Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
            return (sourcePackage != null) ? sourcePackage.getImplementationVersion() : null;
        }
    }

}
