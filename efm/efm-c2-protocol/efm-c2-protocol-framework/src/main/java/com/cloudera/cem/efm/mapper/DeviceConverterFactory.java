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
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DeviceConverterFactory {

    @Bean
    public static PropertyMap<Device, DeviceEntity> deviceToEntityMap() {
        return new PropertyMap<Device, DeviceEntity>() {
            @Override
            protected void configure() {
                map().setId(source.getIdentifier());

                map().setMachineArch(source.getSystemInfo().getMachineArch());
                map().setPhysicalMem(source.getSystemInfo().getPhysicalMem());
                map().setvCores(source.getSystemInfo().getvCores());

                map().setNetworkDeviceId(source.getNetworkInfo().getDeviceId());
                map().setIpAddress(source.getNetworkInfo().getIpAddress());
                map().setHostname(source.getNetworkInfo().getHostname());

                // name, firstSeen, lastSeen are mapped implicitly
            }
        };
    }

    @Bean
    public static Converter<DeviceEntity, DeviceInfo> entityToDeviceInfoConverter() {
        return new AbstractConverter<DeviceEntity, DeviceInfo>() {
            @Override
            protected DeviceInfo convert(DeviceEntity source) {
                final DeviceInfo deviceInfo = new DeviceInfo();
                mapEntityToDeviceInfo(source, deviceInfo);
                return deviceInfo;
            }
        };
    }

    @Bean
    public static Converter<DeviceEntity, Device> entityToDeviceConverter() {
        return new AbstractConverter<DeviceEntity, Device>() {
            @Override
            protected Device convert(DeviceEntity source) {
                final Device device = new Device();
                mapEntityToDevice(source, device);
                return device;
            }
        };
    }

    private static void mapEntityToDevice(DeviceEntity source, Device destination) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source and destination inputs must both be not null");
        }

        mapEntityToDeviceInfo(source, (DeviceInfo) destination);

        destination.setName(source.getName());
        destination.setFirstSeen(source.getFirstSeen() != null ? source.getFirstSeen().getTime() : null);
        destination.setLastSeen(source.getLastSeen() != null ? source.getLastSeen().getTime() : null);
    }

    private static void mapEntityToDeviceInfo(DeviceEntity source, DeviceInfo destination) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source and destination inputs must both be not null");
        }

        destination.setIdentifier(source.getId());

        if (source.getMachineArch() != null
                || source.getPhysicalMem() != null
                || source.getvCores() != null) {
            final SystemInfo destSystemInfo = new SystemInfo();
            destSystemInfo.setMachineArch(source.getMachineArch());
            destSystemInfo.setPhysicalMem(source.getPhysicalMem());
            destSystemInfo.setvCores(source.getvCores());
            destination.setSystemInfo(destSystemInfo);
        }

        if (source.getNetworkDeviceId() != null
                || source.getHostname() != null
                || source.getIpAddress() != null) {
            final NetworkInfo destNetworkInfo = new NetworkInfo();
            destNetworkInfo.setDeviceId(source.getNetworkDeviceId());
            destNetworkInfo.setHostname(source.getHostname());
            destNetworkInfo.setIpAddress(source.getIpAddress());
            destination.setNetworkInfo(destNetworkInfo);
        }
    }

    /**
     * Returns all converters this factory is capable of building.
     * Mainly used for test suites.
     */
    public static List<Converter<?,?>> allConverters() {
        List<Converter<?,?>> converters = Arrays.asList(
                DeviceConverterFactory.entityToDeviceInfoConverter(),
                DeviceConverterFactory.entityToDeviceConverter()
        );
        return converters;
    }

    /**
     * Returns all property maps this factory is capable of building.
     * Mainly used for test suites.
     */
    public static List<PropertyMap<?,?>> allMaps() {
        List<PropertyMap<?,?>> maps = Arrays.asList(
                DeviceConverterFactory.deviceToEntityMap()
        );
        return maps;
    }

}
