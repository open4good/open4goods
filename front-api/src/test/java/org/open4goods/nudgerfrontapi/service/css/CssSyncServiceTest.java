package org.open4goods.nudgerfrontapi.service.css;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService.MerchantDomain;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** Synchronisation scenarios with the simulated client: no network call. */
class CssSyncServiceTest {

    @TempDir
    Path tmp;

    private final Map<String, Product> index = new LinkedHashMap<>();
    private final SimulatedCssProductsClient client = new SimulatedCssProductsClient();
    private final List<Long> sleeps = new ArrayList<>();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private CssProperties properties;
    private TestClock clock;

    @BeforeEach
    void setUp() {
        properties = new CssProperties();
        properties.getSync().setEnabled(true);
        properties.getSync().setStateFile(tmp.resolve("state.json").toString());
        properties.getSync().setMaxRequestsPerSecond(0);
        clock = new TestClock(Instant.parse("2026-11-10T08:00:00Z"));
    }

    private CssSyncService service() {
        ProductRepository repository = mock(ProductRepository.class);
        when(repository.exportAll(anyString())).thenAnswer(i -> new ArrayList<>(index.values()).stream());
        VerticalsConfigService verticals = mock(VerticalsConfigService.class);
        VerticalConfig vertical = CssTestFixtures.vertical();
        when(verticals.getConfigsWithoutDefault(true)).thenReturn(List.of(vertical));
        MerchantDomainsService domains = mock(MerchantDomainsService.class);
        Function<String, MerchantDomain> resolver = CssTestFixtures.resolver();
        when(domains.domainResolver()).thenReturn(resolver);
        CssProductInputMapper mapper = new CssProductInputMapper(CssTestFixtures.eligibility(), properties, clock);
        return new CssSyncService(repository, verticals, domains, CssTestFixtures.eligibility(), mapper, client,
                properties, meters, clock, sleeps::add);
    }

    /** Valid GTIN-13 built from a 12 digit base. */
    private static String gtin(int n) {
        String base = String.format("400638%06d", n);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (base.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return base + (10 - sum % 10) % 10;
    }

    private Product put(int n, String title, boolean eligible) {
        Product p = eligible ? CssTestFixtures.product(CssTestFixtures.offer("shop-a.fr", 100), CssTestFixtures.offer("shop-b.fr", 120))
                : CssTestFixtures.product(CssTestFixtures.offer("shop-a.fr", 100));
        String gtin = gtin(n);
        p.setId(Long.parseLong(gtin));
        p.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, gtin);
        p.getNames().getDisplayName().put("fr", title);
        p.getNames().getUrl().put("fr", "produit-" + gtin);
        index.put(gtin, p);
        return p;
    }

    @Test
    void firstSendInsertsEveryEligibleProductOnceAndSurvivesRestart() throws IOException {
        put(1, "Acme X-100", true);
        put(2, "Acme X-200", true);
        put(3, "Acme seul marchand", false);

        CssSyncReport first = service().run();

        assertThat(first.inserted()).isEqualTo(2);
        assertThat(first.examined()).isEqualTo(3);
        assertThat(client.inserted()).hasSize(2);
        assertThat(meters.counter("css.products.sent", "type", "insert").count()).isEqualTo(2);
        assertThat(meters.find("css.products.sync.duration").timer()).isNotNull();

        // New instance, same state file: nothing is sent again
        clock.advance(Duration.ofHours(1));
        CssSyncReport second = service().run();
        assertThat(second.inserted()).isZero();
        assertThat(second.unchanged()).isEqualTo(2);
        assertThat(client.inserted()).hasSize(2);
    }

    @Test
    void modifiedProductIsInsertedAgain() throws IOException {
        put(1, "Acme X-100", true);
        service().run();

        put(1, "Acme X-100 nouvelle édition", true);
        clock.advance(Duration.ofHours(1));
        CssSyncReport report = service().run();

        assertThat(report.inserted()).isEqualTo(1);
        assertThat(client.inserted()).hasSize(2);
        assertThat(client.inserted().get(1).getAttributes().getTitle()).isEqualTo("Acme X-100 nouvelle édition");
    }

