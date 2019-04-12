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
 * FunnelManager constructor.
 *
 * @param commonService                 The common service.
 * @param canvasUtilsService            The canvas utils service.
 * @constructor
 */
function FunnelManager(commonService, canvasUtilsService) {

    var self = this;
    var canvasSvg = null;
    var flowDesignerApi = null;
    var client = null;
    var contextMenu = null;
    var deselectableBehavior = null;
    var selectableBehavior = null;
    var editableBehavior = null;
    var quickSelectBehavior = null;

    var dimensions = {
        width: 90,
        height: 90
    };

    // -----------------------------
    // funnels currently on the graph
    // -----------------------------

    var funnelMap;

    // -----------------------------------------------------------
    // cache for components that are added/removed from the canvas
    // -----------------------------------------------------------

    var removedCache;
    var addedCache;

    // --------------------
    // component containers
    // --------------------

    var funnelContainer;

    // --------------------------
    // privately scoped functions
    // --------------------------

    /**
     * Selects the funnel elements against the current funnel map.
     */
    var select = function () {
        return funnelContainer.selectAll('g.funnel').data(funnelMap.values(), function (d) {
            return d.id;
        });
    };

    /**
     * Renders the funnels in the specified selection.
     *
     * @param {selection} entered           The selection of funnels to be rendered
     * @param {boolean} selected             Whether the element should be selected
     * @return the entered selection
     */
    var renderFunnels = function (entered, selected) {
        if (entered.empty()) {
            return entered;
        }

        var funnel = entered.append('g')
            .attrs({
                'id': function (d) {
                    return 'id-' + d.id;
                },
                'class': 'funnel component'
            })
            .classed('selected', selected)
            .call(canvasUtilsService.position);

        // funnel border
        funnel.append('circle')
            .attrs({
                'cx': function (d) {
                    return d.dimensions.width / 2
                },
                'cy': function (d) {
                    return d.dimensions.width / 2
                },
                'r': function (d) {
                    return d.dimensions.width / 2
                },
                'class': 'border',
                'fill': 'transparent',
                'stroke': 'transparent'
            });

        // funnel icon
        funnel.append('text')
            .attrs({
                'class': 'funnel-icon fa',
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0,
                'x': 19,
                'y': 66
            })
            .text('\uf066');

        // always support selection
        funnel.call(selectableBehavior.activate).call(deselectableBehavior.activate).call(contextMenu.activate);

        return funnel;
    };

    /**
     * Updates the funnels in the specified selection.
     *
     * @param {selection} updated               The funnels to be updated
     */
    var updateFunnels = function (updated) {
        if (updated.empty()) {
            return;
        }

        // funnel border authorization
        updated.select('circle.border')
            .classed('unauthorized', function (d) {
                return d.permissions.canRead === false;
            });

        updated.each(function () {
            var funnel = d3.select(this);

            // update the component behavior as appropriate
            funnel.call(editableBehavior.activate);
        });
    };

    /**
     * Removes the funnels in the specified selection.
     *
     * @param {selection} removed               The funnels to be removed
     */
    var removeFunnels = function (removed) {
        removed.remove();
    };

    /**
     * Initialize the funnel service
     *
     * @param canvas                  The canvas.
     * @returns {*}
     */
    this.init = function (canvas) {
        funnelMap = d3.map();
        removedCache = d3.map();
        addedCache = d3.map();

        canvasSvg = canvas.getCanvasSvg();
        flowDesignerApi = canvas.flowDesignerApi;
        client = canvas.client;
        contextMenu = canvas.contextMenu;
        deselectableBehavior = canvas.deselectableBehavior;
        selectableBehavior = canvas.selectableBehavior;
        editableBehavior = canvas.editableBehavior;
        quickSelectBehavior = canvas.quickSelectBehavior;

        // create the funnel container
        funnelContainer = canvasSvg.append('g')
            .attrs({
                'pointer-events': 'all',
                'class': 'funnels'
            });
    };

    /**
     * Adds the specified funnel entity.
     *
     * @param funnelEntities       The funnel
     * @param options           Configuration options
     */
    this.add = function (funnelEntities, options) {
        var selectAll = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
        }

        // get the current time
        var now = new Date().getTime();

        var add = function (funnelEntity) {
            addedCache.set(funnelEntity.id, now);

            // add the funnel
            funnelMap.set(funnelEntity.id, $.extend(funnelEntity, {
                type: 'funnel',
                dimensions: dimensions
            }));
        };

        // determine how to handle the specified funnel status
        if ($.isArray(funnelEntities)) {
            $.each(funnelEntities, function (_, funnelEntity) {
                add(funnelEntity);
            });
        } else if (commonService.isDefinedAndNotNull(funnelEntities)) {
            add(funnelEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderFunnels(selection.enter(), selectAll);

        // update
        updateFunnels(selection.merge(entered));
    };

    /**
     * If the funnel id is specified it is returned. If no funnel id
     * specified, all funnels are returned.
     *
     * @param {string} id
     */
    this.get = function (id) {
        if (commonService.isUndefined(id)) {
            return funnelMap.values();
        } else {
            return funnelMap.get(id);
        }
    };

    /**
     * Populates the graph with the specified funnels.
     *
     * @argument {object | array} funnelEntities                    The funnels to add
     * @argument {object} options                Configuration options
     */
    this.set = function (funnelEntities, options) {
        var selectAll = false;
        var transition = false;
        var overrideRevisionCheck = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
            transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
            overrideRevisionCheck = commonService.isDefinedAndNotNull(options.overrideRevisionCheck) ? options.overrideRevisionCheck : overrideRevisionCheck;
        }

        var set = function (proposedFunnelEntity) {
            var currentFunnelEntity = funnelMap.get(proposedFunnelEntity.id);

            // set the funnel if appropriate due to revision and wasn't previously removed
            if ((client.isNewerRevision(currentFunnelEntity, proposedFunnelEntity) && !removedCache.has(proposedFunnelEntity.id)) || overrideRevisionCheck === true) {
                funnelMap.set(proposedFunnelEntity.id, $.extend(proposedFunnelEntity, {
                    type: 'funnel',
                    dimensions: dimensions
                }));
            }
        };

        if ($.isArray(funnelEntities)) {
            $.each(funnelMap.keys(), function (_, key) {
                var currentFunnelEntity = funnelMap.get(key);
                var isPresent = $.grep(funnelEntities, function (proposedFunnelEntity) {
                    return proposedFunnelEntity.id === currentFunnelEntity.id;
                });

                // if the current funnel is not present and was not recently added, remove it
                if (isPresent.length === 0 && !addedCache.has(key)) {
                    funnelMap.remove(key);
                }
            });
            $.each(funnelEntities, function (_, funnelEntity) {
                set(funnelEntity);
            });
        } else if (commonService.isDefinedAndNotNull(funnelEntities)) {
            set(funnelEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderFunnels(selection.enter(), selectAll);

        // update
        var updated = selection.merge(entered);
        updated.call(updateFunnels).call(canvasUtilsService.position, transition);

        // exit
        selection.exit().call(removeFunnels);
    };

    /**
     * Reloads the funnel state from the server and refreshes the UI.
     * If the funnel is currently unknown, this function rejects.
     *
     * @param {string} id The funnel id
     */
    this.reload = function (id) {
        return new Promise(function (resolve, reject) {
            if (funnelMap.has(id)) {
                var funnelEntity = funnelMap.get(id);

                flowDesignerApi.getFunnel(funnelEntity)
                    .subscribe(function (response) {
                        self.set(response);
                        resolve(self.get(id));
                    }, function (errorResponse) {
                        if (!errorResponse.preventDefault) {
                            reject(errorResponse.message);
                        }
                    });
            } else {
                reject('The specified funnel not found.');
            }
        });
    };

    /**
     * Removes the specified funnel.
     *
     * @param {array|string} funnelIds      The funnels
     */
    this.remove = function (funnelIds) {
        var now = new Date().getTime();

        if ($.isArray(funnelIds)) {
            $.each(funnelIds, function (_, funnelId) {
                removedCache.set(funnelId, now);
                funnelMap.remove(funnelId);
            });
        } else {
            removedCache.set(funnelIds, now);
            funnelMap.remove(funnelIds);
        }

        // apply the selection and handle all removed funnels
        select().exit().call(removeFunnels);
    };

    /**
     * Removes all funnels.
     */
    this.removeAll = function () {
        self.remove(funnelMap.keys());
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

FunnelManager.prototype = {
    constructor: FunnelManager
};

FunnelManager.parameters = [
    CommonService,
    CanvasUtilsService
];

module.exports = FunnelManager;
