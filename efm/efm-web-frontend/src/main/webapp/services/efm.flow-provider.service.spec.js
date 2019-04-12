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

var rxjs = require('rxjs/Observable');
var ngCommonHttp = require('@angular/common/http');
var ngCoreTesting = require('@angular/core/testing');

var FdsCoreModule = require('@flow-design-system/core');
var ErrorResponseFactory = require('@flow-designer/services/ErrorResponseFactory');

var EfmApi = require('services/efm.api.service.js');
var DialogService = require('services/efm.dialog.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');

describe('EFM Flow Provider Service', function () {
    var flowProvider;
    var efmApi;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                ngCommonHttp.HttpClientModule
            ],
            providers: [
                FlowProvider,
                EfmApi,
                DialogService,
                {
                    provide: 'ErrorResponse',
                    useFactory: ErrorResponseFactory.build,
                    deps: []
                }
            ]
        });

        // get the efmService and the dialogService from the root injector
        flowProvider = ngCoreTesting.TestBed.get(FlowProvider);
        efmApi = ngCoreTesting.TestBed.get(EfmApi);
    });

    it('should create service', function () {
        //assertions
        expect(flowProvider).toBeDefined();
    });

    it('should fail to get', function () {
        flowProvider.get().subscribe(function () {
        }, function (errorResponse) {
            //assertions
            expect(errorResponse.message).toBe('No flow id set.');

        });
    });

    it('should get', function () {
        // Spy
        spyOn(efmApi, 'getFlow').and.callFake(function () {
            return rxjs.Observable.of({
                identifier: 'testFlow'
            });
        });

        flowProvider.setFlowId('test');
        flowProvider.get().subscribe(function (flow) {
            //assertions
            expect(flow.identifier).toBe('testFlow');

        });
    });

    it('should fail to getFlow', function () {
        // Spy
        spyOn(efmApi, 'getFlow').and.callFake(function () {
            return rxjs.Observable.throw({
                preventDefault: false,
                message: 'test message'
            });
        });

        flowProvider.setFlowId('test');
        flowProvider.get().subscribe(function () {
        }, function (errorResponse) {
            //assertions
            expect(errorResponse.message).toBe('test message');

        });
    });

    it('should fail to get El specification', function () {
        flowProvider.getElSpecification().subscribe(function () {
        }, function (errorResponse) {
            //assertions
            expect(errorResponse.message).toBe('No flow id set.');

        });
    });

    it('should get El specification', function () {
        // Spy
        spyOn(efmApi, 'getElSpecification').and.callFake(function () {
            return rxjs.Observable.of({
                identifier: 'test'
            });
        });

        flowProvider.setFlowId('test');
        flowProvider.getElSpecification().subscribe(function (elSpecification) {
            //assertions
            expect(elSpecification.identifier).toBe('test');

        });
    });

    it('should fail to getElSpecification', function () {
        // Spy
        spyOn(efmApi, 'getElSpecification').and.callFake(function () {
            return rxjs.Observable.throw({
                preventDefault: false,
                message: 'test message'
            });
        });

        flowProvider.setFlowId('test');
        flowProvider.getElSpecification().subscribe(function () {
        }, function (errorResponse) {
            //assertions
            expect(errorResponse.message).toBe('test message');
        });
    });
});
