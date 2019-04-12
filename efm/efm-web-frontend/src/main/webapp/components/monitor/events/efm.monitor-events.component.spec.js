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
require('rxjs/add/operator/switchMap');
require('rxjs/add/operator/shareReplay');
require('rxjs/add/observable/empty');
require('rxjs/add/observable/throw');
require('rxjs/add/operator/map');
require('rxjs/add/operator/mergeMap');
require('rxjs/add/operator/concatMap');
require('rxjs/add/operator/catch');
require('rxjs/add/observable/of');
require('rxjs/add/operator/do');
require('rxjs/add/operator/takeUntil');
require('rxjs/add/observable/forkJoin');
require('rxjs/add/operator/finally');

// rxjs Observable debugger;
var debuggerOn = false;

rxjs.Observable.prototype.debug = function (message) {
    return this.do(
        function (next) {
            if (debuggerOn) {
                console.log(message, next);
            }
        },
        function (err) {
            if (debuggerOn) {
                console.error('ERROR >>> ', message, err);
            }
        },
        function () {
            if (debuggerOn) {
                console.log('Completed.');
            }
        }
    );
};

var ngCommon = require('@angular/common');
var ngCommonHttp = require('@angular/common/http');
var ngCoreTesting = require('@angular/core/testing');
var ngRouter = require('@angular/router');

var FdsCoreModule = require('@flow-design-system/core');
var FlowDesignerCoreModule = require('@flow-designer/modules/core');

var Efm = require('efm.component.js');
var efmRoutes = require('efm.routes.js');

var EfmApi = require('services/efm.api.service.js');
var EfmService = require('services/efm.service.js');
var DialogService = require('services/efm.dialog.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');
var FlowResolver = require('services/efm.flow.resolve.service.js');

var flowDesignerApiFactory = require('@flow-designer/services/FlowDesignerApiFactory');

var EfmMonitor = require('components/monitor/efm.monitor.component.js');
var EfmMonitorEvents = require('components/monitor/events/efm.monitor-events.component.js');
var EfmMonitorEventDetails = require('components/monitor/events/details/efm.monitor-event-details.component.js');
var EfmNotFound = require('components/404/efm.404.component.js');
var EfmError = require('components/error/efm.error.component.js');
var EfmAuthError = require('components/auth-error/efm.auth-error.component.js');
var EfmHost = require('efm.host.component.js');
var EfmOpenFlow = require('components/efm-flow-designer/open/efm.open-flow.component.js');
var EfmFlowListing = require('components/efm-flow-designer/open/efm.flow-listing.component.js');
var EfmFlowDesigner = require('components/efm-flow-designer/efm.flow-designer.component.js');
var EfmFlowDesignerConfigurationWrapper = require('components/efm-flow-designer/configuration/efm.fd-configuration.component.js');
var EfmFlowDesignerComponentListingWrapper = require('components/efm-flow-designer/component-listing/efm.fd-component-listing.component.js');
var EfmFlowDesignerControllerServiceConfigurationWrapper = require('components/efm-flow-designer/controller-service-configuration/efm.fd-controller-service-configuration.component.js');
var EfmFlowDesignerSelectionWrapper = require('components/efm-flow-designer/selection/efm.fd-selection.component.js');
var EfmFlowDesignerPublishFlow = require('components/efm-flow-designer/publish/efm.publish-flow.component.js');

