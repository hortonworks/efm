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
require('slickgrid/lib/jquery.event.drag-2.3.0');
var Slick = $.extend({},
    require('slickgrid/slick.dataview'),
    require('slickgrid/slick.grid'),
    require('slickgrid/plugins/slick.rowselectionmodel'),
    require('slickgrid/plugins/slick.cellrangeselector'),
    require('slickgrid/plugins/slick.cellselectionmodel'),
    require('slickgrid/plugins/slick.autotooltips'),
    require('slickgrid/slick.formatters'),
    require('slickgrid/slick.editors'),
    require('slickgrid/slick.core')
);

/**
 * PropertyTableFactory
 *
 * @returns {Function}
 * @constructor
 */
function PropertyTableFactory() {

    /**
     * PropertyTable constructor.
     *
     * @param canvasUtilsService                    The flow designer canvas utils
     * @param commonService                         The flow designer common service.
     * @param fdsDialogService                      The FDS dialog service module.
     * @param universalCaptureService               The flow designer universal capture service.
     * @constructor
     */
    return function (canvasUtilsService, commonService, fdsDialogService, universalCaptureService) {

        /**
         * PropertyTable constructor.
         *
         * @param client                    The flow designer canvas utils
         * @constructor
         */
        function PropertyTable() {
            var self = this;
            var dialogRef = null;
            var propertyTableContainerElement = null;
            var languageId = 'nfel';
            var editorClass = languageId + '-editor';
            var groupId = null;
            var flowDesignerCanvasComponent = null;
            var flowDesignerApi = null;
            var ngZone = null;
            var flowDesignerPropertyTableComponent = null;

            // text editor
            var textEditor = function (args) {
                var scope = this;
                var initialValue = '';
                var previousValue;
                var propertyDescriptor;
                var wrapper;
                var isEmpty;
                var input;

                this.init = function () {
                    ngZone.runOutsideAngular(function () {
                        var container = $('body');

                        // get the property descriptor
                        var gridContainer = $(args.grid.getContainerNode());
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        propertyDescriptor = descriptors[args.item.property];

                        // record the previous value
                        previousValue = args.item[args.column.field];

                        // create the wrapper
                        wrapper = $('<div></div>').addClass('mat-elevation-z5 mat-menu-panel slickgrid-editor').css({
                            'z-index': 100000,
                            'position': 'absolute',
                            'overflow': 'hidden',
                            'padding': '10px 20px',
                            'cursor': 'move',
                            'transform': 'translate3d(0px, 0px, 0px)'
                        }).appendTo(container);

                        // create the input field
                        input = $('<textarea hidefocus rows="5"/>').css({
                            'height': '80px',
                            'width': args.position.width + 'px',
                            'min-width': '212px',
                            'margin-bottom': '5px',
                            'margin-top': '10px',
                            'white-space': 'pre'
                        }).tab().on('keydown', scope.handleKeyDown).appendTo(wrapper);

                        wrapper.draggable({
                            cancel: '.button, textarea, .nf-checkbox',
                            containment: 'parent'
                        });

                        // create the button panel
                        var stringCheckPanel = $('<div class="string-check-container">');
                        stringCheckPanel.appendTo(wrapper);

                        // build the custom checkbox
                        isEmpty = $('<div class="nf-checkbox string-check"/>')
                            .appendTo(stringCheckPanel);
                        $('<span class="string-check-label nf-checkbox-label">&nbsp;Set empty string</span>').appendTo(stringCheckPanel);

                        var ok = $('<button class="mat-raised-button mat-fds-primary">Ok</button>').css({
                            'top': '-5px',
                            'right': '-15px'
                        }).on('click', scope.save);
                        var cancel = $('<button class="mat-raised-button mat-fds-regular">Cancel</button>').css({
                            'top': '-5px'
                        }).on('click', scope.cancel);
                        $('<div></div>').css({
                            'position': 'relative',
                            'top': '10px',
                            'left': '20px',
                            'width': '212px',
                            'clear': 'both',
                            'float': 'right'
                        }).append(cancel).append(ok).append('<div class="clear"></div>').appendTo(wrapper);

                        // position and focus
                        scope.position(args.position);
                        input.focus().select();
                    });
                };

                this.handleKeyDown = function (e) {
                    ngZone.runOutsideAngular(function () {
                        if (e.which === $.ui.keyCode.ENTER && !e.shiftKey) {
                            scope.save();
                        } else if (e.which === $.ui.keyCode.ESCAPE) {
                            scope.cancel();

                            // prevent further propagation or escape press and prevent default behavior
                            e.stopImmediatePropagation();
                            e.preventDefault();
                        }
                    });
                };

                this.save = function () {
                    ngZone.runOutsideAngular(function () {
                        args.commitChanges();
                    });
                };

                this.cancel = function () {
                    ngZone.runOutsideAngular(function () {
                        input.val(initialValue);
                        args.cancelChanges();
                    });
                };

                this.hide = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.hide();
                    });
                };

                this.show = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.show();
                    });
                };

                this.position = function (position) {
                    ngZone.runOutsideAngular(function () {
                        wrapper.css({
                            'top': position.top - 14,
                            'left': position.left - 10
                        });
                    });
                };

                this.destroy = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.remove();
                    });
                };

                this.focus = function () {
                    ngZone.runOutsideAngular(function () {
                        input.focus();
                    });
                };

                this.loadValue = function (item) {
                    ngZone.runOutsideAngular(function () {
                        // determine if this is a sensitive property
                        var isEmptyChecked = false;
                        var sensitive = commonService.isSensitiveProperty(propertyDescriptor);

                        // determine the value to use when populating the text field
                        if (commonService.isDefinedAndNotNull(item[args.column.field])) {
                            if (sensitive) {
                                initialValue = commonService.config.sensitiveText;
                            } else {
                                initialValue = item[args.column.field];
                                isEmptyChecked = initialValue === '';
                            }
                        }

                        // determine if its an empty string
                        var checkboxStyle = isEmptyChecked ? 'checkbox-checked' : 'checkbox-unchecked';
                        isEmpty.addClass(checkboxStyle);

                        // style sensitive properties differently
                        if (sensitive) {
                            input.addClass('sensitive').keydown(function () {
                                var sensitiveInput = $(this);
                                if (sensitiveInput.hasClass('sensitive')) {
                                    sensitiveInput.removeClass('sensitive');
                                    if (sensitiveInput.val() === commonService.config.sensitiveText) {
                                        sensitiveInput.val('');
                                    }
                                }
                            });
                        }

                        input.val(initialValue);
                        input.select();
                    });
                };

                this.serializeValue = function () {
                    return ngZone.runOutsideAngular(function () {
                        // if the field has been cleared, set the value accordingly
                        if (input.val() === '') {
                            // if the user has checked the empty string checkbox, use emtpy string
                            if (isEmpty.hasClass('checkbox-checked')) {
                                return '';
                            } else {
                                // otherwise if the property is required
                                if (commonService.isRequiredProperty(propertyDescriptor)) {
                                    if (commonService.isBlank(propertyDescriptor.defaultValue)) {
                                        return previousValue;
                                    } else {
                                        return propertyDescriptor.defaultValue;
                                    }
                                } else {
                                    // if the property is not required, clear the value
                                    return null;
                                }
                            }
                        } else {
                            // if the field still has the sensitive class it means a property
                            // was edited but never modified so we should restore the previous
                            // value instead of setting it to the 'sensitive value set' string
                            if (input.hasClass('sensitive')) {
                                return previousValue;
                            } else {
                                // if there is text specified, use that value
                                return input.val();
                            }
                        }
                    });
                };

                this.applyValue = function (item, state) {
                    ngZone.runOutsideAngular(function () {
                        item[args.column.field] = state;
                    });
                };

                this.isValueChanged = function () {
                    return ngZone.runOutsideAngular(function () {
                        return scope.serializeValue() !== previousValue;
                    });
                };

                this.validate = function () {
                    return ngZone.runOutsideAngular(function () {
                        return {
                            valid: true,
                            msg: null
                        };
                    });
                };

                // initialize the custom long text editor
                this.init();
            };

            // nfel editor
            var nfelEditor = function (args) {
                var scope = this;
                var initialValue = '';
                var previousValue;
                var propertyDescriptor;
                var isEmpty;
                var wrapper;
                var editor;

                this.init = function () {
                    ngZone.runOutsideAngular(function () {
                        var container = $('body');

                        // get the property descriptor
                        var gridContainer = $(args.grid.getContainerNode());
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        propertyDescriptor = descriptors[args.item.property];

                        // determine if this is a sensitive property
                        var sensitive = commonService.isSensitiveProperty(propertyDescriptor);

                        // record the previous value
                        previousValue = args.item[args.column.field];

                        var languageId = 'nfel';
                        var editorClass = languageId + '-editor';

                        // create the wrapper
                        wrapper = $('<div></div>').addClass('mat-elevation-z5 mat-menu-panel slickgrid-nfel-editor').css({
                            'z-index': 14000,
                            'position': 'absolute',
                            'padding': '10px 20px',
                            'overflow': 'hidden',
                            'cursor': 'move',
                            'transform': 'translate3d(0px, 0px, 0px)'
                        }).draggable({
                            cancel: 'input, textarea, pre, .nf-checkbox, .button, .' + editorClass,
                            containment: 'parent'
                        }).appendTo(container);

                        // create the editor
                        editor = $('<div></div>').addClass(editorClass).appendTo(wrapper).nfeditor({
                            languageId: languageId,
                            functions: flowDesignerApi.getELSpecification(),
                            width: (args.position.width < 212) ? 212 : args.position.width,
                            minWidth: 212,
                            minHeight: 100,
                            resizable: true,
                            sensitive: sensitive,
                            escape: function () {
                                scope.cancel();
                            },
                            enter: function () {
                                scope.save();
                            }
                        });

                        // create the button panel
                        var stringCheckPanel = $('<div class="string-check-container">');
                        stringCheckPanel.appendTo(wrapper);

                        // build the custom checkbox
                        isEmpty = $('<div class="nf-checkbox string-check"/>')
                            .appendTo(stringCheckPanel);
                        $('<span class="string-check-label nf-checkbox-label">&nbsp;Set empty string</span>').appendTo(stringCheckPanel);

                        var ok = $('<button class="mat-raised-button mat-fds-primary">Ok</button>').css({
                            'top': '-5px',
                            'right': '-15px'
                        }).on('click', scope.save);
                        var cancel = $('<button class="mat-raised-button mat-fds-regular">Cancel</button>').css({
                            'top': '-5px'
                        }).on('click', scope.cancel);
                        $('<div></div>').css({
                            'position': 'relative',
                            'top': '10px',
                            'left': '20px',
                            'width': '212px',
                            'clear': 'both',
                            'float': 'right'
                        }).append(cancel).append(ok).append('<div class="clear"></div>').appendTo(wrapper);

                        // position and focus
                        scope.position(args.position);
                        editor.nfeditor('focus').nfeditor('selectAll');
                    });
                };

                this.save = function () {
                    ngZone.runOutsideAngular(function () {
                        args.commitChanges();
                    });
                };

                this.cancel = function () {
                    ngZone.runOutsideAngular(function () {
                        editor.nfeditor('setValue', initialValue);
                        args.cancelChanges();
                    });
                };

                this.hide = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.hide();
                    });
                };

                this.show = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.show();
                        editor.nfeditor('refresh');
                    });
                };

                this.position = function (position) {
                    ngZone.runOutsideAngular(function () {
                        wrapper.css({
                            'top': position.top - 16,
                            'left': position.left - 42
                        });
                    });
                };

                this.destroy = function () {
                    ngZone.runOutsideAngular(function () {
                        editor.nfeditor('destroy');
                        wrapper.remove();
                    });
                };

                this.focus = function () {
                    ngZone.runOutsideAngular(function () {
                        editor.nfeditor('focus');
                    });
                };

                this.loadValue = function (item) {
                    ngZone.runOutsideAngular(function () {
                        // determine if this is a sensitive property
                        var isEmptyChecked = false;
                        var sensitive = commonService.isSensitiveProperty(propertyDescriptor);

                        // determine the value to use when populating the text field
                        if (commonService.isDefinedAndNotNull(item[args.column.field])) {
                            if (sensitive) {
                                initialValue = commonService.config.sensitiveText;
                            } else {
                                initialValue = item[args.column.field];
                                isEmptyChecked = initialValue === '';
                            }
                        }

                        // determine if its an empty string
                        var checkboxStyle = isEmptyChecked ? 'checkbox-checked' : 'checkbox-unchecked';
                        isEmpty.addClass(checkboxStyle);

                        editor.nfeditor('setValue', initialValue).nfeditor('selectAll');
                    });
                };

                this.serializeValue = function () {
                    return ngZone.runOutsideAngular(function () {
                        var value = editor.nfeditor('getValue');

                        // if the field has been cleared, set the value accordingly
                        if (value === '') {
                            // if the user has checked the empty string checkbox, use emtpy string
                            if (isEmpty.hasClass('checkbox-checked')) {
                                return '';
                            } else {
                                // otherwise if the property is required
                                if (commonService.isRequiredProperty(propertyDescriptor)) {
                                    if (commonService.isBlank(propertyDescriptor.defaultValue)) {
                                        return previousValue;
                                    } else {
                                        return propertyDescriptor.defaultValue;
                                    }
                                } else {
                                    // if the property is not required, clear the value
                                    return null;
                                }
                            }
                        } else {
                            // if the field still has the sensitive class it means a property
                            // was edited but never modified so we should restore the previous
                            // value instead of setting it to the 'sensitive value set' string

                            // if the field hasn't been modified return the previous value... this
                            // is important because sensitive properties contain the text 'sensitive
                            // value set' which is cleared when the value is edited. we do not
                            // want to actually use this value
                            if (editor.nfeditor('isModified') === false) {
                                return previousValue;
                            } else {
                                // if there is text specified, use that value
                                return value;
                            }
                        }
                    });
                };

                this.applyValue = function (item, state) {
                    ngZone.runOutsideAngular(function () {
                        item[args.column.field] = state;
                    });
                };

                this.isValueChanged = function () {
                    return ngZone.runOutsideAngular(function () {
                        return scope.serializeValue() !== previousValue;
                    });
                };

                this.validate = function () {
                    return ngZone.runOutsideAngular(function () {
                        return {
                            valid: true,
                            msg: null
                        };
                    });
                };

                // initialize the custom long nfel editor
                this.init();
            };

            // combo editor
            var comboEditor = function (args) {
                var scope = this;
                var initialValue = null;
                var wrapper;
                var combo;
                var propertyDescriptor;

                this.init = function () {
                    ngZone.runOutsideAngular(function () {
                        var container = $('body');

                        // get the property descriptor
                        var gridContainer = $(args.grid.getContainerNode());
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        propertyDescriptor = descriptors[args.item.property];

                        // get the options
                        var propertyContainer = gridContainer.closest('.property-container');
                        var configurationOptions = flowDesignerPropertyTableComponent.options;

                        // create the wrapper
                        wrapper = $('<div class="mat-elevation-z5 mat-menu-panel combo-editor"></div>').css({
                            'z-index': 1999,
                            'position': 'absolute',
                            'padding': '10px 20px',
                            'overflow': 'hidden',
                            'cursor': 'move',
                            'transform': 'translate3d(0px, 0px, 0px)'
                        }).draggable({
                            cancel: '.button, .combo',
                            containment: 'parent'
                        }).appendTo(container);

                        // check for allowable values which will drive which editor to use
                        var allowableValues = commonService.getAllowableValues(propertyDescriptor);

                        // show the output port options
                        var options = [];
                        if (propertyDescriptor.required === false) {
                            options.push({
                                text: 'No value',
                                value: null,
                                optionClass: 'unset'
                            });
                        }
                        if ($.isArray(allowableValues)) {
                            $.each(allowableValues, function (i, allowableValueEntity) {
                                var allowableValue = allowableValueEntity.allowableValue;
                                options.push({
                                    text: allowableValue.displayName,
                                    value: allowableValue.value,
                                    disabled: allowableValueEntity.canRead === false && allowableValue.value !== args.item['previousValue'],
                                    description: commonService.escapeHtml(allowableValue.description)
                                });
                            });
                        }

                        // ensure the options there is at least one option
                        if (options.length === 0) {
                            options.push({
                                text: 'No value',
                                value: null,
                                optionClass: 'unset',
                                disabled: true
                            });
                        }

                        // determine the max height
                        var position = args.position;
                        var windowHeight = $(window).height();
                        var maxHeight = windowHeight - position.bottom - 16;

                        // build the combo field
                        combo = $('<button class="combo mat-raised-button mat-fds-regular push-top-sm push-bottom-sm"></button>').combo({
                            options: options,
                            maxHeight: maxHeight
                        }).css({
                            'width': ((position.width - 32) < 234) ? 234 : (position.width - 32) + 'px'
                        }).appendTo(wrapper);

                        // add buttons for handling user input
                        var cancel = $('<button class="mat-raised-button mat-fds-regular">Cancel</button>').on('click', scope.cancel);
                        var ok = $('<button class="mat-raised-button mat-fds-primary push-left-sm">Ok</button>').on('click', scope.save);

                        $('<div></div>').css({
                            'position': 'relative',
                            'clear': 'both',
                            'float': 'right'
                        }).append(cancel).append(ok).appendTo(wrapper);

                        // position and focus
                        scope.position(position);
                    });
                };

                this.save = function () {
                    ngZone.runOutsideAngular(function () {
                        args.commitChanges();
                    });
                };

                this.cancel = function () {
                    ngZone.runOutsideAngular(function () {
                        args.cancelChanges();
                    });
                };

                this.hide = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.hide();
                    });
                };

                this.show = function () {
                    ngZone.runOutsideAngular(function () {
                        wrapper.show();
                    });
                };

                this.position = function (position) {
                    ngZone.runOutsideAngular(function () {
                        wrapper.css({
                            'top': position.top - 18,
                            'left': position.left - 23
                        });
                    });
                };

                this.destroy = function () {
                    ngZone.runOutsideAngular(function () {
                        combo.combo('destroy');
                        wrapper.remove();
                    });
                };

                this.focus = function () {
                    ngZone.runOutsideAngular(function () {
                    });
                };

                this.loadValue = function (item) {
                    ngZone.runOutsideAngular(function () {
                        // select as appropriate
                        if (!commonService.isUndefined(item.value)) {
                            initialValue = item.value;

                            combo.combo('setSelectedOption', {
                                value: item.value
                            });
                        } else if (commonService.isDefinedAndNotNull(propertyDescriptor.defaultValue)) {
                            initialValue = propertyDescriptor.defaultValue;

                            combo.combo('setSelectedOption', {
                                value: propertyDescriptor.defaultValue
                            });
                        }
                    });
                };

                this.serializeValue = function () {
                    return ngZone.runOutsideAngular(function () {
                        var selectedOption = combo.combo('getSelectedOption');
                        return selectedOption.value;
                    });
                };

                this.applyValue = function (item, state) {
                    ngZone.runOutsideAngular(function () {
                        item[args.column.field] = state;
                    });
                };

                this.isValueChanged = function () {
                    return ngZone.runOutsideAngular(function () {
                        var selectedOption = combo.combo('getSelectedOption');
                        return (!(selectedOption.value === "" && initialValue === null)) && (selectedOption.value !== initialValue);
                    });
                };

                this.validate = function () {
                    return ngZone.runOutsideAngular(function () {
                        return {
                            valid: true,
                            msg: null
                        };
                    });
                };

                // initialize the custom long text editor
                this.init();
            };

            /**
             * Shows the property value for the specified row and cell.
             *
             * @param {type} propertyGrid
             * @param {type} descriptors
             * @param {type} row
             * @param {type} cell
             */
            var showPropertyValue = function (propertyGrid, descriptors, row, cell) {
                ngZone.runOutsideAngular(function () {
                    // remove any currently open detail dialogs
                    universalCaptureService.removeAllPropertyDetailDialogs();

                    // get the property in question
                    var propertyData = propertyGrid.getData();
                    var property = propertyData.getItem(row);

                    // ensure there is a value
                    if (commonService.isDefinedAndNotNull(property.value)) {

                        // get the descriptor to insert the description tooltip
                        var propertyDescriptor = descriptors[property.property];

                        // ensure we're not dealing with a sensitive property
                        if (!commonService.isSensitiveProperty(propertyDescriptor)) {

                            // get details about the location of the cell
                            var cellNode = $(propertyGrid.getCellNode(row, cell));
                            var offset = cellNode.offset();

                            // create the wrapper
                            var wrapper = $('<div class="property-detail"></div>').css({
                                'z-index': 1999,
                                'position': 'absolute',
                                'padding': '10px 20px',
                                'overflow': 'hidden',
                                'border-radius': '2px',
                                'box-shadow': 'rgba(0, 0, 0, 0.247059) 0px 2px 5px',
                                'background-color': 'rgb(255, 255, 255)',
                                'cursor': 'move',
                                'transform': 'translate3d(0px, 0px, 0px)',
                                'top': offset.top - 17,
                                'left': offset.left - 23
                            }).appendTo('body');

                            var allowableValues = commonService.getAllowableValues(propertyDescriptor);
                            if ($.isArray(allowableValues)) {
                                // prevent dragging over the combo
                                wrapper.draggable({
                                    cancel: '.button, .combo',
                                    containment: 'parent'
                                });

                                // create the read only options
                                var options = [];
                                $.each(allowableValues, function (i, allowableValueEntity) {
                                    var allowableValue = allowableValueEntity.allowableValue;
                                    options.push({
                                        text: allowableValue.displayName,
                                        value: allowableValue.value,
                                        description: commonService.escapeHtml(allowableValue.description),
                                        disabled: true
                                    });
                                });

                                // ensure the options there is at least one option
                                if (options.length === 0) {
                                    options.push({
                                        text: 'No value',
                                        value: null,
                                        optionClass: 'unset',
                                        disabled: true
                                    });
                                }

                                // determine the max height
                                var windowHeight = $(window).height();
                                var maxHeight = windowHeight - (offset.top + cellNode.height()) - 16;
                                var width = cellNode.width() - 16;

                                // build the combo field
                                $('<div class="value-combo combo"></div>').css({
                                    'width': width,
                                    'margin-top': '10px',
                                    'margin-bottom': '10px'
                                }).combo({
                                    options: options,
                                    maxHeight: maxHeight,
                                    selectedOption: {
                                        value: property.value
                                    }
                                }).appendTo(wrapper);

                                $('<div class="button">Ok</div>').css({
                                    'position': 'relative',
                                    'top': '10px',
                                    'left': '20px'
                                }).hover(
                                    function () {
                                        $(this).css('background', '#004849');
                                    }, function () {
                                        $(this).css('background', '#728E9B');
                                    }).on('click', function () {
                                    wrapper.hide().remove();
                                }).appendTo(wrapper);
                            } else {
                                var editor = null;

                                // so the nfel editor is appropriate
                                if (commonService.supportsEl(propertyDescriptor)) {
                                    var languageId = 'nfel';
                                    var editorClass = languageId + '-editor';

                                    // prevent dragging over the nf editor
                                    wrapper.css({
                                        'z-index': 1999,
                                        'position': 'absolute',
                                        'padding': '10px 20px',
                                        'overflow': 'hidden',
                                        'border-radius': '2px',
                                        'box-shadow': 'rgba(0, 0, 0, 0.247059) 0px 2px 5px',
                                        'background-color': 'rgb(255, 255, 255)',
                                        'cursor': 'move',
                                        'transform': 'translate3d(0px, 0px, 0px)',
                                        'top': offset.top - 7,
                                        'left': offset.left - 46
                                    }).draggable({
                                        cancel: 'input, textarea, pre, .button, .' + editorClass,
                                        containment: 'parent'
                                    });

                                    // create the editor
                                    editor = $('<div></div>').addClass(editorClass).appendTo(wrapper).nfeditor({
                                        languageId: languageId,
                                        functions: flowDesignerApi.getELSpecification(),
                                        width: cellNode.width(),
                                        content: property.value,
                                        minWidth: 175,
                                        minHeight: 100,
                                        readOnly: true,
                                        resizable: true,
                                        escape: function () {
                                            cleanUp();
                                        }
                                    });
                                } else {
                                    wrapper.css({
                                        'z-index': 1999,
                                        'position': 'absolute',
                                        'padding': '10px 20px',
                                        'overflow': 'hidden',
                                        'border-radius': '2px',
                                        'box-shadow': 'rgba(0, 0, 0, 0.247059) 0px 2px 5px',
                                        'background-color': 'rgb(255, 255, 255)',
                                        'cursor': 'move',
                                        'transform': 'translate3d(0px, 0px, 0px)',
                                        'top': offset.top - 17,
                                        'left': offset.left - 23
                                    });

                                    // create the input field
                                    $('<textarea hidefocus rows="5" readonly="readonly"/>').css({
                                        'height': '80px',
                                        'resize': 'both',
                                        'width': cellNode.width() + 'px',
                                        'margin': '10px 0px',
                                        'white-space': 'pre'
                                    }).text(property.value).on('keydown', function (evt) {
                                        if (evt.which === $.ui.keyCode.ESCAPE) {
                                            cleanUp();

                                            evt.stopImmediatePropagation();
                                            evt.preventDefault();
                                        }
                                    }).appendTo(wrapper);

                                    // prevent dragging over standard components
                                    wrapper.draggable({
                                        containment: 'parent'
                                    });
                                }

                                var cleanUp = function () {
                                    // clean up the editor
                                    if (editor !== null) {
                                        editor.nfeditor('destroy');
                                    }

                                    // clean up the rest
                                    wrapper.hide().remove();
                                };

                                // add an ok button that will remove the entire pop up
                                var ok = $('<div class="button">Ok</div>').css({
                                    'position': 'relative',
                                    'top': '10px',
                                    'left': '20px'
                                }).hover(
                                    function () {
                                        $(this).css('background', '#004849');
                                    }, function () {
                                        $(this).css('background', '#728E9B');
                                    }).on('click', function () {
                                    cleanUp();
                                });

                                $('<div></div>').append(ok).append('<div class="clear"></div>').appendTo(wrapper);
                            }
                        }
                    }
                });
            };

            var initPropertiesTable = function (table, options) {
                ngZone.runOutsideAngular(function () {
                    // function for formatting the property name
                    var nameFormatter = function (row, cell, value, columnDef, dataContext) {
                        var nameWidthOffset = 30;
                        var cellContent = $('<div></div>');

                        // format the contents
                        var formattedValue = $('<span/>').addClass('table-cell ellipsis').text(value).appendTo(cellContent);
                        if (dataContext.type === 'required') {
                            formattedValue.addClass('required');
                        }

                        // get the property descriptor
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        var propertyDescriptor = descriptors[dataContext.property];

                        // show the property description if applicable
                        if (commonService.isDefinedAndNotNull(propertyDescriptor)) {
                            $('<div class="fa fa-question-circle float-right" alt="Info"></div>').appendTo(cellContent);
                            $('<span class="hidden property-descriptor-name"></span>').text(dataContext.property).appendTo(cellContent);
                            nameWidthOffset = 46; // 10 + icon width (10) + icon margin (6) + padding (20)
                        }

                        // adjust the width accordingly
                        formattedValue.width(columnDef.width - nameWidthOffset);

                        // return the cell content
                        return cellContent.html();
                    };

                    // function for formatting the property value
                    var valueFormatter = function (row, cell, value, columnDef, dataContext) {
                        var valueMarkup;
                        if (commonService.isDefinedAndNotNull(value)) {
                            // get the property descriptor
                            var descriptors = flowDesignerPropertyTableComponent.descriptors;
                            var propertyDescriptor = descriptors[dataContext.property];

                            // determine if the property is sensitive
                            if (commonService.isSensitiveProperty(propertyDescriptor)) {
                                valueMarkup = '<span class="table-cell ellipsis sensitive">Sensitive value set</span>';
                            } else {
                                var resolvedAllowableValue = false;

                                // if there are allowable values, attempt to swap out for the display name
                                var allowableValues = commonService.getAllowableValues(propertyDescriptor);
                                if ($.isArray(allowableValues)) {
                                    $.each(allowableValues, function (_, allowableValueEntity) {
                                        var allowableValue = allowableValueEntity.allowableValue;
                                        if (value === allowableValue.value) {
                                            value = allowableValue.displayName;
                                            resolvedAllowableValue = true;
                                            return false;
                                        }
                                    });
                                }

                                if (value === '') {
                                    valueMarkup = '<span class="table-cell ellipsis blank">Empty string set</span>';
                                } else {
                                    if (!resolvedAllowableValue && commonService.isDefinedAndNotNull(propertyDescriptor.identifiesControllerService)) {
                                        valueMarkup = '<span class="table-cell ellipsis blank">Incompatible Service Configured</div>';
                                    } else {
                                        valueMarkup = '<div class="table-cell ellipsis value"><pre class="ellipsis">' + commonService.escapeHtml(value) + '</pre></div>';
                                    }
                                }
                            }
                        } else {
                            valueMarkup = '<span class="unset ellipsis">No value set</span>';
                        }

                        // format the contents
                        var content = $(valueMarkup);
                        if (dataContext.type === 'required') {
                            content.addClass('required');
                        }
                        content.find('.ellipsis').width(columnDef.width - 10);

                        // return the appropriate markup
                        return $('<div/>').append(content).html();
                    };

                    var propertyColumns = [
                        {
                            id: 'property',
                            field: 'displayName',
                            name: 'Property',
                            sortable: false,
                            resizable: true,
                            rerenderOnResize: true,
                            formatter: nameFormatter
                        },
                        {
                            id: 'value',
                            field: 'value',
                            name: 'Value',
                            sortable: false,
                            resizable: true,
                            cssClass: 'pointer',
                            rerenderOnResize: true,
                            formatter: valueFormatter
                        }
                    ];

                    // custom formatter for the actions column
                    var actionFormatter = function (row, cell, value, columnDef, dataContext) {
                        var markup = '';

                        // allow user defined properties to be removed
                        if (options.readOnly !== true && dataContext.type === 'userDefined') {
                            markup += '<div title="Delete" class="delete-property pointer fa fa-trash"></div>';
                        }

                        return markup;
                    };
                    propertyColumns.push(
                        {
                            id: "actions",
                            name: "&nbsp;",
                            minWidth: 20,
                            width: 20,
                            formatter: actionFormatter
                        });

                    var propertyConfigurationOptions = {
                        forceFitColumns: true,
                        enableTextSelectionOnCells: true,
                        enableCellNavigation: true,
                        enableColumnReorder: false,
                        editable: options.readOnly !== true,
                        enableAddRow: false,
                        autoEdit: false,
                        rowHeight: 34
                    };

                    // initialize the dataview
                    var propertyData = new Slick.Data.DataView({
                        inlineFilters: false
                    });
                    propertyData.setItems([]);
                    propertyData.setFilterArgs({
                        searchString: '',
                        property: 'hidden'
                    });
                    propertyData.setFilter(filter);
                    propertyData.getItemMetadata = function (index) {
                        var item = propertyData.getItem(index);

                        // get the property descriptor
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        var propertyDescriptor = descriptors[item.property];

                        // support el if specified or unsure yet (likely a dynamic property)
                        if (commonService.isUndefinedOrNull(propertyDescriptor) || commonService.supportsEl(propertyDescriptor)) {
                            return {
                                columns: {
                                    value: {
                                        editor: nfelEditor
                                    }
                                }
                            };
                        } else {
                            // check for allowable values which will drive which editor to use
                            var allowableValues = commonService.getAllowableValues(propertyDescriptor);
                            if ($.isArray(allowableValues)) {
                                return {
                                    columns: {
                                        value: {
                                            editor: comboEditor
                                        }
                                    }
                                };
                            } else {
                                return {
                                    columns: {
                                        value: {
                                            editor: textEditor
                                        }
                                    }
                                };
                            }
                        }
                    };

                    // initialize the grid
                    var propertyGrid = new Slick.Grid(table, propertyData, propertyColumns, propertyConfigurationOptions);
                    propertyGrid.setSelectionModel(new Slick.RowSelectionModel());
                    propertyGrid.onClick.subscribe(function (e, args) {
                        if (propertyGrid.getColumns()[args.cell].id === 'value') {
                            if (options.readOnly === true) {
                                var descriptors = flowDesignerPropertyTableComponent.descriptors;
                                showPropertyValue(propertyGrid, descriptors, args.row, args.cell);
                            } else {
                                // edits the clicked cell
                                propertyGrid.gotoCell(args.row, args.cell, true);
                            }

                            // prevents standard edit logic
                            e.stopImmediatePropagation();
                        } else if (propertyGrid.getColumns()[args.cell].id === 'actions') {
                            var property = propertyData.getItem(args.row);

                            var target = $(e.target);
                            if (target.hasClass('delete-property')) {
                                // mark the property in question for removal and refresh the table
                                propertyData.updateItem(property.id, $.extend(property, {
                                    value: null,
                                    hidden: true
                                }));

                                // prevents standard edit logic
                                e.stopImmediatePropagation();
                            }
                        }
                    });
                    propertyGrid.onKeyDown.subscribe(function (e, args) {
                        if (e.which === $.ui.keyCode.ESCAPE) {
                            var editorLock = propertyGrid.getEditorLock();
                            if (editorLock.isActive()) {
                                editorLock.cancelCurrentEdit();

                                // prevents standard cancel logic - standard logic does
                                // not stop propagation when escape is pressed
                                e.stopImmediatePropagation();
                                e.preventDefault();
                            }
                        }
                    });

                    propertyGrid.onCellChange.subscribe(function (e, args) {
                        ngZone.run(function () {
                            flowDesignerPropertyTableComponent.properties = self.marshalProperties();
                            flowDesignerPropertyTableComponent.onChange(flowDesignerPropertyTableComponent.properties);
                        });
                    });

                    if (options.readOnly !== true) {
                        propertyGrid.onBeforeCellEditorDestroy.subscribe(function (e, args) {
                            setTimeout(function () {
                                propertyGrid.resizeCanvas();
                            }, 50);
                        });
                    }

                    // wire up the dataview to the grid
                    propertyData.onRowCountChanged.subscribe(function (e, args) {
                        propertyGrid.updateRowCount();
                        propertyGrid.render();

                        ngZone.run(function () {
                            flowDesignerPropertyTableComponent.properties = self.marshalProperties();
                            flowDesignerPropertyTableComponent.onChange(flowDesignerPropertyTableComponent.properties);
                        });
                    });
                    propertyData.onRowsChanged.subscribe(function (e, args) {
                        propertyGrid.invalidateRows(args.rows);
                        propertyGrid.render();
                    });

                    // hold onto an instance of the grid and listen for mouse events to add tooltips where appropriate
                    table.data('gridInstance', propertyGrid).on('mouseenter', 'div.slick-cell', function (e) {
                        var infoIcon = $(this).find('div.fa-question-circle');
                        if (infoIcon.length && !infoIcon.data('qtip')) {
                            var property = $(this).find('span.property-descriptor-name').text();

                            // get the property descriptor
                            var descriptors = flowDesignerPropertyTableComponent.descriptors;
                            var propertyDescriptor = descriptors[property];

                            // format the tooltip
                            var tooltip = commonService.formatPropertyTooltip(propertyDescriptor);

                            if (commonService.isDefinedAndNotNull(tooltip)) {
                                infoIcon.qtip($.extend({},
                                    commonService.config.tooltipConfig,
                                    {
                                        content: tooltip
                                    }));
                            }
                        }
                    });
                });
            };

            var saveRow = function (table) {
                ngZone.runOutsideAngular(function () {
                    // get the property grid to commit the current edit
                    var propertyGrid = table.data('gridInstance');
                    if (commonService.isDefinedAndNotNull(propertyGrid)) {
                        var editController = propertyGrid.getEditController();
                        editController.commitCurrentEdit();
                    }
                });
            };

            /**
             * Performs the filtering.
             *
             * @param {object} item     The item subject to filtering
             * @param {object} args     Filter arguments
             * @returns {Boolean}       Whether or not to include the item
             */
            var filter = function (item, args) {
                return ngZone.runOutsideAngular(function () {
                    return item.hidden === false;
                });
            };

            /**
             * Loads the specified properties.
             *
             * @param {type} table
             * @param {type} properties
             * @param {type} descriptors
             */
            var loadProperties = function (table, properties, descriptors) {
                ngZone.runOutsideAngular(function () {
                    // get the grid
                    var propertyGrid = table.data('gridInstance');
                    var propertyData = propertyGrid.getData();

                    // generate the properties
                    if (commonService.isDefinedAndNotNull(properties)) {
                        propertyData.beginUpdate();

                        var i = 0;
                        Object.keys(descriptors).forEach(function (name) {
                            // get the property descriptor
                            var descriptor = descriptors[name];
                            var value = properties[name];

                            // determine the property type
                            var type = 'userDefined';
                            var displayName = name;
                            if (commonService.isDefinedAndNotNull(descriptor)) {
                                if (commonService.isRequiredProperty(descriptor)) {
                                    type = 'required';
                                } else if (commonService.isDynamicProperty(descriptor)) {
                                    type = 'userDefined';
                                } else {
                                    type = 'optional';
                                }

                                // use the display name if possible
                                if (commonService.isDefinedAndNotNull(descriptor.displayName)) {
                                    displayName = descriptor.displayName;
                                }

                                // determine the value
                                if (commonService.isNull(value) && commonService.isDefinedAndNotNull(descriptor.defaultValue)) {
                                    value = descriptor.defaultValue;
                                }
                            }

                            // add the row
                            propertyData.addItem({
                                id: i++,
                                hidden: false,
                                property: name,
                                displayName: displayName,
                                previousValue: value,
                                value: value,
                                type: type
                            });
                        });

                        propertyData.endUpdate();
                    }
                });
            };

            /**
             * Clears the property table container.
             *
             * @param {jQuery} propertyTableContainer
             */
            var clear = function (propertyTableContainer) {
                ngZone.runOutsideAngular(function () {
                    var options = flowDesignerPropertyTableComponent.options;
                    if (options.readOnly === true) {
                        universalCaptureService.removeAllPropertyDetailDialogs();
                    }

                    // clean up data
                    var table = propertyTableContainer.find('div.property-table');

                    // clean up any tooltips that may have been generated
                    commonService.cleanUpTooltips(table, 'div.fa-question-circle');

                    // clear the data in the grid
                    var propertyGrid = table.data('gridInstance');
                    var propertyData = propertyGrid.getData();
                    propertyData.setItems([]);
                });
            };

            /**
             * Initialize.
             *
             * @param flowDesignerPropertyTableComponentRef                     The flow designer property table component.
             */
            this.init = function (flowDesignerPropertyTableComponentRef) {
                propertyTableContainerElement = flowDesignerPropertyTableComponentRef.propertyTableElementRef.nativeElement;
                flowDesignerCanvasComponent = flowDesignerPropertyTableComponentRef.flowDesignerCanvasComponent;
                flowDesignerApi = flowDesignerPropertyTableComponentRef.flowDesignerApi;
                ngZone = flowDesignerPropertyTableComponentRef.ngZone;
                flowDesignerPropertyTableComponent = flowDesignerPropertyTableComponentRef;

                var options = flowDesignerPropertyTableComponentRef.options;

                ngZone.runOutsideAngular(function () {
                    // ensure the options have been properly specified
                    if (commonService.isDefinedAndNotNull(options)) {
                        var propertyTableContainer = $(propertyTableContainerElement);

                        // clear any current contents, remote events, and store options
                        propertyTableContainer.empty().unbind().addClass('property-container');

                        // build the table
                        var table = $('<div class="property-table"></div>').appendTo(propertyTableContainer);

                        // initializes the properties table
                        initPropertiesTable(table, options);
                    }
                });

                this.setGroupId(flowDesignerPropertyTableComponent.parentGroupId);

                if(commonService.isDefinedAndNotNull(flowDesignerPropertyTableComponent.properties)) {
                    this.loadProperties(flowDesignerPropertyTableComponent.properties, flowDesignerPropertyTableComponent.descriptors);
                }
            };

            /**
             * Add a new property to this property table. Returns true if the new property was
             * added, false if the property already exists.
             *
             * @param descriptor    The property descriptor
             * @returns             Whether the new property was successfully added
             */
            this.addProperty = function (descriptor) {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    var propertyData = propertyGrid.getData();

                    // ensure the property name is unique
                    var existingItem = null;
                    $.each(propertyData.getItems(), function (_, item) {
                        if (descriptor.name === item.property) {
                            existingItem = item;
                            return false;
                        }
                    });

                    if (existingItem === null) {
                        // store the descriptor for use later
                        var descriptors = flowDesignerPropertyTableComponent.descriptors;
                        if (!commonService.isUndefined(descriptors)) {
                            descriptors[descriptor.name] = descriptor;
                        }

                        // add a row for the new property
                        var id = propertyData.getLength();
                        propertyData.addItem({
                            id: id,
                            hidden: false,
                            property: descriptor.name,
                            displayName: descriptor.displayName,
                            previousValue: null,
                            value: null,
                            type: 'userDefined'
                        });

                        return true;
                    } else {
                        // if this row is currently hidden, clear the value and show it
                        if (existingItem.hidden === true) {
                            propertyData.updateItem(existingItem.id, $.extend(existingItem, {
                                hidden: false,
                                value: null
                            }));

                            return true;
                        } else {
                            dialogRef = fdsDialogService.openConfirm({
                                title: 'Property Exists',
                                message: 'A property with this name already exists.',
                                acceptButton: 'Ok'
                            });

                            return false;
                        }
                    }
                });
            };

            /**
             *
             *
             * @param componentEntity
             */
            this.setValue = function (properties) {
                this.loadProperties(properties, flowDesignerPropertyTableComponent.descriptors);
            };

            /**
             * Loads the specified properties.
             *
             * @param {object} properties        The properties
             * @param {map} descriptors          The property descriptors (property name -> property descriptor)
             */
            this.loadProperties = function (properties, descriptors) {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    loadProperties(table, properties, descriptors);
                });
            };

            /**
             * Saves the last edited row in the specified grid.
             */
            this.saveRow = function () {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    saveRow(table);
                });
            };

            /**
             * Update the size of the grid based on its container's current size.
             */
            this.resetTableSize = function () {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    if (commonService.isDefinedAndNotNull(propertyGrid)) {
                        propertyGrid.resizeCanvas();
                    }
                });
            };

            /**
             * Edits the property with the specified name.
             *
             * @param propertyName
             * @returns {*}
             */
            this.edit = function (propertyName) {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    if (commonService.isDefinedAndNotNull(propertyGrid)) {
                        var propertyData = propertyGrid.getData();

                        // ensure the property name is unique
                        var existingItem = null;
                        $.each(propertyData.getItems(), function (_, item) {
                            if (propertyName === item.property) {
                                existingItem = item;
                                return false;
                            }
                        });

                        // edit the existing item if found
                        if (existingItem !== null) {
                            var row = propertyData.getRowById(existingItem.id);
                            propertyGrid.setActiveCell(row, propertyGrid.getColumnIndex('value'));
                            propertyGrid.editActiveCell();
                        } else {
                            dialogRef = fdsDialogService.openConfirm({
                                title: 'Edit property',
                                message: 'The requested property does not exist.',
                                acceptButton: 'Ok'
                            });
                        }
                    }
                });
            };

            /**
             * Cancels the edit in the specified row.
             */
            this.cancelEdit = function () {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    if (commonService.isDefinedAndNotNull(propertyGrid)) {
                        var editController = propertyGrid.getEditController();
                        editController.cancelCurrentEdit();
                    }
                });
            };

            /**
             * Scrolls to the specified row. If the property does not exist, this
             * function does nothing.
             *
             * @param propertyName      The property to scroll into view
             */
            this.scrollToRow = function (propertyName) {
                return ngZone.runOutsideAngular(function () {
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    if (commonService.isDefinedAndNotNull(propertyGrid)) {
                        var propertyData = propertyGrid.getData();

                        // ensure the property name is unique
                        var existingItem = null;
                        $.each(propertyData.getItems(), function (_, item) {
                            if (propertyName === item.property) {
                                existingItem = item;
                                return false;
                            }
                        });

                        // edit the existing item if found
                        if (existingItem !== null) {
                            // select the existing properties row
                            var row = propertyData.getRowById(existingItem.id);
                            propertyGrid.setSelectedRows([row]);
                            propertyGrid.scrollRowIntoView(row);
                        }
                    }
                });
            };

            /**
             * Destroys the property table.
             */
            this.destroy = function () {
                return ngZone.runOutsideAngular(function () {
                    var propertyTableContainer = $(propertyTableContainerElement);
                    var options = flowDesignerPropertyTableComponent.options;

                    if (commonService.isDefinedAndNotNull(options)) {
                        // clear the property table container
                        clear(propertyTableContainer);
                    }
                    if (commonService.isDefinedAndNotNull(dialogRef)) {
                        dialogRef.close();
                    }
                });
            };

            /**
             * Clears the property table.
             */
            this.clear = function () {
                return ngZone.runOutsideAngular(function () {
                    clear($(propertyTableContainerElement));
                });
            };

            /**
             * Determines if a save is required for the first matching element.
             */
            this.isSaveRequired = function () {
                return ngZone.runOutsideAngular(function () {
                    var isSaveRequired = false;

                    // get the property grid
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    var propertyData = propertyGrid.getData();

                    // determine if any of the properties have changed
                    $.each(propertyData.getItems(), function () {
                        if (this.value !== this.previousValue) {
                            isSaveRequired = true;
                            return false;
                        }
                    });

                    return isSaveRequired;
                });
            };

            /**
             * Marshalls the properties for the first matching element.
             */
            this.marshalProperties = function () {
                return ngZone.runOutsideAngular(function () {
                    // properties
                    var properties = {};

                    // get the property grid data
                    var table = $(propertyTableContainerElement).find('div.property-table');
                    var propertyGrid = table.data('gridInstance');
                    var propertyData = propertyGrid.getData();
                    $.each(propertyData.getItems(), function () {
                        properties[this.property] = this.value;
                    });

                    return properties;
                });
            };

            /**
             * Sets the current group id.
             */
            this.setGroupId = function (currentGroupId) {
                return ngZone.runOutsideAngular(function () {
                    groupId = currentGroupId;
                });
            };
        };

        PropertyTable.prototype = {
            constructor: PropertyTable
        };

        return PropertyTable;
    }
};

PropertyTableFactory.prototype = {
    constructor: PropertyTableFactory
};

module.exports = {
    build: PropertyTableFactory
};
