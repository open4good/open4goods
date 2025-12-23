package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after submitting an agent request")
public record AgentRequestResponseDto(
        @Schema(description = "ID of the created issue") String issueId,
        @Schema(description = "Number of the created issue") int issueNumber,
        @Schema(description = "URL of the created issue") String issueUrl,
        @Schema(description = "Current workflow state") String workflowState,
        @Schema(description = "Preview URL (if available)") String previewUrl,
        @Schema(description = "Visibility of the prompt") AgentRequestDto.PromptVisibility promptVisibility,
        @Schema(description = "Agent template identifier") String promptTemplateId,
        @Schema(description = "Prompt variant identifier") String promptVariantId
) {}
