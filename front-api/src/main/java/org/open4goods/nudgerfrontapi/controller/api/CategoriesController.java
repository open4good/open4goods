package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.CategoryMappingService;
import org.open4goods.verticals.VerticalsConfigService;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing metadata about the available vertical categories for the frontend UI.
 */
@RestController
@RequestMapping("/category")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Categories", description = "Retrieve vertical configurations displayed in the catalog navigation.")
public class CategoriesController {

    private static final CacheControl FIFTEEN_MINUTES_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic();

    private final VerticalsConfigService verticalsConfigService;
    private final CategoryMappingService categoryMappingService;

    public CategoriesController(VerticalsConfigService verticalsConfigService,
                                CategoryMappingService categoryMappingService) {
        this.verticalsConfigService = verticalsConfigService;
        this.categoryMappingService = categoryMappingService;
    }

    @GetMapping
    @Operation(
            summary = "List categories",
            description = "Return vertical configurations optionally filtered by their enabled status.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @Parameter(name = "onlyEnabled", in = ParameterIn.QUERY, required = false,
                            description = "When true, only return verticals flagged as enabled.",
                            schema = @Schema(type = "boolean", defaultValue = "true"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = VerticalConfigDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<VerticalConfigDto>> categories(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "onlyEnabled", defaultValue = "true") boolean onlyEnabled) {
        List<VerticalConfigDto> body = verticalsConfigService.getConfigsWithoutDefault(onlyEnabled).stream()
                .map(categoryMappingService::toVerticalConfigDto)
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/{categoryId}")
    @Operation(
            summary = "Get category details",
            description = "Return the detailed vertical configuration identified by its id.",
            parameters = {
                    @Parameter(name = "categoryId", in = ParameterIn.PATH, required = true,
                            description = "Identifier of the vertical to retrieve.",
                            schema = @Schema(type = "string", example = "tv")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = VerticalConfigFullDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<VerticalConfigFullDto> category(@PathVariable("categoryId") String categoryId,
                                                          @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        VerticalConfig config = verticalsConfigService.getConfigById(categoryId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        VerticalConfigFullDto body = categoryMappingService.toVerticalConfigFullDto(config);
        return ResponseEntity.ok()
                .cacheControl(FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(body);
    }
}
