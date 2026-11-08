package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import org.open4goods.services.contribution.model.AffiliationClick;
import org.open4goods.services.contribution.service.AffiliationClickMappingService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * The mapping update never blocks nor fails the startup: it runs once, off the startup thread, and an unreachable
 * Elasticsearch only yields an ERROR log line. The configuration switch removes the bean.
 */
@ExtendWith(OutputCaptureExtension.class)
class AffiliationClicksMappingRunnerTest {

    @Test
    void onApplicationReady_returnsWhileTheUpdateIsStillRunning() throws InterruptedException {
        CountDownLatch updateStarted = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.indexOps(AffiliationClick.class)).thenAnswer(invocation -> {
            updateStarted.countDown();
            releaseUpdate.await(10, TimeUnit.SECONDS);
            throw new DataAccessResourceFailureException("Connection refused");
        });
        AffiliationClicksMappingRunner runner = new AffiliationClicksMappingRunner(operations);

        runner.onApplicationReady();

        // Back on the startup thread while the update, on its own thread, is still blocked on Elasticsearch.
        assertThat(updateStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(releaseUpdate.getCount()).isEqualTo(1);
        releaseUpdate.countDown();
    }

    @Test
    void runUpdate_logsAnErrorWhenElasticsearchIsUnreachable(CapturedOutput output) {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.indexOps(AffiliationClick.class))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));
        AffiliationClicksMappingRunner runner = new AffiliationClicksMappingRunner(
                new AffiliationClickMappingService(operations), Runnable::run);

        assertThatCode(runner::onApplicationReady).doesNotThrowAnyException();

        assertThat(output).contains("ERROR")
                .contains("affiliation-clicks mapping : échec, mapping non mis à jour ("
                        + DataAccessResourceFailureException.class.getName() + " : Connection refused)");
    }

    @Test
    void onApplicationReady_startsTheUpdateOnlyOnce() {
        AffiliationClickMappingService mappingService = mock(AffiliationClickMappingService.class);
        List<Runnable> submitted = new ArrayList<>();
        AffiliationClicksMappingRunner runner = new AffiliationClicksMappingRunner(mappingService, submitted::add);

        runner.onApplicationReady();
        runner.onApplicationReady();

        assertThat(submitted).hasSize(1);
        verify(mappingService, never()).update();
        submitted.getFirst().run();
        verify(mappingService, times(1)).update();
    }

    @Test
    void onApplicationReady_logsAnErrorWhenTheTaskCannotBeStarted(CapturedOutput output) {
        AffiliationClicksMappingRunner runner = new AffiliationClicksMappingRunner(
                mock(AffiliationClickMappingService.class), task -> {
                    throw new IllegalStateException("no thread");
                });

        assertThatCode(runner::onApplicationReady).doesNotThrowAnyException();

        assertThat(output).contains("affiliation-clicks mapping : échec, mapping non mis à jour");
    }

    @Test
    void bean_isEnabledByDefaultAndRemovedByTheSwitch() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withBean(ElasticsearchOperations.class, () -> mock(ElasticsearchOperations.class))
                .withUserConfiguration(AffiliationClicksMappingRunner.class);

        contextRunner.run(context -> assertThat(context).hasSingleBean(AffiliationClicksMappingRunner.class));
        contextRunner.withPropertyValues("front.affiliation-clicks-mapping.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AffiliationClicksMappingRunner.class));
    }
}
