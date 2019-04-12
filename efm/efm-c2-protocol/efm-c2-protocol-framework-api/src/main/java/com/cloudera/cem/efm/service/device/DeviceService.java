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

import com.cloudera.cem.efm.model.Device;

import java.util.List;
import java.util.Optional;

/**
 *  Standard CRUD method semantics apply to these methods. That is:
 *
 *    - getWidgets: return List of Widget,
 *                  or empty List if no Widgets exist
 *
 *    - getWidget(String): return Optional Widget with matching id,
 *                         or empty Optional if no Widget with matching id exists
 *
 *    - createWidget(Widget): create Widget and assign it a generated id,
 *                            return created widget (including any fields that got generated such as id or creation timestamp),
 *                            throw IllegalStateException if Widget with matching id already exists
 *                            throw IllegalArgumentException if Widget is not valid (e.g., missing required fields)
 *
 *    - updateWidget(Widget): update Widget with the id to match the incoming Widget
 *                            return updated Widget
 *                            throw IllegalArgumentException if Widget is not valid (e.g., missing required fields. Note, id is required when updating existing Widget)
 *                            throw ResourceNotFoundException if no Widget with matching id exists
 *
 *    - deleteWidget(String): delete Widget with id,
 *                            return Widget that was deleted,
 *                            throw ResourceNotFoundException if no Widget with matching id exists
 *
 *  Any invalid arguments (eg, null where required) will result in an IllegalArgumentException
 */
public interface DeviceService {

    //*****************************
    //***  Device CRUD methods  ***
    //*****************************

    Device createDevice(Device device);
    List<Device> getDevices();
    Optional<Device> getDevice(String deviceId);
    Device updateDevice(Device device);
    Device deleteDevice(String deviceId);

}
