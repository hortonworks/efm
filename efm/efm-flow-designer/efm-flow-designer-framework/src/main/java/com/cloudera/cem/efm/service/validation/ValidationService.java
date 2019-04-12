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

package com.cloudera.cem.efm.service.validation;

import java.util.Collection;

public interface ValidationService {
    /**
     * Returns a Collection that represents all of the currently calculated validation errors for the component
     * with the given identifier. If validation errors have not yet been calculated since the component was last
     * updated, the provided validator will be used to compute validation errors and then those results will be
     * cached and returned.
     *
     * @param componentId the ID of the component to validate
     * @param validator the validator that can be used to perform validation if cached results are not available
     * @return the validation errors for the given component
     */
    Collection<String> getValidationErrors(String componentId, Validator validator);

    /**
     * Returns a Collection of all validation errors that are currently cached for the given component, or <code>null</code>
     * if no validation errors have been calculated. Note that an empty Collection here is NOT the same as a null value. A null
     * value indicates that the component's validation has not yet been performed or is out of date, while an empty Collection
     * indicates that the component is valid.
     *
     * @param componentId the ID of the component
     * @return the Validation Errors that are cached for the given component
     */
    Collection<String> getCachedValidationErrors(String componentId);

    /**
     * Clears any cached validation errors for the component with the given ID
     * @param componentId the ID of the component
     */
    void clearValidationErrors(String componentId);
}
