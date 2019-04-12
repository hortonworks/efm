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
var CommonService = require('@flow-designer/services/CommonService');
var ngCore = require('@angular/core');

/**
 * CanvasUtilsService constructor.
 *
 * @param commonService             The common service.
 * @param ngZone                    The ngZone service.
 * @constructor
 */
function CanvasUtilsService(commonService, ngZone) {
    var self = this;
    var TWO_PI = 2 * Math.PI;

    var binarySearch = function (length, comparator) {
        var low = 0;
        var high = length - 1;
        var mid;

        var result = 0;
        while (low <= high) {
            mid = ~~((low + high) / 2);
            result = comparator(mid);
            if (result < 0) {
                high = mid - 1;
            } else if (result > 0) {
                low = mid + 1;
            } else {
                break;
            }
        }

        return mid;
    };

    /**
     * Conditionally apply the transition.
     *
     * @param selection     selection
     * @param transition    transition
     */
    this.transition = function (selection, transition) {
        return ngZone.runOutsideAngular(function () {
            if (transition && !selection.empty()) {
                return selection.transition().duration(400);
            } else {
                return selection;
            }
        });
    };

    /**
     * Position the component accordingly.
     *
     * @param {selection} updated
     */
    this.position = function (updated, transition) {
        return ngZone.runOutsideAngular(function () {
            if (updated.empty()) {
                return;
            }

            return self.transition(updated, transition)
                .attr('transform', function (d) {
                    return 'translate(' + d.position.x + ', ' + d.position.y + ')';
                });
        });
    };

    /**
     * Determines if the specified selection is a connection.
     *
     * @argument {selection} selection      The selection
     */
    this.isConnection = function (selection) {
        return ngZone.runOutsideAngular(function () {
            return selection.classed('connection');
        });
    };

    /**
     * Determines if the specified selection is a remote process group.
     *
     * @argument {selection} selection      The selection
     */
    this.isRemoteProcessGroup = function (selection) {
        return ngZone.runOutsideAngular(function () {
            return selection.classed('remote-process-group');
        });
    };

    /**
     * Determines if the specified selection is a processor.
     *
     * @argument {selection} selection      The selection
     */
    this.isProcessor = function (selection) {
        return ngZone.runOutsideAngular(function () {
            return selection.classed('processor');
        });
    };

    /**
     * Determines if the specified selection is a funnel.
     *
     * @argument {selection} selection      The selection
     */
    this.isFunnel = function (selection) {
        return ngZone.runOutsideAngular(function () {
            return selection.classed('funnel');
        });
    };

    /**
     * Gets the name for this connection.
     *
     * @param {object} connection
     */
    this.formatConnectionName = function (connection) {
        return ngZone.runOutsideAngular(function () {
            if (!commonService.isBlank(connection.name)) {
                return connection.name;
            } else if (commonService.isDefinedAndNotNull(connection.selectedRelationships)) {
                return connection.selectedRelationships.join(', ');
            }
            return '';
        });
    };

    /**
     * Gets the name for this source/destination of a connection.
     *
     * @param {object} terminal
     */
    this.formatConnectionTerminalName = function (terminal) {
        return ngZone.runOutsideAngular(function () {
            if (!commonService.isBlank(terminal.name)) {
                return terminal.name;
            }
            return terminal.id;
        });
    };

    /**
     * Determines whether the components in the specified selection are deletable.
     *
     * @argument {selection} selection      The selection
     * @return {boolean}            Whether the selection is deletable
     */
    this.areDeletable = function (selection) {
        return ngZone.runOutsideAngular(function () {
            if (selection.empty()) {
                return false;
            }

            var isDeletable = true;
            selection.each(function () {
                if (!self.isDeletable(d3.select(this))) {
                    isDeletable = false;
                }
            });
            return isDeletable;
        });
    };

    /**
     * Determines whether the component in the specified selection is deletable.
     *
     * @argument {selection} selection      The selection
     * @return {boolean}            Whether the selection is deletable
     */
    this.isDeletable = function (selection) {
        return ngZone.runOutsideAngular(function () {
            if (selection.size() !== 1) {
                return false;
            }

            if (self.canModify(selection) === false) {
                return false;
            }

            return self.supportsModification(selection);
        });
    };

    /**
     * Determines whether the specified selection is configurable.
     *
     * @param selection
     */
    this.isConfigurable = function (selection) {
        return ngZone.runOutsideAngular(function () {
            // ensure the correct number of components are selected
            if (selection.size() !== 1) {
                if (selection.empty()) {
                    return false;
                }
            }
            if (self.canRead(selection) === false || self.canModify(selection) === false) {
                return false;
            }
            if (self.isFunnel(selection)) {
                return false;
            }

            return self.supportsModification(selection);
        });
    };

    /**
     * Determines whether the specified selection is has components.
     *
     * @param selection
     */
    this.hasComponents = function (selection) {
        return ngZone.runOutsideAngular(function () {
            // ensure the correct number of components are selected
            return selection.empty();
        });
    };

    /**
     * Determines whether the components in the specified selection are writable.
     *
     * @argument {selection} selection      The selection
     * @return {boolean}            Whether the selection is writable
     */
    this.canModify = function (selection) {
        return ngZone.runOutsideAngular(function () {
            var selectionSize = selection.size();
            var writableSize = selection.filter(function (d) {
                return d.permissions.canWrite;
            }).size();

            return selectionSize === writableSize;
        });
    };

    /**
     * Determines whether the components in the specified selection are readable.
     *
     * @argument {selection} selection      The selection
     * @return {boolean}            Whether the selection is readable
     */
    this.canRead = function (selection) {
        return ngZone.runOutsideAngular(function () {
            var selectionSize = selection.size();
            var readableSize = selection.filter(function (d) {
                return d.permissions.canRead;
            }).size();

            return selectionSize === readableSize;
        });
    };

    /**
     * Determines whether the specified selection is in a state to support modification.
     *
     * @argument {selection} selection      The selection
     */
    this.supportsModification = function (selection) {
        return ngZone.runOutsideAngular(function () {
            if (selection.size() !== 1) {
                return false;
            }

            // currently all components are configurable
            return true;
        });
    };

    /**
     * Calculates the perimeter point on a circle based on the specified anchor point.
     *
     * @param {object} p            The anchor point
     * @param {object} c            The circle details {cx: 0, cy: 0, r: 10}
     */
    this.getPerimeterPointCircle = function (p, circle) {
        return ngZone.runOutsideAngular(function () {
            var deltaX = p.x - circle.cx;
            var deltaY = p.y - circle.cy;
            var distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

            var offsetX = 0;
            var offsetY = 0;
            if (distance !== 0) {
                offsetX = (deltaX / distance) * circle.r;
                offsetY = (deltaY / distance) * circle.r;
            }

            return {
                'x': circle.cx + offsetX,
                'y': circle.cy + offsetY
            };
        });
    };

    /**
     * Calculates the point on the specified bounding box that is closest to the
     * specified anchor point.
     *
     * @param {object} p            The anchor point
     * @param {object} bBox         The bounding box
     */
    this.getPerimeterPointRectangle = function (p, bBox) {
        return ngZone.runOutsideAngular(function () {
            // calculate theta
            var theta = Math.atan2(bBox.height, bBox.width);

            // get the rectangle radius
            var xRadius = bBox.width / 2;
            var yRadius = bBox.height / 2;

            // get the center point
            var cx = bBox.x + xRadius;
            var cy = bBox.y + yRadius;

            // calculate alpha
            var dx = p.x - cx;
            var dy = p.y - cy;
            var alpha = Math.atan2(dy, dx);

            // normalize aphla into 0 <= alpha < 2 PI
            alpha = alpha % TWO_PI;
            if (alpha < 0) {
                alpha += TWO_PI;
            }

            // calculate beta
            var beta = (Math.PI / 2) - alpha;

            // detect the appropriate quadrant and return the point on the perimeter
            if ((alpha >= 0 && alpha < theta) || (alpha >= (TWO_PI - theta) && alpha < TWO_PI)) {
                // right quadrant
                return {
                    'x': bBox.x + bBox.width,
                    'y': cy + Math.tan(alpha) * xRadius
                };
            } else if (alpha >= theta && alpha < (Math.PI - theta)) {
                // bottom quadrant
                return {
                    'x': cx + Math.tan(beta) * yRadius,
                    'y': bBox.y + bBox.height
                };
            } else if (alpha >= (Math.PI - theta) && alpha < (Math.PI + theta)) {
                // left quadrant
                return {
                    'x': bBox.x,
                    'y': cy - Math.tan(alpha) * xRadius
                };
            } else {
                // top quadrant
                return {
                    'x': cx - Math.tan(beta) * yRadius,
                    'y': bBox.y
                };
            }
        });
    };

    /**
     * Shows a canvas tooltip.
     *
     * @param {Element} tip             The tooltip
     * @param {Element} target          The target of the tooltip
     * @param {Element} container       The container to calculate the offset for the target
     */
    this.showCanvasTooltip = function (tip, target, container) {
        var targetRect = target.getBoundingClientRect();
        var containerRect = container.getBoundingClientRect();

        var x = (targetRect.left - containerRect.left) + targetRect.width / 2;
        var y = (targetRect.top - containerRect.top) + targetRect.height + 15;

        tip.style.top = y + 'px';
        tip.style.left = x + 'px';
        tip.style.display = 'block';
    };

    /**
     * Hides a canvas tooltip.
     *
     * @param {Element} tip             The tooltip
     */
    this.hideCanvasTooltip = function (tip) {
        tip.style.display = 'none';
    };

    /**
     * Adds the specified tooltip to the specified target.
     *
     * @param {selection} tip           The tooltip
     * @param {selection} target        The target of the tooltip
     * @param {Element} container       The container to calculate the offset for the target
     */
    this.canvasTooltip = function (tip, target, container) {
        target
            .on('mouseenter', function () {
                self.showCanvasTooltip(tip.node(), target.node(), container);
            })
            .on('mouseleave', function () {
                self.hideCanvasTooltip(tip.node());
            });
    };

    /**
     * Determines if the component in the specified selection is a valid connection source.
     *
     * @param {selection} selection         The selection
     * @return {boolean} Whether the selection is a valid connection source
     */
    this.isValidConnectionSource = function (selection) {
        return ngZone.runOutsideAngular(function () {
            if (selection.size() !== 1) {
                return false;
            }

            // require read and write for a connection source since we'll need to read the source to obtain valid relationships, etc
            if (self.canRead(selection) === false || self.canModify(selection) === false) {
                return false;
            }

            // if this is a processor, ensure it has connections
            if (self.isProcessor(selection)) {
                var selectionData = selection.datum();
                return !commonService.isEmpty(selectionData.component.relationships);
            }

            return self.isRemoteProcessGroup(selection) || self.isFunnel(selection);
        });
    };

    /**
     * Determines if the component in the specified selection is a valid connection destination.
     *
     * @param {selection} selection         The selection
     * @return {boolean} Whether the selection is a valid connection destination
     */
    this.isValidConnectionDestination = function (selection) {
        return ngZone.runOutsideAngular(function () {
            if (selection.size() !== 1) {
                return false;
            }

            // require write for a connection destination
            if (self.canModify(selection) === false) {
                return false;
            }

            if (self.isRemoteProcessGroup(selection) || self.isFunnel(selection)) {
                return true;
            }

            // if processor, ensure it supports input
            if (self.isProcessor(selection)) {
                var destinationData = selection.datum();
                return destinationData.inputRequirement !== 'INPUT_FORBIDDEN';
            }
        });
    };

    /**
     * Applies single line ellipsis to the component in the specified selection if necessary.
     *
     * @param {selection} selection
     * @param {string} text
     */
    this.ellipsis = function (selection, text) {
        return ngZone.runOutsideAngular(function () {
            text = text.trim();
            var width = parseInt(selection.attr('width'), 10);
            var node = selection.node();

            // set the element text
            selection.text(text);

            // see if the field is too big for the field
            if (text.length > 0 && node.getSubStringLength(0, text.length - 1) > width) {
                // make some room for the ellipsis
                width -= 5;

                // determine the appropriate index
                var i = binarySearch(text.length, function (x) {
                    var length = node.getSubStringLength(0, x);
                    if (length > width) {
                        // length is too long, try the lower half
                        return -1;
                    } else if (length < width) {
                        // length is too short, try the upper half
                        return 1;
                    }
                    return 0;
                });

                // trim at the appropriate length and add ellipsis
                selection.text(text.substring(0, i) + String.fromCharCode(8230));
            }
        });
    };
};

CanvasUtilsService.prototype = {
    constructor: CanvasUtilsService
};

CanvasUtilsService.parameters = [
    CommonService,
    ngCore.NgZone
];

module.exports = CanvasUtilsService;
