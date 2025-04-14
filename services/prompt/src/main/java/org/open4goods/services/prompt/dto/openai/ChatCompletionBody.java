package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the body of the chat completion response.
 */
public record ChatCompletionBody(
    @JsonProperty("id") String id,
    @JsonProperty("object") String object,
    @JsonProperty("created") long created,
    @JsonProperty("model") String model,
    @JsonProperty("choices") List<BatchChoice> choices,
    @JsonProperty("usage") BatchUsage usage,
    @JsonProperty("system_fingerprint") String systemFingerprint,
    @JsonProperty("service_tier") String serviceTier
    
    
) { }
