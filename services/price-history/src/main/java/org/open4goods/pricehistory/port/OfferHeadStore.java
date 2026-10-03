package org.open4goods.pricehistory.port;

import java.util.Optional;
import java.util.stream.Stream;

import org.open4goods.datareference.model.SourceId;
import org.open4goods.pricehistory.model.OfferHead;
import org.open4goods.pricehistory.model.OfferKey;

/** Replaceable persistence of current provider offer state. */
public interface OfferHeadStore {

    /** Reads the current state of one provider offer. */
    Optional<OfferHead> find(OfferKey key);

    /**
     * Stores a replacement only if the observed revision still matches.
     *
     * @param head replacement head
     * @param expectedRevision zero for creation, otherwise the previous revision
     * @return whether the compare-and-set succeeded
     */
    boolean compareAndSet(OfferHead head, long expectedRevision);

    /**
     * Streams the provider's current heads for a successful complete-feed reconciliation.
     *
     * <p>The returned stream holds an open server-side resource (e.g. an Elasticsearch
     * point-in-time) for its whole traversal. The caller <strong>must</strong> close it — typically
     * with try-with-resources — on every path, including when consumption is interrupted by an
     * exception, or the resource leaks until its own keep-alive expires.
     *
     * @param providerId public provider identity
     * @return heads owned by that provider; must be closed by the caller
     */
    default Stream<OfferHead> findByProvider(SourceId providerId) {
        throw new UnsupportedOperationException("provider reconciliation is not implemented by this head store");
    }
}
