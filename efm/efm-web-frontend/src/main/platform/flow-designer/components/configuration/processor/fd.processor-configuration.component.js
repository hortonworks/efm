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
 * FlowDesignerProcessorConfigurationComponent.
 *
 * @param commonService                         The common service.
 * @param dialog                                The Angular Material dialog.
 * @constructor
 */
function FlowDesignerProcessorConfigurationComponent(commonService, dialog) {
    var self = this;
    var dialogRef = null;

    var marshalProcessor = function () {
        var processorEntity = {
            'id': self.componentEntity.id,
            'component': {
                'name': self.name,
                'config': {
                    'comments': self.comments,
                    'properties': self.properties,
                    'descriptors': self.descriptors,
                    'penaltyDuration': self.penaltyDuration,
                    'yieldDuration': self.yieldDuration,
                    'schedulingStrategy': self.schedulingStrategy,
                    'concurrentlySchedulableTaskCount': self.concurrentlySchedulableTaskCount,
                    'runDurationMillis': self.RUN_DURATION_VALUES[self.runDuration].value,
                    'autoTerminatedRelationships': self.getSelectedRelationships()
                }
            }
        };

        if (self.isTimerDriven()) {
            processorEntity.component.config['schedulingPeriod'] = self.schedulingPeriod;
        }

        return processorEntity;
    };

    /**
     * Gets the selected relationship names.
     *
     * @returns {array}     Array of selected relationship names
     */
    this.getSelectedRelationships = function () {
        return self.relationships.filter(function (relationship) {
            return relationship.autoTerminate === true;
        }).map(function (relationship) {
            return relationship.name;
        });
    };

    /**
     * Determine if any processor settings have changed.
     *
     * @returns {boolean}
     */
    var isSaveRequired = function () {
        // consider auto terminated relationships
        var autoTerminatedChanged = false;
        self.relationships.forEach(function (relationship) {
            if (autoTerminatedChanged === false) {
                if (relationship.autoTerminate === true) {
                    // relationship was auto terminated but is no longer selected
                    if (autoTerminatedRelationships.includes(relationship.name) === false) {
                        autoTerminatedChanged = true;
                    }
                } else {
                    // relationship was not auto terminated but is now selected
                    if (autoTerminatedRelationships.includes(relationship.name)) {
                        autoTerminatedChanged = true;
                    }
                }
            }
        });
        if (autoTerminatedChanged === true) {
            return true;
        }

        // consider the scheduling strategy
        if (self.schedulingStrategy !== self.componentEntity.component.config.schedulingStrategy) {
            return true;
        }

        // only consider the concurrent tasks if appropriate
        if (self.concurrentlySchedulableTaskCount !== self.componentEntity.component.config.concurrentlySchedulableTaskCount) {
            return true;
        }

        // get the appropriate scheduling period field
        if (self.isTimerDriven()) {
            if (self.schedulingPeriod !== self.componentEntity.component.config.schedulingPeriod) {
                return true;
            }
        }

        // check the run duration values
        if (self.RUN_DURATION_VALUES[self.runDuration].value !== self.componentEntity.component.config.runDurationMillis) {
            return true;
        }

        if (self.name !== self.componentEntity.component.name) {
            return true;
        }
        if (self.penaltyDuration !== self.componentEntity.component.config.penaltyDuration) {
            return true;
        }
        if (self.yieldDuration !== self.componentEntity.component.config.yieldDuration) {
            return true;
        }
        if (self.comments !== self.componentEntity.component.config.comments) {
            return true;
        }

        // defer to the property and relationship grids
        return self.processorPropertyTableComponent.isSaveRequired();
    };

    this.RUN_DURATION_VALUES = [
        {name: '0ms', value: 0, clazz: 'width_17 slider-label'},
        {name: '25ms', value: 25, clazz: 'width_17 slider-label'},
        {name: '50ms', value: 50, clazz: 'width_17 slider-label'},
        {name: '100ms', value: 100, clazz: 'width_17 slider-label'},
        {name: '250ms', value: 250, clazz: 'width_28 slider-label'},
        {name: '500ms', value: 500, clazz: 'width_28 slider-label'},
        {name: '1s', value: 1000, clazz: 'width_17 slider-label'},
        {name: '2s', value: 2000, clazz: 'width_17 slider-label'}
    ];

    this.opened = false;
    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();
    this.statusChangeSubscription = null;
    this.unchanged = true;

    this.processorConfigurationElementRef = null;
    this.processorPropertyTableComponent = null;
    this.processorConfigurationForm = null;

    this.componentEntity = null;
    this.flowDesignerCanvasComponent = null;
    this.flowDesignerApi = null;
    this.options = null;

    this.name = null;
    this.processorTypeDisplayName = null;
    this.comments = null;

    this.penaltyDuration = null;
    this.yieldDuration = null;

    this.schedulingStrategy = null;
    this.concurrentlySchedulableTaskCount = null;
    this.schedulingPeriod = null;
    this.runDuration = null;

    var autoTerminatedRelationships = null;
    this.relationships = null;
    this.parentGroupId = null;
    this.descriptors = null;
    this.properties = null;

    /**
     * Configure this component.
     *
     * @param flowDesignerProcessorConfigurationComponent           The flow designer processor configuration component entity.
     */
    this.init = function (flowDesignerProcessorConfigurationComponent) {
        this.flowDesignerCanvasComponent = flowDesignerProcessorConfigurationComponent.flowDesignerCanvasComponent;
        this.flowDesignerApi = flowDesignerProcessorConfigurationComponent.flowDesignerApi;

        this.opened = true;

        // extract the configuration into each respective field so that we do not over write the componentEntity model before the save actually happens
        this.name = flowDesignerProcessorConfigurationComponent.componentEntity.component.name;
        this.processorTypeDisplayName = commonService.substringAfterLast(flowDesignerProcessorConfigurationComponent.componentEntity.component.type, '.');
        this.comments = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.comments;

        this.penaltyDuration = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.penaltyDuration;
        this.yieldDuration = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.yieldDuration;

        this.schedulingStrategy = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.schedulingStrategy;
        this.concurrentlySchedulableTaskCount = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.concurrentlySchedulableTaskCount;
        this.schedulingPeriod = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.schedulingPeriod;
        this.runDuration = this.RUN_DURATION_VALUES.findIndex(function (runDuration) {
            return runDuration['value'] === flowDesignerProcessorConfigurationComponent.componentEntity.component.config.runDurationMillis;
        });
        autoTerminatedRelationships = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.autoTerminatedRelationships;
        this.relationships = flowDesignerProcessorConfigurationComponent.componentEntity.component.relationships.map(function (relationship) {
            return {
                'name': relationship.name,
                'description': relationship.description,
                'autoTerminate': autoTerminatedRelationships.includes(relationship.name)
            };
        });

        this.parentGroupId = flowDesignerProcessorConfigurationComponent.componentEntity.component.parentGroupId;
        this.descriptors = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.descriptors;
        this.properties = flowDesignerProcessorConfigurationComponent.componentEntity.component.config.properties;

        // processor configuration property table options
        this.options = {
            readOnly: false
        };

        this.statusChangeSubscription = this.processorConfigurationForm.statusChanges.subscribe(this.validateForm);
    };

    /**
     * Checks the form for changes.
     */
    this.validateForm = function () {
        self.unchanged = !isSaveRequired();
    };

    /**
     * Determines if timer driven is currently configured.
     *
     * @returns {boolean}
     */
    this.isTimerDriven = function () {
        return this.schedulingStrategy === 'TIMER_DRIVEN';
    };

    /**
     * Gets the scheduling strategy display value
     */
    this.getSchedulingStrategy = function () {
        return (this.schedulingStrategy === 'TIMER_DRIVEN') ? 'Timer Driven' : 'Event Driven';
    },

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
        this.processorPropertyTableComponent.saveRow();

        // show the add property dialog
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
                        self.processorPropertyTableComponent.edit(property);
                    } else {
                        self.processorPropertyTableComponent.scrollToRow(property);
                    }
                }
            });

        // react to add a new property
        dialogRef.componentInstance.subject$
            .subscribe(function (propertyName) {
                self.flowDesignerApi.getProcessorPropertyDescriptor(self.componentEntity, propertyName)
                    .subscribe(function (descriptor) {
                        // add the descriptor
                        propertyAdded = self.processorPropertyTableComponent.addProperty(descriptor);

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
            .debug("FlowDesignerProcessorConfigurationComponent subject$ Next")
            .next(marshalProcessor());
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
            .debug("FlowDesignerProcessorConfigurationComponent subject$ Next Cancel")
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
            .debug("FlowDesignerProcessorConfigurationComponent subject$ Complete")
            .complete();
        this.processorConfigurationElementRef = null;
        this.processorPropertyTableComponent = null;
        this.processorConfigurationForm = null;
        this.componentEntity = null;
        this.flowDesignerCanvasComponent = null;
        this.flowDesignerApi = null;
        this.options = null;
        this.name = null;
        this.processorTypeDisplayName = null;
        this.comments = null;
        this.penaltyDuration = null;
        this.yieldDuration = null;
        this.schedulingStrategy = null;
        this.concurrentlySchedulableTaskCount = null;
        this.schedulingPeriod = null;
        this.runDuration = null;
        autoTerminatedRelationships = null;
        this.relationships = null;
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
        return 'processor';
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
        return this.processorConfigurationForm.invalid;
    };
};

FlowDesignerProcessorConfigurationComponent.prototype = {
    constructor: FlowDesignerProcessorConfigurationComponent,

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

FlowDesignerProcessorConfigurationComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-processor-configuration',
        template: require('./fd.processor-configuration.component.html!text'),
        inputs: [
            'componentEntity',
            'flowDesignerCanvasComponent',
            'flowDesignerApi'
        ],
        queries: {
            processorConfigurationElementRef: new ngCore.ViewChild('processorConfigurationElementRef'),
            processorPropertyTableComponent: new ngCore.ViewChild('processorPropertyTableComponent'),
            processorConfigurationForm: new ngCore.ViewChild('processorConfigurationForm')
        }
    })
];

FlowDesignerProcessorConfigurationComponent.parameters = [
    CommonService,
    ngMaterial.MatDialog
];

module.exports = FlowDesignerProcessorConfigurationComponent;
