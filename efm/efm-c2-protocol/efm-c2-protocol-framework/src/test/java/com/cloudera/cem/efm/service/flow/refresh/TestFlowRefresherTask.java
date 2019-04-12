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
package com.cloudera.cem.efm.service.flow.refresh;

import com.cloudera.cem.efm.db.entity.FlowMappingEntity;
import com.cloudera.cem.efm.db.repository.AgentClassRepository;
import com.cloudera.cem.efm.db.repository.FlowMappingRepository;
import com.cloudera.cem.efm.db.repository.FlowRepository;
import com.cloudera.cem.efm.model.FlowUri;
import com.cloudera.cem.efm.service.flow.FlowTestUtils;
import com.cloudera.cem.efm.service.flow.mapping.FlowMapper;
import com.cloudera.cem.efm.service.flow.mapping.FlowMapperException;
import com.cloudera.cem.efm.service.flow.retrieval.FlowRetrievalException;
import com.cloudera.cem.efm.service.flow.retrieval.FlowRetrievalService;
import com.cloudera.cem.efm.service.flow.serialize.ConfigSchemaSerializer;
import com.cloudera.cem.efm.service.flow.serialize.Serializer;
import com.cloudera.cem.efm.service.flow.serialize.SerializerException;
import com.cloudera.cem.efm.service.flow.transform.FlowTransformException;
import com.cloudera.cem.efm.service.flow.transform.FlowTransformService;
import org.apache.nifi.minifi.commons.schema.ConfigSchema;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestFlowRefresherTask {

    private AgentClassRepository agentClassRepository;
    private FlowMapper flowMapper;
    private FlowRetrievalService flowRetrievalService;
    private FlowTransformService<ConfigSchema> flowTransformService;
    private Serializer<ConfigSchema> configSchemaSerializer;
    private FlowRepository flowRepository;
    private FlowMappingRepository flowMappingRepository;
    private TransactionTemplate transactionTemplate;

    private FlowRefresherTask flowRefresherTask;

    @Before
    public void setup() {
        this.agentClassRepository = mock(AgentClassRepository.class);
        this.flowMapper = mock(FlowMapper.class);
        this.flowRetrievalService = mock(FlowRetrievalService.class);
        this.flowTransformService = mock(FlowTransformService.class);
        this.configSchemaSerializer = mock(ConfigSchemaSerializer.class);
        this.flowRepository = mock(FlowRepository.class);
        this.flowMappingRepository = mock(FlowMappingRepository.class);
        this.transactionTemplate = mock(TransactionTemplate.class);

        this.flowRefresherTask = new FlowRefresherTask(
                agentClassRepository,
                flowMapper,
                flowRetrievalService,
                flowTransformService,
                configSchemaSerializer,
                flowRepository,
                flowMappingRepository,
                transactionTemplate
        );
    }

    @Test
    public void testRefreshWhenNewerVersionExists() throws FlowRetrievalException, FlowMapperException, SerializerException, FlowTransformException, IOException {
        final String className = "Test Class";
        final AtomicInteger refreshedCounter = new AtomicInteger(0);

        final FlowUri flowUri = new FlowUri("http://localhost:18080", "1234", "5678");
        when(flowMapper.getFlowMapping(className)).thenReturn(Optional.of(flowUri));

        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), 2);
        when(flowRetrievalService.getLatestFlow(flowUri)).thenReturn(flowSnapshot);

        final FlowMappingEntity flowMappingEntity = FlowTestUtils.createFlowMappingEntity(className, flowUri, 1, "YAML");

        when(flowMappingRepository.findById(className)).thenReturn(Optional.of(flowMappingEntity));

        flowRefresherTask.refreshAgentClass(className, refreshedCounter);

        // should have refreshed from v1 to v2
        assertEquals(1, refreshedCounter.get());
    }

    @Test
    public void testRefreshWhenSameVersionFound() throws FlowRetrievalException, FlowMapperException, SerializerException, FlowTransformException, IOException {
        final String className = "Test Class";
        final AtomicInteger refreshedCounter = new AtomicInteger(0);

        final FlowUri flowUri = new FlowUri("http://localhost:18080", "1234", "5678");
        when(flowMapper.getFlowMapping(className)).thenReturn(Optional.of(flowUri));

        final Integer flowVersion = new Integer(1);
        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), flowVersion);
        when(flowRetrievalService.getLatestFlow(flowUri)).thenReturn(flowSnapshot);

        final FlowMappingEntity flowMappingEntity = FlowTestUtils.createFlowMappingEntity(className, flowUri, flowVersion, "YAML");
        when(flowMappingRepository.findById(className)).thenReturn(Optional.of(flowMappingEntity));

        // test the refresher
        flowRefresherTask.refreshAgentClass(className, refreshedCounter);

        // no refresh should have happened because already on version 1
        assertEquals(0, refreshedCounter.get());
    }

    @Test
    public void testRefreshWhenRegistryInfoChanged() throws FlowRetrievalException, FlowMapperException, SerializerException, FlowTransformException, IOException {
        final String className = "Test Class";
        final AtomicInteger refreshedCounter = new AtomicInteger(0);

        final FlowUri flowUri = new FlowUri("http://localhost:18080", "1234", "5678");
        when(flowMapper.getFlowMapping(className)).thenReturn(Optional.of(flowUri));

        final Integer flowVersion = new Integer(1);
        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), flowVersion);
        when(flowRetrievalService.getLatestFlow(flowUri)).thenReturn(flowSnapshot);

        final FlowMappingEntity flowMappingEntity = FlowTestUtils.createFlowMappingEntity(className, flowUri, flowVersion, "YAML");
        flowMappingEntity.getFlowEntity().setRegistryUrl("http://changed-host:18080"); // change the hostname to trigger an updated
        when(flowMappingRepository.findById(className)).thenReturn(Optional.of(flowMappingEntity));

        // test the refresher
        flowRefresherTask.refreshAgentClass(className, refreshedCounter);

        // refresh should happen because registry url changed
        assertEquals(1, refreshedCounter.get());
    }

    @Test
    public void testRefreshWhenNoFlowMappingExists() throws FlowMapperException, IOException, FlowRetrievalException, SerializerException, FlowTransformException {
        final String className = "Test Class";
        final AtomicInteger refreshedCounter = new AtomicInteger(0);

        final FlowUri flowUri = new FlowUri("http://localhost:18080", "1234", "5678");
        when(flowMapper.getFlowMapping(className)).thenReturn(Optional.of(flowUri));

        final Integer flowVersion = new Integer(1);
        final VersionedFlowSnapshot flowSnapshot = FlowTestUtils.createSnapshot(flowUri.getBucketId(), flowUri.getFlowId(), flowVersion);
        when(flowRetrievalService.getLatestFlow(flowUri)).thenReturn(flowSnapshot);

        // return no mapping for agent class
        when(flowMappingRepository.findById(className)).thenReturn(Optional.empty());

        // test the refresher
        flowRefresherTask.refreshAgentClass(className, refreshedCounter);

        // refresh should happen because registry url changed
        assertEquals(1, refreshedCounter.get());
        verify(flowTransformService, times(1)).transform(flowSnapshot);
    }
}
