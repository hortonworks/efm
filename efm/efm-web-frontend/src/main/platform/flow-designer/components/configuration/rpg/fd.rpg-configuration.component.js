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
var rxjs = require('rxjs/Subject');
var ngMaterial = require('@angular/material');
var ConfigNotAppliedComponent = require('@flow-designer/components/flow-designer-configuration-not-applied');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * RemoteProcessGroupConfigurationComponent.
 *
 * @param dialog                                The Angular Material dialog.
 * @param commonService                         The common service.
 * @constructor
 */
function RemoteProcessGroupConfigurationComponent(dialog, commonService) {
    var self = this;

    var marshalRpg = function () {
        var rpgEntity = {
            'id': self.componentEntity.id,
            'component': {
                'name': self.componentEntity.name,
                'targetUris': self.targetUris,
                'transportProtocol': self.transportProtocol,
                'localNetworkInterface': self.localNetworkInterface,
                'proxyHost': self.proxyHost,
                'proxyPort': self.proxyPort,
                'communicationsTimeout': self.communicationsTimeout,
                'yieldDuration': self.yieldDuration
            }
        };

        return rpgEntity;
    };

    /**
     * Determine if any rpg settings have changed.
     *
     * @returns {boolean}
     */
    var isSaveRequired = function () {
        // consider the targetUris
        if (self.targetUris !== self.componentEntity.component.targetUris) {
            return true;
        }

        // consider the transportProtocol
        if (self.transportProtocol !== self.componentEntity.component.transportProtocol) {
            return true;
        }

        // consider the localNetworkInterface
        if (self.localNetworkInterface !== self.componentEntity.component.localNetworkInterface) {
            return true;
        }

        // consider the proxyHost
        if (self.proxyHost !== self.componentEntity.component.proxyHost) {
            return true;
        }

        // consider the proxyPort
        if (self.proxyPort !== self.componentEntity.component.proxyPort) {
            return true;
        }

        // consider the communicationsTimeout
        if (self.communicationsTimeout !== self.componentEntity.component.communicationsTimeout) {
            return true;
        }

        // consider the yieldDuration
        if (self.yieldDuration !== self.componentEntity.component.yieldDuration) {
            return true;
        }

        return false;
    };

    this.opened = false;
    this.unchanged = true;
    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();

    this.targetUris = null;

    this.transportProtocol = null;

    this.localNetworkInterface = null;

    this.proxyHost = null;
    this.proxyPort = null;

    this.communicationsTimeout = null;

    this.yieldDuration = null;

    /**
     * Notify subscribers of the cancel rpg configuration action.
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
            .debug('RemoteProcessGroupConfigurationComponent subject$ Cancel')
            .error();
    };

    /**
     * Configure this component.
     *
     * @param flowDesignerRpgConfigurationComponent           The flow designer rpg configuration component entity.
     */
    this.init = function (flowDesignerRpgConfigurationComponent) {
        this.opened = true;

        // extract the configuration into each respective field so that we do not over write the componentEntity model before the save actually happens
        this.targetUris = flowDesignerRpgConfigurationComponent.componentEntity.component.targetUris;
        this.transportProtocol = flowDesignerRpgConfigurationComponent.componentEntity.component.transportProtocol;
        this.localNetworkInterface = flowDesignerRpgConfigurationComponent.componentEntity.component.localNetworkInterface;
        this.proxyHost = flowDesignerRpgConfigurationComponent.componentEntity.component.proxyHost;
        this.proxyPort = flowDesignerRpgConfigurationComponent.componentEntity.component.proxyPort;
        this.communicationsTimeout = flowDesignerRpgConfigurationComponent.componentEntity.component.communicationsTimeout;
        this.yieldDuration = flowDesignerRpgConfigurationComponent.componentEntity.component.yieldDuration;

        this.proxyPortControl.control.setValidators([ngForms.Validators.min(-1), ngForms.Validators.pattern(/^-?[1-9][0-9]*$/), ngForms.Validators.max(65535)]);
        this.statusChangeSubscription = this.rpgConfigurationForm.statusChanges.subscribe(this.validateForm);
    };

    /**
     * Checks the form for changes.
     */
    this.validateForm = function () {
        self.unchanged = !isSaveRequired();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.statusChangeSubscription.unsubscribe();
        this.targetUris = null;
        this.transportProtocol = null;
        this.localNetworkInterface = null;
        this.proxyHost = null;
        this.proxyPort = null;
        this.communicationsTimeout = null;
        this.yieldDuration = null;
        this.opened = false;
        this.unchanged = null;
        this.subject$
            .debug('RemoteProcessGroupConfigurationComponent subject$ Complete')
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();
    };

    /**
     * Update processor.
     */
    this.update = function () {
        this.subject$
            .debug('RemoteProcessGroupConfigurationComponent subject$ Next')
            .next(marshalRpg());
    };

    /**
     * Get the type of the component.
     *
     * @returns {string}
     */
    this.getType = function () {
        return 'remote-process-group';
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
        return this.rpgConfigurationForm.invalid;
    };
}

RemoteProcessGroupConfigurationComponent.prototype = {
    constructor: RemoteProcessGroupConfigurationComponent,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.init(this);
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

RemoteProcessGroupConfigurationComponent.annotations = [
    new ngCore.Component({
        selector: 'remote-process-group-configuration',
        template: require('./fd.rpg-configuration.component.html!text'),
        inputs: [
            'componentEntity'
        ],
        queries: {
            rpgConfigurationElementRef: new ngCore.ViewChild('rpgConfigurationElementRef'),
            rpgConfigurationForm: new ngCore.ViewChild('rpgConfigurationForm'),
            proxyPortControl: new ngCore.ViewChild('proxyPortControl')
        }
    })
];

RemoteProcessGroupConfigurationComponent.parameters = [
    ngMaterial.MatDialog,
    CommonService
];

module.exports = RemoteProcessGroupConfigurationComponent;
