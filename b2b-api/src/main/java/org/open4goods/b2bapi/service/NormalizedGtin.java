package org.open4goods.b2bapi.service;

import org.open4goods.model.product.BarcodeType;

/**
 * Validated and normalized GTIN values used by the price facet workflow.
 *
 * @param value normalized barcode value returned by the canonical validator
 * @param productId numeric Elasticsearch product id
 * @param barcodeType detected barcode type
 */
public record NormalizedGtin(
        String value,
        Long productId,
        BarcodeType barcodeType) {
}
