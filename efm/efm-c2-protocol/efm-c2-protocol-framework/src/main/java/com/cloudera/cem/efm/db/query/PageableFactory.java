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

import com.cloudera.cem.efm.model.QueryParameters;
import com.cloudera.cem.efm.model.SortOrder;
import com.cloudera.cem.efm.model.SortParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PageableFactory {

    private final int defaultPageNumber;
    private final int defaultPageSize;
    private final Supplier<Sort> defaultSortSupplier;

    private PageableFactory(Builder builder) {

        if (builder.getDefaultPageNumber() < 0) {
            throw new IllegalArgumentException("Default page number must not be negative.");
        }
        if (builder.getDefaultPageSize() < 1) {
            throw new IllegalArgumentException("Default page size must be greater than 0.");
        }
        if (builder.getDefaultSortSupplier() == null) {
            throw new IllegalArgumentException("Default sort supplier cannot be null");
        }

        defaultPageNumber = builder.getDefaultPageNumber();
        defaultPageSize = builder.getDefaultPageSize();
        defaultSortSupplier = builder.getDefaultSortSupplier();
    }

    public static PageableFactory defaultFactory() {
        return new PageableFactory(new Builder());
    }

    public static PageableFactory.Builder configure() {
        return new PageableFactory.Builder();
    }

    /**
     * Create the default pageable.
     *
     * @return a not null Pageable instance that represents the default paging.
     */
    public Pageable defaultPageRequest() {
        return PageRequest.of(defaultPageNumber, defaultPageSize, defaultSortSupplier.get());
    }

    /**
     * Create a {@code Pageable} from a given {@link QueryParameters} instance.
     *
     * @param queryParameters the query parameters to follow. Can be null, empty, partial, or fully specified.
     * @return a corresponding pageable, or a default pageable if query parameters is null or empty.
     */
    public Pageable pageableFromQueryParameters(QueryParameters queryParameters) {

        // Check for null
        if (queryParameters == null) {
            return defaultPageRequest();
        }

        // Determine page number and page size
        final int pageNumber = queryParameters.getPageNum() != null ? queryParameters.getPageNum() : defaultPageNumber;
        final int pageSize = queryParameters.getNumRows() != null ? queryParameters.getNumRows() : defaultPageSize;

        // Determine sorts
        Sort sort;
        if (queryParameters.getSortParameters() != null && !queryParameters.getSortParameters().isEmpty()) {
            final List<Sort.Order> orders = new ArrayList<>();
            for(SortParameter sortParameter : queryParameters.getSortParameters()) {
                orders.add(sortOrderFromSortParameter(sortParameter));
            }
            sort = Sort.by(orders);
        } else {
            sort = defaultSortSupplier.get();
        }

        // build Pageable
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    /**
     * Create a {@code Sort.Order} from a given {@link SortParameter} instance.
     *
     * @param sortParameter the sort parameter to follow. must be not null and valid
     * @return a corresponsing Sort.Order
     * @throws IllegalArgumentException if the input sortParameter is null or invalid
     */
    public Sort.Order sortOrderFromSortParameter(SortParameter sortParameter) {
        if (sortParameter == null) {
            throw new IllegalArgumentException("sort parameter cannot be null");
        }
        if (sortParameter.getFieldName() == null) {
            throw new IllegalArgumentException("sort parameter field name cannot be null");
        }

        final Sort.Direction direction;
        if (sortParameter.getOrder() != null) {
            direction = SortOrder.ASC.equals(sortParameter.getOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        } else {
            direction = Sort.Direction.ASC;
        }

        return new Sort.Order(direction, sortParameter.getFieldName(), Sort.NullHandling.NULLS_LAST);
    }

    public static class Builder {

        private int defaultPageNumber = 0;
        private int defaultPageSize = 10;
        private Supplier<Sort> defaultSortSupplier = Sort::unsorted;

        public Builder defaultPageNumber(int defaultPageNumber) {
            this.defaultPageNumber = defaultPageNumber;
            return this;
        }

        public Builder defaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
            return this;
        }

        public Builder defaultSortSupplier(Supplier<Sort> defaultSortSupplier) {
            if (defaultSortSupplier != null) {
                this.defaultSortSupplier = defaultSortSupplier;
            }
            return this;
        }

        public int getDefaultPageNumber() {
            return defaultPageNumber;
        }

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public Supplier<Sort> getDefaultSortSupplier() {
            return defaultSortSupplier;
        }

        public PageableFactory build() {
            return new PageableFactory(this);
        }

    }

}
