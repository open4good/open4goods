package org.open4goods.services.prompt.dto.openai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchOutput(
    @JsonProperty("id") String id,
    @JsonProperty("custom_id") String customId,
    @JsonProperty("response") BatchResponse response,
    @JsonProperty("error") Object error
) { }
