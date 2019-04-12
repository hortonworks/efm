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

var EventsConstants = require('components/monitor/events/efm.monitor-events.constants.js');
var TIME_RANGES = EventsConstants.TIME_RANGES;

var deepEqual = function (x, y) {
    var isXObject = x && typeof x === 'object';
    var isYObject = y && typeof y === 'object';
    if (isXObject && isYObject) {
        var keyCountEqual = Object.keys(x).length === Object.keys(y).length;
        var fieldsEqual = Object.keys(x).reduce(function (isEqual, key) {
            return isEqual && deepEqual(x[key], y[key]);
        }, true);
        return keyCountEqual && fieldsEqual;
    }

    return (x === y);
};

var isExistingSearch = function (searchCriteria, newCriteria) {
    return searchCriteria.some(function (criteria) {
        return criteria.filter === newCriteria.filter;
    });
};

var isValidNumber = function (value) {
    return !isNaN(parseInt(value, 10));
};

var isFilterInvalid = function (filter, validFilters) {
    var isFilterFieldValid;
    var filterValue;
    var filterField;
    var filterOperator;
    var filterText;
    var filterArray = [].concat(filter);
    var filterItem;

    var isExistingField = function (column) {
        return column.name === filterField;
    };

    var splitFilter = function (filterItem) {
        var filterParts = filterItem.split(':');

        if (filterParts.length > 3) {
            var filterPartsModified = [filterParts[0], filterParts[1]];
            filterPartsModified.push(filterParts.slice(2).join(':'));
            return filterPartsModified;
        }

        return filterParts;
    };

    for (var i = 0; i < filterArray.length; i++) {
        filterItem = filterArray[i];
        filterValue = splitFilter(filterItem);
        if (filterValue.length !== 3) {
            return 'Filter expression must be in the form field:operator:value.';
        }

        filterField = filterValue[0];
        isFilterFieldValid = validFilters.some(isExistingField);
        if (!isFilterFieldValid) {
            return 'Invalid filter column.';
        }

        filterOperator = filterValue[1];
        if (filterOperator.length === 0) {
            return 'Filter operator cannot be blank.';
        }

        filterText = filterValue[2];
        if (!filterText) {
            return 'Filter value cannot be blank.';
        }
    }

    return false;
};

var getColumnIndexByName = function (eventColumns, name) {
    var columnIndex = -1;

    for (var i = 0; i < eventColumns.length; ++i) {
        if (eventColumns[i].name === name) {
            columnIndex = i;
            break;
        }
    }

    return columnIndex;
};

var getFilterByTimeRange = function (timeRange) {
    var now = new Date();

    switch (timeRange) {
        case TIME_RANGES.LAST_HOUR:
            return 'created:gte:' + now.setHours(now.getHours() - 1);
        case TIME_RANGES.LAST_4_HOURS:
            return 'created:gte:' + now.setHours(now.getHours() - 4);
        case TIME_RANGES.LAST_24_HOURS:
            return 'created:gte:' + now.setDate(now.getDate() - 1);
        case TIME_RANGES.LAST_7_DAYS:
            return 'created:gte:' + now.setDate(now.getDate() - 7);
        default:
            return '';
    }
};

var getSearchFilters = function (searchCriteria, timeRangeFilter, activeTimeRange) {
    var filters = searchCriteria.map(function (criteria) {
        return criteria.filter;
    });

    if (timeRangeFilter && activeTimeRange !== 'All') {
        filters.push(timeRangeFilter);
    }

    return filters;
};

module.exports = {
    deepEqual: deepEqual,
    isExistingSearch: isExistingSearch,
    isValidNumber: isValidNumber,
    isFilterInvalid: isFilterInvalid,
    getColumnIndexByName: getColumnIndexByName,
    getFilterByTimeRange: getFilterByTimeRange,
    getSearchFilters: getSearchFilters
};
