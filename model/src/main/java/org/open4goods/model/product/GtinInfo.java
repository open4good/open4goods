package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GtinInfo
{

    private BarcodeType upcType;

    /**
     * Manufacturer country, from the gtin
     */
    private String country;

    /**
     * Raw GTINs encountered before any padding or sanitization.
     */
    private List<String> gtinStrings = new ArrayList<>();

    /** 3-digit GS1 prefix (first 3 digits of the normalized GTIN). */
    private String gs1Prefix;

    /** GS1 product/numbering class derived from the prefix range. */
    private Gs1Class gs1Class;

    /** First digit of a GTIN-14 packaging indicator; null for shorter GTINs. */
    private Integer packagingIndicator;

    /** Registration group element of an ISBN-13; null for non-book GTINs. */
    private String isbnRegistrationGroup;

    /** Zero-padded 14-digit canonical form of the GTIN. */
    private String normalizedGtin14;

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public BarcodeType getUpcType()
    {
        return upcType;
    }

    public void setUpcType(BarcodeType upcType)
    {
        this.upcType = upcType;
    }

    public List<String> getGtinStrings()
    {
        return gtinStrings;
    }

    public void setGtinStrings(List<String> gtinStrings)
    {
        this.gtinStrings = gtinStrings == null ? new ArrayList<>() : new ArrayList<>(gtinStrings);
    }

    /**
     * Adds a raw GTIN string if it is non-blank and not already present.
     *
     * @param gtin the raw GTIN string to store
     */
    public void addGtinString(String gtin)
    {
        if (gtin == null) {
            return;
        }
        String trimmed = gtin.trim();
        if (trimmed.isBlank()) {
            return;
        }
        if (!gtinStrings.contains(trimmed)) {
            gtinStrings.add(trimmed);
        }
    }

    public String getGs1Prefix() {
        return gs1Prefix;
    }

    public void setGs1Prefix(final String gs1Prefix) {
        this.gs1Prefix = gs1Prefix;
    }

    public Gs1Class getGs1Class() {
        return gs1Class;
    }

    public void setGs1Class(final Gs1Class gs1Class) {
        this.gs1Class = gs1Class;
    }

    public Integer getPackagingIndicator() {
        return packagingIndicator;
    }

    public void setPackagingIndicator(final Integer packagingIndicator) {
        this.packagingIndicator = packagingIndicator;
    }

    public String getIsbnRegistrationGroup() {
        return isbnRegistrationGroup;
    }

    public void setIsbnRegistrationGroup(final String isbnRegistrationGroup) {
        this.isbnRegistrationGroup = isbnRegistrationGroup;
    }

    public String getNormalizedGtin14() {
        return normalizedGtin14;
    }

    public void setNormalizedGtin14(final String normalizedGtin14) {
        this.normalizedGtin14 = normalizedGtin14;
    }

    /**
     * Detects the original barcode type from the smallest raw GTIN length, while
     * considering leading-zero variants that may have been padded.
     *
     * @return the inferred barcode type, if any
     */
    public Optional<BarcodeType> detectOriginalBarcodeType()
    {
        if (gtinStrings == null || gtinStrings.isEmpty()) {
            return Optional.empty();
        }

        int minLength = Integer.MAX_VALUE;
        int minStrippedLength = Integer.MAX_VALUE;
        boolean hasLeadingZero = false;

        for (String gtin : gtinStrings) {
            if (gtin == null) {
                continue;
            }
            String trimmed = gtin.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            minLength = Math.min(minLength, trimmed.length());
            if (trimmed.startsWith("0")) {
                hasLeadingZero = true;
            }
            String stripped = stripLeadingZeros(trimmed);
            if (!stripped.isEmpty()) {
                minStrippedLength = Math.min(minStrippedLength, stripped.length());
            }
        }

        if (minLength == Integer.MAX_VALUE) {
            return Optional.empty();
        }

        int candidateLength = minLength;
        if (hasLeadingZero && minStrippedLength < candidateLength && isKnownGtinLength(minStrippedLength)) {
            candidateLength = minStrippedLength;
        }

        return barcodeTypeForLength(candidateLength);
    }

    private String stripLeadingZeros(String value)
    {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '0') {
            index++;
        }
        return index == value.length() ? "" : value.substring(index);
    }

    private boolean isKnownGtinLength(int length)
    {
        return length == 8 || length == 12 || length == 13 || length == 14;
    }

    private Optional<BarcodeType> barcodeTypeForLength(int length)
    {
        switch (length) {
            case 8:
                return Optional.of(BarcodeType.GTIN_8);
            case 12:
                return Optional.of(BarcodeType.GTIN_12);
            case 13:
                return Optional.of(BarcodeType.GTIN_13);
            case 14:
                return Optional.of(BarcodeType.GTIN_14);
            default:
                return Optional.empty();
        }
    }

}
