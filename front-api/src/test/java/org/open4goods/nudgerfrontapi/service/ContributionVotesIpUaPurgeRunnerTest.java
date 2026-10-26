package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.open4goods.services.contribution.service.ContributionVoteIpUaPurgeService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * The purge never blocks nor fails the startup: it runs once, off the startup thread, and an unreachable
 * Elasticsearch only yields an ERROR log line. The configuration switch removes the bean.
 */
@ExtendWith(OutputCaptureExtension.class)
class ContributionVotesIpUaPurgeRunnerTest {

    @Test
    void onApplicationReady_returnsWhileThePurgeIsStillRunning() throws InterruptedException {
        CountDownLatch purgeStarted = new CountDownLatch(1);
        CountDownLatch releasePurge = new CountDownLatch(1);
        ElasticsearchTemplate operations = mock(ElasticsearchTemplate.class);
        when(operations.indexOps(any(IndexCoordinates.class))).thenAnswer(invocation -> {
            purgeStarted.countDown();
            releasePurge.await(10, TimeUnit.SECONDS);
            throw new DataAccessResourceFailureException("Connection refused");
        });
        ContributionVotesIpUaPurgeRunner runner = new ContributionVotesIpUaPurgeRunner(operations);

        runner.onApplicationReady();

        // Back on the startup thread while the purge, on its own thread, is still blocked on Elasticsearch.
        assertThat(purgeStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(releasePurge.getCount()).isEqualTo(1);
        releasePurge.countDown();
    }

    @Test
    void runPurge_logsAnErrorWhenElasticsearchIsUnreachable(CapturedOutput output) {
        ElasticsearchTemplate operations = mock(ElasticsearchTemplate.class);
        when(operations.indexOps(any(IndexCoordinates.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));
        ContributionVotesIpUaPurgeRunner runner = new ContributionVotesIpUaPurgeRunner(
                new ContributionVoteIpUaPurgeService(operations), Runnable::run);

        assertThatCode(runner::onApplicationReady).doesNotThrowAnyException();

        assertThat(output).contains("ERROR")
                .contains("contribution-votes purge ip/ua : échec, purge non terminée ("
                        + DataAccessResourceFailureException.class.getName() + " : Connection refused)");
    }

    @Test
    void onApplicationReady_startsThePurgeOnlyOnce() {
        ContributionVoteIpUaPurgeService purgeService = mock(ContributionVoteIpUaPurgeService.class);
        List<Runnable> submitted = new ArrayList<>();
        ContributionVotesIpUaPurgeRunner runner = new ContributionVotesIpUaPurgeRunner(purgeService, submitted::add);

        runner.onApplicationReady();
        runner.onApplicationReady();

        assertThat(submitted).hasSize(1);
        verify(purgeService, never()).purge();
        submitted.getFirst().run();
        verify(purgeService, times(1)).purge();
    }

    @Test
    void onApplicationReady_logsAnErrorWhenTheTaskCannotBeStarted(CapturedOutput output) {
        ContributionVotesIpUaPurgeRunner runner = new ContributionVotesIpUaPurgeRunner(
                mock(ContributionVoteIpUaPurgeService.class), task -> {
                    throw new IllegalStateException("no thread");
                });

        assertThatCode(runner::onApplicationReady).doesNotThrowAnyException();

        assertThat(output).contains("contribution-votes purge ip/ua : échec, purge non terminée");
    }

    @Test
    void bean_isEnabledByDefaultAndRemovedByTheSwitch() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withBean(ElasticsearchOperations.class, () -> mock(ElasticsearchOperations.class))
                .withUserConfiguration(ContributionVotesIpUaPurgeRunner.class);

        contextRunner.run(context -> assertThat(context).hasSingleBean(ContributionVotesIpUaPurgeRunner.class));
        contextRunner.withPropertyValues("front.contribution-votes-purge.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ContributionVotesIpUaPurgeRunner.class));
        contextRunner.withPropertyValues("front.contribution-votes-purge.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ContributionVotesIpUaPurgeRunner.class));
    }
}
