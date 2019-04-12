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
package com.cloudera.cem.efm.web.link;

import com.cloudera.cem.efm.model.ResourceReference;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

/**
 * A simple link supplier implementation that
 */
class SimpleLinkSupplier implements LinkSupplier {

    final Class dtoType;
    final Class resourceType;
    final String typeName;
    final String idUriParam;
    final UriBuilder uriBuilder;

    SimpleLinkSupplier(Class dtoType, Class resourceType) {
        this.dtoType = dtoType;
        this.resourceType = resourceType;
        this.typeName = dtoType.getSimpleName();
        this.idUriParam = "{" + typeName + "Id" + "}";
        this.uriBuilder = UriBuilder.fromResource(resourceType).segment(idUriParam);
    }

    @Override
    public String getType() {
        return typeName;
    }

    @Override
    public Link.Builder generateLinkBuilder(ResourceReference resourceReference) {

        if (resourceReference == null || resourceReference.getId() == null || !typeName.equalsIgnoreCase(resourceReference.getType())) {
            return null;
        }

        return Link.fromUriBuilder(uriBuilder)
                .type(typeName)
                .title(resourceReference.getId());

    }

}