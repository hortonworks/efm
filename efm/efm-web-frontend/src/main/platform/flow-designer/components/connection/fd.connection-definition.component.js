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
var CommonService = require('@flow-designer/services/CommonService');

/**
 * FlowDesignerConnectionDefinitionComponent.
 *
 * @param commonService                         The common service.
 * @constructor
 */
function FlowDesignerConnectionDefinitionComponent(commonService) {
    var self = this;

    this.connectionEntity = null;
    this.sourceEntity = null;
    this.destinationEntity = null;

    this.sourceType = null;
    this.destinationType = null;

    this.relationships = null;

    this.selectedRelationships = null;
    this.sourcePortId = null;
    this.destinationPortId = null;

    /**
     * Initializes the connection definition component.
     */
    this.init = function () {
        this.sourceType = this.sourceEntity.type;
        this.destinationType = this.destinationEntity.type;

        if (this.sourceType === 'processor') {
            this.relationships = this.sourceEntity.component.relationships.map(function (relationship) {
                var checked = false;

                // if there is a connection, select the relationship if configured otherwise pre-select the
                // relationship if there is only one relationship for this source processor
                if (commonService.isDefinedAndNotNull(self.connectionEntity)) {
                    checked = self.connectionEntity.component.selectedRelationships.indexOf(relationship.name) >= 0;
                } else {
                    checked = self.sourceEntity.component.relationships.length === 1;
                }

                return {
                    'name': relationship.name,
                    'description': relationship.description,
                    'checked':  checked,
                    'unavailable': false
                };
            });
        }

        if (commonService.isDefinedAndNotNull(this.connectionEntity)) {
            // source processor
            if (this.sourceType === 'processor') {
                this.selectedRelationships = this.connectionEntity.component.selectedRelationships;

                // identify any unavailable relationships
                var unavailableRelationships = this.connectionEntity.component.selectedRelationships
                    .filter(function (selectedRelationship) {
                        var available = self.sourceEntity.component.relationships.find(function (availableRelationship) {
                            return availableRelationship.name === selectedRelationship;
                        });
                        return commonService.isUndefined(available);
                    })
                    .map(function (unavailableRelationship) {
                        return {
                            'name': unavailableRelationship,
                            'description': 'Dynamic relationship that is no longer supported.',
                            'checked': true,
                            'unavailable': true
                        }
                    });
                this.relationships = this.relationships.concat(unavailableRelationships);
            }

            // source rpg
            if (this.sourceType === 'remote-process-group') {
                this.sourcePortId = this.connectionEntity.sourceId;
            }

            var currentDestinationId = this.connectionEntity.destinationType === 'REMOTE_INPUT_PORT' ? this.connectionEntity.destinationGroupId : this.connectionEntity.destinationId;
            var hasNewDestination = currentDestinationId !== this.destinationEntity.id;

            // destination rpg
            if (!hasNewDestination && this.destinationType === 'remote-process-group') {
                this.destinationPortId = this.connectionEntity.destinationId;
            }
        }
    };

    /**
     * Whether or not a save is required.
     *
     * @returns {boolean}       Whether or not a save is required
     */
    this.isSaveRequired = function () {
        if (commonService.isDefinedAndNotNull(this.connectionEntity)) {
            if (self.sourceType === 'processor') {
                var selectionDiffers = false;
                var updatedRelationshipSelection = self.getSelectedRelationships();

                // consider each relationship and see if the selection has changed
                for (var i = 0; i < self.relationships.length && !selectionDiffers; i++) {
                    var rel = self.relationships[i];

                    var originallySelected = self.selectedRelationships.indexOf(rel.name) >= 0;
                    var currentlySelected = updatedRelationshipSelection.indexOf(rel.name) >= 0;

                    selectionDiffers = originallySelected !== currentlySelected;
                }

                // if the selection differs, return that fact otherwise check the remainder of the conditions
                if (selectionDiffers) {
                    return true;
                }
            }

            if (self.sourceType === 'remote-process-group' && self.sourcePortId !== self.connectionEntity.sourceId) {
                return true;
            }

            if (self.destinationType === 'remote-process-group' && self.destinationPortId !== self.connectionEntity.destinationId) {
                return true;
            }

            return false;
        } else {
            if (self.sourceType === 'processor' && self.getSelectedRelationships().length === 0) {
                return false;
            }

            if (self.sourceType === 'remote-process-group' && commonService.isBlank(self.sourcePortId)) {
                return false;
            }

            if (self.destinationType === 'remote-process-group' && commonService.isBlank(self.destinationPortId)) {
                return false;
            }

            return true;
        }
    };

    /**
     * Gets the source of the connection.
     *
     * @returns {object}    The source of the connection
     */
    this.getSource = function () {
        var sourceConnectableType = commonService.getConnectableTypeForSource(self.sourceEntity);

        // create the connection source
        var source;
        if (sourceConnectableType === 'REMOTE_OUTPUT_PORT') {
            source = {
                'id': self.sourcePortId,
                'groupId': self.sourceEntity.id,
                'type': sourceConnectableType
            };
        } else {
            source = {
                'id': self.sourceEntity.id,
                'groupId': self.sourceEntity.component.parentGroupId,
                'type': sourceConnectableType
            };
        }

        return source;
    };

    /**
     * Gets the destination of the connection.
     *
     * @returns {object}    The destination of the connection
     */
    this.getDestination = function () {
        var destinationConnectionType = commonService.getConnectableTypeForDestination(self.destinationEntity);

        // create the connection destination
        var destination;
        if (destinationConnectionType === 'REMOTE_INPUT_PORT') {
            destination = {
                'id': self.destinationPortId,
                'groupId': self.destinationEntity.id,
                'type': destinationConnectionType
            };
        } else {
            destination = {
                'id': self.destinationEntity.id,
                'groupId': self.destinationEntity.component.parentGroupId,
                'type': destinationConnectionType
            };
        }

        return destination;
    };

    /**
     * Gets the selected relationship names.
     *
     * @returns {array}     Array of selected relationship names
     */
    this.getSelectedRelationships = function () {
        return self.relationships.filter(function (relationship) {
            return relationship.checked === true;
        }).map(function (relationship) {
            return relationship.name;
        });
    };

    /**
     * Returns whether this component definition is being used to edit an existing connection.
     *
     * @returns {boolean}   Whether we're editing an existing connection
     */
    this.isEdit = function () {
        return commonService.isDefinedAndNotNull(self.connectionEntity);
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.connectionEntity = null;
        this.sourceEntity = null;
        this.destinationEntity = null;
        this.sourceType = null;
        this.destinationType = null;
        this.relationships = null;
        this.selectedRelationships = null;
        this.sourcePortId = null;
        this.destinationPortId = null;
    };
};

FlowDesignerConnectionDefinitionComponent.prototype = {
    constructor: FlowDesignerConnectionDefinitionComponent,

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

FlowDesignerConnectionDefinitionComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-connection-definition',
        template: require('../connection/fd.connection-definition.component.html!text'),
        inputs: [
            'connectionEntity',
            'sourceEntity',
            'destinationEntity'
        ],
        viewProviders: [{ provide: ngForms.ControlContainer, useExisting: ngForms.NgForm }]
    })
];

FlowDesignerConnectionDefinitionComponent.parameters = [
    CommonService
];

module.exports = FlowDesignerConnectionDefinitionComponent;
