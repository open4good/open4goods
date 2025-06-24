package org.open4goods.api.controller.api;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssue;
import org.open4goods.api.dto.CreateIssueRequest;
import org.open4goods.commons.helper.IpHelper;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.feedback.dto.IssueDTO;
import org.open4goods.services.feedback.exception.VotingLimitExceededException;
import org.open4goods.services.feedback.exception.VotingNotAllowedException;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.feedback.service.VoteResponse;
import org.open4goods.services.feedback.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST endpoints for creating feedback issues and voting on them.
 */
@RestController
public class FeedbackController {

    private final IssueService issueService;
    private final VoteService voteService;
    private final HcaptchaService hcaptchaService;

    public FeedbackController(IssueService issueService,
                              VoteService voteService,
                              HcaptchaService hcaptchaService) {
        this.issueService = issueService;
        this.voteService = voteService;
        this.hcaptchaService = hcaptchaService;
    }

    @GetMapping("/issues")
    @Operation(summary = "List open feedback issues")
    public List<IssueDTO> listIssues() throws IOException {
        return issueService.listIssues().stream()
                .map(issue -> new IssueDTO(
                        String.valueOf(issue.getNumber()),
                        issue.getNumber(),
                        issue.getTitle(),
                        issue.getHtmlUrl().toString(),
                        voteService.getTotalVotes(String.valueOf(issue.getNumber()))
                ))
                .sorted((a, b) -> Integer.compare(b.votes(), a.votes()))
                .collect(Collectors.toList());
    }

    @PostMapping(value = "/issues", headers = "X-Hcaptcha-Response")
    @Operation(summary = "Create a new feedback issue")
    public ResponseEntity<Void> createIssue(
            @RequestHeader("X-Hcaptcha-Response") String captcha,
            @RequestBody CreateIssueRequest request,
            HttpServletRequest httpRequest
    ) throws IOException {
        String ip = IpHelper.getIp(httpRequest);
        hcaptchaService.verifyRecaptcha(ip, captcha);

        Set<String> labels = request.labels() == null ? new HashSet<>() : new HashSet<>(request.labels());
        GHIssue issue;
        if ("bug".equalsIgnoreCase(request.type())) {
            issue = issueService.createBug(request.title(), request.message(), request.url(), request.author(), labels);
        } else {
            issue = issueService.createIdea(request.title(), request.message(), request.url(), request.author(), labels);
        }
        URI location = issue.getHtmlUrl().toURI();
        return ResponseEntity.created(location).build();
    }

    @PostMapping(value = "/issues/{id}/votes", headers = "X-Hcaptcha-Response")
    @Operation(summary = "Cast a vote on an issue")
    public ResponseEntity<?> vote(
            @PathVariable String id,
            @RequestHeader("X-Hcaptcha-Response") String captcha,
            HttpServletRequest httpRequest
    ) {
        String ip = IpHelper.getIp(httpRequest);
        hcaptchaService.verifyRecaptcha(ip, captcha);
        try {
            VoteResponse resp = voteService.vote(id, ip);
            return ResponseEntity.ok(resp);
        } catch (VotingNotAllowedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("message", ex.getMessage()));
        } catch (VotingLimitExceededException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}
