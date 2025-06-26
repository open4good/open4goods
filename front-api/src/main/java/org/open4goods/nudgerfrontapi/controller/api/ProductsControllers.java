package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoSortableFields;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Product", description = "Read product data, offers, impact score and reviews; trigger AI review generation.")
public class ProductsControllers {

	// TODO : mutualize constant
    private static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final ProductMappingService service;

    public ProductsControllers(ProductMappingService service) {
        this.service = service;
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
                    @Parameter(name = "page[number]", in = ParameterIn.QUERY, description = "Zero-based page index"),
                    @Parameter(name = "page[size]", in = ParameterIn.QUERY, description = "Page size"),
                    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Sort criteria in the format: property,(asc|desc). ",array = @ArraySchema(
                            schema = @Schema(implementation = ProductDtoSortableFields.class)
                    )),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Products returned",
                            headers = @Header(name = "Link", description = "Pagination links as defined by RFC 8288"),
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDto.class, type = "array"))),
                    @ApiResponse(responseCode = "401", description = "Authentication required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<Page<ProductDto>> products(
    		@Parameter(hidden = true) @PageableDefault(size = 20) Pageable page,
    		@RequestParam(required=false) Set<String> include,
    		  Locale locale) {


		/////////////////////
		/// Surface control
		/////////////////////

		// Validating sort field
		page.getSort().stream().forEach(s -> {
			if (!ProductDtoSortableFields.fromText(s.getProperty()).isPresent()) {

				// TODO : HAndle this invalid value, raise approriate http code and
				// problemdetail to the client.
				return;
			}
		});

		// Validating requested components
		if (null != include) {
			include.forEach(i -> {
				if (null == ProductDtoComponent.valueOf(i)) {
					// TODO : Handle this invalid value, raise approriate http code and
					// problemdetail to the client.
					return;
				}

			});
		}


		////////////////////////
		// Transforming product to DTO
		///////////////////////

		Page<ProductDto> body = service.getProducts(page, locale, include == null ? Set.of() : include);



		return ResponseEntity.ok().cacheControl(ONE_HOUR_PUBLIC_CACHE).body(body);
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
                                                       Set<String> include,
                                                       Locale locale){

        ProductDto body = service.getProduct(gtin, locale, include);



        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }
//
//
//    //////////////////////////////////
//    /// AI review endpoints
//    //////////////////////////////////
//
//    /**
//     * List customer or AI reviews for a product.
//     *
//     * <p>Error codes:</p>
//     * <ul>
//     *   <li><b>INVALID_GTIN</b> – 400</li>
//     *   <li><b>UNAUTHORIZED</b> – 401</li>
//     *   <li><b>FORBIDDEN</b> – 403</li>
//     *   <li><b>NOT_FOUND</b> – 404</li>
//     *   <li><b>INTERNAL_ERROR</b> – 500</li>
//     * </ul>
//     */
//    @GetMapping("/{gtin}/reviews")
//    @Operation(
//            summary = "Get product reviews",
//            description = "Return customer or AI‑generated reviews for a product.",
//            security = @SecurityRequirement(name = "bearer-jwt"),
//            parameters = {
//                    @Parameter(name = "gtin", description = "Global Trade Item Number (8–14 digit numeric code)", example = "00012345600012", required = true),
//
//                    @Parameter(name = "page[number]", in = ParameterIn.QUERY, description = "Zero-based page index"),
//                    @Parameter(name = "page[size]", in = ParameterIn.QUERY, description = "Page size")
//
//            },
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "Reviews returned",
//                            headers = @Header(name = "Link", description = "Pagination links as defined by RFC 8288"),
//                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductReviewDto.class, type = "array"))),
//                    @ApiResponse(responseCode = "400", description = "Invalid GTIN"),
//                    @ApiResponse(responseCode = "401", description = "Authentication required"),
//                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
//                    @ApiResponse(responseCode = "404", description = "Product not found"),
//                    @ApiResponse(responseCode = "500", description = "Internal server error")
//            }
//    )
//    public ResponseEntity<Page<ProductReviewDto>> reviews(
//            @PathVariable @Pattern(regexp = "\\d{8,14}") String gtin,
//            @PageableDefault(size = 20) Pageable pageable) throws Exception {
//
//
//        Page<ProductReviewDto> body = service.getReviews(Long.parseLong(gtin), pageable);
//        return ResponseEntity.ok()
//                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
//                .body(body);
//    }
//
//    /**
//     * Enqueue an AI review generation.
//     *
//     * <p>Error codes:</p>
//     * <ul>
//     *   <li><b>INVALID_GTIN</b> – 400</li>
//     *   <li><b>UNAUTHORIZED</b> – 401</li>
//     *   <li><b>FORBIDDEN</b> – 403</li>
//     *   <li><b>TOO_MANY_REQUESTS</b> – 429</li>
//     *   <li><b>INTERNAL_ERROR</b> – 500</li>
//     * </ul>
//     */
//    @PostMapping("/{gtin}/reviews")
//    @Operation(
//            summary = "Generate AI review",
//            description = "Trigger the generation of an AI‑written review.  Returns 202 to indicate that processing has started.",
//            parameters = {
//                    @Parameter(name = "gtin", description = "Global Trade Item Number", example = "00012345600012", required = true),
//                    @Parameter(name = "hcaptchaResponse", required = true,
//                               description = "Token returned by hCaptcha widget for bot mitigation.")
//            },
//            security = {
//                    @SecurityRequirement(name = "bearer-jwt"),
//                    @SecurityRequirement(name = "hCaptcha")
//            },
//            responses = {
//                    @ApiResponse(responseCode = "202", description = "Accepted – review generation enqueued"),
//                    @ApiResponse(responseCode = "400", description = "Invalid GTIN or hCaptcha token"),
//                    @ApiResponse(responseCode = "401", description = "Authentication required"),
//                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
//                    @ApiResponse(responseCode = "429", description = "Too many concurrent generations"),
//                    @ApiResponse(responseCode = "500", description = "Internal server error")
//            }
//    )
//    public ResponseEntity<Void> generateReview(@PathVariable @Pattern(regexp = "\\d{8,14}") String gtin,
//                                               @RequestParam("hcaptchaResponse") String captcha,
//                                               HttpServletRequest request) throws Exception {
//        service.createReview(Long.parseLong(gtin), captcha, request);
//        return ResponseEntity.accepted().build();
//    }

}
