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

module.exports = function (config) {
    config.set({
        basePath: './',
        browserNoActivityTimeout: 300000, //default 10000
        browserDisconnectTimeout: 180000, // default 2000
        browserDisconnectTolerance: 3, // default 0
        captureTimeout: 180000,
        frameworks: ['jasmine'],
        customLaunchers: {
            Chrome_travis_ci: {
                base: 'ChromeHeadless',
                flags: ['--no-sandbox']
            }
        },
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-spec-reporter'),
            require('karma-coverage')
        ],
        files: [
            {pattern: 'node_modules/core-js/client/core.min.js', included: true, watched: false},
            {pattern: 'node_modules/tslib/tslib.js', included: true, watched: false},
            {pattern: 'node_modules/systemjs/dist/system.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/zone.min.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/proxy.min.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/sync-test.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/jasmine-patch.min.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/async-test.js', included: true, watched: false},
            {pattern: 'node_modules/zone.js/dist/fake-async-test.js', included: true, watched: false},
            {pattern: 'node_modules/hammerjs/hammer.min.js', included: true, watched: false},
            {pattern: 'node_modules/systemjs-plugin-text/text.js', included: false, watched: false},

            // Include all Angular dependencies
            {pattern: 'node_modules/@angular/**/*', included: false, watched: false},
            {pattern: 'node_modules/@covalent/**/*', included: false, watched: false},
            {pattern: 'node_modules/@nifi-fds/**/*', included: false, watched: false},
            {pattern: 'node_modules/rxjs/**/*', included: false, watched: false},
            {pattern: 'node_modules/roboto-fontface/**/*', included: false, watched: false},
            {pattern: 'node_modules/jquery/**/*', included: false, watched: false},
            {pattern: 'node_modules/reset.css/**/*', included: false, watched: false},
            {pattern: 'node_modules/qtip2/**/*', included: false, watched: false},
            {pattern: 'node_modules/jquery-ui-dist/**/*', included: false, watched: false},
            {pattern: 'node_modules/font-awesome/**/*', included: false, watched: false},
            {pattern: 'node_modules/d3/**/*', included: false, watched: false},
            {pattern: 'node_modules/d3-*/**/*', included: false, watched: false},
            {pattern: 'node_modules/slickgrid/**/*', included: false, watched: false},

            // SystemJS config
            {pattern: 'systemjs.config.js', included: false, watched: false},
            {pattern: 'karma-test-shim.js', included: true, watched: true},

            // Include the EFM styles
            {pattern: 'webapp/**/*.css', included: true, watched: true},

            // Include the EFM templates
            {pattern: 'webapp/components/**/*.html', included: true, watched: true, served: true},
            {pattern: 'webapp/efm.component.html', included: true, watched: true, served: true},
            {pattern: 'webapp/efm.host.component.html', included: true, watched: true, served: true},

            // Include the EFM images
            {pattern: 'webapp/**/*.svg', included: true, watched: true, served: true},

            // Include the platform flow-designer styles
            {pattern: 'platform/flow-designer/**/*.css', included: true, watched: true},

            // Include the platform flow-designer templates
            {pattern: 'platform/flow-designer/components/**/*.html', included: true, watched: true, served: true},

            // Include the platform flow-designer images
            {pattern: 'platform/flow-designer/**/*.png', included: true, watched: true, served: true},

            // Include test specs
            {pattern: 'webapp/**/*.js', included: false, watched: false},
            {pattern: 'platform/**/*.js', included: false, watched: false}
        ],
        // Proxied base paths for loading assets
        proxies: {
            // required for modules fetched by SystemJS
            '/base/webapp/node_modules/': '/base/node_modules/',
            '/base/webapp/platform/': '/base/platform/'
        },
        exclude: [],
        preprocessors: {
            'webapp/**/!(*spec|*mock|*stub|*config|*extras).js': 'coverage',
            'platform/**/!(*spec|*mock|*stub|*config|*extras).js': 'coverage'
        },
        // Try Websocket for a faster transmission first. Fallback to polling if necessary.
        transports: ['websocket', 'polling'],
        reporters: ['kjhtml', 'spec', 'coverage'],
        coverageReporter: {
            type: 'html',
            dir: 'coverage/'
        },
        specReporter: {
            failFast: false
        },
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: ['Chrome'],
        singleRun: false
    });

    if (process.env.TRAVIS) {
        config.set({
            browsers: ['Chrome_travis_ci']
        });

        // Override base config
        config.set({
            singleRun: true,
            autoWatch: false,
            reporters: ['spec', 'coverage'],
            specReporter: {
                failFast: true
            }
        });
    }
}