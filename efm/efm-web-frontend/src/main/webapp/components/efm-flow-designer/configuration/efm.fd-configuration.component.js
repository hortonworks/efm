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
var ngRouter = require('@angular/router');

/**
 * EfmFlowDesignerConfigurationWrapperComponent constructor.
 *
 * @param efmService            The EFM singleton service.
 * @param flowProvider          The flow provider.
 * @param route                 The angular activated route service.
 * @constructor
 */
function EfmFlowDesignerConfigurationWrapperComponent(efmService, flowProvider, route) {
    var self = this;

    // Subscriptions
    this.routeSubscription = null;

    /**
     * Configure this component.
     */
    this.init = function () {
        this.routeSubscription = route.params
            .subscribe(function (params) {
                // configure the component in question
                efmService.flowDesignerCanvasComponent.configure(route.parent.snapshot.params['id'], route.parent.snapshot.params['type'])
                    .then(function (componentEntity) {
                        // Complete! now route back to the selected component
                        efmService.routeToFlowDesignerCanvasComponent(flowProvider.getFlowId(), componentEntity.id, componentEntity.type);
                    })
                    .catch(function (message) {
                        efmService.routeToFlowDesigner(flowProvider.getFlowId(), true);
                    });
            });
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        efmService.unloadSidenavHost();
        this.routeSubscription.unsubscribe();
    };
};

EfmFlowDesignerConfigurationWrapperComponent.prototype = {
    constructor: EfmFlowDesignerConfigurationWrapperComponent,

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

EfmFlowDesignerConfigurationWrapperComponent.annotations = [
    new ngCore.Component({
        template: require('./efm.fd-configuration.component.html!text')
    })
];

EfmFlowDesignerConfigurationWrapperComponent.parameters = [
    EfmService,
    FlowProvider,
    ngRouter.ActivatedRoute
];

module.exports = EfmFlowDesignerConfigurationWrapperComponent;
