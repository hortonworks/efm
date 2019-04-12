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

var ngCommonHttp = require('@angular/common/http');
var DialogService = require('services/efm.dialog.service.js');
var rxjs = require('rxjs/Observable');

var MOCK_CLASS_DETAIL = require('mock/class-detail.js');
var MOCK_AGENTS = require('mock/agents.js');
var MOCK_SERVER = require('mock/server.js');
var MOCK_OVERVIEW = require('mock/overview.js');

var URLS = require('config/urls.js');

/**
 * EfmApi constructor.
 *
 * @param http                  The angular http module.
 * @constructor
 */
function EfmApi(http, dialogService) {
    this.http = http;
    this.dialogService = dialogService;
}

EfmApi.prototype = (function () {
    /**
     * Map response from the event search endpoint.
     *
     * @param response      the response from the search endpoint
     * @returns {object}    the mapped events
     */
    var mapEvents = function (response) {
        var events = response.elements.map(function (event) {
            var eventSource = event.eventSource || {};

            var mappedFields = {
                eventSourceType: eventSource.type || '',
                eventSourceId: eventSource.id,
                created: new Date(event.created)
            };

            return $.extend({}, event, mappedFields);
        });

        return {
            pageNum: response.page.number,
            rows: response.page.size,
            totalRows: response.page.totalElements,
            events: events,
            newEventsUrlPath: response.links.new.href
        };
    };

    return {
        constructor: EfmApi,

        /**
         * Get event detail.
         *
         * @param {string}  url      The url to GET
         * @returns {Observable}
         */
        getEventDetail: function (url) {
            var self = this;
            return this.http.get(url)
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Get Event Detail',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                })
                .shareReplay();
        },

        /**
         * Temporary method to return a flow id.
         *
         * @param {string}  flowId      The flow id
         * @returns {Observable}
         */
        getFlow: function (flowId) {
            var self = this;
            return this.http.get(URLS.DESIGNER.FLOWS + '/' + flowId)
                .map(function (response) {
                    return response.flowMetadata;
                })
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Get Flow',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                })
                .shareReplay();
        },

        /**
         * Get the el specification for the given flow id.
         *
         * @param flowId
         * @returns {Observable}
         */
        getElSpecification: function (flowId) {
            var self = this;
            return this.http.get(URLS.DESIGNER.FLOWS + '/' + flowId + '/expression-language-spec')
                .map(function (response) {
                    return {
                        flowIdentifier: flowId,
                        spec: response
                    };
                })
                .shareReplay();
        },

        /**
         * Returns the available flows.
         *
         * @returns {Observable}
         */
        getFlowSummaries: function () {
            var self = this;

            return this.http.get(URLS.DESIGNER.FLOWS + '/summaries')
                .map(function (response) {
                    return response.elements;
                })
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Get Flow Summaries',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                });
        },

        /**
         * Publishes the specified flow.
         *
         * @param {string}  [flowId] The flow id to publish
         * @returns {Observable}
         */
        publishFlow: function (flowId, comments) {
            var self = this;
            var publishEntity = {
                'comments': comments
            };

            return this.http.post(URLS.DESIGNER.FLOWS + '/' + flowId + '/publish', publishEntity)
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Publish Flow',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                });
        },

        /**
         * Reverts the specified flow.
         *
         * @param {string}  [flowId] The flow id to revert
         * @returns {Observable}
         */
        revertFlow: function (flowId) {
            var self = this;

            return this.http.post(URLS.DESIGNER.FLOWS + '/' + flowId + '/revert')
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Revert Flow',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                });
        },

        /**
         * Retrieves the agent information.
         *
         * @returns {*}
         */
        getInstances: function () {
            return rxjs.Observable.of(MOCK_OVERVIEW);
        },

        /**
         * Retrieves the instance information.
         *
         * @param {string} [instanceId] Defines a instance id for filtering results.
         * @returns {*}
         */
        getInstance: function () {
            return rxjs.Observable.of(MOCK_AGENTS[0]);
        },

        /**
         * Retrieves events from the specific link from an events response.
         *
         * @param urlPath           The path for the event search
         * @returns {Observable}    The resulting events
         */
        getEventsFromResponseLink: function (urlPath) {
            var self = this;

            return self.http.get(URLS.API +  '/' + urlPath)
                .map(mapEvents)
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Get Events',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                });
        },

        /**
         * Retrieves the agent and server events.
         *
         * @returns {*}
         */
        getEvents: function (params) {
            var self = this;

            var queryParams = {};
            if (typeof params !== 'undefined' && params !== null) {
                queryParams.params = $.extend({}, params, { pageNum: params.pageNum - 1 });
            }

            return self.http.get(URLS.EVENTS, queryParams)
                .map(mapEvents)
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        self.dialogService.openConfirm({
                            title: 'Get Events',
                            message: errorResponse.message
                        });
                    }

                    return rxjs.Observable.throw(errorResponse);
                });
        },

        /**
         * Retrieves the server information.
         *
         * @param {string} [serverIdentifier] Defines a server id for filtering results.
         * @returns {*}
         */
        getServer: function () {
            return rxjs.Observable.of(MOCK_SERVER);
        },

        /**
         * Retrieves the class information.
         *
         * @param {string} [classId] Defines a class id for filtering results.
         * @returns {*}
         */
        getClass: function () {
            return rxjs.Observable.of(MOCK_CLASS_DETAIL);
        }
    };
}());

EfmApi.parameters = [
    ngCommonHttp.HttpClient,
    DialogService
];

module.exports = EfmApi;
