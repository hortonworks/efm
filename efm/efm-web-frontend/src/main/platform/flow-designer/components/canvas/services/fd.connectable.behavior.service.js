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

var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var rxjs = require('rxjs/Subject');
var d3 = require('d3');

/**
 * ConnectableBehavior constructor.
 *
 * @param canvasUtilsService       The flow designer canvas utils service.
 * @constructor
 */
function ConnectableBehavior(canvasUtilsService) {

    var self = this;
    var connect;
    var origin;
    var canvasSvg;

    this.connected$ = new rxjs.Subject();

    /**
     * Determines if we want to allow adding connections in the current state:
     *
     * 1) When shift is down, we could be adding components to the current selection.
     * 2) When the selection box is visible, we are in the process of moving all the
     * components currently selected.
     * 3) When the drag selection box is visible, we are in the process or selecting components
     * using the selection box.
     *
     * @returns {boolean}
     */
    var allowConnection = function () {
        return !d3.event.shiftKey && canvasSvg.select('rect.drag-selection').empty() && canvasSvg.select('rect.component-selection').empty();
    };

    /**
     * Initialize connectable behavior.
     *
     * @param canvas
     * @param selfLoopConfig
     */
    this.init = function (canvas, selfLoopConfig) {
        canvasSvg = canvas.getCanvasSvg();

        // dragging behavior for the connector
        connect = d3.drag()
            .subject(function (d) {
                origin = d3.mouse(canvasSvg.node());
                return {
                    x: origin[0],
                    y: origin[1]
                };
            })
            .on('start', function (d) {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();

                // unselect the previous components
                canvas.getSelection().classed('selected', false);

                // mark the source component has selected
                var source = d3.select(this.parentNode).classed('selected', true).classed('hover', false);

                // mark this component as dragging and selected
                d3.select(this).classed('dragging', true);

                // mark the source of the drag
                var sourceData = source.datum();

                // start the drag line and insert it first to keep it on the bottom
                var position = d3.mouse(canvasSvg.node());
                canvasSvg.insert('path', ':first-child')
                    .datum({
                        'sourceId': sourceData.id,
                        'sourceRadius': sourceData.dimensions.width / 2,
                        'cx': sourceData.position.x + (sourceData.dimensions.width / 2),
                        'cy': sourceData.position.y + (sourceData.dimensions.height / 2)
                    })
                    .attrs({
                        'class': 'connector',
                        'd': function () {
                            return 'M' + position[0] + ' ' + position[1] + 'L' + position[0] + ' ' + position[1];
                        }
                    });

                // updates the location of the connection img
                d3.select(this).attr('transform', function () {
                    return 'translate(' + position[0] + ', ' + (position[1] + 20) + ')';
                });

                // re-append the image to keep it on top
                canvasSvg.node().appendChild(this);
            })
            .on('drag', function (d) {
                // updates the location of the connection img
                d3.select(this).attr('transform', function () {
                    return 'translate(' + d3.event.x + ', ' + (d3.event.y + 50) + ')';
                });

                var connector = canvasSvg.select('path.connector');
                var connectorData = connector.datum();
                var source = canvasSvg.select('#id-' + connectorData.sourceId);
                var sourceData = source.datum();

                // mark node's connectable if supported
                var destination = canvasSvg.select('g.hover').classed('connectable-destination', function () {
                    // ensure the mouse has moved at least 10px in any direction, it seems that
                    // when the drag event is trigger is not consistent between browsers. as a result
                    // some browser would trigger when the mouse hadn't moved yet which caused
                    // click and contextmenu events to appear like an attempt to connection the
                    // component to itself. requiring the mouse to have actually moved before
                    // checking the eligiblity of the destination addresses the issue
                    var mouseMovedEnough = Math.abs(origin[0] - d3.event.x) > 10 || Math.abs(origin[1] - d3.event.y) > 10;
                    var isValidConnectionDestination = canvasUtilsService.isValidConnectionDestination(d3.select(this));

                    var destinationNode = d3.select(this);
                    var destinationData = destinationNode.datum();
                    var isSelfFunnelLoop = sourceData.type === 'funnel' && sourceData.id === destinationData.id;

                    return mouseMovedEnough && isValidConnectionDestination && !isSelfFunnelLoop;
                });

                // update the drag line
                canvasSvg.select('path.connector').classed('connectable', function () {
                    if (destination.empty()) {
                        return false;
                    }

                    // if there is a potential destination, see if its connectable
                    return destination.classed('connectable-destination');
                }).attr('d', function (pathDatum) {
                    if (!destination.empty() && destination.classed('connectable-destination')) {
                        var destinationData = destination.datum();

                        // show the line preview as appropriate
                        if (pathDatum.sourceId === destinationData.id) {
                            var x = pathDatum.cx + pathDatum.sourceRadius;
                            var y = pathDatum.cy;
                            var componentOffset = pathDatum.sourceRadius;
                            var xOffset = selfLoopConfig.selfLoopXOffset;
                            var yOffset = selfLoopConfig.selfLoopYOffset;
                            return 'M' + x + ' ' + y + 'L' + (x + componentOffset + xOffset) + ' ' + (y - yOffset) + 'L' + (x + componentOffset + xOffset) + ' ' + (y + yOffset) + 'Z';
                        } else {
                            // calculate the position on the end component
                            var endCircle = {
                                'cx': destinationData.position.x + (destinationData.dimensions.width / 2),
                                'cy': destinationData.position.y + (destinationData.dimensions.height / 2),
                                'r': destinationData.dimensions.width / 2
                            };
                            var end = canvasUtilsService.getPerimeterPointCircle({
                                'x': pathDatum.cx,
                                'y': pathDatum.cy
                            }, endCircle);

                            // calculate the position on the start component
                            var start = canvasUtilsService.getPerimeterPointCircle({
                                'x': endCircle.cx,
                                'y': endCircle.cy
                            }, {
                                'cx': pathDatum.cx,
                                'cy': pathDatum.cy,
                                'r': pathDatum.sourceRadius
                            });

                            // direct line between components to provide a 'snap feel'
                            return 'M' + start.x + ' ' + start.y + 'L' + end.x + ' ' + end.y;
                        }
                    } else {
                        // calculate the position on the start component
                        var start = canvasUtilsService.getPerimeterPointCircle({
                            'x': d3.event.x,
                            'y': d3.event.y
                        }, {
                            'cx': pathDatum.cx,
                            'cy': pathDatum.cy,
                            'r': pathDatum.sourceRadius
                        });

                        return 'M' + start.x + ' ' + start.y + 'L' + d3.event.x + ' ' + d3.event.y;
                    }
                });
            })
            .on('end', function (d) {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();

                // get the add connect img
                var addConnect = d3.select(this);

                // get the connector, if it the current point is not over a new destination
                // the connector will be removed. otherwise it will be removed after the
                // connection has been configured/cancelled
                var connector = canvasSvg.select('path.connector');
                var connectorData = connector.datum();

                // get the destination
                var destination = canvasSvg.select('g.connectable-destination');

                // get the source to determine if we are still over it
                var source = canvasSvg.select('#id-' + connectorData.sourceId);
                var sourceData = source.datum();

                // we are not over a new destination
                if (destination.empty()) {
                    // get the mouse position relative to the source
                    var position = d3.mouse(source.node());

                    // if the position is outside the component, remove the add connect img
                    if (position[0] < 0 || position[0] > sourceData.dimensions.width || position[1] < 0 || position[1] > sourceData.dimensions.height) {
                        addConnect.remove();
                    } else {
                        // reset the add connect img by restoring the position and place in the DOM
                        addConnect.classed('dragging', false).attr('transform', function () {
                            return 'translate(' + d.origX + ', ' + d.origY + ')';
                        });
                        source.node().appendChild(this);
                    }

                    // remove the connector
                    connector.remove();
                } else {
                    // remove the add connect img
                    addConnect.remove();

                    // create the connection
                    var destinationData = destination.datum();

                    // create the connect request
                    var connectRequest = {
                        'source': sourceData,
                        'destination': destinationData
                    };

                    // if the source and destination are the same, include bend points
                    if (sourceData.id === destinationData.id) {
                        var bends = [];

                        var rightCenter = {
                            x: sourceData.position.x + (sourceData.dimensions.width),
                            y: sourceData.position.y + (sourceData.dimensions.height / 2)
                        };

                        var xOffset = selfLoopConfig.selfLoopXOffset;
                        var yOffset = selfLoopConfig.selfLoopYOffset;
                        bends.push({
                            'x': (rightCenter.x + xOffset),
                            'y': (rightCenter.y - yOffset)
                        });
                        bends.push({
                            'x': (rightCenter.x + xOffset),
                            'y': (rightCenter.y + yOffset)
                        });

                        connectRequest['bends'] = bends;
                    }

                    // the user wants to create a connection... how do we know when it's done?
                    self.connected$.next(connectRequest);
                }
            });
    };

    /**
     * Activates the connect behavior for the components in the specified selection.
     *
     * @param {selection} components
     */
    this.activate = function (components) {
        components
            .classed('connectable', true)
            .on('mouseenter.connectable', function (d) {
                if (allowConnection()) {
                    var selection = d3.select(this);

                    // ensure the current component supports connection source
                    if (canvasUtilsService.isValidConnectionSource(selection)) {
                        // see if theres already a connector rendered
                        var addConnect = canvasSvg.select('text.add-connect');
                        if (addConnect.empty()) {
                            var x = d.dimensions.width - 14;
                            var y = (d.dimensions.height / 2) + 9;

                            selection.append('text')
                                .datum({
                                    origX: x,
                                    origY: y
                                })
                                .call(connect)
                                .attrs({
                                    'class': 'add-connect',
                                    'transform': 'translate(' + x + ', ' + y + ')'
                                })
                                .text('\uf178');
                        }
                    }
                }
            })
            .on('mouseleave.connectable', function () {
                // conditionally remove the connector
                var addConnect = d3.select(this).select('text.add-connect');
                if (!addConnect.empty() && !addConnect.classed('dragging')) {
                    addConnect.remove();
                }
            })
            // Using mouseover/out to workaround chrome issue #122746
            .on('mouseover.connectable', function () {
                // mark that we are hovering when appropriate
                d3.select(this).classed('hover', function () {
                    return allowConnection();
                });
            })
            .on('mouseout.connectable', function () {
                // remove all hover related classes
                d3.select(this).classed('hover connectable-destination', false);
            });
    };

    /**
     * Deactivates the connect behavior for the components in the specified selection.
     *
     * @param {selection} components
     */
    this.deactivate = function (components) {
        components
            .classed('connectable', false)
            .on('mouseenter.connectable', null)
            .on('mouseleave.connectable', null)
            .on('mouseover.connectable', null)
            .on('mouseout.connectable', null);
    };

    /**
     * Removes the temp edge if creating shown.
     */
    this.removeTempEdge = function () {
        canvasSvg.select('path.connector').remove();
    };
};

ConnectableBehavior.prototype = {
    constructor: ConnectableBehavior
};

ConnectableBehavior.parameters = [
    CanvasUtilsService
];

module.exports = ConnectableBehavior;
