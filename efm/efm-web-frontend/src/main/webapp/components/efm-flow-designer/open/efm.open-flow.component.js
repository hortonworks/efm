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

var $ = require('jquery');
var rxjs = $.extend({}, require('rxjs/Subject'), require('rxjs/Observable'));
var ngCore = require('@angular/core');
var ngRouter = require('@angular/router');
var EfmApi = require('services/efm.api.service.js');
var EfmService = require('services/efm.service.js');
var fdsAnimations = require('@flow-design-system/common/animations');
var FlowProvider = require('services/efm.flow-provider.service.js');

/**
 * EfmOpenFlow constructor.
 *
 * @param activatedRoute                    The angular ActivatedRoute service.
 * @param efmApi                            The EFM API.
 * @param efmService                        The EFM singleton service.
 * @param flowProvider                      The flow provider
 * @constructor
 */
function EfmOpenFlow(activatedRoute, efmApi, efmService, flowProvider) {
    var self = this;

    // Subscriptions
    this.routeSubscription = activatedRoute.params
        .switchMap(function (params) {
            return efmApi.getFlowSummaries();
        })
        .subscribe(function (flows) {
            efmService.perspective = 'designer';
            efmService.agentClass = '';

            if (Array.isArray(flows)) {
                self.efmFlowListing.setFlows(flows);
            }
        }, function (errorResponse) {
        });
    this.flowListingSubscription = null;
    this.flowOpen = false;

    // Subjects
    this.componentDestroyed$ = new rxjs.Subject();

    /**
     * Configure this component.
     */
    this.init = function () {
        this.flowOpen = flowProvider.getFlowId() !== null

        this.flowListingSubscription = this.efmFlowListing.subject$
            .subscribe(function (flow) {
                efmService.routeToFlow(flow.identifier);
            });
    };

    /**
     * Opens the currently selected flow.
     */
    this.openFlow = function () {
        var flow = this.efmFlowListing.getSelectedFlow();
        efmService.routeToFlow(flow.identifier);
    };

    /**
     * Goes back to the currently open flow.
     */
    this.backToFlow = function () {
        var flowId = flowProvider.getFlowId();

        if (flowId !== null) {
            efmService.routeToFlow(flowId);
        }
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.routeSubscription.unsubscribe();
        this.flowListingSubscription.unsubscribe();

        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    };
};

EfmOpenFlow.prototype = {
    constructor: EfmOpenFlow,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        this.init();
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

EfmOpenFlow.annotations = [
    new ngCore.Component({
        template: require('./efm.open-flow.component.html!text'),
        queries: {
            efmFlowListing: new ngCore.ViewChild('efmFlowListing'),
            efmOpenFlowElementRef: new ngCore.ViewChild('efmOpenFlowElementRef')
        },
        animations: [fdsAnimations.flyInOutAnimation]
    })
];

EfmOpenFlow.parameters = [
    ngRouter.ActivatedRoute,
    EfmApi,
    EfmService,
    FlowProvider
];

module.exports = EfmOpenFlow;
