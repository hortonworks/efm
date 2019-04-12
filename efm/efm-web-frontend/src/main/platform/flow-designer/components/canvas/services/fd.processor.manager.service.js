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

var CommonService = require('@flow-designer/services/CommonService');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var $ = require('jquery');
var d3 = require('d3');

/**
 * ProcessorManager constructor.
 *
 * @param commonService                 The flow designer common service.
 * @param canvasUtilsService            The flow designer canvas utils service.
 * @constructor
 */
function ProcessorManager(commonService, canvasUtilsService) {

    var self = this;
    var canvasSvg = null;
    var canvasContainer = null;
    var flowDesignerApi = null;
    var client = null;
    var deselectableBehavior = null;
    var selectableBehavior = null;
    var contextMenu = null;
    var editableBehavior = null;
    var quickSelectBehavior = null;

    var PREVIEW_NAME_LENGTH = 25;

    var dimensions = {
        width: 134,
        height: 134
    };

    // -----------------------------
    // processors currently on the graph
    // -----------------------------

    var processorMap;

    // -----------------------------------------------------------
    // cache for components that are added/removed from the canvas
    // -----------------------------------------------------------

    var removedCache;
    var addedCache;

    // --------------------
    // component containers
    // --------------------

    var processorContainer;

    // --------------------------
    // privately scoped functions
    // --------------------------

    /**
     * Selects the processor elements against the current processor map.
     */
    var select = function () {
        return processorContainer.selectAll('g.processor').data(processorMap.values(), function (d) {
            return d.id;
        });
    };

    /**
     * Gets the x coordinate for a given selection.
     *
     * @param d             selection datum
     * @returns {number}    x coordinate
     */
    var getCenteredTextXCoordinate = function (d) {
        return (dimensions.width / 2) - (d3.select(this).node().getComputedTextLength() / 2);
    };

    /**
     * Returns whether the specified processor is invalid.
     *
     * @param d             selection datum
     * @returns {boolean}   if is invalid
     */
    var isInvalid = function (d) {
        return d.permissions.canRead && !commonService.isEmpty(d.component.validationErrors)
    };

    /**
     * Gets the tooltip content.
     *
     * @param d
     * @return the tip content
     */
    var getTip = function (d) {
        if (d.permissions.canRead) {
            var list = commonService.formatUnorderedList(d.component.validationErrors);
            if (list === null || list.length === 0) {
                return '';
            } else {
                return list;
            }
        } else {
            return 'Invalid';
        }
    };

    /**
     * Renders the processors in the specified selection.
     *
     * @param {selection} entered           The selection of processors to be rendered
     * @param {boolean} selected             Whether the element should be selected
     * @return the entered selection
     */
    var renderProcessors = function (entered, selected) {
        if (entered.empty()) {
            return entered;
        }

        var processor = entered.append('g')
            .attrs({
                'id': function (d) {
                    return 'id-' + d.id;
                },
                'class': 'processor component'
            })
            .classed('selected', selected)
            .call(canvasUtilsService.position);

        // processor border
        processor.append('circle')
            .attrs({
                'cx': function (d) {
                    return d.dimensions.width / 2;
                },
                'cy': function (d) {
                    return d.dimensions.width / 2;
                },
                'r': function (d) {
                    return d.dimensions.width / 2;
                },
                'class': 'border',
                'fill': 'transparent',
                'stroke': 'transparent'
            });

        // processor icon
        processor.append('text')
            .attrs({
                'class': 'processor-icon fa',
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0,
                'x': 41,
                'y': 72
            })
            .text('\uf2db');

        // processor type
        processor.append('text')
            .attrs({
                'class': 'processor-name',
                'width': dimensions.width,
                'y': 104,
            });

        // processor type
        processor.append('text')
            .attrs({
                'class': 'processor-type',
                'width': dimensions.width,
                'y': 115
            })
            .each(function (d) {
                var processorType = d3.select(this);

                // reset the processor type to handle any previous state
                processorType.text(null).selectAll('title').remove();

                // apply ellipsis to the processor type as necessary
                canvasUtilsService.ellipsis(processorType, commonService.substringAfterLast(d.component.type, '.'));
            })
            .attr('x', getCenteredTextXCoordinate)
            .append('title').text(function (d) {
                return commonService.substringAfterLast(d.component.type, '.');
            });

        // always support selection
        processor.call(selectableBehavior.activate).call(deselectableBehavior.activate).call(contextMenu.activate).call(quickSelectBehavior.activate);

        return processor;
    };

    /**
     * Updates the processors in the specified selection.
     *
     * @param {selection} updated               The processors to be updated
     */
    var updateProcessors = function (updated) {
        if (updated.empty()) {
            return;
        }

        // processor border authorization
        updated.select('circle.border')
            .classed('unauthorized', function (d) {
                return d.permissions.canRead === false;
            });

        updated.each(function (processorData) {
            var processor = d3.select(this);

            // update the component behavior as appropriate
            processor.call(editableBehavior.activate);

            // if this processor is visible, render everything
            if (processor.classed('visible')) {
                if (processorData.permissions.canRead) {
                    // update the processor name
                    processor.select('text.processor-name')
                        .attr('fill', function (d) {
                            var fill = '#000000';
                            if (isInvalid(d)) {
                                fill = '#c27234';
                            }
                            return fill;
                        })
                        .each(function (d) {
                            var processorName = d3.select(this);

                            // reset the processor name to handle any previous state
                            processorName.text(null).selectAll('title').remove();

                            // apply ellipsis to the processor name as necessary
                            canvasUtilsService.ellipsis(processorName, d.component.name);
                        })
                        .attr('x', getCenteredTextXCoordinate)
                        .append('title').text(function (d) {
                            return d.component.name;
                        });

                    if (isInvalid(processorData)) {
                        var runStatusIcon = processor.select('text.run-status-icon');

                        if (runStatusIcon.empty()) {
                            // run status background
                            processor.append('circle')
                                .attrs({
                                    'cx': function (d) {
                                        return (d.dimensions.width / 2) - 0.5;
                                    },
                                    'cy': function (d) {
                                        return (d.dimensions.width / 2) + 14;
                                    },
                                    'r': '10',
                                    'class': 'run-status-icon-background',
                                });


                            // run status icon
                            runStatusIcon = processor.append('text')
                                .attrs({
                                    'class': 'run-status-icon',
                                    'font-family': 'FontAwesome',
                                    'x': 59.5,
                                    'y': 85.5,
                                    'width': 14,
                                    'height': 14,
                                    'fill': '#e98a40'
                                })
                                .text(function (d) {
                                    return '\uf071';
                                });
                        }

                        runStatusIcon.each(function (d) {
                            // get the tip
                            var tip = d3.select(canvasContainer).select('#run-status-tip-' + d.id);

                            // get the tooltip content
                            if (tip.empty()) {
                                tip = d3.select(canvasContainer).select('.processor-tooltips').append('div')
                                    .attr('id', function () {
                                        return 'run-status-tip-' + d.id;
                                    })
                                    .attr('class', 'tooltip');

                                canvasUtilsService.canvasTooltip(tip, d3.select(this), canvasContainer);
                            }

                            // update the tip
                            tip.html(function () {
                                return $('<div></div>').append(getTip(d)).html();
                            });
                        });
                    } else {
                        // clear any validation errors
                        processor.call(clearValidationErrors);
                    }
                } else {
                    // clear the processor name
                    processor.select('text.processor-name').text(null);

                    // clear any validation errors
                    processor.call(clearValidationErrors);
                }
            } else {
                if (processorData.permissions.canRead) {
                    // update the processor name
                    processor.select('text.processor-name')
                        .text(function (d) {
                            var name = d.component.name;
                            if (name.length > PREVIEW_NAME_LENGTH) {
                                return name.substring(0, PREVIEW_NAME_LENGTH) + String.fromCharCode(8230);
                            } else {
                                return name;
                            }
                        });
                } else {
                    // clear the processor name
                    processor.select('text.processor-name').text(null);
                }

                // clear any validation errors
                processor.call(clearValidationErrors);
            }
        });
    };

    /**
     * Clears validations errors from the specified processor.
     *
     * @param {selection} processor             The processors to clear the validation errors for
     */
    var clearValidationErrors = function (processor) {
        processor.select('circle.run-status-icon-background').remove();
        processor.select('text.run-status-icon').remove();

        // remove any existing tips
        processor.call(removeTooltips);
    };

    /**
     * Removes the processors in the specified selection.
     *
     * @param {selection} removed               The processors to be removed
     */
    var removeProcessors = function (removed) {
        if (removed.empty()) {
            return;
        }

        removed.call(removeTooltips).remove();
    };

    /**
     * Removes the tooltips for the processors in the specified selection.
     *
     * @param {selection} removed
     */
    var removeTooltips = function (removed) {
        removed.each(function (d) {
            // remove any associated tooltips
            d3.select(canvasContainer).select('#run-status-tip-' + d.id).remove();
        });
    };

    /**
     * Initialize the processor service
     *
     * @param canvas                  The canvas.
     *
     * @returns {*}
     */
    this.init = function (canvas) {
        processorMap = d3.map();
        removedCache = d3.map();
        addedCache = d3.map();

        canvasSvg = canvas.getCanvasSvg();
        canvasContainer = canvas.getCanvasContainerElement();
        flowDesignerApi = canvas.flowDesignerApi;
        client = canvas.client;
        contextMenu = canvas.contextMenu;
        deselectableBehavior = canvas.deselectableBehavior;
        selectableBehavior = canvas.selectableBehavior;
        editableBehavior = canvas.editableBehavior;
        quickSelectBehavior = canvas.quickSelectBehavior;

        // create the processor container
        processorContainer = canvasSvg.append('g')
            .attrs({
                'pointer-events': 'all',
                'class': 'processors'
            });
    };

    /**
     * Adds the specified processor entity.
     *
     * @param processorEntities       The processor
     * @param options           Configuration options
     */
    this.add = function (processorEntities, options) {
        var selectAll = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
        }

        // get the current time
        var now = new Date().getTime();

        var add = function (processorEntity) {
            addedCache.set(processorEntity.id, now);

            // add the processor
            processorMap.set(processorEntity.id, $.extend(processorEntity, {
                type: 'processor',
                dimensions: dimensions
            }));
        };

        // determine how to handle the specified processor status
        if ($.isArray(processorEntities)) {
            $.each(processorEntities, function (_, processorEntity) {
                add(processorEntity);
            });
        } else if (commonService.isDefinedAndNotNull(processorEntities)) {
            add(processorEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderProcessors(selection.enter(), selectAll);

        // update
        updateProcessors(selection.merge(entered));
    };

    /**
     * If the processor id is specified it is returned. If no processor id
     * specified, all processors are returned.
     *
     * @param {string} id
     */
    this.get = function (id) {
        if (commonService.isUndefined(id)) {
            return processorMap.values();
        } else {
            return processorMap.get(id);
        }
    };

    /**
     * Populates the graph with the specified processors.
     *
     * @argument {object | array} processorEntities                 The processors to add
     * @argument {object} options                                   Configuration options
     */
    this.set = function (processorEntities, options) {
        var selectAll = false;
        var transition = false;
        var overrideRevisionCheck = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
            transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
            overrideRevisionCheck = commonService.isDefinedAndNotNull(options.overrideRevisionCheck) ? options.overrideRevisionCheck : overrideRevisionCheck;
        }

        var set = function (proposedProcessorEntity) {
            var currentProcessorEntity = processorMap.get(proposedProcessorEntity.id);

            // set the processor if appropriate due to revision and wasn't previously removed
            if ((client.isNewerRevision(currentProcessorEntity, proposedProcessorEntity) && !removedCache.has(proposedProcessorEntity.id)) || overrideRevisionCheck === true) {
                processorMap.set(proposedProcessorEntity.id, $.extend(proposedProcessorEntity, {
                    type: 'processor',
                    dimensions: dimensions
                }));
            }
        };

        // determine how to handle the specified processor
        if ($.isArray(processorEntities)) {
            $.each(processorMap.keys(), function (_, key) {
                var currentProcessorEntity = processorMap.get(key);
                var isPresent = $.grep(processorEntities, function (proposedProcessorEntity) {
                    return proposedProcessorEntity.id === currentProcessorEntity.id;
                });

                // if the current processor is not present and was not recently added, remove it
                if (isPresent.length === 0 && !addedCache.has(key)) {
                    processorMap.remove(key);
                }
            });
            $.each(processorEntities, function (_, processorEntity) {
                set(processorEntity);
            });
        } else if (commonService.isDefinedAndNotNull(processorEntities)) {
            set(processorEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderProcessors(selection.enter(), selectAll);

        // update
        var updated = selection.merge(entered);
        updated.call(updateProcessors);
        updated.call(canvasUtilsService.position, transition);

        // exit
        selection.exit().call(removeProcessors);
    };

    /**
     * Refreshes the components necessary after a pan event.
     */
    this.pan = function () {
        canvasSvg.selectAll('g.processor.entering, g.processor.leaving').call(updateProcessors);
    };

    /**
     * If the processor id is specified it is refresh according to the current
     * state. If not processor id is specified, all processors are refreshed.
     *
     * @param {string} id      Optional
     */
    this.refresh = function (id) {
        if (commonService.isDefinedAndNotNull(id)) {
            canvasSvg.select('#id-' + id).call(updateProcessors);
        } else {
            canvasSvg.selectAll('g.processor').call(updateProcessors);
        }
    };

    /**
     * Reloads the processor state from the server and refreshes the UI.
     * If the processor is currently unknown, this function rejects.
     *
     * @param {string} id The processor id
     */
    this.reload = function (id) {
        return new Promise(function (resolve, reject) {
            if (processorMap.has(id)) {
                var processorEntity = processorMap.get(id);

                flowDesignerApi.getProcessor(processorEntity)
                    .subscribe(function (response) {
                        self.set(response);
                        resolve(self.get(id));
                    }, function (errorResponse) {
                        if (!errorResponse.preventDefault) {
                            reject(errorResponse.message);
                        }
                    });
            } else {
                reject('The specified processor not found.');
            }
        });
    };

    /**
     * Removes the specified processor.
     *
     * @param {array|string} processorIds      The processors
     */
    this.remove = function (processorIds) {
        var now = new Date().getTime();

        if ($.isArray(processorIds)) {
            $.each(processorIds, function (_, processorId) {
                removedCache.set(processorId, now);
                processorMap.remove(processorId);
            });
        } else {
            removedCache.set(processorIds, now);
            processorMap.remove(processorIds);
        }

        // apply the selection and handle all removed processors
        select().exit().call(removeProcessors);
    };

    /**
     * Removes all processors.
     */
    this.removeAll = function () {
        self.remove(processorMap.keys());
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
};

ProcessorManager.prototype = {
    constructor: ProcessorManager
};

ProcessorManager.parameters = [
    CommonService,
    CanvasUtilsService
];

module.exports = ProcessorManager;
