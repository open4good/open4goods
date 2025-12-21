package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.dto.agent.AgentStatusDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentWorkflowState;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    private static final String ISSUE_ID = "42";
    private static final int ISSUE_NUMBER = 42;
    private static final String BRANCH_NAME = "feature/issue-42";

    @Mock
    private GHRepository repository;
    @Mock
    private org.kohsuke.github.GHIssue issue;
    @Mock
    private GHPullRequest pullRequest;
    @Mock
    private GHPullRequestQueryBuilder pullRequestQueryBuilder;
    @Mock
    private PagedIterable<GHPullRequest> pullRequests;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(repository);
    }

    @Test
    void shouldExposeMergedStateWhenPullRequestMerged() throws Exception {
        stubIssueWithRepositoryUrl(GHIssueState.OPEN);
        stubBranch();
        stubPullRequest(List.of(pullRequest));
        when(pullRequest.isMerged()).thenReturn(true);

        AgentStatusDto status = agentService.getAgentStatus(ISSUE_ID);

        assertThat(status.workflowState()).isEqualTo(AgentWorkflowState.MERGED);
        assertThat(status.branchUrl()).endsWith(BRANCH_NAME);
    }

    @Test
    void shouldExposeInReviewWhenPullRequestOpen() throws Exception {
        stubIssueWithRepositoryUrl(GHIssueState.OPEN);
        stubBranch();
        stubPullRequest(List.of(pullRequest));
        when(pullRequest.isMerged()).thenReturn(false);
        when(pullRequest.getState()).thenReturn(GHIssueState.OPEN);

        AgentStatusDto status = agentService.getAgentStatus(ISSUE_ID);

        assertThat(status.workflowState()).isEqualTo(AgentWorkflowState.IN_REVIEW);
    }

    @Test
    void shouldExposeBranchCreatedWhenBranchExistsWithoutPullRequest() throws Exception {
        stubIssueWithRepositoryUrl(GHIssueState.OPEN);
        stubBranch();
        stubPullRequest(Collections.emptyList());

        AgentStatusDto status = agentService.getAgentStatus(ISSUE_ID);

        assertThat(status.workflowState()).isEqualTo(AgentWorkflowState.BRANCH_CREATED);
        assertThat(status.branchUrl()).endsWith(BRANCH_NAME);
    }

    @Test
    void shouldExposeClosedWhenIssueClosedWithoutBranch() throws Exception {
        stubIssue(GHIssueState.CLOSED);
        when(repository.getBranches()).thenReturn(Collections.emptyMap());

        AgentStatusDto status = agentService.getAgentStatus(ISSUE_ID);

        assertThat(status.workflowState()).isEqualTo(AgentWorkflowState.CLOSED);
        assertThat(status.branchUrl()).isNull();
    }

    @Test
    void shouldExposeIssueCreatedWhenNothingElseFound() throws Exception {
        stubIssue(GHIssueState.OPEN);
        when(repository.getBranches()).thenReturn(Collections.emptyMap());

        AgentStatusDto status = agentService.getAgentStatus(ISSUE_ID);

        assertThat(status.workflowState()).isEqualTo(AgentWorkflowState.ISSUE_CREATED);
        assertThat(status.branchUrl()).isNull();
    }

    @Test
    void shouldRejectNonNumericIssueId() {
        assertThatThrownBy(() -> agentService.getAgentStatus("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void stubIssue(GHIssueState state) throws Exception {
        when(repository.getIssue(ISSUE_NUMBER)).thenReturn(issue);
        lenient().when(issue.getState()).thenReturn(state);
    }

    private void stubIssueWithRepositoryUrl(GHIssueState state) throws Exception {
        stubIssue(state);
        when(repository.getHtmlUrl()).thenReturn(new URL("https://github.com/open4good/open4goods"));
    }

    private void stubBranch() throws Exception {
        Map<String, org.kohsuke.github.GHBranch> branches = new HashMap<>();
        branches.put(BRANCH_NAME, mock(org.kohsuke.github.GHBranch.class));
        when(repository.getBranches()).thenReturn(branches);
    }

    private void stubPullRequest(List<GHPullRequest> prs) throws Exception {
        when(repository.queryPullRequests()).thenReturn(pullRequestQueryBuilder);
        when(pullRequestQueryBuilder.state(GHIssueState.ALL)).thenReturn(pullRequestQueryBuilder);
        when(pullRequestQueryBuilder.head(BRANCH_NAME)).thenReturn(pullRequestQueryBuilder);
        when(pullRequestQueryBuilder.list()).thenReturn(pullRequests);
        when(pullRequests.toList()).thenReturn(prs);
    }
}
