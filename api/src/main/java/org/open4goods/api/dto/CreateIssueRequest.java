package org.open4goods.api.dto;

import java.util.Set;

/**
 * Request body for creating a feedback issue.
 */
public record CreateIssueRequest(
        String type,
        String title,
        String message,
        String url,
        String author,
        Set<String> labels
) {}
