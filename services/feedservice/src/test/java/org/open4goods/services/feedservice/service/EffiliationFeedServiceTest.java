package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.feedservice.dto.AffiliationKpis;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Tests KPI aggregation for Effiliation feed service.
 */
class EffiliationFeedServiceTest {

    @Test
    void shouldAggregateEffiliationKpis() throws Exception {
        File fixture = Path.of("src/test/resources/effiliation/effiliation-kpis.json").toFile();
        RemoteFileCachingService cachingService = new StubRemoteFileCachingService(fixture);
        EffiliationFeedService service = new EffiliationFeedService(
                new FeedConfiguration(),
                cachingService,
                new DataSourceConfigService("target"),
                new SerialisationService(),
                "token"
        );

        AffiliationKpis kpis = service.getKpis(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 7));

        assertThat(kpis.clicks()).isEqualTo(10);
        assertThat(kpis.impressions()).isEqualTo(90);
        assertThat(kpis.transactionsTotal()).isEqualTo(3);
        assertThat(kpis.transactionsConfirmed()).isEqualTo(2);
        assertThat(kpis.transactionsPending()).isEqualTo(1);
        assertThat(kpis.commissionTotal()).isEqualByComparingTo(new BigDecimal("6.50"));
        assertThat(kpis.turnoverTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(kpis.breakdown()).containsKeys("Partner A", "Partner B");
    }

    private static final class StubRemoteFileCachingService extends RemoteFileCachingService {

        private final File fixture;

        private StubRemoteFileCachingService(File fixture) {
            super("target");
            this.fixture = fixture;
        }

        @Override
        public File getResource(String url, Integer refreshInDays) {
            return fixture;
        }
    }
}
