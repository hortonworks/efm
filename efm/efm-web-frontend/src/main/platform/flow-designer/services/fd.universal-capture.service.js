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
 * UniversalCaptureService constructor.
 * @param ngZone                    The ngZone service.
 * @constructor
 */
function UniversalCaptureService(ngZone) {
    var self = this;

    /**
     * Removes all read only property detail dialogs.
     */
    this.removeAllPropertyDetailDialogs = function () {
        ngZone.runOutsideAngular(function () {
            var propertyDetails = $('body').find('div.property-detail');
            propertyDetails.find('div.nfel-editor').nfeditor('destroy'); // look for any nfel editors
            propertyDetails.find('div.value-combo').combo('destroy'); // look for any combos
            propertyDetails.hide().remove();
        });
    };

    /**
     * Captures keydown on the window to ensure certain keystrokes are handled in a consistent manner, particularly those
     * that can lead to browser navigation/reload.
     */
    ngZone.runOutsideAngular(function () {
        $(document).ready(function ($) {
            // setup a listener to ensure keystrokes are being overridden in a consistent manner
            $(window).on('keydown', function (evt) {
                // consider escape, before checking dialogs
                var isCtrl = evt.ctrlKey || evt.metaKey;
                if (!isCtrl && evt.keyCode === 27) {
                    // esc

                    // prevent escape when editing a property with allowable values - that component does not handle key
                    // events so it can bubble up to here. once here we are unable to cancel the current edit so we simply
                    // return. this is not an issue for viewing in read only mode as the table is not in an edit mode. this
                    // is not an issue for other fields as they can handle key events locally and cancel the edit appropriately
                    var visibleCombo = $('div.value-combo');
                    if (visibleCombo.is(':visible') && visibleCombo.parent().hasClass('combo-editor')) {
                        return;
                    }

                    // consider property detail dialogs
                    if ($('div.property-detail').is(':visible')) {
                        self.removeAllPropertyDetailDialogs();

                        // prevent further bubbling as we're already handled it
                        evt.stopImmediatePropagation();
                        evt.preventDefault();
                    } else {
                        var target = $(evt.target);
                        if (target.length) {
                            // special handling for body as the target
                            var cancellables = $('.cancellable');
                            if (cancellables.length) {
                                var zIndexMax = null;
                                var dialogMax = null;

                                // identify the top most cancellable
                                $.each(cancellables, function (_, cancellable) {
                                    var dialog = $(cancellable);
                                    var zIndex = dialog.css('zIndex');

                                    // if the dialog has a zIndex consider it
                                    if (dialog.is(':visible') && (zIndex !== null && typeof zIndex !== 'undefined')) {
                                        zIndex = parseInt(zIndex, 10);
                                        if (zIndexMax === null || zIndex > zIndexMax) {
                                            zIndexMax = zIndex;
                                            dialogMax = dialog;
                                        }
                                    }
                                });

                                // if we've identified a dialog to close do so and stop propagation
                                if (dialogMax !== null) {
                                    // hide the cancellable
                                    if (dialogMax.hasClass('modal')) {
                                        dialogMax.modal('hide');
                                    } else {
                                        dialogMax.hide();
                                    }

                                    // prevent further bubbling as we're already handled it
                                    evt.stopImmediatePropagation();
                                    evt.preventDefault();

                                    return;
                                }
                            }
                        }
                    }
                } else {
                    if (isCtrl) {
                        if (evt.keyCode === 82) {
                            // ctrl-r
                            evt.preventDefault();
                        }
                    } else {
                        if (!$('input, textarea').is(':focus') && (evt.keyCode == 8 || evt.keyCode === 46)) {
                            // backspace or delete
                            evt.preventDefault();
                        }
                    }
                }
            });
        });
    });
};

// Public
UniversalCaptureService.prototype = {
    constructor: UniversalCaptureService
};

UniversalCaptureService.parameters = [
    ngCore.NgZone
];

module.exports = UniversalCaptureService;
