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

var d3 = require('d3');
var rxjs = require('rxjs/Subject');
var ngCore = require('@angular/core');
var CommonService = require('@flow-designer/services/CommonService');

/**
 * ContextMenu constructor.
 *
 * @param common                        The flow designer common module.
 * @param ngZone                        The ngZone module.
 * @constructor
 */
function ContextMenu(commonService, ngZone) {
    var self = this;
    var canvas = null;

    this.subject$ = new rxjs.Subject();

    /**
     * Closes sub menu's starting from the specified menu.
     *
     * @param menu menu
     */
    var closeSubMenus = function (menu) {
        return ngZone.runOutsideAngular(function () {
            menu.remove().find('.group-menu-item').each(function () {
                var siblingGroupId = $(this).attr('id');
                closeSubMenus($('#' + siblingGroupId + '-sub-menu'));
            });
        });
    };

    /**
     * Adds a menu item to the context menu.
     *
     * {
     *      click: refresh (function),
     *      text: 'Start' (string),
     *      clazz: 'fa fa-refresh'
     * }
     *
     * @param {jQuery} contextMenu The context menu
     * @param {object} item The menu item configuration
     */
    var addMenuItem = function (contextMenu, item) {
        return ngZone.runOutsideAngular(function () {
            var menuItem = $('<button class="context-menu-item mat-menu-item"></button>').attr('id', item.id).on('mouseenter', function () {
                $(this).addClass('hover');

                contextMenu.find('.group-menu-item').not('#' + item.id).each(function () {
                    var siblingGroupId = $(this).attr('id');
                    closeSubMenus($('#' + siblingGroupId + '-sub-menu'));
                });
            }).on('mouseleave', function () {
                $(this).removeClass('hover');
            }).appendTo(contextMenu);

            // create the img and conditionally add the style
            var img = $('<div class="context-menu-item-img mat-icon"></div>').addClass(item['clazz']).appendTo(menuItem);
            if (commonService.isDefinedAndNotNull(item['imgStyle'])) {
                img.addClass(item['imgStyle']);
            }

            $('<div class="context-menu-item-text"></div>').text(item['text']).appendTo(menuItem);
            if (item.isGroup) {
                $('<div class="fa fa-caret-right context-menu-group-item-img"></div>').appendTo(menuItem);
            }

            return menuItem;
        });
    };

    /**
     * Positions and shows the context menu.
     *
     * @param {jQuery} contextMenu  The context menu
     * @param {object} options      The context menu configuration
     */
    var positionAndShow = function (contextMenu, options) {
        ngZone.runOutsideAngular(function () {
            contextMenu.css({
                'left': options.x + 'px',
                'top': options.y + 'px'
            }).show();
        });
    };

    /**
     * Executes the specified action with the optional selection.
     *
     * @param {string} action
     * @param {selection} selection
     * @param {mouse event} evt
     */
    var executeAction = function (action, selection, evt) {
        ngZone.runOutsideAngular(function () {
            ngZone.run(function () {
                // execute the action
                self.subject$.next({
                    action: action,
                    selection: selection,
                    evt: evt
                });
            });

            // close the context menu
            self.hide();
        });
    };

    var menuItems = [];

    /**
     * Initializes the selectable behavior.
     *
     * @param canvasRef                                     Canvas reference
     */
    this.init = function (canvasRef) {
        canvas = canvasRef;

        ngZone.runOutsideAngular(function () {
            $(canvas.contextMenu).on('contextmenu', function (evt) {
                // stop propagation and prevent default
                evt.preventDefault();
                evt.stopPropagation();
            });
        });
    };

    /**
     * Shows the context menu.
     */
    this.show = function () {
        ngZone.runOutsideAngular(function () {
            // hide the menu if currently visible
            self.hide();

            var canvasBody = canvas.getCanvasContainerElement();
            var contextMenu = $(canvas.getContextMenuElement()).empty();

            // get the current selection
            var selection = canvas.getSelection();

            // get the location for the context menu
            var position = d3.mouse(canvasBody);

            // determines if the specified menu positioned at x would overflow the available width
            var overflowRight = function (x, menu) {
                return x + menu.width() > canvasBody.clientWidth;
            };

            // determines if the specified menu positioned at y would overflow the available height
            var overflowBottom = function (y, menu) {
                return y + menu.height() > (canvasBody.clientHeight);
            };

            // adds a menu item
            var addItem = function (menu, id, item) {
                // add the menu item
                addMenuItem(menu, {
                    id: id,
                    clazz: item.clazz,
                    imgStyle: item.imgStyle,
                    text: item.text,
                    isGroup: false
                }).on('click', function (evt) {
                    executeAction(item.action, selection, evt);
                }).on('contextmenu', function (evt) {
                    executeAction(item.action, selection, evt);

                    // stop propagation and prevent default
                    evt.preventDefault();
                    evt.stopPropagation();
                });
            };

            // adds a group item
            var addGroupItem = function (menu, groupId, groupItem, applicableGroupItems) {
                // add the menu item
                addMenuItem(menu, {
                    id: groupId,
                    clazz: groupItem.clazz,
                    imgStyle: groupItem.imgStyle,
                    text: groupItem.text,
                    isGroup: true
                }).addClass('group-menu-item').on('mouseenter', function () {
                    // see if this submenu item is already open
                    if ($('#' + groupId + '-sub-menu').length == 0) {
                        var groupMenuItem = $(this);
                        var contextMenuPosition = menu.position();
                        var groupMenuItemPosition = groupMenuItem.position();

                        var x = contextMenuPosition.left + groupMenuItemPosition.left + groupMenuItem.width();
                        var y = contextMenuPosition.top + groupMenuItemPosition.top;

                        var subMenu = $('<div class="context-menu mat-menu-content mat-menu-panel unselectable sub-menu"></div>').attr('id', groupId + '-sub-menu').appendTo('body');

                        processMenuItems(subMenu, applicableGroupItems);

                        // make sure the sub menu is not hidden by the browser boundaries
                        if (overflowRight(x, subMenu)) {
                            x -= (subMenu.width() + groupMenuItem.width() - 4);
                        }
                        if (overflowBottom(y, subMenu)) {
                            y -= (subMenu.height() - groupMenuItem.height());
                        }

                        subMenu.css({
                            top: y + 'px',
                            left: x + 'px'
                        }).show();
                    }
                });
            };

            // whether or not a group item should be included
            var includeGroupItem = function (groupItem) {
                if (groupItem.separator) {
                    return true;
                } else if (groupItem.menuItem) {
                    return groupItem.condition(selection);
                } else {
                    var descendantItems = [];
                    $.each(groupItem.menuItems, function (_, descendantItem) {
                        if (includeGroupItem(descendantItem)) {
                            descendantItems.push(descendantItem);
                        }
                    });
                    return descendantItems.length > 0;
                }
            };

            // adds the specified items to the specified menu
            var processMenuItems = function (menu, items) {
                var allowSeparator = false;
                $.each(items, function (_, item) {
                    if (item.separator && allowSeparator) {
                        $('<div class="context-menu-item-separator mat-divider"></div>').appendTo(menu);
                        allowSeparator = false;
                    } else {
                        if (processMenuItem(menu, item)) {
                            allowSeparator = true;
                        }
                    }
                });

                // ensure the last child isn't a separator
                var last = menu.children().last();
                if (last.hasClass('context-menu-item-separator mat-divider')) {
                    last.remove();
                }
            };

            // adds the specified item to the specified menu if the conditions are met, returns if the item was added
            var processMenuItem = function (menu, i) {
                var included = false;

                if (i.menuItem) {
                    included = i.condition(selection);
                    if (included) {
                        addItem(menu, i.id, i.menuItem);
                    }
                } else if (i.groupMenuItem) {
                    var applicableGroupItems = [];
                    $.each(i.menuItems, function (_, groupItem) {
                        if (includeGroupItem(groupItem)) {
                            applicableGroupItems.push(groupItem);
                        }
                    });

                    // ensure the included menu items includes more than just separators
                    var includedMenuItems = $.grep(applicableGroupItems, function (gi) {
                        return commonService.isUndefinedOrNull(gi.separator);
                    });
                    included = includedMenuItems.length > 0;
                    if (included) {
                        addGroupItem(menu, i.id, i.groupMenuItem, applicableGroupItems);
                    }
                }

                return included;
            };

            // consider each component action for the current selection
            processMenuItems(contextMenu, menuItems);

            // make sure the context menu is not hidden by the browser boundaries
            if (overflowRight(position[0], contextMenu)) {
                position[0] = canvasBody.clientWidth - contextMenu.width() - 2;
            }
            if (overflowBottom(position[1], contextMenu)) {
                position[1] = canvasBody.clientHeight - contextMenu.height() - 9;
            }

            // show the context menu
            positionAndShow(contextMenu, {
                'x': position[0],
                'y': position[1]
            });
        });
    };

    /**
     * Register a context menu item.
     *
     * @param menuItem
     */
    this.registerMenuItem = function (menuItem) {
        menuItems.push(menuItem);
    };

    /**
     * Hides the context menu and it's sub menus.
     */
    this.hide = function () {
        ngZone.runOutsideAngular(function () {
            $(canvas.getContextMenuElement()).hide();
            $(canvas.getContextMenuElement()).find('div.context-menu.sub-menu').remove();
        });
    };

    /**
     * Activates the context menu for the components in the specified selection.
     *
     * @param {selection} components    The components to enable the context menu for
     */
    this.activate = function (components) {
        ngZone.runOutsideAngular(function () {
            components.on('contextmenu.selection', function () {
                // get the clicked component to update selection
                self.show();

                // stop propagation and prevent default
                d3.event.preventDefault();
                d3.event.stopPropagation();
            });
        });
    };
};

ContextMenu.prototype = {
    constructor: ContextMenu
};

ContextMenu.parameters = [
    CommonService,
    ngCore.NgZone
];

module.exports = ContextMenu;
