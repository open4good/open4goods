package org.open4goods.services.prompt.dto.openai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents usage statistics of a chat completion response.
 */
public record BatchUsage(
	    @JsonProperty("prompt_tokens") int promptTokens,
	    @JsonProperty("completion_tokens") int completionTokens,
	    @JsonProperty("total_tokens") int totalTokens,
	    @JsonProperty("prompt_tokens_details") Map<String, Integer> promptTokensDetails,
	    @JsonProperty("completion_tokens_details") Map<String, Integer> completionTokensDetails
) { }
