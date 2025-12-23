package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of a recent agent request")
public record AgentActivityDto(
        @Schema(description = "Issue ID") String id,
        @Schema(description = "Request Type") AgentRequestDto.AgentRequestType type,
        @Schema(description = "Issue URL") String issueUrl,
        @Schema(description = "Issue Status") String status,
        @Schema(description = "Prompt Visibility") AgentRequestDto.PromptVisibility promptVisibility,
        @Schema(description = "Prompt Summary (if public)") String promptSummary,
        @Schema(description = "Number of discussion comments") int commentsCount
) {}
