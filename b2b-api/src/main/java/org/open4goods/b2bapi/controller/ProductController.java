package org.open4goods.b2bapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.open4goods.b2bapi.config.OpenApiConfig;
import org.open4goods.b2bapi.dto.product.B2bEnergyDto;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bPriceHistoryDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.open4goods.b2bapi.service.B2bProductService;
import org.open4goods.b2bapi.service.PriceHistoryFacetService;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing B2B product endpoints.
 */
@Tag(name = "Product Data", description = "B2B Product Data Facets")
@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final B2bProductService b2bProductService;
    private final PriceHistoryFacetService priceHistoryFacetService;

    @Autowired
    public ProductController(
            final B2bProductService b2bProductService,
            final PriceHistoryFacetService priceHistoryFacetService) {
        this.b2bProductService = b2bProductService;
        this.priceHistoryFacetService = priceHistoryFacetService;
    }

    /**
     * Retrieves the price facet of a product by its raw GTIN string.
     *
     * @param gtin raw GTIN barcode
     * @param language language parameter
     * @param principal authenticated principal
     * @param request HTTP request
     * @param response HTTP response
     * @return response envelope containing price facet and metadata
     */
    @Operation(
            summary = "Get product price facet",
            description = "Retrieves the price facet and aggregate offers for a product using its GTIN. " +
                          "Requires a valid API key. Billed only if fresh offers exist.",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product price details retrieved successfully.",
            content = @Content(schema = @Schema(implementation = B2bResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid GTIN checksum/format or parameters.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or revoked API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "402",
            description = "Insufficient credits for the request.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected internal server error.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @GetMapping("/{gtin}/price")
    public B2bResponse<B2bPriceDto> getProductPrice(
            @Parameter(description = "Barcode identifier (GTIN-8, GTIN-12, GTIN-13, or GTIN-14)", required = true, example = "0885909950805")
            @PathVariable final String gtin,
            @Parameter(description = "Locale language for text/display names (e.g. 'en', 'fr')", example = "en")
            @RequestParam(required = false, defaultValue = "en") final String language,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return b2bProductService.getProductPrice(gtin, language, principal, request, response);
    }

    /**
     * Retrieves the EPREL-sourced energy label facet of a product by its raw GTIN string.
     *
     * @param gtin raw GTIN barcode
     * @param language language parameter
     * @param principal authenticated principal
     * @param request HTTP request
     * @param response HTTP response
     * @return response envelope containing the energy facet and metadata
     */
    @Operation(
            summary = "Get product energy label facet",
            description = "Retrieves the EU energy label facet for a product using its GTIN, sourced from EPREL. "
                    + "Requires a valid API key (a free authenticated account is enough - no paid plan required). "
                    + "This facet is always free: never billed, zero credits, per the EPREL API Terms and "
                    + "Conditions 4§2(a) prohibition on reselling EPREL data as-is. Attribution to "
                    + "https://eprel.ec.europa.eu is included in every response (article 4§3).",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product energy label retrieved successfully (or absent, still zero credits).",
            content = @Content(schema = @Schema(implementation = B2bResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid GTIN checksum/format or parameters.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or revoked API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected internal server error.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @GetMapping("/{gtin}/energy")
    public B2bResponse<B2bEnergyDto> getProductEnergy(
            @Parameter(description = "Barcode identifier (GTIN-8, GTIN-12, GTIN-13, or GTIN-14)", required = true, example = "0885909950805")
            @PathVariable final String gtin,
            @Parameter(description = "Locale language for text/display names (e.g. 'en', 'fr')", example = "en")
            @RequestParam(required = false, defaultValue = "en") final String language,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return b2bProductService.getProductEnergy(gtin, language, principal, request, response);
    }

    /**
     * Retrieves the provider price-history facet of a product by its raw GTIN string.
     *
     * @param gtin raw GTIN barcode
     * @param language language parameter
     * @param from inclusive UTC start (ISO-8601 instant), defaults to 30 days before {@code to}
     * @param to exclusive UTC end (ISO-8601 instant), defaults to now
     * @param granularity {@code DAY} or {@code CHANGE}, defaults to {@code DAY}
     * @param provider reviewed public provider selector; never an internal source id
     * @param condition {@code NEW}, {@code OCCASION}, or {@code UNKNOWN}
     * @param currency ISO 4217 currency code
     * @param limit page size, {@value PriceHistoryQuery#MIN_PAGE_SIZE}..{@value PriceHistoryQuery#MAX_PAGE_SIZE}
     * @param cursor opaque continuation returned by a previous page
     * @param principal authenticated principal
     * @param request HTTP request
     * @param response HTTP response
     * @return response envelope containing the price-history facet and metadata
     */
    @Operation(
            summary = "Get product provider price-history facet",
            description = "Retrieves licensed provider price-history series (daily rollups or sparse changes) "
                    + "for a product using its GTIN. Requires a valid API key. Billed only when at least one "
                    + "policy-allowed price-history point is served.",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product price-history retrieved successfully (or absent, zero credits).",
            content = @Content(schema = @Schema(implementation = B2bResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid GTIN, date range, granularity, limit, or cursor/query mismatch.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or revoked API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "402",
            description = "Insufficient credits for the request.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected internal server error.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @GetMapping("/{gtin}/price/history")
    public B2bResponse<B2bPriceHistoryDto> getProductPriceHistory(
            @Parameter(description = "Barcode identifier (GTIN-8, GTIN-12, GTIN-13, or GTIN-14)", required = true, example = "0885909950805")
            @PathVariable final String gtin,
            @Parameter(description = "Locale language for text/display names (e.g. 'en', 'fr')", example = "en")
            @RequestParam(required = false, defaultValue = "en") final String language,
            @Parameter(description = "Inclusive UTC start (ISO-8601 instant). Defaults to 30 days before 'to'.", example = "2026-05-16T00:00:00Z")
            @RequestParam(required = false) final String from,
            @Parameter(description = "Exclusive UTC end (ISO-8601 instant). Defaults to now.", example = "2026-06-15T00:00:00Z")
            @RequestParam(required = false) final String to,
            @Parameter(description = "Requested granularity. Defaults to DAY.", example = "DAY")
            @RequestParam(required = false) final String granularity,
            @Parameter(description = "Reviewed public provider selector. Never an internal source id.", example = "Merchant Feed Partner")
            @RequestParam(required = false) final String provider,
            @Parameter(description = "Product condition filter.", example = "NEW")
            @RequestParam(required = false) final String condition,
            @Parameter(description = "ISO 4217 currency filter.", example = "EUR")
            @RequestParam(required = false) final String currency,
            @Parameter(description = "Page size.", example = "100",
                    schema = @Schema(minimum = "" + PriceHistoryQuery.MIN_PAGE_SIZE, maximum = "" + PriceHistoryQuery.MAX_PAGE_SIZE))
            @RequestParam(required = false) final Integer limit,
            @Parameter(description = "Opaque continuation from a previous page.", example = "cHJpY2UtaGlzdG9yeS1jdXJzb3I6MTAw")
            @RequestParam(required = false) final String cursor,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return priceHistoryFacetService.getProductPriceHistory(
                gtin, language, from, to, granularity, provider, condition, currency, limit, cursor,
                principal, request, response);
    }
}
