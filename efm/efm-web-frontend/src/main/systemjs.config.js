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

(function () {
    System.config({
        paths: {
            // paths serve as alias
            'npm:': 'node_modules/',
            'platform:': 'platform/',
            'webapp:': 'webapp/'
        },
        // map tells the System loader where to look for things
        map: {
            'text': 'npm:systemjs-plugin-text/text.js',
            'app': './webapp',

            // jquery
            'jquery': 'npm:jquery/dist/jquery.min.js',
            'jquery-ui-dist': 'npm:jquery-ui-dist/jquery-ui.js',

            // d3
            'd3': 'npm:d3/build/d3.node.js',
            'd3-array': 'npm:d3-array/build/d3-array.min.js',
            'd3-axis': 'npm:d3-axis/build/d3-axis.min.js',
            'd3-brush': 'npm:d3-brush/build/d3-brush.min.js',
            'd3-chord': 'npm:d3-chord/build/d3-chord.min.js',
            'd3-collection': 'npm:d3-collection/build/d3-collection.min.js',
            'd3-color': 'npm:d3-color/build/d3-color.min.js',
            'd3-dispatch': 'npm:d3-dispatch/build/d3-dispatch.min.js',
            'd3-drag': 'npm:d3-drag/build/d3-drag.min.js',
            'd3-dsv': 'npm:d3-dsv/build/d3-dsv.min.js',
            'd3-ease': 'npm:d3-ease/build/d3-ease.min.js',
            'd3-force': 'npm:d3-force/build/d3-force.min.js',
            'd3-format': 'npm:d3-format/build/d3-format.min.js',
            'd3-geo': 'npm:d3-geo/build/d3-geo.min.js',
            'd3-hierarchy': 'npm:d3-hierarchy/build/d3-hierarchy.min.js',
            'd3-interpolate': 'npm:d3-interpolate/build/d3-interpolate.min.js',
            'd3-path': 'npm:d3-path/build/d3-path.min.js',
            'd3-polygon': 'npm:d3-polygon/build/d3-polygon.min.js',
            'd3-quadtree': 'npm:d3-quadtree/build/d3-quadtree.min.js',
            'd3-queue': 'npm:d3-queue/build/d3-queue.min.js',
            'd3-random': 'npm:d3-random/build/d3-random.min.js',
            'd3-request': 'npm:d3-request/build/d3-request.min.js',
            'd3-scale': 'npm:d3-scale/build/d3-scale.min.js',
            'd3-selection': 'npm:d3-selection/dist/d3-selection.min.js',
            'd3-selection-multi': 'npm:d3-selection-multi/build/d3-selection-multi.min.js',
            'd3-shape': 'npm:d3-shape/build/d3-shape.min.js',
            'd3-time': 'npm:d3-time/build/d3-time.min.js',
            'd3-time-format': 'npm:d3-time-format/build/d3-time-format.min.js',
            'd3-timer': 'npm:d3-timer/build/d3-timer.min.js',
            'd3-transition': 'npm:d3-transition/build/d3-transition.min.js',
            'd3-voronoi': 'npm:d3-voronoi/build/d3-voronoi.min.js',
            'd3-zoom': 'npm:d3-zoom/build/d3-zoom.min.js',

            // Angular
            '@angular/core': 'npm:@angular/core/bundles/core.umd.js',
            '@angular/common': 'npm:@angular/common/bundles/common.umd.js',
            '@angular/common/http': 'npm:@angular/common/bundles/common-http.umd.js',
            '@angular/common/http/testing': 'npm:@angular/common/bundles/common-http-testing.umd.js',
            '@angular/platform-browser': 'npm:@angular/platform-browser/bundles/platform-browser.umd.js',
            '@angular/platform-browser-dynamic': 'npm:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.js',
            '@angular/http': 'npm:@angular/http/bundles/http.umd.js',
            '@angular/router': 'npm:@angular/router/bundles/router.umd.js',
            '@angular/forms': 'npm:@angular/forms/bundles/forms.umd.js',
            '@angular/flex-layout': 'npm:@angular/flex-layout/bundles/flex-layout.umd.js',
            '@angular/flex-layout/core': 'npm:@angular/flex-layout/bundles/flex-layout-core.umd.js',
            '@angular/flex-layout/extended': 'npm:@angular/flex-layout/bundles/flex-layout-extended.umd.js',
            '@angular/flex-layout/flex': 'npm:@angular/flex-layout/bundles/flex-layout-flex.umd.js',
            '@angular/material': 'npm:@angular/material/bundles/material.umd.js',
            '@angular/material/core': 'npm:@angular/material/bundles/material-core.umd.js',
            '@angular/material/card': 'npm:@angular/material/bundles/material-card.umd.js',
            '@angular/material/divider': 'npm:@angular/material/bundles/material-divider.umd.js',
            '@angular/material/progress-bar': 'npm:@angular/material/bundles/material-progress-bar.umd.js',
            '@angular/material/progress-spinner': 'npm:@angular/material/bundles/material-progress-spinner.umd.js',
            '@angular/material/chips': 'npm:@angular/material/bundles/material-chips.umd.js',
            '@angular/material/input': 'npm:@angular/material/bundles/material-input.umd.js',
            '@angular/material/icon': 'npm:@angular/material/bundles/material-icon.umd.js',
            '@angular/material/button': 'npm:@angular/material/bundles/material-button.umd.js',
            '@angular/material/checkbox': 'npm:@angular/material/bundles/material-checkbox.umd.js',
            '@angular/material/tooltip': 'npm:@angular/material/bundles/material-tooltip.umd.js',
            '@angular/material/dialog': 'npm:@angular/material/bundles/material-dialog.umd.js',
            '@angular/material/sidenav': 'npm:@angular/material/bundles/material-sidenav.umd.js',
            '@angular/material/menu': 'npm:@angular/material/bundles/material-menu.umd.js',
            '@angular/material/form-field': 'npm:@angular/material/bundles/material-form-field.umd.js',
            '@angular/material/toolbar': 'npm:@angular/material/bundles/material-toolbar.umd.js',
            '@angular/material/autocomplete': 'npm:@angular/material/bundles/material-autocomplete.umd.js',
            '@angular/platform-browser/animations': 'npm:@angular/platform-browser/bundles/platform-browser-animations.umd.js',
            '@angular/cdk': 'npm:@angular/cdk/bundles/cdk.umd.js',
            '@angular/cdk/a11y': 'npm:@angular/cdk/bundles/cdk-a11y.umd.js',
            '@angular/cdk/accordion': 'npm:@angular/cdk/bundles/cdk-accordion.umd.js',
            '@angular/cdk/layout': 'npm:@angular/cdk/bundles/cdk-layout.umd.js',
            '@angular/cdk/collections': 'npm:@angular/cdk/bundles/cdk-collections.umd.js',
            '@angular/cdk/observers': 'npm:@angular/cdk/bundles/cdk-observers.umd.js',
            '@angular/cdk/overlay': 'npm:@angular/cdk/bundles/cdk-overlay.umd.js',
            '@angular/cdk/platform': 'npm:@angular/cdk/bundles/cdk-platform.umd.js',
            '@angular/cdk/portal': 'npm:@angular/cdk/bundles/cdk-portal.umd.js',
            '@angular/cdk/keycodes': 'npm:@angular/cdk/bundles/cdk-keycodes.umd.js',
            '@angular/cdk/bidi': 'npm:@angular/cdk/bundles/cdk-bidi.umd.js',
            '@angular/cdk/coercion': 'npm:@angular/cdk/bundles/cdk-coercion.umd.js',
            '@angular/cdk/table': 'npm:@angular/cdk/bundles/cdk-table.umd.js',
            '@angular/cdk/rxjs': 'npm:@angular/cdk/bundles/cdk-rxjs.umd.js',
            '@angular/cdk/scrolling': 'npm:@angular/cdk/bundles/cdk-scrolling.umd.js',
            '@angular/cdk/stepper': 'npm:@angular/cdk/bundles/cdk-stepper.umd.js',
            '@angular/animations': 'npm:@angular/animations/bundles/animations.umd.js',
            '@angular/animations/browser': 'npm:@angular/animations/bundles/animations-browser.umd.js',
            '@angular/compiler': 'npm:@angular/compiler/bundles/compiler.umd.js',

            // needed to support gestures for angular material
            'hammerjs': 'npm:hammerjs/hammer.min.js',

            // Covalent
            '@covalent/core': 'npm:@covalent/core/bundles/covalent-core.umd.min.js',
            '@covalent/core/common': 'npm:@covalent/core/bundles/covalent-core-common.umd.min.js',

            // other libraries
            'rxjs': 'npm:rxjs',
            'slickgrid': 'npm:slickgrid',
            'zone.js': 'npm:zone.js/dist/zone.js',
            'core-js': 'npm:core-js/client/shim.min.js',
            'superagent': 'npm:superagent/superagent.js',
            'querystring': 'npm:querystring',
            'tslib': 'npm:tslib/tslib.js',
            'qtip2': 'npm:qtip2/dist/jquery.qtip.min.js',
            'codemirror': 'platform:flow-designer/codemirror',

            // Flow Design System
            '@flow-design-system/core': 'npm:@nifi-fds/core/flow-design-system.module.js',
            '@flow-design-system/dialogs': 'npm:@nifi-fds/core/dialogs/fds-dialogs.module.js',
            '@flow-design-system/dialog-component': 'npm:@nifi-fds/core/dialogs/fds-dialog.component.js',
            '@flow-design-system/dialog-service': 'npm:@nifi-fds/core/dialogs/services/dialog.service.js',
            '@flow-design-system/confirm-dialog-component': 'npm:@nifi-fds/core/dialogs/confirm-dialog/confirm-dialog.component.js',
            '@flow-design-system/snackbars': 'npm:@nifi-fds/core/snackbars/fds-snackbars.module.js',
            '@flow-design-system/snackbar-component': 'npm:@nifi-fds/core/snackbars/fds-snackbar.component.js',
            '@flow-design-system/snackbar-service': 'npm:@nifi-fds/core/snackbars/services/snackbar.service.js',
            '@flow-design-system/coaster-component': 'npm:@nifi-fds/core/snackbars/coaster/coaster.component.js',
            '@flow-design-system/common/storage-service': 'npm:@nifi-fds/core/common/services/fds-storage.service.js',
            '@flow-design-system/common/animations': 'npm:@nifi-fds/core/common/fds.animations.js',

            // Flow Designer
            '@flow-designer/jquery/nfeditor': 'platform:flow-designer/jquery/nfeditor/jquery.nfeditor.js',
            '@flow-designer/jquery/nfel': 'platform:flow-designer/jquery/nfeditor/languages/nfel.js',
            '@flow-designer/jquery/combo': 'platform:flow-designer/jquery/combo/jquery.combo.js',
            '@flow-designer/jquery/tab': 'platform:flow-designer/jquery/jquery.tab.js',
            '@flow-designer/modules/core': 'platform:flow-designer/flow-designer.module.js',
            '@flow-designer/services/Client': 'platform:flow-designer/components/canvas/services/fd.client.service.js',
            '@flow-designer/services/CanvasFactory': 'platform:flow-designer/components/canvas/services/fd.canvas.factory.js',
            '@flow-designer/services/ConnectionManager': 'platform:flow-designer/components/canvas/services/fd.connection.manager.service.js',
            '@flow-designer/services/ProcessorManager': 'platform:flow-designer/components/canvas/services/fd.processor.manager.service.js',
            '@flow-designer/services/RemoteProcessGroupManager': 'platform:flow-designer/components/canvas/services/fd.remote-process-group.manager.service.js',
            '@flow-designer/services/FunnelManager': 'platform:flow-designer/components/canvas/services/fd.funnel.manager.service.js',
            '@flow-designer/services/ContextMenu': 'platform:flow-designer/components/canvas/services/fd.context-menu.behavior.service.js',
            '@flow-designer/services/QuickSelectBehavior': 'platform:flow-designer/components/canvas/services/fd.quick-select.behavior.service.js',
            '@flow-designer/services/SelectableBehavior': 'platform:flow-designer/components/canvas/services/fd.selectable.behavior.service.js',
            '@flow-designer/services/DeselectableBehavior': 'platform:flow-designer/components/canvas/services/fd.deselectable.behavior.service.js',
            '@flow-designer/services/DraggableBehavior': 'platform:flow-designer/components/canvas/services/fd.draggable.behavior.service.js',
            '@flow-designer/services/ConnectableBehavior': 'platform:flow-designer/components/canvas/services/fd.connectable.behavior.service.js',
            '@flow-designer/services/EditableBehavior': 'platform:flow-designer/components/canvas/services/fd.editable.behavior.service.js',
            '@flow-designer/services/CanvasUtilsService': 'platform:flow-designer/services/fd.canvas-utils.service.js',
            '@flow-designer/services/ErrorResponseFactory': 'platform:flow-designer/services/fd.error-response.factory.js',
            '@flow-designer/services/CommonService': 'platform:flow-designer/services/fd.common.service.js',
            '@flow-designer/services/UniversalCaptureService': 'platform:flow-designer/services/fd.universal-capture.service.js',
            '@flow-designer/services/FlowDesignerApiFactory': 'platform:flow-designer/components/canvas/services/fd.api.factory.js',
            '@flow-designer/services/PropertyTableFactory': 'platform:flow-designer/components/property-table/services/fd.property-table.factory.js',
            '@flow-designer/components/flow-designer-canvas': 'platform:flow-designer/components/canvas/fd.canvas.component.js',
            '@flow-designer/components/flow-designer-draggable': 'platform:flow-designer/components/draggable/fd.draggable.component.js',
            '@flow-designer/components/flow-designer-property-table': 'platform:flow-designer/components/property-table/fd.property-table.control-value-accessor.component.js',
            '@flow-designer/components/flow-designer-add-property': 'platform:flow-designer/components/property-table/fd.add-property.component.js',
            '@flow-designer/components/flow-designer-processor-configuration': 'platform:flow-designer/components/configuration/processor/fd.processor-configuration.component.js',
            '@flow-designer/components/flow-designer-component-listing': 'platform:flow-designer/components/component-listing/fd.component-listing.component.js',
            '@flow-designer/components/flow-designer-controller-service-configuration': 'platform:flow-designer/components/configuration/controller-service/fd.controller-service-configuration.component.js',
            '@flow-designer/components/flow-designer-extension-creation': 'platform:flow-designer/components/creation/extension/fd.extension-creation.component.js',
            '@flow-designer/components/flow-designer-connection-creation': 'platform:flow-designer/components/creation/connection/fd.connection-creation.component.js',
            '@flow-designer/components/flow-designer-connection-definition': 'platform:flow-designer/components/connection/fd.connection-definition.component.js',
            '@flow-designer/components/flow-designer-connection-configuration': 'platform:flow-designer/components/configuration/connection/fd.connection-configuration.component.js',
            '@flow-designer/components/flow-designer-move-connection': 'platform:flow-designer/components/configuration/connection/fd.move-connection.component.js',
            '@flow-designer/components/flow-designer-rpg-creation': 'platform:flow-designer/components/creation/rpg/fd.rpg-creation.component.js',
            '@flow-designer/components/flow-designer-rpg-configuration': 'platform:flow-designer/components/configuration/rpg/fd.rpg-configuration.component.js',
            '@flow-designer/components/flow-designer-configuration-not-applied': 'platform:flow-designer/components/configuration/fd.configuration-not-applied.component.js',
            '@flow-designer/common/animations': 'platform:flow-designer/common/fd.animations.js',

            // Application
            'efm.module.js': 'webapp:efm.module.js',
            'efm.routes.js': 'webapp:efm.routes.js',
            'efm.component.js': 'webapp:efm.component.js',
            'efm.host.component.js': 'webapp:efm.host.component.js',

            'services/efm.fd-api.factory.js': 'webapp:services/efm.fd-api.factory.js',
            'services/efm.service.js': 'webapp:services/efm.service.js',
            'services/efm.api.service.js': 'webapp:services/efm.api.service.js',
            'services/efm.http-error-response.factory.js': 'webapp:services/efm.http-error-response.factory.js',
            'services/efm.flow-provider.service.js': 'webapp:services/efm.flow-provider.service.js',
            'services/efm.flow.resolve.service.js': 'webapp:services/efm.flow.resolve.service.js',
            'services/efm.dialog.service.js': 'webapp:services/efm.dialog.service.js',
            'services/efm.http-interceptor.service.js': 'webapp:services/efm.http-interceptor.service.js',
            'services/efm.error.service.js': 'webapp:services/efm.error.service.js',
            'services/efm.fd-configuration-can-deactivate-guard.service.js': 'webapp:services/efm.fd-configuration-can-deactivate-guard.service.js',

            'components/efm-flow-designer/efm.flow-designer.component.js': 'webapp:components/efm-flow-designer/efm.flow-designer.component.js',
            'components/efm-flow-designer/open/efm.open-flow.component.js': 'webapp:components/efm-flow-designer/open/efm.open-flow.component.js',
            'components/efm-flow-designer/open/efm.flow-listing.component.js': 'webapp:components/efm-flow-designer/open/efm.flow-listing.component.js',
            'components/efm-flow-designer/configuration/efm.fd-configuration.component.js': 'webapp:components/efm-flow-designer/configuration/efm.fd-configuration.component.js',
            'components/efm-flow-designer/selection/efm.fd-selection.component.js': 'webapp:components/efm-flow-designer/selection/efm.fd-selection.component.js',
            'components/efm-flow-designer/publish/efm.publish-flow.component.js': 'webapp:components/efm-flow-designer/publish/efm.publish-flow.component.js',
            'components/monitor/efm.monitor.component.js': 'webapp:components/monitor/efm.monitor.component.js',
            'components/monitor/events/efm.monitor-events.component.js': 'webapp:components/monitor/events/efm.monitor-events.component.js',
            'components/monitor/events/efm.monitor-events.constants.js': 'webapp:components/monitor/events/efm.monitor-events.constants.js',
            'components/monitor/events/efm.monitor-events.utils.js': 'webapp:components/monitor/events/efm.monitor-events.utils.js',
            'components/404/efm.404.component.js': 'webapp:components/404/efm.404.component.js',
            'components/error/efm.error.component.js': 'webapp:components/error/efm.error.component.js',
            'components/auth-error/efm.auth-error.component.js': 'webapp:components/auth-error/efm.auth-error.component.js',
            'components/efm-flow-designer/component-listing/efm.fd-component-listing.component.js': 'webapp:components/efm-flow-designer/component-listing/efm.fd-component-listing.component.js',
            'components/efm-flow-designer/controller-service-configuration/efm.fd-controller-service-configuration.component.js': 'webapp:components/efm-flow-designer/controller-service-configuration/efm.fd-controller-service-configuration.component.js',
            'components/monitor/events/details/efm.monitor-event-details.component.js': 'webapp:components/monitor/events/details/efm.monitor-event-details.component.js',

            'mock/class-detail.js': 'webapp:mock/class-detail.js',
            'mock/agents.js': 'webapp:mock/agents.js',
            'mock/server.js': 'webapp:mock/server.js',
            'mock/overview.js': 'webapp:mock/overview.js',

            'config/urls.js': 'webapp:config/urls.js'
        },
        // packages tells the System loader how to load when no filename and/or no extension
        packages: {
            '.': {
                defaultExtension: 'js'
            },
            'rxjs': {
                defaultExtension: 'js'
            },
            'slickgrid': {
                defaultExtension: 'js'
            },
            'querystring': {
                main: './index.js',
                defaultExtension: 'js'
            },
            'moment': {
                main: './moment.js',
                defaultExtension: 'js'
            },
            'angular2-moment': {
                main: './index.js',
                defaultExtension: 'js'
            },
            'codemirror': {
                main: './lib/codemirror-compressed.js',
                map: {'../../lib/codemirror': './lib/codemirror-compressed.js'},
                meta: {
                    './*.js': {
                        format: 'global'
                    }
                }
            }
        }
    });
})(this);
