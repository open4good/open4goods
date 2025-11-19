package org.open4goods.services.opendata;
import java.io.File;
import java.util.Set;
import java.util.function.Function;

import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;

public record DatasetDefinition(
            String filename,
            File zipFile,
            String[] header,
            Set<BarcodeType> barcodeTypes,
            Set<String> requiredFields,
            Function<Product, String[]> rowMapper) {
    }