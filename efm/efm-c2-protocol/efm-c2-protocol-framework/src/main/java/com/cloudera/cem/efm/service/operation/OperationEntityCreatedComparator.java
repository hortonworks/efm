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
package com.cloudera.cem.efm.service.operation;

import com.cloudera.cem.efm.db.entity.OperationEntity;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * A comparator for ordering {@code OperationEntity} objects based on their
 * {@code created} timestamps.
 *
 * <p>This comparator's ordering is consistent with equals. It guarantees that if the two
 * OperationEntity inputs are equal based on the {@code OperationEntity.equals(...)}
 * method, then {@code compare(...)} will return 0.
 *
 * <p>This means this comparator is safe to use in ordered Sets/Maps that use a custom
 * comparator to determine equivalence of members/keys in addition to ordering.
 * Specifically, this comparator has been tested when used to create a custom
 * {@code com.google.common.graph.ElementOrder<OperationEntity>} for an instance of
 * {@code com.google.common.graph.Graph<OperationEntity>}.
 *
 * <p>In order to provide this guarantee, this comparator falls back to ordering based
 * on OperationEntity id in the case that the created timestamps are equal. Conversely,
 * this comparator will always return 0 if the two inputs are determined to be equal
 * according to their equals(...) method, regardless of the values of their created
 * timestamps.
 */
public class OperationEntityCreatedComparator implements Comparator<OperationEntity>, Serializable {

    private static final long serialVersionUID = -8171556080130322645L;

    @Override
    public int compare(OperationEntity o1, OperationEntity o2) {

        // Comparator must return zero if o1.equals(o2) to maintain consistency with equals
        if (Objects.equals(o1, o2)) {
            return 0;
        }

        // Note that after this point, this comparator cannot return zero because the Objects.equals(...)
        // condition above did not return zero, and it must maintain consistency with equals.
        // That is:
        //
        //   (OperationEntityCreatedComparator.compare(o1,o2) == 0) == Object.equals(o1,o2)) for all o1,o2
        //
        // This consistency allows this comparator to be safely used in Map and Set implementations that
        // use a custom comparator result in place of equals, such as TreeMap (which is used internally
        // in Guava's Graph implementation when nodes are ordered.)

        // Check if one of the two input objects is null
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }

        // If both input objects have a created timestamp, and those timestamps are different, use that
        if (o1.getCreated() != null && o2.getCreated() != null) {
            final int createdComparisonResult = Long.compare(o1.getCreated().getTime(), o2.getCreated().getTime());
            if (createdComparisonResult != 0) {
                // See documentation about as to why this comparator cannot return 0 at this point.
                return createdComparisonResult;
            }
        }

        // Lastly, fall back to using the natural ordering of the id
        if (o1.getId() != null && o2.getId() != null) {
            final int idComparisonResult = o1.getId().compareTo(o2.getId());
            if (idComparisonResult != 0) {
                // See documentation about as to why this comparator cannot return 0 at this point.
                return idComparisonResult;
            }
        }

        // Unlikely to reach here unless one of the two inputs is missing an id
        // or the implementation of BaseEntity.equals(...) has changed.
        throw new RuntimeException("Operation inputs could not be compared. Did both of them have ids set?");
    }
}
