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

var ngCore = require('@angular/core');
var EfmService = require('services/efm.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');
var DialogService = require('services/efm.dialog.service.js');
var ngRouter = require('@angular/router');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * EfmFlowDesignerComponentListingWrapperComponent constructor.
 *
 * @param efmService            The EFM singleton service.
 * @param flowProvider          The flow provider.
 * @param route                 The angular activated route service.
 * @param dialogService         The dialog service module.
 * @param common                The common service.
 * @constructor
 */
function EfmFlowDesignerComponentListingWrapperComponent(efmService, flowProvider, route, dialogService, common) {
    var self = this;
    var dialogRef = null;

    // Subscriptions
    this.routeSubscription = null;

    /**
     * Configure this component.
     */
    this.init = function () {
        if (['process-groups'].indexOf(route.snapshot.params['type']) >= 0) {
            this.routeSubscription = route.params
                .subscribe(function (params) {
                    if (['process-groups'].indexOf(params['type']) < 0) {
                        // re-route the user back to flow designer
                        efmService.routeToFlowDesigner(flowProvider.getFlowId(), true, 'No components to list for requested type.');
                    } else {
                        efmService.flowDesignerCanvasComponent.disableHotKeys();
                        efmService.flowDesignerCanvasComponent.getProcessGroupComponentListing(params['id'])
                            .then(function (pg) {
                                efmService.flowDesignerCanvasComponent.list(pg)
                                    .then(function () {
                                        // Complete! now route back to the selected component
                                        efmService.routeToFlowDesigner(flowProvider.getFlowId());
                                    })
                                    .catch(function (message) {
                                        dialogRef = dialogService.openConfirm({
                                            title: 'Unable to list components',
                                            message: message
                                        });

                                        // pg doesn't exist
                                        efmService.routeToFlowDesigner(flowProvider.getFlowId(), true, message);
                                    });
                            })
                            .catch(function (message) {
                                // unable to get process group controller services, re-route the user back to flow designer
                                efmService.routeToFlowDesigner(flowProvider.getFlowId(), true);
                            });
                    }
                });
        }
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        if(common.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
        
        efmService.unloadComponentListingHost();
        efmService.flowDesignerCanvasComponent.enableHotKeys();
        if (['process-groups'].indexOf(route.snapshot.params['type']) >= 0) {
            this.routeSubscription.unsubscribe();
        }
    };
};

EfmFlowDesignerComponentListingWrapperComponent.prototype = {
    constructor: EfmFlowDesignerComponentListingWrapperComponent,

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

EfmFlowDesignerComponentListingWrapperComponent.annotations = [
    new ngCore.Component({
        template: require('./efm.fd-component-listing.component.html!text')
    })
];

EfmFlowDesignerComponentListingWrapperComponent.parameters = [
    EfmService,
    FlowProvider,
    ngRouter.ActivatedRoute,
    DialogService,
    CommonService
];

module.exports = EfmFlowDesignerComponentListingWrapperComponent;
