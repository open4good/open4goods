package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexationProperties;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Unit tests for {@link ProductUrlService}.
 */
class ProductUrlServiceTest {

    @Test
    void resolveProductUrlBuildsFromVerticalAndSlug() throws Exception {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        VerticalsConfigService verticalsConfigService = Mockito.mock(VerticalsConfigService.class);
        CategoryMappingService categoryMappingService = Mockito.mock(CategoryMappingService.class);

        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        properties.setSiteBaseUrl("https://nudger.fr");

        Product product = new Product();
        product.setId(42L);
        product.setVertical("tv");
        ProductTexts texts = new ProductTexts();
        Localisable<String, String> url = new Localisable<>();
        url.put("fr", "smart-tv");
        texts.setUrl(url);
        product.setNames(texts);

        when(productRepository.getByIdWithoutEmbedding(42L)).thenReturn(product);
        when(categoryMappingService.toVerticalConfigDto(Mockito.any(), Mockito.eq(DomainLanguage.fr)))
                .thenReturn(new VerticalConfigDto("tv", true, true, null, null, 1, null, null, null, null,
                        null, null, null, "televiseurs", List.of(), null, null, null, null));

        ProductUrlService service = new ProductUrlService(productRepository, verticalsConfigService, categoryMappingService, properties);

        String result = service.resolveProductUrl(42L, DomainLanguage.fr);

        assertThat(result).isEqualTo("https://nudger.fr/televiseurs/smart-tv");
    }

    @Test
    void resolveProductUrlFallsBackToVerticalConfigWhenCategoryDtoHasNoUrl() throws Exception {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        VerticalsConfigService verticalsConfigService = Mockito.mock(VerticalsConfigService.class);
        CategoryMappingService categoryMappingService = Mockito.mock(CategoryMappingService.class);

        GoogleIndexationProperties properties = new GoogleIndexationProperties();
        properties.setSiteBaseUrl("https://nudger.fr/");

        Product product = new Product();
        product.setId(8431312260509L);
        product.setVertical("air-conditioners");
        ProductTexts texts = new ProductTexts();
        Localisable<String, String> url = new Localisable<>();
        url.put("fr", "8431312260509-climatisation-midea-mmcs12hrn8qrd0");
        texts.setUrl(url);
        product.setNames(texts);

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("air-conditioners");
        ProductI18nElements frI18n = new ProductI18nElements();
        frI18n.setVerticalHomeUrl("/climatiseurs/");
        verticalConfig.getI18n().put("fr", frI18n);

        when(productRepository.getByIdWithoutEmbedding(8431312260509L)).thenReturn(product);
        when(verticalsConfigService.getConfigById("air-conditioners")).thenReturn(verticalConfig);
        when(categoryMappingService.toVerticalConfigDto(verticalConfig, DomainLanguage.fr)).thenReturn(null);

        ProductUrlService service = new ProductUrlService(productRepository, verticalsConfigService, categoryMappingService, properties);

        String result = service.resolveProductUrl(8431312260509L, DomainLanguage.fr);

        assertThat(result).isEqualTo(
                "https://nudger.fr/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0");
    }
}
