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

@Schema(description = "Issue comment")
public record IssueCommentDto(
        @Schema(description = "Comment identifier") long id,
        @Schema(description = "Comment author") String author,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Markdown content") String body
) {}
