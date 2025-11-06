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
}
