package org.open4goods.nudgerfrontapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.filter.FilterCriteria;
import org.open4goods.nudgerfrontapi.dto.filter.FilterOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@DataJpaTest
class SpecificationUtilsTest {

    @Entity
    static class Product {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
        double price;
        String name;
    }

    interface ProductRepo extends JpaRepository<Product, Long> {
        List<Product> findAll(Specification<Product> spec);
    }

    @Autowired
    private ProductRepo repo;

    @Test
    void specificationFiltersResults() {
        Product p1 = new Product();
        p1.price = 10.0;
        p1.name = "apple";
        repo.save(p1);
        Product p2 = new Product();
        p2.price = 30.0;
        p2.name = "banana";
        repo.save(p2);

        List<FilterCriteria> filters = List.of(new FilterCriteria("price", FilterOperator.LT, "20"));
        Specification<Product> spec = SpecificationUtils.fromFilters(filters);
        List<Product> results = repo.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name).isEqualTo("apple");
    }

    @Test
    void parserConvertsParameters() {
        Map<String, String[]> params = Map.of("filter[price_lt]", new String[]{"5"});
        List<FilterCriteria> criteria = FilterCriteriaParser.parse(params);
        assertThat(criteria).hasSize(1);
        assertThat(criteria.get(0).field()).isEqualTo("price");
        assertThat(criteria.get(0).operator()).isEqualTo(FilterOperator.LT);
        assertThat(criteria.get(0).value()).isEqualTo("5");
    }
}
