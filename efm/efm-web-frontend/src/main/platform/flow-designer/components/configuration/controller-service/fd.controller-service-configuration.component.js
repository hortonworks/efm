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
var AddPropertyComponent = require('@flow-designer/components/flow-designer-add-property');
var ngMaterial = require('@angular/material');
var ConfigNotAppliedComponent = require('@flow-designer/components/flow-designer-configuration-not-applied');

/**
 * FlowDesignerControllerServiceConfigurationComponent.
 *
 * @param commonService                         The common service.
 * @param dialog                                Material dialog.
 * @constructor
 */
function FlowDesignerControllerServiceConfigurationComponent(commonService, dialog) {
    var self = this;
    var dialogRef = null;

    this.opened = false;
    this.subject$ = new rxjs.Subject();
    this.statusChangeSubscription = null;
    this.componentDestroyed$ = new rxjs.Subject();
    this.unchanged = null;
    
    this.name = null;
    this.componentListingTypeDisplayName = null;
    this.parentGroupId = null;
    this.descriptors = null;
    this.properties = null;
    this.comments = null;

    this.componentListingConfigurationElementRef = null;
    this.componentListingConfigurationPropertyTableComponent = null;
    this.componentListingConfigurationForm = null;
    
    this.componentEntity = null;
    this.flowDesignerCanvasComponent = null;
    this.flowDesignerApi = null;
    this.options = null;

    var marshalComponent = function () {
        var cs = self.componentEntity;

        cs.component.name = self.name;
        cs.component.comments = self.comments;
        cs.component.properties = self.properties;

        return cs;
    };

    /**
     * Determine if any processor settings have changed.
     *
     * @returns {boolean}
     */
    var isSaveRequired = function () {
        if (self.name !== self.componentEntity.component.name) {
            return true;
        }
        if (self.comments !== self.componentEntity.component.comments) {
            return true;
        }

        // defer to the property and relationship grids
        return self.componentListingConfigurationPropertyTableComponent.isSaveRequired();
    };

    /**
     * Configure this component.
     */
    this.init = function () {
        this.opened = true;
        this.componentListingTypeDisplayName = commonService.substringAfterLast(this.componentEntity.component.type, '.');
        this.name = this.componentEntity.component.name;
        this.comments = this.componentEntity.component.comments;
        this.descriptors = this.componentEntity.component.descriptors;
        this.properties = this.componentEntity.component.properties;
        this.parentGroupId = this.componentEntity.component.parentGroupId;

        // component listing configuration property table options
        this.options = {
            readOnly: false
        };

        this.statusChangeSubscription = this.componentListingConfigurationForm.statusChanges.subscribe(function () {
            self.unchanged = !isSaveRequired();
        });
    };

    /**
     * Determines if the processor supports dynamic properties.
     *
     * @returns {boolean}
     */
    this.supportsDynamicProperties = function () {
        return this.componentEntity.component.supportsDynamicProperties;
    };

    /**
     * Add a property to the property table.
     */
    this.addProperty = function () {
        var property = null;
        var propertyAdded = false;

        // commit any current edit
        this.componentListingConfigurationPropertyTableComponent.saveRow();

        // open the add property component
        dialogRef = dialog.open(AddPropertyComponent, {
            width: '30%'
        });

        // add a reaction to the dialog afterClosed to enable closing dialog with 'esc' or by clicking off of the dialog
        dialogRef.beforeClose()
            .takeUntil(dialogRef.componentInstance.componentDestroyed$)
            .subscribe(function () {
                // cancel add property
                dialogRef.componentInstance.cancel();
            });

        // after close edit the new property
        dialogRef.afterClosed()
            .subscribe(function () {
                // edit/select the new property if appropriate
                if (commonService.isDefinedAndNotNull(property)) {
                    if (propertyAdded) {
                        self.componentListingConfigurationPropertyTableComponent.edit(property);
                    } else {
                        self.componentListingConfigurationPropertyTableComponent.scrollToRow(property);
                    }
                }
            });

        // react to add a new property
        dialogRef.componentInstance.subject$
            .subscribe(function (propertyName) {
                self.flowDesignerApi.getControllerServicePropertyDescriptor(self.componentEntity, propertyName)
                    .subscribe(function (descriptor) {
                        // add the descriptor
                        propertyAdded = self.componentListingConfigurationPropertyTableComponent.addProperty(descriptor);

                        // record the newly added property
                        property = propertyName;

                        // complete the add property request
                        dialogRef.componentInstance.subject$.complete();
                    }, function (errorResponse) {
                    });
            }, function () {
                // close the dialog
                dialogRef.close();
            }, function () {
                // close the dialog
                dialogRef.close();
            });
    };

    /**
     * Update processor.
     */
    this.update = function () {
        this.subject$
            .debug("FlowDesignerComponentListingConfigurationComponent subject$ Next")
            .next(marshalComponent());
    };

    /**
     * Notify subscribers of the cancel processor configuration action.
     *
     * @param skipConfigNotAppliedCheck optionally skips the check for possible loss of unapplied changes.
     */
    this.cancel = function (skipConfigNotAppliedCheck) {
        if (!skipConfigNotAppliedCheck && !this.isUnchanged()) {
            // show the ConfigNotAppliedComponent in a dialog
            dialogRef = dialog.open(ConfigNotAppliedComponent, {
                width: '50%',
                data: { applyEnabled: !this.isUnchanged() && !this.isInvalid() }
            });

            dialogRef.componentInstance.dialogRef = dialogRef;

            dialogRef.afterClosed()
                .subscribe(function (response) {
                    switch (response) {
                        case 'CANCEL CHANGES':
                            self.subject$.error();
                            break;
                        case 'APPLY':
                            self.update();
                            break;
                        default:
                            //Do nothing and allow the user to continue editing the configuration
                            break;
                    }
                });

            return;
        }

        this.subject$
            .debug("FlowDesignerComponentListingConfigurationComponent subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        if(commonService.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
        this.statusChangeSubscription.unsubscribe();
        this.subject$
            .debug("FlowDesignerComponentListingConfigurationComponent subject$ Complete")
            .complete();
        this.componentListingConfigurationElementRef = null;
        this.componentListingConfigurationPropertyTableComponent = null;
        this.componentListingConfigurationForm = null;
        this.componentEntity = null;
        this.flowDesignerCanvasComponent = null;
        this.flowDesignerApi = null;
        this.options = null;
        this.name = null;
        this.comments = null;
        this.componentListingTypeDisplayName = null;
        this.parentGroupId = null;
        this.descriptors = null;
        this.properties = null;
        this.unchanged = null;
        this.opened = false;
        this.componentDestroyed$.next();
        this.componentDestroyed$.unsubscribe();
    };

    /**
     * Get the type of the component.
     *
     * @returns {string}
     */
    this.getType = function () {
        return 'controller-service';
    };

    /**
     * Get the id of the component.
     *
     * @returns {*}
     */
    this.getId = function () {
        if (!commonService.isUndefined(this.componentEntity)) {
            return this.componentEntity.id;
        }
        return null;
    };

    /**
     * Get the parent group id that this component belongs to.
     * @returns {*}
     */
    this.getParentGroupId = function () {
        if (!commonService.isUndefined(this.componentEntity) && !commonService.isUndefined(this.componentEntity.component)) {
            return this.componentEntity.component.parentGroupId;
        }
        return null;
    };

    /**
     * Whether or not the configuration has changed
     *
     * @returns {boolean}
     */
    this.isUnchanged = function () {
        return this.unchanged;
    };

    /**
     * Whether or not the configuration is valid
     *
     * @returns {boolean}
     */
    this.isInvalid = function () {
        return this.componentListingConfigurationForm.invalid;
    };
};

FlowDesignerControllerServiceConfigurationComponent.prototype = {
    constructor: FlowDesignerControllerServiceConfigurationComponent,

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

FlowDesignerControllerServiceConfigurationComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-controller-service-configuration',
        template: require('./fd.controller-service-configuration.component.html!text'),
        inputs: [
            'componentEntity',
            'flowDesignerCanvasComponent',
            'flowDesignerApi'
        ],
        queries: {
            componentListingConfigurationElementRef: new ngCore.ViewChild('componentListingConfigurationElementRef'),
            componentListingConfigurationPropertyTableComponent: new ngCore.ViewChild('componentListingConfigurationPropertyTableComponent'),
            componentListingConfigurationForm: new ngCore.ViewChild('componentListingConfigurationForm')
        }
    })
];

FlowDesignerControllerServiceConfigurationComponent.parameters = [
    CommonService,
    ngMaterial.MatDialog
];

module.exports = FlowDesignerControllerServiceConfigurationComponent;
