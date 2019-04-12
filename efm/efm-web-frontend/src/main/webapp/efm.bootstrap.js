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
require('qtip2');
require('jquery-ui-dist');
require('@flow-designer/jquery/tab');
require('@flow-designer/jquery/combo');
require('@flow-designer/jquery/nfel');
require('@flow-designer/jquery/nfeditor');
require('core-js');
require('zone.js');
require('hammerjs');

// patch Observable with appropriate methods
var rxjs = require('rxjs/Observable');
require('rxjs/add/operator/switchMap');
require('rxjs/add/operator/shareReplay');
require('rxjs/add/observable/empty');
require('rxjs/add/observable/throw');
require('rxjs/add/operator/map');
require('rxjs/add/operator/mergeMap');
require('rxjs/add/operator/concatMap');
require('rxjs/add/operator/catch');
require('rxjs/add/observable/of');
require('rxjs/add/operator/do');
require('rxjs/add/operator/takeUntil');
require('rxjs/add/observable/forkJoin');
require('rxjs/add/operator/finally');
require('rxjs/add/operator/distinctUntilChanged');

var EfmModule = require('efm.module.js');
var ngPlatformBrowserDynamic = require('@angular/platform-browser-dynamic');
var ngCore = require('@angular/core');

// rxjs Observable debugger;
var debuggerOn = false;

rxjs.Observable.prototype.debug = function (message) {
    return this.do(
        function (next) {
            if (debuggerOn) {
                console.log(message, next);
            }
        },
        function (err) {
            if (debuggerOn) {
                console.error('ERROR >>> ',message , err);
            }
        },
        function () {
            if (debuggerOn) {
                console.log('Completed.');
            }
        }
    );
};

// This is commented out when developing to assert for unidirectional data flow
ngCore.enableProdMode();

// Get the locale id from the global
var locale = navigator.language;

var providers = [];

// No locale or U.S. English: no translation providers so go ahead and bootstrap the app
if (!locale || locale === 'en-US') {
    ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(EfmModule, {providers: providers});
} else { //load the translation providers and bootstrap the module
    var translationFile = './locale/messages.' + locale + '.xlf';

    $.ajax({
        url: translationFile
    }).done(function (translations) {
        // add providers if translation file for locale is loaded
        if (translations) {
            providers.push({provide: ngCore.TRANSLATIONS, useValue: translations.documentElement.innerHTML});
            providers.push({provide: ngCore.TRANSLATIONS_FORMAT, useValue: 'xlf'});
            providers.push({provide: ngCore.LOCALE_ID, useValue: locale});
        }
        ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(EfmModule, {providers: providers});
    }).fail(function () {
        ngPlatformBrowserDynamic.platformBrowserDynamic().bootstrapModule(EfmModule, {providers: providers});
    });
}
