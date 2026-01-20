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
 * Tests KPI aggregation for Awin feed service.
 */
class AwinFeedServiceTest {

    @Test
    void shouldAggregateAwinKpis() throws Exception {
        File fixture = Path.of("src/test/resources/awin/awin-kpis.json").toFile();
        RemoteFileCachingService cachingService = new StubRemoteFileCachingService(fixture);
        AwinFeedService service = new AwinFeedService(
                new FeedConfiguration(),
                cachingService,
                new DataSourceConfigService("target"),
                new SerialisationService(),
                "123",
                "token"
        );

        AffiliationKpis kpis = service.getKpis(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 7));

        assertThat(kpis.clicks()).isEqualTo(14);
        assertThat(kpis.impressions()).isEqualTo(150);
        assertThat(kpis.transactionsTotal()).isEqualTo(4);
        assertThat(kpis.transactionsConfirmed()).isEqualTo(3);
        assertThat(kpis.transactionsPending()).isEqualTo(1);
        assertThat(kpis.commissionTotal()).isEqualByComparingTo(new BigDecimal("16.25"));
        assertThat(kpis.turnoverTotal()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(kpis.breakdown()).containsKeys("Shop A", "Shop B");
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
