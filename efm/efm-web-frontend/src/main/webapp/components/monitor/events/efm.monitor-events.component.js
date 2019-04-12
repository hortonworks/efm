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
var ngRouter = require('@angular/router');
var ngCommon = require('@angular/common');

var EfmService = require('services/efm.service.js');
var DialogService = require('services/efm.dialog.service.js');
var EfmApi = require('services/efm.api.service.js');

var eventsConstants = require('components/monitor/events/efm.monitor-events.constants.js');
var eventsUtils = require('components/monitor/events/efm.monitor-events.utils.js');

var CommonService = require('@flow-designer/services/CommonService');

var TIME_RANGES = eventsConstants.TIME_RANGES;
var EVENT_COLUMNS = eventsConstants.EVENT_COLUMNS;
var AUTO_REFRESH_SECONDS = eventsConstants.AUTO_REFRESH_SECONDS;

/**
 * EfmMonitorEvents constructor.
 *
 * @param efmService            The EFM service module.
 * @param dialogService         The dialog service module.
 * @param efmApi                The EFM api.
 * @param activatedRoute        The angular activated route module.
 * @param location              The angular location service.
 * @param common                The common service.
 * @constructor
 */
function EfmMonitorEvents(efmService, dialogService, efmApi, activatedRoute, location, common) {
    var self = this;
    var dialogRef = null;

    // data table
    this.eventColumns = EVENT_COLUMNS;

    // events
    this.events = [];
    this.eventCount = 0;
    this.eventsSubscription = null;
    this.selectedEvent = null;
    this.selectedEventId = null;
    this.eventDetailsOpen = false;
    this.isLoading = false;

    // filter
    this.timeRanges = eventsConstants.TIME_RANGE_VALUES;
    this.activeTimeRange = this.timeRanges[0].label;
    this.rows = eventsConstants.ROWS;
    this.rowsCount = this.rows[0].label;
    this.activeColumnIndex = 0;
    this.sortOrder = 'DESC';
    this.pageNumber = '1';

    // search criteria
    this.searchField = this.eventColumns[0];
    this.searchCriteria = [];

    // services
    this.route = activatedRoute;
    this.efmApi = efmApi;
    this.efmService = efmService;
    this.location = location;

    // new events
    var newEventsUrlPath = null;
    var newEventsPollingId = null;
    this.newEventCount = 0;

    this.efmService.perspective = 'events';
    this.efmService.monitorPerspective = 'events';

    this.eventsFilters = {
        pageNum: this.pageNumber,
        rows: this.rowsCount,
        timeRange: TIME_RANGES.ALL,
        sort: this.eventColumns[6].name + ':' + this.sortOrder
    };

    var resetEvents = function () {
        self.isLoading = false;
        self.events = [];
        self.eventCount = 0;
        self.newEventCount = 0;

        self.unselectEvent();
    };

    var addQueryParamsToUrl = function (params) {
        var queryParams = $.extend({}, self.eventsFilters, params);

        // Remove "filter" from the cache & URL if it's empty
        var filter = queryParams.filter;
        var isFilterArray = Array.isArray(filter);
        var isEmptyArray = isFilterArray && filter.length === 0;
        var isEmptyString = isFilterArray && filter.length === 1 && filter[0] === '';
        if (isEmptyArray || isEmptyString) {
            delete self.eventsFilters.filter;
            queryParams.filter = null;
        }

        self.efmService.routeToUrl([], {
            relativeTo: self.route,
            queryParams: queryParams,
            queryParamsHandling: 'merge'
        });
    };

    var setActiveColumn = function (sortFilter) {
        var sort = sortFilter.split(':');
        var activeColumnIndex = eventsUtils.getColumnIndexByName(self.eventColumns, sort[0]);
        var sortOrder = sort[1];

        // only one column can be actively sorted so we reset all to inactive
        self.eventColumns.forEach(function (c) {
            c.active = false;
        });

        self.eventColumns[activeColumnIndex].active = true;
        self.activeColumn = self.eventColumns[activeColumnIndex];
        self.activeColumn.sortOrder = sortOrder;
    };

    var setSearchCriteria = function (filterParams) {
        self.searchCriteria = [];
        var filters = [].concat(filterParams);
        var filterValue;
        var criteria;
        var columnIndex;
        filters.forEach(function (filter) {
            filterValue = filter.split(':');

            // Do NOT include timeRange filter in Search Criteria
            if (filterValue[0] !== 'created') {
                columnIndex = eventsUtils.getColumnIndexByName(self.eventColumns, filterValue[0]);
                criteria = {
                    field: filterValue[0],
                    label: self.eventColumns[columnIndex].label,
                    searchText: filterValue[2],
                    filter: filter
                };

                self.searchCriteria.push(criteria);
            }
        });
    };

    var setTimeRange = function (timeRange) {
        var activeTimeRange;
        self.timeRanges.forEach(function (tr) {
            if (!activeTimeRange && tr.value === timeRange) {
                activeTimeRange = tr;
            }
        });

        self.activeTimeRange = activeTimeRange.label;
    };

    var openErrorDialog = function (title, message) {
        dialogRef = dialogService.openConfirm({
            title: title,
            message: message
        });

        resetEvents();

        return false;
    };

    var validateQueryParams = function (params) {
        if (params.hasOwnProperty('pageNum')) {
            var pageNum = params.pageNum;
            var isPageNumValid = eventsUtils.isValidNumber(pageNum) && pageNum > 0;
            if (!isPageNumValid) {
                return openErrorDialog('Page Number Not Available', 'The specified page number does not exist or may not be valid. Please check that it is a valid integer, greater than \'0\'.');
            }
        }

        if (params.hasOwnProperty('rows')) {
            var rows = params.rows;
            var isRowsValid = eventsUtils.isValidNumber(rows) && rows > 0 && self.rows.some(function (r) { return r.value === rows; });
            if (!isRowsValid) {
                return openErrorDialog('Rows Per Page Number Not Valid', 'The specified rows per page number is not valid. Please choose a defined value as shown in the user interface.');
            }
        }

        if (params.hasOwnProperty('filter')) {
            var filter = params.filter;
            var filterErrorMessage = eventsUtils.isFilterInvalid(filter, self.eventColumns);
            if (filterErrorMessage) {
                return openErrorDialog('Filter Not Valid', filterErrorMessage);
            }
        }

        if (params.hasOwnProperty('sort')) {
            var sortFilter = params.sort;
            var sort = sortFilter.split(':');
            var sortField = sort[0];
            var isSortFieldValid = self.eventColumns.some(function (column) { return column.name === sortField; });
            var sortOrder = sort[1];
            var isSortOrderValid = ['ASC', 'DESC'].some(function (order) { return order === sortOrder; });
            if (!isSortFieldValid || !isSortOrderValid) {
                return openErrorDialog('Unable to Sort Column', 'The specified sorting method is not recognized. Please use ascending (\'ASC\') or descending (\'DESC\')');
            }
        }

        if (params.hasOwnProperty('timeRange')) {
            var timeRange = params.timeRange;
            var isTimeRangeValid = self.timeRanges.some(function (tr) { return tr.value === timeRange; });
            if (!isTimeRangeValid) {
                return openErrorDialog('Time Range Not Valid', 'The specified time range is not valid. Please choose a defined range as shown in the user interface.');
            }
        }

        return true;
    };

    var loadEvents = function (params) {
        // do not load Events if the Query Params have not changed
        if (self.events.length !== 0 && eventsUtils.deepEqual(self.eventsFilters, params)) {
            return;
        }

        self.isLoading = true;

        var isParamsValid = validateQueryParams(params);
        if (!isParamsValid) {
            self.isLoading = false;
            return;
        }

        // cache the events filters
        self.eventsFilters = $.extend({}, self.eventsFilters, params);

        var rowsCount = self.eventsFilters.rows;
        if (rowsCount && rowsCount !== self.rowsCount) {
            self.rowsCount = rowsCount;
        }

        var pageNum = self.eventsFilters.pageNum;
        if (pageNum && pageNum !== self.pageNumber) {
            self.pageNumber = pageNum;
        }

        if (self.eventsFilters.timeRange) {
            setTimeRange(self.eventsFilters.timeRange);
        }

        if (self.eventsFilters.sort) {
            setActiveColumn(self.eventsFilters.sort);
        }

        if (self.eventsFilters.filter) {
            setSearchCriteria(self.eventsFilters.filter);
        }

        if (self.eventsSubscription) {
            // cancel any incomplete http requests
            self.eventsSubscription.unsubscribe();
        }
        self.eventsSubscription = self.efmApi.getEvents(self.eventsFilters)
            .subscribe(function (response) {
                self.eventCount = response.totalRows;
                self.events = response.events;
                self.isLoading = false;

                self.unselectEvent();

                // capture the new events url
                newEventsUrlPath = response.newEventsUrlPath;

                // stop previously polling if necessary
                stopPolling();

                // clear the previously stored new event details
                self.newEventCount = 0;

                // start polling for new events
                startPolling();
            }, function (errorResponse) {
                resetEvents();
            });
    };

    /**
     * Starts polling for new events.
     */
    var startPolling = function () {
        newEventsPollingId = setTimeout(function () {
            poll(AUTO_REFRESH_SECONDS);
        }, AUTO_REFRESH_SECONDS * 1000);
    };

    /**
     * Polls for new events.
     *
     * @param autoRefreshInterval how long to wait to poll again
     */
    var poll = function (autoRefreshInterval) {
        efmApi.getEventsFromResponseLink(newEventsUrlPath).subscribe(function (response) {
            // identify if there are any new events
            self.newEventCount = response.totalRows;

            // polling for new events after auto refresh interval
            newEventsPollingId = setTimeout(function () {
                poll(autoRefreshInterval);
            }, autoRefreshInterval * 1000);
        }, function (errorResponse) {
        });
    };

    /**
     * Stops polling for new events.
     */
    var stopPolling = function () {
        if (newEventsPollingId !== null) {
            clearTimeout(newEventsPollingId);

            newEventsPollingId = null;
        }
    };

    /**
     * Initialize the component
     */
    this.init = function () {
        self.queryParamSubscription = this.route
            .queryParamMap
            .subscribe(function (queryParamMap) {
                var mergedQueryParams = $.extend({}, self.eventsFilters, queryParamMap.params);

                // remove filters if necessary
                if (!queryParamMap.params.filter) {
                    self.eventsFilters.filter = [];
                    delete mergedQueryParams.filter;
                }

                var queryParams = $.param(mergedQueryParams, true);
                self.location.replaceState('/monitor/events?' + queryParams);

                loadEvents(mergedQueryParams);

                // if the page num is specified ensure we navigate to that page through the pager
                if (eventsUtils.isValidNumber(mergedQueryParams['pageNum'])) {
                    self.pagingBar.navigateToPage(parseInt(mergedQueryParams['pageNum'], 10));
                }
            });
    };

    /**
     * Destroy the component
     */
    this.destroy = function () {
        if(common.isDefinedAndNotNull(dialogRef)) {
            dialogRef.close();
        }
        // stop checking for new events
        stopPolling();
        newEventsUrlPath = null;
        self.newEventCount = 0;

        self.efmService.monitorPerspective = '';
        self.queryParamSubscription.unsubscribe();
        self.eventsSubscription.unsubscribe();
    };

    /**
     * Updates the sorted column.
     *
     * @param column column to toggle
     */
    this.sortEvents = function (column) {
        if (column.sortable) {
            self.activeColumn = column;

            // toggle column sort order
            self.activeColumn.sortOrder = (column.sortOrder === 'ASC') ? 'DESC' : 'ASC';

            // only one column can be actively sorted so we reset all to inactive
            self.eventColumns.forEach(function (c) {
                c.active = false;
            });

            // and set this column as the actively sorted column
            self.activeColumn.active = true;

            // go to the first page and update the sort
            addQueryParamsToUrl({
                pageNum: 1,
                sort: self.activeColumn.name + ':' + self.activeColumn.sortOrder,
            });
        }
    };

    /**
     * Reloads events using the same search/sort criteria.
     */
    this.reloadEvents = function () {
        loadEvents({});
    };

    /**
     * Reloads events and resets the sort to show the latest.
     */
    this.showLatest = function () {
        // if we are already sorted correctly for latest just reload the events with the existing criteria, otherwise reset the sort
        if (self.activeColumn.name === 'created' && self.activeColumn.sortOrder === 'DESC') {
            // see if we're already on the first page
            var currentPage = parseInt(self.pageNumber, 10);
            if (currentPage === 1) {
                self.reloadEvents();
            } else {
                self.pagingBar.firstPage();
            }
        } else {
            // find the created column and ensure the new order will be DESC
            var createdColumn = self.eventColumns.find(function (column) {
                return column.name === 'created';
            });
            createdColumn.sortOrder = 'ASC';

            // update the sort criteria
            self.sortEvents(createdColumn);
        }
    };

    /**
     * Get the searchable columns.
     */
    this.getSearchableColumns = function () {
        return self.eventColumns.filter(function (col) {
            return col.searchable;
        });
    };

    /**
     * Updates the search criteria.
     *
     * @param searchText text to search by
     */
    this.updateSearchCriteria = function (searchText) {
        // dual bind will update searchCriteria with the search text already, however
        // we want to associate with the currently selected column so replace the
        // corresponding element in the array
        for (var i = 0; i < self.searchCriteria.length; i++) {
            if (self.searchCriteria[i] === searchText) {
                var filter = self.searchField.name + ':like:' + searchText;
                var newCriteria = {
                    field: self.searchField.name,
                    label: self.searchField.label,
                    searchText: searchText,
                    filter: filter
                };

                if (!eventsUtils.isExistingSearch(self.searchCriteria, newCriteria)) {
                    var filterErrorMessage = eventsUtils.isFilterInvalid(filter, self.eventColumns);
                    if (!filterErrorMessage) {
                        self.searchCriteria[i] = newCriteria;
                    } else {
                        // remove the invalid filter
                        // which is the last element in the array
                        self.searchCriteria.pop();

                        dialogRef = dialogService.openConfirm({
                            title: 'Filter Not Valid',
                            message: filterErrorMessage
                        });
                    }
                } else {
                    // remove the current search criteria
                    // which is the last element in the array
                    self.searchCriteria.pop();

                    dialogService.openCoaster({
                        title: 'Duplicate Filter',
                        message: 'The specified filter has already been applied.',
                        color: '#E98A40',
                        icon: 'fa fa-exclamation-triangle'
                    });

                    // prevent paging
                    return;
                }
            }
        }

        // update the filter and go to the first page
        addQueryParamsToUrl({
            pageNum: 1,
            filter: eventsUtils.getSearchFilters(self.searchCriteria, self.timeRangeFilter, self.activeTimeRange)
        });
    };

    /**
     * Removes an active criteria from the search criterias.
     *
     * @param activeCriteria the criteria to be removed
     */
    this.removeSearchCriteria = function (activeCriteria) {
        self.searchCriteria = self.searchCriteria.filter(function (criteria) {
            return criteria.filter !== activeCriteria.filter;
        });

        addQueryParamsToUrl({ filter: eventsUtils.getSearchFilters(self.searchCriteria, self.timeRangeFilter, self.activeTimeRange) });
    };

    /**
     * Handle a page event to reload the events table.
     *
     * @param pagingEvent page event
     */
    this.page = function (pagingEvent) {
        var currentPage = parseInt(self.pageNumber, 10);
        var currentPageSize = parseInt(self.rowsCount, 10);

        // only update the query params and trigger a reload when something actually changes
        if (currentPage !== pagingEvent.page || currentPageSize !== pagingEvent.pageSize) {
            addQueryParamsToUrl({
                pageNum: pagingEvent.page,
                rows: currentPageSize
            });
        }
    };

    /**
     * Handle the change event for the TimeRange dropdown
     *
     * @param timeRange new dropdown value
     */
    this.onTimeRangeChange = function (timeRange) {
        if (timeRange.value !== self.eventsFilters.timeRange) {
            self.activeTimeRange = timeRange.label;
            self.timeRangeFilter = eventsUtils.getFilterByTimeRange(timeRange.value);
            self.eventsFilters.filter = eventsUtils.getSearchFilters(self.searchCriteria, self.timeRangeFilter, self.activeTimeRange);

            addQueryParamsToUrl({
            	pageNum: 1,
            	timeRange: timeRange.value,
            	filter: self.eventsFilters.filter
            });
        }
    };

    /**
     * Handle the change event for the Rows dropdown
     *
     * @param row new dropdown value
     */
    this.onRowsChange = function (row) {
        if (row.value !== self.eventsFilters.rows) {
            self.rowsCount = row.label;
            addQueryParamsToUrl({ rows: row.value });
        }
    };

    /**
     * Show the details of an Event triggered by the Info icon
     *
     * @param event Event object
     */
    this.selectEvent = function (event) {
        self.eventDetailsOpen = true;
        self.selectedEvent = event;
        self.selectedEventId = event.id;
    };

    /**
     * Show the details of an Event triggered by any table row
     *
     * @param event Event object
     */
    this.selectEventByRow = function (event) {
        if (this.eventDetailsOpen) {
            if(common.isDefinedAndNotNull(event.links.detail)){
                this.selectEvent(event);
            } else {
                this.unselectEvent();
            }
        }
    };

    /**
     * Hide the details of an Event
     */
    this.unselectEvent = function () {
        self.eventDetailsOpen = false;
        self.selectedEvent = null;
        self.selectedEventId = null;
    };
}

EfmMonitorEvents.prototype = {
    constructor: EfmMonitorEvents,

    /**
     * Initialize the component
     */
    ngOnInit: function () {
        this.init();
    },

    /**
     * Destroy the component.
     */
    ngOnDestroy: function () {
        this.destroy();
    }
};

EfmMonitorEvents.annotations = [
    new ngCore.Component({
        template: require('./efm.monitor-events.component.html!text'),
        queries: {
            pagingBar: new ngCore.ViewChild('pagingBar')
        }
    })
];

EfmMonitorEvents.parameters = [
    EfmService,
    DialogService,
    EfmApi,
    ngRouter.ActivatedRoute,
    ngCommon.Location,
    CommonService
];

module.exports = EfmMonitorEvents;
