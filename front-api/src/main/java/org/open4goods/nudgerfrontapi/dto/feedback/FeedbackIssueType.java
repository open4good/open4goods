package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerates the supported categories of feedback that can be submitted.
 */
@Schema(description = "Type of feedback entry.")
public enum FeedbackIssueType {

    /** A suggestion or product evolution idea. */
    IDEA,

    /** A bug encountered while using the service. */
    BUG
}
