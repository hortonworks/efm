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
 * RemoteProcessGroupManager constructor.
 *
 * @param commonService                 The flow designer common service.
 * @param canvasUtilsService            The flow designer canvas utils service.
 * @constructor
 */
function RemoteProcessGroupManager(commonService, canvasUtilsService) {

    var self = this;
    var canvasSvg = null;
    var flowDesignerApi = null;
    var client = null;
    var contextMenu = null;
    var deselectableBehavior = null;
    var selectableBehavior = null;
    var editableBehavior = null;
    var quickSelectBehavior = null;

    var PREVIEW_NAME_LENGTH = 25;

    var dimensions = {
        width: 134,
        height: 134
    };

    // --------------------------------------------
    // remote process groups currently on the graph
    // --------------------------------------------

    var remoteProcessGroupMap;

    // -----------------------------------------------------------
    // cache for components that are added/removed from the canvas
    // -----------------------------------------------------------

    var removedCache;
    var addedCache;

    // --------------------
    // component containers
    // --------------------

    var remoteProcessGroupContainer;

    // --------------------------
    // privately scoped functions
    // --------------------------

    /**
     * Selects the remote process groups elements against the current remote process groups map.
     */
    var select = function () {
        return remoteProcessGroupContainer.selectAll('g.remote-process-group').data(remoteProcessGroupMap.values(), function (d) {
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
     * Renders the remote process groups in the specified selection.
     *
     * @param {selection} entered           The selection of remote process groups to be rendered
     * @param {boolean} selected             Whether the element should be selected
     * @return the entered selection
     */
    var renderRemoteProcessGroups = function (entered, selected) {
        if (entered.empty()) {
            return entered;
        }

        var remoteProcessGroup = entered.append('g')
            .attrs({
                'id': function (d) {
                    return 'id-' + d.id;
                },
                'class': 'remote-process-group component'
            })
            .classed('selected', selected)
            .call(canvasUtilsService.position);

        // remote process group border
        remoteProcessGroup.append('circle')
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

        // remote process group icon
        remoteProcessGroup.append('text')
            .attrs({
                'class': 'remote-process-group-icon fa',
                'filter': 'url(#component-drop-shadow)',
                'stroke-width': 0,
                'x': 36,
                'y': 72
            })
            .text('\uf0c2');

        // remote process group URL
        remoteProcessGroup.append('text')
            .attrs({
                'class': 'remote-process-group-url',
                'width': dimensions.width,
                'y': 95
            })
            .each(function (d) {
                var remoteProcessGroupUri = d3.select(this);

                // reset the rpg uri to handle any previous state
                remoteProcessGroupUri.text(null).selectAll('title').remove();

                // apply ellipsis to the rpg uri as necessary
                canvasUtilsService.ellipsis(remoteProcessGroupUri, d.component.targetUris);
            })
            .attr('x', getCenteredTextXCoordinate)
            .append('title').text(function (d) {
                return d.component.targetUris;
            });

        // always support selection
        remoteProcessGroup.call(selectableBehavior.activate).call(deselectableBehavior.activate).call(contextMenu.activate).call(quickSelectBehavior.activate);

        return remoteProcessGroup;
    };

    /**
     * Updates the remote process group in the specified selection.
     *
     * @param {selection} updated               The remote process groups to be updated
     */
    var updateRemoteProcessGroups = function (updated) {
        if (updated.empty()) {
            return;
        }

        // remote process group border authorization
        updated.select('circle.border')
            .classed('unauthorized', function (d) {
                return d.permissions.canRead === false;
            });

        updated.each(function (remoteProcessGroupData) {
            var remoteProcessGroup = d3.select(this);

            // update the component behavior as appropriate
            remoteProcessGroup.call(editableBehavior.activate);

            if (remoteProcessGroupData.permissions.canRead) {
                // if this remote process group is visible, render everything
                if (remoteProcessGroup.classed('visible')) {
                    // update the remote process group URL
                    remoteProcessGroup.select('text.remote-process-group-url')
                        .each(function (d) {
                            var remoteProcessGroupField = d3.select(this);

                            // reset the remote process group field to handle any previous state
                            remoteProcessGroupField.text(null).selectAll('title').remove();

                            // apply ellipsis to the remote process group field as necessary
                            canvasUtilsService.ellipsis(remoteProcessGroupField, d.component.targetUris);
                        })
                        .attr('x', getCenteredTextXCoordinate)
                        .append('title').text(function (d) {
                            return d.component.targetUris;
                        });
                } else {
                    // update the remote process group URL
                    remoteProcessGroup.select('text.remote-process-group-url')
                        .text(function (d) {
                            var field = d.component.targetUris;
                            if (field.length > PREVIEW_NAME_LENGTH) {
                                return field.substring(0, PREVIEW_NAME_LENGTH) + String.fromCharCode(8230);
                            }

                            return field;
                        });
                }
            } else {
                // clear the remote process group URL
                remoteProcessGroup.select('text.remote-process-group-url').text(null);
            }
        });
    };

    /**
     * Removes the remote process groups in the specified selection.
     *
     * @param {selection} removed               The remote process groups to be removed
     */
    var removeRemoteProcessGroups = function (removed) {
        removed.remove();
    };

    /**
     * Initialize the remote process group service
     *
     * @param canvas                  The canvas.
     *
     * @returns {*}
     */
    this.init = function (canvas) {
        remoteProcessGroupMap = d3.map();
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

        // create the remote process group container
        remoteProcessGroupContainer = canvasSvg.append('g')
            .attrs({
                'pointer-events': 'all',
                'class': 'remote-process-groups'
            });
    };

    /**
     * Adds the specified remote process group entity.
     *
     * @param remoteProcessGroupEntities       The remote process group
     * @param options           Configuration options
     */
    this.add = function (remoteProcessGroupEntities, options) {
        var selectAll = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
        }

        // get the current time
        var now = new Date().getTime();

        var add = function (remoteProcessGroupEntity) {
            addedCache.set(remoteProcessGroupEntity.id, now);

            // add the remote process group
            remoteProcessGroupMap.set(remoteProcessGroupEntity.id, $.extend(remoteProcessGroupEntity, {
                type: 'remote-process-group',
                dimensions: dimensions
            }));
        };

        // determine how to handle the specified remote process group
        if ($.isArray(remoteProcessGroupEntities)) {
            $.each(remoteProcessGroupEntities, function (_, remoteProcessGroupEntity) {
                add(remoteProcessGroupEntity);
            });
        } else if (commonService.isDefinedAndNotNull(remoteProcessGroupEntities)) {
            add(remoteProcessGroupEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderRemoteProcessGroups(selection.enter(), selectAll);

        // update
        updateRemoteProcessGroups(selection.merge(entered));
    };

    /**
     * If the remote process group id is specified it is returned. If no remote process group id
     * specified, all remote process groups are returned.
     *
     * @param {string} id
     */
    this.get = function (id) {
        if (commonService.isUndefined(id)) {
            return remoteProcessGroupMap.values();
        } else {
            return remoteProcessGroupMap.get(id);
        }
    };

    /**
     * Populates the graph with the specified remote process groups.
     *
     * @argument {object | array} remoteProcessGroupEntities                 The remote process groups to add
     * @argument {object} options                                   Configuration options
     */
    this.set = function (remoteProcessGroupEntities, options) {
        var selectAll = false;
        var transition = false;
        var overrideRevisionCheck = false;
        if (commonService.isDefinedAndNotNull(options)) {
            selectAll = commonService.isDefinedAndNotNull(options.selectAll) ? options.selectAll : selectAll;
            transition = commonService.isDefinedAndNotNull(options.transition) ? options.transition : transition;
            overrideRevisionCheck = commonService.isDefinedAndNotNull(options.overrideRevisionCheck) ? options.overrideRevisionCheck : overrideRevisionCheck;
        }

        var set = function (proposedRemoteProcessGroupEntity) {
            var currentRemoteProcessGroupEntity = remoteProcessGroupMap.get(proposedRemoteProcessGroupEntity.id);

            // set the remote process group if appropriate due to revision and wasn't previously removed
            if ((client.isNewerRevision(currentRemoteProcessGroupEntity, proposedRemoteProcessGroupEntity) && !removedCache.has(proposedRemoteProcessGroupEntity.id)) || overrideRevisionCheck === true) {
                remoteProcessGroupMap.set(proposedRemoteProcessGroupEntity.id, $.extend(proposedRemoteProcessGroupEntity, {
                    type: 'remote-process-group',
                    dimensions: dimensions
                }));
            }
        };

        // determine how to handle the specified remote process group
        if ($.isArray(remoteProcessGroupEntities)) {
            $.each(remoteProcessGroupMap.keys(), function (_, key) {
                var currentRemoteProcessGroupEntity = remoteProcessGroupMap.get(key);
                var isPresent = $.grep(remoteProcessGroupEntities, function (proposedRemoteProcessGroupEntity) {
                    return proposedRemoteProcessGroupEntity.id === currentRemoteProcessGroupEntity.id;
                });

                // if the current remote process group is not present and was not recently added, remove it
                if (isPresent.length === 0 && !addedCache.has(key)) {
                    remoteProcessGroupMap.remove(key);
                }
            });
            $.each(remoteProcessGroupEntities, function (_, remoteProcessGroupEntity) {
                set(remoteProcessGroupEntity);
            });
        } else if (commonService.isDefinedAndNotNull(remoteProcessGroupEntities)) {
            set(remoteProcessGroupEntities);
        }

        // select
        var selection = select();

        // enter
        var entered = renderRemoteProcessGroups(selection.enter(), selectAll);

        // update
        var updated = selection.merge(entered);
        updated.call(updateRemoteProcessGroups);
        updated.call(canvasUtilsService.position, transition);

        // exit
        selection.exit().call(removeRemoteProcessGroups);
    };

    /**
     * Refreshes the components necessary after a pan event.
     */
    this.pan = function () {
        canvasSvg.selectAll('g.remote-process-group.entering, g.remote-process-group.leaving').call(updateRemoteProcessGroups);
    };

    /**
     * If the remote process group id is specified it is refresh according to the current
     * state. If not remote process group id is specified, all remote process groups are refreshed.
     *
     * @param {string} id      Optional
     */
    this.refresh = function (id) {
        if (commonService.isDefinedAndNotNull(id)) {
            canvasSvg.select('#id-' + id).call(updateRemoteProcessGroups);
        } else {
            canvasSvg.selectAll('g.remote-process-group').call(updateRemoteProcessGroups);
        }
    };

    /**
     * Reloads the remote process group state from the server and refreshes the UI.
     * If the remote process group is currently unknown, this function rejects.
     *
     * @param {string} id The remote process group id
     */
    this.reload = function (id) {
        return new Promise(function (resolve, reject) {
            if (remoteProcessGroupMap.has(id)) {
                var remoteProcessGroupEntity = remoteProcessGroupMap.get(id);

                flowDesignerApi.getRemoteProcessGroup(remoteProcessGroupEntity)
                    .subscribe(function (response) {
                        self.set(response);
                        resolve(self.get(id));
                    }, function (errorResponse) {
                        if (!errorResponse.preventDefault) {
                            reject(errorResponse.message);
                        }
                    });
            } else {
                reject('The specified remote process group not found.');
            }
        });
    };

    /**
     * Removes the specified remote process group.
     *
     * @param {array|string} remoteProcessGroupIds      The remote process groups
     */
    this.remove = function (remoteProcessGroupIds) {
        var now = new Date().getTime();

        if ($.isArray(remoteProcessGroupIds)) {
            $.each(remoteProcessGroupIds, function (_, remoteProcessGroupId) {
                removedCache.set(remoteProcessGroupId, now);
                remoteProcessGroupMap.remove(remoteProcessGroupId);
            });
        } else {
            removedCache.set(remoteProcessGroupIds, now);
            remoteProcessGroupMap.remove(remoteProcessGroupIds);
        }

        // apply the selection and handle all removed remote process groups
        select().exit().call(removeRemoteProcessGroups);
    };

    /**
     * Removes all remote process groups.
     */
    this.removeAll = function () {
        self.remove(remoteProcessGroupMap.keys());
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

RemoteProcessGroupManager.prototype = {
    constructor: RemoteProcessGroupManager
};

RemoteProcessGroupManager.parameters = [
    CommonService,
    CanvasUtilsService
];

module.exports = RemoteProcessGroupManager;
