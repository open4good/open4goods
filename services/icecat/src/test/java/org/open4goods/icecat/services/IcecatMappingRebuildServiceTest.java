package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.registry.GitRegistryRuntimeImporter;
import org.open4goods.icecat.model.IcecatMappingRebuildState;

class IcecatMappingRebuildServiceTest {

    @Test
    void returnsAJobIdAndPublishesTheCompletedRegistryProjection() {
        IcecatRegistryProjectionService projection = new IcecatRegistryProjectionService(new GitRegistryRuntimeImporter());
        IcecatMappingRebuildService service = new IcecatMappingRebuildService(projection, new DirectExecutorService(),
                Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneOffset.UTC));

        var job = service.submit("\"" + projection.current().contentHash() + "\"");

        assertThat(job.jobId()).isNotNull();
        assertThat(job.state()).isEqualTo(IcecatMappingRebuildState.QUEUED);
        assertThat(service.find(job.jobId())).hasValueSatisfying(completed -> {
            assertThat(completed.state()).isEqualTo(IcecatMappingRebuildState.SUCCEEDED);
            assertThat(completed.completedAt()).isEqualTo(Instant.parse("2026-09-12T10:00:00Z"));
            assertThat(completed.registryHash()).isEqualTo(projection.current().contentHash());
        });
        service.close();
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
