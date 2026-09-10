package org.open4goods.datareference.port;

import java.util.List;

import org.open4goods.datareference.model.projection.ProductReferenceProjection;

/**
 * Write access to built product documents.
 *
 * <p>Separate from {@link ProjectionReadPort} so that the assembler can be the
 * only component holding a write dependency. Every other consumer is compiled
 * against the read port and cannot write a document even by mistake.
 */
public interface ProjectionWritePort {

    /**
     * Writes one product document, replacing any document for the same product
     * and surface.
     *
     * @param projection document to write
     */
    void write(ProductReferenceProjection projection);

    /**
     * Writes several documents, in the order given.
     *
     * <p>Continues past a failure rather than stopping, and reports every element
     * it could not write. A rebuild of millions of documents that aborted on the
     * first bad one would have to be restarted from the beginning to make any
     * further progress, and the operator would learn about one problem per run.
     *
     * <p>Not atomic: documents written before a failure stay written. Callers
     * that need all-or-nothing build into a new alias and switch it.
     *
     * @param projections documents to write
     * @return elements that could not be written, empty when all succeeded
     */
    List<ScanFailure> writeAll(List<ProductReferenceProjection> projections);
}
