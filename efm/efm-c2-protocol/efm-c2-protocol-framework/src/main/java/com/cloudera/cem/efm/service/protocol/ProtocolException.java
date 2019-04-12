package com.cloudera.cem.efm.service.protocol;

public class ProtocolException extends Exception {

    public ProtocolException(final String msg) {
        super(msg);
    }

    public ProtocolException(Throwable throwable){
        super(throwable);
    }
}
