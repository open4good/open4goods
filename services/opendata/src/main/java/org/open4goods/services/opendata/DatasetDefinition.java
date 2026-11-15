package org.open4goods.services.opendata;

import java.io.File;
import java.util.Set;
import java.util.function.Function;

import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;

/**
 * One dataset of a generation pass.
 *
 * @param id             dataset identifier, used in the manifests ({@code gtin}, {@code isbn})
 * @param filename       name of the CSV entry inside the zip
 * @param zipFile        zip written by the pass, in the temporary folder
 * @param finalZipFile   published zip, replaced atomically at the end of a successful pass
 * @param manifestFile   published manifest of this dataset
 * @param publicFilename public object name of the zip ({@code data.nudger.fr/{name}})
 * @param header         CSV header
 * @param barcodeTypes   barcode types routed to this dataset
 * @param rowMapper      conversion of a product into a CSV row of {@code header.length} cells
 */
public record DatasetDefinition(
            String id,
            String filename,
            File zipFile,
            File finalZipFile,
            File manifestFile,
            String publicFilename,
            String[] header,
            Set<BarcodeType> barcodeTypes,
            Function<Product, String[]> rowMapper) {
    }
