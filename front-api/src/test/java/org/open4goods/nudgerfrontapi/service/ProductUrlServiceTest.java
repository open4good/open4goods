package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductTexts;
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

        when(productRepository.getById(42L)).thenReturn(product);
        when(categoryMappingService.toVerticalConfigDto(Mockito.any(), Mockito.eq(DomainLanguage.fr)))
                .thenReturn(new VerticalConfigDto("tv", true, true, null, null, 1, null, null, null, null,
                        null, null, "televiseurs", null, null, null));

        ProductUrlService service = new ProductUrlService(productRepository, verticalsConfigService, categoryMappingService, properties);

        String result = service.resolveProductUrl(42L, DomainLanguage.fr);

        assertThat(result).isEqualTo("https://nudger.fr/televiseurs/smart-tv");
    }
}
