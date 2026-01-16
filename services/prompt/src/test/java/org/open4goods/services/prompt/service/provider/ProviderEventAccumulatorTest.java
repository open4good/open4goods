package org.open4goods.services.prompt.service.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.prompt.config.GenAiServiceType;

/**
 * Tests for provider event accumulation in streaming flows.
 */
class ProviderEventAccumulatorTest {

    @Test
    void shouldAccumulateChunksAndMetadata() {
        ProviderEventAccumulator accumulator = new ProviderEventAccumulator();
        accumulator.accept(ProviderEvent.started(GenAiServiceType.OPEN_AI, "gpt-4o-mini"));
        accumulator.accept(ProviderEvent.streamChunk(GenAiServiceType.OPEN_AI, "gpt-4o-mini", "{"));
        accumulator.accept(ProviderEvent.streamChunk(GenAiServiceType.OPEN_AI, "gpt-4o-mini", "\"key\":"));
        accumulator.accept(ProviderEvent.streamChunk(GenAiServiceType.OPEN_AI, "gpt-4o-mini", "\"value\""));
        accumulator.accept(ProviderEvent.streamChunk(GenAiServiceType.OPEN_AI, "gpt-4o-mini", "}"));
        accumulator.accept(ProviderEvent.metadata(GenAiServiceType.OPEN_AI, "gpt-4o-mini",
                Map.of("citations", java.util.List.of(Map.of("url", "https://example.com")))));
        accumulator.accept(ProviderEvent.completed(GenAiServiceType.OPEN_AI, "gpt-4o-mini",
                "{\"key\":\"value\"}", Map.of()));

        assertThat(accumulator.getContent()).isEqualTo("{\"key\":\"value\"}");
        assertThat(accumulator.getMetadata()).containsKey("citations");
    }
}
