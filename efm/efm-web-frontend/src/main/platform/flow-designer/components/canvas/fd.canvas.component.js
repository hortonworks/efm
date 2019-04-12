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
var d3 = require('d3');
var rxjs = require('rxjs/Subject');
var ngCore = require('@angular/core');

var canvasFactory = require('@flow-designer/services/CanvasFactory');
var Client = require('@flow-designer/services/Client');
var ProcessorManager = require('@flow-designer/services/ProcessorManager');
var RemoteProcessGroupManager = require('@flow-designer/services/RemoteProcessGroupManager');
var FunnelManager = require('@flow-designer/services/FunnelManager');
var ConnectionManager = require('@flow-designer/services/ConnectionManager');
var ContextMenu = require('@flow-designer/services/ContextMenu');
var QuickSelectBehavior = require('@flow-designer/services/QuickSelectBehavior');
var SelectableBehavior = require('@flow-designer/services/SelectableBehavior');
var DeselectableBehavior = require('@flow-designer/services/DeselectableBehavior');
var EditableBehavior = require('@flow-designer/services/EditableBehavior');
var DraggableBehavior = require('@flow-designer/services/DraggableBehavior');
var ConnectableBehavior = require('@flow-designer/services/ConnectableBehavior');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var CommonService = require('@flow-designer/services/CommonService');
var fdsDialogsModule = require('@flow-design-system/dialogs');
var FdsStorageService = require('@flow-design-system/common/storage-service');
var fdsSnackBarsModule = require('@flow-design-system/snackbars');

/**
 * FlowDesignerCanvasComponent.
 *
 * @param fdsSnackBarService            The fds snackbar service.
 * @param FlowDesignerApi               The flow designer api class.
 * @param Canvas                        The Canvas.
 * @param common                        The common utils.
 * @param canvasUtils                   The canvas utils.
 * @param client                        The canvas client.
 * @param processorManager              The canvas processor manager.
 * @param funnelManager                 The canvas funnel manager.
 * @param connectionManager             The canvas connection manager.
 * @param remoteProcessGroupManager     The canvas remote process group manager.
 * @param draggableBehavior             The canvas draggable behavior.
 * @param connectableBehavior           The canvas connectable behavior.
 * @param editableBehavior              The canvas editable behavior.
 * @param selectableBehavior            The canvas selectable behavior.
 * @param contextMenu                   The canvas context menu.
 * @param quickSelectBehavior           The canvas quick select behavior.
 * @param elementRef                    The canvas component elementRef.
 * @constructor
 */
