package org.open4goods.nudgerfrontapi.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Possible states of the share resolution pipeline.
 */
@Schema(description = "State of the share resolution process", allowableValues = { "PENDING", "RESOLVED", "TIMEOUT", "ERROR" })
public enum ShareResolutionStatus {
    /** Request accepted and currently being resolved. */
    PENDING,
    /** Resolution completed with candidates. */
    RESOLVED,
    /** Resolution exceeded its SLA budget. */
    TIMEOUT,
    /** Resolution failed due to invalid input or extractor error. */
    ERROR
}
