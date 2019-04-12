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

package com.cloudera.cem.efm.validation;

import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestDescriptorUtils{


    @Test
    public void testMapping() {
        PropertyDescriptor desc = TestPropertyValidationContext.createDescriptor("boolean", false, Arrays.asList("true", "false"), "false");
        desc.setValidator("BOOLEAN_VALIDATOR");
        desc.setDescription("oh description");
        desc.setSensitive(false);
        org.apache.nifi.components.PropertyDescriptor otherDescriptor = DescriptorUtils.toComponentDescriptor(desc);

        // can't use a reflection equals since they are different types
        assertEquals(desc.getAllowableValues().size(),otherDescriptor.getAllowableValues().size());
        assertEquals(desc.getDefaultValue(),otherDescriptor.getDefaultValue());
        assertEquals(desc.getDescription(),otherDescriptor.getDescription());
        assertEquals(desc.getDisplayName(),otherDescriptor.getDisplayName());
        assertEquals(desc.getName(),otherDescriptor.getName());
        assertEquals(desc.getSensitive(),otherDescriptor.isSensitive());
        assertEquals(desc.getRequired(),otherDescriptor.isRequired());
    }

    @Test
    public void testMappingSomeTrue() {
        PropertyDescriptor desc = TestPropertyValidationContext.createDescriptor("boolean", true, Arrays.asList("true", "false"), "false");
        desc.setValidator("BOOLEAN_VALIDATOR");
        desc.setDescription("oh description");
        desc.setSensitive(true);
        org.apache.nifi.components.PropertyDescriptor otherDescriptor = DescriptorUtils.toComponentDescriptor(desc);

        // can't use a reflection equals since they are different types
        assertEquals(desc.getAllowableValues().size(),otherDescriptor.getAllowableValues().size());
        assertEquals(desc.getDefaultValue(),otherDescriptor.getDefaultValue());
        assertEquals(desc.getDescription(),otherDescriptor.getDescription());
        assertEquals(desc.getDisplayName(),otherDescriptor.getDisplayName());
        assertEquals(desc.getName(),otherDescriptor.getName());
        assertEquals(desc.getSensitive(),otherDescriptor.isSensitive());
        assertEquals(desc.getRequired(),otherDescriptor.isRequired());
    }


    @Test
    public void testNoAllowables() {
        PropertyDescriptor desc = TestPropertyValidationContext.createDescriptor("boolean", true, Lists.newArrayList(), "false");
        desc.setValidator("BOOLEAN_VALIDATOR");
        desc.setDescription("oh description");
        desc.setSensitive(true);
        org.apache.nifi.components.PropertyDescriptor otherDescriptor = DescriptorUtils.toComponentDescriptor(desc);

        // can't use a reflection equals since they are different types
        assertEquals(desc.getDefaultValue(),otherDescriptor.getDefaultValue());
        assertEquals(desc.getDescription(),otherDescriptor.getDescription());
        assertEquals(desc.getDisplayName(),otherDescriptor.getDisplayName());
        assertEquals(desc.getName(),otherDescriptor.getName());
        assertEquals(desc.getSensitive(),otherDescriptor.isSensitive());
        assertEquals(desc.getRequired(),otherDescriptor.isRequired());
    }

    @Test(expected = IllegalStateException.class)
    public void testNiFiDescriptorFailInvalidAllowable() {
        PropertyDescriptor desc = TestPropertyValidationContext.createDescriptor("boolean", true, Arrays.asList("true", "false"), "anotherValue");
        desc.setValidator("BOOLEAN_VALIDATOR");
        desc.setDescription("oh description");
        desc.setSensitive(true);
        org.apache.nifi.components.PropertyDescriptor otherDescriptor = DescriptorUtils.toComponentDescriptor(desc);

        fail("Should not get here");
    }

}
