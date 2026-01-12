package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.commons.model.IpQuotaCategory;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.quota.IpQuotaStatusDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.QuotaService;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller exposing IP quota status endpoints.
 */
@RestController
@RequestMapping("/quota")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Quota", description = "IP quota usage and limits")
public class QuotaController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaController.class);

    private final QuotaService quotaService;

    /**
     * Create a controller using the quota service.
     *
     * @param quotaService service providing quota status
     */
    public QuotaController(QuotaService quotaService)
    {
        this.quotaService = quotaService;
    }

    /**
     * Return the current quota status for the requested category.
     *
     * @param category quota category to resolve
     * @param domainLanguage locale hint for response headers
     * @param httpRequest HTTP request used to resolve the client IP
     * @return quota status response
     */
    @GetMapping("/{category}")
    @Operation(
            summary = "Get IP quota status",
            description = "Return the quota usage and remaining tokens for the requested category.",
            parameters = {
                    @Parameter(name = "category", in = ParameterIn.PATH, required = true,
                            description = "Quota category identifier.",
                            schema = @Schema(implementation = IpQuotaCategory.class)),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Quota status returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = IpQuotaStatusDto.class)))
            }
    )
    public ResponseEntity<IpQuotaStatusDto> getQuotaStatus(@PathVariable("category") IpQuotaCategory category,
                                                           @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                           HttpServletRequest httpRequest)
    {
        String clientIp = IpUtils.getIp(httpRequest);
        LOGGER.info("Quota status requested for category {} from IP {}", category, clientIp);
        IpQuotaStatusDto body = quotaService.getQuotaStatus(category, clientIp);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }
}
