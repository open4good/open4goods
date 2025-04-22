// src/main/java/org/open4goods/ui/controllers/ui/pages/FeedbackController.java
package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.helper.IpHelper;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.captcha.config.HcaptchaProperties;
import org.open4goods.services.feedback.dto.IssueDTO;
import org.open4goods.services.feedback.exception.VotingLimitExceededException;
import org.open4goods.services.feedback.exception.VotingNotAllowedException;
import org.open4goods.services.feedback.service.IssueService;
import org.open4goods.services.feedback.service.VoteResponse;
import org.open4goods.services.feedback.service.VoteService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Thymeleaf + AJAX controller for creating issues and voting.
 */
@Controller
public class FeedbackController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackController.class);

    private final UiService uiService;
    private final UiConfig uiConfig;
    private final IssueService issueService;
    private final VoteService voteService;
    private final HcaptchaService hcaptchaService;
    private final HcaptchaProperties hcaptchaProperties;

    public FeedbackController(UiService uiService,
                              UiConfig uiConfig,
                              IssueService issueService,
                              VoteService voteService,
                              HcaptchaService hcaptchaService,
                              HcaptchaProperties hcaptchaProperties) {
        this.uiService   = uiService;
        this.uiConfig    = uiConfig;
        this.issueService = issueService;
        this.voteService = voteService;
        this.hcaptchaService = hcaptchaService;
        this.hcaptchaProperties = hcaptchaProperties;
    }

    @GetMapping("/feedback/issue")
    public ModelAndView issueForm(HttpServletRequest request) {
        String ip = IpHelper.getIp(request);
        var model = uiService.defaultModelAndView("feedback-issue", request);
        model.addObject("votes", voteService.getRemainingVotes(ip));
        model.addObject("hcaptchaSiteKey", hcaptchaProperties.getKey());
        return model;
    }

    @GetMapping("/feedback/idea")
    public ModelAndView ideaForm(HttpServletRequest request) {
        return uiService.defaultModelAndView("feedback-idea", request);
    }

    @PostMapping("/feedback")
    public ModelAndView createIssue(
            HttpServletRequest request,
            @RequestBody MultiValueMap<String, String> formData
    ) {
        String ip = IpHelper.getIp(request);
        // Verify hCaptcha
        try {
            String token = formData.getFirst("h-captcha-response");
            hcaptchaService.verifyRecaptcha(ip, token);
        } catch (SecurityException e) {
            LOGGER.warn("Invalid captcha: {}", e.getMessage());
            return uiService.defaultModelAndView("feedback-error", request)
                             .addObject("msg", "Captcha non valide : " + e.getMessage());
        }

        // Create GitHub issue
        try {
            Set<String> labels = new HashSet<>(Set.of("nudger.fr", "feedback"));
            String type = formData.getFirst("type");
            if ("bug".equals(type)) {
                issueService.createBug(
                    formData.getFirst("title"),
                    formData.getFirst("message"),
                    formData.getFirst("url"),
                    formData.getFirst("author"),
                    labels
                );
            } else {
                issueService.createIdea(
                    formData.getFirst("title"),
                    formData.getFirst("message"),
                    formData.getFirst("url"),
                    formData.getFirst("author"),
                    labels
                );
            }
        } catch (IOException e) {
            LOGGER.error("Error creating GitHub issue", e);
            return uiService.defaultModelAndView("feedback-error", request)
                            .addObject("msg", "Internal error: " + e.getMessage());
        }
        return uiService.defaultModelAndView("feedback-success", request)
                        .addObject("backUrl", formData.getFirst("url"));
    }

    @GetMapping("/feedback/votes/remaining")
    @ResponseBody
    public Map<String, Integer> remainingVotes(HttpServletRequest request) {
        String ip = IpHelper.getIp(request);
        return Map.of("remainingVotes", voteService.getRemainingVotes(ip));
    }

    @GetMapping("/feedback/votes/can")
    @ResponseBody
    public Map<String, Boolean> canVote(HttpServletRequest request) {
        String ip = IpHelper.getIp(request);
        return Map.of("canVote", voteService.userCanVote(ip));
    }

    @PostMapping("/feedback/vote")
    @ResponseBody
    public ResponseEntity<?> voteOnIssue(
            @RequestParam("issueId") String issueId,
            HttpServletRequest request
    ) {
        String ip = IpHelper.getIp(request);
        try {
            VoteResponse resp = voteService.vote(issueId, ip);
            return ResponseEntity.ok(Map.of(
                "remainingVotes", resp.remainingVotes(),
                "totalVotes", resp.totalVotes()
            ));
        } catch (VotingNotAllowedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", ex.getMessage()));
        } catch (VotingLimitExceededException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/feedback/ideas")
    @ResponseBody
    public List<IssueDTO> listIdeas() throws IOException {
        return issueService.listIdeas().stream()
            .map(issue -> new IssueDTO(
                String.valueOf(issue.getNumber()),
                issue.getNumber(),
                issue.getTitle(),
                issue.getHtmlUrl().toString(),
                voteService.getTotalVotes(String.valueOf(issue.getNumber()))
            ))
            .sorted((a, b) -> Integer.compare(b.votes(), a.votes()))
            .toList();
    }

    @GetMapping("/feedback/bugs")
    @ResponseBody
    public List<IssueDTO> listBugs() throws IOException {
        return issueService.listBugs().stream()
            .map(issue -> new IssueDTO(
                String.valueOf(issue.getNumber()),
                issue.getNumber(),
                issue.getTitle(),
                issue.getHtmlUrl().toString(),
                voteService.getTotalVotes(String.valueOf(issue.getNumber()))
            ))
            .sorted((a, b) -> Integer.compare(b.votes(), a.votes()))
            .toList();
    }
}
