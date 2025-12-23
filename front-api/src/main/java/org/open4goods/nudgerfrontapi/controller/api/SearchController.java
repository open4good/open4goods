package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.search.GlobalSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.GlobalSearchResultDto;
import org.open4goods.nudgerfrontapi.dto.search.GlobalSearchVerticalGroupDto;
import org.open4goods.nudgerfrontapi.dto.search.SearchSuggestCategoryDto;
import org.open4goods.nudgerfrontapi.dto.search.SearchSuggestProductDto;
import org.open4goods.nudgerfrontapi.dto.search.SearchSuggestResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import jakarta.validation.constraints.Min;

/**
 * REST controller exposing the global search endpoint. The controller delegates the
 * two-pass search logic to {@link SearchService} and ensures the response is fully documented
 * in the OpenAPI contract.
 */
@RestController
@RequestMapping("/search")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Search", description = "Global product search with boosted relevance")
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Execute a global search using the two-pass relevance strategy.
     *
     * @param query          free-text query provided by the caller
     * @param domainLanguage requested localisation hint
     * @return grouped search results respecting the configured boosting rules
     */
    @GetMapping("/global")
    @Operation(
            summary = "Execute a global search",
            description = "Runs a two-pass search. Results are first grouped by vertical when matches exist, "
                    + "otherwise a fallback search on non verticalised products is executed.",
            parameters = {
                    @Parameter(name = "query", in = ParameterIn.QUERY, required = true,
                            description = "Free-text query used to search across products",
                            schema = @Schema(type = "string", example = "Fairphone 4")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language hint used to resolve locale specific data.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search executed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GlobalSearchResponseDto.class)),
                            headers = {
                                    @Header(name = "X-Locale", description = "Resolved locale for the response",
                                            schema = @Schema(type = "string", example = "fr"))
                            })
            }
    )
    public ResponseEntity<GlobalSearchResponseDto> globalSearch(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering globalSearch(query='{}', domainLanguage={})", query, domainLanguage);
        SearchService.GlobalSearchResult result = searchService.globalSearch(query, domainLanguage);

        List<GlobalSearchVerticalGroupDto> groups = result.verticalGroups().stream()
                .map(group -> new GlobalSearchVerticalGroupDto(group.verticalId(),
                        group.results().stream().map(this::toDto).toList()))
                .toList();
        List<GlobalSearchResultDto> fallback = result.fallbackResults().stream()
                .map(this::toDto)
                .toList();

        GlobalSearchResponseDto body = new GlobalSearchResponseDto(groups, fallback, result.fallbackTriggered());
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    /**
     * Provide typeahead suggestions mixing vertical matches and product hits.
     *
     * @param query          free text value entered by the end user
     * @param domainLanguage localisation hint used for category lookup
     * @return category and product matches tailored for suggest usage
     */
    @GetMapping("/suggest")
    @Operation(
            summary = "Retrieve search suggestions",
            description = "Returns category matches resolved from an in-memory index and product hits fetched from Elasticsearch.",
            parameters = {
                    @Parameter(name = "query", in = ParameterIn.QUERY, required = true,
                            description = "Free-text fragment typed by the user.",
                            schema = @Schema(type = "string", example = "télév")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language hint to resolve localised category metadata.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Suggestions generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = SearchSuggestResponseDto.class)),
                            headers = {
                                    @Header(name = "X-Locale", description = "Resolved locale for the response",
                                            schema = @Schema(type = "string", example = "fr"))
                            })
            }
    )
    public ResponseEntity<SearchSuggestResponseDto> suggest(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering suggest(query='{}', domainLanguage={})", query, domainLanguage);
        SearchService.SuggestResult result = searchService.suggest(query, domainLanguage);

        List<SearchSuggestCategoryDto> categoryMatches = result.categoryMatches().stream()
                .map(this::toCategoryDto)
                .toList();
        List<SearchSuggestProductDto> productMatches = result.productMatches().stream()
                .map(this::toProductDto)
                .toList();

        SearchSuggestResponseDto body = new SearchSuggestResponseDto(categoryMatches, productMatches);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    @GetMapping("/semantic")
    @Operation(
            summary = "Execute a semantic search within a vertical",
            description = "Runs a vector-based search on product embeddings scoped to a given vertical and filtered using standard guardrails.",
            parameters = {
                    @Parameter(name = "verticalId", in = ParameterIn.QUERY, required = true,
                            description = "Vertical identifier used to constrain the semantic search scope.",
                            schema = @Schema(type = "string", example = "smartphones")),
                    @Parameter(name = "query", in = ParameterIn.QUERY, required = true,
                            description = "Free-text query used to compute the embedding vector.",
                            schema = @Schema(type = "string", example = "noise cancelling headphones")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language hint used for localisation.",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @Parameter(name = "page[number]", in = ParameterIn.QUERY, required = false,
                            description = "Zero-based page index for pagination.",
                            schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
                    @Parameter(name = "page[size]", in = ParameterIn.QUERY, required = false,
                            description = "Number of results per page.",
                            schema = @Schema(type = "integer", minimum = "1", defaultValue = "10"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Semantic search executed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GlobalSearchResultDto.class, type = "array")),
                            headers = {
                                    @Header(name = "X-Locale", description = "Resolved locale for the response",
                                            schema = @Schema(type = "string", example = "fr"))
                            })
            }
    )
    public ResponseEntity<List<GlobalSearchResultDto>> semanticSearch(
            @RequestParam(name = "verticalId") String verticalId,
            @RequestParam(name = "query") String query,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "page[number]", defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(name = "page[size]", defaultValue = "10") @Min(1) int pageSize) {
        LOGGER.info("Entering semanticSearch(verticalId='{}', query='{}', pageNumber={}, pageSize={})", verticalId, query, pageNumber, pageSize);
        List<GlobalSearchResultDto> body = searchService.semanticSearch(verticalId, query, domainLanguage, pageNumber, pageSize)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIVE_MINUTES_PUBLIC_CACHE)
                .header("X-Locale", domainLanguage.languageTag())
                .body(body);
    }

    private GlobalSearchResultDto toDto(GlobalSearchHit hit) {
        return new GlobalSearchResultDto(hit.product(), hit.score());
    }

    private SearchSuggestCategoryDto toCategoryDto(SearchService.CategorySuggestion suggestion) {
        return new SearchSuggestCategoryDto(suggestion.verticalId(), suggestion.imageSmall(),
                suggestion.verticalHomeTitle(), suggestion.verticalHomeUrl());
    }

    private SearchSuggestProductDto toProductDto(SearchService.ProductSuggestHit hit) {
        return new SearchSuggestProductDto(hit.model(), hit.brand(), hit.gtin(), hit.coverImagePath(), hit.verticalId(),
                hit.ecoscoreValue(), hit.bestPrice(), hit.bestPriceCurrency(), hit.score());
    }
}
