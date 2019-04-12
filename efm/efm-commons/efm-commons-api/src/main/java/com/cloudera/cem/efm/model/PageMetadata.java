/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This is a derived work from the Spring Hateoas project:
 * https://github.com/spring-projects/spring-hateoas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PageMetadata {

    private long size;
    private long number;
    private long totalElements;
    private long totalPages;

    protected PageMetadata() {
    }

    /**
     * Creates a new {@link PageMetadata} from the given size, number, total elements and total pages.
     *
     * @param size the size of the page
     * @param number zero-indexed, must be less than totalPages
     * @param totalElements the total number of elements available
     * @param totalPages  the total number of pages available
     */
    public PageMetadata(long size, long number, long totalElements, long totalPages) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must not be negative");
        }
        if (number < 0) {
            throw new IllegalArgumentException("Number must not be negative");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must not be negative");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages must not be negative");
        }

        this.size = size;
        this.number = number;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * Creates a new {@link PageMetadata} from the given size, number and total elements.
     *
     * @param size the size of the page
     * @param number the zero-indexed number of the page
     * @param totalElements the total number of elements available
     */
    public PageMetadata(long size, long number, long totalElements) {
        this(size, number, totalElements, size == 0 ? 0 : (long) Math.ceil((double) totalElements / (double) size));
    }

    @ApiModelProperty
    public long getSize() {
        return size;
    }

    @ApiModelProperty
    public long getTotalElements() {
        return totalElements;
    }

    @ApiModelProperty
    public long getTotalPages() {
        return totalPages;
    }

    @ApiModelProperty
    public long getNumber() {
        return number;
    }

}
