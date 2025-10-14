package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoAggregatableFields;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoSortableFields;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

    public ProductController(ProductMappingService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
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
     * List product fields that support aggregation queries.
     */
    @GetMapping("/fields/aggregatable")
    @Operation(
            summary = "Get aggregatable fields",
            description = "Return the list of fields available for aggregation.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language used to localise aggregation labels in future responses.",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fields returned",

                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(type = "string"))))
            }
    )
    public ResponseEntity<List<String>> aggregatableFields(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<String> body = Arrays.stream(ProductDtoAggregatableFields.values())
                .map(ProductDtoAggregatableFields::getText)
                .toList();
        return ResponseEntity.ok(body);
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
    @GetMapping
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
                    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Sort criteria in the format: property,(asc|desc). ",array = @ArraySchema(
                            schema = @Schema(implementation = ProductDtoSortableFields.class)
                    )),
                    @Parameter(name = "aggs", in = ParameterIn.QUERY,
                            description = "Aggregations definition as JSON",
                            schema = @Schema(implementation = AggregationRequestDto.class)),
                    @Parameter(name = "filters", in = ParameterIn.QUERY,
                            description = "Filters definition as JSON. The payload must follow the FilterRequestDto schema.",
                            schema = @Schema(implementation = FilterRequestDto.class),
                            example = "{\"filters\":[{\"field\":\"condition\",\"operator\":\"term\",\"terms\":[\"NEW\"]}]}"),
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
            responses = {
                    @ApiResponse(responseCode = "200", description = "Products returned",
                            headers = {
                                    @Header(name = "Link", description = "Pagination links as defined by RFC 8288"),
                                    @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                            schema = @Schema(type = "string", example = "fr-FR"))
                            },
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductSearchResponseDto.class))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ProductSearchResponseDto> products(
                @Parameter(hidden = true) @PageableDefault(size = 20) Pageable page,
                @RequestParam(required=false) Set<String> include,
                @RequestParam(required=false) String aggs,
                @RequestParam(required = false) String filters,
                @RequestParam(required = false) String verticalId,
                @RequestParam(required = false) String query,
                @RequestParam() DomainLanguage domainLanguage,
                Locale locale) {


		/////////////////////
		/// Surface control
		/////////////////////

		// Validating sort field
		for (var order : page.getSort()) {
			if (ProductDtoSortableFields.fromText(order.getProperty()).isEmpty()) {
				ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
				pd.setTitle("Invalid sort parameter");
				pd.setDetail("Unknown sort field: " + order.getProperty());
				@SuppressWarnings({ "unchecked", "rawtypes" })
				ResponseEntity<ProductSearchResponseDto> response = (ResponseEntity) ResponseEntity.badRequest().body(pd);
				return response;
			}
		}
		// Validating requested components

		if (include != null) {
			for (String i : include) {
				try {
					ProductDtoComponent.valueOf(i);
				} catch (IllegalArgumentException ex) {
					ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
					pd.setTitle("Invalid include parameter");
					pd.setDetail("Unknown component: " + i);
					@SuppressWarnings({ "unchecked", "rawtypes" })
					ResponseEntity<ProductSearchResponseDto> response = (ResponseEntity) ResponseEntity.badRequest().body(pd);
					return response;
				}
			}
		}

		// Validating requested aggregations

		AggregationRequestDto aggDto = null;
		if (aggs != null) {
			try {
				aggDto = objectMapper.readValue(aggs, AggregationRequestDto.class);
			} catch (JsonProcessingException e) {
				ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
				pd.setTitle("Invalid aggregation parameter");
				pd.setDetail("Unable to parse aggregation query: " + e.getMessage());
				@SuppressWarnings({ "unchecked", "rawtypes" })
				ResponseEntity<ProductSearchResponseDto> response = (ResponseEntity) ResponseEntity.badRequest().body(pd);
				return response;
			}
		}

		FilterRequestDto filterDto = null;
		if (filters != null) {
			try {
				filterDto = objectMapper.readValue(filters, FilterRequestDto.class);
			} catch (JsonProcessingException e) {
				ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
				pd.setTitle("Invalid filters parameter");
				pd.setDetail("Unable to parse filter definition: " + e.getOriginalMessage());
				@SuppressWarnings({ "unchecked", "rawtypes" })
				ResponseEntity<ProductSearchResponseDto> response = (ResponseEntity) ResponseEntity.badRequest().body(pd);
				return response;
			}
		}

		String normalizedVerticalId = (verticalId != null && !verticalId.isBlank()) ? verticalId.trim() : null;
		String normalizedQuery = (query != null && !query.isBlank()) ? query.trim() : null;

		Set<String> requestedComponents = include == null ? Set.of() : include;
		ProductSearchResponseDto body = service.searchProducts(page, locale, requestedComponents, aggDto, domainLanguage, normalizedVerticalId, normalizedQuery, filterDto);

		return ResponseEntity.ok().cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE).body(body);
    }

    /**
     * Return high level information for a product.
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
            description = "Return high‑level product information and aggregated scores.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "gtin",
                            description = "Global Trade Item Number (8–14 digit numeric code)",
                            example = "8806095491998",
                            required = true),
                    @Parameter(
                            name        = "include",
                            in          = ParameterIn.QUERY,
                            description = "Champs à inclure (peut se répéter)",
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