function FlowDesignerCanvasComponent(fdsSnackBarService, fdsDialogService, FlowDesignerApi, Canvas, common, canvasUtils, client, processorManager, funnelManager, connectionManager, remoteProcessGroupManager, draggableBehavior, connectableBehavior, editableBehavior, selectableBehavior, deselectableBehavior, contextMenu, quickSelectBehavior, elementRef) {
    var self = this;

    var flowDesignerApi = new FlowDesignerApi(elementRef);
    var canvas = new Canvas(client, processorManager, funnelManager, connectionManager, remoteProcessGroupManager, draggableBehavior, connectableBehavior, editableBehavior, selectableBehavior, deselectableBehavior, quickSelectBehavior, flowDesignerApi, contextMenu);

    // Subjects
    this.componentDestroyed$ = new rxjs.Subject();
    this.componentCreation$ = new rxjs.Subject();
    this.componentSelection$ = new rxjs.Subject();
    this.componentDeselection$ = new rxjs.Subject();
    this.canvasGroupSelection$ = new rxjs.Subject();
    this.componentConfiguration$ = new rxjs.Subject();
    this.currentProcessGroupComponentListing$ = new rxjs.Subject();

    var allowPageRefresh = false;
    var hotKeysEnabled = true;

    var handleProcessorCreation = null;
    var handleProcessorConfiguration = null;
    var handleControllerServiceConfiguration = null;
    var handleProcessGroupComponentListing = null;
    var handleConnectionCreation = null;
    var handleConnectionConfiguration = null;
    var handleRemoteProcessGroupCreation = null;
    var handleRemoteProcessGroupConfiguration = null;

    this.flowDesignerCanvasElementRef = null;

    this.globalMenuItems = [];

    /**
     * Whether the current user can write in this group.
     *
     * @returns {boolean}   can write
     */
    var canWrite = function () {
        if (canvas.getPermissions() === null) {
            return false;
        } else {
            return canvas.getPermissions().canWrite === true;
        }
    };

    /**
     * Gets the currently selected components and connections.
     *
     * @returns {selection}     The currently selected components and connections
     */
    var getSelection = function () {
        return d3.selectAll('g.component.selected, g.connection.selected');
    };

    /**
     * Removes component from canvas.
     */
    var deleteComponent = function (selection) {
        if (common.isUndefinedOrNull(selection)) {
            return;
        }

        // get the selection data and its revision
        var selectionData = selection.datum();
        var revision = client.getRevision(selectionData);

        var removeRemainingConnections = function () {
            var connections = canvas.connectionManager.getComponentConnections(selectionData.id);
            if (connections.length > 0) {
                canvas.connectionManager.remove(connections.map(function (connection) {
                    return connection.id;
                }));
            }
        };

        var reloadVersionInfoAndDeselect = function () {
            // update version info
            self.reloadVersionInfo();

            // trigger deselection
            self.canvasGroupSelection$.next();
        };

        // handle delete based on the type
        switch (selectionData.type.toLowerCase()) {
            case 'processor':
                flowDesignerApi.removeProcessor($.extend({
                    'revision': revision
                }, selectionData)).subscribe(function () {
                    // clean up the processor
                    canvas.processorManager.remove(selectionData.id);

                    // and any of its connections
                    removeRemainingConnections();

                    reloadVersionInfoAndDeselect();
                }, function (errorResponse) {
                });
                break;
            case 'connection':
                flowDesignerApi.removeConnection($.extend({
                    'revision': revision
                }, selectionData)).subscribe(function () {
                    // clean up the connection
                    canvas.connectionManager.remove(selectionData.id);

                    reloadVersionInfoAndDeselect();
                }, function (errorResponse) {
                });
                break;
            case 'funnel':
                flowDesignerApi.removeFunnel($.extend({
                    'revision': revision
                }, selectionData)).subscribe(function () {
                    // clean up the funnel
                    canvas.funnelManager.remove(selectionData.id);

                    // and any of its connections
                    removeRemainingConnections();

                    reloadVersionInfoAndDeselect();
                }, function (errorResponse) {
                });
                break;
            case 'remote-process-group':
                flowDesignerApi.removeRemoteProcessGroup($.extend({
                    'revision': revision
                }, selectionData)).subscribe(function () {
                    // clean up the remote process group
                    canvas.remoteProcessGroupManager.remove(selectionData.id);

                    // and any of its connections
                    removeRemainingConnections();

                    reloadVersionInfoAndDeselect();
                }, function (errorResponse) {
                });
                break;
            default:
                break;
        }
    };

    /**
     * Handler for window.keyup events.
     */
    var keyupHandler = function (evt) {
        if (hotKeysEnabled) {
            // get the current selection
            var selection = getSelection();

            // capture hotkeys
            var isCtrl = evt.ctrlKey || evt.metaKey;
            if (isCtrl && evt.keyCode === 82) {
                // ctrl-r
                evt.preventDefault();

                if (allowPageRefresh === true) {
                    location.reload();
                    return;
                }

                self.reload();
            } else if (!$('input, textarea').is(':focus') && (evt.keyCode == 8 || evt.keyCode === 46)) {
                // backspace or delete
                evt.preventDefault();

                if (canWrite() && canvasUtils.areDeletable(selection)) {
                    deleteComponent(selection);
                }
            }
        }
    };

    /**
     * Initialize the component
     */
    this.init = function () {
        canvas.init(this);

        canvas.contextMenu.subject$
            .takeUntil(self.componentDestroyed$)
            .subscribe(function (response) {
                switch (response.action) {
                    case 'showConfiguration':
                        // TODO: update conditions below when process groups are rendered on the canvas
                        if (common.isDefinedAndNotNull(response.selection) && !response.selection.empty()) {
                            canvas.graph.reloadComponent(response.selection)
                                .then(function (reloadedComponent) {
                                    self.componentConfiguration$
                                        .debug("quickSelectBehavior.subject$ Next")
                                        .next(reloadedComponent);
                                });
                        }
                        break;
                    case 'showComponentListing':
                        if (common.isDefinedAndNotNull(response.selection) && response.selection.empty()) {
                            self.openServices();
                        }
                        break;
                    case 'reload':
                        self.reload();
                        break;
                    case 'delete':
                        var selection = response.selection;
                        deleteComponent(selection);

                        break;
                    default:
                        // Unable to find specified action;
                        break;
                }
            });

        canvas.quickSelectBehavior.subject$
            .takeUntil(self.componentDestroyed$)
            .debug("quickSelectBehavior.subject$ Subscription")
            .subscribe(function (selection) {
                canvas.graph.reloadComponent(selection)
                    .then(function (reloadedComponent) {
                        self.componentConfiguration$
                            .debug("quickSelectBehavior.subject$ Next")
                            .next(reloadedComponent);
                    });
            });

        canvas.selectableBehavior.subject$
            .takeUntil(self.componentDestroyed$)
            .debug("selectableBehavior.subject$ Subscription")
            .subscribe(function (selection) {
                canvas.contextMenu.hide();
                // TODO - handle multi-select
                self.componentSelection$
                    .debug("componentSelection$ Next")
                    .next(selection.datum());
            });

        canvas.deselectableBehavior.subject$
            .takeUntil(self.componentDestroyed$)
            .debug("deselectableBehavior.subject$ Subscription")
            .subscribe(function () {
                canvas.contextMenu.hide();
                self.componentDeselection$
                    .debug("componentDeselection$ Next")
                    .next();
            });

        canvas.connectableBehavior.connected$
            .takeUntil(self.componentDestroyed$)
            .subscribe(function (connectRequest) {
                if (common.isDefinedAndNotNull(handleConnectionCreation)) {
                    var source = connectRequest.source;
                    var destination = connectRequest.destination;
                    var requiresDialog = (source.type !== 'funnel' || destination.type === 'remote-process-group');

                    var connectionCreation$;
                    if (requiresDialog) {
                        connectionCreation$ = handleConnectionCreation(flowDesignerApi, connectRequest);
                    } else {
                        connectionCreation$ = new rxjs.Subject();
                    }

                    connectionCreation$
                        .subscribe(function (connectionEntity) {
                            // ensure the connection bends points are added if necessary
                            connectionEntity.component.bends = connectRequest.bends;

                            // create the connection
                            flowDesignerApi.createConnection(canvas.getGroupId(), connectionEntity)
                                .subscribe(function (response) {
                                    // set on the connection manager
                                    canvas.graph.add({connections: response});

                                    // reload the connections source/destination components
                                    var sourceComponentId = canvas.connectionManager.getConnectionSourceComponentId(response);
                                    var destinationComponentId = canvas.connectionManager.getConnectionDestinationComponentId(response);

                                    canvas.graph.reloadConnectionSourceAndDestination(sourceComponentId, destinationComponentId);

                                    // update component visibility
                                    canvas.graph.updateVisibility();

                                    // update version info
                                    self.reloadVersionInfo();

                                    // resolve the configuration response
                                    connectionCreation$.complete();

                                    canvas.connectableBehavior.removeTempEdge();
                                }, function (errorResponse) {
                                    // if we opened a dialog, clean up
                                    if (requiresDialog && errorResponse.preventDefault) {
                                        connectionCreation$.error(errorResponse.message);
                                    }
                                });
                        }, function (failed) {
                            // creation was canceled
                            canvas.connectableBehavior.removeTempEdge();
                        });

                    if (!requiresDialog) {
                        connectionCreation$.next({
                            'component': {
                                'source': {
                                    'id': connectRequest.source.id,
                                    'groupId': connectRequest.source.component.parentGroupId,
                                    'type': common.getConnectableTypeForSource(connectRequest.source)
                                },
                                'destination': {
                                    'id': connectRequest.destination.id,
                                    'groupId': connectRequest.destination.component.parentGroupId,
                                    'type': common.getConnectableTypeForDestination(connectRequest.destination)
                                }
                            }
                        });
                    }
                }
            });

        $(window).on('keyup', keyupHandler);
    };

    /**
     * Request to configure a canvas component.
     *
     * @param id
     * @param type
     * @returns {Promise}
     */
    this.configure = function (id, type) {
        return new Promise(function (resolve, reject) {
            self.select(id, type)
                .then(function (g) {
                    // get the canvas component in question
                    switch (type.toLowerCase()) {
                        case 'controller-service':
                            flowDesignerApi.getControllerService(id)
                                .subscribe(function (controllerServiceEntity) {
                                    // trigger the configuration
                                    if (common.isDefinedAndNotNull(handleControllerServiceConfiguration)) {
                                        var controllerServiceConfiguration$ = handleControllerServiceConfiguration(flowDesignerApi, controllerServiceEntity);
                                        controllerServiceConfiguration$
                                            .subscribe(function (configuredControllerServiceEntity) {
                                                // update the controller service
                                                flowDesignerApi.updateControllerService(configuredControllerServiceEntity)
                                                    .subscribe(function (response) {
                                                        controllerServiceConfiguration$.complete();

                                                        fdsSnackBarService.openCoaster({
                                                            title: 'Success',
                                                            message: 'Service configuration saved.',
                                                            verticalPosition: 'bottom',
                                                            horizontalPosition: 'right',
                                                            icon: 'fa fa-check-circle-o',
                                                            color: '#1EB475',
                                                            duration: 3000
                                                        });

                                                        // resolve with the updated entity
                                                        resolve(response);
                                                    }, function (errorResponse) {
                                                        if (!errorResponse.preventDefault && !errorResponse.clientError) {
                                                            reject(errorResponse.message);
                                                        }
                                                    });
                                            }, function () {
                                                // configuration was cancelled, resolve with the original entity
                                                resolve(controllerServiceEntity);
                                            });
                                    } else {
                                        // no configuration callback installed
                                        reject('Configuration not supported for services.');
                                    }
                                }, function (errorResponse) {
                                    if (!errorResponse.preventDefault) {
                                        reject(errorResponse.message);
                                    }
                                });
                            break;
                        case 'processor':
                            var processorEntity = canvas.processorManager.get(id);
                            var revision = client.getRevision(processorEntity);

                            // trigger the configuration
                            if (common.isDefinedAndNotNull(handleProcessorConfiguration)) {
                                var processorConfiguration$ = handleProcessorConfiguration(flowDesignerApi, processorEntity);
                                processorConfiguration$
                                    .subscribe(function (configuredProcessorEntity) {
                                        var processor = $.extend({'revision': revision}, configuredProcessorEntity);

                                        // update the processor
                                        flowDesignerApi.updateProcessor(processor)
                                            .subscribe(function (response) {
                                                // update the graph
                                                canvas.processorManager.set(response);

                                                // refresh this processors connections
                                                canvas.connectionManager.getComponentConnections(response.id).forEach(function (connectionEntity) {
                                                    if (connectionEntity.permissions.canRead && connectionEntity.sourceId === response.id) {
                                                        canvas.connectionManager.reload(connectionEntity.id);
                                                    }
                                                });

                                                // update version info
                                                self.reloadVersionInfo();

                                                processorConfiguration$.complete();

                                                fdsSnackBarService.openCoaster({
                                                    title: 'Success!',
                                                    message: 'Processor configuration saved.',
                                                    verticalPosition: 'bottom',
                                                    horizontalPosition: 'right',
                                                    icon: 'fa fa-check-circle-o',
                                                    color: '#1EB475',
                                                    duration: 3000
                                                });

                                                // resolve with the updated entity
                                                resolve(response);
                                            }, function (errorResponse) {
                                                if (!errorResponse.preventDefault && !errorResponse.clientError) {
                                                    reject(errorResponse.message);
                                                }
                                            });
                                    }, function () {
                                        // configuration was cancelled, resolve with the original entity
                                        resolve(processorEntity);
                                    });
                            } else {
                                // no configuration callback installed
                                reject('Configuration not supported for this type of canvas component.');
                            }
                            break;
                        case 'connection':
                            var connectionEntity = canvas.connectionManager.get(id);
                            var revision = client.getRevision(connectionEntity);

                            var sourceType = common.getComponentTypeForSource(connectionEntity.sourceType);
                            var sourceConnectableId = sourceType === 'remote-process-group' ? connectionEntity.sourceGroupId : connectionEntity.sourceId;
                            var sourceEntity = canvas.graph.getComponent(sourceConnectableId, sourceType);

                            var destinationType = common.getComponentTypeForDestination(connectionEntity.destinationType);
                            var destinationConnectableId = destinationType === 'remote-process-group' ? connectionEntity.destinationGroupId : connectionEntity.destinationId;
                            var destinationEntity = canvas.graph.getComponent(destinationConnectableId, destinationType);

                            // trigger the configuration
                            if (common.isDefinedAndNotNull(handleConnectionConfiguration)) {
                                var connectionConfiguration$ = handleConnectionConfiguration(connectionEntity, sourceEntity, destinationEntity);
                                connectionConfiguration$
                                    .subscribe(function (configuredConnectionEntity) {
                                        // update the connection
                                        flowDesignerApi.updateConnection($.extend({
                                            'revision': revision
                                        }, configuredConnectionEntity))
                                            .subscribe(function (response) {
                                                // update the graph
                                                canvas.connectionManager.set(response);

                                                // update version info
                                                self.reloadVersionInfo();

                                                connectionConfiguration$.complete();

                                                fdsSnackBarService.openCoaster({
                                                    title: 'Success!',
                                                    message: 'Connection configuration saved.',
                                                    verticalPosition: 'bottom',
                                                    horizontalPosition: 'right',
                                                    icon: 'fa fa-check-circle-o',
                                                    color: '#1EB475',
                                                    duration: 3000
                                                });

                                                // resolve with the updated entity
                                                resolve(response);
                                            }, function (errorResponse) {
                                                if (!errorResponse.preventDefault && !errorResponse.clientError) {
                                                    reject(errorResponse.message);
                                                }
                                            });
                                    }, function () {
                                        // configuration was cancelled, resolve with the original entity
                                        resolve(connectionEntity);
                                    });
                            } else {
                                // no configuration callback installed
                                reject('Configuration not supported for this type of canvas component.');
                            }
                            break;
                        case 'remote-process-group':
                            var remoteProcessGroupEntity = canvas.remoteProcessGroupManager.get(id);

                            // trigger the configuration
                            if (common.isDefinedAndNotNull(handleRemoteProcessGroupConfiguration)) {
                                var rpgConfiguration$ = handleRemoteProcessGroupConfiguration(flowDesignerApi, remoteProcessGroupEntity);
                                rpgConfiguration$
                                    .subscribe(function (configuredRemoteProcessGroupEntity) {
                                        // ensure the revision is set
                                        configuredRemoteProcessGroupEntity['revision'] = client.getRevision(remoteProcessGroupEntity);

                                        // update the remote process group
                                        flowDesignerApi.updateRemoteProcessGroup(configuredRemoteProcessGroupEntity)
                                            .subscribe(function (response) {
                                                // update the graph
                                                canvas.remoteProcessGroupManager.set(response);

                                                // update version info
                                                self.reloadVersionInfo();

                                                rpgConfiguration$.complete();

                                                fdsSnackBarService.openCoaster({
                                                    title: 'Success!',
                                                    message: 'Remote process group configuration saved.',
                                                    verticalPosition: 'bottom',
                                                    horizontalPosition: 'right',
                                                    icon: 'fa fa-check-circle-o',
                                                    color: '#1EB475',
                                                    duration: 3000
                                                });

                                                // resolve with the updated entity
                                                resolve(response);
                                            }, function (errorResponse) {
                                                if (!errorResponse.preventDefault && !errorResponse.clientError) {
                                                    reject(errorResponse.message);
                                                }
                                            });
                                    }, function () {
                                        // configuration was cancelled, resolve with the original entity
                                        resolve(remoteProcessGroupEntity);
                                    });
                            } else {
                                // no configuration callback installed
                                reject('Configuration not supported for this type of canvas component.');
                            }
                            break;
                        case 'funnel':
                            reject('Configuration not supported for this type of canvas component.');
                            break;
                        default:
                            reject('Unable to find component matching specified type.');
                            break;
                    }
                })
                .catch(function (message) {
                    reject(message);
                });
        });
    };

    /**
     * Request to list components for a process group.
     *
     * @param pg
     * @returns {Promise}
     */
    this.list = function (pg) {
        return new Promise(function (resolve, reject) {
            // trigger the pg component listing
            if (common.isDefinedAndNotNull(handleProcessGroupComponentListing)) {
                var processGroupComponentListing$ = handleProcessGroupComponentListing(flowDesignerApi, client, pg);
                processGroupComponentListing$
                    .subscribe(
                        function (componentListing) {
                        },
                        function (error) {
                        },
                        function () {
                            // update version info
                            self.reloadVersionInfo();

                            // resolve
                            resolve();
                        });
            } else {
                // no component listing callback installed
                reject('Component listing not supported for this canvas component.');
            }
        });
    };

    this.onProcessorCreation = function (handleProcessorCreationRef) {
        handleProcessorCreation = handleProcessorCreationRef;
    };

    this.onProcessorConfiguration = function (handleProcessorConfigurationRef) {
        handleProcessorConfiguration = handleProcessorConfigurationRef;
    };

    this.onControllerServiceConfiguration = function (handleControllerServiceConfigurationRef) {
        handleControllerServiceConfiguration = handleControllerServiceConfigurationRef;
    };

    this.onProcessGroupComponentListing = function (handleProcessGroupComponentListingRef) {
        handleProcessGroupComponentListing = handleProcessGroupComponentListingRef;
    };

    this.onConnectionCreation = function (handleConnectionCreationRef) {
        handleConnectionCreation = handleConnectionCreationRef;
    };

    this.onConnectionConfiguration = function (handleConnectionConfigurationRef) {
        handleConnectionConfiguration = handleConnectionConfigurationRef;
    };

    this.onRemoteProcessGroupCreation = function (handleRemoteProcessGroupCreationRef) {
        handleRemoteProcessGroupCreation = handleRemoteProcessGroupCreationRef;
    };

    this.onRemoteProcessGroupConfiguration = function (handleRemoteProcessGroupConfigurationRef) {
        handleRemoteProcessGroupConfiguration = handleRemoteProcessGroupConfigurationRef;
    };

    /**
     * Selects a component or connection on the canvas.
     *
     * @param id
     * @param type
     * @returns {Promise}
     */
    this.select = function (id, type) {
        return new Promise(function (resolve, reject) {
            canvas.loadFlow()
                .then(function () {
                    switch (type.toLowerCase()) {
                        case 'controller-service':
                            // nothing to select
                            resolve();
                            break;
                        default:
                            var componentEntity = canvas.graph.getComponent(id, type);
                            if (common.isDefinedAndNotNull(componentEntity)) {
                                var g = canvas.getCanvasSvg().select('#id-' + componentEntity.id);

                                // only need to update selection if necessary
                                if (!g.classed('selected')) {
                                    // deselect the current selection
                                    var currentlySelected = canvas.getSelection();
                                    currentlySelected.classed('selected', false);

                                    // update the selection
                                    g.classed('selected', true);
                                }

                                resolve(g);
                            } else {
                                reject('Unable to find specified component.');
                            }
                            break;
                    }
                })
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        reject(errorResponse.message);
                    }
                });
        });
    };

    /**
     * Deselects all graph components
     */
    this.deselectAll = function () {
        canvas.deselectAll();
    };

    /**
     * Create graph component.
     *
     * @param componentType         The component type to create.
     * @param pt                    The point on the canvas where the component should be placed.
     */
    this.create = function (componentType, pt) {
        return new Promise(function (resolve, reject) {
            canvas.loadFlow()
                .then(function () {
                    switch (componentType.toLowerCase()) {
                        case 'processor':
                            if (common.isDefinedAndNotNull(handleProcessorCreation)) {
                                var processorCreation$ = handleProcessorCreation(flowDesignerApi);
                                processorCreation$
                                    .subscribe(function (processorDescriptor) {
                                        flowDesignerApi.createProcessor(canvas.getGroupId(), processorDescriptor, pt)
                                            .subscribe(function (response) {
                                                // add the processor to the graph
                                                canvas.graph.add({
                                                    'processors': response
                                                }, {
                                                    'selectAll': true
                                                });

                                                // update component visibility
                                                canvas.graph.updateVisibility();

                                                // update version info
                                                self.reloadVersionInfo();

                                                processorCreation$.complete();

                                                self.componentCreation$.next(response);
                                                resolve(response);
                                            }, function (errorResponse) {
                                                if (errorResponse.preventDefault) {
                                                    processorCreation$.error(errorResponse.message);
                                                    reject(errorResponse.message);
                                                }
                                            });
                                    }, function (errorResponse) {
                                        if (errorResponse.preventDefault) {
                                            reject(errorResponse.message);
                                        }
                                    });
                            } else {
                                reject('Unable to create processor.');
                            }
                            break;
                        case 'funnel':
                            flowDesignerApi.createFunnel(canvas.getGroupId(), pt)
                                .subscribe(function (response) {
                                    // add the funnel to the graph
                                    canvas.graph.add({
                                        'funnels': response
                                    }, {
                                        'selectAll': true
                                    });

                                    // update component visibility
                                    canvas.graph.updateVisibility();

                                    // update version info
                                    self.reloadVersionInfo();

                                    self.componentCreation$.next(response);
                                    resolve(response);
                                }, function (errorResponse) {
                                    if (errorResponse.preventDefault) {
                                        reject(errorResponse.message);
                                    }
                                });
                            break;
                        case 'remote-process-group':
                            if (common.isDefinedAndNotNull(handleRemoteProcessGroupCreation)) {
                                var rpgCreation$ = handleRemoteProcessGroupCreation(flowDesignerApi);
                                rpgCreation$
                                    .subscribe(function (targetUris) {
                                        flowDesignerApi.createRemoteProcessGroup(canvas.getGroupId(), pt, targetUris)
                                            .subscribe(function (response) {
                                                // add the remote process group to the graph
                                                canvas.graph.add({
                                                    'remoteProcessGroups': response
                                                }, {
                                                    'selectAll': true
                                                });

                                                // update component visibility
                                                canvas.graph.updateVisibility();

                                                // update version info
                                                self.reloadVersionInfo();

                                                rpgCreation$.complete();

                                                self.componentCreation$.next(response);
                                                resolve(response);
                                            }, function (errorResponse) {
                                                if (errorResponse.preventDefault) {
                                                    rpgCreation$.error(errorResponse.message);
                                                    reject(errorResponse.message);
                                                }
                                            });
                                    }, function (errorResponse) {
                                        if (errorResponse.preventDefault) {
                                            reject(errorResponse.message);
                                        }
                                    });
                            } else {
                                reject('Unable to create remote process group.');
                            }
                            break;
                        default:
                            reject('Unable to create requested component.');
                            break;
                    }
                })
                .catch(function (errorResponse) {
                    if(!errorResponse.preventDefault) {
                        reject(errorResponse.message);
                    }
                });
        });
    };

    /**
     * Prevent default event handling for drop zone.
     *
     * @param $event
     */
    this.allowDrop = function ($event) {
        $event.stopPropagation();
        $event.preventDefault();
    };

    /**
     * Drop handler.
     *
     * @param $event
     */
    this.dropHandler = function ($event) {
        var type = $event.dataTransfer.getData('component-type');
        var mouseX = $event.offsetX;
        var mouseY = $event.offsetY;
        var translate = canvas.view.getTranslate();
        var scale = canvas.view.getScale();

        // adjust the x and y coordinates accordingly
        var x = (mouseX / scale) - (translate[0] / scale);
        var y = (mouseY / scale) - (translate[1] / scale);

        this.create(type, {'x': x, 'y': y})
            .then(function (componentEntity) {})
            .catch(function (message) {
                fdsDialogService.openConfirm({
                    title: 'Create Component',
                    message: message,
                    acceptButton: 'Ok'
                });
            });
    };

    /**
     * Gets the current process group entity.
     *
     * @param id        The process group id.
     */
    this.getProcessGroupComponentListing = function (id) {
        return new Promise(function (resolve, reject) {
            flowDesignerApi.getProcessGroup(id)
                .subscribe(function (processGroupEntity) {
                    flowDesignerApi.getProcessGroupControllerServices(processGroupEntity.id).subscribe(function (controllerServices) {
                        var pg = $.extend({}, processGroupEntity, {'controllerServices': controllerServices});

                        resolve(pg);
                    }, function (errorResponse) {
                        if (!errorResponse.preventDefault) {
                            reject(errorResponse.message);
                        }
                    });
                }, function (errorResponse) {
                    if (!errorResponse.preventDefault) {
                        reject(errorResponse.message);
                    }
                });
        });
    };

    /**
     * Adds an action.
     */
    this.registerGlobalMenuItem = function (menuItem) {
        this.globalMenuItems.push(menuItem);
    };

    /**
     * Register a context menu item.
     *
     * @param menuItem
     */
    this.registerMenuItem = function (menuItem) {
        canvas.contextMenu.registerMenuItem(menuItem);
    };

    /**
     * Returns whether the flow has loaded initially.
     */
    this.isFlowLoaded = function () {
        return canvas.isFlowLoaded();
    };

    /**
     * Returns whether the current flow is dirty.
     *
     * @returns {boolean}
     */
    this.isFlowDirty = function () {
        return canvas.isFlowDirty();
    };

    /**
     * Returns whether this flow has been published.
     *
     * @returns {boolean}
     */
    this.isFlowPublished = function () {
        return canvas.isFlowPublished();
    };

    /**
     * Returns the current flow version.
     */
    this.getFlowVersion = function () {
        return canvas.getFlowVersion();
    };

    /**
     * Returns the timestamp for the current flow version.
     */
    this.getFlowVersionTimestamp = function () {
        return canvas.getFlowVersionTimestamp();
    };

    /**
     * Opens the services
     */
    this.openServices = function () {
        flowDesignerApi.getProcessGroup(canvas.getGroupId()).subscribe(function (processGroupEntity) {
            flowDesignerApi.getProcessGroupControllerServices(processGroupEntity.id).subscribe(function (controllerServices) {
                self.currentProcessGroupComponentListing$.next($.extend({}, processGroupEntity, {'controllerServices': controllerServices}));
            }, function (errorResponse) {
            });
        }, function (errorResponse) {
        });
    };

    /**
     * Reloads the current flow.
     */
    this.reload = function () {
        return canvas.refreshFlow();
    };

    /**
     * Reloads the current flow's version info
     */
    this.reloadVersionInfo = function () {
        return canvas.refreshVersionInfo();
    };

    /**
     * Disable the canvas refresh hot key.
     */
    this.disableRefreshHotKey = function () {
        allowPageRefresh = true;
    };

    /**
     * Enables hot keys
     */
    this.enableHotKeys = function () {
        hotKeysEnabled = true;
    };

    /**
     * Disables hot keys
     */
    this.disableHotKeys = function () {
        hotKeysEnabled = false;
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        this.flowDesignerCanvasElementRef = null;
        this.globalMenuItems = [];
        canvas.stopPolling();
        connectionManager.destroy();
        canvas.destroy();
        canvas.quickSelectBehavior.subject$
            .debug("quickSelectBehavior.subject$ Complete")
            .complete();
        canvas.selectableBehavior.subject$
            .debug("selectableBehavior.subject$ Complete")
            .complete();
        this.canvasGroupSelection$
            .debug("canvasGroupSelection$ Complete")
            .complete();
        this.componentSelection$
            .debug("componentSelection$ Complete")
            .complete();
        this.componentConfiguration$
            .debug("componentConfiguration$ Complete")
            .complete();
        this.currentProcessGroupComponentListing$
            .debug("currentProcessGroupComponentListing$ Complete")
            .complete();
        this.componentDestroyed$.next();
        this.componentDestroyed$.unsubscribe();

        $(window).off('keyup', keyupHandler);
    };
}

