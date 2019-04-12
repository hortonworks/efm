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
var CodeMirror = require('codemirror');
var nfel = require('@flow-designer/jquery/nfel');

(function () {

    var isUndefined = function (obj) {
        return typeof obj === 'undefined';
    };

    var isNull = function (obj) {
        return obj === null;
    };

    var isDefinedAndNotNull = function (obj) {
        return !isUndefined(obj) && !isNull(obj);
    };

    var isBlank = function (str) {
        return isUndefined(str) || isNull(str) || str === '';
    };

    var isFunction = function (funct) {
        return typeof funct === 'function';
    };

    var methods = {

        /**
         * Create a new nf editor. The options are specified in the following
         * format:
         *
         * {
         *   languageId: 'nfel',
         *   resizable: true,
         *   sensitive: false,
         *   readOnly: false,
         *   content: '${attribute}',
         *   width: 200,
         *   height: 200,
         *   minWidth: 150,
         *   minHeight: 150
         * }
         *
         * @param {object} options  The options for this editor.
         */
        init: function (options) {
            return this.each(function () {
                if (isUndefined(options) || isNull(options)) {
                    return false;
                }

                var languageId = options.languageId;
                var languageAssist = nfel;

                if (isUndefined(languageAssist) || isNull(languageAssist)) {
                    return false;
                }

                // should support resizing
                var resizable = options.resizable === true;

                // is the property sensitive
                var sensitive = options.sensitive === true;

                var content = isDefinedAndNotNull(options.content) ? options.content : '';
                var field = $('<textarea></textarea>').text(content).appendTo($(this));

                // define a mode for NiFi expression language
                if (isFunction(languageAssist.color)) {
                    CodeMirror.defineMode(languageId, languageAssist.color);

                    // is the editor read only
                    var readOnly = options.readOnly === true;

                    var extraKeys = {
                        'Esc': function (cm) {
                            if (isFunction(options.escape)) {
                                options.escape();
                            }
                        },
                        'Enter': function (cm) {
                            if (isFunction(options.enter)) {
                                options.enter();
                            }
                        }
                    };

                    var functions$ = options.functions;
                    if (isDefinedAndNotNull(functions$) && isFunction(languageAssist.setFunctions)) {
                        functions$.subscribe(function (functions) {
                            languageAssist.setFunctions(functions);
                        }, function (errorResponse) {});

                        CodeMirror.commands.autocomplete = function (cm) {
                            if (isFunction(languageAssist.suggest)) {
                                CodeMirror.showHint(cm, languageAssist.suggest);
                            }
                        };

                        extraKeys['Ctrl-Space'] = 'autocomplete';
                    }

                    var editor = CodeMirror.fromTextArea(field.get(0), {
                        mode: languageId,
                        lineNumbers: true,
                        matchBrackets: true,
                        readOnly: readOnly,
                        extraKeys: extraKeys
                    });

                    // set the size
                    var width = null;
                    if (isDefinedAndNotNull(options.width)) {
                        width = options.width;
                    }
                    var height = null;
                    if (isDefinedAndNotNull(options.height)) {
                        height = options.height;
                    }
                    editor.setSize(width, height);

                    // store the editor instance for later
                    $(this).data('editorInstance', editor);

                    // get a reference to the codemirror
                    var codeMirror = $(this).find('.CodeMirror');

                    // reference the code portion
                    var code = codeMirror.find('.CodeMirror-code');

                    // make this resizable if specified
                    if (resizable) {
                        codeMirror.append('<div class="ui-resizable-handle ui-resizable-se"></div>').resizable({
                            handles: {
                                'se': '.ui-resizable-se'
                            },
                            resize: function () {
                                editor.setSize($(this).width(), $(this).height());
                                editor.refresh();
                            }
                        });
                    }

                    // handle keydown to signify the content has changed
                    editor.on('change', function (cm, event) {
                        codeMirror.addClass('modified');
                    });

                    // handle keyHandled to stop event propagation/default as necessary
                    editor.on('keyHandled', function (cm, name, evt) {
                        if (name === 'Esc') {
                            // stop propagation of the escape event
                            evt.stopImmediatePropagation();
                            evt.preventDefault();
                        }
                    });

                    // handle sensitive values differently
                    if (sensitive) {
                        code.addClass('sensitive');

                        var handleSensitive = function (cm, event) {
                            if (code.hasClass('sensitive')) {
                                code.removeClass('sensitive');
                                editor.setValue('');
                            }
                        };

                        // remove the sensitive style if necessary
                        editor.on('mousedown', handleSensitive);
                        editor.on('keydown', handleSensitive);
                    }

                    // set the min width/height
                    if (isDefinedAndNotNull(options.minWidth)) {
                        codeMirror.resizable('option', 'minWidth', options.minWidth);
                    }
                    if (isDefinedAndNotNull(options.minHeight)) {
                        codeMirror.resizable('option', 'minHeight', options.minHeight);
                    }
                }
            });
        },

        /**
         * Refreshes the editor.
         */
        refresh: function () {
            return this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    editor.refresh();
                }
            });
        },

        /**
         * Sets the size of the editor.
         *
         * @param {integer} width
         * @param {integer} height
         */
        setSize: function (width, height) {
            return this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    editor.setSize(width, height);
                }
            });
        },

        /**
         * Gets the value of the editor in the first matching selector.
         */
        getValue: function () {
            var value;

            this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    value = editor.getValue();
                }

                return false;
            });

            return value;
        },

        /**
         * Sets the value of the editor.
         *
         * @param {string} value
         */
        setValue: function (value) {
            return this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    editor.setValue(value);

                    // remove the modified marking since the value was reset
                    $(this).find('.CodeMirror').removeClass('modified');
                }
            });
        },

        /**
         * Sets the focus.
         */
        focus: function () {
            return this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    editor.focus();
                }
            });
        },

        /**
         * Sets the focus.
         */
        selectAll: function () {
            return this.each(function () {
                var editor = $(this).data('editorInstance');

                // ensure the editor was initialized
                if (isDefinedAndNotNull(editor)) {
                    editor.execCommand('selectAll');
                }
            });
        },

        /**
         * Gets whether the value of the editor in the first matching selector has been modified.
         */
        isModified: function () {
            var modified;

            this.each(function () {
                modified = $(this).find('.CodeMirror').hasClass('modified');
                return false;
            });

            return modified;
        },

        /**
         * Destroys the editor.
         */
        destroy: function () {
            return this.removeData('editorInstance').find('.CodeMirror').removeClass('modified');
        }
    };

    $.fn.nfeditor = function (method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else {
            return methods.init.apply(this, arguments);
        }
    };
})();