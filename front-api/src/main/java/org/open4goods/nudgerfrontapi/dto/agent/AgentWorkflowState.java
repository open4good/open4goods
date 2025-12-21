package org.open4goods.nudgerfrontapi.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lifecycle steps for an automated contribution agent.
 */
@Schema(description = "Cycle de vie d'un agent automatisé autour d'une issue GitHub.")
public enum AgentWorkflowState {

    /**
     * A request has been received but no issue was created yet.
     */
    @Schema(description = "Requête reçue mais pas encore transformée en issue GitHub.")
    REQUESTED,

    /**
     * The GitHub issue has been created.
     */
    @Schema(description = "Issue GitHub créée.")
    ISSUE_CREATED,

    /**
     * A working branch dedicated to the issue exists.
     */
    @Schema(description = "Branche de travail créée pour l'issue.")
    BRANCH_CREATED,

    /**
     * A pull request is open for the branch.
     */
    @Schema(description = "Pull request ouverte pour revue.")
    IN_REVIEW,

    /**
     * The pull request has been merged.
     */
    @Schema(description = "Pull request fusionnée.")
    MERGED,

    /**
     * The effort ended without a merge (issue or PR closed).
     */
    @Schema(description = "Issue ou pull request fermée sans merge.")
    CLOSED
}
