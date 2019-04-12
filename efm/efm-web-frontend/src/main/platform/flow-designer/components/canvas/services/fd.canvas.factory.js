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
var rxjs = require('rxjs/Observable');
var d3 = require('d3');
require('d3-selection-multi');

/**
 * CanvasFactory
 *
 * @returns {Function}
 * @constructor
 */
function CanvasFactory() {
    /**
     * Canvas constructor.
     *
     * @param canvasUtilsService                    The flow designer canvas utils
     * @param commonService                         The flow designer common service.
     * @param fdsDialogService                      The FDS dialog service module.
     * @param storageService                        The storage service.
     * @constructor
     */
    return function (canvasUtilsService, commonService, fdsDialogService, fdsStorageService) {

        /**
         * Canvas constructor.
         *
         * @param client                    The flow designer canvas utils
         * @param processorManager          The flow designer processor manager.
         * @param funnelManager             The flow designer funnel manager.
         * @param connectionManager         The flow designer connection manager.
         * @param remoteProcessGroupManager The flow designer remote process group manager.
         * @param draggableBehavior         The flow designer draggable behavior.
         * @param connectableBehavior       The flow designer connectable behavior.
         * @param editableBehavior          The flow designer editable behavior
         * @param selectableBehavior        The flow designer selectable behavior.
         * @param deselectableBehavior      The flow designer deselectable behavior.
         * @param quickSelectBehavior       The flow designer quick select behavior.
         * @param flowDesignerApi           The flow designer api.
         * @param contextMenu               The flow designer context menu.
         * @constructor
         */
        function Canvas(client, processorManager, funnelManager, connectionManager, remoteProcessGroupManager, draggableBehavior, connectableBehavior, editableBehavior, selectableBehavior, deselectableBehavior, quickSelectBehavior, flowDesignerApi, contextMenu) {
            var self = this;
            var dialogRef = null;
            var SCALE = 1;
            var TRANSLATE = [0, 0];
            var AUTO_REFRESH_SECONDS = 30;
            var svg = null;
            var canvasSVG = null;
            var canvasContainerElement = null;
            var contextMenuElement = null;

            var loadFlow = null;
            var flowLoaded = false;
            var polling = false;
            var pollingId = null;

            var isPublished = false;
            var isDirty = true;
            var flowVersion = null;
            var flowVersionTimestamp = null;

            var groupId = null;
            var groupName = null;
            var parentGroupId = null;
            var permissions = null;

            this.client = client;
            this.processorManager = processorManager;
            this.funnelManager = funnelManager;
            this.connectionManager = connectionManager;
            this.remoteProcessGroupManager = remoteProcessGroupManager;
            this.draggableBehavior = draggableBehavior;
            this.connectableBehavior = connectableBehavior;
            this.editableBehavior = editableBehavior;
            this.selectableBehavior = selectableBehavior;
            this.deselectableBehavior = deselectableBehavior;
            this.quickSelectBehavior = quickSelectBehavior;
            this.flowDesignerApi = flowDesignerApi;
            this.contextMenu = contextMenu;
            this.inProgress = false;

            /**
             * Handles loading a flow refresh.
             *
             * @param response          Response from the refresh
             * @param options           Options for rendering
             * @param now               Timestamp to expiring caches
             */
            var handleFlow = function (response, options, now) {
                handleVersionInfo(response.versionInfo);

                // load the flow
                self.graph.expireCaches(now);
                self.graph.set(response.flow, $.extend({
                    'selectAll': false
                }, options));
                self.graph.updateVisibility();
            };

            /**
             * Handles updating the version info.
             *
             * @param versionInfo       The flows version info
             */
            var handleVersionInfo = function (versionInfo) {
                // update the version info
                if (commonService.isDefinedAndNotNull(versionInfo)) {
                    isPublished = true;
                    isDirty = versionInfo.dirty;
                    flowVersion = versionInfo.registryVersion;
                    flowVersionTimestamp = versionInfo.lastPublished;
                } else {
                    isPublished = false;
                    isDirty = true;
                    flowVersion = null;
                    flowVersionTimestamp = null;
                }
            };

            /**
             * Poll for a flow update. Will continue polling every autoRefreshInterval seconds.
             *
             * @param {int} autoRefreshInterval      The auto refresh interval
             */
            var poll = function (autoRefreshInterval) {
                // ensure we're suppose to poll
                if (polling) {
                    self.refreshFlow().subscribe(function (response) {
                        // start the wait to poll again
                        pollingId = setTimeout(function () {
                            poll(autoRefreshInterval);
                        }, autoRefreshInterval * 1000);
                    }, function (errorResponse) {

                    });
                }
            };

            /**
             * Refreshes the current flow.
             */
            this.refreshFlow = function () {
                var now = new Date().getTime();
                var reload$ = self.flowDesignerApi.getFlow(groupId);

                reload$.subscribe(function (response) {
                    permissions = response.permissions;
                    // handle the flow
                    handleFlow(response, {
                        'transition': true
                    }, now);
                }, function (errorResponse) {
                });

                return reload$;
            };

            /**
             * Refreshes the current flow's version info.
             */
            this.getPermissions = function () {
                return permissions;
            };

            /**
             * Refreshes the current flow's version info.
             */
            this.refreshVersionInfo = function () {
                var reload$ = self.flowDesignerApi.getVersionInfo();

                reload$.subscribe(function (versionInfo) {
                    // handle the version info
                    handleVersionInfo(versionInfo);
                }, function (errorResponse) {
                });

                return reload$;
            };

            /**
             * Initializes the canvas.
             *
             * @param flowDesignerCanvasComponent           The flow designer canvas component.
             */
            this.init = function (flowDesignerCanvasComponent) {
                canvasContainerElement = flowDesignerCanvasComponent.flowDesignerCanvasElementRef.nativeElement;
                contextMenuElement = flowDesignerCanvasComponent.contextMenuElementRef.nativeElement;
                // create the canvas
                svg = d3.select(canvasContainerElement).append('svg');

                // create the definitions element
                var defs = svg.append('defs');

                // create arrow definitions for the various line types
                defs.selectAll('marker')
                    .data(['normal', 'ghost', 'unauthorized', 'full'])
                    .enter().append('marker')
                    .attrs({
                        'id': function (d) {
                            return d;
                        },
                        'viewBox': '0 0 6 6',
                        'refX': 5,
                        'refY': 3,
                        'markerWidth': 6,
                        'markerHeight': 6,
                        'orient': 'auto',
                        'fill': function (d) {
                            if (d === 'ghost') {
                                return '#999999';
                            } else if (d === 'unauthorized') {
                                return '#ba554a';
                            } else if (d === 'full') {
                                return '#ba554a';
                            } else {
                                return '#000000';
                            }
                        }
                    })
                    .append('path')
                    .attr('d', 'M2,3 L0,6 L6,3 L0,0 z');

                // filter for drop shadow
                var componentDropShadowFilter = defs.append('filter')
                    .attrs({
                        'id': 'component-drop-shadow',
                        'height': '140%',
                        'y': '-20%'
                    });

                // blur
                componentDropShadowFilter.append('feGaussianBlur')
                    .attrs({
                        'in': 'SourceAlpha',
                        'stdDeviation': 3,
                        'result': 'blur'
                    });

                // offset
                componentDropShadowFilter.append('feOffset')
                    .attrs({
                        'in': 'blur',
                        'dx': 0,
                        'dy': 1,
                        'result': 'offsetBlur'
                    });

                // color/opacity
                componentDropShadowFilter.append('feFlood')
                    .attrs({
                        'flood-color': '#000000',
                        'flood-opacity': 0.3,
                        'result': 'offsetColor'
                    });

                // combine
                componentDropShadowFilter.append('feComposite')
                    .attrs({
                        'in': 'offsetColor',
                        'in2': 'offsetBlur',
                        'operator': 'in',
                        'result': 'offsetColorBlur'
                    });

                // stack the effect under the source graph
                var componentDropShadowFeMerge = componentDropShadowFilter.append('feMerge');
                componentDropShadowFeMerge.append('feMergeNode')
                    .attr('in', 'offsetColorBlur');
                componentDropShadowFeMerge.append('feMergeNode')
                    .attr('in', 'SourceGraphic');

                // filter for drop shadow
                var connectionFullDropShadowFilter = defs.append('filter')
                    .attrs({
                        'id': 'connection-full-drop-shadow',
                        'height': '140%',
                        'y': '-20%'
                    });

                // blur
                connectionFullDropShadowFilter.append('feGaussianBlur')
                    .attrs({
                        'in': 'SourceAlpha',
                        'stdDeviation': 3,
                        'result': 'blur'
                    });

                // offset
                connectionFullDropShadowFilter.append('feOffset')
                    .attrs({
                        'in': 'blur',
                        'dx': 0,
                        'dy': 1,
                        'result': 'offsetBlur'
                    });

                // color/opacity
                connectionFullDropShadowFilter.append('feFlood')
                    .attrs({
                        'flood-color': '#ba554a',
                        'flood-opacity': 1,
                        'result': 'offsetColor'
                    });

                // combine
                connectionFullDropShadowFilter.append('feComposite')
                    .attrs({
                        'in': 'offsetColor',
                        'in2': 'offsetBlur',
                        'operator': 'in',
                        'result': 'offsetColorBlur'
                    });

                // stack the effect under the source graph
                var connectionFullFeMerge = connectionFullDropShadowFilter.append('feMerge');
                connectionFullFeMerge.append('feMergeNode')
                    .attr('in', 'offsetColorBlur');
                connectionFullFeMerge.append('feMergeNode')
                    .attr('in', 'SourceGraphic');

                // create the canvas svg element
                canvasSVG = svg.append('g')
                    .attrs({
                        'transform': 'translate(' + TRANSLATE + ') scale(' + SCALE + ')',
                        'pointer-events': 'all'
                    });

                // define a function for update the graph dimensions
                var updateGraphSize = function () {
                    var canvasContainer = $(canvasContainerElement);
                    svg.attrs({
                        'height': canvasContainer.height(),
                        'width': canvasContainer.width()
                    });
                };

                // listen for browser resize events to reset the graph size
                $(window).on('resize', function (e) {
                    if (e.target === window) {
                        updateGraphSize();
                    }
                });

                updateGraphSize();

                this.view = (function (canvas, canvasUtilsService, commonService, SCALE) {
                    var INCREMENT = 1.2;
                    var MAX_SCALE = 8;
                    var MIN_SCALE = 0.2;
                    var MIN_SCALE_TO_RENDER = 0.6;
                    var DFM_VIEW_PREFIX = 'dfm-view-';

                    // initialize the zoom behavior
                    var behavior;
                    var x = 0, y = 0, k = SCALE;

                    return {

                        init: function () {
                            var refreshed;
                            var panning = false;

                            // see if the scale has changed during this zoom event,
                            // we want to only transition when zooming in/out as running
                            // the transitions during pan events is undesirable
                            var shouldTransition = function (sourceEvent) {
                                if (commonService.isDefinedAndNotNull(sourceEvent)) {
                                    return sourceEvent.type === 'wheel' || sourceEvent.type === 'mousewheel';
                                } else {
                                    return true;
                                }
                            };

                            k = SCALE;

                            // define the behavior
                            behavior = d3.zoom()
                                .scaleExtent([MIN_SCALE, MAX_SCALE])
                                .on('start', function () {
                                    // hide the context menu
                                    self.contextMenu.hide();
                                })
                                .on('zoom', function () {
                                    // update the current translation and scale
                                    if (!isNaN(d3.event.transform.x)) {
                                        x = d3.event.transform.x;
                                    }
                                    if (!isNaN(d3.event.transform.y)) {
                                        y = d3.event.transform.y;
                                    }
                                    if (!isNaN(d3.event.transform.k)) {
                                        k = d3.event.transform.k;
                                    }

                                    // indicate that we are panning to prevent deselection in zoom.end below
                                    panning = true;

                                    // refresh the canvas
                                    refreshed = canvas.view.refresh({
                                        persist: false,
                                        transition: shouldTransition(d3.event.sourceEvent),
                                        refreshComponents: false
                                    });
                                })
                                .on('end', function () {
                                    // ensure the canvas was actually refreshed
                                    if (commonService.isDefinedAndNotNull(refreshed)) {
                                        canvas.graph.updateVisibility();

                                        // persist the users view
                                        canvas.view.persistUserView();

                                        // reset the refreshed deferred
                                        refreshed = null;
                                    }

                                    if (panning === false) {
                                        // deselect as necessary if we are not panning
                                        self.deselectAll();

                                        // emit canvas group selection subject
                                        flowDesignerCanvasComponent.canvasGroupSelection$.next();
                                    }

                                    panning = false;
                                });

                            // add the behavior to the canvas and disable dbl click zoom
                            canvas.getSvg().call(behavior)
                                .on('dblclick.zoom', null)
                                .on('contextmenu', function () {
                                    // since the context menu event propagated back to the canvas, clear the selection
                                    self.deselectAll();

                                    // emit canvas group selection subject
                                    flowDesignerCanvasComponent.canvasGroupSelection$.next();

                                    // show the context menu on the canvas
                                    self.contextMenu.show();

                                    // prevent default browser behavior
                                    d3.event.preventDefault();
                                });
                        },

                        /**
                         * Persists the current user view.
                         */
                        persistUserView: function () {
                            var name = DFM_VIEW_PREFIX + canvas.getGroupId();

                            // create the item to store
                            var translate = canvas.view.getTranslate();
                            var item = {
                                scale: canvas.view.getScale(),
                                translateX: translate[0],
                                translateY: translate[1]
                            };

                            // store the item
                            fdsStorageService.setItem(name, item);
                        },

                        /**
                         * Attempts to restore a persisted view. Returns a flag that indicates if the
                         * view was restored.
                         */
                        restoreUserView: function () {
                            var viewRestored = false;

                            try {
                                // see if we can restore the view position from storage
                                var name = DFM_VIEW_PREFIX + canvas.getGroupId();
                                var item = fdsStorageService.getItem(name);

                                // ensure the item is valid
                                if (commonService.isDefinedAndNotNull(item)) {
                                    if (isFinite(item.scale) && isFinite(item.translateX) && isFinite(item.translateY)) {
                                        // restore previous view
                                        canvas.view.transform([item.translateX, item.translateY], item.scale);

                                        // mark the view was restore
                                        viewRestored = true;
                                    }
                                }
                            } catch (e) {
                                // likely could not parse item.. ignoring
                            }

                            return viewRestored;
                        },

                        /**
                         * Whether or not a component should be rendered based solely on the current scale.
                         *
                         * @returns {Boolean}
                         */
                        shouldRenderPerScale: function () {
                            return canvas.view.getScale() >= MIN_SCALE_TO_RENDER;
                        },

                        /**
                         * Translates by the specified [x, y].
                         *
                         * @param {array} translate     [x, y] to translate by
                         */
                        translate: function (translate) {
                            behavior.translateBy(canvas.getSvg(), translate[0], translate[1]);
                        },

                        /**
                         * Scales by the specified scale.
                         *
                         * @param {number} scale        The factor to scale by
                         */
                        scale: function (scale) {
                            behavior.scaleBy(canvas.getSvg(), scale);
                        },

                        /**
                         * Sets the current transform.
                         *
                         * @param translate
                         * @param scale
                         */
                        transform: function (translate, scale) {
                            behavior.transform(canvas.getSvg(), d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale));
                        },

                        /**
                         * Gets the current translate.
                         */
                        getTranslate: function () {
                            return [x, y];
                        },

                        /**
                         * Gets the current scale.
                         */
                        getScale: function () {
                            return k;
                        },

                        /**
                         * Zooms in a single zoom increment.
                         */
                        zoomIn: function () {
                            canvas.scale(INCREMENT);
                        },

                        /**
                         * Zooms out a single zoom increment.
                         */
                        zoomOut: function () {
                            canvas.scale(1 / INCREMENT);
                        },

                        /**
                         * Zooms to fit the entire graph on the canvas.
                         */
                        fit: function () {
                            var translate = canvas.view.getTranslate();
                            var scale = canvas.view.getScale();
                            var newScale;

                            // get the canvas normalized width and height
                            var canvasContainer = $(canvasContainerElement);
                            var canvasWidth = canvasContainer.width();
                            var canvasHeight = canvasContainer.height();
                            var canvasContainerTopOffset = canvasContainer.offset().top;
                            var canvasContainerLeftOffset = canvasContainer.offset().left;

                            // get the bounding box for the graph
                            var graphBox = canvas.getCanvasSvg().node().getBoundingClientRect();
                            var graphWidth = graphBox.width / scale;
                            var graphHeight = graphBox.height / scale;
                            var graphLeft = graphBox.left - canvasContainerLeftOffset / scale;
                            var graphTop = (graphBox.top - canvasContainerTopOffset) / scale;

                            // adjust the scale to ensure the entire graph is visible
                            if (graphWidth > canvasWidth || graphHeight > canvasHeight) {
                                newScale = Math.min(canvasWidth / graphWidth, canvasHeight / graphHeight);

                                // ensure the scale is within bounds
                                newScale = Math.min(Math.max(newScale, MIN_SCALE), MAX_SCALE);
                            } else {
                                newScale = 1;
                            }

                            // center as appropriate
                            canvas.view.centerBoundingBox({
                                x: graphLeft - (translate[0] / scale),
                                y: graphTop - (translate[1] / scale),
                                width: canvasWidth / newScale,
                                height: canvasHeight / newScale,
                                scale: newScale
                            });
                        },

                        /**
                         * Zooms to the actual size (1 to 1).
                         */
                        actualSize: function () {
                            var translate = canvas.view.getTranslate();
                            var scale = canvas.view.getScale();

                            var canvasContainer = $(canvasContainerElement);
                            var canvasContainerTopOffset = canvasContainer.offset().top;
                            var canvasContainerLeftOffset = canvasContainer.offset().left;

                            // get the first selected component
                            var selection = canvas.getSelection();

                            // box to zoom towards
                            var box;

                            // if components have been selected position the view accordingly
                            if (!selection.empty()) {
                                // gets the data for the first component
                                var selectionBox = selection.node().getBoundingClientRect();

                                // get the bounding box for the selected components
                                box = {
                                    x: ((selectionBox.left - canvasContainerLeftOffset) / scale) - (translate[0] / scale),
                                    y: ((selectionBox.top - canvasContainerTopOffset) / scale) - (translate[1] / scale),
                                    width: selectionBox.width / scale,
                                    height: selectionBox.height / scale,
                                    scale: 1
                                };
                            } else {
                                // get the canvas normalized width and height
                                var screenWidth = canvasContainer.width() / scale;
                                var screenHeight = canvasContainer.height() / scale;

                                // center around the center of the screen accounting for the translation accordingly
                                box = {
                                    x: (screenWidth / 2) - (translate[0] / scale),
                                    y: (screenHeight / 2) - (translate[1] / scale),
                                    width: 1,
                                    height: 1,
                                    scale: 1
                                };
                            }

                            // center as appropriate
                            canvas.view.centerBoundingBox(box);
                        },

                        /**
                         * Centers the specified bounding box.
                         *
                         * @param {type} boundingBox
                         */
                        centerBoundingBox: function (boundingBox) {
                            var scale = canvas.view.getScale();
                            if (commonService.isDefinedAndNotNull(boundingBox.scale)) {
                                scale = boundingBox.scale;
                            }

                            // get the canvas normalized width and height
                            var canvasContainer = $(canvasContainerElement);
                            var screenWidth = canvasContainer.width() / scale;
                            var screenHeight = canvasContainer.height() / scale;

                            // determine the center location for this component in canvas space
                            var center = [(screenWidth / 2) - (boundingBox.width / 2), (screenHeight / 2) - (boundingBox.height / 2)];

                            // calculate the difference between the center point and the position of this component and convert to screen space
                            canvas.view.transform([(center[0] - boundingBox.x) * scale, (center[1] - boundingBox.y) * scale], scale);
                        },

                        /**
                         * Refreshes the view based on the configured translation and scale.
                         *
                         * @param {object} options Options for the refresh operation
                         */
                        refresh: function (options) {
                            return $.Deferred(function (deferred) {
                                var persist = true;
                                var transition = false;
                                var refreshComponents = true;

                                // extract the options if specified
                                if (commonService.isDefinedAndNotNull(options)) {
                                    persist = commonService.isDefinedAndNotNull(options.persist) ? options.persist : persist;
                                    transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
                                    refreshComponents = commonService.isDefinedAndNotNull(options.refreshComponents) ? options.refreshComponents : refreshComponents;
                                }

                                // update component visibility
                                if (refreshComponents) {
                                    canvas.graph.updateVisibility();
                                }

                                // persist if appropriate
                                if (persist === true) {
                                    canvas.view.persistUserView();
                                }

                                var t = canvas.view.getTranslate();
                                var s = canvas.view.getScale();

                                // update the canvas
                                if (transition === true) {
                                    canvas.getCanvasSvg().transition()
                                        .duration(500)
                                        .attr('transform', function () {
                                            return 'translate(' + t + ') scale(' + s + ')';
                                        })
                                        .on('end', function () {
                                            deferred.resolve();
                                        });
                                } else {
                                    canvas.getCanvasSvg().attr('transform', function () {
                                        return 'translate(' + t + ') scale(' + s + ')';
                                    });

                                    deferred.resolve();
                                }
                            }).promise();
                        }
                    };
                }(this, canvasUtilsService, commonService, SCALE));
                this.graph = (function (canvas, fdsDialogService, canvasUtilsService, commonService) {
                    return {

                        /**
                         * Initialize the Graph
                         * @returns {*}
                         */
                        init: function () {
                            // initialize the object responsible for each type of component
                            canvas.processorManager.init(canvas);
                            canvas.funnelManager.init(canvas);
                            canvas.connectionManager.init(canvas, flowDesignerCanvasComponent);
                            canvas.remoteProcessGroupManager.init(canvas);
                        },

                        /**
                         * Populates the graph with the resources defined in the response.
                         *
                         * @param {object} processGroupContents      The contents of the process group
                         * @param {boolean} selectAll                Whether or not to select the new contents
                         */
                        add: function (processGroupContents, options) {
                            var selectAll = false;
                            if (commonService.isDefinedAndNotNull(options)) {
                                selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
                            }

                            // if we are going to select the new components, deselect the previous selection
                            if (selectAll) {
                                canvas.getSelection().classed('selected', false);
                            }

                            // add the components to the responsible object
                            canvas.processorManager.add(processGroupContents.processors, options);
                            canvas.remoteProcessGroupManager.add(processGroupContents.remoteProcessGroups, options);
                            canvas.funnelManager.add(processGroupContents.funnels, options);
                            canvas.connectionManager.add(processGroupContents.connections, options);
                        },

                        /**
                         * Refreshes all components currently on the canvas.
                         */
                        pan: function () {
                            // refresh the components
                            canvas.processorManager.pan();
                            canvas.remoteProcessGroupManager.pan();
                            canvas.connectionManager.pan();
                        },

                        /**
                         * Gets the components currently on the canvas.
                         */
                        get: function () {
                            return {
                                processors: canvas.processorManager.get(),
                                remoteProcessGroups: canvas.remoteProcessGroupManager.get(),
                                funnels: canvas.funnelManager.get(),
                                connections: canvas.connectionManager.get()
                            };
                        },

                        /**
                         * Populates the graph with the resources defined in the response.
                         *
                         * @param {object} processGroupContents      The contents of the process group
                         * @param {object} options                   Configuration options
                         */
                        set: function (processGroupContents, options) {
                            var selectAll = false;
                            if (commonService.isDefinedAndNotNull(options)) {
                                selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
                            }

                            // if we are going to select the new components, deselect the previous selection
                            if (selectAll) {
                                canvas.getSelection().classed('selected', false);
                            }

                            // set the components to the responsible object
                            canvas.processorManager.set(processGroupContents.processors, options);
                            canvas.remoteProcessGroupManager.set(processGroupContents.remoteProcessGroups, options);
                            canvas.funnelManager.set(processGroupContents.funnels, options);
                            canvas.connectionManager.set(processGroupContents.connections, options);
                        },

                        /**
                         * Expires any caches prior to setting updated components via .set(...) above. This is necessary
                         * if an ajax request returns out of order. The caches will ensure that added/removed components
                         * will not be removed/added due to process group refreshes. Whether or not a component is present
                         * is ambiguous whether the request is from before the component was added/removed or if another
                         * client has legitimately removed/added it. Once a request is initiated after the component is
                         * added/removed we can remove the entry from the cache.
                         *
                         * @param timestamp expire caches before
                         */
                        expireCaches: function (timestamp) {
                            canvas.funnelManager.expireCaches(timestamp);
                            canvas.remoteProcessGroupManager.expireCaches(timestamp);
                            canvas.processorManager.expireCaches(timestamp);
                            canvas.connectionManager.expireCaches(timestamp);
                        },

                        /**
                         * Refresh the connections after dragging a component
                         *
                         * @param updates
                         */
                        refreshConnections: function (updates) {
                            if (updates.size() > 0) {
                                // wait for all updates to complete
                                var dragUpdates$ = new rxjs.Observable.forkJoin(updates.values());
                                dragUpdates$.subscribe(function (responses) {
                                    var connections = d3.set();

                                    // refresh this component
                                    $.each(responses, function (_, component) {
                                        // check if the component in question is a component
                                        if (typeof component.sourceId === 'undefined') {
                                            // get connections that need to be refreshed because its attached to this component
                                            var componentConnections = canvas.connectionManager.getComponentConnections(component.id);
                                            $.each(componentConnections, function (_, connection) {
                                                connections.add(connection.id);
                                            });
                                        } else {
                                            connections.add(component.id);
                                        }
                                    });

                                    // refresh the connections
                                    connections.each(function (connectionId) {
                                        canvas.connectionManager.refresh(connectionId);
                                    });
                                }, function (errorResponse) {

                                });
                            }
                        },

                        /**
                         * Reloads a connection's source and destination.
                         *
                         * @param {string} sourceComponentId          The connection source id
                         * @param {string} destinationComponentId     The connection destination id
                         */
                        reloadConnectionSourceAndDestination: function (sourceComponentId, destinationComponentId) {
                            if (commonService.isBlank(sourceComponentId) === false) {
                                var source = canvasSVG.select('#id-' + sourceComponentId);
                                if (source.empty() === false) {
                                    this.reloadComponent(source);
                                }
                            }
                            if (commonService.isBlank(destinationComponentId) === false) {
                                var destination = canvasSVG.select('#id-' + destinationComponentId);
                                if (destination.empty() === false) {
                                    this.reloadComponent(destination);
                                }
                            }
                        },

                        /**
                         * Gets the component with the specified id and type.
                         *
                         * @param id
                         * @param type
                         * @return {object}     the component entity
                         */
                        getComponent: function (id, type) {
                            // get the component in question
                            var componentEntity = null;
                            switch (type.toLowerCase()) {
                                case 'processor':
                                    componentEntity = canvas.processorManager.get(id);
                                    break;
                                case 'funnel':
                                    componentEntity = canvas.funnelManager.get(id);
                                    break;
                                case 'remote-process-group':
                                    componentEntity = canvas.remoteProcessGroupManager.get(id);
                                    break;
                                case 'connection':
                                    componentEntity = canvas.connectionManager.get(id);
                                    break;
                                default:
                            }
                            return componentEntity;
                        },

                        /**
                         * Reload the component on the canvas.
                         *
                         * @param component     The component.
                         * @return {Promise}    A promise that will resolve with the reloaded component entity.
                         */
                        reloadComponent: function (component) {
                            var componentData = component.datum();
                            if (componentData.permissions.canRead) {
                                if (canvasUtilsService.isProcessor(component)) {
                                    return canvas.processorManager.reload(componentData.id);
                                } else if (canvasUtilsService.isRemoteProcessGroup(component)) {
                                    return canvas.remoteProcessGroupManager.reload(componentData.id);
                                } else if (canvasUtilsService.isFunnel(component)) {
                                    return canvas.funnelManager.reload(componentData.id);
                                } else if (canvasUtilsService.isConnection(component)) {
                                    return canvas.connectionManager.reload(componentData.id)
                                } else {
                                    return new Promise(function (resolve, reject) {
                                        reject('Unsupported component type.');
                                    });
                                }
                            }
                        },

                        /**
                         * Updates the positioning of all selected components.
                         *
                         * @param {selection} dragSelection The current drag selection
                         */
                        updateComponentsPosition: function (dragSelection) {
                            var updates = d3.map();

                            // determine the drag delta
                            var dragData = dragSelection.datum();
                            var delta = {
                                x: dragData.x - dragData.original.x,
                                y: dragData.y - dragData.original.y
                            };

                            // if the component didn't move, return
                            if (delta.x === 0 && delta.y === 0) {
                                return;
                            }

                            var selectedConnections = canvas.getCanvasSvg().selectAll('g.connection.selected');
                            var selectedComponents = canvas.getCanvasSvg().selectAll('g.component.selected');

                            // ensure every component is writable
                            if (canvasUtilsService.canModify(selectedConnections) === false || canvasUtilsService.canModify(selectedComponents) === false) {
                                dialogRef = fdsDialogService.openConfirm({
                                    title: 'Component Position',
                                    message: 'Must be authorized to modify every component selected.',
                                    acceptButton: 'Ok'
                                });
                                return;
                            }

                            var updateConnectionPosition = function (d, delta) {
                                // only update if necessary
                                if (d.bends.length === 0) {
                                    return null;
                                }

                                // calculate the new bend points
                                var newBends = $.map(d.bends, function (bend) {
                                    return {
                                        x: bend.x + delta.x,
                                        y: bend.y + delta.y
                                    };
                                });

                                var entity = {
                                    'id': d.id,
                                    'revision': client.getRevision(d),
                                    'component': {
                                        id: d.id,
                                        bends: newBends
                                    }
                                };

                                var updateConnection$ = canvas.flowDesignerApi.updateConnection(entity);

                                updateConnection$.subscribe(function (response) {
                                    canvas.connectionManager.set(response);

                                    // update version info
                                    self.refreshVersionInfo();
                                }, function (errorResponse) {
                                });

                                return updateConnection$;
                            };

                            // go through each selected connection
                            selectedConnections.each(function (d) {
                                var updateConnection$ = updateConnectionPosition(d, delta);
                                if (updateConnection$ !== null) {
                                    updates.set(d.id, updateConnection$);
                                }
                            });

                            // go through each selected component
                            selectedComponents.each(function (d) {
                                // consider any self looping connections
                                var connections = canvas.connectionManager.getComponentConnections(d.id);
                                $.each(connections, function (_, connection) {
                                    var isSelfLooping = canvas.connectionManager.getConnectionSourceComponentId(connection) === canvas.connectionManager.getConnectionDestinationComponentId(connection);
                                    if (!updates.has(connection.id) && isSelfLooping) {
                                        var updateConnection$ = updateConnectionPosition(canvas.connectionManager.get(connection.id), delta);
                                        if (updateConnection$ !== null) {
                                            updates.set(connection.id, updateConnection$);
                                        }
                                    }
                                });

                                // consider the component itself
                                var newPosition = {
                                    'x': d.position.x + delta.x,
                                    'y': d.position.y + delta.y
                                };

                                // build the entity
                                var entity = {
                                    'id': d.id,
                                    'revision': client.getRevision(d),
                                    'component': {
                                        'id': d.id,
                                        'position': newPosition
                                    }
                                };

                                var updateComponent$;
                                switch (d.type.toLowerCase()) {
                                    case 'funnel':
                                        updateComponent$ = canvas.flowDesignerApi.updateFunnel(entity);
                                        updateComponent$.subscribe(function (response) {
                                            // update version info
                                            self.refreshVersionInfo();

                                            canvas.funnelManager.set(response);
                                        }, function (errorResponse) {

                                        });
                                        break;
                                    case 'processor':
                                        updateComponent$ = canvas.flowDesignerApi.updateProcessor(entity);
                                        updateComponent$.subscribe(function (response) {
                                            // update version info
                                            self.refreshVersionInfo();

                                            canvas.processorManager.set(response);
                                        }, function (errorResponse) {

                                        });
                                        break;
                                    case 'remote-process-group':
                                        updateComponent$ = canvas.flowDesignerApi.updateRemoteProcessGroup(entity);
                                        updateComponent$.subscribe(function (response) {
                                            // update version info
                                            self.refreshVersionInfo();

                                            canvas.remoteProcessGroupManager.set(response);
                                        }, function (errorResponse) {

                                        });
                                        break;
                                    default:
                                        break;
                                }

                                updates.set(d.id, updateComponent$);
                            });

                            canvas.graph.refreshConnections(updates);
                        },

                        /**
                         * Updates component visibility based on the current translation/scale.
                         */
                        updateVisibility: function () {
                            var graph = this.get();
                            var canvasContainer = $(canvasContainerElement);
                            var translate = canvas.view.getTranslate();
                            var scale = canvas.view.getScale();

                            // scale the translation
                            translate = [translate[0] / scale, translate[1] / scale];

                            // get the normalized screen width and height
                            var screenWidth = canvasContainer.width() / scale;
                            var screenHeight = canvasContainer.height() / scale;

                            // calculate the screen bounds one screens worth in each direction
                            var screenLeft = -translate[0] - screenWidth;
                            var screenTop = -translate[1] - screenHeight;
                            var screenRight = screenLeft + (screenWidth * 3);
                            var screenBottom = screenTop + (screenHeight * 3);

                            // detects whether a component is visible and should be rendered
                            var isComponentVisible = function (d) {
                                if (!canvas.view.shouldRenderPerScale()) {
                                    return false;
                                }

                                var left = d.position.x;
                                var top = d.position.y;
                                var right = left + d.dimensions.width;
                                var bottom = top + d.dimensions.height;

                                // determine if the component is now visible
                                return screenLeft < right && screenRight > left && screenTop < bottom && screenBottom > top;
                            };

                            // detects whether a connection is visible and should be rendered
                            var isConnectionVisible = function (d) {
                                if (!canvas.view.shouldRenderPerScale()) {
                                    return false;
                                }

                                var x, y;
                                if (d.bends.length > 0) {
                                    var i = Math.min(Math.max(0, d.labelIndex), d.bends.length - 1);
                                    x = d.bends[i].x;
                                    y = d.bends[i].y;
                                } else {
                                    x = (d.start.x + d.end.x) / 2;
                                    y = (d.start.y + d.end.y) / 2;
                                }

                                return screenLeft < x && screenRight > x && screenTop < y && screenBottom > y;
                            };

                            // marks the specific component as visible and determines if its entering or leaving visibility
                            var updateVisibility = function (d, isVisible) {
                                var selection = canvas.getCanvasSvg().select('#id-' + d.id);
                                var visible = isVisible(d);
                                var wasVisible = selection.classed('visible');

                                // mark the selection as appropriate
                                selection.classed('visible', visible)
                                    .classed('entering', function () {
                                        return visible && !wasVisible;
                                    }).classed('leaving', function () {
                                    return !visible && wasVisible;
                                });
                            };

                            // update the visibility for each component
                            $.each(graph.processors, function (_, d) {
                                updateVisibility(d, isComponentVisible);
                            });
                            $.each(graph.remoteProcessGroups, function (_, d) {
                                updateVisibility(d, isComponentVisible);
                            });
                            $.each(graph.connections, function (_, d) {
                                updateVisibility(d, isConnectionVisible);
                            });

                            // pan the canvas which updates the DOM
                            this.pan();
                        }
                    };
                }(this, fdsDialogService, canvasUtilsService, commonService));

                this.view.init();
                this.graph.init();
                this.contextMenu.init(this);
                this.deselectableBehavior.init(this);
                this.selectableBehavior.init(this);
                this.draggableBehavior.init(this.getCanvasSvg(), this.view.getScale, this.graph.updateComponentsPosition);
                this.connectableBehavior.init(this, this.connectionManager.selfLoopConfig);
                this.editableBehavior.init(this.draggableBehavior, this.connectableBehavior);

                var now = new Date().getTime();
                var getClientId$ = this.flowDesignerApi.getClientId();
                var getFlow$ = this.flowDesignerApi.getFlow();

                loadFlow = new Promise(function (resolve, reject) {
                    var clientIdAndFlow$ = new rxjs.Observable.forkJoin(getClientId$, getFlow$);
                    clientIdAndFlow$.subscribe(function (responses) {
                        // handle the client id
                        self.client.init(responses[0]);

                        // set permissions
                        permissions = responses[1].permissions;

                        // record the current group id
                        groupId = responses[1].id;

                        // handle the flow
                        handleFlow(responses[1], {}, now);

                        // TODO - only need to do this when we are entering a new group. however, nested
                        // groups are currently not supported so we only actually need to do this on the
                        // initial load... this may need to be adjusted once we introduce polling

                        // attempt to restore the view
                        var viewRestored = self.view.restoreUserView();

                        // if the view was not restore attempt to fit
                        if (viewRestored === false) {
                            self.view.fit();
                        }

                        // register the polling
                        pollingId = setTimeout(function () {
                            self.startPolling(AUTO_REFRESH_SECONDS);
                        }, AUTO_REFRESH_SECONDS * 1000);

                        // mark the flow is loaded
                        flowLoaded = true;

                        // create the canvas tooltip for the flow indicator
                        var tip = d3.select(canvasContainerElement).select('.flow-status-tooltip');
                        var target = d3.select(canvasContainerElement).select('.flow-status-indicator');
                        canvasUtilsService.canvasTooltip(tip, target, canvasContainerElement);

                        resolve();
                    }, function (errorResponse) {
                        reject(errorResponse);
                    });
                });

                loadFlow.catch(function (errorResponse) {

                });
            };

            /**
             * Destroy the service
             */
            this.destroy = function () {
                if (commonService.isDefinedAndNotNull(dialogRef)) {
                    dialogRef.close();
                }
            };

            /**
             * Deselects all currently selected components and connections.
             *
             * @returns {selection}
             */
            this.deselectAll = function () {
                // deselect canvas components
                this.getSelection().classed('selected', false);
                return this.getSelection();
            };

            /**
             * Get the svg element.
             * @returns {*}
             */
            this.getSvg = function () {
                return svg;
            };

            /**
             * Get the canvas SVG element.
             * @returns {*}
             */
            this.getCanvasSvg = function () {
                return canvasSVG;
            };

            /**
             * Get the canvas container element.
             * @returns {*}
             */
            this.getCanvasContainerElement = function () {
                return canvasContainerElement;
            };

            /**
             * Get the canvas context menu container element.
             * @returns {*}
             */
            this.getContextMenuElement = function () {
                return contextMenuElement;
            };

            /**
             * Gets the currently selected components and connections.
             *
             * @returns {selection}     The currently selected components and connections
             */
            this.getSelection = function () {
                return canvasSVG.selectAll('g.component.selected, g.connection.selected');
            };

            /**
             * Get the group id.
             */
            this.getGroupId = function () {
                return groupId;
            };

            /**
             * Get the group name.
             */
            this.getGroupName = function () {
                return groupName;
            };

            /**
             * Get the parent group id.
             */
            this.getParentGroupId = function () {
                return parentGroupId;
            };

            /**
             * Returns whether the current flow is dirty.
             *
             * @returns {boolean}
             */
            this.isFlowDirty = function () {
                return isDirty;
            };

            /**
             * Returns whether the flow is published.
             */
            this.isFlowPublished = function () {
                return isPublished;
            };

            /**
             * Returns the current flow version.
             */
            this.getFlowVersion = function () {
                return flowVersion;
            };

            /**
             * Returns the timestamp for the current flow version.
             */
            this.getFlowVersionTimestamp = function () {
                if (flowVersionTimestamp === null) {
                    return null;
                }

                return new Date(flowVersionTimestamp).toLocaleString();
            };

            /**
             * Returns whether the flow has loaded initially.
             */
            this.isFlowLoaded = function () {
                return flowLoaded;
            };

            /**
             * Returns a promise that resolves once the flow has loaded initially.
             */
            this.loadFlow = function () {
                return loadFlow;
            };

            /**
             * Starts polling for auto flow refreshes.
             *
             * @param autoRefreshInterval
             */
            this.startPolling = function (autoRefreshInterval) {
                if (polling === false) {
                    // set polling flag
                    polling = true;

                    // start polling
                    poll(autoRefreshInterval);
                }
            };

            /**
             * Stops polling for auto flow refreshes.
             */
            this.stopPolling = function () {
                // set polling flag
                polling = false;

                if (pollingId !== null) {
                    clearTimeout(pollingId);
                }
            };
        };

        Canvas.prototype = {
            constructor: Canvas
        };

        return Canvas;
    }
};

CanvasFactory.prototype = {
    constructor: CanvasFactory
};

module.exports = {
    build: CanvasFactory
};