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

var DialogService = require('services/efm.dialog.service.js');
var EfmService = require('services/efm.service.js');
var ngCommon = require('@angular/common');
var rxjs = require('rxjs/Observable');
var ngRouter = require('@angular/router');

/**
 * EfmFlowDesignerConfigurationCanDeactivate constructor.
 *
 * @param dialogService             The DialogService
 * @param efmService                The efm singleton service.
 * @param router                    The angular router.
 * @param location                  The angular location service.
 * @constructor
 */
function EfmFlowDesignerConfigurationCanDeactivate (dialogService, efmService, router, location) {
    var self = this;

    /**
     * canDeactivate route guard impl
     *
     * @param component
     * @param currentRoute
     * @param currentState
     * @param nextState
     * @returns {boolean}
     */
    this.canDeactivate = function (component, currentRoute, currentState, nextState) {

        var sidenavComponent = efmService.sidenavHost.component;

        if (sidenavComponent.isUnchanged()) {
            return rxjs.Observable.of(true);
        }

        var dialogRef = dialogService.openConfigurationNotApplied({ applyEnabled: !sidenavComponent.isUnchanged() && !sidenavComponent.isInvalid() });

        var observable = dialogRef.afterClosed();
        observable.subscribe(function(result) {
            var deactivateAllowed = false;
            switch(result) {
                case 'CANCEL CHANGES':
                    // cancel it, but skip a second check for configuration not applied in the component's cancel method
                    sidenavComponent.cancel(true);
                    deactivateAllowed = true;
                    break;
                case 'APPLY':
                    sidenavComponent.update();
                    deactivateAllowed = true;
                    break;
                default:
                    // at this point the browser history has been popped off the stack so we need to put it back
                    var currentUrlTree = router.createUrlTree([], currentRoute);
                    var currentUrl = currentUrlTree.toString();
                    location.go(currentUrl);
                    break;
            }
            return rxjs.Observable.of(deactivateAllowed);
        });

        // return the Observable for afterClosed, it will have the boolean result to determine if it should be closed or not
        return observable;
    }
}

EfmFlowDesignerConfigurationCanDeactivate.proptotype = {
    constructor: EfmFlowDesignerConfigurationCanDeactivate
};

EfmFlowDesignerConfigurationCanDeactivate.parameters = [
    DialogService,
    EfmService,
    ngRouter.Router,
    ngCommon.Location
];

module.exports = EfmFlowDesignerConfigurationCanDeactivate;
