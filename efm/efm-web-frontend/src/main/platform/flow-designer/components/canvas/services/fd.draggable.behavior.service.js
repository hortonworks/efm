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

/**
 * DraggableBehavior constructor.
 * @constructor
 */
function DraggableBehavior() {
    var drag;

    /**
     * Initialize the draggable behavior.
     *
     * @param canvasSvg
     * @param getScale
     * @param updateComponentsPosition
     */
    this.init = function (canvasSvg, getScale, updateComponentsPosition) {
        // handle component drag events
        drag = d3.drag()
            .on('start', function () {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            })
            .on('drag', function () {
                var dragSelection = canvasSvg.select('rect.drag-selection');

                // lazily create the drag selection box
                if (dragSelection.empty()) {
                    // get the current selection
                    var selection = canvasSvg.selectAll('g.component.selected');

                    // determine the appropriate bounding box
                    var minX = null, maxX = null, minY = null, maxY = null;
                    selection.each(function (d) {
                        if (minX === null || d.position.x < minX) {
                            minX = d.position.x;
                        }
                        if (minY === null || d.position.y < minY) {
                            minY = d.position.y;
                        }
                        var componentMaxX = d.position.x + d.dimensions.width;
                        var componentMaxY = d.position.y + d.dimensions.height;
                        if (maxX === null || componentMaxX > maxX) {
                            maxX = componentMaxX;
                        }
                        if (maxY === null || componentMaxY > maxY) {
                            maxY = componentMaxY;
                        }
                    });

                    // create a selection box for the move
                    canvasSvg.append('rect')
                        .attr('rx', 6)
                        .attr('ry', 6)
                        .attr('x', minX)
                        .attr('y', minY)
                        .attr('class', 'drag-selection')
                        .attr('pointer-events', 'none')
                        .attr('width', maxX - minX)
                        .attr('height', maxY - minY)
                        .attr('stroke-width', function () {
                            return 1 / getScale();
                        })
                        .attr('stroke-dasharray', function () {
                            return 4 / getScale();
                        })
                        .datum({
                            original: {
                                x: minX,
                                y: minY
                            },
                            x: minX,
                            y: minY
                        });
                } else {
                    // update the position of the drag selection
                    dragSelection.attr('x', function (d) {
                        d.x += d3.event.dx;
                        return d.x;
                    })
                        .attr('y', function (d) {
                            d.y += d3.event.dy;
                            return d.y;
                        });
                }
            })
            .on('end', function () {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();

                // get the drag selection
                var dragSelection = canvasSvg.select('rect.drag-selection');

                // ensure we found a drag selection
                if (dragSelection.empty()) {
                    return;
                }

                updateComponentsPosition(dragSelection);

                // remove the drag selection
                dragSelection.remove();
            });
    };

    /**
     * Activates the drag behavior for the components in the specified selection.
     *
     * @param {selection} components
     */
    this.activate = function (components) {
        components.classed('moveable', true).call(drag);
    };

    /**
     * Deactivates the drag behavior for the components in the specified selection.
     *
     * @param {selection} components
     */
    this.deactivate = function (components) {
        components.classed('moveable', false).on('.drag', null);
    };
};

DraggableBehavior.prototype = {
    constructor: DraggableBehavior
};

DraggableBehavior.parameters = [];

module.exports = DraggableBehavior;
