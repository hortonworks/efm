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
var ngForms = require('@angular/forms');
var propertyTableFactory = require('@flow-designer/services/PropertyTableFactory');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var CommonService = require('@flow-designer/services/CommonService');
var UniversalCaptureService = require('@flow-designer/services/UniversalCaptureService');
var fdsDialogsModule = require('@flow-design-system/dialogs');

/**
 * FlowDesignerPropertyTableComponent.
 *
 * @param PropertyTable                         The PropertyTable.
 * @param commonService                         The common service.
 * @param ngZone                                The service for executing work inside or outside of the Angular zone.
 * @constructor
 */
function FlowDesignerPropertyTableComponent(PropertyTable, commonService, ngZone) {
    var self = this;
    var propertyTable = new PropertyTable();

    this.commonService = commonService;
    this.ngZone = ngZone;
    this.propertyTableElementRef = null;
    this.parentGroupId = null;
    this.descriptors = null;
    this.properties = null;
    this.flowDesignerCanvasComponent = null;
    this.flowDesignerApi = null;

    /**
     * Function to call when the property table changes.
     *
     * @param rating
     * @returns {{}}
     */
    this.onChange = null;

    /**
     * Initialize the component
     *
     * format:
     *
     * {
     *   readOnly: true
     * }
     */
    this.init = function (flowDesignerPropertyTableComponentRef) {
        propertyTable.init(flowDesignerPropertyTableComponentRef);
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        // close all fields currently being edited
        propertyTable.saveRow();
        // destroy this property table
        propertyTable.destroy();
        this.propertyTableElementRef = null;
        this.parentGroupId = null;
        this.descriptors = null;
        this.properties = null;
        this.flowDesignerCanvasComponent = null;
        this.flowDesignerApi = null;
    };

    /**
     * Writes a new value from the form model into the view or (if needed) DOM property.
     *
     * @param properties   The properties for this property table component
     */
    this.writeValue = function (properties) {
        if (this.commonService.isDefinedAndNotNull(properties)) {
            this.properties = properties;
            propertyTable.setValue(properties);
        }
    };

    /**
     * Returns whether a save is required.
     *
     * @returns {boolean}
     */
    this.isSaveRequired = function () {
        return propertyTable.isSaveRequired();
    };

    /**
     * Resets the table size.
     */
    this.resetTableSize = function () {
        return propertyTable.resetTableSize();
    };

    /**
     * Saves the last edited row.
     */
    this.saveRow = function () {
        propertyTable.saveRow();
    };

    /**
     * Edit the specified property.
     *
     * @param propertyName
     */
    this.edit = function (propertyName) {
        propertyTable.edit(propertyName);
    };

    /**
     * Scrolls to the specified row. If the property does not exist, this
     * function does nothing.
     *
     * @param propertyName      The property to scroll into view
     */
    this.scrollToRow = function (propertyName) {
        propertyTable.scrollToRow(propertyName);
    };

    /**
     * Add a new property to this property table. Returns true if the new property was
     * added, false if the property already exists.
     *
     * @param descriptor    The property descriptor
     * @returns             Whether the new property was successfully added
     */
    this.addProperty = function (descriptor) {
        return propertyTable.addProperty(descriptor);
    };
};

FlowDesignerPropertyTableComponent.prototype = {
    constructor: FlowDesignerPropertyTableComponent,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        this.init(this);
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    },

    /**
     * Registers a handler that should be called when something in the view has changed.
     *
     * @param fn
     */
    registerOnChange: function (fn) {
        this.onChange = fn;
    },

    /**
     * Registers a handler specifically for when the property table receives a touch event
     *
     * @param fn
     */
    registerOnTouched: function (fn) {
        // Do nothing
    }
};

FlowDesignerPropertyTableComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-property-table',
        template: require('./fd.property-table.control-value-accessor.component.html!text'),
        inputs: [
            'parentGroupId',
            'descriptors',
            'properties',
            'options',
            'flowDesignerCanvasComponent',
            'flowDesignerApi'
        ],
        queries: {
            propertyTableElementRef: new ngCore.ViewChild('propertyTableElementRef')
        },
        // Each instance of this component gets its own instance of the following services
        providers: [
            {
                provide: 'PropertyTable',
                useFactory: propertyTableFactory.build(),
                deps: [
                    CanvasUtilsService,
                    CommonService,
                    fdsDialogsModule.FdsDialogService,
                    UniversalCaptureService
                ]
            },
            {
                provide: ngForms.NG_VALUE_ACCESSOR,
                useExisting: ngCore.forwardRef(function () {
                    return FlowDesignerPropertyTableComponent;
                }),
                multi: true
            }
        ]
    })
];

FlowDesignerPropertyTableComponent.parameters = [
    'PropertyTable',
    CommonService,
    ngCore.NgZone
];

module.exports = FlowDesignerPropertyTableComponent;
