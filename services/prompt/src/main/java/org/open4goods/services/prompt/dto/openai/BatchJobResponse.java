package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Maps the response from the OpenAI Batch API.
 */
public record BatchJobResponse(

    @JsonProperty("id") String id,
    @JsonProperty("object") String object,
    @JsonProperty("endpoint") String endpoint,
    @JsonProperty("errors") Object errors, // can be null or a structure; define type if needed
    @JsonProperty("input_file_id") String inputFileId,
    @JsonProperty("completion_window") String completionWindow,
    @JsonProperty("status") String status,
    @JsonProperty("output_file_id") String outputFileId,
    @JsonProperty("error_file_id") String errorFileId,

    @JsonProperty("created_at") Long createdAt,
    @JsonProperty("in_progress_at") Long inProgressAt,
    @JsonProperty("expires_at") Long expiresAt,
    @JsonProperty("finalizing_at") Long finalizingAt,
    @JsonProperty("completed_at") Long completedAt,
    @JsonProperty("failed_at") Long failedAt,
    @JsonProperty("expired_at") Long expiredAt,
    @JsonProperty("cancelling_at") Long cancellingAt,
    @JsonProperty("cancelled_at") Long cancelledAt,

    @JsonProperty("request_counts") RequestCounts requestCounts,
    @JsonProperty("metadata") Map<String, String> metadata

) {
    public record RequestCounts(
        @JsonProperty("total") int total,
        @JsonProperty("completed") int completed,
        @JsonProperty("failed") int failed
    ) {}
}
