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
var $ = require('jquery');

/**
 * FlowDesignerApiFactory constructor.
 *
 * @param ErrorResponse                                              The flow designer ErrorResponse.
 * @constructor
 */
function FlowDesignerApiFactory(ErrorResponse) {
    return (function () {
        /**
         * FlowDesignerApi constructor.
         *
         * @param elementRef        Reference to the element this api service is associated with
         * @constructor
         */
        function FlowDesignerApi(elementRef) {
            var self = this;
            this.flowDesignerCanvasId = elementRef.nativeElement.getAttribute('id');

            var FUNNELS = 'funnels';
            var CONNECTIONS = 'connections';
            var PROCESSORS = 'processors';
            var PROCESS_GROUPS = 'process-groups';
            var PG_CONTROLLER_SERVICES = 'pg-controller-services';
            var CONTROLLER_SERVICES = 'controller-services';
            var REMOTE_PROCESS_GROUP = 'remote-process-group';
            var MAX_COMPONENTS = 1000;

            var descriptors = {
                "bootstrap.servers": {
                    "name": "bootstrap.servers",
                    "displayName": "Kafka Brokers",
                    "description": "A comma-separated list of known Kafka Brokers in the format <host>:<port>",
                    "defaultValue": "localhost:9092",
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "VARIABLE_REGISTRY",
                    "expressionLanguageScopeDescription": "Variable Registry Only"
                },
                "security.protocol": {
                    "name": "security.protocol",
                    "displayName": "Security Protocol",
                    "description": "Protocol used to communicate with brokers. Corresponds to Kafka's 'security.protocol' property.",
                    "defaultValue": "PLAINTEXT",
                    "allowableValues": [{
                        "allowableValue": {
                            "displayName": "PLAINTEXT",
                            "value": "PLAINTEXT",
                            "description": "PLAINTEXT"
                        }, "canRead": true
                    }, {
                        "allowableValue": {"displayName": "SSL", "value": "SSL", "description": "SSL"},
                        "canRead": true
                    }, {
                        "allowableValue": {
                            "displayName": "SASL_PLAINTEXT",
                            "value": "SASL_PLAINTEXT",
                            "description": "SASL_PLAINTEXT"
                        }, "canRead": true
                    }, {
                        "allowableValue": {"displayName": "SASL_SSL", "value": "SASL_SSL", "description": "SASL_SSL"},
                        "canRead": true
                    }],
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                },
                "sasl.kerberos.service.name": {
                    "name": "sasl.kerberos.service.name",
                    "displayName": "Kerberos Service Name",
                    "description": "The Kerberos principal name that Kafka runs as. This can be defined either in Kafka's JAAS config or in Kafka's config. Corresponds to Kafka's 'security.protocol' property.It is ignored unless one of the SASL options of the <Security Protocol> are selected.",
                    "required": false,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                },
                "ssl.context.service": {
                    "name": "ssl.context.service",
                    "displayName": "SSL Context Service",
                    "description": "Specifies the SSL Context Service to use for communicating with Kafka.",
                    "allowableValues": [],
                    "required": false,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported",
                    "identifiesControllerService": "org.apache.nifi.ssl.SSLContextService",
                    "identifiesControllerServiceBundle": {
                        "group": "org.apache.nifi",
                        "artifact": "nifi-standard-services-api-nar",
                        "version": "1.8.0-SNAPSHOT"
                    }
                },
                "topic": {
                    "name": "topic",
                    "displayName": "Topic Name(s)",
                    "description": "The name of the Kafka Topic(s) to pull from. More than one can be supplied if comma separated.",
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "VARIABLE_REGISTRY",
                    "expressionLanguageScopeDescription": "Variable Registry Only"
                },
                "group.id": {
                    "name": "group.id",
                    "displayName": "Group ID",
                    "description": "A Group ID is used to identify consumers that are within the same consumer group. Corresponds to Kafka's 'group.id' property.",
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "VARIABLE_REGISTRY",
                    "expressionLanguageScopeDescription": "Variable Registry Only"
                },
                "auto.offset.reset": {
                    "name": "auto.offset.reset",
                    "displayName": "Offset Reset",
                    "description": "Allows you to manage the condition when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted). Corresponds to Kafka's 'auto.offset.reset' property.",
                    "defaultValue": "latest",
                    "allowableValues": [{
                        "allowableValue": {
                            "displayName": "earliest",
                            "value": "earliest",
                            "description": "Automatically reset the offset to the earliest offset"
                        }, "canRead": true
                    }, {
                        "allowableValue": {
                            "displayName": "latest",
                            "value": "latest",
                            "description": "Automatically reset the offset to the latest offset"
                        }, "canRead": true
                    }, {
                        "allowableValue": {
                            "displayName": "none",
                            "value": "none",
                            "description": "Throw exception to the consumer if no previous offset is found for the consumer's group"
                        }, "canRead": true
                    }],
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                },
                "key-attribute-encoding": {
                    "name": "key-attribute-encoding",
                    "displayName": "Key Attribute Encoding",
                    "description": "FlowFiles that are emitted have an attribute named 'kafka.key'. This property dictates how the value of the attribute should be encoded.",
                    "defaultValue": "utf-8",
                    "allowableValues": [{
                        "allowableValue": {
                            "displayName": "UTF-8 Encoded",
                            "value": "utf-8",
                            "description": "The key is interpreted as a UTF-8 Encoded string."
                        }, "canRead": true
                    }, {
                        "allowableValue": {
                            "displayName": "Hex Encoded",
                            "value": "hex",
                            "description": "The key is interpreted as arbitrary binary data and is encoded using hexadecimal characters with uppercase letters"
                        }, "canRead": true
                    }],
                    "required": true,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                },
                "message-demarcator": {
                    "name": "message-demarcator",
                    "displayName": "Message Demarcator",
                    "description": "Since KafkaConsumer receives messages in batches, you have an option to output FlowFiles which contains all Kafka messages in a single batch for a given topic and partition and this property allows you to provide a string (interpreted as UTF-8) to use for demarcating apart multiple Kafka messages. This is an optional property and if not provided each Kafka message received will result in a single FlowFile which  time it is triggered. To enter special character such as 'new line' use CTRL+Enter or Shift+Enter depending on the OS",
                    "required": false,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "VARIABLE_REGISTRY",
                    "expressionLanguageScopeDescription": "Variable Registry Only"
                },
                "max.poll.records": {
                    "name": "max.poll.records",
                    "displayName": "Max Poll Records",
                    "description": "Specifies the maximum number of records Kafka should return in a single poll.",
                    "defaultValue": "10000",
                    "required": false,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                },
                "max-uncommit-offset-wait": {
                    "name": "max-uncommit-offset-wait",
                    "displayName": "Max Uncommitted Time",
                    "description": "Specifies the maximum amount of time allowed to pass before offsets must be committed. This value impacts how often offsets will be committed.  Committing offsets less often increases throughput but also increases the window of potential data duplication in the event of a rebalance or JVM restart between commits.  This value is also related to maximum poll records and the use of a message demarcator.  When using a message demarcator we can have far more uncommitted messages than when we're not as there is much less for us to keep track of in memory.",
                    "defaultValue": "1 secs",
                    "required": false,
                    "sensitive": false,
                    "dynamic": false,
                    "expressionLanguageScope": "NONE",
                    "expressionLanguageScopeDescription": "Not Supported"
                }
            };

            // ensures the specific id is unique
            var ensureUnique = function (id, componentType) {
                var components = getEntry(componentType);

                var matchingComponent = components.find(function (component) {
                    return component.id === id;
                });
                return typeof matchingComponent === 'undefined';
            };

            // gets a unique id or -1 if no id is available
            var getComponentId = function () {
                var id;

                var i = 0;
                var unique = false;
                for (var i = 0; i < MAX_COMPONENTS && !unique; i++) {
                    id = Math.floor(Math.random() * MAX_COMPONENTS);

                    if (ensureUnique(id, PROCESSORS) &&
                        ensureUnique(id, FUNNELS) &&
                        ensureUnique(id, REMOTE_PROCESS_GROUP) &&
                        ensureUnique(id, CONNECTIONS)) {

                        unique = true;
                    }
                }

                if (unique) {
                    return id;
                } else {
                    return -1;
                }
            };

            // gets a canvas entry or creates it if it does not exist
            var getCanvasEntry = function () {
                if (self.flowDesignerCanvasId in localStorage) {
                    return JSON.parse(localStorage.getItem(self.flowDesignerCanvasId));
                } else {
                    var canvasStorage = {};
                    localStorage.setItem(self.flowDesignerCanvasId, JSON.stringify(canvasStorage));
                    return canvasStorage;
                }
            };

            // gets an entry or creates it if it does not exist
            var getEntry = function (componentType) {
                var canvasStorage = getCanvasEntry(self.flowDesignerCanvasId);
                if (componentType in canvasStorage) {
                    return canvasStorage[componentType];
                } else {
                    canvasStorage[componentType] = [];
                    localStorage.setItem(self.flowDesignerCanvasId, JSON.stringify(canvasStorage));
                    return canvasStorage[componentType];
                }
            };

            // set an entry
            var setEntry = function (componentType, entry) {
                var canvasStorage = getCanvasEntry(self.flowDesignerCanvasId);
                canvasStorage[componentType] = entry;
                localStorage.setItem(self.flowDesignerCanvasId, JSON.stringify(canvasStorage));
            };

            // helper method to find an item
            var findComponent = function (componentType, id) {
                var components = getEntry(componentType);

                return components.find(function (item) {
                    return item['id'] === id;
                });
            };

            // helper method to find an item index
            var findComponentIndex = function (componentType, id) {
                var components = getEntry(componentType);

                return components.findIndex(function (item) {
                    return item['id'] === id;
                });
            };

            // helper method to remove all connections for the specified component
            var removeConnectionsForComponent = function (id) {
                var connections = getEntry(CONNECTIONS);
                var componentConnections = connections.filter(function (connection) {
                    return connection['sourceId'] === id || connection['destinationId'] === id;
                });

                // remove each of the component connections
                componentConnections.forEach(function (connection) {
                    self.removeConnection(connection);
                });
            };

            /**
             * Get the id for this client.
             *
             * @return {Observable<String>} Observable of the id for this client
             */
            this.getClientId = function () {
                return rxjs.Observable.of('my-client-id');
            };

            /**
             * Get the flow for the specified class/flow id.
             *
             * @param {string} processGroupId       The process group id
             * @return {Observable<object>}         Observable of the flow for this class/flow
             */
            this.getFlow = function (processGroupId) {
                // when process group id is undefined, use the root PG
                var pgId = processGroupId;
                if (typeof pgId === 'undefined' || pgId === null) {
                    pgId = 'root';
                }

                return rxjs.Observable.of({
                        'id': pgId,
                        'permissions': {
                            'canRead': true,
                            'canWrite': true
                        },
                        'flow': {
                            'funnels': getEntry(FUNNELS),
                            'processors': getEntry(PROCESSORS),
                            'remoteProcessGroups': getEntry(REMOTE_PROCESS_GROUP),
                            'connections': getEntry(CONNECTIONS)
                        }
                    });
            };

            /**
             * Get the version information for this flow.
             *
             * @returns {Observable<object>}            Observable of the flow's version info
             */
            this.getVersionInfo = function () {
                return rxjs.Observable.of(undefined);
            };

            /**
             * Gets the available processor extensions.
             * @returns {array}     avaialble processor extensions
             */
            this.getProcessorExtensions = function (flowId) {
                return rxjs.Observable.of([{
                    'bundle': {
                        'group': 'org::apache::nifi::minifi',
                        'artifact': 'minifi-system',
                        'version': '0.6.0'
                    },
                    'description': 'The ConsumeKafka processor description goes here.',
                    'type': 'org.apache.nifi.minifi.processors.ConsumeKafka',
                    'tags': []
                }]);
            };

            /**
             * Gets the available controller service extensions.
             *
             * @returns {array}     avaialble contoller extensions
             */
            this.getControllerServiceExtensions = function (flowId) {
                return rxjs.Observable.of([{
                    'bundle': {
                        'group': 'org::apache::nifi::minifi',
                        'artifact': 'minifi-system',
                        'version': '0.6.0'
                    },
                    'description': 'The SSLContextService controller description goes here.',
                    'type': 'org.apache.nifi.minifi.controllers.SSLContextService',
                    'tags': []
                }]);
            };

            /**
             * Get a processor property descriptor.
             *
             * @param componentEntity
             * @param propertyName
             * @return {Observable<object>} Observable of the component descriptors
             */
            this.getProcessorPropertyDescriptor = function (componentEntity, propertyName) {
                if (descriptors[propertyName]) {
                    return rxjs.Observable.of(descriptors[propertyName]);
                } else {
                    return {
                        "name": propertyName,
                        "displayName": propertyName,
                        "description": 'Dynamic property for ' + propertyName,
                        "required": false,
                        "sensitive": false,
                        "dynamic": true,
                        "expressionLanguageScope": "FLOWFILE_ATTRIBUTES",
                        "expressionLanguageScopeDescription": "Variable Registry and FlowFile Attributes"
                    }
                }
            };

            /**
             * Get a controller service property descriptor.
             *
             * @param componentEntity
             * @param propertyName
             * @return {Observable<object>} Observable of the component descriptors
             */
            this.getControllerServicePropertyDescriptor = function (componentEntity, propertyName) {
                if (descriptors[propertyName]) {
                    return rxjs.Observable.of(descriptors[propertyName]);
                } else {
                    return {
                        "name": propertyName,
                        "displayName": propertyName,
                        "description": 'Dynamic property for ' + propertyName,
                        "required": false,
                        "sensitive": false,
                        "dynamic": true,
                        "expressionLanguageScope": "FLOWFILE_ATTRIBUTES",
                        "expressionLanguageScopeDescription": "Variable Registry and FlowFile Attributes"
                    }
                }
            };

            /**
             * Returns the
             * @param flowId
             */
            this.getELSpecification = function () {
                return rxjs.Observable.of({
                    "isNull": {
                        "name": "isNull",
                        "description": "The isNull function returns true if the subject is null, false otherwise. This is typically used to determine if an attribute exists.",
                        "args": {},
                        "subject": "Any",
                        "returnType": "Boolean"
                    },
                    "notNull": {
                        "name": "notNull",
                        "description": "The notNull function returns the opposite value of the isNull function. That is, it will return true if the subject exists and false otherwise.",
                        "args": {},
                        "subject": "Any",
                        "returnType": "Boolean"
                    },
                    "allDelineatedValues": {
                        "name": "allDelineatedValues",
                        "description": "Splits a String apart according to a delimiter that is provided, and then evaluates each of the values against the rest of the Expression. If the" +
                            " Expression, when evaluated against all of the individual values, returns true in each case, then this function returns true. Otherwise, the function returns false.",
                        "args": {
                            "Delineated Value": "The value that is delineated. This is generally an embedded Expression, though it does not have to be.",
                            "Delimiter": "The value to use to split apart the delineatedValue argument."
                        },
                        "subject": "None",
                        "returnType": "Boolean"
                    }
                });
            };

            // -------
            // funnels
            // -------

            /**
             * Creates a new funnel at the specified point.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} pt                The point that the funnel was dropped.
             * @return {Observable<object>} Observable of the funnel
             */
            this.createFunnel = function (processGroupId, pt) {
                var funnels = getEntry(FUNNELS);

                var funnelId = getComponentId();
                if (funnelId === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Cannot create more than ' + MAX_COMPONENTS + ' components.'));
                }

                var funnelPosition = {
                    'x': pt.x,
                    'y': pt.y
                };

                var funnel = {
                    'revision': {
                        'clientId': 'client-id-1',
                        'version': 1,
                        'lastModifier': 'anonymous'
                    },
                    'id': funnelId,
                    'uri': 'http://localhost:8080/nifi-api/funnels/aebc80c6-0164-1000-5e4b-07fbcc0d96c1',
                    'position': funnelPosition,
                    'permissions': {'canRead': true, 'canWrite': true},
                    'component': {
                        'id': funnelId,
                        'parentGroupId': processGroupId,
                        'position': funnelPosition
                    }
                };

                funnels.push(funnel);
                setEntry(FUNNELS, funnels);

                return rxjs.Observable.of(funnel);
            };

            /**
             * Get a funnel with the specified id.
             *
             * @param funnel
             * @return {Observable<object>} Observable of the funnel
             */
            this.getFunnel = function (funnel) {
                var funnel = findComponent(FUNNELS, funnel.id);
                if (typeof funnel === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                return rxjs.Observable.of(funnel);
            };

            /**
             * Update a funnel.
             *
             * @param {object} Updated funnel
             * @return {Observable<object>}         Observable of the funnel
             */
            this.updateFunnel = function (funnel) {
                var funnelIndex = findComponentIndex(FUNNELS, funnel.id);
                if (funnelIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                var funnels = getEntry(FUNNELS);
                var funnelEntity = funnels[funnelIndex];

                // copy the new config
                funnelEntity.component = $.extend({}, funnelEntity.component, funnel.component);

                // promote necessary fields
                funnelEntity.position = funnelEntity.component.position;

                setEntry(FUNNELS, funnels);

                return rxjs.Observable.of(funnelEntity);
            };

            /**
             * Remove a funnel.
             *
             * @param {object} funnel               Funnel to remove
             * @return {Observable<object>}         Observable of the funnel
             */
            this.removeFunnel = function (funnel) {
                var funnelIndex = findComponentIndex(FUNNELS, funnel.id);
                if (funnelIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                removeConnectionsForComponent(funnel.id);

                var funnels = getEntry(FUNNELS);
                funnels.splice(funnelIndex, 1);
                setEntry(FUNNELS, funnels);

                return rxjs.Observable.of(funnel);
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
                var processors = getEntry(PROCESSORS);

                var processorId = getComponentId();
                if (processorId === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Cannot create more than ' + MAX_COMPONENTS + ' components.'));
                }

                var processorPosition = {
                    'x': pt.x,
                    'y': pt.y
                };

                var processor = {
                    'revision': {
                        'clientId': 'client-id-1',
                        'version': 1,
                        'lastModifier': 'anonymous'
                    },
                    'id': processorId,
                    'uri': 'http://localhost:8080/nifi-api/processors/aebc80c6-0164-1000-5e4b-07fbcc0d96c1',
                    'position': processorPosition,
                    'permissions': {'canRead': true, 'canWrite': true},
                    'component': {
                        'id': processorId,
                        'bundle': {
                            'version': 1
                        },
                        'relationships': [{
                            'name': 'Success',
                            'description': 'The hover text for success description.',
                            'autoTerminate': false
                        }],
                        'parentGroupId': processGroupId,
                        'position': processorPosition,
                        'name': 'My ConsumeKafka Processor',
                        'type': 'org.processor.MyConsumeKafkaProcessor',
                        'config': {
                            'runDurationMillis': 0,
                            'autoTerminatedRelationships': [],
                            'descriptors': descriptors,
                            'properties': {
                                "bootstrap.servers": "localhost:9092",
                                "security.protocol": "PLAINTEXT",
                                "sasl.kerberos.service.name": null,
                                "ssl.context.service": null,
                                "topic": null,
                                "group.id": null,
                                "auto.offset.reset": "latest",
                                "key-attribute-encoding": "utf-8",
                                "message-demarcator": null,
                                "max.poll.records": "10000",
                                "max-uncommit-offset-wait": "1 secs"
                            }
                        }
                    }
                };

                processors.push(processor);
                setEntry(PROCESSORS, processors);

                return rxjs.Observable.of(processor);
            };

            /**
             * Get a processor with the specified id.
             *
             * @param processor
             * @return {Observable<object>} Observable of the processor
             */
            this.getProcessor = function (processor) {
                var processorEntity = findComponent(PROCESSORS, processor.id);
                if (typeof processor === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                return rxjs.Observable.of(processorEntity);
            };

            /**
             * Update a processor.
             *
             * @param {object} Updated processor
             * @return {Observable<object>} Observable of the processor
             */
            this.updateProcessor = function (processor) {
                var processorIndex = findComponentIndex(PROCESSORS, processor.id);
                if (processorIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                var processors = getEntry(PROCESSORS);
                var processorEntity = processors[processorIndex];

                // copy the new config
                if (typeof processor.component.config !== 'undefined') {
                    processorEntity.component.config = $.extend({}, processorEntity.component.config, processor.component.config);
                }
                processorEntity.component = $.extend({}, processorEntity.component, processor.component);

                // promote necessary fields
                processorEntity.position = processorEntity.component.position;
                processorEntity.name = processorEntity.component.name;

                setEntry(PROCESSORS, processors);

                return rxjs.Observable.of(processorEntity);
            };

            /**
             * Remove a processor.
             *
             * @param {object} Processor to remove
             * @return {Observable<object>} Observable of the processor
             */
            this.removeProcessor = function (processor) {
                var processorIndex = findComponentIndex(PROCESSORS, processor.id);
                if (processorIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                removeConnectionsForComponent(processor.id);

                var processors = getEntry(PROCESSORS);
                processors.splice(processorIndex, 1);
                setEntry(PROCESSORS, processors);

                return rxjs.Observable.of(processor);
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
                var processGroupEntity = findComponent(PROCESS_GROUPS, processGroupId);
                if (typeof processGroupEntity === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                return rxjs.Observable.of(processGroupEntity);
            };

            var createRootGroup = function () {
                var processGroups = getEntry(PROCESS_GROUPS);

                // create entry and add root group
                if (!processGroups) {
                    processGroups = [];

                    var processGroup = {
                        'id': 'root',
                        'controllerServices': []
                    };

                    processGroups.push(processGroup);
                    setEntry(PROCESS_GROUPS, processGroups);
                }

                var processGroupEntity = findComponent(PROCESS_GROUPS, 'root');

                // only create root group if it doesn't exist
                if (!processGroupEntity) {
                    processGroups = [];

                    var processGroup = {
                        'id': 'root',
                        'controllerServices': []
                    };

                    processGroups.push(processGroup);
                    setEntry(PROCESS_GROUPS, processGroups);
                }
            };

            createRootGroup();

            /**
             * Get a process group's controller services with the specified id.
             *
             * @param {string} processGroupId           The process group id
             * @return {Observable<object>} Observable of the process group
             */
            this.getProcessGroupControllerServices = function (processGroupId) {
                var processGroupEntity = findComponent(PROCESS_GROUPS, processGroupId);

                if (typeof processGroupEntity === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find service matching specified ID'));
                }

                return rxjs.Observable.of(processGroupEntity.controllerServices);
            };

            /**
             * Create a controller service.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} extensionDescriptor      The descriptor of processor to create
             * @returns {*}
             */
            this.createControllerService = function (processGroupId, extensionDescriptor) {
                var processGroupEntity = findComponent(PROCESS_GROUPS, processGroupId);

                var csId = getComponentId();
                if (csId === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Cannot create more than ' + MAX_COMPONENTS + ' components.'));
                }

                var cs = {
                    'revision': {
                        'clientId': 'client-id-1',
                        'version': 1,
                        'lastModifier': 'anonymous'
                    },
                    'id': csId.toString(),
                    'component': {
                        'name': 'SSLContextService',
                        'type': extensionDescriptor.type,
                        'id': csId.toString(),
                        'parentGroupId': 'root',
                        'validationErrors': [],
                        'bundle': {
                            'group': extensionDescriptor.bundle.group,
                            'artifact': extensionDescriptor.bundle.artifact,
                            'version': extensionDescriptor.bundle.version
                        }
                    }
                };

                var processGroups = getEntry(PROCESS_GROUPS);
                processGroupEntity.controllerServices.push(cs);
                processGroups[0] = processGroupEntity;  // TODO: support multiple process groups
                setEntry(PROCESS_GROUPS, processGroups);

                return rxjs.Observable.of(cs);
            };

            /**
             * Get a controller service with the specified id.
             *
             * @param {object} csId           The controller service id
             * @return {Observable<object>} Observable of the process group
             */
            this.getControllerService = function (csId) {
                var processGroups = getEntry(PROCESS_GROUPS);

                var controllerServiceIndex = processGroups[0].controllerServices.findIndex(function (cs) {
                    return cs.component.id === csId;
                });
                if (controllerServiceIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find service matching specified ID'));
                }

                var controllerServiceEntity = processGroups[0].controllerServices[controllerServiceIndex];

                return rxjs.Observable.of(controllerServiceEntity);
            };

            /**
             * Update a controller service
             *
             * @param {object} cs           The controller service
             * @returns {*}
             */
            this.updateControllerService = function (cs) {
                var processGroups = getEntry(PROCESS_GROUPS);

                var controllerServiceIndex = processGroups[0].controllerServices.findIndex(function (controllerService) {
                    return controllerService['id'] === cs.id;
                });
                if (controllerServiceIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find service matching specified ID'));
                }

                var controllerServiceEntity = processGroups[0].controllerServices[controllerServiceIndex];
                controllerServiceEntity.component = $.extend({}, controllerServiceEntity.component, cs.component);

                // promote necessary fields
                controllerServiceEntity.name = controllerServiceEntity.component.name;

                processGroups[0].controllerServices[controllerServiceIndex] = controllerServiceEntity;

                setEntry(PROCESS_GROUPS, processGroups);

                return rxjs.Observable.of(controllerServiceEntity);
            };

            /**
             * Delete a controller service.
             *
             * @param controllerService
             * @returns {*}
             */
            this.deleteControllerService = function (controllerService) {
                var processGroups = getEntry(PROCESS_GROUPS);

                var controllerServiceIndex = processGroups[0].controllerServices.findIndex(function (cs) {
                    return cs['id'] === controllerService.id;
                });

                if (controllerServiceIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find service matching specified ID'));
                }

                processGroups[0].controllerServices.splice(controllerServiceIndex, 1);
                setEntry(PROCESS_GROUPS, processGroups);

                return rxjs.Observable.of(controllerService);
            };

            /**
             * Update a process group.
             *
             * @param {object} Updated process group
             * @return {Observable<object>} Observable of the process group
             */
            this.updateProcessGroup = function (processGroup) {
                var processGroupIndex = findComponentIndex(PROCESS_GROUPS, processGroup.id);
                if (processGroupIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find process group matching specified ID'));
                }

                var processGroups = getEntry(PROCESS_GROUPS);
                var processGroupEntity = processGroups[processGroupIndex];

                // copy the new config
                if (typeof cs.component.config !== 'undefined') {
                    processGroupEntity.component.config = $.extend({}, processGroupEntity.component.config, processGroup.component.config);
                }
                processGroupEntity.component = $.extend({}, processGroupEntity.component, processGroup.component);
                processGroups[processGroup.id] = processGroupEntity;
                setEntry(PROCESS_GROUPS, processGroups);

                return rxjs.Observable.of(processGroupEntity);
            };

            // -----------
            // connections
            // -----------

            /**
             * Creates a new connection at the specified point.
             *
             * @param {string} processGroupId           The process group id
             * @param {object} connection               The connection
             * @return {Observable<object>} Observable of the connection
             */
            this.createConnection = function (processGroupId, connection) {
                var connections = getEntry(CONNECTIONS);

                var connectionId = getComponentId();
                if (connectionId === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Cannot create more than ' + MAX_COMPONENTS + ' components.'));
                }

                var connectionEntity = {
                    'revision': {
                        'clientId': '2543b847-0165-1000-c3ff-61d6959a4366',
                        'version': 1,
                        'lastModifier': 'anonymous'
                    },
                    'id': connectionId,
                    'uri': 'http://localhost:8080/nifi-api/connections/2544156a-0165-1000-ece9-5b175677ced9',
                    'permissions': {
                        'canRead': true,
                        'canWrite': true
                    },
                    'bends': connection.component.bends,
                    'labelIndex': 0,
                    'zIndex': 0,
                    'sourceId': connection.component.source.id,
                    'sourceGroupId': connection.component.source.groupId,
                    'sourceType': connection.component.source.type,
                    'destinationId': connection.component.destination.id,
                    'destinationGroupId': connection.component.destination.groupId,
                    'destinationType': connection.component.destination.type,
                    'component': $.extend({
                        'id': connectionId,
                        'parentGroupId': processGroupId
                    }, connection.component)
                };

                connections.push(connectionEntity);
                setEntry(CONNECTIONS, connections);

                return rxjs.Observable.of(connectionEntity);
            };

            /**
             * Get a connection with the specified id.
             *
             * @param connection
             * @return {Observable<object>} Observable of the connection
             */
            this.getConnection = function (connection) {
                var connectionEntity = findComponent(CONNECTIONS, connection.id);
                if (typeof connectionEntity === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                return rxjs.Observable.of(connectionEntity);
            };

            /**
             * Update a connection
             *
             * @param {object} connection           The connection
             * @returns {*}
             */
            this.updateConnection = function (connection) {
                var connectionIndex = findComponentIndex(CONNECTIONS, connection.id);
                if (connectionIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                var connections = getEntry(CONNECTIONS);
                var connectionEntity = connections[connectionIndex];

                // copy the new config
                if (typeof connection.component.source !== 'undefined') {
                    connectionEntity.component.source = connection.component.source;
                }
                if (typeof connection.component.destination !== 'undefined') {
                    connectionEntity.component.destination = connection.component.destination;
                }
                connectionEntity.component = $.extend({}, connectionEntity.component, connection.component);

                // promote necessary fields
                connectionEntity.bends = connectionEntity.component.bends;
                connectionEntity.labelIndex = connectionEntity.component.labelIndex;
                connectionEntity.zIndex = connectionEntity.component.zIndex;
                connectionEntity.sourceId = connectionEntity.component.source.id;
                connectionEntity.sourceGroupId = connectionEntity.component.source.groupId;
                connectionEntity.sourceType = connectionEntity.component.source.type;
                connectionEntity.destinationId = connectionEntity.component.destination.id;
                connectionEntity.destinationGroupId = connectionEntity.component.destination.groupId;
                connectionEntity.destinationType = connectionEntity.component.destination.type;

                setEntry(CONNECTIONS, connections);

                return rxjs.Observable.of(connectionEntity);
            };

            /**
             * Remove a connection.
             *
             * @param {object} Connection to remove
             * @return {Observable<object>} Observable of the connection
             */
            this.removeConnection = function (connection) {
                var connectionIndex = findComponentIndex(CONNECTIONS, connection.id);
                if (connectionIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                var connections = getEntry(CONNECTIONS);
                connections.splice(connectionIndex, 1);
                setEntry(CONNECTIONS, connections);

                return rxjs.Observable.of(connection);
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
                var remoteProcessGroups = getEntry(REMOTE_PROCESS_GROUP);

                var remoteProcessGroupId = getComponentId();
                if (remoteProcessGroupId === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Cannot create more than ' + MAX_COMPONENTS + ' components.'));
                }

                var remoteProcessGroupPosition = {
                    'x': pt.x,
                    'y': pt.y
                };

                var remoteProcessGroup = {
                    'revision': {
                        'clientId': 'client-id-1',
                        'version': 1,
                        'lastModifier': 'anonymous'
                    },
                    'id': remoteProcessGroupId,
                    'uri': 'http://localhost:8080/nifi-api/remote-process-groups/aebc80c6-0164-1000-5e4b-07fbcc0d96c1',
                    'position': remoteProcessGroupPosition,
                    'permissions': {'canRead': true, 'canWrite': true},
                    'component': {
                        'id': remoteProcessGroupId,
                        'parentGroupId': processGroupId,
                        'position': remoteProcessGroupPosition,
                        'name': 'NiFi Flow',
                        'targetUris': targetUris,
                        'communicationsTimeout': '30 sec',
                        'yieldDuration': '10 sec',
                        'transportProtocol': 'RAW',
                        'localNetworkInterface': '',
                        'proxyHost': '',
                        'proxyPort': ''
                    }
                };

                remoteProcessGroups.push(remoteProcessGroup);
                setEntry(REMOTE_PROCESS_GROUP, remoteProcessGroups);

                return rxjs.Observable.of(remoteProcessGroup);
            };

            /**
             * Get a remote process group with the specified id.
             *
             * @param remoteProcessGroup
             * @return {Observable<object>}         Observable of the remote process group
             */
            this.getRemoteProcessGroup = function (remoteProcessGroup) {
                var remoteProcessGroupEntity = findComponent(REMOTE_PROCESS_GROUP, remoteProcessGroup.id);
                if (typeof remoteProcessGroupEntity === 'undefined') {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                return rxjs.Observable.of(remoteProcessGroupEntity);
            };

            /**
             * Update a remote process group.
             *
             * @param {object}                      Updated remote process group
             * @return {Observable<object>}         Observable of the remote process group
             */
            this.updateRemoteProcessGroup = function (remoteProcessGroup) {
                var remoteProcessGroupIndex = findComponentIndex(REMOTE_PROCESS_GROUP, remoteProcessGroup.id);
                if (remoteProcessGroupIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                var remoteProcessGroups = getEntry(REMOTE_PROCESS_GROUP);
                var remoteProcessGroupEntity = remoteProcessGroups[remoteProcessGroupIndex];

                // copy the new config
                remoteProcessGroupEntity.component = $.extend({}, remoteProcessGroupEntity.component, remoteProcessGroup.component);

                // promote necessary fields
                remoteProcessGroupEntity.position = remoteProcessGroupEntity.component.position;

                setEntry(REMOTE_PROCESS_GROUP, remoteProcessGroups);

                return rxjs.Observable.of(remoteProcessGroupEntity);
            };

            /**
             * Remove a remote process group.
             *
             * @param {object} remoteProcessGroup               Remote process group to remove
             * @return {Observable<object>}                     Observable of the remote process group
             */
            this.removeRemoteProcessGroup = function (remoteProcessGroup) {
                var remoteProcessGroupIndex = findComponentIndex(REMOTE_PROCESS_GROUP, remoteProcessGroup.id);
                if (remoteProcessGroupIndex === -1) {
                    return rxjs.Observable.throw(new ErrorResponse(false, false, 'Unable to find component matching specified ID'));
                }

                removeConnectionsForComponent(remoteProcessGroup.id);

                var remoteProcessGroups = getEntry(REMOTE_PROCESS_GROUP);
                remoteProcessGroups.splice(remoteProcessGroupIndex, 1);
                setEntry(REMOTE_PROCESS_GROUP, remoteProcessGroups);

                return rxjs.Observable.of(remoteProcessGroup);
            };
        };

        FlowDesignerApi.prototype = {
            constructor: FlowDesignerApi
        };

        return FlowDesignerApi;
    }());
};

FlowDesignerApiFactory.prototype = {
    constructor: FlowDesignerApiFactory
};

module.exports = {
    build: FlowDesignerApiFactory
};
