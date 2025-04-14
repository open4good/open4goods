package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the request payload to create a batch job using the OpenAI Batch API.
 *
 * <p>
 * Contains the input file ID, the API endpoint to use for requests, and the completion window.
 * </p>
 *
 * @param inputFileId the ID of the uploaded input file.
 * @param endpoint the API endpoint to be used (e.g., "/v1/chat/completions").
 * @param completionWindow the time window for the batch completion (e.g., "24h").
 * @param metadata optional metadata.
 */
public record BatchCreateRequest(
    @JsonProperty("input_file_id") String inputFileId,
    String endpoint,
    @JsonProperty("completion_window") String completionWindow,
    Object metadata
) { }
