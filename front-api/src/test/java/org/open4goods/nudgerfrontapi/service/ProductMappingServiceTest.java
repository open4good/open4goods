package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;

class ProductMappingServiceTest {

    private ProductRepository repository;
    private ProductMappingService service;
    private ApiProperties apiProperties;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        apiProperties = new ApiProperties();
        apiProperties.setResourceRootPath("https://static.example");
        service = new ProductMappingService(repository, apiProperties);
    }

    @Test
    void getProductReturnsDtoWithAiReview() throws Exception {
        long gtin = 123L;
        Product product = new Product(gtin);
        AiReviewHolder holder = new AiReviewHolder();
        holder.setReview(new AiReview());
        holder.setEnoughData(true);
        holder.setTotalTokens(10);
        holder.setCreatedMs(5L);
        holder.setSources(Map.of());
        Localisable<String, AiReviewHolder> map = new Localisable<>();
        map.put("en", holder);
        product.setReviews(map);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("aiReview"), DomainLanguage.en);

        assertThat(dto.aiReview()).isNotNull();
        assertThat(dto.aiReview().review()).isEqualTo(holder.getReview());
    }

    @Test
    void getProductReturnsDtoWithBase() throws Exception {
        long gtin = 321L;
        Product product = new Product(gtin);
        product.setCreationDate(1L);
        product.setLastChange(2L);
        product.setCoverImagePath("/covers/main.jpg");
        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.setCountry("FR");
        product.setGtinInfos(gtinInfo);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("base"), DomainLanguage.en);

        assertThat(dto.base()).isNotNull();
        assertThat(dto.base().gtin()).isEqualTo(gtin);
        assertThat(dto.base().coverImagePath()).isEqualTo("https://static.example/covers/main.jpg");
        assertThat(dto.base().gtinInfo()).isNotNull();
        assertThat(dto.base().gtinInfo().countryCode()).isEqualTo("FR");
        assertThat(dto.base().gtinInfo().countryName()).isEqualTo("France");
        assertThat(dto.base().gtinInfo().countryFlagUrl()).isEqualTo("/images/flags/fr.png");
    }



    @Test
    void createReviewCallsRepository() throws Exception {
        long gtin = 42L;
        when(repository.getById(gtin)).thenReturn(new Product(gtin));

        service.createReview(gtin, "token", null);

        verify(repository).getById(gtin);
    }
}
