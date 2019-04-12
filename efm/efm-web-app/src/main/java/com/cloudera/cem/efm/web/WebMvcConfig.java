/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This class adds custom configuration to the default WebMvcConfigurer
 * that comes with Spring Boot Starter Web auto configuration.
 *
 * Spring MVC, specifically the DispatcherServlet, is serving the static assets,
 * whereas Jersey is being using for the REST API endpoints.
 */
@Component
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Handle redirects and forwards for /swagger static assets folder
        registry.addViewController("/swagger").setViewName("redirect:/swagger/");
        registry.addViewController("/swagger/").setViewName("forward:/swagger/ui.html");

        // Handle redirects and forwards for /ui static assets folder
        registry.addViewController("/").setViewName("redirect:/ui/");
        registry.addViewController("/ui").setViewName("redirect:/ui/");
        registry.addViewController("/ui/").setViewName("forward:/ui/index.html");

        // TODO - uncomment if we want path based routing strategy
//        registry.addViewController("/ui/monitor").setViewName("forward:/ui/index.html");
//        registry.addViewController("/ui/monitor/events").setViewName("forward:/ui/index.html");
//        registry.addViewController("/ui/class-detail/{class-id:\\w+}").setViewName("forward:/ui/index.html");
//        registry.addViewController("/ui/instance-detail/{instance-id:[a-fA-F0-9\\-]{36}}").setViewName("forward:/ui/index.html");
//        registry.addViewController("/ui/server-detail/{server-id:\\w+}").setViewName("forward:/ui/index.html");
    }
}
