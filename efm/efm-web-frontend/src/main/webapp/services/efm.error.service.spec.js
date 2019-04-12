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

var ngCoreTesting = require('@angular/core/testing');

var ErrorService = require('services/efm.error.service.js');

describe('Platform Flow Designer Error Service', function () {
    var errorService;

    beforeEach(function () {
        ngCoreTesting.TestBed.configureTestingModule({
            providers: [
                ErrorService
            ]
        });

        errorService = ngCoreTesting.TestBed.get(ErrorService);
    });

    it('should create service', function () {
        expect(errorService).toBeDefined();
    });

    it('should test acceptable error codes', function () {
        expect(errorService.isClientError({ status: 400 })).toBeTruthy();
    });

    it('should test auth error codes', function () {
        expect(errorService.isAuthError({ status: 401 })).toBeTruthy();
        expect(errorService.isAuthError({ status: 403 })).toBeTruthy();
        expect(errorService.isAuthError({ status: 400 })).toBeFalsy();
    });

    it('should test unknown server error codes', function () {
        expect(errorService.isUnknownServerError({ status: 500 })).toBeTruthy();
        expect(errorService.isUnknownServerError({ status: 99 })).toBeTruthy();
        expect(errorService.isUnknownServerError({ status: 100 })).toBeFalsy();
    });
});
