/*
* (c) 2018-2019 Cloudera, Inc. All rights reserved.
*
* This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
* Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
* to distribute this code. If you do not have a written agreement with Cloudera or with an authorized and
* properly licensed third party, you do not have any rights to this code.
*
* If this code is provided to you under the terms of the AGPLv3:
* (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
* (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
* LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
* (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
* FROM OR RELATED TO THE CODE; AND
* (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
* TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
* UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
*/

var ngCore = require('@angular/core');
var fdsAnimations = require('@flow-design-system/common/animations');
var CommonService = require('@flow-designer/services/CommonService');
var EfmApi = require('services/efm.api.service.js');

/**
 * EfmMonitorEventDetails constructor.
 *
 * @param efmApi                The EFM api.
 * @param common                The common service.
 * @constructor
 */
function EfmMonitorEventDetails(efmApi, common) {
    var self = this;
    this.eventDetail = null;

    /**
     * On change lifecycle
     */
    this.onChange = function () {
        if (common.isDefinedAndNotNull(this.event.links.detail)) {
            efmApi.getEventDetail(this.event.links.detail.href)
                .subscribe(
                    function (response) {
                        self.eventDetail = response;
                    },
                    function (errorResponse) {
                        self.eventDetail = self.event;
                    }
                )
        } else {
            this.eventDetail = this.event;
        }
    };

    /**
     * Close the Event Details panel
     */
    this.cancel = function () {
        this.onClose();
    };
}

EfmMonitorEventDetails.prototype = {
    constructor: EfmMonitorEventDetails,

    /**
     * On change lifecycle
     */
    ngOnChanges: function () {
        this.onChange();
    }
};

EfmMonitorEventDetails.annotations = [
    new ngCore.Component({
        selector: 'efm-monitor-event-details',
        template: require('./efm.monitor-event-details.component.html!text'),
        inputs: [
            'event',
            'onClose'
        ],
        animations: [fdsAnimations.fadeAnimation]
    })
];

EfmMonitorEventDetails.parameters = [
    EfmApi,
    CommonService
];

module.exports = EfmMonitorEventDetails;