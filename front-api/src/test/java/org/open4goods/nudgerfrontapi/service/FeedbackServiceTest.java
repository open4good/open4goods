package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueType;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackSubmissionRequestDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteEligibilityDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteResponseDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackRemainingVotesDto;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.feedback.dto.IssueDto;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.feedback.service.VoteResponse;
import org.open4goods.services.feedback.service.VoteService;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private IssueService issueService;
    @Mock
    private VoteService voteService;
    @Mock
    private HcaptchaService hcaptchaService;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(issueService, voteService, hcaptchaService);
    }

    @Test
    void shouldSubmitIdeaFeedback() throws Exception {
        FeedbackSubmissionRequestDto request = new FeedbackSubmissionRequestDto(
                FeedbackIssueType.IDEA,
                "Un titre",
                "Un message",
                "https://nudger.fr/page",
                "Jean",
                "token");

        IssueDto issue = new IssueDto("42", 42, "https://github.com/open4good/open4goods/issues/42", "OPEN", "Un titre", null);
        
        when(issueService.createIdea(eq("Un titre"), eq("Un message"), eq("https://nudger.fr/page"), eq("Jean"), any()))
                .thenReturn(issue);

        FeedbackService.FeedbackSubmissionResult result = feedbackService.submitFeedback(request, "127.0.0.1");

        verify(hcaptchaService).verifyRecaptcha("127.0.0.1", "token");
        ArgumentCaptor<Set<String>> labelsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(issueService).createIdea(eq("Un titre"), eq("Un message"), eq("https://nudger.fr/page"), eq("Jean"),
                labelsCaptor.capture());
        assertThat(labelsCaptor.getValue()).contains("nudger.fr", "feedback");
        assertThat(result.issueNumber()).isEqualTo(42);
        assertThat(result.issueUrl()).isEqualTo("https://github.com/open4good/open4goods/issues/42");
    }

    @Test
    void shouldSubmitBugFeedback() throws Exception {
        FeedbackSubmissionRequestDto request = new FeedbackSubmissionRequestDto(
                FeedbackIssueType.BUG,
                "Bug",
                "Description",
                null,
                null,
                "token");

        IssueDto issue = new IssueDto("7", 7, "https://github.com/open4good/open4goods/issues/7", "OPEN", "Bug", null);

        when(issueService.createBug(eq("Bug"), eq("Description"), eq((String) null), eq((String) null), any()))
                .thenReturn(issue);

        FeedbackService.FeedbackSubmissionResult result = feedbackService.submitFeedback(request, "10.0.0.1");

        verify(hcaptchaService).verifyRecaptcha("10.0.0.1", "token");
        verify(issueService).createBug(eq("Bug"), eq("Description"), eq((String) null), eq((String) null), any());
        assertThat(result.issueNumber()).isEqualTo(7);
        assertThat(result.issueUrl()).isEqualTo("https://github.com/open4good/open4goods/issues/7");
    }

    @Test
    void shouldListIssuesSortedByVotes() throws Exception {
        IssueDto first = new IssueDto("1", 1, "https://github.com/open4good/open4goods/issues/1", "OPEN", "Idea 1", null);
        IssueDto second = new IssueDto("2", 2, "https://github.com/open4good/open4goods/issues/2", "OPEN", "Idea 2", null);

        when(issueService.listIdeas()).thenReturn(List.of(first, second));
        when(voteService.getTotalVotes("1")).thenReturn(3);
        when(voteService.getTotalVotes("2")).thenReturn(8);

        List<FeedbackIssueDto> issues = feedbackService.listIssues(FeedbackIssueType.IDEA);

        assertThat(issues).extracting(FeedbackIssueDto::number).containsExactly(2, 1);
        assertThat(issues.get(0).votes()).isEqualTo(8);
        assertThat(issues.get(1).votes()).isEqualTo(3);
    }

    @Test
    void shouldCastVote() {
        when(voteService.vote("12", "ip")).thenReturn(new VoteResponse(4, 10));

        FeedbackVoteResponseDto response = feedbackService.vote("12", "ip");

        assertThat(response.remainingVotes()).isEqualTo(4);
        assertThat(response.totalVotes()).isEqualTo(10);
    }

    @Test
    void shouldReturnRemainingVotes() {
        when(voteService.getRemainingVotes("ip")).thenReturn(2);

        FeedbackRemainingVotesDto dto = feedbackService.remainingVotes("ip");

        assertThat(dto.remainingVotes()).isEqualTo(2);
    }

    @Test
    void shouldReturnEligibility() {
        when(voteService.userCanVote("ip")).thenReturn(true);

        FeedbackVoteEligibilityDto dto = feedbackService.canVote("ip");

        assertThat(dto.canVote()).isTrue();
    }
}
