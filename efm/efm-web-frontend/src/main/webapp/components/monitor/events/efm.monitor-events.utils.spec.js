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

var eventsUtils = require('components/monitor/events/efm.monitor-events.utils.js');

describe('EFM monitor events utils', function () {
    describe('isFilterInvalid', function () {
        var validFilters = [{ name: 'level' }, { name: 'severity' }, { name: 'source'}];

        it('should return error message for invalid filter expression', function () {
            expect(eventsUtils.isFilterInvalid('field', validFilters)).toBe('Filter expression must be in the form field:operator:value.');
            expect(eventsUtils.isFilterInvalid('field:like', validFilters)).toBe('Filter expression must be in the form field:operator:value.');
        });

        it('should return false for existing column', function () {
            expect(eventsUtils.isFilterInvalid('level:like:INFO', validFilters)).toBeFalsy();
        });

        it('should return error message for non existing column', function () {
            expect(eventsUtils.isFilterInvalid('invalidColumn:like:INFO', validFilters)).toBe('Invalid filter column.');
        });

        it('should return error message for empty filter operator', function () {
            expect(eventsUtils.isFilterInvalid('level::WARN', validFilters)).toBe('Filter operator cannot be blank.');
        });

        it('should return error message for empty filter value', function () {
            expect(eventsUtils.isFilterInvalid('level:like:', validFilters)).toBe('Filter value cannot be blank.');
        });

        it('should return error message for incorrect number of tokens', function () {
            expect(eventsUtils.isFilterInvalid('level:like', validFilters)).toBe('Filter expression must be in the form field:operator:value.');
        });

        it('should return falsy for valid filters', function () {
            expect(eventsUtils.isFilterInvalid('level:like:WARN', validFilters)).toBeFalsy();
        });

        it('should return falsy when searching for value containing :', function () {
            expect(eventsUtils.isFilterInvalid('source:like:http://localhost', validFilters)).toBeFalsy();
        });

        it('should return falsy when searching for value starting with :', function () {
            expect(eventsUtils.isFilterInvalid('source:like:://localhost', validFilters)).toBeFalsy();
        });

        it('should return falsy when searching for value ending with :', function () {
            expect(eventsUtils.isFilterInvalid('source:like:http//localhost:', validFilters)).toBeFalsy();
        });
    });
});
