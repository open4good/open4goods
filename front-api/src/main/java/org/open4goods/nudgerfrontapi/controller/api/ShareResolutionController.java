package org.open4goods.nudgerfrontapi.controller.api;

import java.util.Optional;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionRequestDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.share.ShareResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing asynchronous share resolution endpoints.
 */
@RestController
@RequestMapping("/share/resolutions")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Share", description = "Resolve shared URLs into product candidates")
public class ShareResolutionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareResolutionController.class);

    private final ShareResolutionService shareResolutionService;

    public ShareResolutionController(ShareResolutionService shareResolutionService) {
        this.shareResolutionService = shareResolutionService;
    }

    /**
     * Trigger share resolution for a shared URL.
     *
     * @param request        payload containing the shared URL and optional hints
     * @param domainLanguage localisation hint to forward downstream
     * @return pending snapshot with the assigned token
     */
    @PostMapping
    @Operation(summary = "Create a share resolution", description = "Accepts a shared URL and starts asynchronous resolution",
            security = @SecurityRequirement(name = "sharedToken"),
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language hint used to resolve locale specific data.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Resolution accepted",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ShareResolutionResponseDto.class)),
                            headers = @Header(name = "X-Locale", description = "Resolved locale for the response",
                                    schema = @Schema(type = "string", example = "fr"))),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Unexpected error")
            })
    public ResponseEntity<ShareResolutionResponseDto> createResolution(
            @RequestBody ShareResolutionRequestDto request,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Accepted share resolution for url='{}' language={}.", request.url(), domainLanguage);
        ShareResolutionResponseDto response = shareResolutionService.createResolution(request, domainLanguage);
        return ResponseEntity.accepted()
                .cacheControl(CacheControlConstants.PRIVATE_NO_STORE_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(response);
    }

    /**
     * Retrieve the latest status of a resolution.
     *
     * @param token          resolution token
     * @param domainLanguage localisation hint used to echo the locale header
     * @return snapshot when present or 404 otherwise
     */
    @GetMapping("/{token}")
    @Operation(summary = "Get share resolution status",
            description = "Poll for the current status of a share resolution token.",
            security = @SecurityRequirement(name = "sharedToken"),
            parameters = {
                    @Parameter(name = "token", in = ParameterIn.PATH, required = true,
                            description = "Resolution token returned by the create endpoint",
                            schema = @Schema(type = "string", example = "0df7ce2f-3f9e-44d5-a0c4-5f3f0f7d31a7")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language hint used to resolve locale specific data.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Snapshot found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ShareResolutionResponseDto.class)),
                            headers = @Header(name = "X-Locale", description = "Resolved locale for the response",
                                    schema = @Schema(type = "string", example = "en"))),
                    @ApiResponse(responseCode = "404", description = "Unknown token"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden")
            })
    public ResponseEntity<ShareResolutionResponseDto> getResolution(
            @PathVariable("token") String token,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Polling resolution token={} language={}", token, domainLanguage);
        Optional<ShareResolutionResponseDto> snapshot = shareResolutionService.getResolution(token);
        return snapshot.map(body -> ResponseEntity.ok()
                .cacheControl(CacheControlConstants.PRIVATE_NO_STORE_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
