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

/**
 * FlowDesignerDraggableComponent.
 *
 * @param elementRef                    The draggable element.
 * @constructor
 */
function FlowDesignerDraggableComponent(elementRef) {
    var self = this;

    this.el = elementRef;

    var slugify = function(text) {
        return text.toString().toLowerCase()
            .replace(/\s+/g, '-')           // Replace spaces with -
            .replace(/[^\w\-]+/g, '')       // Remove all non-word chars
            .replace(/\-\-+/g, '-')         // Replace multiple - with single -
            .replace(/^-+/, '')             // Trim - from start of text
            .replace(/-+$/, '');            // Trim - from end of text
    };

    var initDraggableElement = function () {
        var draggable = self.el.nativeElement.getElementsByClassName('js-draggable');
        if (!draggable || draggable.length === 0) {
            return;
        }

        var draggableElement = draggable[0];
        draggableElement.setAttribute('draggable', true);

        var body = document.body;
        var draggingClass = 'js-dragging';
        draggableElement.addEventListener('dragstart', function(event) {
            body.classList.add(draggingClass);
            event.dataTransfer.setData('component-type', slugify(self.type));
        }, false);

        draggableElement.addEventListener('dragend', function(event) {
            body.classList.remove(draggingClass);
        });
    };

    this.init = function () {
        initDraggableElement();
    };
};

FlowDesignerDraggableComponent.prototype = {
    constructor: FlowDesignerDraggableComponent,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.init();
    }
};

FlowDesignerDraggableComponent.annotations = [
    new ngCore.Component({
        selector: 'flow-designer-draggable',
        template: require('./fd.draggable.component.html!text'),
        inputs: [
            'icon',
            'type'
        ]
    })
];

FlowDesignerDraggableComponent.parameters = [ngCore.ElementRef];

module.exports = FlowDesignerDraggableComponent;
