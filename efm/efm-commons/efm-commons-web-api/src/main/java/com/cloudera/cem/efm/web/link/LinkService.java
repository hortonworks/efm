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
package com.cloudera.cem.efm.web.link;

import java.net.URI;

public interface LinkService {

    /**
     * Populate links within the given DTO object.
     *
     * The input object will be modified, so it is expected to be mutable.
     *
     * The links populated on the object will be relative or absolute depending
     * on the inputs to this method and the runtime configuration of the
     * LinkService bean providing this interface.
     *
     * <p>For example:
     *
     * <ul>
     *     <li> If no {@code baseUri} is provided, relative links will be populated </li>
     *     <li> If a {@code baseUri} is provided and the LinkService bean is configured to
     *          populate absolute links when possible, absolute links will be populated </li>
     *     <li> If a {@code baseUri} is provided but the LinkService bean is configured to
     *          populate relative links, relative links will be populated
     *          (the baseUri is ignored in this case) </li>
     * </ul>
     *
     * If the LinkService bean that is loaded at runtime is capable of populating links
     * for the given input object, then it will do so. If not, this method has no side
     * effect and is essentially a no-op.
     *
     * @see LinkSupplier
     *
     * @param object - The object for which links should be populated. Will be modified if the LinkService
     *                 recognizes the Object type as one it knows about.
     * @param baseUri - Optional baseUri. If you wish to specify populating relative links, pass null.
     */
    void populateLinks(Object object, URI baseUri);

}
