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
var EfmApi = require('services/efm.api.service.js');

/**
 * EfmFlowProvider constructor.
 *
 * @param efmApi                The efm api.
 * @param ErrorResponse         The flow designer ErrorResponse.
 * @constructor
 */
function EfmFlowProvider(efmApi, ErrorResponse) {
    var flow = null;
    var flowId = null;
    var elSpecification = null;

    this.get = function () {
        if (flowId === null) {
            return rxjs.Observable.throw(new ErrorResponse(false, false, 'No flow id set.'));
        }

        if (flow === null || flow.identifier !== flowId) {
            var getFlow$ = efmApi.getFlow(flowId);

            getFlow$.subscribe(function (response) {
                flow = response;
            }, function (errorResponse) {
            });

            return getFlow$;
        }

        return rxjs.Observable.of(flow);
    };

    this.getElSpecification = function () {
        if (flowId === null) {
            return rxjs.Observable.throw(new ErrorResponse(false, false, 'No flow id set.'));
        }

        if (elSpecification === null || elSpecification.flowIdentifier !== flowId) {
            var getElSpecification$ = efmApi.getElSpecification(flowId);

            getElSpecification$.subscribe(function (response) {
                elSpecification = response;
            }, function (errorResponse) {
            });

            return getElSpecification$;
        }

        return rxjs.Observable.of(elSpecification);
    };

    this.getFlowId = function () {
        return flowId;
    };

    this.setFlowId = function (id) {
        flowId = id;
    };
};

EfmFlowProvider.prototype = {
    constructor: EfmFlowProvider
};

EfmFlowProvider.parameters = [
    EfmApi,
    'ErrorResponse'
];

module.exports = EfmFlowProvider;
