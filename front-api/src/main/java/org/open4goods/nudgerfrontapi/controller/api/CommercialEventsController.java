package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.event.CommercialEventDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.CommercialEventService;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing commercial events highlighted in the eco-nudger
 * frontend.
 */
@RestController
@RequestMapping("/commercial-events")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Commercial events", description = "Expose configured promotional events to the frontend.")
public class CommercialEventsController {

    private final CommercialEventService commercialEventService;

    public CommercialEventsController(CommercialEventService commercialEventService) {
        this.commercialEventService = commercialEventService;
    }

    /**
     * List commercial events configured in the application properties.
     *
     * @param domainLanguage mandatory domain language hint driving localisation
     * @return commercial events ready to be consumed by the frontend. Results are cached for one hour using the
     *         {@link CacheConstants#ONE_HOUR_LOCAL_CACHE_NAME} cache to avoid recomputing static configuration.
     */
    @GetMapping
    @Operation(
            summary = "List commercial events",
            description = "Return configured commercial events for the requested domain language.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Commercial events returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = CommercialEventDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<List<CommercialEventDto>> commercialEvents(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<CommercialEventDto> events = commercialEventService.commercialEvents(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(events);
    }
}
