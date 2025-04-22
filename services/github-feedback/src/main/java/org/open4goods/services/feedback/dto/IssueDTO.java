package org.open4goods.services.feedback.dto;

/**
 * DTO representing a GitHub issue plus vote count for the UI.
 *
 * @param id     issue ID (String)
 * @param number GitHub issue number
 * @param title  issue title
 * @param url    issue URL
 * @param votes  total votes recorded locally
 */
public record IssueDTO(
    String id,
    int number,
    String title,
    String url,
    int votes
) {}
