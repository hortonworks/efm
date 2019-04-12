package com.cloudera.cem.efm.registry;

import com.cloudera.cem.efm.security.NiFiUser;
import org.apache.nifi.registry.client.BucketClient;
import org.apache.nifi.registry.client.FlowClient;
import org.apache.nifi.registry.client.FlowSnapshotClient;

public interface FDNiFiRegistryClient {

    BucketClient getBucketClient(String baseUrl, NiFiUser user);

    FlowClient getFlowClient(String baseUrl, NiFiUser user);

    FlowSnapshotClient getFlowSnapshotClient(String baseUrl, NiFiUser user);

}
