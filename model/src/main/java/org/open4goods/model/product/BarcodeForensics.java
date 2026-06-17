package org.open4goods.model.product;

/**
 * Immutable result of a forensic GTIN analysis.
 *
 * @param valid whether the check digit is valid
 * @param type the detected barcode type
 * @param gs1Prefix the 3-digit GS1 prefix (first 3 digits of the normalized GTIN)
 * @param issuingCountryCode ISO 3166-1 alpha-2 code of the GS1 member organisation (not country of manufacture)
 * @param gs1Class the GS1 product/numbering class derived from the prefix range
 * @param packagingIndicator first digit of a GTIN-14 (1-8 = trade unit, 9 = variable measure); null for shorter GTINs
 * @param isbnRegistrationGroup registration group element of an ISBN-13; null for non-book GTINs
 * @param normalizedGtin14 zero-padded 14-digit canonical form
 * @param normalizedGtin13 13-digit form (null when GTIN-14 indicator is non-zero and the number is not ISBN)
 * @param checkDigit the final check digit of the input barcode
 */
public record BarcodeForensics(
        boolean valid,
        BarcodeType type,
        String gs1Prefix,
        String issuingCountryCode,
        Gs1Class gs1Class,
        Integer packagingIndicator,
        String isbnRegistrationGroup,
        String normalizedGtin14,
        String normalizedGtin13,
        Integer checkDigit) {
}
