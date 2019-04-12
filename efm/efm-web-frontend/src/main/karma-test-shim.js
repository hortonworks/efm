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

// /*global jasmine, __karma__, window*/
Error.stackTraceLimit = Infinity;

// The default time that jasmine waits for an asynchronous test to finish is five seconds.
// If this timeout is too short the CI may fail randomly because our asynchronous tests can
// take longer in some situations
jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;

__karma__.loaded = function () {
};

var specFiles = Object.keys(window.__karma__.files).filter(isSpecFile);

System.config({
    // Base URL for System.js calls. 'base/' is where Karma serves files from.
    baseURL: 'base',

    // Map the angular testing umd bundles
    map: {
        '@angular/core': 'npm:@angular/core/bundles/core.umd.js',
        '@angular/core/testing': 'npm:@angular/core/bundles/core-testing.umd.js',
        '@angular/compiler/testing': 'npm:@angular/compiler/bundles/compiler-testing.umd.js',
        '@angular/platform-browser/testing': 'npm:@angular/platform-browser/bundles/platform-browser-testing.umd.js',
        '@angular/platform-browser-dynamic/testing': 'npm:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic-testing.umd.js'
    }
});

System.import('systemjs.config.js')
    .then(initTestBed)
    .then(runSpecs)
    .then(__karma__.start, function(error) {
        // Passing in the error object directly to Karma won't log out the stack trace and
        // passing the `originalErr` doesn't work correctly either. We have to log out the
        // stack trace so we can actually debug errors before the tests have started.
        console.error(error.originalErr.stack);
        __karma__.error(error);
    });

/** Runs the specs in Karma. */
function runSpecs() {
    // By importing all spec files, Karma will run the tests directly.
    return Promise.all(specFiles.map(function(fileName) {
        return System.import(fileName);
    }));
}

/** Whether the specified file is part of EFM. */
function isSpecFile(path) {
    return path.slice(-8) === '.spec.js' && path.indexOf('node_modules') === -1;
}

function initTestBed() {
    return Promise.all([
        System.import('@angular/core'),
        System.import('@angular/core/testing'),
        System.import('@angular/platform-browser-dynamic/testing')
    ])

        .then(function (providers) {
            var core = providers[0];
            var coreTesting = providers[1];
            var browserTesting = providers[2];

            console.log('Running tests using Angular version: ' + core.VERSION.full);

            var testBed = coreTesting.TestBed.initTestEnvironment(
                browserTesting.BrowserDynamicTestingModule,
                browserTesting.platformBrowserDynamicTesting());

            patchTestBedToDestroyFixturesAfterEveryTest(testBed);
        })
}

/**
 * Monkey-patches TestBed.resetTestingModule such that any errors that occur during component
 * destruction are thrown instead of silently logged. Also runs TestBed.resetTestingModule after
 * each unit test.
 *
 * Without this patch, the combination of two behaviors is problematic:
 * - TestBed.resetTestingModule catches errors thrown on fixture destruction and logs them without
 *     the errors ever being thrown. This means that any component errors that occur in ngOnDestroy
 *     can encounter errors silently and still pass unit tests.
 * - TestBed.resetTestingModule is only called *before* a test is run, meaning that even *if* the
 *    aforementioned errors were thrown, they would be reported for the wrong test (the test that's
 *    about to start, not the test that just finished).
 */
function patchTestBedToDestroyFixturesAfterEveryTest(testBed) {
    // Original resetTestingModule function of the TestBed.
    var _resetTestingModule = testBed.resetTestingModule;

    // Monkey-patch the resetTestingModule to destroy fixtures outside of a try/catch block.
    // With https://github.com/angular/angular/commit/2c5a67134198a090a24f6671dcdb7b102fea6eba
    // errors when destroying components are no longer causing Jasmine to fail.
    testBed.resetTestingModule = function() {
        try {
            this._activeFixtures.forEach(function (fixture) { fixture.destroy(); });
        } finally {
            this._activeFixtures = [];
            // Regardless of errors or not, run the original reset testing module function.
            _resetTestingModule.call(this);
        }
    };

    // Angular's testing package resets the testing module before each test. This doesn't work well
    // for us because it doesn't allow developers to see what test actually failed.
    // Fixing this by resetting the testing module after each test.
    // https://github.com/angular/angular/blob/master/packages/core/testing/src/before_each.ts#L25
    afterEach(function() {
        testBed.resetTestingModule();
    });
}

