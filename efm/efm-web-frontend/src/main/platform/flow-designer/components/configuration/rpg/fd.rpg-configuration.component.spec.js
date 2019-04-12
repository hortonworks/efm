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

var RpgConfigurationComponent = require('@flow-designer/components/flow-designer-rpg-configuration');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');
var rxjs = require('rxjs/Observable');
var ngMaterial = require('@angular/material');

describe('Platform Flow Designer RPG Configuration Component', function () {
    var rpgConfigurationComponent;
    var fixture;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngCommonHttp.HttpClientModule
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(RpgConfigurationComponent);

        rpgConfigurationComponent = fixture.componentInstance;
        rpgConfigurationComponent.componentEntity = {
            'revision': {
                'clientId': 'client-id-1',
                'version': 1,
                'lastModifier': 'anonymous'
            },
            'id': '1234',
            'uri': 'http://localhost:8080/nifi-api/remote-process-groups/aebc80c6-0164-1000-5e4b-07fbcc0d96c1',
            'position': {'x':1, 'y':1},
            'permissions': {'canRead': true, 'canWrite': true},
            'component': {
                'id': '1234',
                'parentGroupId': '12345',
                'position': {'x':1, 'y':1},
                'name': 'NiFi Flow',
                'targetUris': 'http://localhost:11080/efm/ui/flow-designer',
                'communicationsTimeout': '30 sec',
                'yieldDuration': '10 sec',
                'transportProtocol': 'RAW',
                'localNetworkInterface': '',
                'proxyHost': '',
                'proxyPort': ''
            }
        };

        // ngOnInit()
        fixture.detectChanges();
    });

    it('should create component', function () {
        //assertions
        expect(rpgConfigurationComponent).toBeDefined();
    });

    it('should open the confirm dialog when cancel is called and the config has changed', function () {
        var matDialog = ngCoreTesting.TestBed.get(ngMaterial.MatDialog);

        spyOn(matDialog, 'open').and.callFake(function () {
            return {
                afterClosed: function () {
                    return rxjs.Observable.of(true);
                },
                componentInstance: {
                    dialogRef: null
                },
                close: function() {}
            }
        });

        rpgConfigurationComponent.unchanged = false;

        rpgConfigurationComponent.cancel();

        expect(matDialog.open).toHaveBeenCalled();
    });

    it('should cancel if no changes have been made', function () {
        var matDialog = ngCoreTesting.TestBed.get(ngMaterial.MatDialog);

        spyOn(matDialog, 'open').and.callFake(function () {
            return {
                afterClosed: function () {
                    return rxjs.Observable.of(true);
                },
                componentInstance: {
                    dialogRef: null
                },
                close: function() {}
            }
        });

        rpgConfigurationComponent.unchanged = true;
        rpgConfigurationComponent.subject$.subscribe(function () {
            fail();
        }, function () {
            // make sure the error path of the subject$ is called
            expect(true).toBeTruthy();
        });

        rpgConfigurationComponent.cancel();

        expect(matDialog.open).not.toHaveBeenCalled();
    });

});
