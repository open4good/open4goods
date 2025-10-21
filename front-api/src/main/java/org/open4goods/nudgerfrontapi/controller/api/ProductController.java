package org.open4goods.nudgerfrontapi.controller.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.Localisable;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.AggregationConfiguration;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoFilterFields;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoSortableFields;
import org.open4goods.nudgerfrontapi.dto.product.FieldMetadataDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductFieldOptionsResponse;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto.Agg;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterValueType;
import org.open4goods.nudgerfrontapi.dto.search.SortRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

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
import org.open4goods.verticals.VerticalsConfigService;

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

        String normalizedVerticalId = StringUtils.hasText(verticalId) ? verticalId.trim() : null;

        List<FieldMetadataDto> filterableGlobal = Arrays.stream(ProductDtoFilterFields.values())
                .map(field -> new FieldMetadataDto(field.getText(), null, null, determineFilterValueType(field), null))
                .toList();
        ProductFieldOptionsResponse filterOptions = safeResolveVerticalFields(normalizedVerticalId, domainLanguage,
                filterableGlobal);

        Set<String> globalFilterMappings = Arrays.stream(ProductDtoFilterFields.values())
                .map(ProductDtoFilterFields::getText)
                .collect(Collectors.toSet());
        Set<String> allowedFilterMappings = collectAllowedFieldMappings(filterOptions);

        Set<String> allowedSortMappings;
        if (normalizedVerticalId == null) {
            allowedSortMappings = Arrays.stream(ProductDtoSortableFields.values())
                    .map(ProductDtoSortableFields::getText)
                    .collect(Collectors.toSet());
        } else {
            allowedSortMappings = allowedFilterMappings;
        }

        Pageable effectivePageable = page;
        SortRequestDto sortDto = searchPayload == null ? null : searchPayload.sort();
        if (sortDto != null && sortDto.sorts() != null) {
            List<Sort.Order> orders = new ArrayList<>();
            for (SortRequestDto.SortOption option : sortDto.sorts()) {
                if (option == null || !StringUtils.hasText(option.field())) {
                    return badRequest("Invalid sort parameter", "Sort field is mandatory");
                }
                String mapping = option.field().trim();
                if (!allowedSortMappings.contains(mapping)) {
                    return badRequest("Invalid sort parameter", "Unknown sort field: " + mapping);
                }
                Sort.Direction direction = option.order() == SortRequestDto.SortOrder.desc
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                orders.add(new Sort.Order(direction, mapping));
            }
            Sort sortSpec = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
            effectivePageable = PageRequest.of(page.getPageNumber(), page.getPageSize(), sortSpec);
        } else {
            for (Sort.Order order : page.getSort()) {
                if (!allowedSortMappings.contains(order.getProperty())) {
                    return badRequest("Invalid sort parameter", "Unknown sort field: " + order.getProperty());
                }
            }
        }

        if (include != null) {
            for (String component : include) {
                try {
                    ProductDtoComponent.valueOf(component);
                } catch (IllegalArgumentException ex) {
                    return badRequest("Invalid include parameter", "Unknown component: " + component);
                }
            }
        }

        AggregationRequestDto aggDto = searchPayload == null ? null : searchPayload.aggs();
        if (aggDto != null && aggDto.aggs() != null && !aggDto.aggs().isEmpty()) {
            if (normalizedVerticalId == null) {
                return badRequest("Invalid aggregation parameter", "Aggregations require a verticalId");
            }
            List<Agg> sanitized = new ArrayList<>();
            for (Agg aggregation : aggDto.aggs()) {
                if (aggregation == null || !StringUtils.hasText(aggregation.field())) {
                    return badRequest("Invalid aggregation parameter", "Aggregation field is mandatory");
                }
                String mapping = aggregation.field().trim();
                if (!allowedFilterMappings.contains(mapping)) {
                    return badRequest("Invalid aggregation parameter", "Aggregation not permitted for field: " + mapping);
                }
                sanitized.add(new Agg(aggregation.name(), mapping, aggregation.type(), aggregation.min(),
                        aggregation.max(), aggregation.buckets(), aggregation.step()));
            }
            aggDto = new AggregationRequestDto(List.copyOf(sanitized));
        } else if (aggDto != null) {
            aggDto = new AggregationRequestDto(List.of());
        }

        FilterRequestDto filterDto = searchPayload == null ? null : searchPayload.filters();
        if (filterDto != null && filterDto.filters() != null) {
            Set<String> validationSet = normalizedVerticalId == null ? globalFilterMappings : allowedFilterMappings;
            List<FilterRequestDto.Filter> sanitized = new ArrayList<>();
            for (FilterRequestDto.Filter filter : filterDto.filters()) {
                if (filter == null || !StringUtils.hasText(filter.field())) {
                    return badRequest("Invalid filters parameter", "Filter field is mandatory");
                }
                String mapping = filter.field().trim();
                if (!validationSet.contains(mapping)) {
                    return badRequest("Invalid filters parameter", "Filter not permitted for field: " + mapping);
                }
                sanitized.add(new FilterRequestDto.Filter(mapping, filter.operator(), filter.terms(), filter.min(),
                        filter.max()));
            }
            filterDto = new FilterRequestDto(List.copyOf(sanitized));
        }

        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Set<String> requestedComponents = include == null ? Set.of() : include;

        ProductSearchResponseDto body = service.searchProducts(effectivePageable, locale, requestedComponents, aggDto,
                domainLanguage, normalizedVerticalId, normalizedQuery, filterDto);

        return ResponseEntity.ok().cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE).body(body);
    }

    private ResponseEntity<ProductSearchResponseDto> badRequest(String title, String detail) {
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
        ProductFieldOptionsResponse body = resolveVerticalFields(verticalId, domainLanguage, globalFields);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }

    private ProductFieldOptionsResponse resolveVerticalFields(String verticalId, DomainLanguage domainLanguage,
            List<FieldMetadataDto> globalFields) {
        List<FieldMetadataDto> immutableGlobal = List.copyOf(globalFields);
        if (!StringUtils.hasText(verticalId)) {
            return new ProductFieldOptionsResponse(immutableGlobal, List.of(), List.of());
        }

        VerticalConfig vConfig = verticalsConfigService.getConfigById(verticalId);
        if (vConfig == null) {
            return null;
        }

        List<FieldMetadataDto> globalWithAggregation = augmentFieldsWithAggregationMetadata(immutableGlobal, vConfig);
        List<FieldMetadataDto> impactFields = new ArrayList<>();
        FieldMetadataDto ecoscore = buildEcoscoreField(vConfig);
        impactFields.add(ecoscore);
        mapImpactScores(vConfig, domainLanguage).stream()
                .filter(field -> !Objects.equals(field.mapping(), ecoscore.mapping()))
                .forEach(impactFields::add);

        List<FieldMetadataDto> technicalFields = new ArrayList<>();
        technicalFields.addAll(mapVerticalAttributeFilters(vConfig.getEcoFilters(), vConfig, domainLanguage));
        technicalFields.addAll(mapVerticalAttributeFilters(vConfig.getGlobalTechnicalFilters(), vConfig, domainLanguage));
        technicalFields.addAll(mapVerticalAttributeFilters(vConfig.getTechnicalFilters(), vConfig, domainLanguage));

        return new ProductFieldOptionsResponse(globalWithAggregation, List.copyOf(impactFields), List.copyOf(technicalFields));
    }

    private ProductFieldOptionsResponse safeResolveVerticalFields(String verticalId, DomainLanguage domainLanguage,
            List<FieldMetadataDto> globalFields) {
        ProductFieldOptionsResponse resolved = resolveVerticalFields(verticalId, domainLanguage, globalFields);
        if (resolved == null) {
            return new ProductFieldOptionsResponse(List.copyOf(globalFields), List.of(), List.of());
        }
        return resolved;
    }

    private Set<String> collectAllowedFieldMappings(ProductFieldOptionsResponse fieldOptions) {
        Set<String> allowed = new HashSet<>();
        if (fieldOptions == null) {
            return allowed;
        }
        addFieldMappings(allowed, fieldOptions.global());
        addFieldMappings(allowed, fieldOptions.impact());
        addFieldMappings(allowed, fieldOptions.technical());
        return allowed;
    }

    private void addFieldMappings(Set<String> target, List<FieldMetadataDto> fields) {
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
     * @return fully qualified field path pointing to the indexed attribute value
     */
    private String toIndexedAttribute(String filterName, VerticalConfig config) {
        String baseField = "attributes.indexed." + filterName;
        AttributeConfig attributeConfig = null;
        if (config.getAttributesConfig() != null) {
            attributeConfig = config.getAttributesConfig().getAttributeConfigByKey(filterName);
        }
        if (attributeConfig != null && attributeConfig.getFilteringType() == AttributeType.NUMERIC) {
            return baseField + ".numericValue";
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
    }

    private FieldMetadataDto buildEcoscoreField(VerticalConfig config) {
        String mapping = "scores.ECOSCORE.value";
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
        if (config.getAvailableImpactScoreCriterias() == null || config.getAvailableImpactScoreCriterias().isEmpty()) {
            return List.of();
        }

        List<FieldMetadataDto> results = new ArrayList<>();
        config.getAvailableImpactScoreCriterias().forEach((key, criteria) -> {
            String mapping = "scores." + key + ".value";
            String title = criteria.getTitle() != null ? localise(criteria.getTitle(), domainLanguage) : null;
            String description = criteria.getDescription() != null ? localise(criteria.getDescription(), domainLanguage) : null;
            FieldMetadataDto.AggregationMetadata aggregation = resolveAggregationMetadata(config, mapping, key);
            results.add(new FieldMetadataDto(mapping, title, description, VALUE_TYPE_NUMERIC, aggregation));
        });
        return List.copyOf(results);
    }

    private String resolveAttributeValueType(VerticalConfig config, String attributeKey) {
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
        return switch (field) {
        case price, offersCount -> VALUE_TYPE_NUMERIC;
        };
    }

    private String determineFilterValueType(ProductDtoFilterFields field) {
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
        if (localisable == null) {
            return null;
        }
        String language = domainLanguage != null ? domainLanguage.languageTag() : null;
        return localisable.i18n(language);
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

        ProductDto body = service.getProduct(gtin, locale, include, domainLanguage);



        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

}
