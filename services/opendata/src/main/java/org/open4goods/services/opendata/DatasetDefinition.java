package org.open4goods.services.opendata;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import org.open4goods.services.opendata.model.ColumnManifest;
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
 * @param selector       tells whether a product is a row of this dataset (a product may belong to several datasets)
 * @param indexCount     products this dataset should hold in the index, counted at the start of the pass
 * @param columnDocs     unit, label and source of the columns, empty when the dataset does not describe them
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
            Predicate<Product> selector,
            LongSupplier indexCount,
            List<ColumnManifest> columnDocs,
            Function<Product, String[]> rowMapper) {
    }
