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

var rxjs = require('rxjs/Subject');
var ngCore = require('@angular/core');
var EfmService = require('services/efm.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');
var fdsAnimations = require('@flow-design-system/common/animations');
var fdAnimations = require('@flow-designer/common/animations');
var EfmHttpInterceptor = require('services/efm.http-interceptor.service.js');

/**
 * Efm constructor.
 *
 * @param efmService                The efm singleton service.
 * @param flowProvider              The flow provider.
 * @param ngZone                    The ngZone service.
 * @constructor
 */
function Efm(efmService, flowProvider, ngZone, efmHttpInterceptor) {
    var self = this;

    this.efmService = efmService;
    this.ngZone = ngZone;

    var componentDestroyed$ = new rxjs.Subject();

    this.isHttpRequestInProgress = false;

    var httpRequestSubscription = efmHttpInterceptor.httpRequestLoadingObservable();
    httpRequestSubscription
        .takeUntil(componentDestroyed$)
        .distinctUntilChanged()
        .subscribe(function (inProgress) {
            self.isHttpRequestInProgress = inProgress;
        }, function (error) {
            self.isHttpRequestInProgress = false;
        });

    this.init = function () {
        this.efmService.sidenav = this.sidenav; //ngCore.ViewChild
        this.efmService.sidenavHost = this.sidenavHost; //ngCore.ViewChild

        this.ngZone.runOutsideAngular(function () {
            // listen for browser resize events to reset the graph size
            $(window).on('resize', function (e) {
                if (e.target === window) {
                    // resize grids when appropriate
                    var gridElements = $('*[class*="slickgrid_"]');
                    for (var j = 0, len = gridElements.length; j < len; j++) {
                        if ($(gridElements[j]).is(':visible')) {
                            setTimeout(function (gridElement) {
                                var grid = gridElement.data('gridInstance');
                                var editorLock = grid.getEditorLock();
                                if (!editorLock.isActive()) {
                                    grid.resizeCanvas();
                                }
                            }, 50, $(gridElements[j]));
                        }
                    }
                }
            });

            // setup custom checkbox
            $(document).on('click', 'div.nf-checkbox', function () {
                var checkbox = $(this);
                if (checkbox.hasClass('checkbox-unchecked')) {
                    checkbox.removeClass('checkbox-unchecked').addClass('checkbox-checked');
                } else {
                    checkbox.removeClass('checkbox-checked').addClass('checkbox-unchecked');
                }
                // emit a state change event
                checkbox.trigger('change');
            });

            $(document).on('click', 'span.nf-checkbox-label', function (e) {
                $(e.target).parent().find('.nf-checkbox').click();
            });
        });
    };

    this.destroy = function () {
        componentDestroyed$.next();
        componentDestroyed$.unsubscribe();
    };

    /**
     * Gets the link for the flow designer.
     */
    this.getDesignerLink = function () {
        var flowId = flowProvider.getFlowId();
        if (flowId === null) {
            return 'flow-designer/open';
        }

        return 'flow-designer/flow/' + flowId;
    };

    this.closeSafely = function () {
        var component = self.efmService.sidenavHost.component;

        if (component.getType() === 'controller-service') {
            efmService.routeToProcessGroupComponentListing(flowProvider.getFlowId(), component.getParentGroupId(), false);
        } else {
            efmService.routeToFlowDesignerCanvasComponent(flowProvider.getFlowId(), component.getId(), component.getType());
        }
    }
}

Efm.prototype = {
    constructor: Efm,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.init();
    },

    /**
     * Destroy the component
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

Efm.annotations = [
    new ngCore.Component({
        selector: 'efm-app',
        template: require('./efm.component.html!text'),
        queries: {
            sidenav: new ngCore.ViewChild('sidenav'),
            sidenavHost: new ngCore.ViewChild('sidenavHost')
        },
        animations: [
            fdsAnimations.flyInOutAnimation,
            fdAnimations.fadeAndRotateAnimation
        ]
    })
];

Efm.parameters = [
    EfmService,
    FlowProvider,
    ngCore.NgZone,
    EfmHttpInterceptor
];

module.exports = Efm;
