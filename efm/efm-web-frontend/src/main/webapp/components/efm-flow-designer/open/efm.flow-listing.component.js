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
var rxjs = $.extend({}, require('rxjs/Subject'), require('rxjs/Observable'));
var ngCore = require('@angular/core');

/**
 * EfmFlowListing constructor.
 *
 * @constructor
 */
function EfmFlowListing() {
    var self = this;

    // data table
    this.flowColumns = [
        {
            name: 'status',
            label: 'Status',
            sortable: true,
            tooltip: 'Status',
            width: 10,
            align: 'center',
            get: function (flow) {
                return self.isDirty(flow) ? 1 : 0;
            },
            format: function (flow) {
                if (self.isDirty(flow)) {
                    return 'Modified since the last published version.';
                }

                return 'Not modified since the last published version.';
            }
        },
        {
            name: 'agentClass',
            label: 'Class',
            sortable: true,
            tooltip: 'Class',
            width: 50,
            align: 'start',
            get: function (flow) {
                return flow.agentClass;
            },
            format: function (flow) {
                return flow.agentClass;
            }
        },
        {
            name: 'publishedVersion',
            label: 'Published Version',
            sortable: true,
            tooltip: 'Published Version',
            width: 15,
            align: 'end',
            get: function (flow) {
                return self.isVersioned(flow) ? flow.versionInfo.registryVersion : -1;
            },
            format: function (flow) {
                if (self.isVersioned(flow)) {
                    return flow.versionInfo.registryVersion;
                }

                return '';
            }
        },
        {
            name: 'publishedOn',
            label: 'Published On',
            sortable: true,
            tooltip: 'Published On',
            width: 25,
            align: 'start',
            get: function (flow) {
                return self.isVersioned(flow) ? flow.versionInfo.lastPublished : -1;
            },
            format: function (flow) {
                if (self.isVersioned(flow)) {
                    return new Date(flow.versionInfo.lastPublished).toLocaleString();
                }

                return '';
            }
        }
    ];

    this.flows = [];
    this.selectedFlow = null;

    // sort
    var ACTIVE_COLUMN = 1;
    this.flowColumns[ACTIVE_COLUMN].active = true;
    this.activeColumn = this.flowColumns[ACTIVE_COLUMN];
    this.activeColumn.sortOrder = 'DESC';

    // Subjects
    this.subject$ = new rxjs.Subject();
    this.componentDestroyed$ = new rxjs.Subject();

    /**
     * Sets the available flows for the listing.
     *
     * @param availableFlows
     */
    this.setFlows = function (availableFlows) {
        this.flows = availableFlows.map(function (flow) {
            return $.extend({}, {
                selected: false
            }, flow);
        });

        this.sortFlows(this.activeColumn);
    };

    /**
     * Returns whether the specified flow is versioned.
     *
     * @param flow
     */
    this.isVersioned = function (flow) {
        return typeof flow.versionInfo !== 'undefined' && flow.versionInfo !== null;
    };

    /**
     * Gets the status for the specified flow.
     *
     * @param flow
     */
    this.isDirty = function (flow) {
        if (!self.isVersioned(flow)) {
            return true;
        }

        return flow.versionInfo.dirty === true;
    };

    /**
     * Selects the specified flow.
     *
     * @param flow
     */
    this.selectFlow = function (flow) {
        // deselect other flows
        this.flows.forEach(function (flow) {
            flow.selected = false;
        });

        // select this flow
        flow.selected = true;
        this.selectedFlow = flow;
    };

    /**
     * Returns the currently selected flow.
     */
    this.getSelectedFlow = function () {
        return this.selectedFlow;
    };

    /**
     * Updates the sorted column.
     *
     * @param column column to toggle
     */
    this.sortFlows = function (column) {
        if (column.sortable === true) {
            this.activeColumn = column;

            // toggle column sort order
            this.activeColumn.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';

            // only one column can be actively sorted so we reset all to inactive
            this.flowColumns.forEach(function (c) {
                c.active = false;
            });

            // sort the flows
            this.flows.sort(function (a, b) {
                var aString = column.get(a);
                var bString = column.get(b);
                return aString === bString ? 0 : aString > bString ? 1 : -1;
            });
            if (this.activeColumn.sortOrder === 'DESC') {
                this.flows.reverse();
            }

            // and set this column as the actively sorted column
            this.activeColumn.active = true;
        }
    };

    /**
     * Open the specified flow.
     *
     * @param flow
     */
    this.openFlow = function (flow) {
        this.subject$
            .debug("EfmFlowListing subject$ Next")
            .next(flow);
    };

    /**
     * Notify subscribers of the cancel processor configuration action.
     */
    this.cancel = function () {
        this.subject$
            .debug("EfmFlowListing subject$ Next Cancel")
            .error();
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.subject$
            .debug("EfmFlowListing subject$ Complete")
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();

        this.flows = [];
        this.selectedFlow = null;
    };
};

EfmFlowListing.prototype = {
    constructor: EfmFlowListing,

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

EfmFlowListing.annotations = [
    new ngCore.Component({
        selector: 'efm-flow-listing',
        template: require('./efm.flow-listing.component.html!text')
    })
];

EfmFlowListing.parameters = [
];

module.exports = EfmFlowListing;
