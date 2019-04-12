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
package com.cloudera.cem.efm.mapper;

/**
 * This is a simple helper that allows one to define a reusable test object along with its expected serialization
 * (that is, a given input that is initialized as part of the setup/arrangement of a unit test case)
 *
 * @param <T> The type of object being wrapped
 * @param <S> The type of the serialization of the object (such as String, byte[])
 */
public class SerializableTestObject<T, S> {

    private T object;
    private S serialization;
    private SerializableTestObjectAssertions<T, S> assertions;

    public SerializableTestObject(T object, S serialization) {
        if (object == null || serialization == null) {
            throw new IllegalArgumentException("All constructor arguments must be not null.");
        }
        this.object = object;
        this.serialization = serialization;
        this.assertions = new SerializableTestObjectAssertions<T, S>() {};  // default assertions
    }

    public SerializableTestObject(T object, S serialization, SerializableTestObjectAssertions<T, S> assertions) {
        if (object == null || serialization == null || assertions == null) {
            throw new IllegalArgumentException("All constructor arguments must be not null.");
        }
        this.object = object;
        this.serialization = serialization;
        this.assertions = assertions;
    }

    public T getObject() {
        return object;
    }

    public S getSerialization() {
        return serialization;
    }

    public void assertObjectEquals(T actualObject) throws Exception {
        assertions.assertObjectEquals(this, actualObject);
    }

    public void assertSerializationEquals(S actualSerialization) throws Exception {
        assertions.assertSerializationEquals(this, actualSerialization);
    }

}
