package com.cloudera.cem.efm.coap.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "efm.coap.server")
public class CoapProperties {

    private int port = -1;
    private int threads = 1;
    private String host = "localhost";


    public int getPort() {
        return port;
    }

    public void setPort(final int port){
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host){
        this.host = host;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads){
        this.threads = threads;
    }
}
