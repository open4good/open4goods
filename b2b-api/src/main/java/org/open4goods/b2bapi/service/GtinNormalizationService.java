package org.open4goods.b2bapi.service;

import java.util.AbstractMap.SimpleEntry;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.product.BarcodeType;
import org.springframework.stereotype.Service;

/**
 * Validates public GTIN inputs and converts them to repository lookup ids.
 */
@Service
public class GtinNormalizationService {

    private final BarcodeValidationService barcodeValidationService;

    public GtinNormalizationService(BarcodeValidationService barcodeValidationService) {
        this.barcodeValidationService = barcodeValidationService;
    }

    /**
     * Validates a GTIN and returns both the canonical string and Long lookup id.
     *
     * @param rawGtin raw path variable supplied by the caller
     * @return normalized GTIN values
     */
    public NormalizedGtin normalize(String rawGtin) {
        if (rawGtin == null || !rawGtin.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            throw new InvalidGtinException("GTIN must contain 8, 12, 13, or 14 digits.");
        }
        SimpleEntry<BarcodeType, String> sanitized = barcodeValidationService.sanitize(rawGtin);
        if (sanitized == null || sanitized.getKey() == BarcodeType.UNKNOWN || sanitized.getValue() == null) {
            throw new InvalidGtinException("GTIN checksum is invalid.");
        }
        try {
            return new NormalizedGtin(
                    sanitized.getValue(),
                    Long.parseLong(sanitized.getValue()),
                    sanitized.getKey());
        } catch (NumberFormatException ex) {
            throw new InvalidGtinException("GTIN cannot be converted to the product lookup id.");
        }
    }
}
