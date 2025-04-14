package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the response portion of a batch output.
 */
public record BatchResponse(
    @JsonProperty("status_code") int statusCode,
    @JsonProperty("request_id") String requestId,
    @JsonProperty("body") ChatCompletionBody body
) { }
