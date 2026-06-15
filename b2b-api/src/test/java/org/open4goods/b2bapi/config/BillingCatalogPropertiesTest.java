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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.CreditBucketRepository creditBucketRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.services.productrepository.services.ProductRepository productRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.StripeEventRepository stripeEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.StripeCustomerRepository stripeCustomerRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.StripeCheckoutSessionRepository stripeCheckoutSessionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.StripeSubscriptionRepository stripeSubscriptionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.InvoiceRepository invoiceRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.OrganizationRepository organizationRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.service.CreditGrantService creditGrantService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.UserRepository userRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.OrganizationMemberRepository organizationMemberRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.CreditTransactionRepository creditTransactionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.ApiKeyRepository apiKeyRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.UsageEventRepository usageEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.repository.AdminAuditEventRepository adminAuditEventRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.open4goods.b2bapi.service.UsageStreamService usageStreamService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

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
