package org.open4goods.b2bapi.service;

import org.open4goods.b2bapi.dto.ProductSimpleDto;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProductMappingService {

    private final ProductRepository repository;

    public ProductMappingService(ProductRepository repository) {
        this.repository = repository;
    }

    // Retrieves a paginated list of simplified product DTOs, optionally filtered by vertical presence and minimum offers.
    public Page<ProductSimpleDto> getProducts(Pageable pageable, boolean withVerticalOnly, Integer minOffers) {
        SearchHits<Product> hits = (withVerticalOnly || minOffers != null)
                ? searchWithFilters(pageable, withVerticalOnly, minOffers)
                : repository.get(pageable);

        if (hits == null) {
            return Page.empty(pageable);
        }

        List<ProductSimpleDto> dtos = hits.stream()
                .map(hit -> mapToDto(hit.getContent()))
                .toList();

        return new PageImpl<>(dtos, pageable, hits.getTotalHits());
    }

    // Builds and executes a filtered search query using optional filters.
    private SearchHits<Product> searchWithFilters(Pageable pageable, boolean withVerticalOnly, Integer minOffers) {
        Criteria criteria = buildCriteria(withVerticalOnly, minOffers);

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(new CriteriaQuery(criteria))
                .withPageable(pageable)
                .build();

        return repository.search(query, ProductRepository.MAIN_INDEX_NAME);
    }

    // Dynamically constructs search criteria based on optional flags.
    private Criteria buildCriteria(boolean withVerticalOnly, Integer minOffers) {
        Criteria criteria = new Criteria();

        if (withVerticalOnly) {
            criteria = criteria.and(new Criteria("vertical").exists());
        }

        if (minOffers != null) {
            criteria = criteria.and(new Criteria("offersCount").greaterThanEqual(minOffers));
        }

        return criteria;
    }

    private ProductSimpleDto mapToDto(Product product) {
        return new ProductSimpleDto(
                product.getId(),
                product.getVertical(),
                product.getOffersCount()
        );
    }
}
