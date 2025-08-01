package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.services.productrepository.services.ProductRepository;

class ProductMappingServiceTest {

    private ProductRepository repository;
    private ProductMappingService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductMappingService(repository);
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

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("aiReview"));

        assertThat(dto.aiReview()).isNotNull();
        assertThat(dto.aiReview().review()).isEqualTo(holder.getReview());
    }

    @Test
    void getProductReturnsDtoWithBase() throws Exception {
        long gtin = 321L;
        Product product = new Product(gtin);
        product.setCreationDate(1L);
        product.setLastChange(2L);

        when(repository.getById(gtin)).thenReturn(product);

        ProductDto dto = service.getProduct(gtin, Locale.ENGLISH, Set.of("base"));

        assertThat(dto.base()).isNotNull();
        assertThat(dto.base().gtin()).isEqualTo(gtin);
    }



    @Test
    void createReviewCallsRepository() throws Exception {
        long gtin = 42L;
        when(repository.getById(gtin)).thenReturn(new Product(gtin));

        service.createReview(gtin, "token", null);

        verify(repository).getById(gtin);
    }
}
