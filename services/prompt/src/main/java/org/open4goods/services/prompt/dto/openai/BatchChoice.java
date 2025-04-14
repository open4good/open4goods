package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single choice within the chat completion response.
 */
public record BatchChoice(
	    @JsonProperty("index") int index,
	    @JsonProperty("message") BatchMessage message,
	    @JsonProperty("finish_reason") String finishReason
) { }
