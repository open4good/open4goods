package org.open4goods.b2bapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the scaffolded application context starts.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "management.health.redis.enabled=false"
})
class B2bApiApplicationTests {

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

    @Test
    void contextLoads() {
    }
}

