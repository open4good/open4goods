package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.config.properties.TeamProperties;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.springframework.cache.annotation.Cacheable;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Public controller exposing the eco-nudger team roster for the frontend.
 */
@RestController
@RequestMapping("/team")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Team", description = "Nudger team exposed to the frontend")
public class TeamController {

    private final TeamProperties teamProperties;

    public TeamController(TeamProperties teamProperties) {
        this.teamProperties = teamProperties;
    }

    /**
     * Expose the static team roster.
     *
     * @param domainLanguage mandatory domain language hint (future localisation use)
     * @return configured team roster
     */
    @GetMapping
    @Operation(
            summary = "Get eco-nudger team roster",
            description = "Return the configured list of core team members and contributors.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Team roster returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TeamProperties.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<TeamProperties> team(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(teamProperties);
    }
}
