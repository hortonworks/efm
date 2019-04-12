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

var ngCoreTesting = require('@angular/core/testing');
var ngHttpTesting = require('@angular/common/http/testing');
var ngCommonHttp = require('@angular/common/http');

var EfmApi = require('services/efm.api.service.js');
var DialogService = require('services/efm.dialog.service.js');
var EfmErrorService = require('services/efm.error.service.js');
var EfmHttpErrorResponseFactory = require('services/efm.http-error-response.factory.js');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');

var URLS = require('config/urls.js');

describe('EFM API Service', function () {
    var httpMock;
    var dialogService;
    var efmApi;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngHttpTesting.HttpClientTestingModule
            ],
            providers: [
                EfmApi,
                EfmErrorService,
                DialogService,
                {
                    provide: 'EfmHttpErrorResponse',
                    useFactory: EfmHttpErrorResponseFactory.build,
                    deps: [
                        EfmErrorService
                    ]
                }
            ]
        });

        httpMock = ngCoreTesting.TestBed.get(ngHttpTesting.HttpTestingController);
        dialogService = ngCoreTesting.TestBed.get(DialogService);
        efmApi = ngCoreTesting.TestBed.get(EfmApi);
    });

    it('should create service', function () {
        //assertions
        expect(efmApi).toBeDefined();
    });

    it('should get EL specification', function () {
        efmApi.getElSpecification('888').subscribe(
            function (response) {
                expect(response.flowIdentifier).toBe('888');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888/expression-language-spec');
        expect(req.request.method).toEqual('GET');

        req.flush({});

        httpMock.verify();
    });

    it('should fail to get EL specification', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getElSpecification('888').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm.calls.count()).toBe(0);
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888/expression-language-spec');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get flow summaries', function () {
        efmApi.getFlowSummaries().subscribe(
            function (response) {
                expect(response).toBe('test');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/summaries');
        expect(req.request.method).toEqual('GET');

        req.flush({
            elements: 'test'
        });

        httpMock.verify();
    });

    it('should fail to get flow summaries', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getFlowSummaries().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/summaries');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get flow summaries', function () {
        efmApi.getFlowSummaries().subscribe(
            function (response) {
                expect(response).toBe('test');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/summaries');
        expect(req.request.method).toEqual('GET');

        req.flush({
            elements: 'test'
        });

        httpMock.verify();
    });

    it('should fail to get flow summaries', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getFlowSummaries().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/summaries');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get flow', function () {
        efmApi.getFlow('888').subscribe(
            function (response) {
                expect(response).toBe('test');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            flowMetadata: 'test'
        });

        httpMock.verify();
    });

    it('should fail to get flow', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getFlow('888').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get events', function () {
        efmApi.getEvents().subscribe(
            function (response) {
                //assertions
                expect(response.pageNum).toBe(1);
                expect(response.rows).toBe(20);
                expect(response.totalRows).toBe(8710);
                expect(response.newEventsUrlPath).toBe("events?sort=eventType%3Aasc&filter=created%3A-lte%3A1549640875995");
                expect(response.events[0].id).toBe("20ce6cdc-6f94-4ccc-a530-e7a44334c29a");
            }
        );

        var req = httpMock.expectOne(URLS.EVENTS);
        expect(req.request.method).toEqual('GET');

        req.flush({
            "elements": [
                {
                    "id": "20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                    "level": "WARN",
                    "eventType": "Agent Status",
                    "message": "Netus et malesuada fames ac turpis.",
                    "created": 1549567882068,
                    "eventSource": {
                        "type": "Server",
                        "id": "http://localhost:18080/efm/api/c2-configuration"
                    },
                    "links": {
                        "detail": {
                            "href": "http://localhost:10080/efm/api/heartbeats/e8a2e023-1c70-4d4b-9a94-7bd5efd10820",
                            "rel": "details",
                            "title": "e8a2e023-1c70-4d4b-9a94-7bd5efd10820",
                            "type": "C2Heartbeat"
                        },
                        "self": {
                            "href": "http://localhost:10080/efm/api/events/20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                            "rel": "self",
                            "title": "20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                            "type": "Event"
                        }
                    }
                }
            ],
            "links": {
                "first": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=0",
                    "rel": "first"
                },
                "next": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=2",
                    "rel": "next"
                },
                "last": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=435",
                    "rel": "last"
                },
                "prev": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=0",
                    "rel": "prev"
                },
                "new": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3A-lte%3A1549640875995",
                    "rel": "new"
                },
                "self": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=1",
                    "rel": "self"
                }
            },
            "page": {
                "size": 20,
                "number": 1,
                "totalElements": 8710,
                "totalPages": 436
            }
        });

        httpMock.verify();
    });

    it('should fail to get events', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getEvents().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.EVENTS);
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get events from response link', function () {
        efmApi.getEventsFromResponseLink('888').subscribe(
            function (response) {
                //assertions
                expect(response.pageNum).toBe(1);
                expect(response.rows).toBe(20);
                expect(response.totalRows).toBe(8710);
                expect(response.newEventsUrlPath).toBe("events?sort=eventType%3Aasc&filter=created%3A-lte%3A1549640875995");
                expect(response.events[0].id).toBe("20ce6cdc-6f94-4ccc-a530-e7a44334c29a");
            }
        );

        var req = httpMock.expectOne(URLS.API +  '/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            "elements": [
                {
                    "id": "20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                    "level": "WARN",
                    "eventType": "Agent Status",
                    "message": "Netus et malesuada fames ac turpis.",
                    "created": 1549567882068,
                    "eventSource": {
                        "type": "Server",
                        "id": "http://localhost:18080/efm/api/c2-configuration"
                    },
                    "links": {
                        "detail": {
                            "href": "http://localhost:10080/efm/api/heartbeats/e8a2e023-1c70-4d4b-9a94-7bd5efd10820",
                            "rel": "details",
                            "title": "e8a2e023-1c70-4d4b-9a94-7bd5efd10820",
                            "type": "C2Heartbeat"
                        },
                        "self": {
                            "href": "http://localhost:10080/efm/api/events/20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                            "rel": "self",
                            "title": "20ce6cdc-6f94-4ccc-a530-e7a44334c29a",
                            "type": "Event"
                        }
                    }
                }
            ],
            "links": {
                "first": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=0",
                    "rel": "first"
                },
                "next": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=2",
                    "rel": "next"
                },
                "last": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=435",
                    "rel": "last"
                },
                "prev": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=0",
                    "rel": "prev"
                },
                "new": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3A-lte%3A1549640875995",
                    "rel": "new"
                },
                "self": {
                    "href": "events?sort=eventType%3Aasc&filter=created%3Alte%3A1549640875995&rows=20&pageNum=1",
                    "rel": "self"
                }
            },
            "page": {
                "size": 20,
                "number": 1,
                "totalElements": 8710,
                "totalPages": 436
            }
        });

        httpMock.verify();
    });

    it('should fail to get events from response link', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.getEventsFromResponseLink('888').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.API +  '/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should fail to revert flow', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.revertFlow('888').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888/revert');
        expect(req.request.method).toEqual('POST');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should fail to publish flow', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        efmApi.publishFlow('888').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/888/publish');
        expect(req.request.method).toEqual('POST');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });
});