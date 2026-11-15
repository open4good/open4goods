package org.open4goods.services.opendata.model;

/**
 * An object of the open data bucket.
 *
 * @param key    object key
 * @param bytes  size in bytes
 * @param sha256 value of the {@code sha256} user metadata, null when absent or not read (listing)
 */
public record StoredObject(String key, long bytes, String sha256) {
}
