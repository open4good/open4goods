package org.open4goods.api.services.migration.legacybackup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/** Minimal in-memory {@link SourceRecordHeadStore} test double: CAS by key, no journal retained. */
final class InMemorySourceRecordHeadStore implements SourceRecordHeadStore {

    private final Map<SourceRecordKey, SourceRecordHead> heads = new ConcurrentHashMap<>();
    private final Map<SourceRecordKey, Long> revisions = new ConcurrentHashMap<>();

    @Override
    public SourceRecordTransition apply(SourceRecordMutation mutation) {
        SourceRecordHead candidate = mutation.candidate();
        SourceRecordHead current = heads.get(candidate.key());
        long revision = revisions.merge(candidate.key(), 1L, Long::sum);
        if (candidate.supersedes(current)) {
            heads.put(candidate.key(), candidate);
            return new SourceRecordTransition(SourceRecordTransition.idFor(candidate.key(), revision), candidate.key(),
                    revision, candidate.schemaVersion(), candidate.providerVersion(),
                    current == null ? null : current.payloadHash(), candidate.payloadHash(), candidate.observedAt(),
                    candidate.retrievedAt(), candidate.state(), SourceRecordTransitionOutcome.ACCEPTED, null,
                    List.of());
        }
        return new SourceRecordTransition(SourceRecordTransition.idFor(candidate.key(), revision), candidate.key(),
                revision, candidate.schemaVersion(), candidate.providerVersion(),
                current == null ? null : current.payloadHash(), candidate.payloadHash(), candidate.observedAt(),
                candidate.retrievedAt(), candidate.state(), SourceRecordTransitionOutcome.OUT_OF_ORDER, null,
                List.of());
    }

    @Override
    public Optional<SourceRecordHead> find(SourceRecordKey key) {
        return Optional.ofNullable(heads.get(key));
    }

    @Override
    public List<SourceRecordHead> findByGtin(Gtin gtin) {
        List<SourceRecordHead> result = new ArrayList<>();
        for (SourceRecordHead head : heads.values()) {
            if (head.gtinLinks().stream().anyMatch(link -> link.gtin().equals(gtin))) {
                result.add(head);
            }
        }
        return result;
    }

    @Override
    public boolean storeIfNewer(SourceRecordHead head) {
        SourceRecordHead current = heads.get(head.key());
        if (head.supersedes(current)) {
            heads.put(head.key(), head);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(SourceRecordKey key) {
        return heads.remove(key) != null;
    }

    int size() {
        return heads.size();
    }
}
