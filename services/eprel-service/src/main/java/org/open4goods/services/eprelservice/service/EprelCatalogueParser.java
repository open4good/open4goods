package org.open4goods.services.eprelservice.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.open4goods.model.eprel.EprelProduct;

/**
 * Contract responsible for turning a downloaded catalogue into {@link EprelProduct} instances.
 */
public interface EprelCatalogueParser
{
    /**
     * Parses the provided ZIP archive.
     *
     * @param zipFile  path to the downloaded archive
     * @param consumer callback receiving each deserialised product
     * @throws IOException when the archive cannot be read
     */
    void parse(Path zipFile, Consumer<EprelProduct> consumer) throws IOException;

    /**
     * Parses the provided ZIP archive, resuming after a prior record position.
     *
     * <p>The first {@code skipRecords} catalogue records are traversed without being
     * deserialised or handed to {@code consumer}, so a retry of an already-downloaded
     * archive does not pay the deserialisation cost of records it has already produced.
     *
     * @param zipFile     path to the downloaded archive
     * @param skipRecords number of leading records, across the whole archive, to skip
     * @param consumer    callback receiving each deserialised product past that position
     * @throws IOException when the archive cannot be read
     */
    default void parse(Path zipFile, long skipRecords, Consumer<EprelProduct> consumer) throws IOException {
        if (skipRecords > 0) {
            throw new UnsupportedOperationException(
                    getClass().getSimpleName() + " does not support resuming from a record position");
        }
        parse(zipFile, consumer);
    }
}
