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
var ngCoreTesting = require('@angular/core/testing');
var ngHttpTesting = require('@angular/common/http/testing');
var ngCommonHttp = require('@angular/common/http');

var efmFlowDesignerApiFactory = require('services/efm.fd-api.factory.js');
var EfmApi = require('services/efm.api.service.js');
var EfmHttpErrorResponseFactory = require('services/efm.http-error-response.factory.js');
var DialogService = require('services/efm.dialog.service.js');
var EfmErrorService = require('services/efm.error.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');
var ProcessorManager = require('@flow-designer/services/ProcessorManager');
var RemoteProcessGroupManager = require('@flow-designer/services/RemoteProcessGroupManager');
var FunnelManager = require('@flow-designer/services/FunnelManager');
var ConnectionManager = require('@flow-designer/services/ConnectionManager');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');
var CommonService = require('@flow-designer/services/CommonService');

var URLS = require('config/urls.js');

describe('EFM Flow Designer Api Factory', function () {
    var flowDesignerApi;
    var httpMock;
    var dialogService;
    var flowProvider;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngHttpTesting.HttpClientTestingModule
            ],
            providers: [
                EfmApi,
                FlowProvider,
                EfmErrorService,
                DialogService,
                ProcessorManager,
                FunnelManager,
                ConnectionManager,
                RemoteProcessGroupManager,
                {
                    provide: 'EfmHttpErrorResponse',
                    useFactory: EfmHttpErrorResponseFactory.build,
                    deps: [
                        EfmErrorService
                    ]
                },
                {
                    provide: 'FlowDesignerApi',
                    useFactory: efmFlowDesignerApiFactory.build,
                    deps: [
                        ngCommonHttp.HttpClient,
                        FlowProvider,
                        CommonService,
                        DialogService
                    ]
                }
            ]
        });

        // get the flowDesignerApi from the root injector
        flowDesignerApi = new (ngCoreTesting.TestBed.get('FlowDesignerApi'))({
            nativeElement: {
                getAttribute: function () {
                    return '1234';
                }
            }
        });
        httpMock = ngCoreTesting.TestBed.get(ngHttpTesting.HttpTestingController);
        dialogService = ngCoreTesting.TestBed.get(DialogService);
        flowProvider = ngCoreTesting.TestBed.get(FlowProvider);

        var processorManager = ngCoreTesting.TestBed.get(ProcessorManager);
        var remoteProcessGroupManager = ngCoreTesting.TestBed.get(RemoteProcessGroupManager);
        var funnelManager = ngCoreTesting.TestBed.get(FunnelManager);
        var connectionManager = ngCoreTesting.TestBed.get(ConnectionManager);

        // Spy
        spyOn(flowProvider, 'getFlowId').and.callFake(function () {
            return '1234';
        });
        spyOn(processorManager, 'expireCaches').and.callFake(function () {});
        spyOn(processorManager, 'get').and.callFake(function () {});
        spyOn(processorManager, 'set').and.callFake(function () {});
        spyOn(processorManager, 'pan').and.callFake(function () {});
        spyOn(funnelManager, 'expireCaches').and.callFake(function () {});
        spyOn(funnelManager, 'get').and.callFake(function () {});
        spyOn(funnelManager, 'set').and.callFake(function () {});
        spyOn(connectionManager, 'expireCaches').and.callFake(function () {});
        spyOn(connectionManager, 'get').and.callFake(function () {});
        spyOn(connectionManager, 'pan').and.callFake(function () {});
        spyOn(connectionManager, 'set').and.callFake(function () {});
        spyOn(remoteProcessGroupManager, 'expireCaches').and.callFake(function () {});
        spyOn(remoteProcessGroupManager, 'get').and.callFake(function () {});
        spyOn(remoteProcessGroupManager, 'set').and.callFake(function () {});
        spyOn(remoteProcessGroupManager, 'pan').and.callFake(function () {});
    });

    it('should create service', function () {
        //assertions
        expect(flowDesignerApi).toBeDefined();
    });

    it('should fail to get client id', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getClientId().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get client id', function () {
        flowDesignerApi.getClientId().subscribe(
            function (cId) {
                expect(cId).toBe('444');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        httpMock.verify();
    });

    it('should get flow', function () {
        // Spy
        spyOn(flowProvider, 'get').and.callFake(function () {
            return rxjs.Observable.of({
                identifier: '000'
            });
        });

        flowDesignerApi.getFlow(888).subscribe(
            function (mappedFlow) {
                expect(mappedFlow.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/000/process-groups/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            flowContent: {
                identifier: '000',
                funnels: [],
                processors: [],
                remoteProcessGroups: [],
                connections: []
            }
        });

        httpMock.verify();
    });

    it('should fail to get flow', function () {
        // Spy
        spyOn(flowProvider, 'get').and.callFake(function () {
            return rxjs.Observable.of({
                identifier: '000'
            });
        });
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getFlow(888).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/000/process-groups/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get version info', function () {
        flowDesignerApi.getVersionInfo().subscribe(
            function (versionInfo) {
                expect(versionInfo).toBe('444');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/version-info');
        expect(req.request.method).toEqual('GET');

        req.flush({versionInfo:'444'});

        httpMock.verify();
    });

    it('should fail to get version info', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getVersionInfo().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/version-info');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get processor extensions', function () {
        flowDesignerApi.getProcessorExtensions().subscribe(
            function (mappedExtensions) {
                expect(mappedExtensions[0].type).toBe('testType');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/types/processors');
        expect(req.request.method).toEqual('GET');

        req.flush({componentTypes: [
            {
                type: 'testType'
            }
        ]});

        httpMock.verify();
    });

    it('should fail to get processor extensions', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessorExtensions().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/types/processors');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get CS extensions', function () {
        flowDesignerApi.getControllerServiceExtensions().subscribe(
            function (mappedExtensions) {
                expect(mappedExtensions[0].type).toBe('testType');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/types/controller-services');
        expect(req.request.method).toEqual('GET');

        req.flush({componentTypes: [
            {
                type: 'testType'
            }
        ]});

        httpMock.verify();
    });

    it('should fail to get CS extensions', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getControllerServiceExtensions().subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/types/controller-services');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get processor property descriptor', function () {
        flowDesignerApi.getProcessorPropertyDescriptor({
            id: '999'
        }, 'testName').subscribe(
            function (mappedPropertyDescriptor) {
                expect(mappedPropertyDescriptor.displayName).toBe('testDisplayName');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/999/descriptors/testName');
        expect(req.request.method).toEqual('GET');

        req.flush({propertyDescriptor:
            {
                displayName: 'testDisplayName'
            }
        });

        httpMock.verify();
    });

    it('should fail to get processor property descriptor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessorPropertyDescriptor({
            id: '999'
        }, 'testName').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/999/descriptors/testName');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get controller service property descriptor', function () {
        flowDesignerApi.getControllerServicePropertyDescriptor({
            id: '999'
        }, 'testName').subscribe(
            function (mappedPropertyDescriptor) {
                expect(mappedPropertyDescriptor.displayName).toBe('testDisplayName');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/999/descriptors/testName');
        expect(req.request.method).toEqual('GET');

        req.flush({propertyDescriptor:
            {
                displayName: 'testDisplayName'
            }
        });

        httpMock.verify();
    });

    it('should fail to get controller service property descriptor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getControllerServicePropertyDescriptor({
            id: '999'
        }, 'testName').subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/999/descriptors/testName');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get EL specification', function () {
        // Spy
        spyOn(flowProvider, 'getElSpecification').and.callFake(function () {
            return rxjs.Observable.of({
                spec: {
                    operations: 'testOperations'
                }
            })
        });

        flowDesignerApi.getELSpecification().subscribe(
            function (operations) {
                expect(operations).toBe('testOperations');
            }
        );
    });

    it('should fail to get EL specification', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});
        spyOn(flowProvider, 'getElSpecification').and.callFake(function () {
            return rxjs.Observable.throw({
                preventDefault: false,
                message: 'test'
            })
        });

        flowDesignerApi.getELSpecification().subscribe(
            function () {},
            function (error) {
                expect(dialogService.openConfirm.calls.count()).toBe(0);
            }
        );
    });

    it('should create funnel', function () {
        flowDesignerApi.createFunnel('888', {x:1, y:1}).subscribe(
            function (mappedFunnel) {
                expect(mappedFunnel.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/funnels');
        expect(req2.request.method).toEqual('POST');

        req2.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to create funnel', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.createFunnel('888', {x:1, y:1}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/funnels');
        expect(req2.request.method).toEqual('POST');

        req2.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get funnel', function () {
        flowDesignerApi.getFunnel({id:888}).subscribe(
            function (mappedFunnel) {
                expect(mappedFunnel.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to get funnel', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getFunnel({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update funnel', function () {
        flowDesignerApi.updateFunnel({id:888}).subscribe(
            function (mappedFunnel) {
                expect(mappedFunnel.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to update funnel', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateFunnel({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should delete funnel', function () {
        flowDesignerApi.removeFunnel(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function (mappedFunnel) {
                expect(mappedFunnel.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to delete funnel', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.removeFunnel(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/funnels/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should create processor', function () {
        flowDesignerApi.createProcessor('888',
            {
                type: 'testDescriptorType',
                bundle: {
                    group: 'testDescriptorBundleGroup',
                    artifact: 'testDescriptorBundleArtifact',
                    version: 'testDescriptorBundleVersion'
                }
            },
            {
                x:1,
                y:1
            }).subscribe(
            function (mappedProcessor) {
                expect(mappedProcessor.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/processors');
        expect(req2.request.method).toEqual('POST');

        req2.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to create processor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.createProcessor('888',
            {
                type: 'testDescriptorType',
                bundle: {
                    group: 'testDescriptorBundleGroup',
                    artifact: 'testDescriptorBundleArtifact',
                    version: 'testDescriptorBundleVersion'
                }
            },
            {
                x:1,
                y:1
            }).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/processors');
        expect(req2.request.method).toEqual('POST');

        req2.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get processor', function () {
        flowDesignerApi.getProcessor({id:888}).subscribe(
            function (mappedProcessor) {
                expect(mappedProcessor.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to get processor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessor({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update processor', function () {
        flowDesignerApi.updateProcessor({id:888}).subscribe(
            function (mappedProcessor) {
                expect(mappedProcessor.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to update processor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateProcessor({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should remove processor', function () {
        flowDesignerApi.removeProcessor(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function (mappedProcessor) {
                expect(mappedProcessor.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to remove processor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.removeProcessor(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should create connection', function () {
        flowDesignerApi.createConnection('888',
            {
                component: {}
            }).subscribe(
            function (mappedConnection) {
                expect(mappedConnection.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/connections');
        expect(req2.request.method).toEqual('POST');

        req2.flush({
            componentConfiguration: {
                identifier: '000',
                source: {},
                destination: {},
                selectedRelationships: [],
                bends: []
            }
        });

        httpMock.verify();
    });

    it('should fail to create connection', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.createConnection('888',
            {
                component: {}
            }).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/connections');
        expect(req2.request.method).toEqual('POST');

        req2.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get connection', function () {
        flowDesignerApi.getConnection({id:888}).subscribe(
            function (mappedConnection) {
                expect(mappedConnection.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            componentConfiguration: {
                identifier: '000',
                source: {},
                destination: {},
                selectedRelationships: [],
                bends: []
            }
        });

        httpMock.verify();
    });

    it('should fail to get connection', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getConnection({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update connection', function () {
        flowDesignerApi.updateConnection({id:888}).subscribe(
            function (mappedConnection) {
                expect(mappedConnection.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            componentConfiguration: {
                identifier: '000',
                source: {},
                destination: {},
                selectedRelationships: [],
                bends: []
            }
        });

        httpMock.verify();
    });

    it('should fail to update connection', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateConnection({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should remove connection', function () {
        flowDesignerApi.removeConnection(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function (mappedConnection) {
                expect(mappedConnection.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.flush({
            componentConfiguration: {
                identifier: '000',
                source: {},
                destination: {},
                selectedRelationships: [],
                bends: []
            }
        });

        httpMock.verify();
    });

    it('should fail to remove connection', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.removeConnection(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/connections/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should create rpg', function () {
        flowDesignerApi.createRemoteProcessGroup('888', {x: 1, y: 1}, []).subscribe(
            function (mappedRpg) {
                expect(mappedRpg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/remote-process-groups');
        expect(req2.request.method).toEqual('POST');

        req2.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to create rpg', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.createRemoteProcessGroup('888', {x: 1, y: 1}, []).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/remote-process-groups');
        expect(req2.request.method).toEqual('POST');

        req2.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get rpg', function () {
        flowDesignerApi.getRemoteProcessGroup({id:888}).subscribe(
            function (mappedRpg) {
                expect(mappedRpg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to get rpg', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getRemoteProcessGroup({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update rpg', function () {
        flowDesignerApi.updateRemoteProcessGroup({id:888}).subscribe(
            function (mappedRpg) {
                expect(mappedRpg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to update rpg', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateRemoteProcessGroup({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should remove rpg', function () {
        flowDesignerApi.removeRemoteProcessGroup(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function (mappedRpg) {
                expect(mappedRpg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to remove rpg', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.removeRemoteProcessGroup(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/remote-process-groups/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should create CS', function () {
        flowDesignerApi.createControllerService('888', {
            bundle: {
                group: 'testDescriptorBundleGroup',
                artifact: 'testDescriptorBundleArtifact',
                version: 'testDescriptorBundleVersion'
            }
        }).subscribe(
            function (mappedCs) {
                expect(mappedCs.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/controller-services');
        expect(req2.request.method).toEqual('POST');

        req2.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to create CS', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.createControllerService('888', {
            bundle: {
                group: 'testDescriptorBundleGroup',
                artifact: 'testDescriptorBundleArtifact',
                version: 'testDescriptorBundleVersion'
            }
        }).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.CLIENT_ID);
        expect(req.request.method).toEqual('GET');

        req.flush('444');

        var req2 = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/controller-services');
        expect(req2.request.method).toEqual('POST');

        req2.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get CS', function () {
        flowDesignerApi.getControllerService(888).subscribe(
            function (mappedCs) {
                expect(mappedCs.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888');
        expect(req.request.method).toEqual('GET');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to get CS', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getControllerService(888).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should delete CS', function () {
        flowDesignerApi.deleteControllerService(
            {
                id: '888',
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function (mappedCs) {
                expect(mappedCs.id).toBe('888');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.flush({
            id: '888'
        });

        httpMock.verify();
    });

    it('should fail to delete CS', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.deleteControllerService(
            {
                id: 888,
                revision: {
                    version: 1,
                    clientId: 777
                }
            }
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888?version=1&clientId=777');
        expect(req.request.method).toEqual('DELETE');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update CS', function () {
        flowDesignerApi.updateControllerService({id:888}).subscribe(
            function (mappedCs) {
                expect(mappedCs.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            componentConfiguration: {
                identifier: '000'
            },
            componentDefinition: {
                propertyDescriptors: []
            }
        });

        httpMock.verify();
    });

    it('should fail to update CS', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateControllerService({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get PG', function () {
        flowDesignerApi.getProcessGroup(888).subscribe(
            function (mappedPg) {
                expect(mappedPg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888?includeChildren=false');
        expect(req.request.method).toEqual('GET');

        req.flush({
            flowContent: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to get PG', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessGroup(888).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888?includeChildren=false');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should update PG', function () {
        flowDesignerApi.updateProcessGroup({id:888}).subscribe(
            function (mappedPg) {
                expect(mappedPg.id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888');
        expect(req.request.method).toEqual('PUT');

        req.flush({
            flowContent: {
                identifier: '000'
            }
        });

        httpMock.verify();
    });

    it('should fail to update PG', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.updateProcessGroup({id:888}).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888');
        expect(req.request.method).toEqual('PUT');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get PG CS\'s', function () {
        flowDesignerApi.getProcessGroupControllerServices(888).subscribe(
            function (mappedServices) {
                expect(mappedServices[0].id).toBe('000');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/controller-services');
        expect(req.request.method).toEqual('GET');

        req.flush({
            elements: [
                {
                    componentConfiguration: {
                        identifier: '000'
                    },
                    componentDefinition: {
                        propertyDescriptors: []
                    }
                }
            ]
        });

        httpMock.verify();
    });

    it('should fail to get PG CS\'s', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessGroupControllerServices(888).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/process-groups/888/controller-services');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get processor property descriptor', function () {
        flowDesignerApi.getProcessorPropertyDescriptor(
            {
                id: 888
            },
            'testPropertyName'
        ).subscribe(
            function (mappedPropertyDescriptor) {
                expect(mappedPropertyDescriptor.name).toBe('testname');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888/descriptors/testPropertyName');
        expect(req.request.method).toEqual('GET');

        req.flush({
            propertyDescriptor: {
                name: 'testname'
            }
        });

        httpMock.verify();
    });

    it('should fail to get processor property descriptor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getProcessorPropertyDescriptor(
            {
                id: 888
            },
            'testPropertyName'
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/processors/888/descriptors/testPropertyName');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });

    it('should get CS property descriptor', function () {
        flowDesignerApi.getControllerServicePropertyDescriptor(
            {
                id: 888
            },
            'testPropertyName'
        ).subscribe(
            function (mappedPropertyDescriptor) {
                expect(mappedPropertyDescriptor.name).toBe('testname');
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888/descriptors/testPropertyName');
        expect(req.request.method).toEqual('GET');

        req.flush({
            propertyDescriptor: {
                name: 'testname'
            }
        });

        httpMock.verify();
    });

    it('should fail to get CS property descriptor', function () {
        // Spy
        spyOn(dialogService, 'openConfirm').and.callFake(function () {});

        flowDesignerApi.getControllerServicePropertyDescriptor(
            {
                id: 888
            },
            'testPropertyName'
        ).subscribe(
            function () {},
            function (error) {
                expect(error instanceof ngCommonHttp.HttpErrorResponse).toBeTruthy();
                expect(dialogService.openConfirm).toHaveBeenCalled();
            }
        );

        var req = httpMock.expectOne(URLS.DESIGNER.FLOWS + '/1234/controller-services/888/descriptors/testPropertyName');
        expect(req.request.method).toEqual('GET');

        req.error('UnExpectedError', {
            status: 500
        });

        httpMock.verify();
    });
});
