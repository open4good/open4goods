package org.open4goods.datareference.port;

import java.util.List;
import java.util.Optional;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;

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
     * @param surface surface being served
     * @return the document, or empty when none was built for that surface
     */
    Optional<ProductReferenceProjection> find(Gtin gtin, ProjectionSurface surface);

    /**
     * Reads several product documents.
     *
     * @param gtins product identities
     * @param surface surface being served
     * @return the documents that exist, in the order the identities were given
     */
    List<ProductReferenceProjection> findAll(List<Gtin> gtins, ProjectionSurface surface);

    /**
     * Scans every document built for one surface.
     *
     * @param surface surface being scanned
     * @param request page position, size, ordering and failure behavior
     * @return one page of documents
     */
    ScanPage<ProductReferenceProjection> scan(ProjectionSurface surface, ScanRequest request);
}
