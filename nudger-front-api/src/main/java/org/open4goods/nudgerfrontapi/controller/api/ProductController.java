package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.List;

import org.open4goods.nudgerfrontapi.dto.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.OfferDto;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;
import org.open4goods.nudgerfrontapi.service.ProductService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;

/**
 * REST controller exposing read‑only information about a product as well as the ability to
 * trigger an AI‑generated review.  All endpoints are grouped under the <i>Product</i> tag in the
 * generated OpenAPI contract and share the common base path <code>/product</code>.
 */
@RestController
@RequestMapping("/product")
@Validated
@Tag(name = "Product", description = "Read product data, offers, impact score and reviews; trigger AI review generation.")
public class ProductController {

    private static final CacheControl ONE_HOUR_PUBLIC_CACHE =
            CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/{gtin}")
    @Operation(
            summary = "Get product view",
            description = "Return high‑level product information and aggregated scores.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = @Parameter(name = "gtin",
                    description = "Global Trade Item Number (8–14 digit numeric code)",
                    example = "00012345600012",
                    required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ProductViewResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            }
    )
    public ResponseEntity<ProductViewResponse> product(@PathVariable
                                                       @Pattern(regexp = "\\d{8,14}") String gtin) throws Exception {
        ProductViewResponse body = service.getProduct(Long.parseLong(gtin));
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/{gtin}/reviews")
    @Operation(
            summary = "Get product reviews",
            description = "Return customer or AI‑generated reviews for a product.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = @Parameter(name = "gtin",
                    description = "Global Trade Item Number (8–14 digit numeric code)",
                    example = "00012345600012",
                    required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reviews returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReviewDto.class, type = "array"))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            }
    )
    public ResponseEntity<List<ReviewDto>> reviews(@PathVariable
                                                   @Pattern(regexp = "\\d{8,14}") String gtin) throws Exception {
        List<ReviewDto> body = service.getReviews(Long.parseLong(gtin));
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @PostMapping("/{gtin}/reviews")
    @Operation(
            summary = "Generate AI review",
            description = "Trigger the generation of an AI‑written review.  Returns 202 to indicate that processing has started.",
            parameters = {
                    @Parameter(name = "gtin", description = "Global Trade Item Number", example = "00012345600012", required = true),
                    @Parameter(name = "hcaptchaResponse", required = true,
                               description = "Token returned by hCaptcha widget for bot mitigation.")
            },
            security = {
                    @SecurityRequirement(name = "bearer-jwt"),
                    @SecurityRequirement(name = "hCaptcha")
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Accepted – review generation enqueued"),
                    @ApiResponse(responseCode = "400", description = "Invalid GTIN or hCaptcha token"),
                    @ApiResponse(responseCode = "429", description = "Too many concurrent generations")
            }
    )
    public ResponseEntity<Void> generateReview(@PathVariable @Pattern(regexp = "\\d{8,14}") String gtin,
                                               @RequestParam("hcaptchaResponse") String captcha,
                                               HttpServletRequest request) throws Exception {
        service.createReview(Long.parseLong(gtin), captcha, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{gtin}/offers")
    @Operation(
            summary = "Get product offers",
            description = "Return available commercial offers for a product, sorted by total price ascending.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = @Parameter(name = "gtin", description = "Global Trade Item Number", example = "00012345600012", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offers returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OfferDto.class, type = "array"))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            }
    )
    public ResponseEntity<List<OfferDto>> offers(@PathVariable @Pattern(regexp = "\\d{8,14}") String gtin) throws Exception {
        List<OfferDto> body = service.getOffers(Long.parseLong(gtin));
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/{gtin}/impact")
    @Operation(
            summary = "Get product impact score",
            description = "Return environmental and social impact composite score for a product.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = @Parameter(name = "gtin", description = "Global Trade Item Number", example = "00012345600012", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Impact score returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ImpactScoreDto.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            }
    )
    public ResponseEntity<ImpactScoreDto> impact(@PathVariable @Pattern(regexp = "\\d{8,14}") String gtin) throws Exception {
        ImpactScoreDto body = service.getImpactScore(Long.parseLong(gtin));
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }
}
