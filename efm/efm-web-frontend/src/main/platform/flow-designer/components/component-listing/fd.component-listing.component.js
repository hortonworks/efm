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

var $ = require('jquery');
var rxjs = require('rxjs/Subject');
var ngCore = require('@angular/core');
var ngMaterial = require('@angular/material');
var covalentCore = require('@covalent/core');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var fdsAnimations = require('@flow-design-system/common/animations');
var CommonService = require('@flow-designer/services/CommonService');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var ExtensionCreationComponent = require('@flow-designer/components/flow-designer-extension-creation');

// TODO: update the animation on the component listing route exit
// var ngAnimate = require('@angular/animations');
// var flyInOutAnimation = ngAnimate.trigger('flyInOut', [
//     ngAnimate.state('in',
//         ngAnimate.style({transform: 'translateX(0)'})
//     ),
//     ngAnimate.transition('void => *', [
//         ngAnimate.style({transform: 'translateX(100%)'}),
//         ngAnimate.animate('0.4s 0.1s ease-in')
//     ]),
//     ngAnimate.transition('* => void', [
//         ngAnimate.style({transform: 'translateX(-100%)'}),
//         ngAnimate.animate('0.4s')
//     ])
//     // ngAnimate.transition('* => void', ngAnimate.animate('0.4s ease-out', ngAnimate.style({transform: 'translateX(-100%)'})))
// ])


/**
 * FlowDesignerComponentListingComponent.
 *
 * @param tdDataTableService                        The covalent data table service module.
 * @param dialog                                    The angular material dialog.
 * @param fdsDialogService                          The FDS dialog service module.
 * @param common                                    The common module.
 * @param canvasUtilsService                        The canvas utils service.
 * @constructor
 */
