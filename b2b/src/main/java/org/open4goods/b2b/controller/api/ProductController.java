package org.open4goods.b2b.controller.api;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.open4goods.b2b.dto.product.ProductDto;
import org.open4goods.b2b.service.ProductAccessService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import model.AvailableFacets;


@RestController
@RequestMapping("/products")
@Validated
@Tag(name = "Product", description = "Get product data")
public class ProductController {

	// TODO : mutualize constant
    private static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final ProductAccessService service;

    public ProductController(ProductAccessService service) {
        this.service = service;
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
                            required = true),
                    @Parameter(
                            name        = "include",
                            in          = ParameterIn.QUERY,
                            description = "Champs à inclure",
                            array       = @ArraySchema(
                                schema = @Schema(implementation = AvailableFacets.class)
                            )
                        )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product found",
                                    content = @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ProductDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid GTIN or include parameter"),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Product not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ProductDto> product(@PathVariable
                                                       Long gtin,
                                                       @RequestParam(required = false)
                                                       Set<AvailableFacets> include,
                                                       Locale locale) throws ResourceNotFoundException {

        ProductDto body = service.getProduct(gtin, locale, include);

        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }


}
