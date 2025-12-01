package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

class AttributeRealtimeAggregationServiceTest {

    @Test
    void onProductFiltersEprelBrandUsingExclusions() throws AggregationSkipException {
        VerticalsConfigService verticalConfigService = Mockito.mock(VerticalsConfigService.class);
        BrandService brandService = Mockito.mock(BrandService.class);
        IcecatService icecatService = Mockito.mock(IcecatService.class);
        Logger logger = Mockito.mock(Logger.class);

        AttributeRealtimeAggregationService service = new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger, icecatService);

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setAttributesConfig(new AttributesConfig());
        verticalConfig.getBrandsExclusion().add("BANNED BRAND");

        Product product = new Product(123L);
        EprelProduct eprelProduct = new EprelProduct();
        eprelProduct.setSupplierOrTrademark("Banned Brand");
        eprelProduct.setModelIdentifier("ModelX123");
        product.setEprelDatas(eprelProduct);

        service.onProduct(product, verticalConfig);

        assertThat(product.brand()).isNull();
        assertThat(product.getAkaBrands()).isEmpty();
        assertThat(product.isExcluded()).isTrue();
        assertThat(product.getExcludedCauses()).contains("missing_brand");
        assertThat(product.model()).isEqualTo("ModelX123");
    }
}
