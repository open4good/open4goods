package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.services.feedback.dto.IssueDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueType;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackSubmissionRequestDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteEligibilityDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteResponseDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackRemainingVotesDto;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.feedback.service.VoteResponse;
import org.open4goods.services.feedback.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Domain service orchestrating captcha verification, GitHub issue creation and voting flows.
 * <p>
 * Every entry point centralises the interaction between the public REST API and the
 * underlying GitHub-backed feedback system. Keeping the orchestration in a dedicated
 * service makes controller code predictable and simplifies future migrations away from
 * GitHub issues.
 * </p>
 */
@Service
public class FeedbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackService.class);

    private static final Set<String> DEFAULT_LABELS = Set.of("nudger.fr", "feedback");

    private final IssueService issueService;
    private final VoteService voteService;
    private final HcaptchaService hcaptchaService;

    public FeedbackService(IssueService issueService,
                           VoteService voteService,
                           HcaptchaService hcaptchaService) {
        this.issueService = issueService;
        this.voteService = voteService;
        this.hcaptchaService = hcaptchaService;
    }

    /**
     * Verify captcha token then create the corresponding GitHub issue.
     *
     * @param request feedback payload submitted by the client
     * @param clientIp originating IP address used for captcha verification
     * @return details about the created issue (number and HTML URL)
     * @throws SecurityException when captcha verification fails
     * @throws IOException       when GitHub communication fails
     */
    public FeedbackSubmissionResult submitFeedback(FeedbackSubmissionRequestDto request, String clientIp)
            throws IOException {
        LOGGER.debug("Submitting {} feedback from IP {}", request.type(), clientIp);
        hcaptchaService.verifyRecaptcha(clientIp, request.captchaResponse());

        Set<String> labels = new HashSet<>(DEFAULT_LABELS);
        // Ensure the type specific label is always present so triaging stays reliable.
        IssueDto created = switch (request.type()) {
            case BUG -> issueService.createBug(request.title(), request.message(), request.url(), request.author(), labels);
            case IDEA -> issueService.createIdea(request.title(), request.message(), request.url(), request.author(), labels);
        };

        LOGGER.info("Created GitHub issue #{} of type {}", created.number(), request.type());
        return new FeedbackSubmissionResult(created.number(), created.htmlUrl() == null ? null
                : created.htmlUrl());
    }

    /**
     * Retrieve and map issues for the requested category.
     *
     * @param type category to filter on, or {@code null} to return all votable issues
     * @return sorted issues enriched with vote totals
     * @throws IOException when GitHub communication fails
     */
    public List<FeedbackIssueDto> listIssues(FeedbackIssueType type) throws IOException {
        List<IssueDto> issues = new ArrayList<>();
        if (type == null) {
            issues.addAll(issueService.listIssues());
        } else {
            switch (type) {
                case BUG -> issues.addAll(issueService.listBugs());
                case IDEA -> issues.addAll(issueService.listIdeas());
            }
        }

        return issues.stream()
                .map(this::mapIssue)
                .sorted(Comparator.comparingInt(FeedbackIssueDto::votes).reversed())
                .toList();
    }

    // ... (vote, remainingVotes, canVote methods unchanged) ...

    /**
     * Cast a vote on the requested GitHub issue and expose the new totals.
     *
     * @param issueId GitHub issue identifier (number)
     * @param clientIp originating IP address
     * @return vote summary after casting the vote
     */
    public FeedbackVoteResponseDto vote(String issueId, String clientIp) {
        VoteResponse response = voteService.vote(issueId, clientIp);
        return new FeedbackVoteResponseDto(response.remainingVotes(), response.totalVotes());
    }

    /**
     * Retrieve how many votes remain for the current IP address.
     *
     * @param clientIp originating IP address
     * @return remaining votes for the day
     */
    public FeedbackRemainingVotesDto remainingVotes(String clientIp) {
        return new FeedbackRemainingVotesDto(voteService.getRemainingVotes(clientIp));
    }

    /**
     * Determine if the current IP address can still cast votes today.
     *
     * @param clientIp originating IP address
     * @return whether the user can still vote today
     */
    public FeedbackVoteEligibilityDto canVote(String clientIp) {
        return new FeedbackVoteEligibilityDto(voteService.userCanVote(clientIp));
    }

    /**
     * Map the GitHub issue returned by the backend to the DTO expected by the frontend.
     *
     * @param issue GitHub issue fetched through the REST API
     * @return immutable DTO exposing the public information about the issue
     */
    private FeedbackIssueDto mapIssue(IssueDto issue) {
        String issueId = String.valueOf(issue.number());
        String url = issue.htmlUrl();
        int votes = voteService.getTotalVotes(issueId);
        return new FeedbackIssueDto(issueId, issue.number(), issue.title(), url, votes);
    }

    /**
     * Simple value object used to propagate information about a created issue.
     *
     * @param issueNumber sequential GitHub number
     * @param issueUrl    public HTML URL of the issue
     */
    public record FeedbackSubmissionResult(int issueNumber, String issueUrl) { }
}