describe('EFM monitor events Component', function () {
    var efmMonitorEvents;
    var fixture;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            imports: [
                FdsCoreModule,
                FlowDesignerCoreModule,
                ngCommonHttp.HttpClientModule,
                efmRoutes
            ],
            declarations: [
                Efm,
                EfmMonitor,
                EfmMonitorEvents,
                EfmMonitorEventDetails,
                EfmFlowDesigner,
                EfmOpenFlow,
                EfmFlowListing,
                EfmHost,
                EfmFlowDesignerConfigurationWrapper,
                EfmFlowDesignerSelectionWrapper,
                EfmFlowDesignerComponentListingWrapper,
                EfmFlowDesignerControllerServiceConfigurationWrapper,
                EfmFlowDesignerPublishFlow,
                EfmNotFound,
                EfmError,
                EfmAuthError
            ],
            entryComponents: [
                EfmFlowListing,
                EfmFlowDesignerPublishFlow,
                EfmMonitorEventDetails
            ],
            providers: [
                FlowProvider,
                FlowResolver,
                EfmApi,
                EfmService,
                DialogService,
                {
                    provide: 'FlowDesignerApi',
                    useFactory: flowDesignerApiFactory.build,
                    deps: []
                },
                {
                    provide: ngCommon.APP_BASE_HREF,
                    useValue: '/'
                }, {
                    provide: ngRouter.ActivatedRoute,
                    useValue: {
                        queryParamMap: rxjs.Observable.of({
                            params: {
                                filter: "level:like:WARN",
                                pageNum: "1",
                                rows: "100",
                                sort: "level:ASC",
                                timeRange: "LAST_7_DAYS"
                            }
                        })
                    }
                }
            ]
        });

        fixture = ngCoreTesting.TestBed.createComponent(EfmMonitorEvents);
        efmMonitorEvents = fixture.componentInstance;

        // Spy
        spyOn(efmMonitorEvents, 'init').and.callThrough();
        jasmine.clock().uninstall();
        jasmine.clock().install();

        // get the efmApi from the root injector
        var efmApi = ngCoreTesting.TestBed.get(EfmApi);
        spyOn(efmApi, 'getEvents').and.callFake(function () {
            return rxjs.Observable.of({});
        });

        // init
        efmMonitorEvents.ngOnInit();
    });

    it('should create component', function () {
        //assertions
        expect(efmMonitorEvents).toBeDefined();
        expect(efmMonitorEvents.init).toHaveBeenCalled();
        expect(efmMonitorEvents.searchCriteria[0].searchText).toBe("WARN");
    });

    it('should update search criteria', function () {
        efmMonitorEvents.updateSearchCriteria('test');
    });

    it('should remove search criteria', function () {
        efmMonitorEvents.searchCriteria = [{filter: 'test'}];

        efmMonitorEvents.removeSearchCriteria({filter: 'test'});
        expect(efmMonitorEvents.searchCriteria.length).toBe(0);
    });

    it('should page events', function () {
        efmMonitorEvents.page({
            page: 1,
            pageSize: 50
        });
    });

    it('should sort events', function () {
        efmMonitorEvents.sortEvents({
            sortable: true,
            sortOrder: 'DESC'
        });

        //assertions
        expect(efmMonitorEvents.activeColumn.sortOrder).toBe('ASC');
        expect(efmMonitorEvents.activeColumn.active).toBe(true);
    });

    it('should time range change', function () {
        efmMonitorEvents.onTimeRangeChange({
            value: 'LAST_HOUR',
            label: 'last hour'
        });

        //assertions
        expect(efmMonitorEvents.activeTimeRange).toBe('last hour');
    });

    it('should rows change', function () {
        efmMonitorEvents.onRowsChange({
            value: 'test',
            label: 'label'
        });

        //assertions
        expect(efmMonitorEvents.rowsCount).toBe('label');
    });

    it('should reload events', function () {
        efmMonitorEvents.events = [];
        efmMonitorEvents.reloadEvents();
    });

    it('should show latest and reload page', function () {
        efmMonitorEvents.events = [];

        spyOn(efmMonitorEvents, 'reloadEvents').and.callThrough();

        efmMonitorEvents.activeColumn = {
            name: 'created',
            sortOrder: 'DESC'
        };
        efmMonitorEvents.pageNumber = 1;

        efmMonitorEvents.showLatest();

        //assertions
        expect(efmMonitorEvents.reloadEvents).toHaveBeenCalled();
    });

    it('should show latest and show first page', function () {
        spyOn(efmMonitorEvents.pagingBar, 'firstPage').and.callThrough();

        efmMonitorEvents.activeColumn = {
            name: 'created',
            sortOrder: 'DESC'
        };
        efmMonitorEvents.pageNumber = 10;

        efmMonitorEvents.showLatest();

        //assertions
        expect(efmMonitorEvents.pagingBar.firstPage).toHaveBeenCalled();
    });

    it('should show latest and sort events', function () {
        efmMonitorEvents.events = [];

        spyOn(efmMonitorEvents, 'sortEvents').and.callThrough();

        efmMonitorEvents.activeColumn = {
            name: 'uncreated',
            sortOrder: 'DESC'
        };

        efmMonitorEvents.showLatest();

        //assertions
        expect(efmMonitorEvents.sortEvents).toHaveBeenCalled();
    });

    it('should select event to show details', function () {
        efmMonitorEvents.selectEvent({
            id: 123,
            name: 'event1'
        });
        jasmine.clock().tick(501);

        //assertions
        expect(efmMonitorEvents.eventDetailsOpen).toBe(true);
        expect(efmMonitorEvents.selectedEvent.name).toBe('event1');
        expect(efmMonitorEvents.selectedEventId).toBe(123);
    });

    it('should select event by row', function () {
        efmMonitorEvents.eventDetailsOpen = true;

        spyOn(efmMonitorEvents, 'selectEvent').and.callThrough();

        efmMonitorEvents.selectEventByRow({
            id: 123,
            name: 'event1',
            links: {
                detail: {
                    href: 'http://localhost'
                }
            }
        });

        //assertions
        expect(efmMonitorEvents.selectEvent).toHaveBeenCalled();
        expect(efmMonitorEvents.eventDetailsOpen).toBe(true);
        expect(efmMonitorEvents.selectedEvent.name).toBe('event1');
        expect(efmMonitorEvents.selectedEventId).toBe(123);
    });

    it('should unselect event to hide details', function () {
        efmMonitorEvents.unselectEvent();
        jasmine.clock().tick(501);

        //assertions
        expect(efmMonitorEvents.eventDetailsOpen).toBe(false);
        expect(efmMonitorEvents.selectedEvent).toBe(null);
        expect(efmMonitorEvents.selectedEventId).toBe(null);
    });
});