FlowDesignerCanvasComponent.prototype = {
    constructor: FlowDesignerCanvasComponent,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.init();
    },

    /**
     * Destroy the component
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

FlowDesignerCanvasComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-canvas',
        template: require('./fd.canvas.component.html!text'),
        queries: {
            flowDesignerCanvasElementRef: new ngCore.ViewChild('flowDesignerCanvasElementRef'),
            contextMenuElementRef: new ngCore.ViewChild('contextMenuElementRef')
        },

        // Each instance of this component gets its own instance of the following services
        providers: [
            {
                provide: 'Canvas',
                useFactory: canvasFactory.build(),
                deps: [
                    CanvasUtilsService,
                    CommonService,
                    fdsDialogsModule.FdsDialogService,
                    FdsStorageService
                ]
            }, //non-singleton
            Client, //non-singleton
            ProcessorManager, //non-singleton
            FunnelManager, //non-singleton
            ConnectionManager, //non-singleton
            RemoteProcessGroupManager, //non-singleton
            DraggableBehavior, //non-singleton
            ConnectableBehavior, //non-singleton
            EditableBehavior, //non-singleton
            SelectableBehavior, //non-singleton
            DeselectableBehavior, //non-singleton
            ContextMenu, //non-singleton
            QuickSelectBehavior //non-singleton
        ]
    })
];

FlowDesignerCanvasComponent.parameters = [
    fdsSnackBarsModule.FdsSnackBarService,
    fdsDialogsModule.FdsDialogService,
    'FlowDesignerApi',
    'Canvas',
    CommonService,
    CanvasUtilsService,
    Client,
    ProcessorManager,
    FunnelManager,
    ConnectionManager,
    RemoteProcessGroupManager,
    DraggableBehavior,
    ConnectableBehavior,
    EditableBehavior,
    SelectableBehavior,
    DeselectableBehavior,
    ContextMenu,
    QuickSelectBehavior,
    ngCore.ElementRef
];

module.exports = FlowDesignerCanvasComponent;
