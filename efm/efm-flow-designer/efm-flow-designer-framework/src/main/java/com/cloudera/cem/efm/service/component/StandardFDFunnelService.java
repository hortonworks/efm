/**
 * (c) 2018-2019 Cloudera, Inc. All rights reserved.
 * <p>
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 * <p>
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
package com.cloudera.cem.efm.service.component;

import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.service.flow.FDFlowManager;
import com.cloudera.cem.efm.service.validation.ValidationService;
import com.cloudera.cem.efm.validation.ControllerServiceLookup;
import com.cloudera.cem.efm.validation.FunnelValidationContext;
import org.apache.nifi.registry.flow.VersionedFunnel;
import org.apache.nifi.registry.flow.VersionedProcessGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Standard implementation of FDFunnelService.
 */
@Service
@Transactional(rollbackFor = Throwable.class)
public class StandardFDFunnelService extends BaseComponentService<VersionedFunnel, FDFunnel> implements FDFunnelService {

    public StandardFDFunnelService(final FDFlowManager flowManager, final ValidationService validationService) {
        super(flowManager, validationService);
    }

    @Override
    protected FDFunnel instantiateComponent(final VersionedFunnel componentConfig) {
        final FDFunnel funnel = new FDFunnel();
        funnel.setComponentConfiguration(componentConfig);
        return funnel;
    }

    @Override
    protected void configureComponentSpecifics(final VersionedFunnel requestComponent, final VersionedFunnel resultComponent) {
        // funnels don't have any additional configuration
    }

    @Override
    public FDFunnel create(final String flowId, final String processGroupId, final VersionedFunnel requestComponentConfig) {
        return createComponent(
                flowId,
                processGroupId,
                null,
                requestComponentConfig, () -> {
                    final VersionedFunnel componentConfig = new VersionedFunnel();

                    final FDFunnel component = new FDFunnel();
                    component.setComponentConfiguration(componentConfig);
                    return component;
                },
                VersionedProcessGroup::getFunnels,
                this::validate);
    }

    @Override
    public FDFunnel get(final String flowId, final String componentId) {
        return getComponent(flowId, componentId, VersionedProcessGroup::getFunnels, this::validate);
    }

    @Override
    public FDFunnel update(final String flowId, final VersionedFunnel requestComponentConfig) {
        return updateComponent(flowId, requestComponentConfig, VersionedProcessGroup::getFunnels, this::validate);
    }

    @Override
    public void delete(final String flowId, final String componentId) {
        deleteComponent(flowId, componentId, VersionedProcessGroup::getFunnels);
    }

    private Collection<String> validate(final FDFunnel funnel, final VersionedProcessGroup group, final String flowId,
                                        final ControllerServiceLookup controllerServiceLookup) {
        final FunnelValidationContext validationContext = new FunnelValidationContext();
        return validationContext.validate(funnel.getComponentConfiguration(), group);
    }
}
