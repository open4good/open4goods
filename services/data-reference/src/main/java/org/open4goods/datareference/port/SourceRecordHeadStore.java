package org.open4goods.datareference.port;

import java.util.List;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;

/**
 * Persistence of the current head of each provider record.
 *
 * <p>Implementations keep exactly one head per {@link SourceRecordKey}.
 */
public interface SourceRecordHeadStore {

    /**
     * Reads the current head of one record.
     *
     * @param key record identity
     * @return the head, or empty when the record is unknown
     */
    Optional<SourceRecordHead> find(SourceRecordKey key);

    /**
     * Reads the current heads of every record attached to a GTIN.
     *
     * @param gtin product identity
     * @return heads whose GTIN links include this identity, ordered by record key
     */
    List<SourceRecordHead> findByGtin(Gtin gtin);

    /**
     * Stores a head only when it supersedes the stored one.
     *
     * <p>Idempotent: a head carrying a payload hash already stored is not
     * written and reports {@code false}. An older observation is likewise
     * refused, so replaying an archive cannot reinstate stale values.
     *
     * @param head head to store
     * @return {@code true} when the head was written
     * @see SourceRecordHead#supersedes(SourceRecordHead)
     */
    boolean storeIfNewer(SourceRecordHead head);

    /**
     * Removes the head of one record.
     *
     * <p>Withdrawal by the source is a {@code DELETED} head, not a deletion:
     * this exists for retention enforcement, where the record must genuinely
     * leave storage.
     *
     * @param key record identity
     * @return {@code true} when a head was removed
     */
    boolean delete(SourceRecordKey key);
}
