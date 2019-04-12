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
var ngCore = require('@angular/core');

/**
 * CommonService constructor.
 * @param ngZone                    The ngZone service.
 * @constructor
 */
function CommonService(ngZone) {
    var self = this;
    this.config = {
        sensitiveText: 'Sensitive value set',
        tooltipConfig: {
            style: {
                classes: 'fds-tooltip'
            },
            show: {
                solo: true,
                effect: function (offset) {
                    $(this).slideDown(100);
                }
            },
            hide: {
                effect: function (offset) {
                    $(this).slideUp(100);
                }
            },
            position: {
                at: 'top center',
                my: 'bottom center'
            }
        }
    };

    /**
     * Determines if the specified object is defined and not null.
     *
     * @argument {object} obj   The object to test
     */
    this.isDefinedAndNotNull = function (obj) {
        return ngZone.runOutsideAngular(function () {
            return !self.isUndefined(obj) && !self.isNull(obj);
        });
    };

    /**
     * Determines if the specified object is undefined or null.
     *
     * @param {object} obj      The object to test
     */
    this.isUndefinedOrNull = function (obj) {
        return ngZone.runOutsideAngular(function () {
            return self.isUndefined(obj) || self.isNull(obj);
        });
    };

    /**
     * Determines if the specified object is undefined.
     *
     * @argument {object} obj   The object to test
     */
    this.isUndefined = function (obj) {
        return ngZone.runOutsideAngular(function () {
            return typeof obj === 'undefined';
        });
    };

    /**
     * Determines whether the specified string is blank (or null or undefined).
     *
     * @argument {string} str   The string to test
     */
    this.isBlank = function (str) {
        return ngZone.runOutsideAngular(function () {
            return self.isUndefined(str) || self.isNull(str) || $.trim(str) === '';
        });
    };

    /**
     * Determines if the specified object is null.
     *
     * @argument {object} obj   The object to test
     */
    this.isNull = function (obj) {
        return ngZone.runOutsideAngular(function () {
            return obj === null;
        });
    };

    /**
     * Determines if the specified array is empty. If the specified arg is not an
     * array, then true is returned.
     *
     * @argument {array} arr    The array to test
     */
    this.isEmpty = function (arr) {
        return ngZone.runOutsideAngular(function () {
            return $.isArray(arr) ? arr.length === 0 : true;
        });
    };

    /**
     * Determines the connectable type for the specified source component.
     *
     * @argument {object} d      The component
     */
    this.getConnectableTypeForSource = function (d) {
        var type;
        if (d.type === 'processor') {
            type = 'PROCESSOR';
        } else if (d.type === 'remote-process-group') {
            type = 'REMOTE_OUTPUT_PORT';
        } else if (d.type === 'funnel') {
            type = 'FUNNEL';
        }
        return type;
    };

    /**
     * Determines the component type for the specified source component.
     *
     * @param {string} sourceType        The source component type
     */
    this.getComponentTypeForSource = function (sourceType) {
        var type;
        if (sourceType === 'PROCESSOR') {
            type = 'processor';
        } else if (sourceType === 'REMOTE_OUTPUT_PORT') {
            type = 'remote-process-group';
        } else if (sourceType === 'FUNNEL') {
            type = 'funnel';
        }
        return type;
    };

    /**
     * Determines the connectable type for the specified destination component.
     *
     * @argument {object} d      The component
     */
    this.getConnectableTypeForDestination = function (d) {
        var type;
        if (d.type === 'processor') {
            type = 'PROCESSOR';
        } else if (d.type === 'remote-process-group') {
            type = 'REMOTE_INPUT_PORT';
        } else if (d.type === 'funnel') {
            type = 'FUNNEL';
        }
        return type;
    };

    /**
     * Determines the component type for the specified destination component.
     *
     * @param {string} destinationType        The destination component type
     */
    this.getComponentTypeForDestination = function (destinationType) {
        var type;
        if (destinationType === 'PROCESSOR') {
            type = 'processor';
        } else if (destinationType === 'REMOTE_INPUT_PORT') {
            type = 'remote-process-group';
        } else if (destinationType === 'FUNNEL') {
            type = 'funnel';
        }
        return type;
    };

    /**
     * Extracts the contents of the specified str after the last strToFind. If the
     * strToFind is not found or the last part of the str, an empty string is
     * returned.
     *
     * @argument {string} str       The full string
     * @argument {string} strToFind The substring to find
     */
    this.substringAfterLast = function (str, strToFind) {
        return ngZone.runOutsideAngular(function () {
            var result = '';
            var indexOfStrToFind = str.lastIndexOf(strToFind);
            if (indexOfStrToFind >= 0) {
                var indexAfterStrToFind = indexOfStrToFind + strToFind.length;
                if (indexAfterStrToFind < str.length) {
                    result = str.substr(indexAfterStrToFind);
                }
            }
            return result;
        });
    };

    /**
     * Extracts the contents of the specified str after the strToFind. If the
     * strToFind is not found or the last part of the str, an empty string is
     * returned.
     *
     * @argument {string} str       The full string
     * @argument {string} strToFind The substring to find
     */
    this.substringAfterFirst = function (str, strToFind) {
        return ngZone.runOutsideAngular(function () {
            var result = '';
            var indexOfStrToFind = str.indexOf(strToFind);
            if (indexOfStrToFind >= 0) {
                var indexAfterStrToFind = indexOfStrToFind + strToFind.length;
                if (indexAfterStrToFind < str.length) {
                    result = str.substr(indexAfterStrToFind);
                }
            }
            return result;
        });
    };

    /**
     * Extracts the contents of the specified str before the last strToFind. If the
     * strToFind is not found or the first part of the str, an empty string is
     * returned.
     *
     * @argument {string} str       The full string
     * @argument {string} strToFind The substring to find
     */
    this.substringBeforeLast = function (str, strToFind) {
        return ngZone.runOutsideAngular(function () {
            var result = '';
            var indexOfStrToFind = str.lastIndexOf(strToFind);
            if (indexOfStrToFind >= 0) {
                result = str.substr(0, indexOfStrToFind);
            }
            return result;
        });
    };

    /**
     * Extracts the contents of the specified str before the strToFind. If the
     * strToFind is not found or the first path of the str, an empty string is
     * returned.
     *
     * @argument {string} str       The full string
     * @argument {string} strToFind The substring to find
     */
    this.substringBeforeFirst = function (str, strToFind) {
        return ngZone.runOutsideAngular(function () {
            var result = '';
            var indexOfStrToFind = str.indexOf(strToFind);
            if (indexOfStrToFind >= 0) {
                result = str.substr(0, indexOfStrToFind);
            }
            return result;
        });
    };

    /**
     * Formats the specified array as an unordered list. If the array is not an
     * array, null is returned.
     *
     * @argument {array} array      The array to convert into an unordered list
     */
    this.formatUnorderedList = function (array) {
        return ngZone.runOutsideAngular(function () {
            if ($.isArray(array)) {
                var ul = $('<ul class="unordered"></ul>');
                $.each(array, function (_, item) {
                    var li = $('<li></li>').appendTo(ul);
                    if (item instanceof $) {
                        li.append(item);
                    } else {
                        li.text(item);
                    }
                });
                return ul;
            } else {
                return null;
            }
        });
    };

    /**
     * Determines if the specified property is required.
     *
     * @param {object} propertyDescriptor           The property descriptor
     */
    this.isRequiredProperty = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                return propertyDescriptor.required === true;
            } else {
                return false;
            }
        });
    };

    /**
     * Determines if the specified property is sensitive.
     *
     * @argument {object} propertyDescriptor        The property descriptor
     */
    this.isSensitiveProperty = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                return propertyDescriptor.sensitive === true;
            } else {
                return false;
            }
        });
    };

    /**
     * Determines if the specified property is required.
     *
     * @param {object} propertyDescriptor           The property descriptor
     */
    this.isDynamicProperty = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                return propertyDescriptor.dynamic === true;
            } else {
                return false;
            }
        });
    };

    /**
     * Returns whether the specified property supports EL.
     *
     * @param {object} propertyDescriptor           The property descriptor
     */
    this.supportsEl = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                return propertyDescriptor.expressionLanguageScope !== 'NONE';
            } else {
                return false;
            }
        });
    };

    /**
     * Gets the allowable values for the specified property.
     *
     * @argument {object} propertyDescriptor        The property descriptor
     */
    this.getAllowableValues = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                return propertyDescriptor.allowableValues;
            } else {
                return null;
            }
        });
    };

    /**
     * Formats the tooltip for the specified property.
     *
     * @param {object} propertyDescriptor      The property descriptor
     * @returns {string}
     */
    this.formatPropertyTooltip = function (propertyDescriptor) {
        return ngZone.runOutsideAngular(function () {
            var tipContent = [];

            // show the property description if applicable
            if (self.isDefinedAndNotNull(propertyDescriptor)) {
                if (!self.isBlank(propertyDescriptor.description)) {
                    tipContent.push(self.escapeHtml(propertyDescriptor.description));
                }

                if (!self.isBlank(propertyDescriptor.defaultValue)) {
                    tipContent.push('<b>Default value:</b> ' + self.escapeHtml(propertyDescriptor.defaultValue));
                }

                tipContent.push('<b>Expression language scope:</b> ' + self.escapeHtml(propertyDescriptor.expressionLanguageScopeDescription));

                if (!self.isBlank(propertyDescriptor.identifiesControllerService)) {
                    var formattedType = self.formatType({
                        'type': propertyDescriptor.identifiesControllerService,
                        'bundle': propertyDescriptor.identifiesControllerServiceBundle
                    });
                    var formattedBundle = self.formatBundle(propertyDescriptor.identifiesControllerServiceBundle);
                    tipContent.push('<b>Requires Service:</b> ' + self.escapeHtml(formattedType + ' from ' + formattedBundle));
                }
            }

            if (tipContent.length > 0) {
                return tipContent.join('<br/><br/>');
            } else {
                return null;
            }
        });
    };

    /**
     * Formats the class name of this component.
     *
     * @param dataContext component datum
     */
    this.formatClassName = function (dataContext) {
        return ngZone.runOutsideAngular(function () {
            return self.substringAfterLast(dataContext.type, '.');
        });
    };

    /**
     * Formats the type of this component.
     *
     * @param dataContext component datum
     */
    this.formatType = function (dataContext) {
        return ngZone.runOutsideAngular(function () {
            var typeString = self.formatClassName(dataContext);
            if (self.isDefinedAndNotNull(dataContext.bundle.version) && dataContext.bundle.version !== 'unversioned') {
                typeString += (' ' + dataContext.bundle.version);
            }
            return typeString;
        });
    };

    /**
     * Formats the bundle label.
     *
     * @param bundle
     */
    this.formatBundle = function (bundle) {
        return ngZone.runOutsideAngular(function () {
            var groupString = '';
            if (bundle.group !== 'default') {
                groupString = bundle.group + ' - ';
            }
            return groupString + bundle.artifact;
        });
    };

    /**
     * Cleans up any tooltips that have been created for the specified container.
     *
     * @param {jQuery} container
     * @param {string} tooltipTarget
     */
    this.cleanUpTooltips = function (container, tooltipTarget) {
        return ngZone.runOutsideAngular(function () {
            container.find(tooltipTarget).each(function () {
                var tip = $(this);
                if (tip.data('qtip')) {
                    var api = tip.qtip('api');
                    api.destroy(true);
                }
            });
        });
    };

    /**
     * HTML escapes the specified string. If the string is null
     * or undefined, an empty string is returned.
     *
     * @returns {string}
     */
    this.escapeHtml = (function () {
        return ngZone.runOutsideAngular(function () {
            var entityMap = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;',
                '/': '&#x2f;'
            };

            return function (string) {
                if (self.isDefinedAndNotNull(string)) {
                    return String(string).replace(/[&<>"'\/]/g, function (s) {
                        return entityMap[s];
                    });
                } else {
                    return '';
                }
            };
        });
    }());
};

CommonService.prototype = {
    constructor: CommonService
};

CommonService.parameters = [
    ngCore.NgZone
];

module.exports = CommonService;
