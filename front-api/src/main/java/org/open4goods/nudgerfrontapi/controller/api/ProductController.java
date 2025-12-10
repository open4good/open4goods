package org.open4goods.nudgerfrontapi.controller.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.open4goods.model.Localisable;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.AggregationConfiguration;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.product.FieldMetadataDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoFilterFields;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoSortableFields;
import org.open4goods.nudgerfrontapi.dto.product.ProductFieldOptionsResponse;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterValueType;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.SortRequestDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.nudgerfrontapi.service.exception.ReviewGenerationClientException;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller exposing read‑only information about a product as well as the ability to
 * trigger an AI‑generated review.  All endpoints are grouped under the <i>Product</i> tag in the
 * generated OpenAPI contract and share the common base path <code>/product</code>.
 * The locale used by the service is resolved via {@link
 * org.open4goods.nudgerfrontapi.config.UserPreferenceLocaleResolver}, which
 * checks a cookie or JWT claim before falling back to the request
 * {@code Accept-Language} header.
 */
@RestController
@RequestMapping("/products")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Product", description = "Read product data, offers, impact score and reviews; trigger AI review generation.")
public class ProductController {

    private final ProductMappingService service;
    private final VerticalsConfigService verticalsConfigService;

    private static final String VALUE_TYPE_NUMERIC = "numeric";
    private static final String VALUE_TYPE_TEXT = "text";
    private static final String NUMERIC_VALUE_SUFFIX = ".numericValue";
    private static final String KEYWORD_VALUE_SUFFIX = ".value";
    private static final String INDEXED_ATTRIBUTE_PREFIX = "attributes.indexed.";
    private static final String ECOSCORE_RELATIVE_FIELD = "scores.ECOSCORE.value";
    private static final String ADMIN_EXCLUDED_CAUSES_FIELD = FilterField.excludedCauses.fieldPath();

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    public ProductController(ProductMappingService service,
                             VerticalsConfigService verticalsConfigService) {
        this.service = service;
        this.verticalsConfigService = verticalsConfigService;
    }

