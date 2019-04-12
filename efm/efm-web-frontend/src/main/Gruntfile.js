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

var url = require('url');
var proxy = require('proxy-middleware');

var proxyOptions = url.parse('http://localhost:10080/efm/api');
proxyOptions.route = '/api';
proxyOptions.headers = {
    'X-Forwarded-Host': 'localhost',
    'X-Forwarded-Port': '8080'
};

module.exports = function (grunt) {
    // load all grunt tasks matching the ['grunt-*', '@*/grunt-*'] patterns
    require('load-grunt-tasks')(grunt);

    grunt.initConfig({
        watch: {
            files: [
                'webapp/theming/**/*.scss',
                'platform/flow-designer/theming/**/*.scss',
                'platform/flow-designer/flow-designer.scss'
            ],
            tasks: ['compile-web-ui-styles', 'compile-flow-designer-styles']
        },
        sass: {
            options: {
                outputStyle: 'compressed',
                sourceMap: true
            },
            minifyWebUi: {
                files: [{
                    './webapp/css/efm.min.css': ['./webapp/theming/efm.scss']
                }]
            },
            minifyFlowDesigner: {
                files: [{
                    './platform/flow-designer/css/flow-designer.min.css': ['./platform/flow-designer/flow-designer.scss']
                }]
            }
        },
        systemjs: {
            options: {
                sfx: true,
                minify: true,
                sourceMaps: true,
                build: {
                    lowResSourceMaps: true
                }
            },
            bundleWebUi: {
                options: {
                    configFile: "./systemjs.config.js"
                },
                files: [{
                    "src": "./webapp/efm.bootstrap.js",
                    "dest": "./webapp/efm.bundle.min.js"
                }]
            }
        },
        compress: {
            options: {
                mode: 'gzip'
            },
            webUi: {
                files: [{
                    expand: true,
                    src: ['./webapp/efm.bundle.min.js'],
                    dest: './',
                    ext: '.bundle.min.js.gz'
                }]
            },
            webUiStyles: {
                files: [{
                    expand: true,
                    src: ['./webapp/css/efm.min.css'],
                    dest: './',
                    ext: '.min.css.gz'
                }]
            },
            flowDesignerStyles: {
                files: [{
                    expand: true,
                    src: ['./platform/flow-designer/css/flow-designer.min.css'],
                    dest: './',
                    ext: '.min.css.gz'
                }]
            }
        },
        browserSync: {
            bsFiles: {
                src : [
                    // JS files
                    'webapp/**/*.js',
                    'platform/flow-designer/**/*.js',

                    // CSS files
                    'webapp/css/*.css',
                    'platform/flow-designer/css/*.css',

                    // HTML files
                    'webapp/**/*.html',
                    'platform/flow-designer/**/*.html'
                ]
            },
            options: {
                port: 8080,
                watchTask: true,
                server: {
                    baseDir: './',
                    middleware: [proxy(proxyOptions)]
                }
            }
        }
    });
    grunt.registerTask('compile-flow-designer-styles', ['sass:minifyFlowDesigner', 'compress:flowDesignerStyles']);
    grunt.registerTask('compile-web-ui-styles', ['sass:minifyWebUi', 'compress:webUiStyles']);
    grunt.registerTask('bundle-web-ui', ['systemjs:bundleWebUi', 'compress:webUi']);
    grunt.registerTask('default', ['compile-web-ui-styles', 'compile-flow-designer-styles', 'browserSync', 'watch']);
};
