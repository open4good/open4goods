package org.open4goods.nudgerfrontapi.controller.api;

import java.io.IOException;
import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackErrorResponseDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackIssueType;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackRemainingVotesDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackSubmissionRequestDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackSubmissionResponseDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteEligibilityDto;
import org.open4goods.nudgerfrontapi.dto.feedback.FeedbackVoteResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.FeedbackService;
import org.open4goods.nudgerfrontapi.service.FeedbackService.FeedbackSubmissionResult;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.open4goods.services.feedback.exception.VotingLimitExceededException;
import org.open4goods.services.feedback.exception.VotingNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller exposing feedback submission and voting features for the frontend.
 */
@RestController
@RequestMapping("/feedback")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Feedback", description = "Feedback submission and voting")
public class FeedbackController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackController.class);

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(
            summary = "Submit a feedback entry",
            description = "Verify hCaptcha token then create a GitHub issue representing the feedback.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Feedback payload to submit.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FeedbackSubmissionRequestDto.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Feedback registered",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackSubmissionResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Captcha validation failed",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackSubmissionResponseDto.class))),
                    @ApiResponse(responseCode = "500", description = "Unable to create GitHub issue",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackSubmissionResponseDto.class)))
            }
    )
    public ResponseEntity<FeedbackSubmissionResponseDto> submitFeedback(@Valid @RequestBody FeedbackSubmissionRequestDto request,
                                                                        @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                                        HttpServletRequest httpRequest) {
        String clientIp = IpUtils.getIp(httpRequest);
        try {
            FeedbackSubmissionResult result = feedbackService.submitFeedback(request, clientIp);
            FeedbackSubmissionResponseDto body = new FeedbackSubmissionResponseDto(true, result.issueNumber(),
                    result.issueUrl(), "Merci pour votre retour !");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        } catch (SecurityException e) {
            LOGGER.warn("Invalid captcha while submitting feedback: {}", e.getMessage());
            FeedbackSubmissionResponseDto body = new FeedbackSubmissionResponseDto(false, null, null,
                    "Captcha invalide : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        } catch (IOException e) {
            LOGGER.error("Failed to create feedback issue", e);
            FeedbackSubmissionResponseDto body = new FeedbackSubmissionResponseDto(false, null, null,
                    "Une erreur interne est survenue lors de la création de l'issue.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        }
    }

    @GetMapping("/issues")
    @Operation(
            summary = "List feedback issues",
            description = "Return GitHub issues labelled for feedback along with their vote counts.",
            parameters = {
                    @Parameter(name = "type", in = ParameterIn.QUERY, required = false,
                            description = "Optional filter on feedback type.",
                            schema = @Schema(implementation = FeedbackIssueType.class)),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Issues returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = FeedbackIssueDto.class)))),
                    @ApiResponse(responseCode = "502", description = "Upstream GitHub failure",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackErrorResponseDto.class)))
            }
    )
    public ResponseEntity<?> listIssues(@RequestParam(name = "type", required = false) FeedbackIssueType type,
                                        @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            List<FeedbackIssueDto> issues = feedbackService.listIssues(type);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(issues);
        } catch (IOException e) {
            LOGGER.error("Failed to list feedback issues", e);
            FeedbackErrorResponseDto body = new FeedbackErrorResponseDto(
                    "Impossible de récupérer les issues GitHub pour le moment.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        }
    }

    @GetMapping("/votes/remaining")
    @Operation(
            summary = "Get remaining votes",
            description = "Return the number of votes still available for the caller today.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Remaining votes",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackRemainingVotesDto.class)))
            }
    )
    public ResponseEntity<FeedbackRemainingVotesDto> remainingVotes(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                                   HttpServletRequest httpRequest) {
        FeedbackRemainingVotesDto body = feedbackService.remainingVotes(IpUtils.getIp(httpRequest));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    @GetMapping("/votes/can")
    @Operation(
            summary = "Check voting eligibility",
            description = "Indicate whether the caller may still vote today.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Eligibility returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackVoteEligibilityDto.class)))
            }
    )
    public ResponseEntity<FeedbackVoteEligibilityDto> canVote(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                              HttpServletRequest httpRequest) {
        FeedbackVoteEligibilityDto body = feedbackService.canVote(IpUtils.getIp(httpRequest));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    @PostMapping("/vote")
    @Operation(
            summary = "Vote on a feedback issue",
            description = "Register a vote for the provided GitHub issue if quotas allow it.",
            parameters = {
                    @Parameter(name = "issueId", in = ParameterIn.QUERY, required = true,
                            description = "GitHub issue number to vote for.",
                            schema = @Schema(type = "string", example = "128")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Vote accepted",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackVoteResponseDto.class))),
                    @ApiResponse(responseCode = "403", description = "Voting not allowed on this issue",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackErrorResponseDto.class))),
                    @ApiResponse(responseCode = "429", description = "Vote quota exceeded",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FeedbackErrorResponseDto.class)))
            }
    )
    public ResponseEntity<?> vote(@RequestParam(name = "issueId") String issueId,
                                  @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                  HttpServletRequest httpRequest) {
        String clientIp = IpUtils.getIp(httpRequest);
        try {
            FeedbackVoteResponseDto body = feedbackService.vote(issueId, clientIp);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        } catch (VotingNotAllowedException ex) {
            LOGGER.warn("Voting not allowed for issue {}: {}", issueId, ex.getMessage());
            FeedbackErrorResponseDto body = new FeedbackErrorResponseDto(ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        } catch (VotingLimitExceededException ex) {
            LOGGER.warn("Vote limit reached for IP {}", clientIp);
            FeedbackErrorResponseDto body = new FeedbackErrorResponseDto(ex.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(body);
        }
    }
}
