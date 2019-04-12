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

var rxjs = require('rxjs/Observable');

/**
 * FlowDesignerApiFactory constructor.
 *
 * @param http                                              The angular http service.
 * @param flowProvider                                      The flow provider service.
 * @param commonService                                     The common service.
 * @param dialogService                                     The dialog service.
 * @constructor
 */
function EfmFlowDesignerApiFactory(http, flowProvider, commonService, dialogService) {
    return (function () {

        // --------------
        // funnel mapping
        // --------------

        // funnel from canvas may be partial depending on what's changing
        var mapFunnelToEfm = function (funnel) {
            var f = {
                'revision': funnel.revision,
                'componentConfiguration': {
                    'identifier': funnel.id
                }
            };

            if (commonService.isDefinedAndNotNull(funnel.component)) {
                f.componentConfiguration['position'] = funnel.component.position;
            }

            return f;
        };

        // funnel is never partial when mapping from efm
        var mapFunnelFromEfm = function (funnel) {
            return {
                'id': funnel.componentConfiguration.identifier,
                'uri': funnel.uri,
                'permissions': {
                    'canRead': true,
                    'canWrite': true
                },
                'revision': funnel.revision,
                'position': funnel.componentConfiguration.position,
                'component': {
                    'id': funnel.componentConfiguration.identifier,
                    'parentGroupId': funnel.componentConfiguration.groupIdentifier,
                    'position': funnel.componentConfiguration.position
                }
            };
        };

        // -----------------
        // processor mapping
        // -----------------

        // processor from canvas may be partial depending on what's changing
        var mapProcessorToEfm = function (processor) {
            var p = {
                'revision': processor.revision,
                'componentConfiguration': {
                    'identifier': processor.id,
                }
            };

            if (commonService.isDefinedAndNotNull(processor.component)) {
                p.componentConfiguration['position'] = processor.component.position;
                p.componentConfiguration['name'] = processor.component.name;
                p.componentConfiguration['style'] = processor.component.style;

                if (commonService.isDefinedAndNotNull(processor.component.config)) {
                    p.componentConfiguration['concurrentlySchedulableTaskCount'] = processor.component.config.concurrentlySchedulableTaskCount;
                    p.componentConfiguration['autoTerminatedRelationships'] = processor.component.config.autoTerminatedRelationships;
                    p.componentConfiguration['properties'] = processor.component.config.properties;
                    p.componentConfiguration['penaltyDuration'] = processor.component.config.penaltyDuration;
                    p.componentConfiguration['runDurationMillis'] = processor.component.config.runDurationMillis;
                    p.componentConfiguration['schedulingPeriod'] = processor.component.config.schedulingPeriod;
                    p.componentConfiguration['schedulingStrategy'] = processor.component.config.schedulingStrategy;
                    p.componentConfiguration['yieldDuration'] = processor.component.config.yieldDuration;
                    p.componentConfiguration['comments'] = processor.component.config.comments;
                }
            }

            return p;
        };

        // processor is never partial when mapping from efm
        var mapProcessorFromEfm = function (processor) {
            return {
                'id': processor.componentConfiguration.identifier,
                'uri': processor.uri,
                'permissions': {
                    'canRead': true,
                    'canWrite': true
                },
                'revision': processor.revision,
                'position': processor.componentConfiguration.position,
                'component': {
                    'id': processor.componentConfiguration.identifier,
                    'parentGroupId': processor.componentConfiguration.groupIdentifier,
                    'bundle': processor.componentConfiguration.bundle,
                    'name': processor.componentConfiguration.name,
                    'type': processor.componentConfiguration.type,
                    'style': processor.componentConfiguration.style,
                    'relationships': processor.componentDefinition.supportedRelationships,
                    'validationErrors': processor.validationErrors,
                    'supportsDynamicProperties': processor.componentDefinition.supportsDynamicProperties,
                    'config': {
                        'concurrentlySchedulableTaskCount': processor.componentConfiguration.concurrentlySchedulableTaskCount,
                        'autoTerminatedRelationships': processor.componentConfiguration.autoTerminatedRelationships,
                        'properties': processor.componentConfiguration.properties,
                        'descriptors': mapPropertyDescriptorsFromEfm(processor.componentDefinition.propertyDescriptors),
                        'penaltyDuration': processor.componentConfiguration.penaltyDuration,
                        'runDurationMillis': processor.componentConfiguration.runDurationMillis,
                        'schedulingPeriod': processor.componentConfiguration.schedulingPeriod,
                        'schedulingStrategy': processor.componentConfiguration.schedulingStrategy,
                        'yieldDuration': processor.componentConfiguration.yieldDuration,
                        'comments': processor.componentConfiguration.comments
                    }
                }
            };
        };

        // ---------------------
        // process group mapping
        // ---------------------

        // process group from canvas may be partial depending on what's changing
        var mapProcessGroupToEfm = function (processGroup) {
            var pg = {
                'flowContent': {
                    'identifier': processGroup.id,
                    'name': processGroup.name,
                    'description': processGroup.description
                }
            };

            return pg;
        };

        // process group is never partial when mapping from efm
        var mapProcessGroupFromEfm = function (processGroup) {
            return {
                'id': processGroup.flowContent.identifier,
                'name': processGroup.flowContent.name,
                'description': ''
            };
        };

        // --------------------------
        // controller service mapping
        // --------------------------

        // controller service from canvas may be partial depending on what's changing
        var mapControllerServiceToEfm = function (controllerService) {
            var cs = {
                'revision': controllerService.revision,
                'componentConfiguration': controllerService.component,
                'uri': controllerService.uri
            };

            return cs;
        };

        // controller service is never partial when mapping from efm
        var mapControllerServiceFromEfm = function (controllerService) {
            var component = {
                'id': controllerService.componentConfiguration.identifier,
                'permissions': {
                    'canRead': true,
                    'canWrite': true
                },
                'revision': controllerService.revision,
                'uri': controllerService.uri,
                'component': {
                    'id': controllerService.componentConfiguration.identifier,
                    'comments': controllerService.componentConfiguration.comments,
                    'bundle': controllerService.componentConfiguration.bundle,
                    'name': controllerService.componentConfiguration.name,
                    'parentGroupId': controllerService.componentConfiguration.groupIdentifier,
                    'properties': controllerService.componentConfiguration.properties,
                    'descriptors': mapPropertyDescriptorsFromEfm(controllerService.componentDefinition.propertyDescriptors),
                    'type': controllerService.componentConfiguration.type,
                    'state': controllerService.componentConfiguration.state,
                    'persistsState': controllerService.componentConfiguration.persistsState,
                    'restricted': controllerService.componentConfiguration.restricted,
                    'multipleVersionsAvailable': controllerService.componentConfiguration.multipleVersionsAvailable,
                    'controllerServiceApis': controllerService.componentConfiguration.controllerServiceApis,
                    'extensionMissing': controllerService.componentConfiguration.extensionMissing,
                    'validationStatus': controllerService.validationStatus,
                    'validationErrors': controllerService.validationErrors,
                    'supportsDynamicProperties': controllerService.componentDefinition.supportsDynamicProperties
                }
            };
            return component;
        };

        // controller service is never partial when mapping from efm
        var mapControllerServicesFromEfm = function (component) {
            var components = [];
            var arrayLength = component.elements.length;
            for (var i = 0; i < arrayLength; i++) {
                components.push(mapControllerServiceFromEfm(component.elements[i]));
            }
            return components;
        };

        // ------------------
        // connection mapping
        // ------------------

        // connection from canvas may be partial depending on what's changing
        var mapConnectionToEfm = function (connection) {
            var c = {
                'revision': connection.revision,
                'componentConfiguration': {
                    'identifier': connection.id
                }
            };

            if (commonService.isDefinedAndNotNull(connection.component)) {
                c.componentConfiguration['name'] = connection.component.name;
                c.componentConfiguration['source'] = connection.component.source;
                c.componentConfiguration['destination'] = connection.component.destination;
                c.componentConfiguration['selectedRelationships'] = connection.component.selectedRelationships;
                c.componentConfiguration['bends'] = connection.component.bends;
                c.componentConfiguration['labelIndex'] = connection.component.labelIndex;
                c.componentConfiguration['zIndex'] = connection.component.zIndex;
                c.componentConfiguration['flowFileExpiration'] = connection.component.flowFileExpiration;
                c.componentConfiguration['backPressureDataSizeThreshold'] = connection.component.backPressureDataSizeThreshold;
                c.componentConfiguration['backPressureObjectThreshold'] = connection.component.backPressureObjectThreshold;
            }

            return c;
        };

        // connection is never partial when mapping from efm
        var mapConnectionFromEfm = function (connection) {
            return {
                'id': connection.componentConfiguration.identifier,
                'uri': connection.uri,
                'permissions': {
                    'canRead': true,
                    'canWrite': true
                },
                'sourceId': connection.componentConfiguration.source.id,
                'sourceGroupId': connection.componentConfiguration.source.groupId,
                'sourceType': connection.componentConfiguration.source.type,
                'destinationId': connection.componentConfiguration.destination.id,
                'destinationGroupId': connection.componentConfiguration.destination.groupId,
                'destinationType': connection.componentConfiguration.destination.type,
                'bends': connection.componentConfiguration.bends,
                'labelIndex': connection.componentConfiguration.labelIndex,
                'revision': connection.revision,
                'zIndex': connection.componentConfiguration.zIndex,
                'component': {
                    'id': connection.componentConfiguration.identifier,
                    'parentGroupId': connection.componentConfiguration.groupIdentifier,
                    'name': connection.componentConfiguration.name,
                    'selectedRelationships': connection.componentConfiguration.selectedRelationships,
                    'bends': connection.componentConfiguration.bends,
                    'labelIndex': connection.componentConfiguration.labelIndex,
                    'zIndex': connection.componentConfiguration.zIndex,
                    'source': connection.componentConfiguration.source,
                    'destination': connection.componentConfiguration.destination,
                    'flowFileExpiration': connection.componentConfiguration.flowFileExpiration,
                    'backPressureDataSizeThreshold': connection.componentConfiguration.backPressureDataSizeThreshold,
                    'backPressureObjectThreshold': connection.componentConfiguration.backPressureObjectThreshold
                }
            };
        };

        // ----------------------------
        // remote process group mapping
        // ----------------------------

        // remote process group from canvas may be partial depending on what's changing
        var mapRemoteProcessGroupToEfm = function (remoteProcessGroup) {
            var r = {
                'revision': remoteProcessGroup.revision,
                'componentConfiguration': {
                    'identifier': remoteProcessGroup.id,
                }
            };

            if (commonService.isDefinedAndNotNull(remoteProcessGroup.component)) {
                r.componentConfiguration['position'] = remoteProcessGroup.component.position;
                r.componentConfiguration['targetUris'] = remoteProcessGroup.component.targetUris;
                r.componentConfiguration['communicationsTimeout'] = remoteProcessGroup.component.communicationsTimeout;
                r.componentConfiguration['yieldDuration'] = remoteProcessGroup.component.yieldDuration;
                r.componentConfiguration['transportProtocol'] = remoteProcessGroup.component.transportProtocol;
                r.componentConfiguration['localNetworkInterface'] = remoteProcessGroup.component.localNetworkInterface;
                r.componentConfiguration['proxyHost'] = remoteProcessGroup.component.proxyHost;
                r.componentConfiguration['proxyPort'] = remoteProcessGroup.component.proxyPort;
            }

            return r;
        };

        // remote process group is never partial when mapping from efm
        var mapRemoteProcessGroupFromEfm = function (remoteProcessGroup) {
            return {
                'id': remoteProcessGroup.componentConfiguration.identifier,
                'uri': remoteProcessGroup.uri,
                'permissions': {
                    'canRead': true,
                    'canWrite': true
                },
                'revision': remoteProcessGroup.revision,
                'position': remoteProcessGroup.componentConfiguration.position,
                'component': {
                    'id': remoteProcessGroup.componentConfiguration.identifier,
                    'parentGroupId': remoteProcessGroup.componentConfiguration.groupIdentifier,
                    'name': remoteProcessGroup.componentConfiguration.name,
                    'targetUris': remoteProcessGroup.componentConfiguration.targetUris,
                    'communicationsTimeout': remoteProcessGroup.componentConfiguration.communicationsTimeout,
                    'yieldDuration': remoteProcessGroup.componentConfiguration.yieldDuration,
                    'transportProtocol': remoteProcessGroup.componentConfiguration.transportProtocol,
                    'localNetworkInterface': remoteProcessGroup.componentConfiguration.localNetworkInterface,
                    'proxyHost': remoteProcessGroup.componentConfiguration.proxyHost,
                    'proxyPort': remoteProcessGroup.componentConfiguration.proxyPort
                }
            };
        };

        // -----------------
        // extension mapping
        // -----------------

        var mapExtensionFromEfm = function (extension) {
            return {
                'bundle': {
                    'group': extension.group,
                    'artifact': extension.artifact,
                    'version': extension.version
                },
                'type': extension.type,
                'description': extension.description,
                'tags': extension.tags
            };
        };

        // --------------------
        // property descriptors
        // --------------------

        // map property descriptors
        var mapPropertyDescriptorsFromEfm = function (propertyDescriptors) {
            var mappedPropertyDescriptors = {};

            Object.keys(propertyDescriptors).forEach(function (propertyName) {
                var propertyDescriptor = propertyDescriptors[propertyName];

                mappedPropertyDescriptors[propertyName] = mapPropertyDescriptorFromEfm(propertyDescriptor);
            });

            return mappedPropertyDescriptors;
        };

        // map property descriptor
        var mapPropertyDescriptorFromEfm = function (propertyDescriptor) {
            var mappedPropertyDescriptor = {
                'name': propertyDescriptor.name,
                'displayName': propertyDescriptor.displayName,
                'description': propertyDescriptor.description,
                'defaultValue': propertyDescriptor.defaultValue,
                'required': propertyDescriptor.required,
                'sensitive': propertyDescriptor.sensitive,
                'dynamic': propertyDescriptor.dynamic,
                'expressionLanguageScope': propertyDescriptor.expressionLanguageScope,
                'expressionLanguageScopeDescription': propertyDescriptor.expressionLanguageScopeDescription
            };

            // if controller service details are provided, map it into the identifiesControllerService fields
            if (commonService.isDefinedAndNotNull(propertyDescriptor.typeProvidedByValue)) {
                mappedPropertyDescriptor['identifiesControllerService'] = propertyDescriptor.typeProvidedByValue.type;
                mappedPropertyDescriptor['identifiesControllerServiceBundle'] = {
                    'group': propertyDescriptor.typeProvidedByValue.group,
                    'artifact': propertyDescriptor.typeProvidedByValue.artifact,
                    'version': propertyDescriptor.typeProvidedByValue.version
                }
            }

            if (Array.isArray(propertyDescriptor.allowableValues)) {
                mappedPropertyDescriptor['allowableValues'] = propertyDescriptor.allowableValues.map(function (allowableValue) {
                    return {
                        'canRead': true,
                        'allowableValue': allowableValue
                    };
                });
            }

            return mappedPropertyDescriptor;
        };

        /**
         * EfmFlowDesignerApi constructor.
         *
         * @param elementRef        Reference to the element this api service is associated with
         * @constructor
         */
        function EfmFlowDesignerApi(elementRef) {
            var self = this;
            var clientId = null;

            /**
             * Get the id for this client.
             *
             * @return {Observable<String>} Observable of the id for this client
             */
            this.getClientId = function () {
                if (clientId === null) {
                    return http.get('../api/designer/client-id', {
                            'responseType': 'text'
                        }).map(function (cId) {
                            clientId = cId;
                            return cId;
                        })
                        .catch(function (errorResponse) {
                            if(!errorResponse.preventDefault) {
                                dialogService.openConfirm({
                                    title: 'Get Client ID',
                                    message: errorResponse.message
                                });
                            }

                            return rxjs.Observable.throw(errorResponse);
                        })
                        .shareReplay();
                } else {
                    return new rxjs.Observable.of(clientId);
                }
            };

            /**
             * Get the flow for the specified process group.
             *
             * @param {string} processGroupId       The process group id
             * @return {Observable<object>}         Observable of the flow for this class/flow
             */
            this.getFlow = function (processGroupId) {
                return flowProvider.get()
                    .concatMap(function (flow) {
                        // when process group id is undefined, use the value from the flow provider
                        var pgId = processGroupId;
                        if (typeof pgId === 'undefined' || pgId === null) {
                            pgId = flow.rootProcessGroupIdentifier;
                        }

                        // get the flow
                        return http.get('../api/designer/flows/' + flow.identifier + '/process-groups/' + pgId)
                            .map(function (response) {
                                return {
                                    'id': response.flowContent.identifier,
                                    'permissions': {
                                        'canRead': true,
                                        'canWrite': true
                                    },
                                    'versionInfo': response.versionInfo,
                                    'flow': {
                                        'funnels': response.flowContent.funnels.map(mapFunnelFromEfm),
                                        'processors': response.flowContent.processors.map(mapProcessorFromEfm),
                                        'remoteProcessGroups': response.flowContent.remoteProcessGroups.map(mapRemoteProcessGroupFromEfm),
                                        'connections': response.flowContent.connections.map(mapConnectionFromEfm)
                                    }
                                };
                            })
                            .catch(function (errorResponse) {
                                if(!errorResponse.preventDefault) {
                                    dialogService.openConfirm({
                                        title: 'Get Flow',
                                        message: errorResponse.message
                                    });
                                }

                                return rxjs.Observable.throw(errorResponse);
                            });
                    })
                    .shareReplay();
            };

            /**
             * Get the version information for this flow.
             *
             * @returns {Observable<object>}            Observable of the flow's version info
             */
            this.getVersionInfo = function () {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/version-info')
                    .map(function (response) {
                        return response.versionInfo;
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Version Info',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Gets the available processor extensions.
             *
             * @returns {array}     available processor extensions
             */
            this.getProcessorExtensions = function () {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/types/processors')
                    .map(function (response) {
                        return response.componentTypes.map(mapExtensionFromEfm);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Processor Extensions',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Gets the available controller service extensions.
             *
             * @returns {array}     available controller service extensions
             */
            this.getControllerServiceExtensions = function () {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/types/controller-services')
                    .map(function (response) {
                        return response.componentTypes.map(mapExtensionFromEfm);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Service Extensions',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Get a processor's descriptor.
             *
             * @param componentEntity
             * @param propertyName
             * @return {Observable<object>} Observable of the component descriptors
             */
            this.getProcessorPropertyDescriptor = function (componentEntity, propertyName) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/processors/' + componentEntity.id + '/descriptors/' + propertyName)
                    .map(function (response) {
                        return mapPropertyDescriptorFromEfm(response.propertyDescriptor);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Processor Property Descriptor',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Get a controller service's descriptor.
             *
             * @param componentEntity
             * @param propertyName
             * @return {Observable<object>} Observable of the component descriptors
             */
            this.getControllerServicePropertyDescriptor = function (componentEntity, propertyName) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/controller-services/' + componentEntity.id + '/descriptors/' + propertyName)
                    .map(function (response) {
                        return mapPropertyDescriptorFromEfm(response.propertyDescriptor);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Service Property Descriptor',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Returns the el specification for the current flow.
             */
            this.getELSpecification = function () {
                return flowProvider.getElSpecification()
                    .map(function (elSpecification) {
                        return elSpecification.spec.operations;
                    });
            };

            // -------
            // funnels
            // -------

            /**
             * Creates a new funnel at the specified point.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} pt                       The point that the funnel was dropped.
             */
            this.createFunnel = function (processGroupId, pt) {
                return self.getClientId()
                    .concatMap(function (clientId) {
                        var funnelEntity = {
                            'revision': {
                                'version': 0,
                                'clientId': clientId
                            },
                            'componentConfiguration': {
                                'position': {
                                    'x': pt.x,
                                    'y': pt.y
                                }
                            }
                        };

                        return http.post('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/funnels', funnelEntity)
                                .map(function (funnel) {
                                    return mapFunnelFromEfm(funnel);
                                })
                                .catch(function (errorResponse) {
                                    if(!errorResponse.preventDefault) {
                                        dialogService.openConfirm({
                                            title: 'Create Funnel',
                                            message: errorResponse.message
                                        });
                                    }

                                    return rxjs.Observable.throw(errorResponse);
                                });
                    })
                    .shareReplay();
            };

            /**
             * Get a funnel with the specified id.
             *
             * @param funnel
             * @return {Observable<object>} Observable of the funnel
             */
            this.getFunnel = function (funnel) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/funnels/' + funnel.id)
                    .map(mapFunnelFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Funnel',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a funnel.
             *
             * @param {object} Updated funnel
             * @return {Observable<object>} Observable of the funnel
             */
            this.updateFunnel = function (funnel) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/funnels/' + funnel.id, mapFunnelToEfm(funnel))
                    .map(function (funnel) {
                        return mapFunnelFromEfm(funnel);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Funnel',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Remove a funnel.
             *
             * @param {object} Funnel to remove
             * @return {Observable<object>} Observable of the funnel
             */
            this.removeFunnel = function (funnel) {
                var queryParams = {
                    'params': {
                        'version': funnel.revision.version,
                        'clientId': funnel.revision.clientId
                    }
                };

                return http.delete('../api/designer/flows/' + flowProvider.getFlowId() + '/funnels/' + funnel.id, queryParams)
                    .map(function (funnel) {
                        return mapFunnelFromEfm(funnel);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Delete Funnel',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            // ----------
            // processors
            // ----------

            /**
             * Creates a new processor at the specified point.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} processorDescriptor      The descriptor of processor to create
             * @param {object} pt                       The point that the processor was dropped.
             * @return {Observable<object>} Observable of the processor
             */
            this.createProcessor = function (processGroupId, processorDescriptor, pt) {
                return self.getClientId()
                    .concatMap(function (clientId) {
                        var processorEntity = {
                            'revision': {
                                'version': 0,
                                'clientId': clientId
                            },
                            'componentConfiguration': {
                                'type': processorDescriptor.type,
                                'bundle': {
                                    'group': processorDescriptor.bundle.group,
                                    'artifact': processorDescriptor.bundle.artifact,
                                    'version': processorDescriptor.bundle.version
                                },
                                'position': {
                                    'x': pt.x,
                                    'y': pt.y
                                }
                            }
                        };

                        return http.post('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/processors', processorEntity)
                            .map(function (processor) {
                                return mapProcessorFromEfm(processor);
                            })
                            .catch(function (errorResponse) {
                                if(!errorResponse.preventDefault) {
                                    dialogService.openConfirm({
                                        title: 'Create Processor',
                                        message: errorResponse.message
                                    });
                                }

                                return rxjs.Observable.throw(errorResponse);
                            });
                    })
                    .shareReplay();
            };

            /**
             * Get a processor with the specified id.
             *
             * @param processor
             * @return {Observable<object>} Observable of the processor
             */
            this.getProcessor = function (processor) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/processors/' + processor.id)
                    .map(mapProcessorFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Processor',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a processor.
             *
             * @param {object} Updated processor
             * @return {Observable<object>} Observable of the processor
             */
            this.updateProcessor = function (processor) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/processors/' + processor.id, mapProcessorToEfm(processor))
                    .map(function (processor) {
                        return mapProcessorFromEfm(processor);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Processor',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Remove a processor.
             *
             * @param {object} Processor to remove
             * @return {Observable<object>} Observable of the processor
             */
            this.removeProcessor = function (processor) {
                var queryParams = {
                    'params': {
                        'version': processor.revision.version,
                        'clientId': processor.revision.clientId
                    }
                };

                return http.delete('../api/designer/flows/' + flowProvider.getFlowId() + '/processors/' + processor.id, queryParams)
                    .map(function (processor) {
                        return mapProcessorFromEfm(processor);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Delete Processor',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            // -----------
            // process groups
            // -----------

            /**
             * Get a process group with the specified id.
             *
             * @param {string} processGroupId           The process group id
             * @return {Observable<object>} Observable of the process group
             */
            this.getProcessGroup = function (processGroupId) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '?includeChildren=false')
                    .map(mapProcessGroupFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Process Group',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Get a process group's controller services with the specified id.
             *
             * @param {string} processGroupId           The process group id
             * @return {Observable<object>} Observable of the process group
             */
            this.getProcessGroupControllerServices = function (processGroupId) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/controller-services')
                    .map(mapControllerServicesFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Process Group Services',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Create a controller service.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} extensionDescriptor      The descriptor of processor to create
             * @returns {*}
             */
            this.createControllerService = function (processGroupId, extensionDescriptor) {
                return self.getClientId()
                    .concatMap(function (clientId) {
                        var controllerServiceEntity = {
                            'revision': {
                                'version': 0,
                                'clientId': clientId
                            },
                            'componentConfiguration': {
                                'type': extensionDescriptor.type,
                                'bundle': {
                                    'group': extensionDescriptor.bundle.group,
                                    'artifact': extensionDescriptor.bundle.artifact,
                                    'version': extensionDescriptor.bundle.version
                                }
                            }
                        };

                        return http.post('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/controller-services', controllerServiceEntity)
                            .map(function (controllerService) {
                                return mapControllerServiceFromEfm(controllerService);
                            })
                            .catch(function (errorResponse) {
                                if(!errorResponse.preventDefault) {
                                    dialogService.openConfirm({
                                        title: 'Create Service',
                                        message: errorResponse.message
                                    });
                                }

                                return rxjs.Observable.throw(errorResponse);
                            });
                    })
                    .shareReplay();
            };

            /**
             * Get a controller service with the specified id.
             *
             * @param {object} csId           The controller service id
             * @return {Observable<object>} Observable of the connection
             */
            this.getControllerService = function (csId) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/controller-services/' + csId)
                    .map(mapControllerServiceFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Service',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a controller service
             *
             * @param {object} cs           The controller service
             * @returns {*}
             */
            this.updateControllerService = function (cs) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/controller-services/' + cs.id, mapControllerServiceToEfm(cs))
                    .map(function (controllerService) {
                        return mapControllerServiceFromEfm(controllerService);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Service',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Delete a controller service.
             *
             * @param controllerService
             * @returns {*}
             */
            this.deleteControllerService = function (controllerService) {
                var queryParams = {
                    'params': {
                        'version': controllerService.revision.version,
                        'clientId': controllerService.revision.clientId
                    }
                };

                return http.delete('../api/designer/flows/' + flowProvider.getFlowId() + '/controller-services/' + controllerService.id, queryParams)
                    .map(function (deletedControllerService) {
                        return deletedControllerService;
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Delete Service',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a process group.
             *
             * @param {object} Updated process group
             * @return {Observable<object>} Observable of the process group
             */
            this.updateProcessGroup = function (processGroup) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroup.id, mapProcessGroupToEfm(processGroup))
                    .map(function (pg) {
                        return mapProcessGroupFromEfm(pg);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Process Group',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            // -----------
            // connections
            // -----------

            /**
             * Creates a new connection.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} connection               The connection
             * @return {Observable<object>} Observable of the connection
             */
            this.createConnection = function (processGroupId, connection) {
                return self.getClientId()
                    .concatMap(function (clientId) {
                        var connectionEntity = {
                            'revision': {
                                'version': 0,
                                'clientId': clientId
                            },
                            'componentConfiguration': {
                                'source': connection.component.source,
                                'destination': connection.component.destination,
                                'selectedRelationships': connection.component.selectedRelationships,
                                'bends': connection.component.bends
                            }
                        };

                        return http.post('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/connections', connectionEntity)
                            .map(function (connection) {
                                return mapConnectionFromEfm(connection);
                            })
                            .catch(function (errorResponse) {
                                if(!errorResponse.preventDefault) {
                                    dialogService.openConfirm({
                                        title: 'Create Connection',
                                        message: errorResponse.message
                                    });
                                }

                                return rxjs.Observable.throw(errorResponse);
                            });
                    })
                    .shareReplay();
            };

            /**
             * Get a connection with the specified id.
             *
             * @param connection
             * @return {Observable<object>} Observable of the connection
             */
            this.getConnection = function (connection) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/connections/' + connection.id)
                    .map(mapConnectionFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Connection',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a connection
             *
             * @param {object} connection           The connection
             * @returns {*}
             */
            this.updateConnection = function (connection) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/connections/' + connection.id, mapConnectionToEfm(connection))
                    .map(function (mappedConnection) {
                        return mapConnectionFromEfm(mappedConnection);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Connection',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Remove a connection.
             *
             * @param {object} Connection to remove
             * @return {Observable<object>} Observable of the connection
             */
            this.removeConnection = function (connection) {
                var queryParams = {
                    'params': {
                        'version': connection.revision.version,
                        'clientId': connection.revision.clientId
                    }
                };

                return http.delete('../api/designer/flows/' + flowProvider.getFlowId() + '/connections/' + connection.id, queryParams)
                    .map(function (connection) {
                        return mapConnectionFromEfm(connection);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Delete Connection',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            // ---------------------
            // remote process groups
            // ---------------------

            /**
             * Creates a new remote process group at the specified point.
             *
             * @param {string} processGroupId                   The process group id
             * @param {object} pt                               The point that the remote process group was dropped.
             * @param {object} targetUri                        The target uri
             * @return {Observable<object>}                     Observable of the remote process group
             */
            this.createRemoteProcessGroup = function (processGroupId, pt, targetUris) {
                return self.getClientId()
                    .concatMap(function (clientId) {
                        var remoteProcessGroupEntity = {
                            'revision': {
                                'version': 0,
                                'clientId': clientId
                            },
                            'componentConfiguration': {
                                'position': {
                                    'x': pt.x,
                                    'y': pt.y
                                },
                                'targetUris': targetUris,
                            }
                        };

                        return http.post('../api/designer/flows/' + flowProvider.getFlowId() + '/process-groups/' + processGroupId + '/remote-process-groups', remoteProcessGroupEntity)
                            .map(function (rpg) {
                                return mapRemoteProcessGroupFromEfm(rpg);
                            })
                            .catch(function (errorResponse) {
                                if(!errorResponse.preventDefault) {
                                    dialogService.openConfirm({
                                        title: 'Create Remote Process Group',
                                        message: errorResponse.message
                                    });
                                }

                                return rxjs.Observable.throw(errorResponse);
                            });
                    })
                    .shareReplay();
            };

            /**
             * Get a remote process group with the specified id.
             *
             * @param remoteProcessGroup
             * @return {Observable<object>}         Observable of the remote process group
             */
            this.getRemoteProcessGroup = function (remoteProcessGroup) {
                return http.get('../api/designer/flows/' + flowProvider.getFlowId() + '/remote-process-groups/' + remoteProcessGroup.id)
                    .map(mapRemoteProcessGroupFromEfm)
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Get Remote Process Group',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Update a remote process group.
             *
             * @param {object} remoteProcessGroup            Updated remote process group
             * @return {Observable<object>}                  Observable of the remote process group
             */
            this.updateRemoteProcessGroup = function (remoteProcessGroup) {
                return http.put('../api/designer/flows/' + flowProvider.getFlowId() + '/remote-process-groups/' + remoteProcessGroup.id, mapRemoteProcessGroupToEfm(remoteProcessGroup))
                    .map(function (rpg) {
                        return mapRemoteProcessGroupFromEfm(rpg);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Update Remote Process Group',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

            /**
             * Remove a remote process group.
             *
             * @param {object} remoteProcessGroup               Remote process group to remove
             * @return {Observable<object>}                     Observable of the remote process group
             */
            this.removeRemoteProcessGroup = function (remoteProcessGroup) {
                var queryParams = {
                    'params': {
                        'version': remoteProcessGroup.revision.version,
                        'clientId': remoteProcessGroup.revision.clientId
                    }
                };

                return http.delete('../api/designer/flows/' + flowProvider.getFlowId() + '/remote-process-groups/' + remoteProcessGroup.id, queryParams)
                    .map(function (rpg) {
                        return mapRemoteProcessGroupFromEfm(rpg);
                    })
                    .catch(function (errorResponse) {
                        if(!errorResponse.preventDefault) {
                            dialogService.openConfirm({
                                title: 'Delete Remote Process Group',
                                message: errorResponse.message
                            });
                        }

                        return rxjs.Observable.throw(errorResponse);
                    })
                    .shareReplay();
            };

        };

        EfmFlowDesignerApi.prototype = {
            constructor: EfmFlowDesignerApi
        };

        return EfmFlowDesignerApi;
    }());
};

EfmFlowDesignerApiFactory.prototype = {
    constructor: EfmFlowDesignerApiFactory
};

module.exports = {
    build: EfmFlowDesignerApiFactory
};
