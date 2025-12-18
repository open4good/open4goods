package org.open4goods.nudgerfrontapi.service.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.ShareResolutionProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareExtractionDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionRequestDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionStatus;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchHit;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchVerticalGroup;

/**
 * Unit tests for {@link ShareResolutionService}.
 */
class ShareResolutionServiceTest {

    private final Executor sameThreadExecutor = Runnable::run;

    @Test
    void createResolutionResolvesCandidates() {
        ShareExtractionService extractionService = mock(ShareExtractionService.class);
        SearchService searchService = mock(SearchService.class);
        ProductMappingService mappingService = mock(ProductMappingService.class);

        ShareResolutionProperties properties = new ShareResolutionProperties();
        properties.setResolutionWindow(Duration.ofSeconds(4));
        Clock clock = Clock.systemUTC();
        ShareResolutionStore store = new InMemoryShareResolutionStore(clock);

        ShareResolutionService service = new ShareResolutionService(extractionService, searchService, mappingService, store,
                properties, clock, sameThreadExecutor);

        when(extractionService.extract("https://example.org/p/slug", null, null))
                .thenReturn(Optional.of(new ShareExtractionDto(null, "fairphone")));

        ProductDto product = new ProductDto(1L, "fairphone-4", null, null, null, null, null, null, null, null, null, null,
                null, null);
        GlobalSearchHit hit = new GlobalSearchHit("phones", product, 0.92d);
        GlobalSearchResult searchResult = new GlobalSearchResult(List.of(new GlobalSearchVerticalGroup("phones", List.of(hit))),
                List.of(), false);
        when(searchService.globalSearch("fairphone", DomainLanguage.fr)).thenReturn(searchResult);

        ShareResolutionResponseDto pending = service
                .createResolution(new ShareResolutionRequestDto("https://example.org/p/slug", null, null), DomainLanguage.fr);

        Optional<ShareResolutionResponseDto> resolved = store.get(pending.token());
        assertThat(resolved).isPresent();
        assertThat(resolved.get().status()).isEqualTo(ShareResolutionStatus.RESOLVED);
        assertThat(resolved.get().candidates()).hasSize(1);
        assertThat(resolved.get().candidates().getFirst().productId()).isEqualTo("fairphone-4");

        verify(searchService).globalSearch("fairphone", DomainLanguage.fr);
    }

    @Test
    void resolutionTimesOutWhenClockExceedsWindow() {
        ShareExtractionService extractionService = mock(ShareExtractionService.class);
        SearchService searchService = mock(SearchService.class);
        ProductMappingService mappingService = mock(ProductMappingService.class);

        ShareResolutionProperties properties = new ShareResolutionProperties();
        properties.setResolutionWindow(Duration.ofSeconds(1));

        StepClock clock = new StepClock(Instant.parse("2024-06-01T10:00:00Z"));
        ShareResolutionStore store = new InMemoryShareResolutionStore(clock);

        ShareResolutionService service = new ShareResolutionService(extractionService, searchService, mappingService, store,
                properties, clock, sameThreadExecutor);

        when(extractionService.extract("https://example.org/p/slow", null, null))
                .thenReturn(Optional.of(new ShareExtractionDto(null, "slow-query")));

        ProductDto product = new ProductDto(9L, "slow", null, null, null, null, null, null, null, null, null, null, null, null);
        GlobalSearchHit hit = new GlobalSearchHit(null, product, 0.1d);
        GlobalSearchResult searchResult = new GlobalSearchResult(List.of(), List.of(hit), true);
        when(searchService.globalSearch("slow-query", DomainLanguage.en)).then(invocation -> {
            clock.step();
            return searchResult;
        });

        ShareResolutionResponseDto pending = service
                .createResolution(new ShareResolutionRequestDto("https://example.org/p/slow", null, null), DomainLanguage.en);

        Optional<ShareResolutionResponseDto> resolved = store.get(pending.token());
        assertThat(resolved).isPresent();
        assertThat(resolved.get().status()).isEqualTo(ShareResolutionStatus.TIMEOUT);
        assertThat(resolved.get().message()).contains("timed out");
        assertThat(resolved.get().candidates()).hasSize(1);
    }

    private static final class StepClock extends Clock {
        private Instant instant;

        StepClock(Instant start) {
            this.instant = start;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void step() {
            instant = instant.plusSeconds(2);
        }
    }
}
