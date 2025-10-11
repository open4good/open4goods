package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategorySummaryDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.GoogleCategoryNavigationService;
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
 * REST controller exposing navigation endpoints over the Google taxonomy tree used by the frontend.
 */
@RestController
@RequestMapping("/categories/taxonomy")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Categories", description = "Explore Google taxonomy categories and their associated vertical configurations.")
public class GoogleCategoriesController {

    private final GoogleCategoryNavigationService navigationService;

    public GoogleCategoriesController(GoogleCategoryNavigationService navigationService) {
        this.navigationService = navigationService;
    }

    /**
     * Retrieve a taxonomy category by its Google taxonomy identifier.
     */
    @GetMapping("/{taxonomyId}")
    @Operation(
            summary = "Get taxonomy category by id",
            description = "Return a taxonomy category enriched with breadcrumbs, children and associated vertical configuration.",
            parameters = {
                    @Parameter(name = "taxonomyId", in = ParameterIn.PATH, required = true,
                            description = "Google taxonomy identifier of the category to retrieve.",
                            schema = @Schema(type = "integer", example = "1234")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GoogleCategoryDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<GoogleCategoryDto> getCategoryById(
            @PathVariable("taxonomyId") Integer taxonomyId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getCategoryById(taxonomyId, domainLanguage)
                .map(dto -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(dto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve the taxonomy root category without providing a slug path.
     */
    @GetMapping("/path")
    @Operation(
            summary = "Get taxonomy root category",
            description = "Return the root taxonomy node enriched with breadcrumbs, children and associated vertical configuration.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GoogleCategoryDto.class)))
            }
    )
    public ResponseEntity<GoogleCategoryDto> getRootCategory(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getCategoryByPath(null, domainLanguage)
                .map(dto -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(dto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve a taxonomy category by its slug path.
     */
    @GetMapping("/path/{categoryPath:.+}")
    @Operation(
            summary = "Get taxonomy category by slug path",
            description = "Return a taxonomy category referenced by its slug path relative to the taxonomy root.",
            parameters = {
                    @Parameter(name = "categoryPath", in = ParameterIn.PATH, required = true,
                            description = "Slug path of the taxonomy category (e.g. electronics/televisions).",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GoogleCategoryDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<GoogleCategoryDto> getCategoryByPath(
            @PathVariable(name = "categoryPath") String categoryPath,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getCategoryByPath(categoryPath, domainLanguage)
                .map(dto -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(dto))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve the immediate children of a taxonomy category by id.
     */
    @GetMapping("/{taxonomyId}/children")
    @Operation(
            summary = "List taxonomy children by id",
            description = "Return the immediate children of a taxonomy node identified by its Google taxonomy id.",
            parameters = {
                    @Parameter(name = "taxonomyId", in = ParameterIn.PATH, required = true,
                            description = "Google taxonomy identifier of the parent category.",
                            schema = @Schema(type = "integer", example = "1234")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Children returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = GoogleCategorySummaryDto.class)))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<GoogleCategorySummaryDto>> getChildrenById(
            @PathVariable("taxonomyId") Integer taxonomyId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getChildrenById(taxonomyId, domainLanguage)
                .map(children -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(children))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve the immediate children of a taxonomy category by slug path.
     */
    @GetMapping("/path/children")
    @Operation(
            summary = "List root taxonomy children",
            description = "Return the immediate children of the taxonomy root node.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Children returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = GoogleCategorySummaryDto.class))))
            }
    )
    public ResponseEntity<List<GoogleCategorySummaryDto>> getRootChildren(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getChildrenByPath(null, domainLanguage)
                .map(children -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(children))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/path/{categoryPath:.+}/children")
    @Operation(
            summary = "List taxonomy children by slug path",
            description = "Return the immediate children of a taxonomy node referenced by its slug path.",
            parameters = {
                    @Parameter(name = "categoryPath", in = ParameterIn.PATH, required = true,
                            description = "Slug path of the taxonomy category (e.g. electronics/televisions).",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Children returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = GoogleCategorySummaryDto.class)))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<GoogleCategorySummaryDto>> getChildrenByPath(
            @PathVariable(name = "categoryPath") String categoryPath,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return navigationService.getChildrenByPath(categoryPath, domainLanguage)
                .map(children -> ResponseEntity.ok()
                        .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                        .header("X-Locale", domainLanguage.languageTag())
                        .body(children))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
