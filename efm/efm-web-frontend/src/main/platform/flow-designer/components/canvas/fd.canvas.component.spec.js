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

var rxjs = $.extend({}, require('rxjs/Subject'), require('rxjs/Observable'));
var ngCommonHttp = require('@angular/common/http');
var ngCoreTesting = require('@angular/core/testing');

var CanvasComponent = require('@flow-designer/components/flow-designer-canvas');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');
var flowDesignerApiFactory = require('@flow-designer/services/FlowDesignerApiFactory');

describe('Platform Flow Designer Canvas Component', function () {
    var canvasComponent;
    var fixture;
    var canvasUtilsService;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngCommonHttp.HttpClientModule
            ],
            providers: [
                CanvasUtilsService,
                {
                    provide: 'FlowDesignerApi',
                    useFactory: flowDesignerApiFactory.build,
                    deps: []
                }
            ]
        });

        canvasUtilsService = ngCoreTesting.TestBed.get(CanvasUtilsService);

        fixture = ngCoreTesting.TestBed.createComponent(CanvasComponent);

        canvasComponent = fixture.componentInstance;

        // ngOnInit()
        fixture.detectChanges();

        // Spy
        spyOn(canvasUtilsService, 'ellipsis').and.callFake(function () {});
    });

    it('should create component', function () {
        //assertions
        expect(canvasComponent).toBeDefined();
    });

    it('should create processor', function () {
        var processorCreation$ = new rxjs.Subject();

        var handleProcessorCreation = function () {
            setTimeout(function () {
                processorCreation$.next({
                    id: '1111'
                });
            }, 0);
            return processorCreation$;
        };

        canvasComponent.onProcessorCreation(handleProcessorCreation);

        canvasComponent.create('processor', {x: 1, y: 1}).then(function (componentEntity) {
            //assertions
            expect(componentEntity.type).toBe('processor');

        });

    });

    it('should create funnel', function () {
        canvasComponent.create('funnel', {x: 1, y: 1}).then(function (componentEntity) {
            //assertions
            expect(componentEntity.type).toBe('funnel');
        });

    });

    it('should create remote-process-group', function () {
        var rpgCreation$ = new rxjs.Subject();

        var handleRpgCreation = function () {
            setTimeout(function () {
                rpgCreation$.next('http://localhost:11080/dfo/ui/flow-designer');
            }, 0);
            return rpgCreation$;
        };

        canvasComponent.onRemoteProcessGroupCreation(handleRpgCreation);

        canvasComponent.create('remote-process-group', {x: 1, y: 1}).then(function (componentEntity) {
            //assertions
            expect(componentEntity.type).toBe('remote-process-group');
        });

    });
});
