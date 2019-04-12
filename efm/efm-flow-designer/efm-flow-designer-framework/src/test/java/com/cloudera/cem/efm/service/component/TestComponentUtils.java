/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 * LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 * FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 * TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 * UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.component;

import org.apache.nifi.registry.flow.VersionedControllerService;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

public class TestComponentUtils {

    private VersionedProcessGroup rootGroup;
    private VersionedControllerService rootService;

    private VersionedProcessGroup groupLevel1Child1;
    private VersionedControllerService serviceLevel1Child1;

    private VersionedProcessGroup groupLevel1Child2;
    private VersionedControllerService serviceLevel1Child2;

    private VersionedProcessGroup groupLevel2Child1;
    private VersionedControllerService serviceLevel2Child1;

    private VersionedProcessGroup groupLevel2Child2;
    private VersionedControllerService serviceLevel2Child2;

    private VersionedProcessGroup groupLevel2Child3;
    private VersionedControllerService serviceLevel2Child3;

    private VersionedProcessGroup groupLevel2Child4;
    private VersionedControllerService serviceLevel2Child4;


    @Before
    public void setup() {
        // Setup root group...
        rootGroup = new VersionedProcessGroup();
        rootGroup.setIdentifier(UUID.randomUUID().toString());
        rootGroup.setName("root");

        rootService = new VersionedControllerService();
        rootService.setIdentifier(UUID.randomUUID().toString());
        rootGroup.getControllerServices().add(rootService);

        // Setup level 1....
        groupLevel1Child1 = new VersionedProcessGroup();
        groupLevel1Child1.setIdentifier(UUID.randomUUID().toString());
        groupLevel1Child1.setName("Level 1 Child 1");

        serviceLevel1Child1 = new VersionedControllerService();
        serviceLevel1Child1.setIdentifier(UUID.randomUUID().toString());
        groupLevel1Child1.getControllerServices().add(serviceLevel1Child1);

        groupLevel1Child2 = new VersionedProcessGroup();
        groupLevel1Child2.setIdentifier(UUID.randomUUID().toString());
        groupLevel1Child2.setName("Level 1 Child 2");

        serviceLevel1Child2 = new VersionedControllerService();
        serviceLevel1Child2.setIdentifier(UUID.randomUUID().toString());
        groupLevel1Child2.getControllerServices().add(serviceLevel1Child2);

        // Setup level 2...
        groupLevel2Child1 = new VersionedProcessGroup();
        groupLevel2Child1.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child1.setName("Level 2 Child 1");

        serviceLevel2Child1 = new VersionedControllerService();
        serviceLevel2Child1.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child1.getControllerServices().add(serviceLevel2Child1);

        groupLevel2Child2 = new VersionedProcessGroup();
        groupLevel2Child2.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child2.setName("Level 2 Child 2");

        serviceLevel2Child2 = new VersionedControllerService();
        serviceLevel2Child2.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child2.getControllerServices().add(serviceLevel2Child2);

        groupLevel2Child3 = new VersionedProcessGroup();
        groupLevel2Child3.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child3.setName("Level 2 Child 3");

        serviceLevel2Child3 = new VersionedControllerService();
        serviceLevel2Child3.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child3.getControllerServices().add(serviceLevel2Child3);

        groupLevel2Child4 = new VersionedProcessGroup();
        groupLevel2Child4.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child4.setName("Level 2 Child 4");

        serviceLevel2Child4 = new VersionedControllerService();
        serviceLevel2Child4.setIdentifier(UUID.randomUUID().toString());
        groupLevel2Child4.getControllerServices().add(serviceLevel2Child4);

        // Connect the process groups...

        rootGroup.getProcessGroups().add(groupLevel1Child1);
        rootGroup.getProcessGroups().add(groupLevel1Child2);

        groupLevel1Child1.getProcessGroups().add(groupLevel2Child1);
        groupLevel1Child1.getProcessGroups().add(groupLevel2Child2);

        groupLevel1Child2.getProcessGroups().add(groupLevel2Child3);
        groupLevel1Child2.getProcessGroups().add(groupLevel2Child4);
    }

    @Test
    public void testGetAncestorControllerServicesFromLeafBackToRoot() {
        final Set<VersionedControllerService> services = ComponentUtils.getAncestorControllerServices(groupLevel2Child4.getIdentifier(), rootGroup);
        Assert.assertEquals(3, services.size());
        Assert.assertTrue(services.contains(rootService));
        Assert.assertTrue(services.contains(serviceLevel1Child2));
        Assert.assertTrue(services.contains(serviceLevel2Child4));
    }

    @Test
    public void testGetAncestorControllerServicesFromRootToRoot() {
        final Set<VersionedControllerService> services = ComponentUtils.getAncestorControllerServices(rootGroup.getIdentifier(), rootGroup);
        Assert.assertEquals(1, services.size());
        Assert.assertTrue(services.contains(rootService));
    }

    @Test
    public void testGetAncestorControllerServicesFromLeafToLeaf() {
        final Set<VersionedControllerService> services = ComponentUtils.getAncestorControllerServices(groupLevel2Child4.getIdentifier(), groupLevel2Child4);
        Assert.assertEquals(1, services.size());
        Assert.assertTrue(services.contains(serviceLevel2Child4));
    }

    @Test
    public void testValidateDataSize() {
        final String input1 = "10 MB";
        final String parsed1 = ComponentUtils.validateDataUnitValue(input1);
        Assert.assertEquals(input1, parsed1);

        final String input2 = "10 mb";
        final String parsed2 = ComponentUtils.validateDataUnitValue(input2);
        Assert.assertEquals("10 MB", parsed2);

        final String input3 = "10 MB abc";
        final String parsed3 = ComponentUtils.validateDataUnitValue(input3);
        Assert.assertEquals("10 MB", parsed3);

        final String input4 = "INVALID";
        try {
            ComponentUtils.validateDataUnitValue(input4);
            Assert.fail("Should have thrown exception");
        } catch (Exception e) {

        }

        final String input5 = "10.0 MB";
        final String parsed5 = ComponentUtils.validateDataUnitValue(input5);
        Assert.assertEquals(input5, parsed5);
    }

}
