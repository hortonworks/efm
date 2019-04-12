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
var covalentCore = require('@covalent/core');
var rxjs = require('rxjs/Subject');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * FlowDesignerExtensionCreationComponent.
 *
 * @param commonService                         The common service.
 * @param tdDataTableService                    The covalent data table service module.
 * @constructor
 */
function FlowDesignerExtensionCreationComponent(commonService, tdDataTableService) {
    var self = this;

    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();
    this.filteredExtensions = [];
    this.extensions = null;
    this.selectedExtension = null;
    this.extensionSearchTerms = [];

    // data table
    this.activeColumn = null;
    this.extensionColumns = [
        {
            name: 'displayType',
            label: 'Type',
            sortable: true,
            tooltip: 'Extension Type',
            active: true,
            sortOrder: 'DESC',
            width: 35
        },
        {
            name: 'description',
            label: 'Description',
            sortable: false,
            tooltip: 'Description',
            width: 65
        }
    ];

    /**
     * Filters the extensions by column and order.
     *
     * @param sortBy
     * @param sortOrder
     */
    this.filterExtensions = function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC'
        }
        // if `sortBy` is `undefined` then find the first sortable column in this.extensionColumns
        if (sortBy === undefined) {
            var arrayLength = self.extensionColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (self.extensionColumns[i].sortable === true) {
                    sortBy = self.extensionColumns[i].name;
                    self.activeColumn = self.extensionColumns[i];
                    //only one column can be actively sorted so we reset all to inactive
                    self.extensionColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    self.extensionColumns[i].active = true;
                    self.extensionColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newData = self.extensions;

        for (var j = 0; j < newData.length; j++) {
            newData[j].displayType = commonService.substringAfterLast(newData[j].type, '.')
        }

        for (var i = 0; i < self.extensionSearchTerms.length; i++) {
            newData = tdDataTableService.filterData(newData, self.extensionSearchTerms[i], true, ['bundle', 'description', 'tags', 'type']);
        }

        newData = tdDataTableService.sortData(newData, sortBy, sortOrder);

        self.filteredExtensions = newData;
        return newData;
    };

    /**
     * Filters the extensions by column and order.
     *
     * @param searchTerm
     */
    this.autoFilterExtensions = function (searchTerm) {
        var newData = self.filterExtensions(self.activeColumn.sortOrder, self.activeColumn.name);
        newData = tdDataTableService.filterData(newData, searchTerm, true, ['bundle', 'description', 'tags', 'type']);
        newData = tdDataTableService.sortData(newData, self.activeColumn.name, self.activeColumn.sortOrder);
        self.filteredExtensions = newData;
    };

    /**
     * Configure this component.
     */
    this.init = function () {
        this.filterExtensions();
    };

    /**
     * Updates the sorted column.
     *
     * @param column column to toggle
     */
    this.sortExtensions = function (column) {
        if (column.sortable === true) {
            this.activeColumn = column;

            // toggle column sort order
            this.activeColumn.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';

            // filter
            this.filterExtensions(column.name, this.activeColumn.sortOrder);

            // only one column can be actively sorted so we reset all to inactive
            this.extensionColumns.forEach(function (c) {
                c.active = false;
            });

            // and set this column as the actively sorted column
            this.activeColumn.active = true;
        }
    };

    /**
     * Selects and extension for creation.
     *
     * @param extension
     */
    this.select = function (extension) {
        var arrayLength = this.filteredExtensions.length;
        for (var i = 0; i < arrayLength; i++) {
            this.filteredExtensions[i].checked = false;
        }
        extension.checked = true;
        this.selectedExtension = extension;
    };

    /**
     * Select processor to create.
     *
     * @param name
     */
    this.create = function (extension) {
        this.subject$
            .debug("FlowDesignerExtensionCreationComponent subject$ Next")
            .next(extension);
    };

    /**
     * Notify subscribers of the cancel processor configuration action.
     */
    this.cancel = function () {
        this.subject$
            .debug("FlowDesignerExtensionCreationComponent subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.extensionSearchTerms = [];
        this.filteredExtensions = [];
        this.extensions = null;
        this.selectedExtension = null;
        this.activeColumn = null;
        this.subject$
            .debug("FlowDesignerExtensionCreationComponent subject$ Complete")
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    };
};

FlowDesignerExtensionCreationComponent.prototype = {
    constructor: FlowDesignerExtensionCreationComponent,

    /**
     * Initialize the component
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

FlowDesignerExtensionCreationComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-extension-creation',
        template: require('./fd.extension-creation.component.html!text'),
        inputs: [
            'extensions',
            'extensionType'
        ]
    })
];

FlowDesignerExtensionCreationComponent.parameters = [
    CommonService,
    covalentCore.TdDataTableService
];

module.exports = FlowDesignerExtensionCreationComponent;
