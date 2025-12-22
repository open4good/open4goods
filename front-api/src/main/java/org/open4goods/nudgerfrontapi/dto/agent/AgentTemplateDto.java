package org.open4goods.nudgerfrontapi.dto.agent;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuration template for an agent")
public record AgentTemplateDto(
        @Schema(description = "Unique identifier of the agent") String id,
        @Schema(description = "Localized name of the agent") String name,
        @Schema(description = "Localized description") String description,
        @Schema(description = "Icon identifier (MDI)") String icon,
        @Schema(description = "Prompt template text") String promptTemplate,
        @Schema(description = "Associated tags") List<String> tags,
        @Schema(description = "Allowed roles to access this agent") List<String> allowedRoles,
        @Schema(description = "Whether the prompt history is public") boolean publicPromptHistory,
        @Schema(description = "Mailto template configuration") MailTemplateDto mailTemplate,
        @Schema(description = "Custom attributes for the agent") List<AgentAttributeDto> attributes
) {
    @Schema(description = "Mailto configuration")
    public record MailTemplateDto(
            String to,
            String subject,
            String body
    ) {}

    @Schema(description = "Custom attribute definition")
    public record AgentAttributeDto(
            String id,
            String type,
            String label,
            List<String> options
    ) {}
}
