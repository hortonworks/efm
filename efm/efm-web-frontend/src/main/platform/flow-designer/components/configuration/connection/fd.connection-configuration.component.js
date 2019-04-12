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
var ngCore = require('@angular/core');
var rxjs = require('rxjs/Subject');
var CommonService = require('@flow-designer/services/CommonService');
var ngMaterial = require('@angular/material');
var ConfigNotAppliedComponent = require('@flow-designer/components/flow-designer-configuration-not-applied');

/**
 * FlowDesignerConnectionConfigurationComponent.
 *
 * @param commonService                         The common service.
 * @param dialog                                The Angular Material dialog.
 * @constructor
 */
function FlowDesignerConnectionConfigurationComponent(commonService, dialog) {
    var self = this;

    var marshalConnection = function () {
        // get the connection source and destination
        var source = self.connectionDefinitionComponent.getSource();
        var destination = self.connectionDefinitionComponent.getDestination();

        var connectionEntity = {
            'id': self.connectionEntity.id,
            'component': {
                'destination': destination,
                'name': self.name,
                'flowFileExpiration': self.flowFileExpiration,
                'backPressureObjectThreshold': self.backPressureObjectThreshold,
                'backPressureDataSizeThreshold': self.backPressureDataSizeThreshold
            }
        };

        // add the selected relationships if appropriate
        if (source.type === 'PROCESSOR') {
            connectionEntity.component['selectedRelationships'] = self.connectionDefinitionComponent.getSelectedRelationships();
        }

        return connectionEntity;
    };

    /**
     * Determine if any processor settings have changed.
     *
     * @returns {boolean}
     */
    var isSaveRequired = function () {
        if (self.name !== self.connectionEntity.component.name) {
            return true;
        }
        if (self.flowFileExpiration !== self.connectionEntity.component.flowFileExpiration) {
            return true;
        }
        if (self.backPressureObjectThreshold !== self.connectionEntity.component.backPressureObjectThreshold) {
            return true;
        }
        if (self.backPressureDataSizeThreshold !== self.connectionEntity.component.backPressureDataSizeThreshold) {
            return true;
        }

        return self.connectionDefinitionComponent.isSaveRequired();
    };

    this.opened = false;
    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();

    this.statusChangeSubscription = null;
    this.unchanged = null;

    this.connectionDefinitionComponent = null;
    this.connectionConfigurationForm = null;

    this.connectionEntity = null;
    this.sourceEntity = null;
    this.destinationEntity = null;

    this.name = null;
    this.flowFileExpiration = null;
    this.backPressureObjectThreshold = null;
    this.backPressureDataSizeThreshold = null;

    /**
     * Configure this component.
     */
    this.init = function () {
        // extract the configuration into each respective field so that we do not over write the connectionEntity model before the save actually happens
        this.name = this.connectionEntity.component.name;
        this.flowFileExpiration = this.connectionEntity.component.flowFileExpiration;
        this.backPressureObjectThreshold = this.connectionEntity.component.backPressureObjectThreshold;
        this.backPressureDataSizeThreshold = this.connectionEntity.component.backPressureDataSizeThreshold;

        this.opened = true;

        this.statusChangeSubscription = this.connectionConfigurationForm.statusChanges.subscribe(function () {
            self.unchanged = !isSaveRequired();
        });
    };

    /**
     * Gets the title for this configuration component.
     */
    this.getTitle = function () {
        var name = self.connectionEntity.component.name;
        if (commonService.isUndefinedOrNull(name)) {
            name = '';
        }

        return name + ' (Connection)';
    };

    /**
     * Gets the name of the source component.
     */
    this.getComponentName = function (componentEntity) {
        if (componentEntity.type === 'funnel') {
            return '(Funnel)';
        } else {
            return componentEntity.component.name;
        }
    };

    /**
     * Update processor.
     */
    this.update = function () {
        this.subject$
            .debug("FlowDesignerConnectionConfigurationComponent subject$ Next")
            .next(marshalConnection());
    };

    /**
     * Notify subscribers of the cancel processor configuration action.
     *
     * @param skipConfigNotAppliedCheck optionally skips the check for possible loss of unapplied changes.
     */
    this.cancel = function (skipConfigNotAppliedCheck) {
        if (!skipConfigNotAppliedCheck && !this.isUnchanged()) {
            // show the ConfigNotAppliedComponent in a dialog
            var dialogRef = dialog.open(ConfigNotAppliedComponent, {
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
            .debug("FlowDesignerConnectionConfigurationComponent subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.statusChangeSubscription.unsubscribe();
        this.unchanged = null;

        this.name = null
        this.flowFileExpiration = null;
        this.backPressureObjectThreshold = null;
        this.backPressureDataSizeThreshold = null;

        this.connectionDefinitionComponent = null;
        this.connectionConfigurationForm = null;
        this.connectionEntity = null;
        this.sourceEntity = null;
        this.destinationEntity = null;
        this.newDestinationEntity = null;
        this.effectiveDestinationEntity = null;

        this.subject$
            .debug("FlowDesignerConnectionConfigurationComponent subject$ Complete")
            .complete();
        this.opened = false;
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    };

    /**
     * Get the type of the component.
     *
     * @returns {string}
     */
    this.getType = function () {
        return 'connection';
    };

    /**
     * Get the id of the connection.
     *
     * @returns {*}
     */
    this.getId = function () {
        if (!commonService.isUndefined(this.connectionEntity)) {
            return this.connectionEntity.id;
        }
        return null;
    };

    /**
     * Get the parent group id that this connection belongs to.
     * @returns {*}
     */
    this.getParentGroupId = function () {
        if (!commonService.isUndefined(this.connectionEntity) && !commonService.isUndefined(this.connectionEntity.component)) {
            return this.connectionEntity.component.parentGroupId;
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
        return this.connectionConfigurationForm.invalid;
    };
};

FlowDesignerConnectionConfigurationComponent.prototype = {
    constructor: FlowDesignerConnectionConfigurationComponent,

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

FlowDesignerConnectionConfigurationComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-connection-configuration',
        template: require('./fd.connection-configuration.component.html!text'),
        inputs: [
            'connectionEntity',
            'sourceEntity',
            'destinationEntity'
        ],
        queries: {
            connectionDefinitionComponent: new ngCore.ViewChild('connectionDefinitionComponent'),
            connectionConfigurationForm: new ngCore.ViewChild('connectionConfigurationForm')
        }
    })
];

FlowDesignerConnectionConfigurationComponent.parameters = [
    CommonService,
    ngMaterial.MatDialog
];

module.exports = FlowDesignerConnectionConfigurationComponent;
