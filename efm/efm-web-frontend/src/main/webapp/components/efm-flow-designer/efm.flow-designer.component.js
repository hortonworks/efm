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
var rxjs = $.extend({}, require('rxjs/Subject'), require('rxjs/Observable'));
var ngCore = require('@angular/core');
var ngMaterial = require('@angular/material');
var ngRouter = require('@angular/router');

var CommonService = require('@flow-designer/services/CommonService');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var ProcessorConfigurationComponent = require('@flow-designer/components/flow-designer-processor-configuration');
var RpgConfigurationComponent = require('@flow-designer/components/flow-designer-rpg-configuration');
var ExtensionCreationComponent = require('@flow-designer/components/flow-designer-extension-creation');
var ComponentListingComponent = require('@flow-designer/components/flow-designer-component-listing');
var ComponentConfigurationComponent = require('@flow-designer/components/flow-designer-controller-service-configuration');
var ConnectionCreationComponent = require('@flow-designer/components/flow-designer-connection-creation');
var ConnectionConfigurationComponent = require('@flow-designer/components/flow-designer-connection-configuration');
var RpgCreationComponent = require('@flow-designer/components/flow-designer-rpg-creation');
var ErrorService = require('services/efm.error.service.js');

var EfmFlowDesignerPublishFlow = require('components/efm-flow-designer/publish/efm.publish-flow.component.js');

var EfmService = require('services/efm.service.js');
var EfmApi = require('services/efm.api.service.js');
var FlowProvider = require('services/efm.flow-provider.service.js');
var DialogService = require('services/efm.dialog.service.js');

/**
 * EfmFlowDesigner constructor.
 *
 * @param efmService                        The EFM singleton service.
 * @param efmApi                            The EFM API.
 * @param flowProvider                      The flow provider.
 * @param common                            The common module.
 * @param canvasUtilsService                The canvas utils module.
 * @param ngZone                            The ngZone module.
 * @param activatedRoute                    The angular ActivatedRoute service.
 * @param dialog                            The angular material dialog.
 * @param dialogService                     The dialog service module.
 * @constructor
 */
