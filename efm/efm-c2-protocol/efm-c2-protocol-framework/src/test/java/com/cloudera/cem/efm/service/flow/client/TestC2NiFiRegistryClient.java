/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
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
package com.cloudera.cem.efm.service.flow.client;

import com.cloudera.cem.efm.properties.NifiRegistryProperties;
import org.apache.nifi.registry.bucket.Bucket;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.NiFiRegistryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestC2NiFiRegistryClient {

    private NiFiRegistryClient client;
    private BucketClient bucketClient;

    private static final String REGISTRY_URL = "http://localhost:18080";
    private static final String BUCKET_ID = UUID.randomUUID().toString();
    private static final String BUCKET_NAME = "Test Bucket";

    private Bucket noNameBucket;
    private Bucket nonMatchingBucket;
    private Bucket matchingBucket;

    @Before
    public void setup() {

        bucketClient = mock(BucketClient.class);
        client = mock(NiFiRegistryClient.class);
        when(client.getBucketClient()).thenReturn(bucketClient);

        // Buckets

        noNameBucket = new Bucket();
        noNameBucket.setIdentifier(UUID.randomUUID().toString());

        nonMatchingBucket = new Bucket();
        nonMatchingBucket.setIdentifier(UUID.randomUUID().toString());
        nonMatchingBucket.setName("Not " + BUCKET_NAME);

        matchingBucket = new Bucket();
        matchingBucket.setIdentifier(BUCKET_ID);
        matchingBucket.setName(BUCKET_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorWithNullProperties() {
        createC2NiFiRegistryClient(null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorWithMissingProperties() {
        createC2NiFiRegistryClient("", "");
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorWithBlankProperties() {
        createC2NiFiRegistryClient(" ", "\t");
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorWithBucketIdAndName() {
        createC2NiFiRegistryClient(BUCKET_ID, BUCKET_NAME);
    }

    @Test
    public void testConstructorWithBucketId() throws Exception {
        C2NiFiRegistryClient client = createC2NiFiRegistryClient(BUCKET_ID, null);

        assertFalse(client.getNiFiRegistryBucketName().isPresent());
        assertTrue(client.getNiFiRegistryBucketId().isPresent());
        assertEquals(BUCKET_ID, client.getNiFiRegistryBucketId().get());
    }

    @Test
    public void testConstructorWithBucketName() throws Exception {
        C2NiFiRegistryClient client = createC2NiFiRegistryClient(null, BUCKET_NAME);

        assertFalse(client.getNiFiRegistryBucketId().isPresent());
        assertTrue(client.getNiFiRegistryBucketName().isPresent());
        assertEquals(BUCKET_NAME, client.getNiFiRegistryBucketName().get());
    }

    @Test
    public void testResolveBucketIdWhenIdIsSet() throws Exception {
        C2NiFiRegistryClient client = createC2NiFiRegistryClient(BUCKET_ID, null);

        assertEquals(BUCKET_ID, client.resolveNiFiRegistryBucketId().get());
    }

    @Test
    public void testResolveBucketIdWhenNameIsSet() throws Exception {
        C2NiFiRegistryClient client = createC2NiFiRegistryClient(null, matchingBucket.getName());

        when(bucketClient.getAll()).thenReturn(Arrays.asList(noNameBucket, nonMatchingBucket, matchingBucket));

        assertEquals(BUCKET_ID, client.resolveNiFiRegistryBucketId().get());
    }

    @Test
    public void testResolveBucketIdWhenNameIsSetButNotFound() throws Exception {
        C2NiFiRegistryClient client = createC2NiFiRegistryClient(null, matchingBucket.getName());

        when(bucketClient.getAll()).thenReturn(Arrays.asList(noNameBucket, nonMatchingBucket));

        assertFalse(client.resolveNiFiRegistryBucketId().isPresent());
    }

    private C2NiFiRegistryClient createC2NiFiRegistryClient(String bucketIdValue, String bucketNameValue) {
        final NifiRegistryProperties properties = createRegistryProperties(bucketIdValue, bucketNameValue);
        return new C2NiFiRegistryClient(properties, null, client);

    }

    private NifiRegistryProperties createRegistryProperties(String bucketIdValue, String bucketNameValue) {
        final NifiRegistryProperties properties = new NifiRegistryProperties();
        properties.setUrl(REGISTRY_URL);
        if (bucketIdValue != null) {
            properties.setBucketId(bucketIdValue);
        }
        if (bucketNameValue != null) {
            properties.setBucketName(bucketNameValue);
        }
        return properties;
    }



}
