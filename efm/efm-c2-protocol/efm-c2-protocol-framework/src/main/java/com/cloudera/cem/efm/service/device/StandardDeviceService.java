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
package com.cloudera.cem.efm.service.device;

import com.cloudera.cem.efm.db.entity.DeviceEntity;
import com.cloudera.cem.efm.db.repository.DeviceRepository;
import com.cloudera.cem.efm.exception.ResourceNotFoundException;
import com.cloudera.cem.efm.mapper.OptionalModelMapper;
import com.cloudera.cem.efm.model.Device;
import com.cloudera.cem.efm.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class StandardDeviceService extends BaseService implements DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(StandardDeviceService.class);

    private final DeviceRepository deviceRepository;

    @Autowired
    public StandardDeviceService(
            final DeviceRepository deviceRepository,
            final Validator validator,
            final OptionalModelMapper modelMapper) {
        super(validator, modelMapper);
        this.deviceRepository = deviceRepository;
    }

    //*****************************
    //***  Device CRUD methods  ***
    //*****************************

    @Override
    public Device createDevice(Device device) {
        validate(device, "Cannot create device");
        if (deviceRepository.existsById(device.getIdentifier())) {
            throw new IllegalStateException(String.format("Device already exists with id='%s", device.getIdentifier()));
        }
        final DeviceEntity entity = modelMapper.map(device, DeviceEntity.class);
        final DeviceEntity savedEntity = deviceRepository.save(entity);
        final Device savedDevice = modelMapper.map(savedEntity, Device.class);
        return savedDevice;

    }

    @Override
    public List<Device> getDevices() {
        final List<Device> devices = new ArrayList<>();
        deviceRepository.findAll().forEach(entity -> {
            final Device device = modelMapper.map(entity, Device.class);
            devices.add(device);
        });
        return devices;
    }

    @Override
    public Optional<Device> getDevice(String deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }
        final Optional<DeviceEntity> entityOptional = deviceRepository.findById(deviceId);
        final Optional<Device> deviceOptional = modelMapper.mapOptional(entityOptional, Device.class);
        return deviceOptional;
    }

    @Override
    public Device updateDevice(Device device) {
        validate(device, "Cannot update device");

        final Optional<DeviceEntity> entityOptional = deviceRepository.findById(device.getIdentifier());
        final DeviceEntity entity = entityOptional.orElseThrow(() -> new ResourceNotFoundException("Device not found with id " + device.getIdentifier()));
        final Date existingFirstSeenDate = entity.getFirstSeen();
        modelMapper.map(device, entity);
        if (existingFirstSeenDate != null) {
            entity.setFirstSeen(existingFirstSeenDate);  // firstSeen timestamp, once set, is immutable
        }
        final DeviceEntity updatedEntity = deviceRepository.save(entity);
        final Device updatedDevice = modelMapper.map(updatedEntity, Device.class);
        return updatedDevice;
    }

    @Override
    public Device deleteDevice(String deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }

        final Optional<DeviceEntity> entityOptional = deviceRepository.findById(deviceId);
        if (!entityOptional.isPresent()) {
            throw new ResourceNotFoundException("Device not found with id " + deviceId);
        }
        deviceRepository.deleteById(deviceId);

        final Device deletedDevice = modelMapper.map(entityOptional.get(), Device.class);
        return deletedDevice;
    }

}
