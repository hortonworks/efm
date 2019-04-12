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

import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.extension.DefinedType;
import com.cloudera.cem.efm.model.extension.PropertyAllowableValue;
import com.cloudera.cem.efm.model.extension.PropertyDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.registry.flow.ControllerServiceAPI;
import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PropertyValidationContext {
    private static final Logger logger = LoggerFactory.getLogger(PropertyValidationContext.class);
    private final Map<String, PropertyDescriptor> propertyDescriptors;
    private final boolean dynamicPropertiesSupported;


    public PropertyValidationContext(final Map<String, PropertyDescriptor> propertyDescriptors, final boolean dynamicPropertiesSupported) {
        this.propertyDescriptors = propertyDescriptors;
        this.dynamicPropertiesSupported = dynamicPropertiesSupported;
    }

    protected Collection<String> validateProperties(final Map<String, String> properties, final VersionedProcessGroup flow, final String flowId,
                                                    final ControllerServiceLookup controllerServiceLookup) {
        final List<String> validationErrors = new ArrayList<>();

        computePropertyValidationErrors(properties, validationErrors);
        validateNoExtraProperties(properties, validationErrors);
        validateControllerServiceReferences(properties, flow, flowId, controllerServiceLookup, validationErrors);

        return validationErrors;
    }

    private void validateNoExtraProperties(final Map<String, String> properties, final Collection<String> validationErrors) {
        // If processor supports dynamic properties then there's no need to validate anything here.
        if (dynamicPropertiesSupported) {
            return;
        }

        for (final String propertyName : properties.keySet()) {
            if (!propertyDescriptors.containsKey(propertyName)) {
                validationErrors.add(String.format("Property '%s' is not supported", propertyName));
            }
        }
    }

    private void computePropertyValidationErrors(final Map<String, String> properties, final Collection<String> validationErrors) {
        for (final PropertyDescriptor descriptor : propertyDescriptors.values()) {
            final String propertyValue = getPropertyValue(descriptor, properties);
            final String displayName = getDisplayName(descriptor);

            if (propertyValue == null) {
                if (Boolean.TRUE.equals(descriptor.getRequired())) {
                    validationErrors.add(String.format("Property '%s' is required", displayName));
                }

                continue;
            }

            final String validatorName = descriptor.getValidator();
            // validation is only needed if it is supplied AND it is != VALID
            if (StringUtils.isNotBlank(validatorName) && !validatorName.equals("VALID")) {
                if (validatorName.equals("INVALID")) {
                    validationErrors.add(String.format("Property '%s' is an invalid field", displayName, validatorName));
                } else {
                    try {
                        logger.trace("{} received for {}", validatorName, propertyValue);
                        Field validatorField = StandardValidators.class.getField(validatorName);
                        final Validator validator = (Validator) validatorField.get(null);
                        final ValidationResult result = validator.validate(displayName, propertyValue, new C2ValidationContext(DescriptorUtils.toComponentDescriptor(descriptor), propertyValue));
                        if (!result.isValid()) {
                            validationErrors.add(
                                    String.format("Property '%s' has a value that is not valid according to the validator %s. reason %s", displayName, validatorName, result.getExplanation()));
                        } else {
                            logger.debug("{} is valid according to {}", propertyValue, validatorName);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
                        // this will result in an invalid validator name. In this case we are labeling this
                        // a validation error. Please note that this is a MAJOR assumption to utilizing validators.
                        logger.error("{} failed attempted validation because {}", validatorName, e.getLocalizedMessage(), e);
                        validationErrors.add(String.format("Property '%s' has a value, but failed validation because %s was not found", displayName, validatorName));
                    }
                }
            }

            final String regex = descriptor.getValidRegex();
            if (regex != null && !ExpressionLanguageUtil.isExpressionLanguagePresent(descriptor, propertyValue)) {
                final Pattern pattern = Pattern.compile(regex);
                if (!pattern.matcher(propertyValue).matches()) {
                    validationErrors.add(String.format("Property '%s' has a value that does not match the Validation Pattern.", displayName));
                }

                continue;
            }

            final List<PropertyAllowableValue> allowableValueList = descriptor.getAllowableValues();
            if (allowableValueList != null) {
                final Set<String> allowableValues = descriptor.getAllowableValues().stream()
                        .map(PropertyAllowableValue::getValue)
                        .collect(Collectors.toSet());

                if (!allowableValues.contains(propertyValue)) {
                    validationErrors.add(String.format("Property '%s' has a value that is not in the set of Allowable Values", displayName));
                }
            }
        }
    }


    private void validateControllerServiceReferences(final Map<String, String> properties, final VersionedProcessGroup flow, final String flowId,
                                                     final ControllerServiceLookup controllerServiceLookup, final Collection<String> validationErrors) {
        for (final PropertyDescriptor descriptor : propertyDescriptors.values()) {
            final String displayName = getDisplayName(descriptor);

            final DefinedType definedType = descriptor.getTypeProvidedByValue();
            if (definedType == null) {
                continue;
            }

            final String serviceId = getPropertyValue(descriptor, properties);
            if (serviceId == null) {
                continue;
            }

            final Optional<VersionedControllerService> service = findControllerService(flow, serviceId);
            if (!service.isPresent()) {
                validationErrors.add(String.format("Property '%s' references a Service that does not exist", displayName));
                continue;
            }

            final FDControllerService serviceInstance = controllerServiceLookup.lookupInstance(flowId, service.get().getIdentifier());
            if (!isServiceOfCorrectType(serviceInstance.getComponentConfiguration(), definedType)) {
                validationErrors.add(String.format("Property '%s' references the wrong type of Service", displayName));
                continue;
            }

            final Collection<String> serviceValidationErrors = serviceInstance.getValidationErrors();
            if (serviceValidationErrors != null && !serviceValidationErrors.isEmpty()) {
                validationErrors.add(String.format("Property '%s' references Service '%s' but that Service is not valid",
                        displayName, serviceInstance.getComponentConfiguration().getName()));

                continue;
            }
        }
    }

    private boolean isServiceOfCorrectType(final VersionedControllerService service, final DefinedType expectedType) {
        for (final ControllerServiceAPI candidateSvcApi : service.getControllerServiceApis()) {
            if (candidateSvcApi.getType().equals(expectedType.getType())) {
                return true;
            }
        }
        return false;

    }


    private Optional<VersionedControllerService> findControllerService(final VersionedProcessGroup group, final String serviceId) {
        for (final VersionedControllerService service : group.getControllerServices()) {
            if (service.getIdentifier().equals(serviceId)) {
                return Optional.of(service);
            }
        }

        for (final VersionedProcessGroup childGroup : group.getProcessGroups()) {
            final Optional<VersionedControllerService> childGroupService = findControllerService(childGroup, serviceId);
            if (childGroupService.isPresent()) {
                return childGroupService;
            }
        }

        return Optional.empty();
    }

    private String getDisplayName(final PropertyDescriptor descriptor) {
        final String displayName = descriptor.getDisplayName();
        return (displayName == null) ? descriptor.getName() : displayName;
    }

    private String getPropertyValue(final PropertyDescriptor descriptor, final Map<String, String> properties) {
        final String propName = descriptor.getName();
        final String propValue = properties.get(propName);
        if (propValue != null) {
            return propValue;
        }

        return descriptor.getDefaultValue();
    }
}
