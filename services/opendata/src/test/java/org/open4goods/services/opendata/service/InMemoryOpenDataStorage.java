package org.open4goods.services.opendata.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.services.opendata.model.ObjectHeaders;
import org.open4goods.services.opendata.model.StoredObject;

/** An open data bucket in memory, recording each call (GOU-2289). */
final class InMemoryOpenDataStorage implements OpenDataStorage {

    /** Content of an object and the headers it was written with. */
    record Stored(byte[] content, ObjectHeaders headers) {
    }

    final Map<String, Stored> objects = new LinkedHashMap<>();
    final List<String> operations = new ArrayList<>();
    boolean corruptUploads;
    String failUploadOf;

    long totalBytes() {
        return objects.values().stream().mapToLong(stored -> stored.content().length).sum();
    }

    @Override
    public List<StoredObject> list(String prefix) {
        operations.add("list " + prefix);
        return objects.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .map(entry -> new StoredObject(entry.getKey(), entry.getValue().content().length, null))
                .toList();
    }

    @Override
    public Optional<StoredObject> head(String key) {
        operations.add("head " + key);
        Stored stored = objects.get(key);
        return stored == null ? Optional.empty() : Optional.of(new StoredObject(key, stored.content().length,
                stored.headers() == null ? null : stored.headers().sha256()));
    }

    @Override
    public void upload(Path file, String key, ObjectHeaders headers) throws IOException {
        operations.add("upload " + key);
        if (key.equals(failUploadOf)) {
            throw new IOException("connection reset during part 2");
        }
        byte[] content = Files.readAllBytes(file);
        if (corruptUploads) {
            content[0] ^= 1;
        }
        objects.put(key, new Stored(content, headers));
    }

    @Override
    public void put(byte[] content, String key, ObjectHeaders headers) {
        operations.add("put " + key);
        objects.put(key, new Stored(content.clone(), headers));
    }

    @Override
    public InputStream read(String key) throws IOException {
        operations.add("read " + key);
        Stored stored = objects.get(key);
        if (stored == null) {
            throw new IOException("NoSuchKey " + key);
        }
        return new ByteArrayInputStream(stored.content());
    }

    @Override
    public void copy(String sourceKey, String targetKey, long bytes, ObjectHeaders headers) {
        operations.add("copy " + sourceKey + " " + targetKey);
        objects.put(targetKey, new Stored(objects.get(sourceKey).content(), headers));
    }

    @Override
    public void delete(String key) {
        operations.add("delete " + key);
        objects.remove(key);
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
