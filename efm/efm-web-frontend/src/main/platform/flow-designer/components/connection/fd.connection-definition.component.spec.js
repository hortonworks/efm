/*
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 *  This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 *  Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *  to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 *  properly licensed third party, you do not have any rights to this code.
 *
 *  If this code is provided to you under the terms of the AGPLv3:
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

var ngCommonHttp = require('@angular/common/http');
var ngCoreTesting = require('@angular/core/testing');

var ConnectionDefinitionComponent = require('@flow-designer/components/flow-designer-connection-definition');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');

describe('Platform Flow Designer Connection Definition Component', function () {
    var connectionDefinitionComponent;
    var fixture;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngCommonHttp.HttpClientModule
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(ConnectionDefinitionComponent);

        connectionDefinitionComponent = fixture.componentInstance;
        connectionDefinitionComponent.connectionEntity = {
            'revision': {
                'clientId': '2543b847-0165-1000-c3ff-61d6959a4366',
                'version': 1,
                'lastModifier': 'anonymous'
            },
            'id': '1234',
            'uri': 'http://localhost:8080/nifi-api/connections/2544156a-0165-1000-ece9-5b175677ced9',
            'permissions': {
                'canRead': true,
                'canWrite': true
            },
            'bends': [],
            'labelIndex': 0,
            'zIndex': 0,
            'sourceGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'sourceId': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
            'sourceType': 'processor',
            'destinationGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'destinationId': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
            'destinationType': 'processor',
            'component': {
                'backPressureDataSizeThreshold': '10000 B',
                'backPressureObjectThreshold': 0,
                'bends': [],
                'destination': {
                    'id': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'flowFileExpiration': '60 seconds',
                'id': '16b337ec-b1fa-49e7-b6c7-ee592ef525bf',
                'labelIndex': 1,
                'name': '',
                'parentGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
                'selectedRelationships': ['success'],
                'source': {
                    'id': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'zIndex': 0
            }
        };

        connectionDefinitionComponent.sourceEntity = {
            'revision': {
                'clientId': '2543b847-0165-1000-c3ff-61d6959a4366',
                'version': 1,
                'lastModifier': 'anonymous'
            },
            'id': '1234',
            'uri': 'http://localhost:8080/nifi-api/connections/2544156a-0165-1000-ece9-5b175677ced9',
            'permissions': {
                'canRead': true,
                'canWrite': true
            },
            'bends': [],
            'labelIndex': 0,
            'zIndex': 0,
            'sourceGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'sourceId': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
            'sourceType': 'processor',
            'destinationGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'destinationId': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
            'destinationType': 'processor',
            'component': {
                'backPressureDataSizeThreshold': '10000 B',
                'backPressureObjectThreshold': 0,
                'bends': [],
                'destination': {
                    'id': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'flowFileExpiration': '60 seconds',
                'id': '16b337ec-b1fa-49e7-b6c7-ee592ef525bf',
                'labelIndex': 1,
                'name': '',
                'parentGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
                'selectedRelationships': ['success'],
                'source': {
                    'id': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'zIndex': 0
            }
        };

        connectionDefinitionComponent.destinationEntity = {
            'revision': {
                'clientId': '2543b847-0165-1000-c3ff-61d6959a4366',
                'version': 1,
                'lastModifier': 'anonymous'
            },
            'id': '1234',
            'uri': 'http://localhost:8080/nifi-api/connections/2544156a-0165-1000-ece9-5b175677ced9',
            'permissions': {
                'canRead': true,
                'canWrite': true
            },
            'bends': [],
            'labelIndex': 0,
            'zIndex': 0,
            'sourceGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'sourceId': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
            'sourceType': 'processor',
            'destinationGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
            'destinationId': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
            'destinationType': 'processor',
            'component': {
                'backPressureDataSizeThreshold': '10000 B',
                'backPressureObjectThreshold': 0,
                'bends': [],
                'destination': {
                    'id': 'ba5587dd-192c-47d1-b9e6-4a16f32dca63',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'flowFileExpiration': '60 seconds',
                'id': '16b337ec-b1fa-49e7-b6c7-ee592ef525bf',
                'labelIndex': 1,
                'name': '',
                'parentGroupId': '11026c1b-72d0-4d54-81a4-6e2032672f52',
                'selectedRelationships': ['success'],
                'source': {
                    'id': '296dd89c-bba2-4da3-9b2c-38ab5401d7dd',
                    'type': 'processor',
                    'groupId': '11026c1b-72d0-4d54-81a4-6e2032672f52'
                },
                'zIndex': 0
            }
        };

        // ngOnInit()
        fixture.detectChanges();
    });

    it('should create component', function () {
        //assertions
        expect(connectionDefinitionComponent).toBeDefined();
    });
});
