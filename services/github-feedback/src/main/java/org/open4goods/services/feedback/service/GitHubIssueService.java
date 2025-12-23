// src/main/java/org/open4goods/services/feedback/service/GitHubIssueService.java
package org.open4goods.services.feedback.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.open4goods.services.feedback.config.FeedbackConfiguration;
import org.open4goods.services.feedback.dto.IssueDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link IssueService} that publishes/reads issues on GitHub.
 */
@Service
@ConditionalOnProperty(prefix = "feedback.github", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GitHubIssueService implements IssueService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubIssueService.class);

    private final FeedbackConfiguration config;
    private final GHRepository repository;

    public GitHubIssueService(FeedbackConfiguration config,
                              GHRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    @Override
    public IssueDto createBug(String title,
                             String description,
                             String urlSource,
                             String author,
                             Set<String> labels) throws IOException {
        labels.add("bug");
        return createIssue("Bug", title, description, urlSource, author, labels);
    }

    @Override
    public IssueDto createIdea(String title,
                              String description,
                              String urlSource,
                              String author,
                              Set<String> labels) throws IOException {
        labels.add("feature");
        return createIssue("Idea", title, description, urlSource, author, labels);
    }

    @Override
    public IssueDto createIssue(String title,
                               String description,
                               String author,
                               Set<String> labels) throws IOException {
        return createIssue("Issue", title, description, "Agent", author, labels);
    }

    private IssueDto createIssue(String kind,
                                String title,
                                String description,
                                String urlSource,
                                String author,
                                Set<String> labels) throws IOException {
        // If configured, add the default "votable" label
        if (config.getVoting().isDefaultVotable()) {
            labels.add(config.getVoting().getRequiredLabel());
        }

        StringBuilder sb = new StringBuilder()
            .append("## ").append(kind).append("\n\n")
            .append("Submitted by *").append(author)
            .append("* via ").append(urlSource).append("\n\n")
            .append("#### Message:\n\n> ").append(description).append("\n\n");

        GHIssueBuilder builder = repository.createIssue(title).body(sb.toString());
        labels.forEach(builder::label);
        GHIssue issue = builder.create();
        logger.info("Created {} issue #{} on GitHub with labels {}", kind, issue.getNumber(), labels);
        return mapToDto(issue);
    }

    @Override
    public List<IssueDto> listBugs() throws IOException {
        return listIssues("bug");
    }

    @Override
    public List<IssueDto> listIdeas() throws IOException {
        return listIssues("feature");
    }

    @Override
    public List<IssueDto> listIssues(String... labels) throws IOException {
        // Always require the configured "votable" label
        String required = config.getVoting().getRequiredLabel();
        Set<String> allLabels = new HashSet<>(Arrays.asList(labels));
        allLabels.add(required);

        return repository.getIssues(GHIssueState.OPEN).stream()
                .filter(issue -> {
                    Set<String> names = issue.getLabels().stream()
                                             .map(GHLabel::getName)
                                             .collect(Collectors.toSet());
                    return names.containsAll(allLabels);
                })
                .map(this::mapToDto)
                .toList();
    }

<<<<<<< HEAD
    private IssueDto mapToDto(GHIssue issue) {
        Set<String> labels = issue.getLabels().stream()
                .map(GHLabel::getName)
                .collect(Collectors.toSet());
        
        return new IssueDto(
                String.valueOf(issue.getId()),
                issue.getNumber(),
                issue.getHtmlUrl().toString(),
                issue.getState().toString(),
                issue.getTitle(),
                labels
        );
=======
    @Override
    public List<GHIssueComment> listIssueComments(int issueNumber) throws IOException {
        return repository.getIssue(issueNumber).getComments();
>>>>>>> branch 'main' of https://github.com/open4good/open4goods.git
    }
}
