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

var EfmService = require('services/efm.service.js');

var rxjs = require('rxjs/Observable');
var rxjsSubject = require('rxjs/Subject');

    /**
     * EfmHttpInterceptor constructor.
     *
     * @param efmService                                The efm service
     * @param EfmHttpErrorResponse                      The EfmHttpErrorResponse.
     * @param ErrorResponse                             The flow designer ErrorResponse.
     * @constructor
     */
function EfmHttpInterceptorService(efmService, EfmHttpErrorResponse, ErrorResponse) {
    var httpRequestLoading$ = new rxjsSubject.Subject();
    var httpRequestLoadingObservable = httpRequestLoading$.asObservable();

    var startLoading = function () {
        httpRequestLoading$.next(true);
    };

    var stopLoading = function () {
        // using setTimeout to trigger the loading animation
        setTimeout(function () {
            httpRequestLoading$.next(false);
        }, 500);
    };

    this.httpRequestLoadingObservable = function () {
         return httpRequestLoadingObservable;
    };

    this.intercept = function (req, next) {
        startLoading();

        return next.handle(req)
            .do(function (response) {
                stopLoading();

                return response;
            })
            .catch(function (httpErrorResponse) {
                var isClientError = false;
                var preventDefault = false;
                var efmHttpErrorResponse = new EfmHttpErrorResponse(httpErrorResponse);

                if (efmHttpErrorResponse.isAuthError) {
                    preventDefault = true;
                    efmService.routeToUrl(['/auth-error']);
                } else if (efmHttpErrorResponse.isUnknownServerError) {
                    preventDefault = true;
                    efmService.routeToUrl(['/error']);
                } else if (efmHttpErrorResponse.isClientError) {
                    isClientError = true;
                }

                var errorResponse = new ErrorResponse(preventDefault, isClientError, efmHttpErrorResponse.error);
                return rxjs.Observable.throw(errorResponse);
            });
    };
}

EfmHttpInterceptorService.prototype = {
    constructor: EfmHttpInterceptorService
};

EfmHttpInterceptorService.parameters = [
    EfmService,
    'EfmHttpErrorResponse',
    'ErrorResponse'
];

module.exports = EfmHttpInterceptorService;