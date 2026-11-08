package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Ensures feed fetching triggered by URL never logs the URL, which can carry an API key (Effinity, Awin).
 */
class BatchServiceFeedLogsTest {

    private static final String SECRET = "secret-key";

    @Test
    void fetchFeedsByUrlNeverLogsTheApiKey() {
        BatchService batchService = new BatchService(mock(AggregationFacadeService.class),
                mock(CompletionFacadeService.class), mock(VerticalsConfigService.class), mock(ProductRepository.class),
                mock(FeedIndexingService.class), mock(FeedService.class), mock(SerialisationService.class),
                mock(ResourceService.class), mock(ApiProperties.class));

        Logger logger = (Logger) LoggerFactory.getLogger(BatchService.class);
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            batchService.fetchFeedsByUrl("https://api.effinity.fr/apiv3/" + SECRET + "/publisher/feed.csv");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .isEqualTo("Fetching feeds with URL: https://api.effinity.fr/<redacted>"));
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain(SECRET));
    }
}
