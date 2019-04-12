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

var ngCore = require('@angular/core');
var ngForms = require('@angular/forms');
var CommonService = require('@flow-designer/services/CommonService');
var UniversalCaptureService = require('@flow-designer/services/UniversalCaptureService');
var ErrorResponseFactory = require('@flow-designer/services/ErrorResponseFactory');
var CanvasUtilsService = require('@flow-designer/services/CanvasUtilsService');
var CanvasComponent = require('@flow-designer/components/flow-designer-canvas');
var DraggableComponent = require('@flow-designer/components/flow-designer-draggable');
var ExtensionCreationComponent = require('@flow-designer/components/flow-designer-extension-creation');
var ConnectionCreationComponent = require('@flow-designer/components/flow-designer-connection-creation');
var RpgCreationComponent = require('@flow-designer/components/flow-designer-rpg-creation');
var ConnectionDefinitionComponent = require('@flow-designer/components/flow-designer-connection-definition');
var ConnectionConfigurationComponent = require('@flow-designer/components/flow-designer-connection-configuration');
var MoveConnectionComponent = require('@flow-designer/components/flow-designer-move-connection');
var ProcessorConfigurationComponent = require('@flow-designer/components/flow-designer-processor-configuration');
var ComponentConfigurationComponent = require('@flow-designer/components/flow-designer-controller-service-configuration');
var ComponentListingComponent = require('@flow-designer/components/flow-designer-component-listing');
var RpgConfigurationComponent = require('@flow-designer/components/flow-designer-rpg-configuration');
var PropertyTableComponent = require('@flow-designer/components/flow-designer-property-table');
var AddPropertyComponent = require('@flow-designer/components/flow-designer-add-property');
var ConfigNotAppliedComponent = require('@flow-designer/components/flow-designer-configuration-not-applied');

var FdsCoreModule = require('@flow-design-system/core');
var FdsStorageService = require('@flow-design-system/common/storage-service');

/**
 * FlowDesignerModule constructor.
 *
 * @constructor
 */
function FlowDesignerModule() {
}

FlowDesignerModule.prototype = {
    constructor: FlowDesignerModule
};

FlowDesignerModule.annotations = [
    new ngCore.NgModule({
        imports: [
            FdsCoreModule,
            ngForms.FormsModule
        ],
        declarations: [
            [
                CanvasComponent,
                DraggableComponent,
                ConfigNotAppliedComponent,
                ProcessorConfigurationComponent,
                ComponentListingComponent,
                ComponentConfigurationComponent,
                PropertyTableComponent,
                AddPropertyComponent,
                ExtensionCreationComponent,
                ConnectionCreationComponent,
                ConnectionDefinitionComponent,
                ConnectionConfigurationComponent,
                MoveConnectionComponent,
                RpgCreationComponent,
                RpgConfigurationComponent
            ]
        ],
        exports: [
            [
                CanvasComponent,
                DraggableComponent,
                ConfigNotAppliedComponent,
                ProcessorConfigurationComponent,
                ComponentListingComponent,
                ComponentConfigurationComponent,
                PropertyTableComponent,
                AddPropertyComponent,
                ExtensionCreationComponent,
                ConnectionCreationComponent,
                ConnectionDefinitionComponent,
                ConnectionConfigurationComponent,
                MoveConnectionComponent,
                RpgCreationComponent,
                RpgConfigurationComponent
            ]
        ],
        providers: [
            CommonService,
            CanvasUtilsService,
            UniversalCaptureService,
            FdsStorageService,
            {
                provide: 'ErrorResponse',
                useFactory: ErrorResponseFactory.build,
                deps: []
            }
        ],
        entryComponents: [
            [
                CanvasComponent,
                DraggableComponent,
                ConfigNotAppliedComponent,
                ProcessorConfigurationComponent,
                ComponentListingComponent,
                ComponentConfigurationComponent,
                PropertyTableComponent,
                AddPropertyComponent,
                ExtensionCreationComponent,
                ConnectionCreationComponent,
                ConnectionDefinitionComponent,
                ConnectionConfigurationComponent,
                MoveConnectionComponent,
                RpgCreationComponent,
                RpgConfigurationComponent
            ]
        ]
    })
];

module.exports = FlowDesignerModule;
