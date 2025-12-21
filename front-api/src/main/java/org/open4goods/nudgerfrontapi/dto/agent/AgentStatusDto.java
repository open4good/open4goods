package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public contract exposing the status of an automated agent working on a GitHub issue.
 *
 * @param issueId       GitHub issue identifier used to track the agent progress
 * @param workflowState Current workflow state inferred from GitHub objects
 * @param branchUrl     URL of the working branch if it exists
 */
public record AgentStatusDto(
        @Schema(description = "Identifiant de l'issue GitHub suivie.", example = "128")
        String issueId,

        @Schema(description = "État courant du workflow de l'agent.",
                implementation = AgentWorkflowState.class)
        AgentWorkflowState workflowState,

        @Schema(description = "URL de la branche associée si elle existe.",
                example = "https://github.com/open4goods/open4goods/tree/issue-128",
                nullable = true, format = "uri")
        String branchUrl) {
}