function FlowDesignerComponentListingComponent(tdDataTableService, dialog, fdsDialogService, common, canvasUtilsService) {
    var self = this;
    var dialogRef = null;

    this.opened = false;
    this.entity = null;
    this.title = null;
    this.flowDesignerApi = null;
    this.client = null;
    this.componentDestroyed$ = new rxjs.Subject();

    // data table
    this.componentColumns = [
        {
            name: 'name',
            label: 'Name',
            sortable: true,
            tooltip: 'Name'
        }
    ];
    this.componentsSearchTerms = [];
    this.activeColumn = this.componentColumns[0];
    this.filteredComponents = null;

    // Subjects
    this.subject$ = new rxjs.Subject();
    this.componentConfiguration$ = new rxjs.Subject();
    this.componentConfigurationComplete$ = new rxjs.Subject();

    /**
     * Configure this component.
     */
    this.init = function () {
        this.opened = true;
        this.sortComponents(this.activeColumn, 'ASC');
    };

    /**
     * Delete the extension.
     *
     * @param extensionEntity       The extension to delete.
     */
    this.remove = function (extensionEntity) {
        dialogRef = fdsDialogService.openConfirm({
            title: 'Delete Service',
            message: 'Flow components using this service will no longer be able to reference it.',
            acceptButton: 'Delete',
            acceptButtonColor: 'fds-warn',
            cancelButton: 'Cancel',
            cancelButtonColor: 'fds-regular'
        });

        dialogRef
        .afterClosed()
        .toPromise()
        .then(function (confirm) {
            if (confirm) {
                self.flowDesignerApi.deleteControllerService($.extend({
                    'revision': self.client.getRevision(extensionEntity)
                }, extensionEntity))
                    .subscribe(function () {
                        self.flowDesignerApi.getProcessGroupControllerServices(self.entity.id)
                            .subscribe(function (response) {
                                // emit the latest process group entity
                                self.subject$.next($.extend({}, self.entity, {'controllerServices': response}));
                            }, function (errorResponse) {
                            });
                    }, function (errorResponse) {
                    });
            }
        });
    };

    /**
     * Shows the tooltip for the icon specified in the specified event.
     *
     * @param event             mouse event
     */
    this.showTip = function (event) {
        var target = event.target;
        var tip = $(target).next('.tooltip').get(0);
        var container = $(target).closest('.flow-designer-component-listing-list').get(0);
        canvasUtilsService.showCanvasTooltip(tip, target, container);
    };

    /**
     * Hides the tooltip for the icon specified in the specified event.
     *
     * @param event             mouse event
     */
    this.hideTip = function (event) {
        var target = event.target;
        canvasUtilsService.hideCanvasTooltip($(target).next('.tooltip').get(0));
    };

    /**
     * Launches UX to add a component to the PG
     */
    this.extensionCreation = function () {
        this.flowDesignerApi.getControllerServiceExtensions().subscribe(function (extensions) {
            if (common.isEmpty(extensions)) {
                dialogRef = fdsDialogService.openConfirm({
                    title: 'Create Service',
                    message: 'No Service extensions are available.'
                });
            } else {
                dialogRef = dialog.open(ExtensionCreationComponent, {
                    width: '75%'
                });

                dialogRef.componentInstance.extensions = extensions;
                dialogRef.componentInstance.extensionType = "Service";

                // add a reaction to the dialog afterClosed to enable closing dialog with 'esc' or by clicking off of the dialog
                dialogRef.afterClosed()
                    .subscribe(function () {
                        // cancel component addition
                        dialogRef.componentInstance.cancel();
                    });

                // react to request to create a process group component
                dialogRef.componentInstance.subject$
                    .subscribe(function (extensionDescriptor) {
                        // create
                        self.flowDesignerApi.createControllerService(self.entity.id, extensionDescriptor)
                            .subscribe(function () {
                                self.flowDesignerApi.getProcessGroupControllerServices(self.entity.id)
                                    .subscribe(function (response) {
                                        // emit the latest process group entity
                                        self.subject$.next($.extend({}, self.entity, {'controllerServices': response}));
                                        dialogRef.close();
                                    }, function (errorResponse) {
                                    });
                            }, function (errorResponse) {
                                if (errorResponse.preventDefault) {
                                    dialogRef.close();
                                }
                            });
                    }, function (error) {
                        // close the dialog
                        dialogRef.close();
                    }, function () {
                        // close the dialog
                        dialogRef.close();
                    });
            }
        }, function (errorResponse) {
        });
    };

    /**
     * Close this component.
     */
    this.close = function () {
        this.subject$
            .debug("FlowDesignerComponentListingComponent subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy this component.
     */
    this.destroy = function () {
        if(common.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
        this.opened = false;
        this.title = null;
        this.entity = null;
        this.flowDesignerApi = null;
        this.client = null;
        this.componentsSearchTerms = [];
        this.activeColumn = this.componentColumns[0];
        this.filteredComponents = null;
        this.componentConfiguration$
            .debug("FlowDesignerComponentListingComponent componentConfiguration$ Complete")
            .complete();
        this.componentConfigurationComplete$
            .debug("FlowDesignerComponentListingComponent componentConfigurationComplete$ Complete")
            .complete();
        this.subject$
            .debug("FlowDesignerComponentListingComponent subject$ Complete")
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.unsubscribe();
    };

    /**
     * Updates the sorted column.
     *
     * @param column column to toggle
     */
    this.sortComponents = function (column, sortOrder) {
        if (column.sortable === true) {
            this.activeColumn = column;
            this.activeColumn.sortOrder = sortOrder;

            this.filterComponents(column.name, sortOrder);

            // only one column can be actively sorted so we reset all to inactive
            this.componentColumns.forEach(function (c) {
                c.active = false;
            });

            // and set this column as the actively sorted column
            this.activeColumn.active = true;
        }
    };

    this.filterComponents = function (sortBy, sortOrder) {
        // if `sortOrder` is `undefined` then use 'ASC'
        if (sortOrder === undefined) {
            sortOrder = 'ASC';
        }
        // if `sortBy` is `undefined` then find the first sortable column in this.componentColumns
        if (sortBy === undefined) {
            var arrayLength = this.componentColumns.length;
            for (var i = 0; i < arrayLength; i++) {
                if (this.componentColumns[i].sortable) {
                    sortBy = this.componentColumns[i].name;
                    this.activeColumn = this.componentColumns[i];
                    //only one column can be actively sorted so we reset all to inactive
                    this.componentColumns.forEach(function (c) {
                        c.active = false;
                    });
                    //and set this column as the actively sorted column
                    this.componentColumns[i].active = true;
                    this.componentColumns[i].sortOrder = sortOrder;
                    break;
                }
            }
        }

        var newData = [];

        // promote the name property for sorting
        for (var i = 0; i < self.entity.controllerServices.length; i++) {
            newData.push(self.entity.controllerServices[i]);
            newData[i].name = self.entity.controllerServices[i].component.name;
            newData[i].validationErrors = self.entity.controllerServices[i].component.validationErrors;
        }

        for (var i = 0; i < this.componentsSearchTerms.length; i++) {
            newData = tdDataTableService.filterData(newData, this.componentsSearchTerms[i], true, this.activeColumn.name);
        }

        newData = tdDataTableService.sortData(newData, sortBy, sortOrder);

        this.filteredComponents = newData;
    };

    /**
     * Execute the given component action.
     *
     * @param action                            The action object.
     * @param extensionEntity                   The extension object the `action` will act upon.
     */
    this.executeComponentAction = function (action, extensionEntity) {
        switch (action.name.toLowerCase()) {
            case 'configure':
                self.componentConfiguration$.next(extensionEntity);
                break;
            case 'delete':
                this.remove(extensionEntity);
                break;
            default:
                break;
        }
    };
}

FlowDesignerComponentListingComponent.prototype = {
    constructor: FlowDesignerComponentListingComponent,

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

FlowDesignerComponentListingComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-component-listing',
        template: require('./fd.component-listing.component.html!text'),
        inputs: [
            'entity',
            'flowDesignerCanvasComponent',
            'flowDesignerApi'
        ],
        queries: {
            componentListingElementRef: new ngCore.ViewChild('componentListingElementRef')
        },
        animations: [fdsAnimations.flyInOutAnimation]
    })
];

FlowDesignerComponentListingComponent.parameters = [
    covalentCore.TdDataTableService,
    ngMaterial.MatDialog,
    fdsDialogsModule.FdsDialogService,
    CommonService,
    CanvasUtilsService
];

module.exports = FlowDesignerComponentListingComponent;
