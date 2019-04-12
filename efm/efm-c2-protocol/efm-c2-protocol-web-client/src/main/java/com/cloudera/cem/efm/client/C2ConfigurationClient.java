package com.cloudera.cem.efm.client;

import com.cloudera.cem.efm.model.NiFiRegistryInfo;

import java.io.IOException;

/**
 * Client for obtaining configuration information about the given C2 server.
 */
public interface C2ConfigurationClient {

    /**
     * @return the NiFiRegistryInfo for the given C2 server
     */
    NiFiRegistryInfo getNiFiRegistryInfo() throws C2Exception, IOException;

}