    @Test
    void noLongerEligibleOrRemovedProductIsDeleted() throws IOException {
        put(1, "Acme X-100", true);
        put(2, "Acme X-200", true);
        service().run();

        put(1, "Acme X-100", false); // loses its second merchant
        index.remove(gtin(2)); // disappears from the index
        clock.advance(Duration.ofHours(1));
        CssSyncReport report = service().run();

        assertThat(report.deleted()).isEqualTo(2);
        assertThat(client.deleted()).containsExactlyInAnyOrder(gtin(1), gtin(2));
        assertThat(meters.counter("css.products.deleted").count()).isEqualTo(2);

        // Deleted products are forgotten: nothing more to do
        assertThat(service().run().deleted()).isZero();
    }

    @Test
    void unchangedProductIsSentAgainBeforeExpirationOnly() throws IOException {
        put(1, "Acme X-100", true); // expiration: 3 days, resend-before: 24 h
        service().run();

        clock.advance(Duration.ofHours(24));
        assertThat(service().run().renewed()).isZero();

        clock.advance(Duration.ofHours(25)); // 49 h: expires in 23 h
        CssSyncReport report = service().run();
        assertThat(report.renewed()).isEqualTo(1);
        assertThat(report.inserted()).isZero();
        assertThat(client.inserted()).hasSize(2);
        assertThat(client.inserted().get(1).getAttributes().getExpirationDate().getSeconds())
                .isGreaterThan(client.inserted().get(0).getAttributes().getExpirationDate().getSeconds());

        // Right after a renewal nothing is due
        clock.advance(Duration.ofHours(1));
        assertThat(service().run().renewed()).isZero();
    }

    @Test
    void productLosingItsOffersIsNotRenewedAndExpiresByItself() throws IOException {
        put(1, "Acme X-100", true);
        service().run();

        put(1, "Acme X-100", false);
        clock.advance(Duration.ofDays(4)); // already expired at Google
        CssSyncReport report = service().run();

        assertThat(report.expired()).isEqualTo(1);
        assertThat(report.deleted()).isZero();
        assertThat(client.deleted()).isEmpty();
        assertThat(meters.counter("css.products.expired").count()).isEqualTo(1);
    }

    @Test
    void partialFailureThenResumeOnlySendsWhatFailed() throws IOException {
        put(1, "A", true);
        put(2, "B", true);
        put(3, "C", true);
        client.failFor(gtin(2));

        CssSyncReport first = service().run();
        assertThat(first.inserted()).isEqualTo(2);
        assertThat(first.rejected()).containsEntry("SIMULATED_FAILURE", 1L);
        assertThat(meters.counter("css.products.rejected", "reason", "SIMULATED_FAILURE").count()).isEqualTo(1);

        client.stopFailing();
        clock.advance(Duration.ofMinutes(10));
        CssSyncReport second = service().run();

        assertThat(second.inserted()).isEqualTo(1);
        assertThat(second.unchanged()).isEqualTo(2);
        assertThat(client.inserted()).hasSize(3);
    }

    @Test
    void tooManyConsecutiveFailuresAbortTheRun() throws IOException {
        properties.getSync().setMaxConsecutiveFailures(2);
        for (int i = 1; i <= 5; i++) {
            put(i, "P" + i, true);
            client.failFor(gtin(i));
        }

        CssSyncReport report = service().run();

        assertThat(report.aborted()).isTrue();
        assertThat(report.rejectedCount()).isEqualTo(2);
    }

    @Test
    void capLimitsTheCallsPerRunAndTheNextRunContinues() throws IOException {
        properties.getSync().setMaxProductsPerRun(2);
        for (int i = 1; i <= 5; i++) {
            put(i, "P" + i, true);
        }

        CssSyncReport first = service().run();
        assertThat(first.inserted()).isEqualTo(2);
        assertThat(first.capped()).isTrue();

        clock.advance(Duration.ofMinutes(10));
        CssSyncReport second = service().run();
        assertThat(second.inserted()).isEqualTo(2);
        assertThat(client.inserted()).hasSize(4);
    }

    @Test
    void verticalFilterSkipsOtherVerticals() throws IOException {
        put(1, "A", true);
        properties.getSync().getVerticals().add("other");

        CssSyncReport report = service().run();

        assertThat(report.examined()).isZero();
        assertThat(client.inserted()).isEmpty();
    }

    @Test
    void rateLimitSpacesTheCalls() throws IOException {
        properties.getSync().setMaxRequestsPerSecond(2); // 500 ms between two calls
        for (int i = 1; i <= 3; i++) {
            put(i, "P" + i, true);
        }

        service().run();

        // The test clock does not move while sending: the 2nd and 3rd calls wait the whole interval
        assertThat(sleeps).containsExactly(500L, 500L);
    }

    /** Clock moved by hand. */
    private static final class TestClock extends Clock {
        private Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
