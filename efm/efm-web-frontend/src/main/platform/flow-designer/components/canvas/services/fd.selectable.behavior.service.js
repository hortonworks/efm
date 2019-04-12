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

var d3 = require('d3');
var rxjs = require('rxjs/Subject');

/**
 * SelectableBehavior constructor.
 *
 * @constructor
 */
function SelectableBehavior() {
    var self = this;
    var canvas = null;

    this.subject$ = new rxjs.Subject();

    /**
     * Initializes the selectable behavior.
     *
     * @param canvasRef             Canvas reference
     */
    this.init = function (canvasRef) {
        canvas = canvasRef;
    };

    /**
     * Select component. This method must be called within the context of a d3 event handler.
     *
     * @param g
     */
    this.select = function (g) {
        // only need to update selection if necessary
        if (!g.classed('selected')) {
            // we are not currently selected, if not the shift key then select
            if (!d3.event.shiftKey) {
                // deselect the current selection
                var currentlySelected = canvas.getSelection();
                currentlySelected.classed('selected', false);

                // update the selection
                g.classed('selected', true);

                self.subject$.next(g);
            }
        }

        // selection handled... stop propagation
        d3.event.stopPropagation();
    };

    /**
     * Activates the select behavior for the components in the specified selection.
     *
     * @param {selection} components
     */
    this.activate = function (components) {
        components.on('mousedown.selection', function () {
            // get the clicked component to update selection
            self.select(d3.select(this));
        });
    };
};

SelectableBehavior.prototype = {
    constructor: SelectableBehavior
};

SelectableBehavior.parameters = [
];

module.exports = SelectableBehavior;
