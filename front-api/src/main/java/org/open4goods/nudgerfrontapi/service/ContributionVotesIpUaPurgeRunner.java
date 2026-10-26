package org.open4goods.nudgerfrontapi.service;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.services.contribution.service.ContributionVoteIpUaPurgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

/**
 * Starts, once per run and off the startup thread, the purge of the historical {@code ip}/{@code ua} fields of the
 * {@code contribution-votes} index ({@link ContributionVoteIpUaPurgeService}, board approval 2f4935d1, GOU-1230).
 * <p>
 * front-api writes this index with its own Elasticsearch access, which is why the purge lives here. It never blocks
 * nor fails the startup: it runs on a dedicated virtual thread after {@link ApplicationReadyEvent}, and any error
 * (Elasticsearch unreachable, unexpected mapping...) ends in a single ERROR log line. The purge is idempotent, so the
 * next startup simply resumes it.
 * <p>
 * Enabled by default; {@code front.contribution-votes-purge.enabled=false} removes the bean (the {@code local}
 * profile does, since it has no Elasticsearch).
 */
@Component
@ConditionalOnProperty(prefix = "front.contribution-votes-purge", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class ContributionVotesIpUaPurgeRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContributionVotesIpUaPurgeRunner.class);
    private static final String THREAD_NAME = "contribution-votes-purge";

    private final ContributionVoteIpUaPurgeService purgeService;
    private final Executor executor;
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * @param elasticsearchOperations the Elasticsearch template front-api already uses for the vote repository
     */
    @Autowired
    public ContributionVotesIpUaPurgeRunner(ElasticsearchOperations elasticsearchOperations) {
        this(new ContributionVoteIpUaPurgeService(elasticsearchOperations),
                task -> Thread.ofVirtual().name(THREAD_NAME).start(task));
    }

    /**
     * @param purgeService the purge to run
     * @param executor     runs the purge off the caller thread
     */
    ContributionVotesIpUaPurgeRunner(ContributionVoteIpUaPurgeService purgeService, Executor executor) {
        this.purgeService = purgeService;
        this.executor = executor;
    }

    /**
     * Hands the purge to the executor and returns at once; later events are ignored.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this::runPurge);
        } catch (RuntimeException e) {
            logFailure(e);
        }
    }

    /**
     * Runs the purge and turns any failure into an ERROR log line.
     */
    void runPurge() {
        try {
            purgeService.purge();
        } catch (RuntimeException e) {
            logFailure(e);
        }
    }

    /**
     * Logs the exception type and message only: no stack trace, and the purge itself never puts a field value in an
     * exception.
     */
    private static void logFailure(RuntimeException e) {
        LOGGER.error("{} : échec, purge non terminée ({} : {}) ; relancée au prochain démarrage",
                ContributionVoteIpUaPurgeService.LOG_PREFIX, e.getClass().getName(), e.getMessage());
    }
}
