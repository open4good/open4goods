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

class ProductServiceTest {

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductService(repository);
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

        assertThat(dto.getAiReview()).isNotNull();
        assertThat(dto.getAiReview().review()).isEqualTo(holder.getReview());
    }

    @Test
    void getProductPropagatesNotFound() throws Exception {
        long gtin = 1L;
        when(repository.getById(gtin)).thenThrow(new ResourceNotFoundException("not found"));

        assertThatThrownBy(() -> service.getProduct(gtin, Locale.ENGLISH, Set.of()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReviewCallsRepository() throws Exception {
        long gtin = 42L;
        when(repository.getById(gtin)).thenReturn(new Product(gtin));

        service.createReview(gtin, "token", null);

        verify(repository).getById(gtin);
    }
}
