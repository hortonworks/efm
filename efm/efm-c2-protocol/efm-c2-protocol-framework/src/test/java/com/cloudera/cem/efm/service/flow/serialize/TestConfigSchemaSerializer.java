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
package com.cloudera.cem.efm.service.flow.serialize;

import org.apache.nifi.minifi.commons.schema.ConfigSchema;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

public class TestConfigSchemaSerializer {

    @Test
    public void testConfigSchemaSerializer() throws IOException, SerializerException {
        final File configSchemaFile = new File("src/test/resources/serialize/config.yml");
        final byte[] inputBytes = Files.readAllBytes(configSchemaFile.toPath());

        final Serializer<ConfigSchema> serDe = new ConfigSchemaSerializer();

        final ConfigSchema configSchema;
        try (final InputStream in = new ByteArrayInputStream(inputBytes)){
            configSchema = serDe.deserialize(in);
        }

        Assert.assertNotNull(configSchema);
        Assert.assertEquals("MiNiFi Flow", configSchema.getFlowControllerProperties().getName());

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serDe.serialize(configSchema, out);
            out.flush();

            final byte[] outputBytes = out.toByteArray();
            Arrays.equals(inputBytes, outputBytes);
        }
    }

}
