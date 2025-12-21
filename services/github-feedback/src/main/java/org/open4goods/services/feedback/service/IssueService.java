// src/main/java/org/open4goods/services/feedback/service/IssueService.java
package org.open4goods.services.feedback.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.kohsuke.github.GHIssue;

/**
 * Defines operations around creating and listing GitHub issues.
 */
public interface IssueService {

    GHIssue createBug(String title,
                      String description,
                      String urlSource,
                      String author,
                      Set<String> labels) throws IOException;

    GHIssue createIdea(String title,
                       String description,
                       String urlSource,
                       String author,
                       Set<String> labels) throws IOException;

    List<GHIssue> listBugs() throws IOException;

    List<GHIssue> listIdeas() throws IOException;

    /**
     * List all open issues having *all* of the given labels.
     * @param labels labels to filter on (logical AND)
     * @return list of matching open issues
     */
    List<GHIssue> listIssues(String... labels) throws IOException;

    /**
     * Create a generic issue.
     * @param title Title of the issue
     * @param description Body of the issue
     * @param author Author of the issue (or representative)
     * @param labels Labels to apply
     * @return The created issue
     * @throws IOException If GitHub communication fails
     */
    GHIssue createIssue(String title, String description, String author, Set<String> labels) throws IOException;
}
