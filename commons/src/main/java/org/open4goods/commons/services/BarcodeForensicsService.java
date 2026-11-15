package org.open4goods.commons.services;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.product.BarcodeForensics;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;

import java.util.AbstractMap.SimpleEntry;

/**
 * Derives rich forensic metadata from a raw barcode string.
 *
 * <p>Combines check-digit validation (via {@link BarcodeValidationService}) with
 * GS1 prefix lookup (via {@link Gs1PrefixService}) to produce a {@link BarcodeForensics}
 * result that is safe to cache and expose publicly.
 *
 * <p>The issuing-country field reflects the GS1 member organisation that manages
 * the prefix range — it is <em>not</em> the country of manufacture.
 */
public class BarcodeForensicsService {

    private final BarcodeValidationService barcodeValidationService;
    private final Gs1PrefixService gs1PrefixService;

    public BarcodeForensicsService(
            final BarcodeValidationService barcodeValidationService,
            final Gs1PrefixService gs1PrefixService) {
        this.barcodeValidationService = barcodeValidationService;
        this.gs1PrefixService = gs1PrefixService;
    }

    /**
     * Analyses a raw barcode string and returns all derivable forensic metadata.
     *
     * @param rawBarcode the barcode as supplied by the caller (may be any length)
     * @return a fully populated {@link BarcodeForensics} result; {@code valid} is
     *         {@code false} and most fields are null when the check digit is wrong
     */
    public BarcodeForensics analyze(final String rawBarcode) {
        if (StringUtils.isBlank(rawBarcode)) {
            return invalid(rawBarcode);
        }

        final SimpleEntry<BarcodeType, String> sanitized = barcodeValidationService.sanitize(rawBarcode.trim());
        if (sanitized == null || sanitized.getKey() == BarcodeType.UNKNOWN) {
            return invalid(rawBarcode);
        }

        final BarcodeType type = sanitized.getKey();
        final String normalized = sanitized.getValue();

        final String gs1Prefix = normalized.length() >= 3 ? normalized.substring(0, 3) : null;
        final String issuingCountryCode = gs1Prefix != null ? gs1PrefixService.detectCountry(normalized) : null;
        final Gs1Class gs1Class = barcodeValidationService.classifyGs1Class(normalized);

        final Integer packagingIndicator = type == BarcodeType.GTIN_14
                ? Character.getNumericValue(normalized.charAt(0)) : null;

        final String isbnRegistrationGroup = extractIsbnRegistrationGroup(normalized, type);

        final String normalizedGtin14 = org.apache.commons.lang3.StringUtils.leftPad(normalized, 14, '0');
        final String normalizedGtin13 = deriveGtin13(normalized, type);

        final int checkDigit = Character.getNumericValue(normalized.charAt(normalized.length() - 1));

        return new BarcodeForensics(
                true,
                type,
                gs1Prefix,
                issuingCountryCode,
                gs1Class,
                packagingIndicator,
                isbnRegistrationGroup,
                normalizedGtin14,
                normalizedGtin13,
                checkDigit);
    }

    private BarcodeForensics invalid(final String rawBarcode) {
        final Integer checkDigit = (rawBarcode != null && !rawBarcode.isBlank())
                ? safeLastDigit(rawBarcode.trim()) : null;
        return new BarcodeForensics(false, BarcodeType.UNKNOWN, null, null, Gs1Class.UNKNOWN,
                null, null, null, null, checkDigit);
    }

    private String extractIsbnRegistrationGroup(final String normalized, final BarcodeType type) {
        if (type != BarcodeType.ISBN_13) {
            return null;
        }
        // ISBN-13 starts with 978 or 979; registration group follows the 3-digit prefix
        if (normalized.length() < 4) {
            return null;
        }
        // Single-character registration group is the 4th digit
        return String.valueOf(normalized.charAt(3));
    }

    private String deriveGtin13(final String normalized, final BarcodeType type) {
        if (type == BarcodeType.GTIN_13 || type == BarcodeType.ISBN_13) {
            return normalized;
        }
        if (type == BarcodeType.GTIN_14 && normalized.charAt(0) == '0') {
            return normalized.substring(1);
        }
        return null;
    }

    private Integer safeLastDigit(final String value) {
        if (value.isEmpty()) {
            return null;
        }
        final char last = value.charAt(value.length() - 1);
        return Character.isDigit(last) ? Character.getNumericValue(last) : null;
    }
}
