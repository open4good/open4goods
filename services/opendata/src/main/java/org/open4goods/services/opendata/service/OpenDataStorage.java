package org.open4goods.services.opendata.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.open4goods.services.opendata.model.ObjectHeaders;
import org.open4goods.services.opendata.model.StoredObject;

/**
 * Objects of the open data bucket, as {@link OpenDataPublisher} uses them (GOU-2289). {@link S3OpenDataStorage} is
 * the R2 implementation; tests use an in-memory one. Every write is atomic for a reader: an object is either the
 * previous one or the complete new one.
 */
public interface OpenDataStorage extends AutoCloseable {

    /**
     * @return every object of the bucket whose key starts with {@code prefix} (empty for all), with its size
     */
    List<StoredObject> list(String prefix) throws IOException;

    /**
     * @return the object with its size and {@code sha256} metadata, empty when absent
     */
    Optional<StoredObject> head(String key) throws IOException;

    /**
     * Sends a file by multipart upload. A failed upload is aborted: no part stays billed and {@code key} is unchanged.
     */
    void upload(Path file, String key, ObjectHeaders headers) throws IOException;

    /**
     * Writes a small object in one request.
     */
    void put(byte[] content, String key, ObjectHeaders headers) throws IOException;

    /**
     * @return the content of an object; the caller closes it
     */
    InputStream read(String key) throws IOException;

    /**
     * Copies an object server-side, replacing the headers of the copy with {@code headers}.
     *
     * @param bytes size of the source object, to choose between a single copy and a multipart copy
     */
    void copy(String sourceKey, String targetKey, long bytes, ObjectHeaders headers) throws IOException;

    /**
     * Deletes an object; an absent object is not an error.
     */
    void delete(String key) throws IOException;

    @Override
    void close();
}
