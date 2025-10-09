package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.config.properties.EcosystemPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.MentorPartnersProperties;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.partner.AffiliationPartnerDto;
import org.open4goods.nudgerfrontapi.dto.partner.StaticPartnerDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.AffiliationPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

/**
 * REST controller exposing affiliation partners consumed by the nudger frontend.
 */
@RestController
@RequestMapping("/partners")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Partner", description = "Expose partner directories consumed by the eco-nudger frontend.")
public class PartnerController {

    private final AffiliationPartnerService affiliationPartnerService;
    private final EcosystemPartnersProperties ecosystemPartnersProperties;
    private final MentorPartnersProperties mentorPartnersProperties;

    public PartnerController(AffiliationPartnerService affiliationPartnerService,
            EcosystemPartnersProperties ecosystemPartnersProperties,
            MentorPartnersProperties mentorPartnersProperties) {
        this.affiliationPartnerService = affiliationPartnerService;
        this.ecosystemPartnersProperties = ecosystemPartnersProperties;
        this.mentorPartnersProperties = mentorPartnersProperties;
    }

    /**
     * List affiliation partners available for the frontend affiliation program.
     *
     * @param domainLanguage mandatory domain language hint
     * @return affiliation partner list enriched with computed assets
     */
    @GetMapping("/affiliation")
    @Operation(
            summary = "List affiliation partners",
            description = "Return affiliation partners enriched with front-served logo and favicon URLs.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Affiliation partners returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AffiliationPartnerDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<AffiliationPartnerDto>> affiliationPartners(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<AffiliationPartnerDto> partnerDtos = affiliationPartnerService.getPartnerDtos();
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(partnerDtos);
    }

    /**
     * List ecosystem partners configured directly from application properties.
     *
     * @param domainLanguage mandatory domain language hint
     * @return ecosystem partner list enriched with static assets
     */
    @GetMapping("/ecosystem")
    @Operation(
            summary = "List ecosystem partners",
            description = "Return ecosystem partners configured from application properties.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ecosystem partners returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = StaticPartnerDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<StaticPartnerDto>> ecosystemPartners(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<StaticPartnerDto> partners = ecosystemPartnersProperties.getPartners().stream()
                .map(partner -> new StaticPartnerDto(partner.getName(), partner.getBlocId(), partner.getUrl(),
                        partner.getImageUrl()))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(partners);
    }

    /**
     * List mentor partners configured directly from application properties.
     *
     * @param domainLanguage mandatory domain language hint
     * @return mentor partner list enriched with static assets
     */
    @GetMapping("/mentors")
    @Operation(
            summary = "List mentor partners",
            description = "Return mentor partners configured from application properties.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mentor partners returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = StaticPartnerDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<StaticPartnerDto>> mentorPartners(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<StaticPartnerDto> partners = mentorPartnersProperties.getPartners().stream()
                .map(partner -> new StaticPartnerDto(partner.getName(), partner.getBlocId(), partner.getUrl(),
                        partner.getImageUrl()))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(partners);
    }
}

