package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRepository;
import org.open4goods.nudgerfrontapi.dto.agent.AgentStatusDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentWorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for mapping GitHub issues/branches to an agent workflow status.
 */
@Service
public class AgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentService.class);

    private final GHRepository repository;

    public AgentService(GHRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieve the workflow status for the given GitHub issue.
     *
     * @param issueId GitHub issue identifier supplied by the client
     * @return agent status including branch URL (if any) and workflow state
     * @throws IOException when GitHub communication fails
     * @throws IllegalArgumentException when the issue id cannot be parsed
     */
    public AgentStatusDto getAgentStatus(String issueId) throws IOException {
        int issueNumber = parseIssueNumber(issueId);
        GHIssue issue = repository.getIssue(issueNumber);

        Optional<String> branchName = findBranch(issueNumber);
        Optional<GHPullRequest> pullRequest = findPullRequest(branchName);

        AgentWorkflowState workflowState = resolveWorkflowState(issue, pullRequest, branchName);
        String branchUrl = branchName.map(this::buildBranchUrl).orElse(null);

        return new AgentStatusDto(issueId, workflowState, branchUrl);
    }

    private int parseIssueNumber(String issueId) {
        try {
            return Integer.parseInt(issueId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Issue id must be a number", ex);
        }
    }

    private Optional<String> findBranch(int issueNumber) throws IOException {
        String issueToken = String.valueOf(issueNumber);
        Map<String, GHBranch> branches = repository.getBranches();
        return branches.keySet().stream()
                .filter(name -> name.contains(issueToken))
                .findFirst();
    }

    private Optional<GHPullRequest> findPullRequest(Optional<String> branchName) throws IOException {
        if (branchName.isEmpty()) {
            return Optional.empty();
        }

        GHPullRequestQueryBuilder query = repository.queryPullRequests()
                .state(GHIssueState.ALL)
                .head(branchName.get());

        return query.list()
                .toList()
                .stream()
                .findFirst();
    }

    private AgentWorkflowState resolveWorkflowState(GHIssue issue,
                                                    Optional<GHPullRequest> pullRequest,
                                                    Optional<String> branchName) throws IOException {
        if (pullRequest.isPresent()) {
            GHPullRequest pr = pullRequest.get();
            if (Boolean.TRUE.equals(pr.isMerged())) {
                return AgentWorkflowState.MERGED;
            }
            if (pr.getState() == GHIssueState.OPEN) {
                return AgentWorkflowState.IN_REVIEW;
            }
            return AgentWorkflowState.CLOSED;
        }

        if (issue.getState() == GHIssueState.CLOSED) {
            return AgentWorkflowState.CLOSED;
        }

        if (branchName.isPresent()) {
            return AgentWorkflowState.BRANCH_CREATED;
        }

        LOGGER.debug("No branch or pull request found for issue {}, returning ISSUE_CREATED state", issue.getNumber());
        return AgentWorkflowState.ISSUE_CREATED;
    }

    private String buildBranchUrl(String branchName) {
        URL repositoryUrl = repository.getHtmlUrl();
        return repositoryUrl == null ? null : repositoryUrl.toString() + "/tree/" + branchName;
    }
}
