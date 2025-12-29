package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.icecat.model.retailer.RetailerCategory;
import org.open4goods.icecat.services.RetailerCategoryService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.icecat.IcecatRetailerCategoryDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.springframework.cache.annotation.Cacheable;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for Icecat retailer categories.
 * Exposes category data from the Icecat Retailer API to the frontend.
 */
@RestController
@RequestMapping("/icecat/categories")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Icecat Categories", description = "Retrieve Icecat retailer category data")
public class IcecatCategoriesController {

    private final RetailerCategoryService retailerCategoryService;

    /**
     * Constructor.
     *
     * @param retailerCategoryService the retailer category service
     */
    public IcecatCategoriesController(RetailerCategoryService retailerCategoryService) {
        this.retailerCategoryService = retailerCategoryService;
    }

    /**
     * Gets all Icecat retailer categories.
     *
     * @param domainLanguage the domain language for localization
     * @return list of all categories
     */
    @GetMapping
    @Operation(
            summary = "List Icecat categories",
            description = "Return all categories from the Icecat Retailer API.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language for localization.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories returned",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = IcecatRetailerCategoryDto.class)))),
                    @ApiResponse(responseCode = "503", description = "Icecat Retailer API not configured"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<List<IcecatRetailerCategoryDto>> getAllCategories(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {

        if (!retailerCategoryService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }

        List<IcecatRetailerCategoryDto> categories = retailerCategoryService.getAllCategories()
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(categories);
    }

    /**
     * Gets root categories (categories with no parent).
     *
     * @param domainLanguage the domain language for localization
     * @return list of root categories
     */
    @GetMapping("/roots")
    @Operation(
            summary = "List root Icecat categories",
            description = "Return top-level categories (categories with no parent).",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language for localization.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Root categories returned",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = IcecatRetailerCategoryDto.class)))),
                    @ApiResponse(responseCode = "503", description = "Icecat Retailer API not configured")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<List<IcecatRetailerCategoryDto>> getRootCategories(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {

        if (!retailerCategoryService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }

        List<IcecatRetailerCategoryDto> categories = retailerCategoryService.getRootCategories()
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(categories);
    }

    /**
     * Gets a category by ID.
     *
     * @param categoryId     the category ID
     * @param domainLanguage the domain language for localization
     * @return the category details
     */
    @GetMapping("/{categoryId}")
    @Operation(
            summary = "Get Icecat category by ID",
            description = "Return a specific category identified by its ID.",
            parameters = {
                    @Parameter(name = "categoryId", in = ParameterIn.PATH, required = true,
                            description = "Icecat category ID.",
                            schema = @Schema(type = "integer", example = "234")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language for localization.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = IcecatRetailerCategoryDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "503", description = "Icecat Retailer API not configured")
            }
    )
    public ResponseEntity<IcecatRetailerCategoryDto> getCategoryById(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {

        if (!retailerCategoryService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }

        RetailerCategory category = retailerCategoryService.getCategoryById(categoryId);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(toDto(category));
    }

    /**
     * Gets child categories for a parent category.
     *
     * @param parentId       the parent category ID
     * @param domainLanguage the domain language for localization
     * @return list of child categories
     */
    @GetMapping("/{parentId}/children")
    @Operation(
            summary = "Get child categories",
            description = "Return child categories for a given parent category.",
            parameters = {
                    @Parameter(name = "parentId", in = ParameterIn.PATH, required = true,
                            description = "Parent category ID.",
                            schema = @Schema(type = "integer", example = "1")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language for localization.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Child categories returned",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = IcecatRetailerCategoryDto.class)))),
                    @ApiResponse(responseCode = "503", description = "Icecat Retailer API not configured")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<List<IcecatRetailerCategoryDto>> getChildCategories(
            @PathVariable("parentId") Long parentId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {

        if (!retailerCategoryService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }

        List<IcecatRetailerCategoryDto> categories = retailerCategoryService.getChildCategories(parentId)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(categories);
    }

    /**
     * Searches categories by name.
     *
     * @param query          the search query
     * @param domainLanguage the domain language for localization
     * @return matching categories
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search Icecat categories",
            description = "Search categories by name (case-insensitive partial match).",
            parameters = {
                    @Parameter(name = "q", in = ParameterIn.QUERY, required = true,
                            description = "Search query string.",
                            schema = @Schema(type = "string", example = "television")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language for localization.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Matching categories returned",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = IcecatRetailerCategoryDto.class)))),
                    @ApiResponse(responseCode = "503", description = "Icecat Retailer API not configured")
            }
    )
    public ResponseEntity<List<IcecatRetailerCategoryDto>> searchCategories(
            @RequestParam("q") String query,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {

        if (!retailerCategoryService.isConfigured()) {
            return ResponseEntity.status(503).build();
        }

        List<IcecatRetailerCategoryDto> categories = retailerCategoryService.searchByName(query)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(categories);
    }

    /**
     * Converts a RetailerCategory to its DTO representation.
     *
     * @param category the retailer category
     * @return the DTO
     */
    private IcecatRetailerCategoryDto toDto(RetailerCategory category) {
        return new IcecatRetailerCategoryDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getParentCategoryId(),
                category.getLevel(),
                category.getDescription(),
                category.getThumbPic()
        );
    }
}
