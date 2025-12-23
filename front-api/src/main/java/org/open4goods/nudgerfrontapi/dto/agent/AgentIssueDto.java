package org.open4goods.nudgerfrontapi.dto.agent;

import java.util.List;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed view of an agent-created issue")
public record AgentIssueDto(
        @Schema(description = "Issue ID") String issueId,
        @Schema(description = "Issue Number") int number,
        @Schema(description = "Issue Title") String title,
        @Schema(description = "Issue URL") String url,
        @Schema(description = "Issue Status (open/closed)") String status,
        @Schema(description = "Issue Labels") List<String> labels,
        @Schema(description = "Workflow State") String workflowState,
        @Schema(description = "Preview URL") String previewUrl,
        @Schema(description = "Prompt Visibility") AgentRequestDto.PromptVisibility promptVisibility,
        @Schema(description = "Prompt Summary (if public)") String promptSummary,
        @Schema(description = "Discussion comments") List<IssueCommentDto> comments
) {}