    /**
     * List allowed component names that can be requested via the {@code include} parameter.
     */
    @GetMapping("/fields/components")
    @Operation(
            summary = "Get available components",
            description = "Return the list of components that can be included in product responses.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise component labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Components returned",

                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(type = "string"))))
            }
    )
    public ResponseEntity<List<String>> components(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering components(domainLanguage={})", domainLanguage);
        List<String> body = Arrays.stream(ProductDtoComponent.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * List product fields that can be used for sorting.
     */
    @GetMapping("/fields/sortable")
    @Operation(
            summary = "Get sortable fields",
            description = "Return the list of fields accepted by the sort parameter.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise field labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fields returned",

                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(type = "string"))))
            }
    )
    public ResponseEntity<List<String>> sortableFields(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering sortableFields(domainLanguage={})", domainLanguage);
        List<String> body = Arrays.stream(ProductDtoSortableFields.values())
                .map(ProductDtoSortableFields::getText)
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * List product field metadata that can be used for sorting for a specific vertical.
     */
    @GetMapping("/fields/sortable/{verticalId}")
    @Operation(
            summary = "Get sortable fields for a vertical",
            description = "Return the field metadata accepted by the sort parameter, grouped by scope and enriched with vertical specific filters.",
            parameters = {
                    @Parameter(name = "verticalId", in = ParameterIn.PATH, required = true,
                            description = "Identifier of the vertical whose sortable fields are requested.",
                            schema = @Schema(type = "string", example = "tv")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise field labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fields returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductFieldOptionsResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Vertical not found")
            }
    )
    public ResponseEntity<ProductFieldOptionsResponse> sortableFieldsForVertical(
            @PathVariable("verticalId") String verticalId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering sortableFieldsForVertical(verticalId={}, domainLanguage={})", verticalId, domainLanguage);
        List<FieldMetadataDto> global = Arrays.stream(ProductDtoSortableFields.values())
                .map(field -> new FieldMetadataDto(field.getText(), null, null, determineSortableValueType(field), null))
                .toList();
        return buildVerticalFieldsResponse(verticalId, domainLanguage, global);
    }

    /**
     * List product fields that can be used for filtering.
     */
    @GetMapping("/fields/filters")
    @Operation(
            summary = "Get filterable fields",
            description = "Return the list of fields accepted by the filters parameter.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise field labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fields returned",

                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(type = "string"))))
            }
    )
    public ResponseEntity<List<String>> filterableFields(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering filterableFields(domainLanguage={})", domainLanguage);
        List<String> body = Arrays.stream(ProductDtoFilterFields.values())
                .map(ProductDtoFilterFields::getText)
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * List product field metadata that can be used for filtering for a specific vertical.
     */
    @GetMapping("/fields/filters/{verticalId}")
    @Operation(
            summary = "Get filterable fields for a vertical",
            description = "Return the field metadata accepted by the filters parameter, grouped by scope and enriched with vertical specific filters.",
            parameters = {
                    @Parameter(name = "verticalId", in = ParameterIn.PATH, required = true,
                            description = "Identifier of the vertical whose filterable fields are requested.",
                            schema = @Schema(type = "string", example = "tv")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise field labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fields returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductFieldOptionsResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Vertical not found")
            }
    )
    public ResponseEntity<ProductFieldOptionsResponse> filterableFieldsForVertical(
            @PathVariable("verticalId") String verticalId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering filterableFieldsForVertical(verticalId={}, domainLanguage={})", verticalId, domainLanguage);
        List<FieldMetadataDto> global = Arrays.stream(ProductDtoFilterFields.values())
                .map(field -> new FieldMetadataDto(field.getText(), null, null, determineFilterValueType(field), null))
                .toList();
        return buildVerticalFieldsResponse(verticalId, domainLanguage, global);
    }


    /**
     * List products.
     *
     * <p>Error codes:</p>
     * <ul>
     *   <li><b>UNAUTHORIZED</b> – 401</li>
     *   <li><b>FORBIDDEN</b> – 403</li>
     *   <li><b>INTERNAL_ERROR</b> – 500</li>
     * </ul>
     */
    @PostMapping
    @Operation(
            summary = "List products",
            description = "Return paginated products.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "include", in = ParameterIn.QUERY, description = "Components to include (can be coma separated)",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ProductDtoComponent.class)
                            )),
                    @Parameter(name = "pageNumber", in = ParameterIn.QUERY,
                            description = "Zero-based page index",
                            schema = @Schema(type = "integer", minimum = "0")),
                    @Parameter(name = "pageSize", in = ParameterIn.QUERY,
                            description = "Page size",
                            schema = @Schema(type = "integer", minimum = "0")),
                    @Parameter(name = "verticalId", in = ParameterIn.QUERY,
                            description = "Optional vertical identifier used to scope the search.",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "query", in = ParameterIn.QUERY,
                            description = "Optional free text search applied on offer names.",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional JSON payload carrying sort, aggregation and filter definitions.",
                    required = false,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductSearchRequestDto.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Products returned",
                            headers = {
                                    @Header(name = "Link", description = "Pagination links as defined by RFC 8288"),
                                    @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                            schema = @Schema(type = "string", example = "fr-FR"))
                            },
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSearchResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ProductSearchResponseDto> products(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable page,
            @RequestParam(required = false) Set<String> include,
            @RequestParam(required = false) String verticalId,
            @RequestParam(required = false) String query,
            @RequestParam() DomainLanguage domainLanguage,
            Locale locale,
            @RequestBody(required = false) ProductSearchRequestDto searchPayload) {

        LOGGER.info(
                "Entering products(page={}, include={}, verticalId={}, query={}, domainLanguage={}, locale={}, hasSearchPayload={})",
                page, include, verticalId, query, domainLanguage, locale, searchPayload != null);

        String normalizedVerticalId = StringUtils.hasText(verticalId) ? verticalId.trim() : null;

        List<FieldMetadataDto> filterableGlobal = Arrays.stream(ProductDtoFilterFields.values())
                .map(field -> new FieldMetadataDto(field.getText(), null, null, determineFilterValueType(field), null))
                .toList();

        SearchCapabilities capabilities = buildSearchCapabilities(normalizedVerticalId, domainLanguage, filterableGlobal);
        Set<String> allowedSortMappings = capabilities.allowedSorts();

        Pageable effectivePageable = page;
        SortRequestDto sortDto = searchPayload == null ? null : searchPayload.sort();
        Validation<Pageable> sortValidation = sanitizeSort(page, sortDto, allowedSortMappings);
        if (sortValidation.hasError()) {
            LOGGER.warn("Sort validation failed for request: {}", sortDto);
            return sortValidation.error();
        }
        effectivePageable = sortValidation.value();

        if (include != null) {
            for (String component : include) {
                try {
                    ProductDtoComponent.valueOf(component);
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Invalid include parameter encountered: {}", component, ex);
                    return badRequest("Invalid include parameter", "Unknown component: " + component);
                }
            }
        }

        AggregationRequestDto aggDto = searchPayload == null ? null : searchPayload.aggs();
        Validation<AggregationRequestDto> aggregationValidation = sanitizeAggregations(aggDto, normalizedVerticalId,
                capabilities.allowedAggregations());
        if (aggregationValidation.hasError()) {
            LOGGER.warn("Aggregation validation failed for request: {}", aggDto);
            return aggregationValidation.error();
        }
        aggDto = aggregationValidation.value();

        FilterRequestDto filterDto = searchPayload == null ? null : searchPayload.filters();
        Validation<FilterRequestDto> filterValidation = sanitizeFilters(filterDto, capabilities.allowedFilters());
        if (filterValidation.hasError()) {
            LOGGER.warn("Filter validation failed for request: {}", filterDto);
            return filterValidation.error();
        }
        filterDto = filterValidation.value();

        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Set<String> requestedComponents = include == null ? Set.of() : include;

        ProductSearchResponseDto body = service.searchProducts(effectivePageable, locale, requestedComponents, aggDto,
                domainLanguage, normalizedVerticalId, normalizedQuery, filterDto);

        return ResponseEntity.ok().cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE).body(body);
    }

    private ResponseEntity<ProductSearchResponseDto> badRequest(String title, String detail) {
        LOGGER.warn("Returning bad request ProblemDetail with title='{}', detail='{}'", title, detail);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle(title);
        pd.setDetail(detail);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ResponseEntity<ProductSearchResponseDto> response = (ResponseEntity) ResponseEntity.badRequest().body(pd);
        return response;
    }

    /**
     * Combine the base field list with the filters configured on the specified vertical.
     *
     * @param verticalId identifier of the vertical to resolve
     * @param fields     base list of field names already allowed globally
     * @return {@link ResponseEntity} wrapping the augmented list or {@code 404} if the vertical is unknown
     */
    private ResponseEntity<ProductFieldOptionsResponse> buildVerticalFieldsResponse(String verticalId,
            DomainLanguage domainLanguage, List<FieldMetadataDto> globalFields) {
        LOGGER.info(
                "Entering buildVerticalFieldsResponse(verticalId={}, domainLanguage={}, globalFieldCount={})",
                verticalId, domainLanguage, globalFields != null ? globalFields.size() : 0);
        ProductFieldOptionsResponse body = resolveVerticalFields(verticalId, domainLanguage, globalFields);
        if (body == null) {
            LOGGER.warn("No vertical configuration found for id='{}'", verticalId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }

    private ProductFieldOptionsResponse resolveVerticalFields(String verticalId, DomainLanguage domainLanguage,
            List<FieldMetadataDto> globalFields) {
        LOGGER.info(
                "Entering resolveVerticalFields(verticalId={}, domainLanguage={}, globalFieldCount={})",
                verticalId, domainLanguage, globalFields != null ? globalFields.size() : 0);
        if (!StringUtils.hasText(verticalId)) {
            return resolveVerticalFields((VerticalConfig) null, domainLanguage, globalFields);
        }

        VerticalConfig vConfig = verticalsConfigService.getConfigById(verticalId);
        if (vConfig == null) {
            LOGGER.warn("Vertical configuration not found for id='{}'", verticalId);
            return null;
        }

        return resolveVerticalFields(vConfig, domainLanguage, globalFields);
    }

    private ProductFieldOptionsResponse resolveVerticalFields(VerticalConfig config, DomainLanguage domainLanguage,
            List<FieldMetadataDto> globalFields) {
        LOGGER.info(
                "Entering resolveVerticalFields(config={}, domainLanguage={}, globalFieldCount={})",
                config, domainLanguage, globalFields != null ? globalFields.size() : 0);
        List<FieldMetadataDto> immutableGlobal = List.copyOf(globalFields);
        if (config == null) {
            return new ProductFieldOptionsResponse(immutableGlobal, List.of(), List.of());
        }

        List<FieldMetadataDto> globalWithAggregation = augmentFieldsWithAggregationMetadata(immutableGlobal, config);
        List<FieldMetadataDto> impactFields = new ArrayList<>();
        FieldMetadataDto ecoscore = buildEcoscoreField(config);
        impactFields.add(ecoscore);
        mapImpactScores(config, domainLanguage).stream()
                .filter(field -> !Objects.equals(field.mapping(), ecoscore.mapping()))
                .forEach(impactFields::add);

        List<FieldMetadataDto> technicalFields = new ArrayList<>();
        technicalFields.addAll(mapVerticalAttributeFilters(config.getEcoFilters(), config, domainLanguage));
        technicalFields.addAll(mapVerticalAttributeFilters(config.getGlobalTechnicalFilters(), config, domainLanguage));
        technicalFields.addAll(mapVerticalAttributeFilters(config.getTechnicalFilters(), config, domainLanguage));

        return new ProductFieldOptionsResponse(globalWithAggregation, List.copyOf(impactFields), List.copyOf(technicalFields));
    }

    /**
     * Resolve the search capabilities for the requested vertical.
     *
     * @param normalizedVerticalId optional vertical identifier supplied by the client
     * @param domainLanguage       language used to localise filter metadata
     * @param globalFields         list of fields available regardless of the vertical
     * @return aggregation, sort and filter capabilities derived from the vertical configuration
     */
    private SearchCapabilities buildSearchCapabilities(String normalizedVerticalId, DomainLanguage domainLanguage,
            List<FieldMetadataDto> globalFields) {
        LOGGER.info(
                "Entering buildSearchCapabilities(normalizedVerticalId={}, domainLanguage={}, globalFieldCount={})",
                normalizedVerticalId, domainLanguage, globalFields != null ? globalFields.size() : 0);
        boolean hasVertical = StringUtils.hasText(normalizedVerticalId);
        VerticalConfig config = hasVertical ? verticalsConfigService.getConfigById(normalizedVerticalId) : null;
        ProductFieldOptionsResponse fieldOptions = resolveVerticalFields(config, domainLanguage, globalFields);
        Set<String> allowedFilters = collectAllowedFieldMappings(fieldOptions);
        augmentWithAggregatedScores(allowedFilters, config);
        Set<String> allowedSorts = collectGlobalSortMappings();
        if (hasVertical) {
            allowedSorts.addAll(allowedFilters);
        }
        Set<String> allowedAggregations = collectAllowedAggregationMappings(allowedFilters, config);
        return new SearchCapabilities(allowedFilters, allowedSorts, allowedAggregations);
    }

    private Set<String> collectAllowedFieldMappings(ProductFieldOptionsResponse fieldOptions) {
        LOGGER.info("Entering collectAllowedFieldMappings(fieldOptions={})", fieldOptions);
        Set<String> allowed = new HashSet<>();
        if (fieldOptions == null) {
            return allowed;
        }
        addFieldMappings(allowed, fieldOptions.global());
        addFieldMappings(allowed, fieldOptions.impact());
        addFieldMappings(allowed, fieldOptions.technical());
        return allowed;
    }

    /**
     * Add aggregated score mappings derived from the vertical configuration.
     *
     * @param target collection of allowed filter mappings to update
     * @param config vertical configuration supplying aggregated score identifiers
     */
    private void augmentWithAggregatedScores(Set<String> target, VerticalConfig config) {
        LOGGER.info("Entering augmentWithAggregatedScores(targetSize={}, config={})",
                target != null ? target.size() : 0, config);
        if (target == null || config == null || config.getAggregatedScores() == null) {
            return;
        }
        for (String score : config.getAggregatedScores()) {
            if (!StringUtils.hasText(score)) {
                continue;
            }
            String mapping = "scores." + score.trim()+".value";
            target.add(mapping);
        }
    }

    /**
     * Combine the explicit filter mappings with the aggregation hints defined in the vertical configuration.
     *
     * @param allowedFilterMappings mappings allowed for filtering
     * @param config                vertical configuration possibly containing aggregation hints
     * @return set of mappings that may be used to request aggregations
     */
    private Set<String> collectAllowedAggregationMappings(Set<String> allowedFilterMappings, VerticalConfig config) {
        LOGGER.info("Entering collectAllowedAggregationMappings(allowedFilterMappingsSize={}, config={})",
                allowedFilterMappings != null ? allowedFilterMappings.size() : 0, config);
        Set<String> allowed = new HashSet<>(allowedFilterMappings);
        if (config == null || config.getAggregationConfiguration() == null) {
            return allowed;
        }
        config.getAggregationConfiguration().keySet()
                .forEach(candidate -> addAggregationKeyVariants(allowed, candidate, config));
        return allowed;
    }

    /**
     * Add acceptable aggregation key variants (with or without the {@code .numericValue} suffix).
     *
     * @param target    destination set to update
     * @param candidate aggregation key declared in the configuration
     * @param config    vertical configuration used to infer attribute metadata
     */
    private void addAggregationKeyVariants(Set<String> target, String candidate, VerticalConfig config) {
        LOGGER.info("Entering addAggregationKeyVariants(candidate={}, config={})", candidate, config);
        if (!StringUtils.hasText(candidate)) {
            return;
        }
        String normalized = candidate.trim();
        target.add(normalized);
        if (normalized.endsWith(NUMERIC_VALUE_SUFFIX)) {
            String base = normalized.substring(0, normalized.length() - NUMERIC_VALUE_SUFFIX.length());
            if (!base.isEmpty()) {
                target.add(base);
            }
            return;
        }
        if (normalized.endsWith(KEYWORD_VALUE_SUFFIX)) {
            String base = normalized.substring(0, normalized.length() - KEYWORD_VALUE_SUFFIX.length());
            if (!base.isEmpty()) {
                target.add(base);
            }
            return;
        }
        if (normalized.startsWith(INDEXED_ATTRIBUTE_PREFIX)) {
            String attributeKey = normalized.substring(INDEXED_ATTRIBUTE_PREFIX.length());
            String valueType = resolveAttributeValueType(config, attributeKey);
            if (VALUE_TYPE_NUMERIC.equals(valueType)) {
                target.add(normalized + NUMERIC_VALUE_SUFFIX);
            } else if (VALUE_TYPE_TEXT.equals(valueType)) {
                target.add(normalized + KEYWORD_VALUE_SUFFIX);
            }
        }
    }

    /**
     * @return default sort mappings accepted when no vertical override is provided.
     */
    private Set<String> collectGlobalSortMappings() {
        LOGGER.info("Entering collectGlobalSortMappings()");
        Set<String> fields = new HashSet<>();
        for (ProductDtoSortableFields field : ProductDtoSortableFields.values()) {
            fields.add(field.getText());
        }
        return fields;
    }

    /**
     * Validate and normalise the requested sort specification.
     */
    private Validation<Pageable> sanitizeSort(Pageable requestedPageable, SortRequestDto sortDto,
            Set<String> allowedSortMappings) {
        LOGGER.info("Entering sanitizeSort(requestedPageable={}, sortDto={}, allowedSortMappingsSize={})",
                requestedPageable, sortDto, allowedSortMappings != null ? allowedSortMappings.size() : 0);
        if (sortDto == null || sortDto.sorts() == null) {
            for (Sort.Order order : requestedPageable.getSort()) {
                if (!allowedSortMappings.contains(order.getProperty())) {
                    LOGGER.warn("Sort order property '{}' is not permitted", order.getProperty());
                    return Validation.error(badRequest("Invalid sort parameter",
                            "Unknown sort field: " + order.getProperty()));
                }
            }
            return Validation.ok(requestedPageable);
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (SortRequestDto.SortOption option : sortDto.sorts()) {
            if (option == null || !StringUtils.hasText(option.field())) {
                LOGGER.warn("Sort option is missing a field definition: {}", option);
                return Validation.error(badRequest("Invalid sort parameter", "Sort field is mandatory"));
            }
            String mapping = option.field().trim();
            if (!allowedSortMappings.contains(mapping)) {
                LOGGER.warn("Sort field '{}' is not permitted", mapping);
                return Validation.error(badRequest("Invalid sort parameter", "Unknown sort field: " + mapping));
            }
            Sort.Direction direction = option.order() == SortRequestDto.SortOrder.desc
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, mapping));
        }

        Sort sortSpec = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        Pageable sanitized = PageRequest.of(requestedPageable.getPageNumber(), requestedPageable.getPageSize(), sortSpec);
        return Validation.ok(sanitized);
    }

    /**
     * Validate aggregations and ensure they target authorised mappings.
     */
    private Validation<AggregationRequestDto> sanitizeAggregations(AggregationRequestDto aggregationRequest,
            String normalizedVerticalId, Set<String> allowedAggregationMappings) {
        LOGGER.info(
                "Entering sanitizeAggregations(aggregationRequest={}, normalizedVerticalId={}, allowedAggregationMappingsSize={})",
                aggregationRequest, normalizedVerticalId,
                allowedAggregationMappings != null ? allowedAggregationMappings.size() : 0);
        if (aggregationRequest == null) {
            return Validation.ok(null);
        }

        List<Agg> aggregations = aggregationRequest.aggs();
        if (aggregations == null || aggregations.isEmpty()) {
            if (normalizedVerticalId == null) {
                LOGGER.warn("Aggregation request requires a verticalId when aggregations are empty");
                return Validation.error(badRequest("Invalid aggregation parameter",
                        "Aggregations require a verticalId"));
            }
            return Validation.ok(new AggregationRequestDto(List.of()));
        }

        if (normalizedVerticalId == null) {
            LOGGER.warn("Aggregation request received without a verticalId");
            return Validation.error(badRequest("Invalid aggregation parameter", "Aggregations require a verticalId"));
        }

        List<Agg> sanitized = new ArrayList<>();
        for (Agg aggregation : aggregations) {
            if (aggregation == null || !StringUtils.hasText(aggregation.field())) {
                LOGGER.warn("Aggregation entry is missing a field definition: {}", aggregation);
                return Validation.error(badRequest("Invalid aggregation parameter", "Aggregation field is mandatory"));
            }
            String mapping = aggregation.field().trim();
            // Allow aggregation on admin-only exclusion causes field regardless of vertical configuration
            if (!mapping.equals(ADMIN_EXCLUDED_CAUSES_FIELD) && !allowedAggregationMappings.contains(mapping)) {
                LOGGER.warn("Aggregation field '{}' is not permitted", mapping);
                return Validation.error(badRequest("Invalid aggregation parameter",
                        "Aggregation not permitted for field: " + mapping));
            }
            sanitized.add(new Agg(aggregation.name(), mapping, aggregation.type(), aggregation.min(), aggregation.max(),
                    aggregation.buckets(), aggregation.step()));
        }

        return Validation.ok(new AggregationRequestDto(List.copyOf(sanitized)));
    }

    /**
     * Validate filter clauses and keep only authorised mappings.
     */
    private Validation<FilterRequestDto> sanitizeFilters(FilterRequestDto filterRequest,
            Set<String> allowedFilterMappings) {
        LOGGER.info("Entering sanitizeFilters(filterRequest={}, allowedFilterMappingsSize={})", filterRequest,
                allowedFilterMappings != null ? allowedFilterMappings.size() : 0);
        if (filterRequest == null) {
            return Validation.ok(null);
        }
        if (filterRequest.filters() == null) {
            return Validation.ok(filterRequest);
        }

        List<FilterRequestDto.Filter> sanitized = new ArrayList<>();
        for (FilterRequestDto.Filter filter : filterRequest.filters()) {
            if (filter == null || !StringUtils.hasText(filter.field())) {
                LOGGER.warn("Filter entry is missing a field definition: {}", filter);
                return Validation.error(badRequest("Invalid filters parameter", "Filter field is mandatory"));
            }
            String mapping = filter.field().trim();
            // Allow filtering on admin-only exclusion causes field regardless of vertical configuration
            if (!ADMIN_EXCLUDED_CAUSES_FIELD.equals(mapping) && !allowedFilterMappings.contains(mapping)) {
                LOGGER.warn("Filter field '{}' is not permitted", mapping);
                return Validation.error(badRequest("Invalid filters parameter",
                        "Filter not permitted for field: " + mapping));
            }
            sanitized.add(new FilterRequestDto.Filter(mapping, filter.operator(), filter.terms(), filter.min(), filter.max()));
        }

        return Validation.ok(new FilterRequestDto(List.copyOf(sanitized)));
    }

    private record SearchCapabilities(Set<String> allowedFilters, Set<String> allowedSorts,
            Set<String> allowedAggregations) {
    }

    private record Validation<T>(T value, ResponseEntity<ProductSearchResponseDto> error) {

        static <T> Validation<T> ok(T value) {
            return new Validation<>(value, null);
        }

        static <T> Validation<T> error(ResponseEntity<ProductSearchResponseDto> error) {
            return new Validation<>(null, error);
        }

        boolean hasError() {
            return error != null;
        }
    }

    private void addFieldMappings(Set<String> target, List<FieldMetadataDto> fields) {
        LOGGER.info("Entering addFieldMappings(currentTargetSize={}, fieldsSize={})",
                target != null ? target.size() : 0, fields != null ? fields.size() : null);
        if (fields == null) {
            return;
        }
        for (FieldMetadataDto field : fields) {
            if (field == null) {
                continue;
            }
            if (StringUtils.hasText(field.mapping())) {
                target.add(field.mapping());
            }
        }
    }

    private List<FieldMetadataDto> augmentFieldsWithAggregationMetadata(List<FieldMetadataDto> fields, VerticalConfig config) {
        LOGGER.info("Entering augmentFieldsWithAggregationMetadata(fieldsSize={}, config={})",
                fields != null ? fields.size() : null, config);
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        List<FieldMetadataDto> results = new ArrayList<>(fields.size());
        for (FieldMetadataDto field : fields) {
            if (field == null) {
                continue;
            }
            if (!StringUtils.hasText(field.mapping())) {
                results.add(field);
                continue;
            }
            FieldMetadataDto.AggregationMetadata existing = field.aggregationConfiguration();
            if (existing != null) {
                results.add(field);
                continue;
            }
            FieldMetadataDto.AggregationMetadata resolved = resolveAggregationMetadata(config, field.mapping());
            if (resolved == null) {
                results.add(field);
            } else {
                results.add(new FieldMetadataDto(field.mapping(), field.title(), field.description(), field.valueType(), resolved));
            }
        }
        return List.copyOf(results);
    }

    /**
     * Translate a vertical filter key to the indexed attribute path used by Elasticsearch.
     *
     * @param filterName filter identifier as defined in the vertical configuration
     * @param config     vertical configuration used to resolve attribute metadata
     * @return fully qualified field path pointing to the indexed attribute value. Numeric
     *         attributes resolve to the <code>.numericValue</code> sub-field and textual ones to
     *         the <code>.keyword</code> sub-field in order to target the appropriate Elasticsearch
     *         data type.
     */
    private String toIndexedAttribute(String filterName, VerticalConfig config) {
        LOGGER.info("Entering toIndexedAttribute(filterName={}, config={})", filterName, config);
        String baseField = "attributes.indexed." + filterName;
        AttributeConfig attributeConfig = null;
        if (config.getAttributesConfig() != null) {
            attributeConfig = config.getAttributesConfig().getAttributeConfigByKey(filterName);
        }
        if (attributeConfig != null) {
            if (attributeConfig.getFilteringType() == AttributeType.NUMERIC) {
                return baseField + NUMERIC_VALUE_SUFFIX;
            }
            if (attributeConfig.getFilteringType() == AttributeType.TEXT) {
                return baseField + KEYWORD_VALUE_SUFFIX;
            }
        }
        return baseField;
    }

    /**
     * Convert the configured attribute filters into field metadata enriched with localisation.
     *
     * @param filters        filter identifiers defined in the vertical configuration
     * @param config         vertical configuration used to resolve attribute metadata
     * @param domainLanguage requested domain language driving localisation
     * @return immutable list of {@link FieldMetadataDto} describing the filters
     */
    private List<FieldMetadataDto> mapVerticalAttributeFilters(List<String> filters, VerticalConfig config,
            DomainLanguage domainLanguage) {
        LOGGER.info("Entering mapVerticalAttributeFilters(filtersSize={}, config={}, domainLanguage={})",
                filters != null ? filters.size() : null, config, domainLanguage);
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }

        List<FieldMetadataDto> results = new ArrayList<>();
        for (String filterName : filters) {
            if (!StringUtils.hasText(filterName)) {
                continue;
            }
            String normalizedName = filterName.trim();
            String mapping = toIndexedAttribute(normalizedName, config);
            String title = resolveAttributeTitle(config, normalizedName, domainLanguage);
            String valueType = resolveAttributeValueType(config, normalizedName);
            FieldMetadataDto.AggregationMetadata aggregation = resolveAggregationMetadata(config, mapping, normalizedName);
            FieldMetadataDto dto = new FieldMetadataDto(mapping, title, null, valueType, aggregation);
            results.add(dto);
        }
        return List.copyOf(results);
    }

    private FieldMetadataDto.AggregationMetadata resolveAggregationMetadata(VerticalConfig config, String mapping,
            String... fallbackKeys) {
        LOGGER.info("Entering resolveAggregationMetadata(mapping={}, fallbackKeysCount={}, config={})", mapping,
                fallbackKeys != null ? fallbackKeys.length : 0, config);
        AggregationConfiguration aggregationConfig = findAggregationConfiguration(config, mapping, fallbackKeys);
        if (aggregationConfig == null) {
            return null;
        }
        Integer buckets = aggregationConfig.getBuckets();
        Double interval = aggregationConfig.getInterval();
        if (buckets == null && interval == null) {
            return null;
        }
        return new FieldMetadataDto.AggregationMetadata(buckets, interval);
    }

    private AggregationConfiguration findAggregationConfiguration(VerticalConfig config, String mapping,
            String... fallbackKeys) {
        LOGGER.info("Entering findAggregationConfiguration(mapping={}, fallbackKeysCount={}, config={})", mapping,
                fallbackKeys != null ? fallbackKeys.length : 0, config);
        if (config == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        addAggregationCandidate(candidates, mapping);
        if (fallbackKeys != null) {
            for (String fallback : fallbackKeys) {
                addAggregationCandidate(candidates, fallback);
            }
        }
        for (String candidate : candidates) {
            AggregationConfiguration resolved = config.getAggregationConfigurationFor(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private void addAggregationCandidate(List<String> target, String value) {
        LOGGER.info("Entering addAggregationCandidate(value={})", value);
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalized = value.trim();
        if (!target.contains(normalized)) {
            target.add(normalized);
        }
        if (normalized.endsWith(NUMERIC_VALUE_SUFFIX)) {
            String shortened = normalized.substring(0, normalized.length() - NUMERIC_VALUE_SUFFIX.length());
            if (!shortened.isEmpty() && !target.contains(shortened)) {
                target.add(shortened);
            }
        }
        if (normalized.endsWith(KEYWORD_VALUE_SUFFIX)) {
            String shortened = normalized.substring(0, normalized.length() - KEYWORD_VALUE_SUFFIX.length());
            if (!shortened.isEmpty() && !target.contains(shortened)) {
                target.add(shortened);
            }
        }
    }

    private FieldMetadataDto buildEcoscoreField(VerticalConfig config) {
        LOGGER.info("Entering buildEcoscoreField(config={})", config);
        String mapping = ECOSCORE_RELATIVE_FIELD;
        FieldMetadataDto.AggregationMetadata aggregation = resolveAggregationMetadata(config, mapping, "ECOSCORE");
        return new FieldMetadataDto(mapping, "impactscore", null, VALUE_TYPE_NUMERIC, aggregation);
    }

    /**
     * Resolve the localised attribute title for the provided key.
     *
     * @param config         vertical configuration supplying attribute metadata
     * @param attributeKey   identifier of the attribute
     * @param domainLanguage requested domain language driving localisation
     * @return localised title or {@code null} when no localisation is available
     */
    private String resolveAttributeTitle(VerticalConfig config, String attributeKey, DomainLanguage domainLanguage) {
        LOGGER.info("Entering resolveAttributeTitle(attributeKey={}, domainLanguage={}, config={})", attributeKey,
                domainLanguage, config);
        if (config.getAttributesConfig() == null) {
            return null;
        }
        AttributeConfig attributeConfig = config.getAttributesConfig().getAttributeConfigByKey(attributeKey);
        if (attributeConfig == null || attributeConfig.getName() == null) {
            return null;
        }
        return localise(attributeConfig.getName(), domainLanguage);
    }

    /**
     * Build metadata entries for the impact scores configured on the vertical.
     *
     * @param config         vertical configuration supplying impact scores
     * @param domainLanguage requested domain language driving localisation
     * @return immutable list of {@link FieldMetadataDto} representing the scores
     */
    private List<FieldMetadataDto> mapImpactScores(VerticalConfig config, DomainLanguage domainLanguage) {
        LOGGER.info("Entering mapImpactScores(config={}, domainLanguage={})", config, domainLanguage);
        if (config.getAvailableImpactScoreCriterias() == null || config.getAvailableImpactScoreCriterias().isEmpty()) {
            return List.of();
        }

        List<FieldMetadataDto> results = new ArrayList<>();
        for (String key : config.getAvailableImpactScoreCriterias()) {
            if (!StringUtils.hasText(key)) {
                continue;
            }

            String normalizedKey = key.trim();
            AttributeConfig attributeConfig = config.getAttributesConfig() == null
                    ? null
                    : config.getAttributesConfig().getAttributeConfigByKey(normalizedKey);

            // Titles and descriptions now rely on attribute metadata when available.
            String title = resolveImpactScoreTitle(attributeConfig, normalizedKey, domainLanguage);
            String description = resolveImpactScoreDescription(attributeConfig, domainLanguage);

            String mapping = "scores." + normalizedKey + ".value";
            FieldMetadataDto.AggregationMetadata aggregation = resolveAggregationMetadata(config, mapping, normalizedKey);
            results.add(new FieldMetadataDto(mapping, title, description, VALUE_TYPE_NUMERIC, aggregation));
        }
        return List.copyOf(results);
    }

    private String resolveImpactScoreTitle(AttributeConfig attributeConfig, String key, DomainLanguage domainLanguage) {
        String localizedTitle = attributeConfig == null ? null : localise(attributeConfig.getScoreTitle(), domainLanguage);
        if (!StringUtils.hasText(localizedTitle) && attributeConfig != null) {
            localizedTitle = localise(attributeConfig.getName(), domainLanguage);
        }
        return StringUtils.hasText(localizedTitle) ? localizedTitle : key;
    }

    private String resolveImpactScoreDescription(AttributeConfig attributeConfig, DomainLanguage domainLanguage) {
        if (attributeConfig == null) {
            return null;
        }
        String localizedDescription = localise(attributeConfig.getScoreDescription(), domainLanguage);
        return StringUtils.hasText(localizedDescription) ? localizedDescription : null;
    }

    private String resolveAttributeValueType(VerticalConfig config, String attributeKey) {
        LOGGER.info("Entering resolveAttributeValueType(attributeKey={}, config={})", attributeKey, config);
        if (config.getAttributesConfig() == null) {
            return VALUE_TYPE_TEXT;
        }
        AttributeConfig attributeConfig = config.getAttributesConfig().getAttributeConfigByKey(attributeKey);
        if (attributeConfig == null || attributeConfig.getFilteringType() == null) {
            return VALUE_TYPE_TEXT;
        }
        return switch (attributeConfig.getFilteringType()) {
        case NUMERIC -> VALUE_TYPE_NUMERIC;
        case BOOLEAN, TEXT -> VALUE_TYPE_TEXT;
        };
    }

    private String determineSortableValueType(ProductDtoSortableFields field) {
        LOGGER.info("Entering determineSortableValueType(field={})", field);
        return switch (field) {
        case price, offersCount -> VALUE_TYPE_NUMERIC;
		case brand, model -> VALUE_TYPE_TEXT;
		default -> throw new IllegalArgumentException("Unexpected value: " + field);
        };
    }

    private String determineFilterValueType(ProductDtoFilterFields field) {
        LOGGER.info("Entering determineFilterValueType(field={})", field);
        FilterValueType delegateType = field.getDelegate().valueType();
        return delegateType == FilterValueType.numeric ? VALUE_TYPE_NUMERIC : VALUE_TYPE_TEXT;
    }

    /**
     * Localise the provided {@link Localisable} using the requested domain language.
     *
     * @param localisable    localisable value to translate
     * @param domainLanguage requested domain language
     * @return translated value or {@code null} when none is defined
     */
    private String localise(Localisable<String, String> localisable, DomainLanguage domainLanguage) {
        LOGGER.info("Entering localise(localisable={}, domainLanguage={})", localisable, domainLanguage);
        if (localisable == null) {
            return null;
        }
        String language = domainLanguage != null ? domainLanguage.languageTag() : null;
        return localisable.i18n(language);
    }

    /**
     * Trigger asynchronous AI review generation after verifying hCaptcha.
     */
    @PostMapping("/{gtin}/review")
    @Operation(
            summary = "Trigger AI review generation",
            description = "Validate the provided hCaptcha token and forward the request to the back-office API.",
            parameters = {
                    @Parameter(name = "gtin",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Product GTIN/UPC (numeric identifier)",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0", example = "8806095491998")),
                    @Parameter(name = "hcaptchaResponse",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "hCaptcha token returned by the widget.",
                            schema = @Schema(type = "string")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Generation scheduled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(type = "integer", format = "int64"))),
                    @ApiResponse(responseCode = "400", description = "Captcha verification failed"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Product not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<Long> triggerReview(@PathVariable Long gtin,
                                              @RequestParam(name = "hcaptchaResponse") String hcaptchaResponse,
                                              @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                              HttpServletRequest request)
            throws ResourceNotFoundException {
        LOGGER.info("Entering triggerReview(gtin={}, domainLanguage={}, hasHcaptchaResponse={}, remoteAddr={})", gtin,
                domainLanguage, StringUtils.hasText(hcaptchaResponse), request != null ? request.getRemoteAddr() : null);
        long scheduledUpc = service.createReview(gtin, hcaptchaResponse, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(scheduledUpc);
    }

    /**
     * Poll the AI review generation status for a product.
     */
    @GetMapping("/{gtin}/review")
    @Operation(
            summary = "Get AI review generation status",
            description = "Return the latest status snapshot for the requested product.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "gtin",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Product GTIN/UPC (numeric identifier)",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0", example = "8806095491998")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReviewGenerationStatus.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Product not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ReviewGenerationStatus> reviewStatus(@PathVariable Long gtin,
                                                               @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        LOGGER.info("Entering reviewStatus(gtin={}, domainLanguage={})", gtin, domainLanguage);
        try {
            ReviewGenerationStatus status = service.getReviewStatus(gtin);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(status);
        } catch (ReviewGenerationClientException e) {
            HttpStatus resolvedStatus = e.getStatusCode() != null
                    ? HttpStatus.valueOf(e.getStatusCode().value())
                    : HttpStatus.BAD_GATEWAY;
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(resolvedStatus, e.getMessage());
            throw new ResponseStatusException(resolvedStatus, detail.getDetail(), e);
        }
    }

    /**
     * Return high level information for a product, including optional AI generated review
     * metadata when the corresponding component is requested.
     *
     * <p>Error codes:</p>
     * <ul>
     *   <li><b>INVALID_GTIN</b> – 400</li>
     *   <li><b>UNAUTHORIZED</b> – 401</li>
     *   <li><b>FORBIDDEN</b> – 403</li>
     *   <li><b>NOT_FOUND</b> – 404</li>
     *   <li><b>INTERNAL_ERROR</b> – 500</li>
     * </ul>
     */
    @GetMapping("/{gtin}")
    @Operation(
            summary = "Get product view",
            description = "Return high‑level product information, aggregated scores and optional AI review content, "
                    + "including datasource favicons for offers and AI source references.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "gtin",
                            description = "Global Trade Item Number (8–14 digit numeric code)",
                            example = "8806095491998",
                            required = true,
                            schema = @Schema(type = "integer", format = "int64", minimum = "0")),
                    @Parameter(
                            name        = "include",
                            in          = ParameterIn.QUERY,
                            description = "Components to include in the response (repeatable parameter)",
                            array       = @ArraySchema(
                                schema = @Schema(implementation = ProductDtoComponent.class)
                            )
                        ),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product found",
                                    headers = @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                            schema = @Schema(type = "string", example = "fr-FR")),
                                    content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ProductDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid GTIN or include parameter"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Product not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ProductDto> product(@PathVariable Long gtin,
                                                   @RequestParam(required = false)
                                                   Set<String> include,
                                                   @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                   Locale locale) throws ResourceNotFoundException {
        LOGGER.info("Entering product(gtin={}, include={}, domainLanguage={}, locale={})", gtin, include, domainLanguage,
                locale);
        ProductDto body = service.getProduct(gtin, locale, include, domainLanguage);



        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

}
