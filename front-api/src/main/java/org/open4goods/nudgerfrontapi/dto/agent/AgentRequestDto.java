package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to submit a prompt to an agent")
public record AgentRequestDto(
        @Schema(description = "Type of request (FEATURE, QUESTION)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull AgentRequestType type,

        @Schema(description = "User input prompt", requiredMode = Schema.RequiredMode.REQUIRED, example = "I want a dark mode")
        @NotBlank String promptUser,

        @Schema(description = "ID of the agent template used", requiredMode = Schema.RequiredMode.REQUIRED, example = "agent-feature")
        @NotBlank String promptTemplateId,

        @Schema(description = "ID of the prompt variant selected", requiredMode = Schema.RequiredMode.REQUIRED, example = "default")
        @NotBlank String promptVariantId,

        @Schema(description = "Override prompt visibility (PUBLIC, PRIVATE)")
        PromptVisibility promptVisibility,

        @Schema(description = "User handle or email (optional)")
        String userHandle,

        @Schema(description = "Values for custom attributes")
        java.util.Map<String, Object> attributeValues,

        @Schema(description = "Captcha validation token")
        String captchaToken
) {
    public enum AgentRequestType {
        FEATURE, QUESTION
    }

    public enum PromptVisibility {
        PUBLIC, PRIVATE
    }
}
