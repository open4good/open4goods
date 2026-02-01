package org.open4goods.model.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class AiReviewDeserializationTest {

    @Test
    void testAiRatingDeserializationOptionalComment() throws Exception {
        String json = """
            {
                "source": "Test Source",
                "score": "4.5",
                "max": "5",
                "number": 1
            }
        """;

        ObjectMapper mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .build();

        AiReview.AiRating rating = mapper.readValue(json, AiReview.AiRating.class);

        assertThat(rating).isNotNull();
        assertThat(rating.getSource()).isEqualTo("Test Source");
        assertThat(rating.getComment()).isNull();
    }
}
