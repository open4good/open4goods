package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsContentDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsOverviewDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsSearchResultDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsTreeNodeDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.exposed.ExposedDocsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller proxying exposed documentation resources for the frontend.
 */
@RestController
@RequestMapping("/exposed")
@Tag(name = "Exposed Docs", description = "Browse embedded documentation and prompt resources.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "') "
        + "or @exposedDocsSecurity.isPublicAccess()")
public class ExposedDocsController
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedDocsController.class);

    private final ExposedDocsService exposedDocsService;

    public ExposedDocsController(ExposedDocsService exposedDocsService)
    {
        this.exposedDocsService = exposedDocsService;
    }

    /**
     * Lists exposed categories.
     *
     * @param domainLanguage localisation hint
     * @return overview of exposed categories
     * @throws ResourceNotFoundException when the backing service is unavailable
     */
    @Operation(
            summary = "List exposed documentation categories",
            description = "Returns the categories of embedded resources exposed by the documentation service.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Domain language hint",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories returned",
                            headers = @Header(name = "X-Locale", description = "Locale applied to the response"),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExposedDocsOverviewDto.class))),
                    @ApiResponse(responseCode = "404", description = "Service unavailable")
            }
    )
    @GetMapping
    public ResponseEntity<ExposedDocsOverviewDto> getOverview(@RequestParam DomainLanguage domainLanguage)
            throws ResourceNotFoundException
    {
        LOGGER.info("Listing exposed documentation categories");
        return ResponseEntity.ok(exposedDocsService.getOverview(domainLanguage));
    }

    /**
     * Returns the tree view for a category.
     *
     * @param categoryId category identifier
     * @param domainLanguage localisation hint
     * @return category tree
     * @throws ResourceNotFoundException when the category is missing
     */
    @Operation(
            summary = "Get exposed resource tree",
            description = "Returns the directory tree for a specific exposed resource category.",
            parameters = {
                    @Parameter(name = "categoryId", in = ParameterIn.PATH, required = true,
                            description = "Category identifier",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Domain language hint",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tree returned",
                            headers = @Header(name = "X-Locale", description = "Locale applied to the response"),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExposedDocsTreeNodeDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found")
            }
    )
    @GetMapping("/{categoryId}")
    public ResponseEntity<ExposedDocsTreeNodeDto> getTree(@PathVariable String categoryId,
                                                          @RequestParam DomainLanguage domainLanguage)
            throws ResourceNotFoundException
    {
        LOGGER.info("Listing exposed docs tree for category {}", categoryId);
        return ResponseEntity.ok(exposedDocsService.getTree(categoryId, domainLanguage));
    }

    /**
     * Retrieves the content for a resource path.
     *
     * @param categoryId category identifier
     * @param path resource path relative to the category root
     * @param domainLanguage localisation hint
     * @return resource content
     * @throws ResourceNotFoundException when the resource is missing
     */
    @Operation(
            summary = "Get exposed resource content",
            description = "Returns the raw content of a documentation resource.",
            parameters = {
                    @Parameter(name = "categoryId", in = ParameterIn.PATH, required = true,
                            description = "Category identifier",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "path", in = ParameterIn.QUERY, required = true,
                            description = "Resource path relative to the category root",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Domain language hint",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content returned",
                            headers = @Header(name = "X-Locale", description = "Locale applied to the response"),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExposedDocsContentDto.class))),
                    @ApiResponse(responseCode = "404", description = "Resource not found")
            }
    )
    @GetMapping("/{categoryId}/content")
    public ResponseEntity<ExposedDocsContentDto> getContent(@PathVariable String categoryId,
                                                            @RequestParam("path") String path,
                                                            @RequestParam DomainLanguage domainLanguage)
            throws ResourceNotFoundException
    {
        LOGGER.info("Fetching exposed docs content for category {} and path {}", categoryId, path);
        return ResponseEntity.ok(exposedDocsService.getContent(categoryId, path, domainLanguage));
    }

    /**
     * Searches embedded resources with optional content filtering.
     *
     * @param query search query
     * @param categories categories to search
     * @param pathPrefix optional path prefix filter
     * @param searchContent whether to search file content
     * @param includeContent whether to include file content in results
     * @param domainLanguage localisation hint
     * @return search results
     * @throws ResourceNotFoundException when the search endpoint is unavailable
     */
    @Operation(
            summary = "Search exposed resources",
            description = "Searches exposed documentation and prompt resources by path or content.",
            parameters = {
                    @Parameter(name = "query", in = ParameterIn.QUERY, description = "Search query",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "categories", in = ParameterIn.QUERY, description = "Categories to search",
                            array = @ArraySchema(schema = @Schema(type = "string"))),
                    @Parameter(name = "pathPrefix", in = ParameterIn.QUERY, description = "Path prefix filter",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "searchContent", in = ParameterIn.QUERY, description = "Search inside file content",
                            schema = @Schema(type = "boolean")),
                    @Parameter(name = "includeContent", in = ParameterIn.QUERY, description = "Include file content in results",
                            schema = @Schema(type = "boolean")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Domain language hint",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned",
                            headers = @Header(name = "X-Locale", description = "Locale applied to the response"),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExposedDocsSearchResultDto.class, type = "array"))),
                    @ApiResponse(responseCode = "404", description = "Search endpoint unavailable")
            }
    )
    @GetMapping("/search")
    public ResponseEntity<List<ExposedDocsSearchResultDto>> search(@RequestParam(value = "query", required = false) String query,
                                                                    @RequestParam(value = "categories", required = false) List<String> categories,
                                                                    @RequestParam(value = "pathPrefix", required = false) String pathPrefix,
                                                                    @RequestParam(value = "searchContent", defaultValue = "false") boolean searchContent,
                                                                    @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent,
                                                                    @RequestParam DomainLanguage domainLanguage)
            throws ResourceNotFoundException
    {
        LOGGER.info("Searching exposed docs with query '{}'", query);
        return ResponseEntity.ok(exposedDocsService.search(
                query,
                categories,
                pathPrefix,
                searchContent,
                includeContent,
                domainLanguage));
    }
}
