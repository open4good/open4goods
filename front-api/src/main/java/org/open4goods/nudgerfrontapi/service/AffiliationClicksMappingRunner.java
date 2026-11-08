package org.open4goods.nudgerfrontapi.service;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.services.contribution.service.AffiliationClickMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

/**
 * Starts, once per run and off the startup thread, the additive mapping update of the {@code affiliation-clicks}
 * index ({@link AffiliationClickMappingService}, GOU-1828: {@code contentPath} as a {@code keyword}).
 * <p>
 * front-api writes this index, which is why the update lives here. It is fail-open: it runs on a dedicated virtual
 * thread after {@link ApplicationReadyEvent}, never blocks nor fails the startup nor the redirects, and any error
 * (Elasticsearch unreachable, field already mapped with another type...) ends in a single ERROR log line. The update
 * is idempotent, so the next startup simply retries.
 * <p>
 * Enabled by default; {@code front.affiliation-clicks-mapping.enabled=false} removes the bean (the {@code local}
 * profile does, since it has no Elasticsearch).
 */
@Component
@ConditionalOnProperty(prefix = "front.affiliation-clicks-mapping", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class AffiliationClicksMappingRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationClicksMappingRunner.class);
    private static final String THREAD_NAME = "affiliation-clicks-mapping";

    private final AffiliationClickMappingService mappingService;
    private final Executor executor;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * @param elasticsearchOperations the Elasticsearch template front-api already uses for the click repository
     */
    @Autowired
    public AffiliationClicksMappingRunner(ElasticsearchOperations elasticsearchOperations) {
        this(new AffiliationClickMappingService(elasticsearchOperations),
                task -> Thread.ofVirtual().name(THREAD_NAME).start(task));
    }

    /**
     * @param mappingService the mapping update to run
     * @param executor       runs the update off the caller thread
     */
    AffiliationClicksMappingRunner(AffiliationClickMappingService mappingService, Executor executor) {
        this.mappingService = mappingService;
        this.executor = executor;
    }

    /**
     * Hands the update to the executor and returns at once; later events are ignored.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this::runUpdate);
        } catch (RuntimeException e) {
            logFailure(e);
        }
    }

    /**
     * Runs the update and turns any failure into an ERROR log line.
     */
    void runUpdate() {
        try {
            mappingService.update();
        } catch (RuntimeException e) {
            logFailure(e);
        }
    }

    /**
     * Logs the exception type and message only: the mapping holds no click value.
     */
    private static void logFailure(RuntimeException e) {
        LOGGER.error("{} : échec, mapping non mis à jour ({} : {}) ; relancé au prochain démarrage",
                AffiliationClickMappingService.LOG_PREFIX, e.getClass().getName(), e.getMessage());
    }
}
