package org.open4goods.model.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests tolerant deserialization behavior for AI impact-score responses.
 */
public class ImpactScoreAiResultDeserializationTest {

    @Test
    void sourceYearAcceptsLegacyFreeTextValues() throws Exception {
        String json = """
                {
                  "year": "2024 report"
                }
                """;

        ObjectMapper mapper = JsonMapper.builder().build();

        ImpactScoreAiResult.Source source = mapper.readValue(json, ImpactScoreAiResult.Source.class);

        assertThat(source.year).isEqualTo(2024);
    }

    @Test
    void sourceYearFallsBackToZeroWhenUnknown() throws Exception {
        String json = """
                {
                  "year": "unknown"
                }
                """;

        ObjectMapper mapper = JsonMapper.builder().build();

        ImpactScoreAiResult.Source source = mapper.readValue(json, ImpactScoreAiResult.Source.class);

        assertThat(source.year).isZero();
    }
}
