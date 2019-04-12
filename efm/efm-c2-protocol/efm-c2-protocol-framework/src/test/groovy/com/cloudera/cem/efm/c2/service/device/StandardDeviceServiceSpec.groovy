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
package com.cloudera.cem.efm.service.device

import com.cloudera.cem.efm.db.entity.DeviceEntity
import com.cloudera.cem.efm.db.repository.DeviceRepository
import com.cloudera.cem.efm.exception.ResourceNotFoundException
import com.cloudera.cem.efm.mapper.OptionalModelMapper
import com.cloudera.cem.efm.model.Device
import com.cloudera.cem.efm.service.SpecUtil
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import javax.validation.Validation
import javax.validation.Validator

class StandardDeviceServiceSpec extends Specification {

    static OptionalModelMapper modelMapper
    static Validator validator
    DeviceRepository deviceRepository

    DeviceService deviceService

    def setupSpec() {
        validator = Validation.buildDefaultValidatorFactory().getValidator()
        modelMapper = SpecUtil.buildOptionalModelMapper()
    }

    def setup() {
        deviceRepository = Mock(DeviceRepository)

        deviceService = new StandardDeviceService(
                deviceRepository,
                validator,
                modelMapper)
    }

    //*****************************
    //***  Device CRUD methods  ***
    //*****************************

    def "create device"() {

        when: "arg is null"
        deviceService.createDevice(null)

        then: "exception is thrown"
        thrown IllegalArgumentException


        when: "arg is invalid"
        deviceService.createDevice(new Device())

        then: "exception is thrown"
        thrown ConstraintViolationException


        when: "valid device is created"
        deviceRepository.existsById("device1") >> false
        deviceRepository.findById("device1") >> Optional.empty()
        deviceRepository.save(_ as DeviceEntity) >> { DeviceEntity e -> return e }
        def created = deviceService.createDevice(new Device([identifier: "device1"]))

        then: "created device is returned"
        with(created) {
            identifier == "device1"
        }


        when: "device is created with existing id"
        deviceRepository.existsById("device2") >> true
        deviceRepository.findById("device2") >> Optional.of(new DeviceEntity([id: "device2"]))
        deviceService.createDevice(new Device([identifier: "device2"]))

        then: "exception is thrown"
        thrown IllegalStateException

    }

    def "get devices"() {

        setup:
        DeviceEntity dev1 = new DeviceEntity([
                id: "device1",
                name: "Device One"
        ])
        DeviceEntity dev2 = new DeviceEntity([
                id: "device2",
                name: "Device Two"
        ])
        deviceRepository.findAll() >> [dev1, dev2]

        when:
        def devices = deviceService.getDevices()

        then:
        devices.size() == 2
        with(devices.get(0)) {
            identifier == dev1.getId()
            name == dev1.getName()
        }

    }

    def "get device"() {

        when: "device does not exist"
        deviceRepository.findById("device1") >> Optional.empty()
        def device1 = deviceService.getDevice("device1")

        then: "empty optional is returned"
        !device1.isPresent()


        when: "device exists"
        deviceRepository.findById("device2") >> Optional.of(new DeviceEntity([id: "device2"]))
        def device2 = deviceService.getDevice("device2")

        then: "device is returned"
        device2.isPresent()
        with(device2.get()) {
            identifier == "device2"
        }

    }

    def "update device"() {

        when: "device does not exist"
        deviceRepository.findById("device1") >> Optional.empty()
        deviceService.updateDevice(new Device([identifier: "device1", name: "MiNiFi Device"]))

        then:
        thrown ResourceNotFoundException


        when: "device exists"
        deviceRepository.findById("device2") >> Optional.of(new DeviceEntity([id: "device2"]))
        deviceRepository.save(_ as DeviceEntity) >> { DeviceEntity e -> return e }
        def updated = deviceService.updateDevice(new Device([identifier: "device2", name: "MiNiFi Device"]))

        then:
        with(updated) {
            identifier == "device2"
            name == "MiNiFi Device"
        }

    }

    def "delete device"() {

        when: "device does not exist"
        deviceRepository.findById("device1") >> Optional.empty()
        deviceService.deleteDevice("device1")

        then:
        thrown ResourceNotFoundException


        when: "device exists"
        deviceRepository.findById("device2") >> Optional.of(new DeviceEntity([id: "device2"]))
        def deleted = deviceService.deleteDevice("device2")

        then:
        with(deleted) {
            identifier == "device2"
        }

    }

}
