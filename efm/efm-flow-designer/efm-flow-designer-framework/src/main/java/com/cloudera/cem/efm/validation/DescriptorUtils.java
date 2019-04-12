package com.cloudera.cem.efm.validation;

import java.util.HashSet;
import java.util.Set;

/**
 * Purpose and Justification: Simply translation utility for PropertyDescriptor objects
 * Because we are using validators, we need to translate the object types. Currently there
 * is no easy way ( as in the builder ) so this class functions as the translation unit.
 *
 */
public class DescriptorUtils {
    public static final org.apache.nifi.components.PropertyDescriptor toComponentDescriptor(
            com.cloudera.cem.efm.model.extension.PropertyDescriptor descriptor) {
        Set<String> allowableValues = new HashSet<>();
        if (null != descriptor.getAllowableValues())
        descriptor.getAllowableValues().stream().map(e -> e.getValue()).forEach(allowableValues::add);
        org.apache.nifi.components.PropertyDescriptor desc = new org.apache.nifi.components.PropertyDescriptor.Builder()
                .defaultValue(descriptor.getDefaultValue())
                .description(descriptor.getDescription())
                .dynamic(descriptor.isDynamic())
                .required(descriptor.getRequired())
                .sensitive(descriptor.getSensitive())
                .name(descriptor.getName())
                .displayName(descriptor.getDisplayName())
                .allowableValues(allowableValues.isEmpty() ? null : allowableValues).dynamic(descriptor.isDynamic())
                .build();
        return desc;
    }
}