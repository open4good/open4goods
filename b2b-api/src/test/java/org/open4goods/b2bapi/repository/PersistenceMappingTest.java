package org.open4goods.b2bapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.open4goods.b2bapi.B2bApiApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Verifies the Flyway schema and Hibernate mappings against Postgres.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
class PersistenceMappingTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void postgresProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private AdminAuditEventRepository adminAuditEventRepository;

    @Test
    void flywayCreatesExpectedTablesAndHibernateMappingsValidate() {
        final Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                      'organizations',
                      'users',
                      'organization_members',
                      'api_keys',
                      'credit_buckets',
                      'credit_transactions',
                      'stripe_customers',
                      'stripe_checkout_sessions',
                      'stripe_subscriptions',
                      'invoices',
                      'stripe_events',
                      'usage_events',
                      'admin_audit_events')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(13);
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSizeGreaterThanOrEqualTo(13);
        assertThat(creditBucketRepository).isNotNull();
        assertThat(creditTransactionRepository).isNotNull();
        assertThat(adminAuditEventRepository).isNotNull();
    }

    @Test
    void flywayCreatesBillingCorrectnessPartialIndexes() {
        final Map<String, Boolean> indexPresence = jdbcTemplate.query("""
                select indexname, indexdef like '%WHERE (role = ''OWNER''::text)%'
                    or indexdef like '%WHERE ((role)::text = ''OWNER''::text)%'
                    or indexdef like '%WHERE role = ''OWNER''%' as owner_index,
                    indexdef like '%WHERE (type = ''DEBIT''::text)%'
                    or indexdef like '%WHERE ((type)::text = ''DEBIT''::text)%'
                    or indexdef like '%WHERE type = ''DEBIT''%' as debit_index
                from pg_indexes
                where schemaname = 'public'
                  and indexname in ('ux_organization_members_one_owner', 'ux_credit_tx_debit_request')
                """, resultSet -> {
            final Map<String, Boolean> indexes = new java.util.HashMap<>();
            while (resultSet.next()) {
                indexes.put(resultSet.getString("indexname") + ".owner", resultSet.getBoolean("owner_index"));
                indexes.put(resultSet.getString("indexname") + ".debit", resultSet.getBoolean("debit_index"));
            }
            return indexes;
        });

        assertThat(indexPresence).containsEntry("ux_organization_members_one_owner.owner", true);
        assertThat(indexPresence).containsEntry("ux_credit_tx_debit_request.debit", true);
    }
}
