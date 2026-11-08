package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.BarcodeForensics;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

class IdentityAggregationServiceTest
{

    @Test
    void onDataFragmentCapturesRawGtinBeforeSanitization() throws Exception
    {
        Gs1PrefixService gs1Service = Mockito.mock(Gs1PrefixService.class);
        BarcodeValidationService validationService = Mockito.mock(BarcodeValidationService.class);
        BarcodeForensicsService forensicsService = Mockito.mock(BarcodeForensicsService.class);
        IdentityAggregationService service = new IdentityAggregationService(
                LoggerFactory.getLogger(IdentityAggregationServiceTest.class),
                gs1Service,
                validationService,
                forensicsService
        );

        DataFragment fragment = new DataFragment();
        fragment.getReferentielAttributes().put(ReferentielKey.GTIN, "0123456789012");

        Product product = new Product();
        VerticalConfig verticalConfig = new VerticalConfig();

        Mockito.when(validationService.sanitize("123456789012"))
                .thenReturn(new SimpleEntry<>(BarcodeType.GTIN_12, "123456789012"));
        Mockito.when(gs1Service.detectCountry("123456789012"))
                .thenReturn("FR");
        Mockito.when(forensicsService.analyze("123456789012"))
                .thenReturn(new BarcodeForensics(true, BarcodeType.GTIN_12, "123", "FR",
                        Gs1Class.GTIN, null, null, "00123456789012", "123456789012", 2));

        service.onDataFragment(fragment, product, verticalConfig);

        assertThat(product.getGtinInfos().getGtinStrings())
                .containsExactly("0123456789012");
        assertThat(product.gtin()).isEqualTo("123456789012");
    }

    /**
     * GOU-1841: an ISBN-10 fragment aggregated on the ISBN-13 product keeps the
     * ISBN-13 id and referential GTIN.
     */
    @Test
    void isbn10FragmentKeepsTheIsbn13IdentityOfTheProduct() throws Exception
    {
        BarcodeValidationService validationService = new BarcodeValidationService();
        BarcodeForensicsService forensicsService = Mockito.mock(BarcodeForensicsService.class);
        Mockito.when(forensicsService.analyze(Mockito.anyString()))
                .thenReturn(new BarcodeForensics(false, BarcodeType.UNKNOWN, null, null, null, null, null, null, null, 0));
        IdentityAggregationService service = new IdentityAggregationService(
                LoggerFactory.getLogger(IdentityAggregationServiceTest.class),
                Mockito.mock(Gs1PrefixService.class),
                validationService,
                forensicsService
        );

        Product product = new Product(9782726862216L);
        product.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, "9782726862216");
        DataFragment fragment = new DataFragment();
        fragment.getReferentielAttributes().put(ReferentielKey.GTIN, "2726862217");

        service.onDataFragment(fragment, product, new VerticalConfig());

        assertThat(product.getId()).isEqualTo(9782726862216L);
        assertThat(product.gtin()).isEqualTo("9782726862216");
        assertThat(product.getGtinInfos().getUpcType()).isEqualTo(BarcodeType.ISBN_13);
        assertThat(validationService.toProductId(fragment.gtin())).isEqualTo(product.getId());
    }
}
