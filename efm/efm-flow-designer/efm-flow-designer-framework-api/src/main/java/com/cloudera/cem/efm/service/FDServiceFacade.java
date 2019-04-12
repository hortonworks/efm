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
package com.cloudera.cem.efm.service;

import com.cloudera.cem.efm.exception.FlowPublisherException;
import com.cloudera.cem.efm.model.ELSpecification;
import com.cloudera.cem.efm.model.component.FDConnection;
import com.cloudera.cem.efm.model.component.FDControllerService;
import com.cloudera.cem.efm.model.component.FDFunnel;
import com.cloudera.cem.efm.model.component.FDPropertyDescriptor;
import com.cloudera.cem.efm.model.flow.FDFlowPublishMetadata;
import com.cloudera.cem.efm.model.flow.FDFlowSummary;
import com.cloudera.cem.efm.model.flow.FDProcessGroupFlow;
import com.cloudera.cem.efm.model.component.FDProcessor;
import com.cloudera.cem.efm.model.component.FDRemoteProcessGroup;
import com.cloudera.cem.efm.model.component.FDRevision;
import com.cloudera.cem.efm.model.flow.FDFlow;
import com.cloudera.cem.efm.model.flow.FDFlowEvent;
import com.cloudera.cem.efm.model.flow.FDFlowMetadata;
import com.cloudera.cem.efm.model.flow.FDVersionInfo;
import com.cloudera.cem.efm.model.types.FDComponentTypes;
import com.cloudera.cem.efm.security.NiFiUser;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * Main entry-point to flow designer back-end, REST resources should call this interface.
 */
public interface FDServiceFacade {

    // ---- Flow methods ----

    FDFlow getFlow(String fdFlowId);

    List<FDFlowEvent> getFlowEvents(String flowId);

    FDFlowMetadata getFlowMetadata(String fdFlowId);

    FDFlowMetadata createFlow(String agentClass, NiFiUser user);

    List<FDFlowMetadata> getAvailableFlows();

    List<FDFlowSummary> getFlowSummaries();

    FDVersionInfo publishFlow(String fdFlowId, FDFlowPublishMetadata publishMetadata) throws FlowPublisherException;

    FDVersionInfo revertFlowToLastPublishedState(String fdFlowId);

    FDVersionInfo revertFlowToFlowRevision(String fdFlowId, BigInteger flowRevision);

    FDVersionInfo getFlowVersionInfo(String fdFlowId);

    ELSpecification getELSpecification(String fdFlowId);

    FDFlowMetadata deleteFlow(String fdFlowId);

    // ---- Processor group methods ----

    FDProcessGroupFlow getProcessGroup(String flowId, String processGroupId, boolean includeChildren);

    // ---- Processor methods ----

    FDComponentTypes getProcessorTypes(String fdFlowId);

    FDProcessor createProcessor(String flowId, String processGroupId, FDProcessor requestProcessor);

    FDProcessor getProcessor(String flowId, String processorId);

    FDProcessor updateProcessor(String flowId, String processorId, FDProcessor requestProcessor);

    FDProcessor deleteProcessor(String flowId, String processorId, FDRevision requestRevision);

    FDPropertyDescriptor getProcessorPropertyDescriptor(String flowId, String processorId, String propertyName);

    // ---- Controller service methods ----

    FDComponentTypes getControllerServiceTypes(String fdFlowId);

    FDControllerService createControllerService(String flowId, String processGroupId, FDControllerService requestControllerService);

    FDControllerService getControllerService(String flowId, String controllerServiceId);

    Set<FDControllerService> getControllerServices(String flowId, String processGroupId);

    FDControllerService updateControllerService(String flowId, String controllerServiceId, FDControllerService requestControllerService);

    FDControllerService deleteControllerService(String flowId, String controllerServiceId, FDRevision requestRevision);

    FDPropertyDescriptor getControllerServicePropertyDescriptor(String flowId, String controllerServiceId, String propertyName);

    // ---- Connection methods ----

    FDConnection createConnection(String flowId, String processGroupId, FDConnection requestConnection);

    FDConnection getConnection(String flowId, String connectionId);

    FDConnection updateConnection(String flowId, String connectionId, FDConnection requestConnection);

    FDConnection deleteConnection(String flowId, String connectionId, FDRevision requestRevision);

    // ---- Funnel methods ----

    FDFunnel createFunnel(String flowId, String processGroupId, FDFunnel requestFunnel);

    FDFunnel getFunnel(String flowId, String funnelId);

    FDFunnel updateFunnel(String flowId, String funnelId, FDFunnel requestFunnel);

    FDFunnel deleteFunnel(String flowId, String funnelId, FDRevision requestRevision);

    // ---- Remote Process Group methods ----

    FDRemoteProcessGroup createRemoteProcessGroup(String flowId, String processGroupId, FDRemoteProcessGroup requestRemoteProcessGroup);

    FDRemoteProcessGroup getRemoteProcessGroup(String flowId, String remoteProcessGroupId);

    FDRemoteProcessGroup updateRemoteProcessGroup(String flowId, String remoteProcessGroupId, FDRemoteProcessGroup requestRemoteProcessGroup);

    FDRemoteProcessGroup deleteRemoteProcessGroup(String flowId, String remoteProcessGroupId, FDRevision requestRevision);

}
