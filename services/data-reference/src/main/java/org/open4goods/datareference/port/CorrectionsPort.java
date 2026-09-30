package org.open4goods.datareference.port;

import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.resolution.Correction;

/**
 * Reviewed O4G decisions that override sources.
 */
public interface CorrectionsPort {

    /**
     * Returns the corrections applying to one product.
     *
     * @param gtin product identity
     * @return corrections, ordered by canonical attribute; empty when none apply
     */
    List<Correction> findByGtin(Gtin gtin);

    /**
     * Scans every stored correction.
     *
     * @param request page position, size, ordering and failure behavior
     * @return one page of corrections
     */
    ScanPage<Correction> scanAll(ScanRequest request);
}
