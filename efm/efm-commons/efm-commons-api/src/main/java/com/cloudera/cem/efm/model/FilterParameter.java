/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
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
package com.cloudera.cem.efm.model;

public class FilterParameter {

    public static final String NEGATION_SYMBOL = "-";

    public static final String API_PARAM_DESCRIPTION =
            "Apply client-defined filtering to the resulting list of resource objects. " +
                    "The value of this parameter should be in the format \"field:operator:value\". " +
                    "Valid values for 'field' can be discovered via GET :resourceURI/fields. " +
                    "Valid values for 'operator' are: " +
                        "'eq' (equal to), " +
                        "'like' (like), " +
                        "'gt' (greater than), " +
                        "'gte' (greater than or equal to), " +
                        "'lt' (less than), " +
                        "'lte' (less than or equal to). " +
                    "Any operator can be negated by prefixing it with '-'; " +
                    "for example '-eq' becomes 'not equal to'.";

    private final String fieldName;
    private final FilterOperator filterOperator;
    private final String filterValue;
    private final boolean negated;

    public FilterParameter(final String fieldName, final FilterOperator filterOperator, final String filterValue) {
        this(fieldName, filterOperator, filterValue, false);
    }

    public FilterParameter(final String fieldName, final FilterOperator filterOperator, final String filterValue, boolean negated) {
        this.fieldName = fieldName;
        this.filterOperator = filterOperator;
        this.filterValue = filterValue;
        this.negated = negated;

        if (this.fieldName == null || this.fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("fieldName cannot be empty");
        }
        if (this.filterOperator == null) {
            throw new IllegalArgumentException("filterOperator cannot be null");
        }
        if (this.filterValue == null) {
            throw new IllegalArgumentException("filterValue cannot be null");
        }

    }

    public String getFieldName() {
        return fieldName;
    }

    public FilterOperator getFilterOperator() {
        return filterOperator;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public boolean isNegated() {
        return negated;
    }

    public static FilterParameter fromString(final String filterExpression) {
        if (filterExpression == null) {
            throw new IllegalArgumentException("filterExpression cannot be null");
        }

        final String[] filterExpParts = filterExpression.split("[:]", 3);
        if (filterExpParts.length != 3) {
            throw new IllegalArgumentException("filterExpression must be in the form field:operator:value");
        }

        final String fieldName = filterExpParts[0];
        final String filterOperatorString = filterExpParts[1];
        final String filterValue = filterExpParts[2];

        final boolean negated = filterOperatorString.startsWith(NEGATION_SYMBOL);
        final FilterOperator filterOperator = FilterOperator.fromString(negated
                ? filterOperatorString.substring(1)
                : filterOperatorString);

        return new FilterParameter(fieldName, filterOperator, filterValue, negated);
    }

    @Override
    public String toString() {
        return String.format("%s:%s%s:%s",
                fieldName,
                (negated ? "-" : ""),
                filterOperator.getName(),
                filterValue);
    }

    /** Returns a copy of this filter parameter, but with
     *  negated flipped.
     *
     * @return the inverse version of this filter.
     */
    public FilterParameter not() {
        return new FilterParameter(fieldName, filterOperator, filterValue, !negated);
    }
}
