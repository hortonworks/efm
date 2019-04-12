package com.cloudera.cem.efm.service.protocol;

import com.cloudera.cem.efm.model.OperationType;

/**
 * Purpose: Payload represents the abstracted view of a payload that arrives or is sent
 * via the Payload Services.
 *
 * Design: Implemented as an interface, our intent is that each implementing protocol
 * define payload times which it can consume
 */
public interface Payload{

    public OperationType getPayloadType();

    public String getIdentifier();

    public int getMajorVersion();
}
