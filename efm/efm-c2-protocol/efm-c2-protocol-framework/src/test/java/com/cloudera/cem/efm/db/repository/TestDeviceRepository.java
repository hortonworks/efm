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
package com.cloudera.cem.efm.db.repository;

import com.cloudera.cem.efm.db.DatabaseTest;
import com.cloudera.cem.efm.db.entity.DeviceEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestDeviceRepository extends DatabaseTest {

    // See insertData.sql
    private static final long PREPOPULATED_DEVICE_COUNT = 2;

    @Autowired
    private DeviceRepository repository;

    @Test
    public void testCount() {
        // Arrange
        // See insertData.sql

        // Act
        final long actualCount = repository.count();

        // Assert
        assertEquals(PREPOPULATED_DEVICE_COUNT, actualCount);
    }

    @Test
    public void testSave() {
        // Arrange
        Date testStartTime = new Date(System.currentTimeMillis());
        final DeviceEntity entity = new DeviceEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName("My Test Device");
        entity.setFirstSeen(new Date(1L));
        entity.setLastSeen(new Date(2L));
        entity.setMachineArch("ARM");
        entity.setvCores(4);
        entity.setPhysicalMem(1_000_000_000L);
        entity.setHostname("testdev01.testdomain");
        entity.setNetworkDeviceId("00:11:22:33:44:55");
        entity.setIpAddress("10.0.0.1");

        // Act
        final DeviceEntity savedEntity = repository.save(entity);

        // Assert
        assertNotNull(savedEntity.getCreated());
        assertTrue(savedEntity.getCreated().after(testStartTime));
        assertNotNull(savedEntity.getUpdated());
        assertEquals(savedEntity.getCreated(), savedEntity.getUpdated());
        assertEquals(PREPOPULATED_DEVICE_COUNT + 1, repository.count());
    }

    /* GitHub Issue #59 */
    @Test
    public void testSaveLargeValues() {
        // Arrange
        Date testStartTime = new Date(System.currentTimeMillis());
        final DeviceEntity entity = new DeviceEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setvCores(Integer.MAX_VALUE);
        entity.setPhysicalMem(Long.MAX_VALUE);

        // Act
        final DeviceEntity savedEntity = repository.save(entity);

        // Assert
        assertNotNull(savedEntity.getCreated());
        assertTrue(savedEntity.getCreated().getTime() >= testStartTime.getTime());
        assertNotNull(savedEntity.getUpdated());
        assertEquals(savedEntity.getCreated(), savedEntity.getUpdated());
        assertEquals(PREPOPULATED_DEVICE_COUNT + 1, repository.count());
    }

    @Test
    public void testFindById() {
        // Arrange
        // See insertData.sql
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Act "when finding a missing id"
        final Optional<DeviceEntity> missingEntity = repository.findById("nonexistent-id");

        // Assert "empty optional is returned"
        assertFalse(missingEntity.isPresent());

        // Act "when finding a pre-populated id"
        final Optional<DeviceEntity> entityOptional = repository.findById("device-1");

        // Assert "pre-populated entity is returned with all fields correct"
        assertTrue(entityOptional.isPresent());
        DeviceEntity entity = entityOptional.get();
        assertEquals("device-1", entity.getId());
        assertEquals("Device 1", entity.getName());
        assertEquals("x86_64", entity.getMachineArch());
        assertEquals(Long.valueOf(2_000_000_000L), entity.getPhysicalMem());
        assertEquals(Integer.valueOf(4), entity.getvCores());
        assertEquals("00:11:22:33:44:55", entity.getNetworkDeviceId());
        assertEquals("10.0.0.1", entity.getIpAddress());
        assertEquals("device01.domain", entity.getHostname());
        assertEquals("2018-04-11 12:52:00.000 UTC", dateFormat.format(entity.getFirstSeen()));
        assertEquals("2018-04-11 12:53:00.000 UTC", dateFormat.format(entity.getLastSeen()));
        assertEquals("2018-04-11 12:51:00.000 UTC", dateFormat.format(entity.getCreated()));
        assertEquals("2018-04-11 12:54:00.000 UTC", dateFormat.format(entity.getUpdated()));
    }

    @Test
    public void testUpdate() {
        // Arrange
        // See insertData.sql
        final Date testStartTime = new Date(System.currentTimeMillis());
        final Optional<DeviceEntity> result = repository.findById("device-1");
        assertTrue(result.isPresent());
        final DeviceEntity entity = result.get();

        // save the original dates so we can compare them later
        final Date originalCreateDate = entity.getCreated();
        final Date originalUpdateDate = entity.getUpdated();

        final String updatedName = "A Better Device Name";
        assertNotEquals(updatedName, entity.getName());
        entity.setName(updatedName);

        // Act
        // force a commit so we can verify that the preUpdate gets called and sets updated date
        final DeviceEntity updatedEntity = repository.save(entity);
        entityManager.flush();  // so we can verify the effects of preUpdate

        // Assert
        assertEquals(updatedName, updatedEntity.getName());
        assertEquals(originalCreateDate, updatedEntity.getCreated());
        assertNotEquals(originalUpdateDate, updatedEntity.getUpdated());
        assertTrue(updatedEntity.getUpdated().after(testStartTime));
    }

    @Test
    public void testDeleteById() {
        // Arrange
        // See insertData.sql

        // Act
        repository.deleteById("device-1");

        // Assert
        assertFalse(repository.findById("device-1").isPresent());
    }


}
