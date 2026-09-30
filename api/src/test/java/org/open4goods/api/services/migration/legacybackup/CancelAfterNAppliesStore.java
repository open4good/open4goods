package org.open4goods.api.services.migration.legacybackup;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * Test double that requests cancellation deterministically after a fixed number of accepted
 * writes, so a test can simulate a mid-run stop without racing a background thread.
 */
final class CancelAfterNAppliesStore implements SourceRecordHeadStore {

    private final SourceRecordHeadStore delegate;
    private final int cancelAfter;
    private final LegacyBackupCancellationRegistry cancellationRegistry;
    private final String datasetId;
    private final AtomicInteger applied = new AtomicInteger();

    CancelAfterNAppliesStore(SourceRecordHeadStore delegate, int cancelAfter,
            LegacyBackupCancellationRegistry cancellationRegistry, String datasetId) {
        this.delegate = delegate;
        this.cancelAfter = cancelAfter;
        this.cancellationRegistry = cancellationRegistry;
        this.datasetId = datasetId;
    }

    @Override
    public SourceRecordTransition apply(SourceRecordMutation mutation) {
        SourceRecordTransition transition = delegate.apply(mutation);
        if (applied.incrementAndGet() >= cancelAfter) {
            cancellationRegistry.requestCancellation(datasetId);
        }
        return transition;
    }

    @Override
    public Optional<SourceRecordHead> find(SourceRecordKey key) {
        return delegate.find(key);
    }

    @Override
    public List<SourceRecordHead> findByGtin(Gtin gtin) {
        return delegate.findByGtin(gtin);
    }

    @Override
    public boolean storeIfNewer(SourceRecordHead head) {
        return delegate.storeIfNewer(head);
    }

    @Override
    public boolean delete(SourceRecordKey key) {
        return delegate.delete(key);
    }
}
