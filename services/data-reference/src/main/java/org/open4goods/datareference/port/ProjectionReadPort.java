package org.open4goods.datareference.port;

import java.util.List;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;

/**
 * Read access to built product documents.
 *
 * <p>This is what consumers read. Batch jobs read it, or a purpose-built
 * rollup, and never aggregate raw assertions: doing so would reimplement
 * resolution in each job and produce a different answer from the page.
 */
public interface ProjectionReadPort {

    /**
     * Reads one product document.
     *
     * @param gtin product identity
     * @return the one-GTIN envelope, or empty when none was built
     */
    Optional<ProductReferenceProjectionEnvelope> find(Gtin gtin);

    /**
     * Reads several product documents.
     *
     * @param gtins product identities
     * @return the documents that exist, in the order the identities were given
     */
    List<ProductReferenceProjectionEnvelope> findAll(List<Gtin> gtins);

    /**
     * Scans every one-GTIN projection envelope.
     *
     * @param request page position, size, ordering and failure behavior
     * @return one page of documents
     */
    ScanPage<ProductReferenceProjectionEnvelope> scan(ScanRequest request);
}
