package org.open4goods.datareference.model;

import java.util.Objects;

/**
 * Identity of one provider record: the source, and the record id that source
 * issued.
 *
 * <p>The GTIN a record is attached to is deliberately absent. Attachment is
 * evidence that can be wrong and later corrected, and it is carried by
 * {@link GtinLink}. Were it part of the key, correcting an attachment would
 * create a second record for the same provider observation and the original
 * would have to be reconciled or deleted.
 *
 * @param sourceId source that issued the record
 * @param sourceRecordId identifier issued by that source
 */
public record SourceRecordKey(SourceId sourceId, SourceRecordId sourceRecordId) {

    /**
     * Validates the record identity.
     */
    public SourceRecordKey {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(sourceRecordId, "sourceRecordId must not be null");
    }

    /**
     * Builds a key from raw identifiers.
     *
     * @param sourceId source identifier
     * @param sourceRecordId provider record identifier
     * @return validated record key
     */
    public static SourceRecordKey of(String sourceId, String sourceRecordId) {
        return new SourceRecordKey(new SourceId(sourceId), new SourceRecordId(sourceRecordId));
    }

    /**
     * Returns the stable serialized form used by stores and logs.
     *
     * @return identifier such as {@code icecat/123456}
     */
    public String externalForm() {
        return sourceId.value() + "/" + sourceRecordId.value();
    }

    @Override
    public String toString() {
        return externalForm();
    }
}
