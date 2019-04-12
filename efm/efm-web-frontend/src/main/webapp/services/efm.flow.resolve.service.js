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
var FlowProvider = require('services/efm.flow-provider.service.js');
var EfmService = require('services/efm.service.js');

/**
 * EfmFlowResolver constructor.
 *
 * @constructor
 */
function EfmFlowResolver(flowProvider, efmService) {

    /**
     * Attempts to resolve the flow in the activated route.
     *
     * @param route                     The activated route
     * @returns {Observable}            Resolved flow
     */
    this.resolve = function (route) {
        var flowId = route.paramMap.get('flowId');
        var existingFlowId = flowProvider.getFlowId();

        // set the flow id
        flowProvider.setFlowId(flowId);

        return flowProvider.get().catch(function (errorResponse) {
            // reset the flow id
            flowProvider.setFlowId(existingFlowId);

            // route to the open flow page
            if (!errorResponse.preventDefault) {
                efmService.routeToOpenFlow();
            }

            // resolve as empty to prevent loading configured route
            return rxjs.Observable.empty();
        });
    };
};

EfmFlowResolver.prototype = {
    constructor: EfmFlowResolver,
};

EfmFlowResolver.parameters = [
    FlowProvider,
    EfmService
];

module.exports = EfmFlowResolver;
