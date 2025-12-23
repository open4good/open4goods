package org.open4goods.services.feedback.dto;

import java.util.Set;

/**
 * DTO representing a feedback issue, decoupled from the underlying provider (GitHub).
 */
public record IssueDto(
        String id,
        int number,
        String htmlUrl,
        String state,
        String title,
        Set<String> labels,
        int commentsCount
) {
}
