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
package com.cloudera.cem.efm.model;


import org.junit.Test;

import javax.ws.rs.core.Link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/*
 * This test exists in the c2-protocol-web-api module instead
 * of c2-protocol-api because the Jax-RS Link.Builder
 * methods called for the SimpleLink -> Link conversion
 * have a runtime dependency on a Jersey RuntimeDelegate
 */
public class TestSimpleLink {

    @Test
    public void testAdapter() {
        // Arrange
        final Link link = Link.fromUri("http://example.com/test")
                .rel("self")
                .title("This is a website.")
                .type("text/html")
                .param("x-http-method", "GET")
                .build();

        // Act (Link -> SimpleLink)
        final SimpleLink simpleLink = new SimpleLink.Adapter().marshal(link);

        // Assert
        assertEquals("http://example.com/test", simpleLink.getHref());
        assertEquals("self", simpleLink.getRel());
        assertEquals("This is a website.", simpleLink.getTitle());
        assertEquals("text/html", simpleLink.getType());
        assertEquals("GET", simpleLink.getParams().get("x-http-method"));

        // Act (SimpleLink back to Link)
        final Link unmarshalledLink = new SimpleLink.Adapter().unmarshal(simpleLink);

        // Assert
        assertEquals(link, unmarshalledLink);
    }

    @Test
    public void testAdapterWithPartialLink() {
        // Arrange
        final Link link = Link.fromUri("http://example.com/test").build();

        // Act (Link -> SimpleLink)
        final SimpleLink simpleLink = new SimpleLink.Adapter().marshal(link);

        // Assert
        assertEquals("http://example.com/test", simpleLink.getHref());
        assertNull(simpleLink.getRel());
        assertNull(simpleLink.getTitle());
        assertNull(simpleLink.getType());
        assertNull(simpleLink.getParams());

        // Act (SimpleLink back to Link)
        final Link unmarshalledLink = new SimpleLink.Adapter().unmarshal(simpleLink);

        // Assert
        assertEquals(link, unmarshalledLink);

    }

}
