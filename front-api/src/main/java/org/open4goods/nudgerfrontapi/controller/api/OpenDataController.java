package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDatasetDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataOverviewDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.OpenDataMappingService;
import java.util.List;

import jakarta.validation.constraints.Pattern;
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
 * REST controller exposing OpenData dataset metadata for the frontend.
 */
@RestController
@RequestMapping("/opendata")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "OpenData", description = "Metadata describing the public GTIN and ISBN datasets.")
public class OpenDataController {

    private final OpenDataMappingService openDataFrontService;

    public OpenDataController(OpenDataMappingService openDataFrontService) {
        this.openDataFrontService = openDataFrontService;
    }

    @GetMapping
    @Operation(
            summary = "Get OpenData overview",
            description = "Return aggregated metadata about the available GTIN and ISBN datasets.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Overview returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OpenDataOverviewDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<OpenDataOverviewDto> overview(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        OpenDataOverviewDto body = openDataFrontService.overview(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", openDataFrontService.resolvedLocaleTag(domainLanguage))
                .body(body);
    }

    @GetMapping("/gtin")
    @Operation(
            summary = "Get GTIN dataset metadata",
            description = "Return metadata describing the GTIN OpenData export.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataset returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OpenDataDatasetDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<OpenDataDatasetDto> gtin(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        OpenDataDatasetDto body = openDataFrontService.gtin(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", openDataFrontService.resolvedLocaleTag(domainLanguage))
                .body(body);
    }

    @GetMapping("/isbn")
    @Operation(
            summary = "Get ISBN dataset metadata",
            description = "Return metadata describing the ISBN OpenData export.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataset returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OpenDataDatasetDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<OpenDataDatasetDto> isbn(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        OpenDataDatasetDto body = openDataFrontService.isbn(domainLanguage);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", openDataFrontService.resolvedLocaleTag(domainLanguage))
                .body(body);
    }

    @GetMapping("/impact")
    @Operation(
            summary = "List impact datasets",
            description = "Return the impact datasets (one per vertical) listed in the published manifest. Empty "
                    + "while the manifest is not configured or not readable.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Datasets returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = OpenDataDatasetDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<OpenDataDatasetDto>> impactDatasets(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", openDataFrontService.resolvedLocaleTag(domainLanguage))
                .body(openDataFrontService.impactDatasets(domainLanguage));
    }

    @GetMapping("/impact/{vertical}")
    @Operation(
            summary = "Get an impact dataset",
            description = "Return the impact dataset of one vertical, read from the published manifest.",
            parameters = {
                    @Parameter(name = "vertical", in = ParameterIn.PATH, required = true,
                            description = "Vertical identifier, without the impact- prefix.",
                            schema = @Schema(type = "string", example = "dishwasher")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dataset returned",
                            headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OpenDataDatasetDto.class))),
                    @ApiResponse(responseCode = "404", description = "No impact dataset for this vertical"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<OpenDataDatasetDto> impactDataset(
            @PathVariable("vertical") @Pattern(regexp = "[a-z0-9][a-z0-9-]*") String vertical,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return openDataFrontService.impactDataset(domainLanguage, vertical)
                .map(body -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", openDataFrontService.resolvedLocaleTag(domainLanguage))
                        .body(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
