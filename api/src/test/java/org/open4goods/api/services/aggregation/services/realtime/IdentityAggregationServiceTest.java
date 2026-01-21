package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.BarcodeType;
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
        IdentityAggregationService service = new IdentityAggregationService(
                LoggerFactory.getLogger(IdentityAggregationServiceTest.class),
                gs1Service,
                validationService
        );

        DataFragment fragment = new DataFragment();
        fragment.getReferentielAttributes().put(ReferentielKey.GTIN, "0123456789012");

        Product product = new Product();
        VerticalConfig verticalConfig = new VerticalConfig();

        Mockito.when(validationService.sanitize("123456789012"))
                .thenReturn(new SimpleEntry<>(BarcodeType.GTIN_12, "123456789012"));
        Mockito.when(gs1Service.detectCountry("123456789012"))
                .thenReturn("FR");

        service.onDataFragment(fragment, product, verticalConfig);

        assertThat(product.getGtinInfos().getGtinStrings())
                .containsExactly("0123456789012");
        assertThat(product.gtin()).isEqualTo("123456789012");
    }
}
