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

var covalentCore = require('@covalent/core');
var DialogService = require('services/efm.dialog.service.js');
var ngCommon = require('@angular/common');
var ngRouter = require('@angular/router');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * EfmService constructor.
 *
 * @param common                The flow designer common module.
 * @param tdDataTableService    The covalent data table service module.
 * @param location              The angular location service.
 * @param router                The angular router service.
 * @param dialogService         The dialog service.
 * @constructor
 */
function EfmService(common, tdDataTableService, location, router, dialogService) {
    // Services
    this.common = common;
    this.dialogService = dialogService;
    this.location = location;
    this.router = router;
    this.dataTableService = tdDataTableService;

    // General
    this.title = "EFM";
    this.perspective = '';
    this.agentClass = '';
    this.flowDesignerCanvasComponent = null;
    this.componentListingHost = null;
    this.sidenav = null;
    this.sidenavHost = null;
};

EfmService.prototype = {
    constructor: EfmService,

    _experimental_filterData: function (data, searchTerm, ignoreCase) {
        var field = '';
        if (searchTerm.indexOf(":") > -1) {
            field = searchTerm.split(':')[0].trim();
            searchTerm = searchTerm.split(':')[1].trim();
        }
        var filter = searchTerm ? (ignoreCase ? searchTerm.toLowerCase() : searchTerm) : '';

        if (filter) {
            data = data.filter(function (item) {
                var res = Object.keys(item).find(function (key) {
                    if (field.indexOf(".") > -1) {
                        var objArray = field.split(".");
                        var obj = item;
                        var arrayLength = objArray.length;
                        for (var i = 0; i < arrayLength; i++) {
                            try {
                                obj = obj[objArray[i]];
                            } catch (e) {
                                return false;
                            }
                        }
                        var preItemValue = ('' + obj);
                        var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                        return itemValue.indexOf(filter) > -1;
                    } else {
                        if (key !== field && field !== '') {
                            return false;
                        }
                        var preItemValue = ('' + item[key]);
                        var itemValue = ignoreCase ? preItemValue.toLowerCase() : preItemValue;
                        return itemValue.indexOf(filter) > -1;
                    }
                });
                return !(typeof res === 'undefined');
            });
        }
        return data;
    },

    /**
     * Resynchronize the angular router
     *
     * @param message       Message to display to user in a dialog.
     * @param url           The url to sync the angular router with.
     */
    resyncRouter: function (title, message, url, queryParams) {
        var self = this;

        // ISSUE #5268
        // https://github.com/angular/material2/issues/5268
        // https://stackoverflow.com/questions/51629756/mat-dialog-error-in-angular-component-expressionchangedafterithasbeencheckederro
        setTimeout(function () {
            if (self.common.isDefinedAndNotNull(message)) {
                self.dialogService.openConfirm({
                    title: title,
                    message: message
                });
            }

            // resync the router
            var urlTree = self.router.createUrlTree([url], {queryParams: queryParams});
            self.router.navigateByUrl(urlTree, {skipLocationChange: true});
        });
    },

    /**
     * Route the application to /flow-designer
     *
     * @param flowId                    The flow id.
     * @param replaceState              The replace location.
     * @param message                   The error message to display.
     */
    routeToFlowDesigner: function (flowId, replaceState, message) {
        var self = this;

        if (replaceState === true) {
            // remove the invalid URL from the browser history
            self.location.replaceState('/flow-designer/flow/' + flowId);

            // resync the router
            self.resyncRouter('Invalid URL', message, '/flow-designer/flow/' + flowId);
        } else {
            self.router.navigateByUrl('/flow-designer/flow/' + flowId);
        }
    },

    /**
     * Routes to open a flow.
     */
    routeToOpenFlow: function () {
        this.router.navigateByUrl('/flow-designer/open');
    },

    /**
     * Routes to the specified flow.
     *
     * @param flowId
     */
    routeToFlow: function (flowId) {
        this.router.navigateByUrl('/flow-designer/flow/' + flowId);
    },

    /**
     * Route the application to /flow-designer/flow/:flowId/process-groups/:id/component-listing/controller-service/:componentId/configuration
     *
     * @param flowId                                                The flow id.
     * @param id                                                    The process group id.
     * @param componentId                                           The controller service id.
     * @param message                                               The error message to display.
     */
    routeToProcessGroupControllerServiceConfiguration: function (flowId, id, componentId, message) {
        var self = this;

        if (self.common.isDefinedAndNotNull(message)) {
            // remove the invalid URL from the browser history
            self.location.replaceState('/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing/controller-service/' + componentId + '/configuration');

            // resync the router
            self.resyncRouter('Invalid URL', message, '/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing/controller-service/' + componentId + '/configuration');
        } else {
            self.router.navigateByUrl('/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing/controller-service/' + componentId + '/configuration');
        }
    },

    /**
     * Route the application to /flow-designer/flow/:flowId/process-groups/:id/component-listing
     *
     * @param flowId                                                The flow id.
     * @param id                                                    The process group id.
     * @param replaceState                                          The replace location.
     * @param message                                               The error message to display.
     */
    routeToProcessGroupComponentListing: function (flowId, id, replaceState, message) {
        var self = this;

        if (replaceState === true) {
            // remove the invalid URL from the browser history
            self.location.replaceState('/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing');

            // resync the router
            self.resyncRouter('Invalid URL', message, '/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing');
        } else {
            self.router.navigateByUrl('/flow-designer/flow/' + flowId + '/process-groups/' + id + '/component-listing');
        }
    },

    /**
     * Route the application to /flow-designer/flow/:flowId/:type/:id/configuration
     *
     * @parm flowId                                                 The flow id
     * @param id                                                    The component id.
     * @param type                                                  The component type (valid type strings: 'remote-process-group', 'processor', 'connection', 'funnel').
     * @param message                                               The error message to display.
     */
    routeToConfiguration: function (flowId, id, type, message) {
        var self = this;

        if (self.common.isDefinedAndNotNull(message)) {
            // remove the invalid URL from the browser history
            self.location.replaceState('/flow-designer/flow/' + flowId + '/' + type + '/' + id + '/configuration');

            // resync the router
            self.resyncRouter('Invalid URL', message, '/flow-designer/flow/' + flowId + '/' + type + '/' + id + '/configuration');
        } else {
            self.router.navigateByUrl('/flow-designer/flow/' + flowId + '/' + type + '/' + id + '/configuration');
        }
    },

    /**
     * Route the application to /flow-designer/flow/:flowId/:type/:id
     *
     * @parm flowId                                                 The flow id
     * @param id                                                    The component id.
     * @param type                                                  The component type.
     * @param message                                               The error message to display.
     */
    routeToFlowDesignerCanvasComponent: function (flowId, id, type, message) {
        var self = this;

        if (self.common.isDefinedAndNotNull(message)) {
            // remove the invalid URL from the browser history
            self.location.replaceState('/flow-designer/flow/' + flowId + '/' + type + '/' + id);

            // resync the router
            self.resyncRouter('Invalid URL', message, '/flow-designer/flow/' + flowId + '/' + type + '/' + id);

        } else {
            self.router.navigateByUrl('/flow-designer/flow/' + flowId + '/' + type + '/' + id);
        }
    },

    /**
     * Unload the sidenavHost
     */
    unloadSidenavHost: function () {
        var self = this;

        // close the sidenav
        self.sidenav.close();

        // unload component
        self.sidenavHost.unloadComponent();
    },

    /**
     * Unload the componentListingHost
     */
    unloadComponentListingHost: function () {
        var self = this;

        // unload component
        self.componentListingHost.unloadComponent();
    },

    /**
     * Route the application to `url`
     *
     * @param url               The route.
     * @param queryParams       The query params.
     * @param options           Optional router options.
     */
    routeToUrl: function (url, routerOptions) {
        this.router.navigate(url, routerOptions);
    }
};

EfmService.parameters = [
    CommonService,
    covalentCore.TdDataTableService,
    ngCommon.Location,
    ngRouter.Router,
    DialogService
];

module.exports = EfmService;
