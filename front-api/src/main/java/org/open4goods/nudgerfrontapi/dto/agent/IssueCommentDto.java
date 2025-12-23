package org.open4goods.nudgerfrontapi.dto.agent;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue comment")
public record IssueCommentDto(
        @Schema(description = "Comment identifier") long id,
        @Schema(description = "Comment author") String author,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Markdown content") String body
) {}
