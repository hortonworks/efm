package com.cloudera.cem.efm.service.flow.transform;

/**
 * An error that occurred while transforming a flow.
 */
public class FlowTransformException extends RuntimeException {

    public FlowTransformException(String message) {
        super(message);
    }

    public FlowTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}
