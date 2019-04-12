package com.cloudera.cem.efm.service.protocol;

import com.cloudera.cem.efm.service.c2protocol.C2ProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Optional;

/**
 * Purpose: Payload represents the abstracted view of a payload servicer
 * Design: Accepts the the protocol service and C2 Properties are arguments for each
 * implementing protocol.
 */
@Service
public abstract class PayloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadService.class);
    /**
     * Class members that are provided for each protocol implementation.
     */
    protected final C2ProtocolService service;

    /**
     * This URI is the context for this payload service. Can be null.
     */
    protected URI urlBase;

    @Autowired
    public PayloadService(final C2ProtocolService service) {
        this.service = service;
        urlBase = null;
    }


    /**
     * Sets the URL associated with this payload service.
     * @param context payload service context URI
     */
    public void setURI(final URI context) {
        this.urlBase = context;
    }


    /**
     * Retrieves the URL associated with this payload service
     * @return context URL
     */
    public Optional<URI> getURI() {
        return Optional.ofNullable(this.urlBase);
    }
}