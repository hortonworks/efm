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
var rxjs = require('rxjs/Subject');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * FlowDesignerAddPropertyComponent.
 *
 * @param commonService                         The common service.
 * @constructor
 */
function FlowDesignerAddPropertyComponent(commonService) {
    var self = this;

    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();

    this.propertyName = null;

    var marshalProperty = function () {
        return self.propertyName;
    };

    /**
     * Creates the configured connection.
     *
     * @param name
     */
    this.create = function () {
        this.subject$
            .debug("FlowDesignerAddPropertyComponent subject$ Next")
            .next(marshalProperty());
    };

    /**
     * Notify subscribers of the cancel processor configuration action.
     */
    this.cancel = function () {
        this.subject$
            .debug("FlowDesignerAddPropertyComponent subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.subject$
            .debug("FlowDesignerAddPropertyComponent subject$ Complete")
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    };
};

FlowDesignerAddPropertyComponent.prototype = {
    constructor: FlowDesignerAddPropertyComponent,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

FlowDesignerAddPropertyComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-add-property',
        template: require('./fd.add-property.component.html!text')
    })
];

FlowDesignerAddPropertyComponent.parameters = [
    CommonService
];

module.exports = FlowDesignerAddPropertyComponent;
