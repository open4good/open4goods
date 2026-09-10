package org.open4goods.datareference.model;

/**
 * Identity of the replaceable latest snapshot for one provider record and GTIN.
 *
 * @param gtin canonical product identity
 * @param source source identifier
 * @param sourceRecordId provider record identifier
 */
public record SourceSnapshotKey(Gtin gtin, String source, String sourceRecordId) {

    /**
     * Validates the stable snapshot key.
     */
    public SourceSnapshotKey {
        if (gtin == null || source == null || source.isBlank()
                || sourceRecordId == null || sourceRecordId.isBlank()) {
            throw new IllegalArgumentException("snapshot key components must not be blank");
        }
        source = source.trim();
        sourceRecordId = sourceRecordId.trim();
    }
}
