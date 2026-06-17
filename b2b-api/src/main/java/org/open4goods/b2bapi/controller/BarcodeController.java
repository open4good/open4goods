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
import jakarta.validation.Valid;
import java.util.List;
import org.open4goods.b2bapi.config.OpenApiConfig;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderResponse;
import org.open4goods.b2bapi.dto.barcode.check.BarcodeCheckResponse;
import org.open4goods.b2bapi.model.BarcodeAsset;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.open4goods.b2bapi.service.B2bBarcodeCheckService;
import org.open4goods.b2bapi.service.B2bBarcodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for barcode generation, ZIP exports, and asset retrieval.
 */
@Tag(name = "Barcode Rendering", description = "B2B Barcode Generation and Storage Services")
@Validated
@RestController
@RequestMapping("/api/v1/barcodes")
public class BarcodeController {

    private final B2bBarcodeService barcodeService;
    private final B2bBarcodeCheckService barcodeCheckService;

    @Autowired
    public BarcodeController(
            final B2bBarcodeService barcodeService,
            final B2bBarcodeCheckService barcodeCheckService) {
        this.barcodeService = barcodeService;
        this.barcodeCheckService = barcodeCheckService;
    }

    /**
     * Renders a barcode asset and returns a metadata envelope and signed URL.
     *
     * @param req the barcode rendering parameters
     * @param principal authenticated principal
     * @param request HTTP request
     * @param response HTTP response
     * @return response containing signed URL and metadata details
     */
    @Operation(
            summary = "Render a barcode",
            description = "Generates a print-ready barcode image or SVG based on the parameters and returns a signed asset URL. Requires a valid API key.",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "Barcode generated successfully.",
            content = @Content(schema = @Schema(implementation = B2bBarcodeRenderResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid barcode symbology, checksum, or options.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "402",
            description = "Insufficient credits.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @PostMapping("/render")
    public B2bBarcodeRenderResponse renderBarcode(
            @RequestBody @Valid final B2bBarcodeRenderRequest req,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return barcodeService.renderBarcode(req, principal, request, response);
    }

    /**
     * Renders multiple barcodes and exports them as a ZIP archive.
     *
     * @param requests list of barcode rendering requests
     * @param principal authenticated principal
     * @param request HTTP request
     * @param response HTTP response
     * @return a ZIP archive download containing the rendered barcodes
     */
    @Operation(
            summary = "Batch export barcodes as ZIP",
            description = "Generates a list of barcodes and returns a ZIP file. Requires a valid API key. Billed per item rendered.",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "ZIP archive returned successfully.",
            content = @Content(mediaType = "application/zip")
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid barcode options or parameters.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "402",
            description = "Insufficient credits.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @PostMapping(value = "/render-zip", produces = "application/zip")
    public ResponseEntity<byte[]> renderBarcodeZip(
            @RequestBody @Valid final List<B2bBarcodeRenderRequest> requests,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final byte[] zipBytes = barcodeService.renderBarcodeZip(requests, principal, request, response);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"barcodes.zip\"")
                .body(zipBytes);
    }

    /**
     * Public endpoint to download a signed, cached barcode asset.
     *
     * @param token the signed token for the asset
     * @return the barcode image bytes (PNG or SVG)
     */
    @Operation(
            summary = "Download signed barcode asset",
            description = "Publicly retrieves a cached barcode asset by its signed token. Does not require authentication."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Barcode image retrieved successfully."
    )
    @ApiResponse(
            responseCode = "404",
            description = "Asset not found or expired.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/assets/{token}")
    public ResponseEntity<byte[]> getBarcodeAsset(
            @Parameter(description = "Signed JWT token identifying the asset", required = true)
            @PathVariable final String token) {
        final BarcodeAsset asset = barcodeService.getBarcodeAsset(token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000") // 30 days cache
                .body(asset.getContent());
    }

    /**
     * Public barcode validity check and product lookup — no authentication required.
     * Rate-limited per IP address.
     *
     * @param barcode the barcode string to check
     * @param request HTTP request (used to extract the client IP for rate limiting)
     * @return check response with forensics and optional product teaser
     */
    @Operation(
            summary = "Check a barcode (public)",
            description = "Validates the check digit, classifies the barcode type and GS1 class, " +
                    "resolves the issuing GS1 member country, and returns a product teaser when a " +
                    "matching entry exists in the nudger.fr index. No authentication required. " +
                    "Rate-limited per IP address."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result returned (even for invalid barcodes).",
            content = @Content(schema = @Schema(implementation = BarcodeCheckResponse.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "IP rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping("/check")
    public BarcodeCheckResponse checkBarcodePublic(
            @Parameter(description = "Barcode string to check", required = true, example = "3017620422003")
            @RequestParam final String barcode,
            final HttpServletRequest request) {
        final String clientIp = resolveClientIp(request);
        return barcodeCheckService.checkPublic(barcode, clientIp);
    }

    /**
     * Authenticated barcode check — requires a valid API key. Records a zero-cost usage event.
     *
     * @param gtin the barcode / GTIN to check
     * @param principal authenticated API key principal
     * @param request HTTP request
     * @param response HTTP response
     * @return check response with forensics and optional product teaser
     */
    @Operation(
            summary = "Check a barcode (API key)",
            description = "Same as the public check endpoint but authenticated. " +
                    "Records a zero-cost usage event. Credits consumed: 0.",
            security = @SecurityRequirement(name = OpenApiConfig.PRODUCT_DATA_API_KEY)
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result returned.",
            content = @Content(schema = @Schema(implementation = BarcodeCheckResponse.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid API key.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PreAuthorize("hasAuthority('PDAPI_KEY')")
    @GetMapping("/{gtin}/check")
    public BarcodeCheckResponse checkBarcode(
            @Parameter(description = "Barcode or GTIN to check", required = true, example = "3017620422003")
            @PathVariable final String gtin,
            @AuthenticationPrincipal final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        return barcodeCheckService.check(gtin, principal, request, response);
    }

    private String resolveClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
