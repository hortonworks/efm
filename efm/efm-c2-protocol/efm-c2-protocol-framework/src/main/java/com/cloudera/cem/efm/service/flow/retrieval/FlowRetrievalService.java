/*
 * Apache NiFi - MiNiFi
 * Copyright 2014-2018 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cem.efm.service.flow.retrieval;

import com.cloudera.cem.efm.model.FlowUri;
import org.apache.nifi.registry.flow.VersionedFlowSnapshot;
import org.apache.nifi.registry.flow.VersionedFlowSnapshotMetadata;

import java.io.IOException;
import java.util.List;

/**
 * A service used to retrieve versioned flows from NiFi registries based on a given agent class.
 *
 * The implementations of this service know about a given set of mappings from agent class to versioned flow.
 */
public interface FlowRetrievalService {

    /**
     * Gets the available versions for the given agent class.
     *
     * @param flowUri the flow uri to retrieve versions for
     * @return the list of available versions
     * @throws IOException if an I/O error occurs retrieving the flow versions
     * @throws FlowRetrievalException if any other error is encountered attempting to retrieve the flow versions
     */
    List<VersionedFlowSnapshotMetadata> getVersions(FlowUri flowUri) throws IOException, FlowRetrievalException;

    /**
     * Gets the specific version of a flow for the given agent class.
     *
     * @param flowUri the flow uri to retrieve the version for
     * @param version the version of the flow to get
     * @return the flow for the given class and version
     * @throws IOException if an I/O error occurs retrieving the flow
     * @throws FlowRetrievalException if any other error is encountered attempting to retrieve the flow version
     */
    VersionedFlowSnapshot getFlow(FlowUri flowUri, int version) throws IOException, FlowRetrievalException;

    /**
     * Gets the latest flow for the given agent class.
     *
     * @param flowUri the flow uri to retrieve the latest versions for
     * @return the latest flow
     * @throws IOException if an I/O error occurs retrieving the flow
     * @throws FlowRetrievalException if any other error is encountered attempting to retrieve the flow version
     */
    VersionedFlowSnapshot getLatestFlow(FlowUri flowUri) throws IOException, FlowRetrievalException;

}
