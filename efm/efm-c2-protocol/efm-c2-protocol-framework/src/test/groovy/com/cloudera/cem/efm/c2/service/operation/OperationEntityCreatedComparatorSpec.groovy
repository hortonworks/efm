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
package com.cloudera.cem.efm.service.operation

import com.cloudera.cem.efm.db.entity.OperationEntity
import spock.lang.Specification

class OperationEntityCreatedComparatorSpec extends Specification {

    OperationEntityCreatedComparator comparator = new OperationEntityCreatedComparator();

    def "compare"(OperationEntity o1, OperationEntity o2, int expectedCompareResult) {

        expect:
        expectedCompareResult == comparator.compare(o1, o2)

        where:
        o1                    | o2                    || expectedCompareResult
        null                  | null                  ||  0
        testEntity()          | null                  || -1
        null                  | testEntity()          ||  1
        testEntity("a", null) | testEntity("a", null) ||  0
        testEntity("b", null) | testEntity("c", null) || -1
        testEntity("c", null) | testEntity("b", null) ||  1
        testEntity("a", null) | testEntity("b", 0L)   || -1
        testEntity("a", 0L)   | testEntity("b", null) || -1
        testEntity("b", null) | testEntity("a", 0L)   ||  1
        testEntity("b", 0L)   | testEntity("a", null) ||  1
        testEntity("a", 1L)   | testEntity("a", 2L)   ||  0
        testEntity("c", 1L)   | testEntity("b", 2L)   || -1
        testEntity("b", 2L)   | testEntity("c", 1L)   ||  1
        testEntity("a", 0L)   | testEntity("a", 0L)   ||  0
        testEntity("b", 0L)   | testEntity("c", 0L)   || -1
        testEntity("c", 0L)   | testEntity("b", 0L)   ||  1

    }

    private static OperationEntity testEntity(String id="", Long timestamp=null) {
        return (timestamp != null
                ? new TestableOperationEntity([id: id, created: new Date(timestamp)])
                : new TestableOperationEntity([id: id]))
    }

}
