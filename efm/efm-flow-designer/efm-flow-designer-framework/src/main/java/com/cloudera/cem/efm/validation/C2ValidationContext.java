package com.cloudera.cem.efm.validation;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.controller.ControllerServiceLookup;
import org.apache.nifi.expression.ExpressionLanguageCompiler;

import java.util.HashMap;
import java.util.Map;

/**
 * Purpose: Validation context is needed to execute the validation. Simply provides
 * the context and is intended to be a glue layer between C2 and the validation contexts.
 *
 * Design: StandardValidation context offers more than we need. In this case,
 * we simply want to validate a single property descriptor in the confines
 * of the standard validators, so the design is inherently simple, where
 * most operations are unsupported.
 */
public class C2ValidationContext implements ValidationContext {


    private final PropertyDescriptor descriptor;
    private final String value;

    public C2ValidationContext(
            final PropertyDescriptor descriptor, final String value){
        this.descriptor = descriptor;
        this.value=value;
    }

    @Override
    public ControllerServiceLookup getControllerServiceLookup() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValidationContext getControllerServiceValidationContext(ControllerService controllerService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExpressionLanguageCompiler newExpressionLanguageCompiler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyValue newPropertyValue(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<PropertyDescriptor, String> getProperties() {
        Map<PropertyDescriptor,String> prop = new HashMap<>();
        prop.put(descriptor,value);
        return prop;
    }

    @Override
    public String getAnnotationData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidationRequired(ControllerService controllerService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExpressionLanguagePresent(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExpressionLanguageSupported(String s) {
        if (s.equals(descriptor.getName())){
            return descriptor.isExpressionLanguageSupported();
        }
        return false;
    }

    @Override
    public String getProcessGroupIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyValue getProperty(PropertyDescriptor propertyDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getAllProperties() {
        Map<String,String> prop = new HashMap<>();
        prop.put(descriptor.getName(),value);
        return prop;
    }
}
