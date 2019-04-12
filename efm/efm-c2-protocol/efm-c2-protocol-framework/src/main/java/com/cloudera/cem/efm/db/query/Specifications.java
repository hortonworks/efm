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
package com.cloudera.cem.efm.db.query;

import com.cloudera.cem.efm.db.entity.EventEntity;
import com.cloudera.cem.efm.model.FilterOperator;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Specifications {

    static Predicate buildWherePredicate(
            final String fieldName,
            FilterOperator filterOperator,
            Object filterValue,
            boolean negated,
            final Root<EventEntity> root,
            final CriteriaQuery<?> query,
            final CriteriaBuilder builder) {

        final Predicate predicate;
        switch (filterOperator) {
            case EQ:
                predicate = builder.equal(root.get(fieldName), filterValue);
                break;
            case LIKE:
                predicate = builder.like(root.get(fieldName), "%" + filterValue.toString() + "%");
                break;
            case GT:
                if (filterValue instanceof Number) {
                    predicate = builder.gt(root.get(fieldName), (Number) filterValue);
                } else if (filterValue instanceof Comparable) {
                    predicate = builder.greaterThan(root.get(fieldName), (Comparable) filterValue);
                } else {
                    throw new IllegalArgumentException("Filter operator 'GT' requires Number or Comparable filter value");
                }
                break;
            case GTE:
                if (filterValue instanceof Number) {
                    predicate = builder.ge(root.get(fieldName), (Number) filterValue);
                } else if (filterValue instanceof Comparable) {
                    predicate = builder.greaterThanOrEqualTo(root.get(fieldName), (Comparable) filterValue);
                } else {
                    throw new IllegalArgumentException("Filter operator 'GTE' requires Number or Comparable filter value");
                }
                break;
            case LT:
                if (filterValue instanceof Number) {
                    predicate = builder.lt(root.get(fieldName), (Number) filterValue);
                } else if (filterValue instanceof Comparable) {
                    predicate = builder.lessThan(root.get(fieldName), (Comparable) filterValue);
                } else {
                    throw new IllegalArgumentException("Filter operator 'LT' requires Number or Comparable filter value");
                }
                break;
            case LTE:
                if (filterValue instanceof Number) {
                    predicate = builder.le(root.get(fieldName), (Number) filterValue);
                } else if (filterValue instanceof Comparable) {
                    predicate = builder.lessThanOrEqualTo(root.get(fieldName), (Comparable) filterValue);
                } else {
                    throw new IllegalArgumentException("Filter operator 'LTE' requires Number or Comparable filter value");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown FilterOperator " + filterOperator);
        }

        return negated ? builder.or(predicate.not(), builder.isNull(root.get(fieldName))) : predicate;
    }

    static <E extends Enum<E>> Predicate buildEnumWherePredicate(
            final String fieldName,
            FilterOperator filterOperator,
            String filterValue,
            Class<E> filterEnumClass,
            boolean negated,
            final Root<EventEntity> root,
            final CriteriaQuery<?> query,
            final CriteriaBuilder builder) {

        // TODO validation of inputs

        Predicate[] predicates = matchingEnumValues(filterOperator, filterValue, filterEnumClass)
                .map(s -> builder.equal(root.get(fieldName), s)).toArray(Predicate[]::new);

        final Predicate predicate = builder.or(predicates);

        return negated ? builder.or(predicate.not(), builder.isNull(root.get(fieldName))) : predicate;
    }

    private static <E extends Enum<E>> Stream<E> matchingEnumValues(FilterOperator operator, String thresholdValue, Class<E> enumClass) {
        final E[] possibleValues = enumClass.getEnumConstants();
        if (possibleValues == null || possibleValues.length == 0) {
            return Stream.empty();
        }

        final E thresholdEnum = enumValueOfIgnoreCase(enumClass, thresholdValue);

        switch (operator) {
            case EQ:
            case LIKE:
                return Arrays.stream(possibleValues).filter(o -> o.equals(thresholdEnum));
            case LT:
                return Arrays.stream(possibleValues).filter(o -> o.compareTo(thresholdEnum) < 0);
            case LTE:
                return Arrays.stream(possibleValues).filter(o -> o.compareTo(thresholdEnum) <= 0);
            case GT:
                return Arrays.stream(possibleValues).filter(o -> o.compareTo(thresholdEnum) > 0);
            case GTE:
                return Arrays.stream(possibleValues).filter(o -> o.compareTo(thresholdEnum) >= 0);
            default:
                throw new IllegalArgumentException("Unknown FilterOperator: " + operator);
        }
    }

    private static <E extends Enum<E>> E enumValueOfIgnoreCase(Class<E> enumClass, String value) {
        final E[] possibleValues = enumClass.getEnumConstants();
        for (E each : possibleValues) {
            if (each.name().equalsIgnoreCase(value)) {
                return each;
            }
        }
        throw new IllegalArgumentException("Unknown " + enumClass.getSimpleName() + ": '" + value + "'. Expected one of [" +
                String.join(", ", Arrays.stream(possibleValues).map(Enum::toString).collect(Collectors.toList())) +
                "]");
    }

}
