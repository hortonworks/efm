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

import com.cloudera.cem.efm.db.entity.DeviceEntity;
import com.cloudera.cem.efm.model.Device;
import com.cloudera.cem.efm.model.DeviceInfo;
import com.cloudera.cem.efm.model.NetworkInfo;
import com.cloudera.cem.efm.model.SystemInfo;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestDeviceConverter extends ModelMapperTest {

    @Test
    public void testMapDeviceToEntity() {
        // Arrange
        final Device device = new Device();
        arrangeTestDevice(device);

        // Act
        DeviceEntity deviceEntity = modelMapper.map(device, DeviceEntity.class);

        // Assert
        assertCorrectMapping(device, deviceEntity);
    }

    @Test
    public void testMapPartialDeviceToEntity() {
        // Arrange
        final Device device = new Device();
        device.setIdentifier("device-1");
        device.setNetworkInfo(null);
        device.setSystemInfo(null);

        // Act
        DeviceEntity deviceEntity = modelMapper.map(device, DeviceEntity.class);

        // Assert
        assertCorrectMapping(device, deviceEntity);
    }

    @Test
    public void testMapEntityToDevice() {
        // Arrange
        final DeviceEntity deviceEntity = new DeviceEntity();
        arrangeTestDeviceEntity(deviceEntity);

        // Act
        Device device = modelMapper.map(deviceEntity, Device.class);

        // Assert
        assertCorrectMapping(deviceEntity, device);
    }

    @Test
    public void testMapEntityToDeviceInfo() {
        // Arrange
        final DeviceEntity deviceEntity = new DeviceEntity();
        arrangeTestDeviceEntity(deviceEntity);

        // Act
        DeviceInfo deviceInfo = modelMapper.map(deviceEntity, DeviceInfo.class);

        // Assert
        assertCorrectMapping(deviceEntity, deviceInfo);
    }

    @Test
    public void testMapPartialEntityToDevice() {
        // Arrange
        final DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setId("device-1");
        deviceEntity.setName("Device One");

        // Act
        Device device = modelMapper.map(deviceEntity, Device.class);

        // Assert
        assertCorrectMapping(deviceEntity, device);
    }

    private static void arrangeTestDevice(Device device) {
        arrangeTestDeviceInfo(device);
        device.setName("Device One");
        device.setFirstSeen(1L);
        device.setLastSeen(2L);
    }

    private static void arrangeTestDeviceInfo(DeviceInfo deviceInfo) {
        deviceInfo.setIdentifier("device-1");
        deviceInfo.setNetworkInfo(new NetworkInfo());
        deviceInfo.getNetworkInfo().setHostname("host.domain");
        deviceInfo.getNetworkInfo().setIpAddress("1.1.1.1");
        deviceInfo.getNetworkInfo().setDeviceId("00:11:22:33:44:55");
        deviceInfo.setSystemInfo(new SystemInfo());
        deviceInfo.getSystemInfo().setMachineArch("ARM");
        deviceInfo.getSystemInfo().setPhysicalMem(1_000_000_000L);
        deviceInfo.getSystemInfo().setvCores(2);
    }

    private static void arrangeTestDeviceEntity(DeviceEntity deviceEntity) {
        deviceEntity.setId("device-1");
        deviceEntity.setName("Device One");
        deviceEntity.setFirstSeen(new Date(1L));
        deviceEntity.setLastSeen(new Date(2L));
        deviceEntity.setNetworkDeviceId("00:11:22:33:44:55");
        deviceEntity.setHostname("host.domain");
        deviceEntity.setIpAddress("1.1.1.1");
        deviceEntity.setMachineArch("ARM");
        deviceEntity.setPhysicalMem(1_000_000_000L);
        deviceEntity.setvCores(2);
    }

    private static void assertCorrectMapping(Device source, DeviceEntity mapResult) {
        assertCorrectMapping((DeviceInfo)source, mapResult);
        assertEquals(source.getName(), mapResult.getName());
        if (source.getFirstSeen() != null) {
            assertEquals(new Date(source.getFirstSeen()), mapResult.getFirstSeen());
        } else {
            assertNull(mapResult.getFirstSeen());
        }
        if (source.getLastSeen() != null) {
            assertEquals(new Date(source.getLastSeen()), mapResult.getLastSeen());
        } else {
            assertNull(mapResult.getLastSeen());
        }
    }

    private static void assertCorrectMapping(DeviceInfo source, DeviceEntity mapResult) {
        assertNotNull(mapResult);
        assertEquals(source.getIdentifier(), mapResult.getId());
        if (source.getNetworkInfo() != null) {
            assertEquals(source.getNetworkInfo().getHostname(), mapResult.getHostname());
            assertEquals(source.getNetworkInfo().getIpAddress(), mapResult.getIpAddress());
            assertEquals(source.getNetworkInfo().getDeviceId(), mapResult.getNetworkDeviceId());
        } else {
            assertNull(mapResult.getHostname());
            assertNull(mapResult.getIpAddress());
            assertNull(mapResult.getNetworkDeviceId());
        }
        if (source.getSystemInfo() != null) {
            assertEquals(source.getSystemInfo().getMachineArch(), mapResult.getMachineArch());
            assertEquals(source.getSystemInfo().getPhysicalMem(), mapResult.getPhysicalMem());
            assertEquals(source.getSystemInfo().getvCores(), mapResult.getvCores());
        } else {
            assertNull(mapResult.getMachineArch());
            assertNull(mapResult.getPhysicalMem());
            assertNull(mapResult.getvCores());
        }
    }

    private static void assertCorrectMapping(DeviceEntity source, Device mapResult) {
        assertNotNull(mapResult);
        assertCorrectMapping(source, (DeviceInfo)mapResult);
        assertEquals(source.getName(), mapResult.getName());
        if (source.getFirstSeen() != null) {
            assertEquals(Long.valueOf(source.getFirstSeen().getTime()), mapResult.getFirstSeen());
        } else {
            assertNull(mapResult.getFirstSeen());
        }
        if (source.getLastSeen() != null) {
            assertEquals(Long.valueOf(source.getLastSeen().getTime()), mapResult.getLastSeen());
        } else {
            assertNull(mapResult.getLastSeen());
        }

    }

    private static void assertCorrectMapping(DeviceEntity source, DeviceInfo mapResult) {
        assertNotNull(mapResult);
        assertEquals(source.getId(), mapResult.getIdentifier());
        if (source.getNetworkDeviceId() != null
                || source.getHostname() != null
                || source.getIpAddress() != null) {
            assertNotNull(mapResult.getNetworkInfo());
            assertEquals(source.getNetworkDeviceId(), mapResult.getNetworkInfo().getDeviceId());
            assertEquals(source.getIpAddress(), mapResult.getNetworkInfo().getIpAddress());
            assertEquals(source.getHostname(), mapResult.getNetworkInfo().getHostname());
        } else {
            assertNull(mapResult.getNetworkInfo());
        }
        if (source.getMachineArch() != null
                || source.getPhysicalMem() != null
                || source.getvCores() != null) {
            assertNotNull(mapResult.getSystemInfo());
            assertEquals(source.getMachineArch(), mapResult.getSystemInfo().getMachineArch());
            assertEquals(source.getPhysicalMem(), mapResult.getSystemInfo().getPhysicalMem());
            assertEquals(source.getvCores(), mapResult.getSystemInfo().getvCores());
        } else {
            assertNull(mapResult.getSystemInfo());
        }
    }

}
