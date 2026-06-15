package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the dedicated YAML billing catalog is bound.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "management.health.redis.enabled=false"
})
class BillingCatalogPropertiesTest {

    @Autowired
    private BillingCatalogProperties catalog;

    @Test
    void bindsPriceFacetAndBillingPlans() {
        assertThat(catalog.getFacets()).containsKey("product.price");
        assertThat(catalog.getFacets().get("product.price").getCredits()).isEqualTo(5);
        assertThat(catalog.getBilling().getPacks()).containsKeys("starter", "growth", "scale");
        assertThat(catalog.getBilling().getSubscriptions()).containsKeys("starter", "growth", "scale");
    }
}
