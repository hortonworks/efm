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
import com.cloudera.cem.efm.model.Event;
import com.cloudera.cem.efm.model.EventSeverity;
import com.cloudera.cem.efm.model.FilterOperator;
import com.cloudera.cem.efm.model.FilterParameter;
import com.cloudera.cem.efm.model.QueryParameters;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class EventSpecifications {

    public static Specification<EventEntity> fromQueryParameters(final QueryParameters queryParameters) {
        if (queryParameters == null || queryParameters.getFilterParameters() == null) {
            return null;
        }
        return fromFilterParameters(queryParameters.getFilterParameters());
    }

    public static Specification<EventEntity> fromFilterParameters(final Collection<FilterParameter> filterParameters) {
        if (filterParameters == null || filterParameters.isEmpty()) {
            return null;
        }

        return new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

                List<Predicate> predicates = new ArrayList<>();

                for (FilterParameter filter : filterParameters) {
                    final String field = filter.getFieldName();
                    Specification<EventEntity> filterSpec = fromFilterParameter(filter);
                    if (filterSpec != null) {
                        predicates.add(filterSpec.toPredicate(root, query, builder));
                    }
                }

                return predicates.isEmpty() ? null : builder.and(predicates.toArray(new Predicate[] {}));
            }
        };
    }

    public static Specification<EventEntity> fromFilterParameter(FilterParameter filterParameter) {
        if (filterParameter == null) {
            return null;
        }

        return new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                return buildWherePredicate(filterParameter, root, query, builder);
            }
        };
    }

    private static Predicate buildWherePredicate(
            FilterParameter filterParameter,
            final Root<EventEntity> root,
            final CriteriaQuery<?> query,
            final CriteriaBuilder builder) {

        final String field = filterParameter.getFieldName();
        if (!Event.Field.allFields().contains(field)) {
            throw new IllegalArgumentException("Filtering for field '" + field + "' not supported.");
        }

        final FilterOperator operator = filterParameter.getFilterOperator();
        final String filterValue = filterParameter.getFilterValue();

        final Object typedFilterValue;
        switch (field) {

            case Event.Field.SEVERITY_LEVEL:
                if (FilterOperator.LIKE.equals(filterParameter.getFilterOperator())) {
                    // just doing String value matching, so regular logic applies ...
                    typedFilterValue = filterValue;
                    break;
                } else {
                    // doing directional matching on enum ordinal value...
                    try {
                        return Specifications.buildEnumWherePredicate(
                                field,
                                operator,
                                filterValue,
                                EventSeverity.class,
                                filterParameter.isNegated(),
                                root,
                                query,
                                builder);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid query filter: '" + filterParameter + "'.", e);
                    }
                }

            case Event.Field.CREATED:
                try {
                    typedFilterValue = new Date(Long.valueOf(filterValue));
                } catch (NumberFormatException e) {
                    final String errMessage =  "Filter for '" + field +
                            "' accepts milliseconds since epoch timestamp value (a long).";
                    throw new IllegalArgumentException(errMessage, e);
                }
                break;

            default:
                // Invalid field strings already rejected above, so we know it is a valid field
                // The default typed value is a string, which the input is already, so nothing to do in this case
                typedFilterValue = filterValue;
                break;
        }

        return Specifications.buildWherePredicate(
                field,
                operator,
                typedFilterValue,
                filterParameter.isNegated(),
                root,
                query,
                builder);
    }

}
