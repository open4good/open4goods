package org.open4goods.model.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GtinInfoTest
{

    @Test
    void addGtinStringDeduplicatesAndTrims()
    {
        GtinInfo gtinInfo = new GtinInfo();

        gtinInfo.addGtinString(" 0123456789012 ");
        gtinInfo.addGtinString("0123456789012");

        assertThat(gtinInfo.getGtinStrings()).containsExactly("0123456789012");
    }

    @Test
    void detectOriginalBarcodeTypeUsesStrippedLengthWhenLeadingZeroPresent()
    {
        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.addGtinString("0123456789012");

        assertThat(gtinInfo.detectOriginalBarcodeType())
                .contains(BarcodeType.GTIN_12);
    }

    @Test
    void detectOriginalBarcodeTypeUsesSmallestRawLength()
    {
        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.addGtinString("12345670");
        gtinInfo.addGtinString("12345678901234");

        assertThat(gtinInfo.detectOriginalBarcodeType())
                .contains(BarcodeType.GTIN_8);
    }
}
