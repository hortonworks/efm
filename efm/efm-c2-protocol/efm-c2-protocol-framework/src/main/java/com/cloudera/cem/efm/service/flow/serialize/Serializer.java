package com.cloudera.cem.efm.service.flow.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @param <T> the type of object being serialized and deserialized.
 */
public interface Serializer <T> {

    /**
     * Serializes the given type to the OutputStream.
     *
     * @param in the object to serialize
     * @param out the OutputStream to serialize to
     * @throws IOException if an I/O error occurs serializing
     * @throws SerializerException if any other error occurs serializing
     */
    void serialize(T in, OutputStream out) throws IOException, SerializerException;

    /**
     * Deserializes the given InputStream back to the type.
     *
     * @param in an input stream containing the bytes of a serialized type
     * @return the deserialized object
     * @throws IOException if an I/O error occurs deserializing
     * @throws SerializerException if any other error occurs deserializing
     */
    T deserialize(InputStream in) throws IOException, SerializerException;

}
