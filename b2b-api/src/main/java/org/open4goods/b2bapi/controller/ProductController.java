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
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.open4goods.b2bapi.service.B2bProductService;
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

    @Autowired
    public ProductController(final B2bProductService b2bProductService) {
        this.b2bProductService = b2bProductService;
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
}