function EfmFlowDesigner(efmService, efmApi, flowProvider, common, canvasUtilsService, ngZone, activatedRoute, dialog, dialogService, errorService) {
    var self = this;
    var dialogRef = null;

    // Subscriptions
    this.routeSubscription = activatedRoute.data
        .subscribe(function (data) {
            efmService.perspective = 'designer';
            efmService.agentClass = data.flow.agentClass;
        });

    // Subjects
    this.componentDestroyed$ = new rxjs.Subject();

    /**
     * Configure this component.
     */
    this.init = function () {
        // set flow designer canvas components on the EFM service application wide singleton
        efmService.flowDesignerCanvasComponent = this.flowDesignerCanvasComponent;
        efmService.componentListingHost = this.componentListingHost;

        /**
         * Determines whether the specified selection is empty.
         *
         * @param {selection} selection         The selection
         */
        var emptySelection = function (selection) {
            return ngZone.runOutsideAngular(function () {
                return selection.empty();
            });
        };

        /**
         * Determines whether the components in the specified selection are deletable.
         *
         * @param {selection} selection         The selection of currently selected components
         */
        var isDeletable = function (selection) {
            return ngZone.runOutsideAngular(function () {
                return canvasUtilsService.areDeletable(selection);
            });
        };

        /**
         * Determines whether the component in the specified selection is configurable.
         *
         * @param {selection} selection         The selection of currently selected components
         */
        var isConfigurable = function (selection) {
            return ngZone.runOutsideAngular(function () {
                return canvasUtilsService.isConfigurable(selection);
            });
        };

        /**
         * Determines whether the component in the specified selection has components to list.
         *
         * @param {selection} selection         The selection of currently selected components
         */
        var hasComponents = function (selection) {
            return ngZone.runOutsideAngular(function () {
                return canvasUtilsService.hasComponents(selection);
            });
        };

        // TODO: Do we
        // TODO: 1) handle 'common' context menu items and have them available by default (do we need to provide ability to remove any of these?)
        // TODO: 2) require client application to always create their own context menu items
        // TODO: 3) provide the ones we think the client application will want but the client application must regiser them via the flow designer canvas component
        // defines the available context menu actions and the conditions for which they apply
        var menuItems = [
            {
                id: 'reload-menu-item',
                condition: emptySelection,
                menuItem: {
                    clazz: 'fa fa-refresh',
                    text: 'Refresh',
                    action: 'reload'
                }
            },
            {
                id: 'show-configuration-menu-item',
                condition: isConfigurable,
                menuItem: {
                    clazz: 'fa fa-gear',
                    text: 'Configure',
                    action: 'showConfiguration'
                }
            },
            {
                id: 'delete-menu-item',
                condition: isDeletable,
                menuItem: {
                    clazz: 'fa fa-trash',
                    text: 'Delete',
                    action: 'delete'
                }
            },
            {
                id: 'show-component-listing-menu-item',
                condition: hasComponents,
                menuItem: {
                    clazz: 'fa fa-gear',
                    text: 'Services',
                    action: 'showComponentListing'
                }
            }
        ];

        var arrayLength = menuItems.length;
        for (var i = 0; i < arrayLength; i++) {
            this.flowDesignerCanvasComponent.registerMenuItem(menuItems[i]);
        }

        // registry the global menu items
        this.flowDesignerCanvasComponent.registerGlobalMenuItem({
            condition: function () {
                return true;
            },
            menuItem: {
                text: 'Open...',
                action: function () {
                    efmService.routeToOpenFlow();
                }
            }
        });
        this.flowDesignerCanvasComponent.registerGlobalMenuItem({
            separator: true,
            condition: function () {
                return self.flowDesignerCanvasComponent.isFlowDirty();
            }
        });
        this.flowDesignerCanvasComponent.registerGlobalMenuItem({
            condition: function () {
                return self.flowDesignerCanvasComponent.isFlowDirty();
            },
            menuItem: {
                text: 'Publish...',
                action: function () {
                    flowProvider.get().subscribe(function (flow) {
                        disableHotKeys();
                        // show the publish flow dialog
                        dialogRef = dialog.open(EfmFlowDesignerPublishFlow, {
                            width: '30%'
                        });

                        dialogRef.componentInstance.agentClass = flow.agentClass;

                        // add a reaction to the dialog beforeClose to enable closing dialog with 'esc' or by clicking off of the dialog
                        dialogRef.beforeClose()
                            .takeUntil(dialogRef.componentInstance.componentDestroyed$)
                            .subscribe(function () {
                                // cancel add property
                                dialogRef.componentInstance.cancel();
                                enableHotKeys();
                            });

                        // react to add a new property
                        dialogRef.componentInstance.subject$
                            .subscribe(function (comments) {
                                efmApi.publishFlow(flow.identifier, comments).subscribe(function () {
                                    dialogService.openCoaster({
                                        title: 'Success!',
                                        message: 'Flow published.'
                                    });

                                    // reload the canvas to update the version control details
                                    self.flowDesignerCanvasComponent.reload();

                                    // complete the add property request
                                    dialogRef.componentInstance.subject$.complete();
                                }, function (errorResponse) {
                                    dialogRef.componentInstance.subject$.error();
                                });
                            }, function () {
                                // close the dialog
                                dialogRef.close();
                            }, function () {
                                // close the dialog
                                dialogRef.close();
                            });
                    });
                }
            }
        });
        this.flowDesignerCanvasComponent.registerGlobalMenuItem({
            condition: function () {
                return self.flowDesignerCanvasComponent.isFlowDirty() && self.flowDesignerCanvasComponent.isFlowPublished();
            },
            menuItem: {
                text: 'Revert to last published',
                action: function () {
                    disableHotKeys();
                    dialogRef = dialogService.openConfirm({
                        title: 'Revert to last published version',
                        message: 'All changes made to this flow since it was published will be lost.',
                        cancelButton: 'Cancel',
                        acceptButton: 'Revert',
                        acceptButtonColor: 'fds-warn'
                    });

                    dialogRef.afterClosed().subscribe(function (confirm) {
                        if (confirm) {
                            efmApi.revertFlow(flowProvider.getFlowId()).subscribe(function () {
                                dialogService.openCoaster({
                                    title: 'Success!',
                                    message: 'Flow reverted.'
                                });

                                self.flowDesignerCanvasComponent.reload();
                            }, function (errorResponse) {
                            });
                        }
                        enableHotKeys();
                    });
                }
            }
        });

        // handle processor creation
        var handleProcessorCreation = function (flowDesignerApi) {
            var processorCreation$ = new rxjs.Subject();

            processorCreation$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas create()
                }, function (message) {
                    dialogRef.close();
                }, function () {
                    // complete the rpg creation subject which will trigger dialog closing
                    dialogRef.componentInstance.subject$.complete();
                });

            flowDesignerApi.getProcessorExtensions()
                .subscribe(function (extensions) {
                    if (common.isEmpty(extensions)) {
                        disableHotKeys();
                        dialogRef = dialogService.openConfirm({
                            title: 'Create Processor',
                            message: 'No Processor extensions are available.'
                        });
                        dialogRef.beforeClose().subscribe(function () {
                            enableHotKeys();
                        });
                    } else {
                        disableHotKeys();
                        dialogRef = dialog.open(ExtensionCreationComponent, {
                            width: '75%'
                        });

                        dialogRef.componentInstance.extensions = extensions;
                        dialogRef.componentInstance.extensionType = "Processor";

                        dialogRef.beforeClose()
                            .takeUntil(dialogRef.componentInstance.componentDestroyed$)
                            .subscribe(function () {
                                dialogRef.componentInstance.cancel();
                                enableHotKeys();
                            });

                        dialogRef.componentInstance.subject$
                            .subscribe(function (processorEntity) {
                                processorCreation$.next(processorEntity);
                            }, function () {
                                dialogRef.close();
                            }, function () {
                                dialogRef.close();
                            });
                    }
                }, function (errorResponse) {
                });

            return processorCreation$;
        };

        // handle process group configuration
        var handleProcessGroupComponentListing = function (flowDesignerCanvasComponent, flowDesignerApi, client, componentListing) {
            var processGroupComponentListing$ = new rxjs.Subject();

            var componentListingComponent = efmService.componentListingHost.loadComponent(ComponentListingComponent);

            // load the process group component listing component
            componentListingComponent.title = 'Services';
            componentListingComponent.flowDesignerApi = flowDesignerApi;
            componentListingComponent.flowDesignerCanvasComponent = flowDesignerCanvasComponent;
            componentListingComponent.client = client;
            componentListingComponent.entity = componentListing;

            // react to request to add a component
            componentListingComponent.subject$
                .takeUntil(componentListingComponent.componentDestroyed$)
                .subscribe(function (componentListing) {
                    componentListingComponent.entity = componentListing;
                    componentListingComponent.filterComponents();
                    // emit configured entity to subscribers
                    processGroupComponentListing$.next(componentListing);
                }, function (error) {
                    processGroupComponentListing$.complete();
                });

            componentListingComponent.componentConfiguration$
                .takeUntil(componentListingComponent.componentDestroyed$)
                .subscribe(function (extensionEntity) {
                    efmService.routeToProcessGroupControllerServiceConfiguration(flowProvider.getFlowId(), extensionEntity.component.parentGroupId, extensionEntity.id);
                });

            componentListingComponent.componentConfigurationComplete$
                .takeUntil(componentListingComponent.componentDestroyed$)
                .subscribe(function (extensionEntity) {
                    // update the component listing component with the latest extensions
                    flowDesignerCanvasComponent.getProcessGroupComponentListing(extensionEntity.component.parentGroupId).then(function (pg) {
                        componentListingComponent.entity = pg;
                        componentListingComponent.filterComponents();
                    }).catch(function (message) {
                        // unable to get process group controller services
                        efmService.routeToFlowDesigner(flowProvider.getFlowId(), true, message);
                    });
                });

            return processGroupComponentListing$;
        };

        // handle controller service configuration
        var handleControllerServiceConfiguration = function (flowDesignerCanvasComponent, flowDesignerApi, componentEntity) {
            var controllerServiceConfiguration$ = new rxjs.Subject();

            // load the processor configuration Angular component
            var controllerServiceConfigurationComponent = efmService.sidenavHost.loadComponent(ComponentConfigurationComponent);

            // initialize the processor configuration Angular component inputs
            controllerServiceConfigurationComponent.flowDesignerCanvasComponent = flowDesignerCanvasComponent;
            controllerServiceConfigurationComponent.flowDesignerApi = flowDesignerApi;
            controllerServiceConfigurationComponent.componentEntity = componentEntity;

            disableHotKeys();
            // open the sidenav
            efmService.sidenav.open();

            // add a reaction to the sidenav closeStart to enable closing sidenav with 'esc' or by clicking off of the sidenav
            efmService.sidenav.closedStart
                .takeUntil(controllerServiceConfigurationComponent.componentDestroyed$)
                .subscribe(function () {
                    if (common.isDefinedAndNotNull(controllerServiceConfigurationComponent.flowDesignerCanvasComponent)) {
                        // cancel component configuration
                        controllerServiceConfigurationComponent.cancel();
                    }
                    enableHotKeys();
                });

            // react to request to apply an update to a component's configuration
            controllerServiceConfigurationComponent.subject$
                .finally(function() {
                    controllerServiceConfigurationComponent.unchanged = true;
                })
                .subscribe(function (configuredControllerServiceEntity) {
                    // emit configured entity to subscribers
                    controllerServiceConfiguration$.next(configuredControllerServiceEntity);
                }, function (error) {//This is only invoked via the controllerServiceConfigurationComponent.cancel()
                    // emit error to controller service configuration subscribers
                    controllerServiceConfiguration$.error(error);
                });

            controllerServiceConfiguration$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas component configure()
                }, function (error) {
                    // .error() calls are handled in the flow designer canvas component configure()
                }, function () {
                    controllerServiceConfigurationComponent.subject$.complete();
                });

            return controllerServiceConfiguration$;
        };

        // handle processor configuration
        var handleProcessorConfiguration = function (flowDesignerCanvasComponent, flowDesignerApi, componentEntity) {
            var processorConfiguration$ = new rxjs.Subject();

            // load the processor configuration Angular component
            var processorConfigurationComponent = efmService.sidenavHost.loadComponent(ProcessorConfigurationComponent);

            // initialize the processor configuration Angular component inputs
            processorConfigurationComponent.flowDesignerCanvasComponent = flowDesignerCanvasComponent;
            processorConfigurationComponent.flowDesignerApi = flowDesignerApi;
            processorConfigurationComponent.componentEntity = componentEntity;

            disableHotKeys();
            // open the sidenav
            efmService.sidenav.open();

            // add a reaction to the sidenav closeStart to enable closing sidenav with 'esc' or by clicking off of the sidenav
            efmService.sidenav.closedStart
                .takeUntil(processorConfigurationComponent.componentDestroyed$)
                .subscribe(function () {
                    if (common.isDefinedAndNotNull(processorConfigurationComponent.flowDesignerCanvasComponent)) {
                        // cancel component configuration
                        processorConfigurationComponent.cancel();
                    }
                    enableHotKeys();
                });

            // react to request to apply an update to a component's configuration
            processorConfigurationComponent.subject$
                .finally(function() {
                    processorConfigurationComponent.unchanged = true;
                })
                .subscribe(function (configuredProcessorEntity) {
                    // emit configured entity to subscribers
                    processorConfiguration$.next(configuredProcessorEntity);
                }, function (error) {// This is only invoked via the processorConfigurationComponent.cancel()
                    // emit error to processor configuration subscribers
                    processorConfiguration$.error(error);
                });

            processorConfiguration$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas component configure()
                }, function (error) {
                    // .error() calls are handled in the flow designer canvas component configure()
                }, function () {
                    processorConfigurationComponent.subject$.complete();
                });

            return processorConfiguration$;
        };

        var handleConnectionCreation = function (flowDesignerApi, connectionCreationRequest) {
            var connectionCreation$ = new rxjs.Subject();

            disableHotKeys();
            dialogRef = dialog.open(ConnectionCreationComponent, {
                width: '50%'
            });

            dialogRef.componentInstance.sourceEntity = connectionCreationRequest.source;
            dialogRef.componentInstance.destinationEntity = connectionCreationRequest.destination;

            // add a reaction to the dialog afterClosed to enable closing dialog with 'esc' or by clicking off of the dialog
            dialogRef.beforeClose()
                .takeUntil(dialogRef.componentInstance.componentDestroyed$)
                .subscribe(function () {
                    // cancel connection creation
                    dialogRef.componentInstance.cancel();
                    enableHotKeys();
                });

            // react to request to create a canvas connection component
            dialogRef.componentInstance.subject$
                .subscribe(function (connectionEntity) {
                    connectionCreation$.next(connectionEntity);
                }, function () {
                    // emit error to connection creation subscribers
                    connectionCreation$.error();

                    // close the dialog
                    dialogRef.close();
                }, function () {
                    // close the dialog
                    dialogRef.close();
                });

            // handle the subject completion
            connectionCreation$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas connection creation subject
                }, function (message) {
                    // allow the user to re-configure the connection in the case of expected error
                    dialogRef.close();
                }, function () {
                    // complete the connection creation subject which will trigger dialog closing
                    dialogRef.componentInstance.subject$.complete();
                });

            return connectionCreation$;
        };

        var handleConnectionConfiguration = function (flowDesignerCanvasComponent, connectionEntity, sourceEntity, destinationEntity, newDestinationEntity) {
            var connectionConfiguration$ = new rxjs.Subject();

            // load the processor configuration Angular component
            var connectionConfigurationComponent = efmService.sidenavHost.loadComponent(ConnectionConfigurationComponent);

            // initialize the connection configuration Angular component inputs
            connectionConfigurationComponent.connectionEntity = connectionEntity;
            connectionConfigurationComponent.sourceEntity = sourceEntity;
            connectionConfigurationComponent.destinationEntity = destinationEntity;
            connectionConfigurationComponent.newDestinationEntity = newDestinationEntity;

            disableHotKeys();
            // open the sidenav
            efmService.sidenav.open();

            // add a reaction to the sidenav closeStart to enable closing sidenav with 'esc' or by clicking off of the sidenav
            efmService.sidenav.closedStart
                .takeUntil(connectionConfigurationComponent.componentDestroyed$)
                .subscribe(function () {
                    // cancel component configuration
                    connectionConfigurationComponent.cancel();
                    enableHotKeys();
                });

            // react to request to apply an update to a component's configuration
            connectionConfigurationComponent.subject$
                .finally(function() {
                    connectionConfigurationComponent.unchanged = true;
                })
                .subscribe(function (configuredProcessorEntity) {
                    // emit configured entity to subscribers
                    connectionConfiguration$.next(configuredProcessorEntity);
                }, function (error) {
                    // emit error to processor configuration subscribers
                    connectionConfiguration$.error(error);
                });

            connectionConfiguration$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas component configure()
                }, function (error) {
                    // .error() should never be invoked as we may need to allow the user to re-configure the processor
                }, function () {
                    connectionConfigurationComponent.subject$.complete();
                });

            return connectionConfiguration$;
        };

        var handleRemoteProcessGroupCreation = function (flowDesignerApi) {
            var rpgCreation$ = new rxjs.Subject();

            disableHotKeys();
            dialogRef = dialog.open(RpgCreationComponent, {
                width: '50%'
            });

            dialogRef.beforeClose()
                .takeUntil(dialogRef.componentInstance.componentDestroyed$)
                .subscribe(function () {
                    dialogRef.componentInstance.cancel();
                    enableHotKeys();
                });

            dialogRef.componentInstance.subject$
                .subscribe(function (rpgEntity) {
                    rpgCreation$.next(rpgEntity);
                }, function () {
                    dialogRef.close();
                }, function () {
                    dialogRef.close();
                });

            rpgCreation$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas create()
                }, function (message) {
                    dialogRef.close();
                }, function () {
                    // complete the rpg creation subject which will trigger dialog closing
                    dialogRef.componentInstance.subject$.complete();
                });

            return rpgCreation$;
        };

        var handleRemoteProcessGroupConfiguration = function (flowDesignerCanvasComponent, flowDesignerApi, componentEntity) {
            var rpgConfiguration$ = new rxjs.Subject();

            var rpgConfigurationComponent = efmService.sidenavHost.loadComponent(RpgConfigurationComponent);

            rpgConfigurationComponent.flowDesignerCanvasComponent = flowDesignerCanvasComponent;
            rpgConfigurationComponent.flowDesignerApi = flowDesignerApi;
            rpgConfigurationComponent.componentEntity = componentEntity;

            disableHotKeys();
            // open the sidenav
            efmService.sidenav.open();

            // add a reaction to the sidenav closeStart to enable closing sidenav with 'esc' or by clicking off of the sidenav
            efmService.sidenav.closedStart
                .takeUntil(rpgConfigurationComponent.componentDestroyed$)
                .subscribe(function () {
                    rpgConfigurationComponent.cancel();
                    enableHotKeys();
                });

            // react to request to apply an update to a component's configuration
            rpgConfigurationComponent.subject$
                .finally(function() {
                    rpgConfigurationComponent.unchanged = true;
                })
                .subscribe(function (rpgComponentEntity) {
                    rpgConfiguration$.next(rpgComponentEntity);
                }, function (error) {
                    rpgConfiguration$.error(error);
                });

            rpgConfiguration$
                .subscribe(function () {
                    // .next() calls are handled in the flow designer canvas component configure()
                }, function (error) {
                    // .error() calls are handled in the flow designer canvas component configure()
                }, function () {
                    rpgConfigurationComponent.subject$.complete();
                });

            return rpgConfiguration$;
        };

        var enableHotKeys = function () {
            if (common.isDefinedAndNotNull(self.flowDesignerCanvasComponent)) {
                self.flowDesignerCanvasComponent.enableHotKeys();
            }
        };

        var disableHotKeys = function () {
            if (common.isDefinedAndNotNull(self.flowDesignerCanvasComponent)) {
                self.flowDesignerCanvasComponent.disableHotKeys();
            }
        };

        /** flow designer canvas component 1 **/

        // set handler that react to request to create a flow designer canvas component processor
        this.flowDesignerCanvasComponent.onProcessorCreation(handleProcessorCreation);

        // set handler that react to request to configure a flow designer canvas component processor
        this.flowDesignerCanvasComponent.onProcessorConfiguration(function (flowDesignerApi, componentEntity) {
            return handleProcessorConfiguration(self.flowDesignerCanvasComponent, flowDesignerApi, componentEntity);
        });

        // set handler that react to request to configure a flow designer canvas process group's controller service
        this.flowDesignerCanvasComponent.onControllerServiceConfiguration(function (flowDesignerApi, componentEntity) {
            return handleControllerServiceConfiguration(self.flowDesignerCanvasComponent, flowDesignerApi, componentEntity);
        });

        // set handler that react to request to configure a flow designer canvas process group
        this.flowDesignerCanvasComponent.onProcessGroupComponentListing(function (flowDesignerApi, client, processGroupComponentListing) {
            return handleProcessGroupComponentListing(self.flowDesignerCanvasComponent, flowDesignerApi, client, processGroupComponentListing);
        });

        // set handler that react to request to create a flow designer canvas component connection
        this.flowDesignerCanvasComponent.onConnectionCreation(handleConnectionCreation);

        // set handler that react to request to configure a flow designer canvas component connection
        this.flowDesignerCanvasComponent.onConnectionConfiguration(function (connectionEntity, sourceEntity, destinationEntity) {
            return handleConnectionConfiguration(self.flowDesignerCanvasComponent, connectionEntity, sourceEntity, destinationEntity);
        });

        // set handler that react to request to create a flow designer canvas component RPG
        this.flowDesignerCanvasComponent.onRemoteProcessGroupCreation(handleRemoteProcessGroupCreation);

        // set handler that react to request to configure a flow designer canvas component RPG
        this.flowDesignerCanvasComponent.onRemoteProcessGroupConfiguration(function (flowDesignerApi, componentEntity) {
            return handleRemoteProcessGroupConfiguration(self.flowDesignerCanvasComponent, flowDesignerApi, componentEntity);
        });

        // react to request to creation of a flow designer canvas component
        this.flowDesignerCanvasComponent.componentCreation$
            .takeUntil(this.componentDestroyed$)
            .debug("creationSubscription1 subscribe")
            .subscribe(function (componentEntity) {
                efmService.routeToFlowDesignerCanvasComponent(flowProvider.getFlowId(), componentEntity.id, componentEntity.type);
            });

        // react to request to select a flow designer canvas component
        this.flowDesignerCanvasComponent.componentSelection$
            .takeUntil(this.componentDestroyed$)
            .debug("selectionSubscription1 subscribe")
            .subscribe(function (componentEntity) {
                efmService.routeToFlowDesignerCanvasComponent(flowProvider.getFlowId(), componentEntity.id, componentEntity.type);
            });

        // react to request to deselect a flow designer canvas component
        this.flowDesignerCanvasComponent.componentDeselection$
            .takeUntil(this.componentDestroyed$)
            .debug("selectionSubscription1 subscribe")
            .subscribe(function () {
                efmService.routeToFlowDesigner(flowProvider.getFlowId());
            });

        // react to request to select a flow designer canvas group
        this.flowDesignerCanvasComponent.canvasGroupSelection$
            .takeUntil(this.componentDestroyed$)
            .debug("selectionSubscription2 subscribe")
            .subscribe(function (group) {
                efmService.routeToFlowDesigner(flowProvider.getFlowId());
            });

        // react to request to configure a flow designer canvas component
        this.flowDesignerCanvasComponent.componentConfiguration$
            .takeUntil(this.componentDestroyed$)
            .debug("selectionSubscription1 subscribe")
            .subscribe(function (componentEntity) {
                efmService.routeToConfiguration(flowProvider.getFlowId(), componentEntity.id, componentEntity.type);
            });

        // react to request to configure a flow designer canvas process group
        this.flowDesignerCanvasComponent.currentProcessGroupComponentListing$
            .takeUntil(this.componentDestroyed$)
            .debug("pgConfigurationSubscription1 subscribe")
            .subscribe(function (processGroupComponentListing) {
                efmService.routeToProcessGroupComponentListing(flowProvider.getFlowId(), processGroupComponentListing.id);
            });
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        if(common.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
        this.componentDestroyed$.next();
        this.componentDestroyed$.complete();

        this.routeSubscription.unsubscribe();
    };
};

EfmFlowDesigner.prototype = {
    constructor: EfmFlowDesigner,

    /**
     * Initialize the component.
     */
    ngOnInit: function () {
        this.init();
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

EfmFlowDesigner.annotations = [
    new ngCore.Component({
        template: require('./efm.flow-designer.component.html!text'),
        queries: {
            flowDesignerCanvasComponent: new ngCore.ViewChild('flowDesignerCanvasComponent'),
            componentListingHost: new ngCore.ViewChild('componentListingHost')
        }
    })
];

EfmFlowDesigner.parameters = [
    EfmService,
    EfmApi,
    FlowProvider,
    CommonService,
    CanvasUtilsService,
    ngCore.NgZone,
    ngRouter.ActivatedRoute,
    ngMaterial.MatDialog,
    DialogService,
    ErrorService
];

module.exports = EfmFlowDesigner;
