package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.ProductIndexMappingRunner.Action;
import org.open4goods.model.product.Product;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;

/**
 * The products index mapping step creates the index when absent, puts the mapping when present, retries a transient
 * failure, and never fails the api startup: three failures give three WARN lines with the full stack trace, then one
 * ERROR (GOU-1835).
 */
class ProductIndexMappingRunnerTest {

    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    private final ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    private final IndexOperations indexOperations = mock(IndexOperations.class);
    private final List<Duration> sleeps = new ArrayList<>();
    private final ProductIndexMappingRunner runner = new ProductIndexMappingRunner(operations, RETRY_DELAY,
            sleeps::add);

    private final Logger logger = (Logger) LoggerFactory.getLogger(ProductIndexMappingRunner.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        when(operations.indexOps(Product.class)).thenReturn(indexOperations);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
    }

    @Test
    void absentIndex_isCreatedWithSettingsAndMapping() {
        when(indexOperations.exists()).thenReturn(false);
        when(indexOperations.createWithMapping()).thenReturn(true);

        assertThat(runner.applyMapping()).contains(Action.CREATED);

        verify(indexOperations).createWithMapping();
        verify(indexOperations, never()).putMapping();
        assertThat(sleeps).isEmpty();
        assertThat(events(Level.INFO)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .startsWith("Products index mapping: index products-moustik, action CREATED, attempt 1/3, ")
                .endsWith(" ms");
    }

    @Test
    void presentIndex_getsItsMappingPut() {
        when(indexOperations.exists()).thenReturn(true);
        when(indexOperations.putMapping()).thenReturn(true);

        assertThat(runner.applyMapping()).contains(Action.MAPPING_UPDATED);

        verify(indexOperations).putMapping();
        verify(indexOperations, never()).createWithMapping();
        assertThat(events(Level.INFO)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .startsWith("Products index mapping: index products-moustik, action MAPPING_UPDATED, attempt 1/3, ");
        assertThat(events(Level.WARN)).isEmpty();
    }

    @Test
    void transientFailure_isRetriedThenSucceeds() {
        when(indexOperations.exists()).thenReturn(true);
        when(indexOperations.putMapping())
                .thenThrow(new DataAccessResourceFailureException("timeout", new java.io.IOException("socket reset")))
                .thenReturn(true);

        assertThat(runner.applyMapping()).contains(Action.MAPPING_UPDATED);

        verify(indexOperations, times(2)).putMapping();
        assertThat(sleeps).containsExactly(RETRY_DELAY);
        assertThat(events(Level.WARN)).singleElement().satisfies(warn -> {
            assertThat(warn.getFormattedMessage())
                    .isEqualTo("Products index mapping: index products-moustik, attempt 1/3 failed");
            assertThat(ThrowableProxyUtil.asString(warn.getThrowableProxy()))
                    .contains("timeout").contains("Caused by: java.io.IOException: socket reset");
        });
        assertThat(events(Level.INFO)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains("action MAPPING_UPDATED, attempt 2/3");
        assertThat(events(Level.ERROR)).isEmpty();
    }

    @Test
    void threeFailures_logThreeFullWarnsThenAnErrorAndNeverThrow() {
        when(indexOperations.exists()).thenReturn(true);
        when(indexOperations.putMapping()).thenThrow(
                new DataAccessResourceFailureException("put mapping failed", new IllegalStateException("blocked")));

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();

        verify(indexOperations, times(ProductIndexMappingRunner.MAX_ATTEMPTS)).putMapping();
        assertThat(sleeps).containsExactly(RETRY_DELAY, RETRY_DELAY.multipliedBy(2));
        assertThat(events(Level.WARN)).hasSize(3).allSatisfy(warn -> assertThat(
                ThrowableProxyUtil.asString(warn.getThrowableProxy()))
                .contains("put mapping failed").contains("Caused by: java.lang.IllegalStateException: blocked"));
        assertThat(events(Level.WARN)).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "Products index mapping: index products-moustik, attempt 1/3 failed",
                "Products index mapping: index products-moustik, attempt 2/3 failed",
                "Products index mapping: index products-moustik, attempt 3/3 failed");
        assertThat(events(Level.ERROR)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .startsWith("Products index mapping: index products-moustik, failed after 3 attempts, ")
                .contains("mapping not applied; the api keeps starting");
        assertThat(events(Level.INFO)).isEmpty();
        // The ERROR comes last.
        assertThat(appender.list.getLast().getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void unacknowledgedCreation_countsAsAFailure() {
        when(indexOperations.exists()).thenReturn(false, true);
        when(indexOperations.createWithMapping()).thenReturn(false);
        when(indexOperations.putMapping()).thenReturn(true);

        assertThat(runner.applyMapping()).contains(Action.MAPPING_UPDATED);

        assertThat(events(Level.WARN)).singleElement().satisfies(warn -> assertThat(
                ThrowableProxyUtil.asString(warn.getThrowableProxy()))
                .contains("index creation not acknowledged by Elasticsearch"));
    }

    @Test
    void interruptedWait_stopsRetryingWithAnError() {
        when(indexOperations.exists()).thenThrow(new DataAccessResourceFailureException("unreachable"));
        ProductIndexMappingRunner interrupted = new ProductIndexMappingRunner(operations, RETRY_DELAY, delay -> {
            throw new InterruptedException();
        });

        try {
            assertThat(interrupted.applyMapping()).isEmpty();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        verify(indexOperations, times(1)).exists();
        assertThat(events(Level.ERROR)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains("interrupted after 1 attempt(s)");
    }

    private List<ILoggingEvent> events(Level level) {
        return appender.list.stream().filter(event -> event.getLevel() == level).toList();
    }
}
