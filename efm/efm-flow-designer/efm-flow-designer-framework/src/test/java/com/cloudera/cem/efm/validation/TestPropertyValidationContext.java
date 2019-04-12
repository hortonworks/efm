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

import com.cloudera.cem.efm.model.extension.ExpressionLanguageScope;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestPropertyValidationContext {


    @Test
    public void testExtraProperties() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        final VersionedProcessGroup flow = new VersionedProcessGroup();

        final Map<String, String> properties = new HashMap<>();
        properties.put("Extra Property", "Extra Value");

        final ControllerServiceLookup serviceLookup = Mockito.mock(ControllerServiceLookup.class);

        final PropertyValidationContext dynamicNotAllowed = new PropertyValidationContext(descriptors, false);
        Collection<String> validationErrors = dynamicNotAllowed.validateProperties(properties, flow, "flow-a", serviceLookup);
        assertEquals(1, validationErrors.size());

        final PropertyValidationContext dynamicAllowed = new PropertyValidationContext(descriptors, true);
        validationErrors = dynamicAllowed.validateProperties(properties, flow,"flow-a", serviceLookup);
        assertTrue(validationErrors.isEmpty());
    }

    @Test
    public void testRequiredProperties() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        descriptors.put("abc", createDescriptor("abc", true));
        descriptors.put("xyz", createDescriptor("xyz", false));
        descriptors.put("defaulted", createDescriptor("defaulted", true, null, "default", ExpressionLanguageScope.NONE));

        final VersionedProcessGroup flow = new VersionedProcessGroup();
        final PropertyValidationContext context = new PropertyValidationContext(descriptors, false);

        final Map<String, String> properties = new HashMap<>();
        properties.put("abc", "abc");
        properties.put("xyz", "xyz");

        assertValid(context, properties, flow);

        properties.remove("abc");
        assertInvalid(context, properties, flow);

        properties.put("abc", "abc");
        properties.remove("xyz");
        assertValid(context, properties, flow);

        properties.put("defaulted", "new value");
        assertValid(context, properties, flow);
    }

    @Test
    public void testValidationRegex() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        descriptors.put("numeric", createDescriptor("numeric", true, "[-+]?\\d+", "0", null));
        descriptors.put("positive number", createDescriptor("positive number", false, "\\d+", null, ExpressionLanguageScope.VARIABLE_REGISTRY));

        final VersionedProcessGroup flow = new VersionedProcessGroup();
        final PropertyValidationContext context = new PropertyValidationContext(descriptors, false);

        final Map<String, String> properties = new HashMap<>();
        assertValid(context, properties, flow);

        properties.put("numeric", "+1");
        assertValid(context, properties, flow);

        properties.put("numeric", "-1");
        assertValid(context, properties, flow);

        properties.put("numeric", "1");
        assertValid(context, properties, flow);

        properties.put("numeric", "1.0");
        assertInvalid(context, properties, flow);

        properties.remove("numeric");
        properties.put("positive number", "4");
        assertValid(context, properties, flow);

        properties.put("positive number", "");
        assertInvalid(context, properties, flow);

        properties.put("positive number", "${using el means no validation against regex}");
        assertValid(context, properties, flow);

        properties.put("numeric", "${invalid because el not supported}");
        assertInvalid(context, properties, flow);
    }

    @Test
    public void testAllowableValues() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        PropertyDescriptor desc = createDescriptor("boolean", false, Arrays.asList("true", "false"), null);
        desc.setValidator("BOOLEAN_VALIDATOR");
        descriptors.put("boolean", desc);

        final VersionedProcessGroup flow = new VersionedProcessGroup();
        final PropertyValidationContext context = new PropertyValidationContext(descriptors, false);

        final Map<String, String> properties = new HashMap<>();
        assertValid(context, properties, flow);

        properties.put("boolean", "yes");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "true");
        assertValid(context, properties, flow);

        properties.put("boolean", "false");
        assertValid(context, properties, flow);
    }

    @Test
    public void testIntegerValidator() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        PropertyDescriptor desc = createDescriptor("boolean", false, Arrays.asList("true", "false"), null);
        desc.setValidator("INTEGER_VALIDATOR");
        descriptors.put("boolean", desc);

        final VersionedProcessGroup flow = new VersionedProcessGroup();
        final PropertyValidationContext context = new PropertyValidationContext(descriptors, false);

        final Map<String, String> properties = new HashMap<>();
        assertValid(context, properties, flow);

        properties.put("boolean", "yes");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "true");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "false");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "3");
        assertInvalid(context, properties, flow);
    }

    @Test
    public void testInvalidValidator() {
        final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        PropertyDescriptor desc = createDescriptor("boolean", false, Arrays.asList("true", "false"), null);
        // should generate NoSuchFieldException, but the end result is that all will be invalid
        // in this test case we want to ensure that if a validator is provided, but cannot be found, we set this as a
        // validator error
        desc.setValidator("INTEGER_VALIDATOOHWHATAWORLDWHATWORLDR");
        descriptors.put("boolean", desc);

        final VersionedProcessGroup flow = new VersionedProcessGroup();
        final PropertyValidationContext context = new PropertyValidationContext(descriptors, false);

        final Map<String, String> properties = new HashMap<>();
        assertValid(context, properties, flow);

        properties.put("boolean", "yes");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "true");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "false");
        assertInvalid(context, properties, flow);

        properties.put("boolean", "3");
        assertInvalid(context, properties, flow);
    }

    private void assertValid(final PropertyValidationContext context, final Map<String, String> properties, final VersionedProcessGroup flow) {
        final ControllerServiceLookup serviceLookup = Mockito.mock(ControllerServiceLookup.class);
        final Collection<String> validationErrors = context.validateProperties(properties, flow, "flow-a", serviceLookup);
        if (validationErrors.isEmpty()) {
            return;
        }

        fail("Expected configuration to be valid but it had " + validationErrors.size() + " validation errors: " + validationErrors);
    }

    private void assertInvalid(final PropertyValidationContext context, final Map<String, String> properties, final VersionedProcessGroup flow) {
        final ControllerServiceLookup serviceLookup = Mockito.mock(ControllerServiceLookup.class);
        final Collection<String> validationErrors = context.validateProperties(properties, flow, "flow-a", serviceLookup);
        if (!validationErrors.isEmpty()) {
            return;
        }

        fail("Expected configuration to be invalid but it was valid");
    }

    public static PropertyDescriptor createDescriptor(final String name, final boolean required) {
        return createDescriptor(name, required, null, null, ExpressionLanguageScope.NONE);
    }

    public static PropertyDescriptor createDescriptor(final String name, final boolean required, final Collection<String> allowableValues, final String defaultValue) {
        final List<PropertyAllowableValue> allowables = allowableValues.stream()
            .map(val -> {
                final PropertyAllowableValue value = new PropertyAllowableValue();
                value.setDescription(val);
                value.setDisplayName(val);
                value.setValue(val);
                return value;
            }).collect(Collectors.toList());

        final PropertyDescriptor descriptor = new PropertyDescriptor();
        descriptor.setName(name);
        descriptor.setDisplayName(name);
        descriptor.setRequired(required);
        descriptor.setAllowableValues(allowables);
        descriptor.setDefaultValue(defaultValue);

        return descriptor;
    }

    public static PropertyDescriptor createDescriptor(final String name, final boolean required, final String regex, final String defaultValue, final ExpressionLanguageScope elScope) {
        final PropertyDescriptor descriptor = new PropertyDescriptor();
        descriptor.setName(name);
        descriptor.setDisplayName(name);
        descriptor.setRequired(required);
        descriptor.setValidRegex(regex);
        descriptor.setDefaultValue(defaultValue);
        descriptor.setExpressionLanguageScope(elScope);

        return descriptor;
    }
}
