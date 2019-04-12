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
var $ = require('jquery');
var CommonService = require('@flow-designer/services/CommonService');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var MoveConnectionComponent = require('@flow-designer/components/flow-designer-move-connection');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var ngMaterial = require('@angular/material');

/**
 * ConnectionManager constructor.
 *
 * @param commonService            The common service.
 * @param canvasUtilsService       The canvas utils service.
 * @param fdsDialogService         The dialog service.
 * @param dialog                   The material dialog
 * @constructor
 */
function ConnectionManager(commonService, canvasUtilsService, fdsDialogService, dialog) {
    var self = this;
    var dialogRef = null;

    var canvas = null;
    var canvasSvg = null;
    var flowDesignerApi = null;
    var client = null;
    var contextMenu = null;
    var selectableBehavior = null;
    var deselectableBehavior = null;
    var quickSelectBehavior = null;

    // the dimensions for the connection label
    var dimensions = {
        width: 200
    };

    /**
     * Gets the position of the label for the specified connection.
     *
     * @param {type} connectionLabel      The connection label
     */
    var getLabelPosition = function (connectionLabel) {
        var d = connectionLabel.datum();

        var x, y;
        if (d.bends.length > 0) {
            var i = Math.min(Math.max(0, d.labelIndex), d.bends.length - 1);
            x = d.bends[i].x;
            y = d.bends[i].y;
        } else {
            x = (d.start.x + d.end.x) / 2;
            y = (d.start.y + d.end.y) / 2;
        }

        // offset to account for the label dimensions
        x -= (dimensions.width / 2);
        y -= (connectionLabel.attr('height') / 2);

        return {
            x: x,
            y: y
        };
    };

    // ----------------------------------
    // connections currently on the graph
    // ----------------------------------

    var connectionMap;

    // -----------------------------------------------------------
    // cache for components that are added/removed from the canvas
    // -----------------------------------------------------------

    var removedCache;
    var addedCache;

    // ---------------------
    // connection containers
    // ---------------------

    var connectionContainer;

    // ------------------------
    // line point drag behavior
    // ------------------------

    var bendPointDrag;
    var endpointDrag;

    // ------------------------------
    // connection label drag behavior
    // ------------------------------

    var labelDrag;

    // function for generating lines
    var lineGenerator = d3.line()
        .x(function (d) {
            return d.x;
        })
        .y(function (d) {
            return d.y;
        })
        .curve(d3.curveLinear);

    // --------------------------
    // privately scoped functions
    // --------------------------

    /**
     * Calculates the distance between the two points specified squared.
     *
     * @param {object} v        First point
     * @param {object} w        Second point
     */
    var distanceSquared = function (v, w) {
        return Math.pow(v.x - w.x, 2) + Math.pow(v.y - w.y, 2);
    };

    /**
     * Calculates the distance between the two points specified.
     *
     * @param {object} v        First point
     * @param {object} w        Second point
     */
    var distanceBetweenPoints = function (v, w) {
        return Math.sqrt(distanceSquared(v, w));
    };

    /**
     * Calculates the distance between the point and the line created by s1 and s2.
     *
     * @param {object} p            The point
     * @param {object} s1           Segment start
     * @param {object} s2           Segment end
     */
    var distanceToSegment = function (p, s1, s2) {
        var l2 = distanceSquared(s1, s2);
        if (l2 === 0) {
            return Math.sqrt(distanceSquared(p, s1));
        }

        var t = ((p.x - s1.x) * (s2.x - s1.x) + (p.y - s1.y) * (s2.y - s1.y)) / l2;
        if (t < 0) {
            return Math.sqrt(distanceSquared(p, s1));
        }
        if (t > 1) {
            return Math.sqrt(distanceSquared(p, s2));
        }

        return Math.sqrt(distanceSquared(p, {
            'x': s1.x + t * (s2.x - s1.x),
            'y': s1.y + t * (s2.y - s1.y)
        }));
    };

    /**
     * Calculates the index of the bend point that is nearest to the specified point.
     *
     * @param {object} p
     * @param {object} connectionData
     */
    var getNearestSegment = function (p, connectionData) {
        if (connectionData.bends.length === 0) {
            return 0;
        }

        var minimumDistance;
        var index;

        // line is comprised of start -> [bends] -> end
        var line = [connectionData.start].concat(connectionData.bends, [connectionData.end]);

        // consider each segment
        for (var i = 0; i < line.length; i++) {
            if (i + 1 < line.length) {
                var distance = distanceToSegment(p, line[i], line[i + 1]);
                if (commonService.isUndefined(minimumDistance) || distance < minimumDistance) {
                    minimumDistance = distance;
                    index = i;
                }
            }
        }

        return index;
    };

    /**
     * Sorts the specified connections according to the z index.
     *
     * @param {type} connections
     */
    var sort = function (connections) {
        connections.sort(function (a, b) {
            return a.zIndex === b.zIndex ? 0 : a.zIndex > b.zIndex ? 1 : -1;
        });
    };

    /**
     * Selects the connection elements against the current connection map.
     */
    var select = function () {
        return connectionContainer.selectAll('g.connection').data(connectionMap.values(), function (d) {
            return d.id;
        });
    };

    /**
     * Renders the connections in the specified selection.
     *
     * @param {selection} entered           The selection of connections to be rendered
     * @param {boolean} selected             Whether the element should be selected
     * @return the entered selection
     */
    var renderConnections = function (entered, selected) {
        if (entered.empty()) {
            return entered;
        }

        var connection = entered.append('g')
            .attrs({
                'id': function (d) {
                    return 'id-' + d.id;
                },
                'class': 'connection'
            })
            .classed('selected', selected);

        // create a connection between the two components
        connection.append('path')
            .attrs({
                'class': 'connection-path',
                'pointer-events': 'none'
            });

        // path to show when selection
        connection.append('path')
            .attrs({
                'class': 'connection-selection-path',
                'pointer-events': 'none'
            });

        // path to make selection easier
        connection.append('path')
            .attrs({
                'class': 'connection-path-selectable',
                'pointer-events': 'stroke'
            })
            .on('mousedown.selection', function () {
                var parent = d3.select(this.parentNode);

                // (de)select the connection when clicking the selectable path
                if (parent.classed('selected')) {
                    deselectableBehavior.deselect(parent);
                } else {
                    selectableBehavior.select(parent);
                }
            })
            .call(contextMenu.activate);

        return connection;
    };

    // determines whether the specified connection contains an unsupported relationship
    var hasUnavailableRelationship = function (d) {
        var unavailable = false;

        // verify each selected relationship is still available
        if (commonService.isDefinedAndNotNull(d.component.selectedRelationships) && d.sourceType === 'PROCESSOR') {
            // get the processor details
            var sourceProcessorId = self.getConnectionSourceComponentId(d);
            var sourceProcessorData = canvasSvg.select('#id-' + sourceProcessorId).datum();

            // see if the connection contains a selected relationship that the source processor does not support
            for (var i = 0; i < d.component.selectedRelationships.length && !unavailable; i++) {
                var selectedRelationship = d.component.selectedRelationships[i];
                var available = sourceProcessorData.component.relationships.find(function (availableRelationship) {
                    return availableRelationship.name === selectedRelationship;
                });

                if (commonService.isUndefined(available)) {
                    unavailable = true;
                }
            }
        }

        return unavailable;
    };

    /**
     * Determines if the specified type is a type of input port.
     *
     * @argument {string} type      The port type
     */
    var isInputPortType = function (type) {
        return type.indexOf('INPUT_PORT') >= 0;
    };

    /**
     * Determines if the specified type is a type of output port.
     *
     * @argument {string} type      The port type
     */
    var isOutputPortType = function (type) {
        return type.indexOf('OUTPUT_PORT') >= 0;
    };

    /**
     * Determines whether the terminal of the connection (source|destination) is
     * a group.
     *
     * @param {object} terminal
     */
    var isGroup = function (terminal) {
        return terminal.groupId !== canvas.getGroupId() && (isInputPortType(terminal.type) || isOutputPortType(terminal.type));
    };

    // gets the appropriate end marker
    var getEndMarker = function (d) {
        var marker = 'normal';

        if (d.permissions.canRead) {
            // if the connection has a relationship that is unavailable, mark it a ghost relationship
            if (hasUnavailableRelationship(d)) {
                marker = 'ghost';
            }
        } else {
            marker = 'unauthorized';
        }

        return 'url(#' + marker + ')';
    };

    // gets the appropriate drop shadow
    var getDropShadow = function (d) {
        return 'url(#component-drop-shadow)';
    };

    /**
     * Determines if the specified connection has a label.
     *
     * @param d             connection
     * @returns {boolean}   whether a label should be rendered
     */
    var hasConnectionLabel = function (d) {
        return d.sourceType === 'PROCESSOR' || d.sourceType === 'REMOTE_OUTPUT_PORT' ||
            d.destinationType === 'REMOTE_INPUT_PORT' || !commonService.isBlank(d.component.name);
    };

    // updates the specified connections
    var updateConnections = function (updated, options) {
        if (updated.empty()) {
            return;
        }

        var updatePath = true;
        var updateLabel = true;
        var transition = false;

        // extract the options if specified
        if (commonService.isDefinedAndNotNull(options)) {
            updatePath = commonService.isDefinedAndNotNull(options.updatePath) ? options.updatePath : updatePath;
            updateLabel = commonService.isDefinedAndNotNull(options.updateLabel) ? options.updateLabel : updateLabel;
            transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
        }

        if (updatePath === true) {
            updated
                .classed('grouped', function (d) {
                    var grouped = false;

                    if (d.permissions.canRead) {
                        // if there are more than one selected relationship, mark this as grouped
                        if (commonService.isDefinedAndNotNull(d.component.selectedRelationships) && d.component.selectedRelationships.length > 1) {
                            grouped = true;
                        }
                    }

                    return grouped;
                })
                .classed('ghost', function (d) {
                    var ghost = false;

                    if (d.permissions.canRead) {
                        // if the connection has a relationship that is unavailable, mark it a ghost relationship
                        if (hasUnavailableRelationship(d)) {
                            ghost = true;
                        }
                    }

                    return ghost;
                });

            // update connection path
            updated.select('path.connection-path')
                .classed('unauthorized', function (d) {
                    return d.permissions.canRead === false;
                });

            // update connection behavior
            updated.select('path.connection-path-selectable')
                .on('dblclick', function (d) {
                    if (d.permissions.canWrite && d.permissions.canRead) {
                        var position = d3.mouse(this.parentNode);

                        // find where to put this bend point
                        var bendIndex = getNearestSegment({
                            'x': position[0],
                            'y': position[1]
                        }, d);

                        // copy the original to restore if necessary
                        var bends = d.component.bends.slice();

                        // add it to the collection of points
                        bends.splice(bendIndex, 0, {
                            'x': position[0],
                            'y': position[1]
                        });

                        var connection = {
                            id: d.id,
                            bends: bends
                        };

                        // update the label index if necessary
                        var labelIndex = d.component.labelIndex;
                        if (bends.length === 1) {
                            connection.labelIndex = 0;
                        } else if (bendIndex <= labelIndex) {
                            connection.labelIndex = labelIndex + 1;
                        }

                        // save the new state
                        save(d, connection);

                        d3.event.stopPropagation();
                    } else {
                        return null;
                    }
                });
        }

        updated.each(function (d) {
            var connection = d3.select(this);

            if (updatePath === true) {
                // calculate the start and end points
                var sourceComponentId = self.getConnectionSourceComponentId(d);
                var sourceData = canvasSvg.select('#id-' + sourceComponentId).datum();
                var end;

                // get the appropriate end anchor point
                var endAnchor;
                if (d.bends.length > 0) {
                    endAnchor = d.bends[d.bends.length - 1];
                } else {
                    endAnchor = {
                        x: sourceData.position.x + (sourceData.dimensions.width / 2),
                        y: sourceData.position.y + (sourceData.dimensions.height / 2)
                    };
                }

                // if we are currently dragging the endpoint to a new target, use that
                // position, otherwise we need to calculate it for the current target
                if (commonService.isDefinedAndNotNull(d.end) && d.end.dragging === true) {
                    // since we're dragging, use the same object thats bound to the endpoint drag event
                    end = d.end;

                    // if we're not over a connectable destination use the current point
                    var newDestination = canvasSvg.select('g.hover.connectable-destination');
                    if (!newDestination.empty()) {
                        var newDestinationData = newDestination.datum();

                        // get the position on the new destination perimeter
                        var newEnd = canvasUtilsService.getPerimeterPointCircle(endAnchor, {
                            'cx': newDestinationData.position.x + (newDestinationData.dimensions.width / 2),
                            'cy': newDestinationData.position.y + (newDestinationData.dimensions.height / 2),
                            'r': newDestinationData.dimensions.width / 2
                        });

                        // update the coordinates with the new point
                        end.x = newEnd.x;
                        end.y = newEnd.y;
                    }
                } else {
                    var destinationComponentId = self.getConnectionDestinationComponentId(d);
                    var destinationData = canvasSvg.select('#id-' + destinationComponentId).datum();

                    // get the position on the destination perimeter
                    end = canvasUtilsService.getPerimeterPointCircle(endAnchor, {
                        'cx': destinationData.position.x + (destinationData.dimensions.width / 2),
                        'cy': destinationData.position.y + (destinationData.dimensions.height / 2),
                        'r': destinationData.dimensions.width / 2
                    });
                }

                // get the appropriate start anchor point
                var startAnchor;
                if (d.bends.length > 0) {
                    startAnchor = d.bends[0];
                } else {
                    startAnchor = end;
                }

                // get the position on the source perimeter
                var start = canvasUtilsService.getPerimeterPointCircle(startAnchor, {
                    'cx': sourceData.position.x + (sourceData.dimensions.width / 2),
                    'cy': sourceData.position.y + (sourceData.dimensions.height / 2),
                    'r': sourceData.dimensions.width / 2
                });

                // store the updated endpoints
                d.start = start;
                d.end = end;

                // update the connection paths
                canvasUtilsService.transition(connection.select('path.connection-path'), transition)
                    .attrs({
                        'd': function () {
                            var datum = [d.start].concat(d.bends, [d.end]);
                            return lineGenerator(datum);
                        }
                    });
                canvasUtilsService.transition(connection.select('path.connection-selection-path'), transition)
                    .attrs({
                        'd': function () {
                            var datum = [d.start].concat(d.bends, [d.end]);
                            return lineGenerator(datum);
                        }
                    });
                canvasUtilsService.transition(connection.select('path.connection-path-selectable'), transition)
                    .attrs({
                        'd': function () {
                            var datum = [d.start].concat(d.bends, [d.end]);
                            return lineGenerator(datum);
                        }
                    });

                // -----
                // bends
                // -----

                var startpoints = connection.selectAll('rect.startpoint');
                var endpoints = connection.selectAll('rect.endpoint');
                var midpoints = connection.selectAll('rect.midpoint');

                // require read and write permissions as it's required to read the connections available relationships
                // when connecting to a group or remote group
                if (d.permissions.canWrite && d.permissions.canRead) {

                    // ------------------
                    // bends - startpoint
                    // ------------------

                    startpoints = startpoints.data([d.start]);

                    // create a point for the start
                    var startpointsEntered = startpoints.enter().append('rect')
                        .attrs({
                            'class': 'startpoint linepoint',
                            'pointer-events': 'all',
                            'width': 8,
                            'height': 8
                        })
                        .on('mousedown.selection', function () {
                            var parent = d3.select(this.parentNode);

                            // (de)select the connection when clicking the start point
                            if (parent.classed('selected')) {
                                deselectableBehavior.deselect(parent);
                            } else {
                                selectableBehavior.select(parent);
                            }
                        })
                        .call(contextMenu.activate);

                    // update the start point
                    canvasUtilsService.transition(startpoints.merge(startpointsEntered), transition)
                        .attr('transform', function (p) {
                            return 'translate(' + (p.x - 4) + ', ' + (p.y - 4) + ')';
                        });

                    // remove old items
                    startpoints.exit().remove();

                    // ----------------
                    // bends - endpoint
                    // ----------------

                    var endpoints = endpoints.data([d.end]);

                    // create a point for the end
                    var endpointsEntered = endpoints.enter().append('rect')
                        .attrs({
                            'class': 'endpoint linepoint',
                            'pointer-events': 'all',
                            'width': 8,
                            'height': 8
                        })
                        .on('mousedown.selection', function () {
                            var parent = d3.select(this.parentNode);

                            // (de)select the connection when clicking the end point
                            if (parent.classed('selected')) {
                                deselectableBehavior.deselect(parent);
                            } else {
                                selectableBehavior.select(parent);
                            }
                        })
                        .call(endpointDrag)
                        .call(contextMenu.activate);

                    // update the end point
                    canvasUtilsService.transition(endpoints.merge(endpointsEntered), transition)
                        .attr('transform', function (p) {
                            return 'translate(' + (p.x - 4) + ', ' + (p.y - 4) + ')';
                        });

                    // remove old items
                    endpoints.exit().remove();

                    // -----------------
                    // bends - midpoints
                    // -----------------

                    var midpoints = midpoints.data(d.bends);

                    // create a point for the end
                    var midpointsEntered = midpoints.enter().append('rect')
                        .attrs({
                            'class': 'midpoint linepoint',
                            'pointer-events': 'all',
                            'width': 8,
                            'height': 8
                        })
                        .on('mousedown.selection', function () {
                            var parent = d3.select(this.parentNode);

                            // (de)select the connection when clicking a bend
                            if (parent.classed('selected')) {
                                deselectableBehavior.deselect(parent);
                            } else {
                                selectableBehavior.select(parent);
                            }
                        })
                        .on('dblclick', function (p) {
                            // stop even propagation
                            d3.event.stopPropagation();

                            var connection = d3.select(this.parentNode);
                            var connectionData = connection.datum();

                            // if this is a self loop prevent removing the last two bends
                            var sourceComponentId = self.getConnectionSourceComponentId(connectionData);
                            var destinationComponentId = self.getConnectionDestinationComponentId(connectionData);
                            if (sourceComponentId === destinationComponentId && d.component.bends.length <= 2) {
                                dialogRef = fdsDialogService.openConfirm({
                                    title: 'Connection',
                                    message: 'Looping connections must have at least two bend points.',
                                    acceptButton: 'Ok'
                                });
                                return;
                            }

                            var newBends = [];
                            var bendIndex = -1;

                            // create a new array of bends without the selected one
                            $.each(connectionData.component.bends, function (i, bend) {
                                if (p.x !== bend.x && p.y !== bend.y) {
                                    newBends.push(bend);
                                } else {
                                    bendIndex = i;
                                }
                            });

                            if (bendIndex < 0) {
                                return;
                            }

                            var connection = {
                                id: connectionData.id,
                                bends: newBends
                            };

                            // update the label index if necessary
                            var labelIndex = connectionData.component.labelIndex;
                            if (newBends.length <= 1) {
                                connection.labelIndex = 0;
                            } else if (bendIndex <= labelIndex) {
                                connection.labelIndex = Math.max(0, labelIndex - 1);
                            }

                            // save the updated connection
                            save(connectionData, connection);
                        })
                        .call(bendPointDrag)
                        .call(contextMenu.activate);

                    // update the midpoints
                    canvasUtilsService.transition(midpoints.merge(midpointsEntered), transition)
                        .attr('transform', function (p) {
                            return 'translate(' + (p.x - 4) + ', ' + (p.y - 4) + ')';
                        });

                    // remove old items
                    midpoints.exit().remove();
                } else {
                    // remove the start, mid, and end points
                    startpoints.remove();
                    endpoints.remove();
                    midpoints.remove();
                }
            }

            // only update the label if necessary
            if (updateLabel === true) {
                var connectionLabelContainer = connection.select('g.connection-label-container');

                // update visible connections
                if (connection.classed('visible')) {

                    // check if this connection requires a label
                    if (hasConnectionLabel(d)) {

                        // if there is no connection label this connection is becoming
                        // visible so we need to render it
                        if (connectionLabelContainer.empty()) {
                            // connection label container
                            connectionLabelContainer = connection.insert('g', 'rect.startpoint')
                                .attrs({
                                    'class': 'connection-label-container',
                                    'pointer-events': 'all'
                                })
                                .on('mousedown.selection', function () {
                                    var parent = d3.select(this.parentNode);

                                    // (de)select the connection when clicking the label
                                    if (parent.classed('selected')) {
                                        deselectableBehavior.deselect(parent);
                                    } else {
                                        selectableBehavior.select(parent);
                                    }
                                })
                                .on('dblclick', function () {
                                    quickSelectBehavior.quickSelect(d3.select(this.parentNode));
                                })
                                .call(contextMenu.activate);

                            // connection label
                            connectionLabelContainer.append('rect')
                                .attrs({
                                    'class': 'body',
                                    'width': dimensions.width,
                                    'x': 0,
                                    'y': 0
                                });

                            // processor border
                            connectionLabelContainer.append('rect')
                                .attrs({
                                    'class': 'border',
                                    'width': dimensions.width,
                                    'fill': 'transparent',
                                    'stroke': 'transparent'
                                });
                        }

                        var labelCount = 0;
                        var rowHeight = 19;

                        var connectionFrom = connectionLabelContainer.select('g.connection-from-container');
                        var connectionTo = connectionLabelContainer.select('g.connection-to-container');
                        var connectionName = connectionLabelContainer.select('g.connection-name-container');

                        if (d.permissions.canRead) {

                            // -----------------------
                            // connection label - from
                            // -----------------------

                            // determine if the connection require a from label
                            if (isGroup(d.component.source)) {
                                var sourceName = canvasUtilsService.formatConnectionTerminalName(d.component.source);

                                // see if the connection from label is already rendered
                                if (connectionFrom.empty()) {
                                    connectionFrom = connectionLabelContainer.append('g')
                                        .attrs({
                                            'class': 'connection-from-container'
                                        });

                                    connectionFrom.append('text')
                                        .attrs({
                                            'class': 'stats-label',
                                            'x': 5,
                                            'y': 14
                                        })
                                        .text('From');

                                    connectionFrom.append('text')
                                        .attrs({
                                            'class': 'stats-value connection-from',
                                            'x': 43,
                                            'y': 14,
                                            'width': 130
                                        });
                                }

                                // update the connection from positioning
                                connectionFrom.attr('transform', function () {
                                    var y = (rowHeight * labelCount++);
                                    return 'translate(0, ' + y + ')';
                                });

                                // update the label text
                                connectionFrom.select('text.connection-from')
                                    .each(function () {
                                        var connectionFromLabel = d3.select(this);

                                        // reset the label name to handle any previous state
                                        connectionFromLabel.text(null).selectAll('title').remove();

                                        // apply ellipsis to the label as necessary
                                        canvasUtilsService.ellipsis(connectionFromLabel, sourceName);
                                    })
                                    .append('title').text(function () {
                                        return sourceName;
                                    });
                            } else {
                                // there is no connection from, remove the previous if necessary
                                connectionFrom.remove();
                            }

                            // ---------------------
                            // connection label - to
                            // ---------------------

                            // determine if the connection require a to label
                            if (isGroup(d.component.destination)) {
                                var destinationName = canvasUtilsService.formatConnectionTerminalName(d.component.destination);

                                // see if the connection to label is already rendered
                                if (connectionTo.empty()) {
                                    connectionTo = connectionLabelContainer.append('g')
                                        .attrs({
                                            'class': 'connection-to-container'
                                        });

                                    connectionTo.append('text')
                                        .attrs({
                                            'class': 'stats-label',
                                            'x': 5,
                                            'y': 14
                                        })
                                        .text('To');

                                    connectionTo.append('text')
                                        .attrs({
                                            'class': 'stats-value connection-to',
                                            'x': 25,
                                            'y': 14,
                                            'width': 145
                                        });
                                }

                                // update the connection to positioning
                                connectionTo.attr('transform', function () {
                                    var y = (rowHeight * labelCount++);
                                    return 'translate(0, ' + y + ')';
                                });

                                // update the label text
                                connectionTo.select('text.connection-to')
                                    .each(function (d) {
                                        var connectionToLabel = d3.select(this);

                                        // reset the label name to handle any previous state
                                        connectionToLabel.text(null).selectAll('title').remove();

                                        // apply ellipsis to the label as necessary
                                        canvasUtilsService.ellipsis(connectionToLabel, destinationName);
                                    })
                                    .append('title').text(function (d) {
                                        return destinationName;
                                    });
                            } else {
                                // there is no connection to, remove the previous if necessary
                                connectionTo.remove();
                            }

                            // -----------------------
                            // connection label - name
                            // -----------------------

                            // get the connection name
                            var connectionNameValue = canvasUtilsService.formatConnectionName(d.component);

                            // is there a name to render
                            if (!commonService.isBlank(connectionNameValue)) {
                                // see if the connection name label is already rendered
                                if (connectionName.empty()) {
                                    connectionName = connectionLabelContainer.append('g')
                                        .attrs({
                                            'class': 'connection-name-container'
                                        });

                                    connectionName.append('text')
                                        .attrs({
                                            'class': 'stats-label',
                                            'x': 5,
                                            'y': 14
                                        })
                                        .text('Name');

                                    connectionName.append('text')
                                        .attrs({
                                            'class': 'stats-value connection-name',
                                            'x': 45,
                                            'y': 14,
                                            'width': 142
                                        });
                                }

                                // update the connection name positioning
                                connectionName.attr('transform', function () {
                                    var y = (rowHeight * labelCount++);
                                    return 'translate(0, ' + y + ')';
                                });

                                // update the connection name
                                connectionName.select('text.connection-name')
                                    .each(function () {
                                        var connectionToLabel = d3.select(this);

                                        // reset the label name to handle any previous state
                                        connectionToLabel.text(null).selectAll('title').remove();

                                        // apply ellipsis to the label as necessary
                                        canvasUtilsService.ellipsis(connectionToLabel, connectionNameValue);
                                    })
                                    .append('title').text(function () {
                                    return connectionNameValue;
                                });
                            } else {
                                // there is no connection name, remove the previous if necessary
                                connectionName.remove();
                            }
                        } else {
                            // no permissions to read to remove previous if necessary
                            connectionFrom.remove();
                            connectionTo.remove();
                            connectionName.remove();
                        }

                        var HEIGHT_FOR_BACKPRESSURE = 3;

                        // update the height based on the labels being rendered
                        connectionLabelContainer.select('rect.body')
                            .attr('height', function () {
                                return (rowHeight * labelCount) + HEIGHT_FOR_BACKPRESSURE;
                            })
                            .classed('unauthorized', function () {
                                return d.permissions.canRead === false;
                            });
                        connectionLabelContainer.select('rect.border')
                            .attr('height', function () {
                                return (rowHeight * labelCount) + HEIGHT_FOR_BACKPRESSURE;
                            })
                            .classed('unauthorized', function () {
                                return d.permissions.canRead === false;
                            });

                        if (d.permissions.canWrite) {
                            // only support dragging the label when appropriate
                            connectionLabelContainer.call(labelDrag);
                        }
                    } else {
                        // no connection label, remove it's container
                        connectionLabelContainer.remove();
                    }

                    // update the connection status
                    connection.call(updateConnectionStatus);
                } else {
                    if (!connectionLabelContainer.empty()) {
                        connectionLabelContainer.remove();
                    }
                }
            }

            // update the position of the label if possible
            canvasUtilsService.transition(connection.select('g.connection-label-container'), transition)
                .attr('transform', function () {
                    var label = d3.select(this).select('rect.body');
                    var position = getLabelPosition(label);
                    return 'translate(' + position.x + ', ' + position.y + ')';
                });
        });
    };

    /**
     * Updates the stats of the connections in the specified selection.
     *
     * @param {selection} updated       The selected connections to update
     */
    var updateConnectionStatus = function (updated) {
        if (updated.empty()) {
            return;
        }

        // connection stroke
        updated.select('path.connection-path')
            .attrs({
                'marker-end': getEndMarker
            });

        // drop shadow
        updated.select('rect.body')
            .attrs({
                'filter': getDropShadow
            });
    };

    /**
     * Saves the connection entry specified by d with the new configuration specified
     * in connection.
     *
     * @param {type} d
     * @param {type} connection
     */
    var save = function (d, connection) {
        var entity = {
            'id': d.id,
            'revision': client.getRevision(d),
            'component': connection
        };

        var updateConnection$ = flowDesignerApi.updateConnection(entity);
        updateConnection$.subscribe(function (response) {
            self.set(response);

            // update version info
            canvas.refreshVersionInfo();
        }, function (errorResponse) {
        });

        return updateConnection$;
    };

    // removes the specified connections
    var removeConnections = function (removed) {
        // consider reloading source/destination of connection being removed
        removed.each(function (d) {
            var srcComponentId = self.getConnectionSourceComponentId(d);
            var destComponentId = self.getConnectionDestinationComponentId(d);

            canvas.graph.reloadConnectionSourceAndDestination(srcComponentId, destComponentId);
        });

        // remove the connection
        removed.remove();
    };

    /**
     * Initialize the connection service
     *
     * @param canvasRef                  The canvas
     *
     * @returns {*}
     */
    this.init = function (canvasRef, flowDesignerCanvasComponent) {
        canvas = canvasRef;
        canvasSvg = canvas.getCanvasSvg();
        flowDesignerApi = canvas.flowDesignerApi;
        client = canvas.client;
        contextMenu = canvas.contextMenu;
        selectableBehavior = canvas.selectableBehavior;
        deselectableBehavior = canvas.deselectableBehavior;
        quickSelectBehavior = canvas.quickSelectBehavior;

        connectionMap = d3.map();
        removedCache = d3.map();
        addedCache = d3.map();

        // create the connection container
        connectionContainer = canvasSvg.append('g')
            .attrs({
                'pointer-events': 'stroke',
                'class': 'connections'
            });

        // initial bend point drag
        bendPointDrag = d3.drag()
            .on('start', function () {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            })
            .on('drag', function (d) {
                d.x = d3.event.x;
                d.y = d3.event.y;

                // redraw this connection
                d3.select(this.parentNode).call(updateConnections, {
                    'updatePath': true,
                    'updateLabel': false
                });
            })
            .on('end', function () {
                var connection = d3.select(this.parentNode);
                var connectionData = connection.datum();
                var bends = connection.selectAll('rect.midpoint').data();

                // ensure the bend lengths are the same
                if (bends.length === connectionData.component.bends.length) {
                    // determine if the bend points have moved
                    var different = false;
                    for (var i = 0; i < bends.length && !different; i++) {
                        if (bends[i].x !== connectionData.component.bends[i].x || bends[i].y !== connectionData.component.bends[i].y) {
                            different = true;
                        }
                    }

                    // only save the updated bends if necessary
                    if (different) {
                        var updateConnection$ = save(connectionData, {
                            id: connectionData.id,
                            bends: bends
                        });

                        updateConnection$.subscribe(function () {
                        }, function (failed) {
                            // restore the previous bend points
                            connectionData.bends = $.map(connectionData.component.bends, function (bend) {
                                return {
                                    x: bend.x,
                                    y: bend.y
                                };
                            });

                            // refresh the connection
                            connection.call(updateConnections, {
                                'updatePath': true,
                                'updateLabel': false
                            });
                        });
                    }
                }

                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            });

        // initial endpoint drag
        endpointDrag = d3.drag()
            .on('start', function (d) {
                // indicate that dragging has begun
                d.dragging = true;

                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            })
            .on('drag', function (d) {
                d.x = d3.event.x - 8;
                d.y = d3.event.y - 8;

                // ensure the new destination is valid
                canvasSvg.select('g.hover').classed('connectable-destination', function () {
                    return canvasUtilsService.isValidConnectionDestination(d3.select(this));
                });

                // redraw this connection
                d3.select(this.parentNode).call(updateConnections, {
                    'updatePath': true,
                    'updateLabel': false
                });
            })
            .on('end', function (d) {
                // indicate that dragging as stopped
                d.dragging = false;

                // get the corresponding connection
                var connection = d3.select(this.parentNode);
                var connectionData = connection.datum();
                var previousDestinationComponentId = self.getConnectionDestinationComponentId(connectionData);

                // get the current source
                var sourceComponentId = self.getConnectionSourceComponentId(connectionData);
                var sourceData = canvasSvg.select('#id-' + sourceComponentId).datum();

                // attempt to select a new destination
                var destination = canvasSvg.select('g.connectable-destination');

                // resets the connection if we're not over a new destination
                if (destination.empty()) {
                    connection.call(updateConnections, {
                        'updatePath': true,
                        'updateLabel': false
                    });
                } else {
                    var destinationData = destination.datum();

                    var reset = function () {
                        // reset the connection
                        connection.call(updateConnections, {
                            'updatePath': true,
                            'updateLabel': false
                        });
                    };

                    // prompt for the new port if appropriate
                    if (canvasUtilsService.isRemoteProcessGroup(destination)) {

                        flowDesignerCanvasComponent.disableHotKeys();
                        dialogRef = dialog.open(MoveConnectionComponent, {
                            width: '40%'
                        });

                        dialogRef.componentInstance.connectionEntity = connectionData;
                        dialogRef.componentInstance.destinationEntity = destinationData;

                        // add a reaction to the dialog afterClosed to enable closing dialog with 'esc' or by clicking off of the dialog
                        dialogRef.beforeClose()
                            .takeUntil(dialogRef.componentInstance.componentDestroyed$)
                            .subscribe(function () {
                                // cancel move connection
                                dialogRef.componentInstance.cancel();
                                flowDesignerCanvasComponent.enableHotKeys();
                            });

                        // react to request to move a connection to a new destination
                        dialogRef.componentInstance.subject$
                            .subscribe(function (configuredConnectionEntity) {
                                var revision = client.getRevision(connectionData);

                                // update the connection
                                flowDesignerApi.updateConnection($.extend({
                                    'revision': revision
                                }, configuredConnectionEntity))
                                    .subscribe(function (response) {
                                        // update the graph
                                        self.set(response);

                                        // reload the previous destination
                                        canvas.graph.reloadConnectionSourceAndDestination(null, previousDestinationComponentId);

                                        // update version info
                                        canvas.refreshVersionInfo();

                                        // complete the connection creation subject which will trigger dialog closing
                                        dialogRef.componentInstance.subject$.complete();
                                    }, function (errorResponse) {
                                        if (errorResponse.preventDefault) {
                                            dialogRef.close();
                                        }
                                    });
                            }, function () {
                                // configuration was cancelled, reset the connection
                                reset();

                                // close the dialog
                                dialogRef.close();
                            }, function () {
                                // close the dialog
                                dialogRef.close();
                            });
                    } else {
                        // get the destination type
                        var destinationType = commonService.getConnectableTypeForDestination(destinationData);

                        var connectionEntity = {
                            'id': connectionData.id,
                            'revision': client.getRevision(connectionData),
                            'component': {
                                'id': connectionData.id,
                                'destination': {
                                    'id': destinationData.id,
                                    'groupId': canvas.getGroupId(),
                                    'type': destinationType
                                }
                            }
                        };

                        // if this is a self loop and there are less than 2 bends, add them
                        if (connectionData.bends.length < 2 && connectionData.sourceId === destinationData.id) {
                            var rightCenter = {
                                x: destinationData.position.x + (destinationData.dimensions.width),
                                y: destinationData.position.y + (destinationData.dimensions.height / 2)
                            };
                            var xOffset = self.config.selfLoopXOffset;
                            var yOffset = self.config.selfLoopYOffset;

                            connectionEntity.component.bends = [];
                            connectionEntity.component.bends.push({
                                'x': (rightCenter.x + xOffset),
                                'y': (rightCenter.y - yOffset)
                            });
                            connectionEntity.component.bends.push({
                                'x': (rightCenter.x + xOffset),
                                'y': (rightCenter.y + yOffset)
                            });
                        }

                        // update the connection
                        flowDesignerApi.updateConnection(connectionEntity)
                            .subscribe(function (response) {
                                // refresh to update the label
                                self.set(response);

                                // reload the previous destination and the new source/destination
                                canvas.graph.reloadConnectionSourceAndDestination(null, previousDestinationComponentId);

                                var srcComponentId = self.getConnectionSourceComponentId(response);
                                var destComponentId = self.getConnectionDestinationComponentId(response);

                                canvas.graph.reloadConnectionSourceAndDestination(srcComponentId, destComponentId);

                                // update version info
                                canvas.refreshVersionInfo();
                            }, function (error) {
                                // reset the connection
                                reset();
                            });
                    }
                }

                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            });

        // initial label drag
        labelDrag = d3.drag()
            .on('start', function (d) {
                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            })
            .on('drag', function (d) {
                if (d.bends.length > 1) {
                    // get the dragged component
                    var drag = canvasSvg.select('rect.label-drag');

                    // lazily create the drag selection box
                    if (drag.empty()) {
                        var connectionLabel = d3.select(this).select('rect.body');

                        var position = getLabelPosition(connectionLabel);
                        var width = dimensions.width;
                        var height = connectionLabel.attr('height');

                        // create a selection box for the move
                        drag = canvasSvg.append('rect')
                            .attr('x', position.x)
                            .attr('y', position.y)
                            .attr('class', 'label-drag')
                            .attr('width', width)
                            .attr('height', height)
                            .attr('stroke-width', function () {
                                return 1 / canvas.view.getScale();
                            })
                            .attr('stroke-dasharray', function () {
                                return 4 / canvas.view.getScale();
                            })
                            .datum({
                                x: position.x,
                                y: position.y,
                                width: width,
                                height: height
                            });
                    } else {
                        // update the position of the drag selection
                        drag.attr('x', function (d) {
                            d.x += d3.event.dx;
                            return d.x;
                        })
                            .attr('y', function (d) {
                                d.y += d3.event.dy;
                                return d.y;
                            });
                    }

                    // calculate the current point
                    var datum = drag.datum();
                    var currentPoint = {
                        x: datum.x + (datum.width / 2),
                        y: datum.y + (datum.height / 2)
                    };

                    var closestBendIndex = -1;
                    var minDistance;
                    $.each(d.bends, function (i, bend) {
                        var bendPoint = {
                            'x': bend.x,
                            'y': bend.y
                        };

                        // get the distance
                        var distance = distanceBetweenPoints(currentPoint, bendPoint);

                        // see if its the minimum
                        if (closestBendIndex === -1 || distance < minDistance) {
                            closestBendIndex = i;
                            minDistance = distance;
                        }
                    });

                    // record the closest bend
                    d.labelIndex = closestBendIndex;

                    // refresh the connection
                    d3.select(this.parentNode).call(updateConnections, {
                        'updatePath': true,
                        'updateLabel': false
                    });
                }
            })
            .on('end', function (d) {
                if (d.bends.length > 1) {
                    // get the drag selection
                    var drag = canvasSvg.select('rect.label-drag');

                    // ensure we found a drag selection
                    if (!drag.empty()) {
                        // remove the drag selection
                        drag.remove();
                    }

                    // only save if necessary
                    if (d.labelIndex !== d.component.labelIndex) {
                        // get the connection to refresh below
                        var connection = d3.select(this.parentNode);

                        // save the new label index
                        var updateConnection$ = save(d, {
                            id: d.id,
                            labelIndex: d.labelIndex
                        });

                        updateConnection$.subscribe(function () {
                        }, function () {
                            // restore the previous label index
                            d.labelIndex = d.component.labelIndex;

                            // refresh the connection
                            connection.call(updateConnections, {
                                'updatePath': true,
                                'updateLabel': false
                            });
                        });
                    }
                }

                // stop further propagation
                d3.event.sourceEvent.stopPropagation();
            });
    };

    /**
     * Adds the specified connection entity.
     *
     * @param connectionEntities       The connection
     * @param options           Configuration options
     */
    this.add = function (connectionEntities, options) {
        var selectAll = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
        }

        // get the current time
        var now = new Date().getTime();

        var add = function (connectionEntity) {
            addedCache.set(connectionEntity.id, now);

            // need to clone the bends due to how we handle the midpoint drag
            connectionEntity.bends = $.map(connectionEntity.bends, function (bend) {
                return $.extend({}, bend);
            });

            // add the connection
            connectionMap.set(connectionEntity.id, $.extend(connectionEntity, {
                type: 'connection'
            }));
        };

        // determine how to handle the specified connection
        if ($.isArray(connectionEntities)) {
            $.each(connectionEntities, function (_, connectionEntity) {
                add(connectionEntity);
            });
        } else if (commonService.isDefinedAndNotNull(connectionEntities)) {
            add(connectionEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderConnections(selection.enter(), selectAll);

        // update
        var updated = selection.merge(entered);
        updated.call(updateConnections, {
            'updatePath': true,
            'updateLabel': false
        }).call(sort);
    };

    /**
     * Populates the graph with the specified connections.
     *
     * @argument {object | array} connectionEntities               The connections to add
     * @argument {object} options                Configuration options
     */
    this.set = function (connectionEntities, options) {
        var selectAll = false;
        var transition = false;
        var overrideRevisionCheck = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
            transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
            overrideRevisionCheck = commonService.isDefinedAndNotNull(options.overrideRevisionCheck) ? options.overrideRevisionCheck : overrideRevisionCheck;
        }

        var set = function (proposedConnectionEntity) {
            var currentConnectionEntity = connectionMap.get(proposedConnectionEntity.id);

            // set the connection if appropriate due to revision and wasn't previously removed
            if ((client.isNewerRevision(currentConnectionEntity, proposedConnectionEntity) && !removedCache.has(proposedConnectionEntity.id)) || overrideRevisionCheck === true) {
                // need to clone the bends due to how we handle the midpoint drag
                proposedConnectionEntity.bends = $.map(proposedConnectionEntity.bends, function (bend) {
                    return $.extend({}, bend);
                });

                connectionMap.set(proposedConnectionEntity.id, $.extend(proposedConnectionEntity, {
                    type: 'connection'
                }));
            }
        };

        // determine how to handle the specified connection
        if ($.isArray(connectionEntities)) {
            $.each(connectionMap.keys(), function (_, key) {
                var currentConnectionEntity = connectionMap.get(key);
                var isPresent = $.grep(connectionEntities, function (proposedConnectionEntity) {
                    return proposedConnectionEntity.id === currentConnectionEntity.id;
                });

                // if the current connection is not present and was not recently added, remove it
                if (isPresent.length === 0 && !addedCache.has(key)) {
                    connectionMap.remove(key);
                }
            });
            $.each(connectionEntities, function (_, connectionEntity) {
                set(connectionEntity);
            });
        } else if (commonService.isDefinedAndNotNull(connectionEntities)) {
            set(connectionEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderConnections(selection.enter(), selectAll);

        // update
        var updated = selection.merge(entered);
        updated.call(updateConnections, {
            'updatePath': true,
            'updateLabel': true,
            'transition': transition
        }).call(sort);

        // exit
        selection.exit().call(removeConnections);
    };

    /**
     * Refreshes the connection in the UI.
     *
     * @param {string} connectionId
     */
    this.refresh = function (connectionId) {
        if (commonService.isDefinedAndNotNull(connectionId)) {
            canvasSvg.select('#id-' + connectionId).call(updateConnections, {
                'updatePath': true,
                'updateLabel': true
            });
        } else {
            canvasSvg.selectAll('g.connection').call(updateConnections, {
                'updatePath': true,
                'updateLabel': true
            });
        }
    };

    /**
     * Refreshes the components necessary after a pan event.
     */
    this.pan = function () {
        canvasSvg.selectAll('g.connection.entering, g.connection.leaving').call(updateConnections, {
            'updatePath': false,
            'updateLabel': true
        });
    };

    /**
     * Removes the specified connection.
     *
     * @param {array|string} connectionIds      The connection id
     */
    this.remove = function (connectionIds) {
        var now = new Date().getTime();

        if ($.isArray(connectionIds)) {
            $.each(connectionIds, function (_, connectionId) {
                removedCache.set(connectionId, now);
                connectionMap.remove(connectionId);
            });
        } else {
            removedCache.set(connectionIds, now);
            connectionMap.remove(connectionIds);
        }

        // apply the selection and handle all removed connections
        select().exit().call(removeConnections);
    };

    /**
     * Removes all processors.
     */
    this.removeAll = function () {
        this.remove(connectionMap.keys());
    };

    /**
     * Gets the connection that have a source or destination component with the specified id.
     *
     * @param {string} id     component id
     * @returns {Array}     components connections
     */
    this.getComponentConnections = function (id) {
        var connections = [];
        connectionMap.each(function (entry) {
            // see if this component is the source or destination of this connection
            if (self.getConnectionSourceComponentId(entry) === id || self.getConnectionDestinationComponentId(entry) === id) {
                connections.push(entry);
            }
        });
        return connections;
    };

    /**
     * Returns the component id of the source of this processor. If the connection is attached
     * to a port in a [sub|remote] group, the component id will be that of the group. Otherwise
     * it is the component itself.
     *
     * @param {object} connection   The connection in question
     */
    this.getConnectionSourceComponentId = function (connection) {
        var sourceId = connection.sourceId;
        if (connection.sourceGroupId !== canvas.getGroupId()) {
            sourceId = connection.sourceGroupId;
        }
        return sourceId;
    };

    /**
     * Returns the component id of the source of this processor. If the connection is attached
     * to a port in a [sub|remote] group, the component id will be that of the group. Otherwise
     * it is the component itself.
     *
     * @param {object} connection   The connection in question
     */
    this.getConnectionDestinationComponentId = function (connection) {
        var destinationId = connection.destinationId;
        if (connection.destinationGroupId !== canvas.getGroupId()) {
            destinationId = connection.destinationGroupId;
        }
        return destinationId;
    };

    /**
     * If the connection id is specified it is returned. If no connection id
     * specified, all connections are returned.
     *
     * @param {string} id
     */
    this.get = function (id) {
        if (commonService.isUndefined(id)) {
            return connectionMap.values();
        } else {
            return connectionMap.get(id);
        }
    };

    /**
     * Reloads the connection state from the server and refreshes the UI.
     * If the connection is currently unknown, this function rejects.
     *
     * @param {string} id The remote process group id
     */
    this.reload = function (id) {
        return new Promise(function (resolve, reject) {
            if (connectionMap.has(id)) {
                var connectionEntity = connectionMap.get(id);

                flowDesignerApi.getConnection(connectionEntity)
                    .subscribe(function (response) {
                        self.set(response);
                        resolve(self.get(id));
                    }, function (errorResponse) {
                        if (!errorResponse.preventDefault) {
                            reject(errorResponse.message);
                        }
                    });
            } else {
                reject('The specified connection not found.');
            }
        });
    };

    /**
     * Removes the specified connection.
     *
     * @param {array|string} connectionIds      The connections
     */
    this.remove = function (connectionIds) {
        var now = new Date().getTime();

        if ($.isArray(connectionIds)) {
            $.each(connectionIds, function (_, connectionId) {
                removedCache.set(connectionId, now);
                connectionMap.remove(connectionId);
            });
        } else {
            removedCache.set(connectionIds, now);
            connectionMap.remove(connectionIds);
        }

        // apply the selection and handle all removed connection
        select().exit().call(removeConnections);
    };

    /**
     * Removes all connections.
     */
    this.removeAll = function () {
        self.remove(connectionMap.keys());
    };

    /**
     * Expires the caches up to the specified timestamp.
     *
     * @param timestamp
     */
    this.expireCaches = function (timestamp) {
        var expire = function (cache) {
            cache.each(function (entryTimestamp, id) {
                if (timestamp > entryTimestamp) {
                    cache.remove(id);
                }
            });
        };

        expire(addedCache);
        expire(removedCache);
    };

    this.selfLoopConfig = {
        selfLoopXOffset: (dimensions.width / 2) + 5,
        selfLoopYOffset: 25
    };

    /**
     * Destroy the service
     */
    this.destroy = function () {
        if (commonService.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
    };
};

ConnectionManager.prototype = {
    constructor: ConnectionManager
};

ConnectionManager.parameters = [
    CommonService,
    CanvasUtilsService,
    fdsDialogsModule.FdsDialogService,
    ngMaterial.MatDialog
];

module.exports = ConnectionManager;
