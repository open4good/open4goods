package org.open4goods.nudgerfrontapi.controller.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.blog.BlogPostDto;
import org.open4goods.nudgerfrontapi.dto.category.CategoryNavigationDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.Filter;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterOperator;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.CategoryMappingService;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final String SORT_FIELD_IMPACT_SCORE = "scores.ECOSCORE.value";
    private static final String CONDITION_NEW = "NEW";
    private static final String CONDITION_OCCASION = "OCCASION";

    private final VerticalsConfigService verticalsConfigService;
    private final CategoryMappingService categoryMappingService;
    private final BlogService blogService;
    private final GoogleTaxonomyService googleTaxonomyService;
    private final SearchService searchService;
    private final ProductMappingService productMappingService;

    public CategoriesController(VerticalsConfigService verticalsConfigService,
                                CategoryMappingService categoryMappingService,
                                BlogService blogService,
                                GoogleTaxonomyService googleTaxonomyService,
                                SearchService searchService,
                                ProductMappingService productMappingService) {
        this.verticalsConfigService = verticalsConfigService;
        this.categoryMappingService = categoryMappingService;
        this.blogService = blogService;
        this.googleTaxonomyService = googleTaxonomyService;
        this.searchService = searchService;
        this.productMappingService = productMappingService;
    }

    @GetMapping
    @Operation(
            summary = "List categories",
            description = "Return vertical configurations. The enabled flag is exposed but not filtered server-side.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @Parameter(name = "onlyEnabled", in = ParameterIn.QUERY, required = false,
                            description = "Deprecated: retained for backward compatibility. All verticals are returned regardless of this flag.",
                            schema = @Schema(type = "boolean", defaultValue = "false"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories returned",

                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = VerticalConfigDto.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<List<VerticalConfigDto>> categories(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "onlyEnabled", defaultValue = "false") boolean onlyEnabled) {
        List<VerticalConfigDto> body = verticalsConfigService.getConfigsWithoutDefault().stream()
                .map(config -> categoryMappingService.toVerticalConfigDto(config, domainLanguage))
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
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

                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = VerticalConfigFullDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<VerticalConfigFullDto> category(@PathVariable("categoryId") String categoryId,
                                                          @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        VerticalConfig config = verticalsConfigService.getConfigById(categoryId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        List<BlogPostDto> relatedPosts = blogService.getPosts(categoryId).stream()
                .limit(3)
                .map(this::mapBlogPost)
                .toList();

        VerticalConfigFullDto body = categoryMappingService.toVerticalConfigFullDto(config, domainLanguage, relatedPosts);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/navigation")
    @Operation(
            summary = "Get category navigation",
            description = "Return the Google taxonomy navigation data required to display deep category navigation.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields.",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @Parameter(name = "googleCategoryId", in = ParameterIn.QUERY,
                            description = "Google taxonomy identifier of the category to retrieve.",
                            schema = @Schema(type = "integer", example = "500001")),
                    @Parameter(name = "path", in = ParameterIn.QUERY,
                            description = "Slug path identifying the category when the googleCategoryId is not provided.",
                            schema = @Schema(type = "string", example = "electronique/televiseurs"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Navigation data returned",

                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CategoryNavigationDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<CategoryNavigationDto> navigation(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "googleCategoryId", required = false) Integer googleCategoryId,
            @RequestParam(name = "path", required = false) String path) {

        ProductCategory category = resolveCategory(domainLanguage, googleCategoryId, path);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<ProductDto> topNewProducts = resolveTopProducts(category, domainLanguage, CONDITION_NEW);
        List<ProductDto> topOccasionProducts = resolveTopProducts(category, domainLanguage, CONDITION_OCCASION);

        CategoryNavigationDto body = categoryMappingService.toCategoryNavigationDto(category, domainLanguage, true,
                topNewProducts,
                topOccasionProducts);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.FIFTEEN_MINUTES_PUBLIC_CACHE)
                .body(body);
    }

    private List<ProductDto> resolveTopProducts(ProductCategory category,
                                                DomainLanguage domainLanguage,
                                                String condition) {
        List<String> taxonomyIds = collectGoogleCategoryIds(category);
        if (taxonomyIds.isEmpty()) {
            return List.of();
        }

        Filter conditionFilter = new Filter(FilterField.condition.fieldPath(), FilterOperator.term,
                List.of(condition), null, null);
        Filter taxonomyFilter = new Filter(FilterField.googleTaxonomyId.fieldPath(), FilterOperator.term,
                taxonomyIds, null, null);
        FilterRequestDto filters = new FilterRequestDto(List.of(conditionFilter, taxonomyFilter), List.of());

        Pageable pageable = PageRequest.of(0, TOP_PRODUCTS_LIMIT,
                Sort.by(Sort.Order.desc(SORT_FIELD_IMPACT_SCORE)));

        SearchService.SearchResult result = searchService.search(pageable, null, null, null, filters, false);
        Locale locale = resolveLocale(domainLanguage);

        Map<Long, ProductDto> uniqueProducts = new LinkedHashMap<>();
        Set<String> includes = Set.of(ProductDtoComponent.base.name());
        for (SearchHit<Product> hit : result.hits().getSearchHits()) {
            ProductDto dto = productMappingService.mapProduct(hit.getContent(), locale, includes, domainLanguage, false);
            if (dto != null && !uniqueProducts.containsKey(dto.gtin())) {
                uniqueProducts.put(dto.gtin(), dto);
            }
            if (uniqueProducts.size() >= TOP_PRODUCTS_LIMIT) {
                break;
            }
        }
        return List.copyOf(uniqueProducts.values());
    }

    private List<String> collectGoogleCategoryIds(ProductCategory category) {
        if (category == null) {
            return List.of();
        }
        LinkedHashSet<Integer> identifiers = new LinkedHashSet<>();
        if (category.getGoogleCategoryId() != null) {
            identifiers.add(category.getGoogleCategoryId());
        }
        category.verticals().stream()
                .map(ProductCategory::getGoogleCategoryId)
                .filter(Objects::nonNull)
                .forEach(identifiers::add);
        return identifiers.stream()
                .map(String::valueOf)
                .toList();
    }

    private Locale resolveLocale(DomainLanguage domainLanguage) {
        if (domainLanguage != null && StringUtils.hasText(domainLanguage.languageTag())) {
            return Locale.forLanguageTag(domainLanguage.languageTag());
        }
        return Locale.getDefault();
    }

    private BlogPostDto mapBlogPost(BlogPost post) {
        return new BlogPostDto(
                post.getUrl(),
                post.getTitle(),
                post.getAuthor(),
                post.getSummary(),
                null,
                post.getCategory(),
                post.getImage(),
                post.getEditLink(),
                post.getCreated() == null ? null : post.getCreated().getTime(),
                post.getModified() == null ? null : post.getModified().getTime()
        );
    }

    private ProductCategory resolveCategory(DomainLanguage domainLanguage,
                                            Integer googleCategoryId,
                                            String path) {
        if (googleCategoryId != null) {
            return googleTaxonomyService.byId(googleCategoryId);
        }

        String normalisedPath = normalisePath(path);
        if (StringUtils.hasText(normalisedPath)) {
            Map<String, ProductCategory> paths = googleTaxonomyService.getCategories()
                    .paths(languageKey(domainLanguage));
            return paths.get(normalisedPath);
        }

        return googleTaxonomyService.getCategories().asRootNode();
    }

    private String normalisePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalised = path.trim();
        while (normalised.startsWith("/")) {
            normalised = normalised.substring(1);
        }
        while (normalised.endsWith("/")) {
            normalised = normalised.substring(0, normalised.length() - 1);
        }
        return normalised;
    }

    private String languageKey(DomainLanguage domainLanguage) {
        return domainLanguage == null ? "default" : domainLanguage.name();
    }
}
