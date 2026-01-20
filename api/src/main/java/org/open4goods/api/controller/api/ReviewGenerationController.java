package org.open4goods.api.controller.api;

import java.util.concurrent.CompletableFuture;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing AI review generation endpoints for the back-office API.
 * <p>
 * The controller mirrors the behaviour previously offered by the legacy UI module: a POST endpoint triggers
 * the asynchronous generation process while a GET endpoint returns the latest status snapshot for polling clients.
 * API access is guarded by the {@link RolesConstants#ROLE_ADMIN} authority which is granted by the API key interceptor.
 * </p>
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Review generation", description = "Trigger and inspect AI review generation jobs.")
public class ReviewGenerationController {

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final ReviewGenerationService reviewGenerationService;

    public ReviewGenerationController(ProductRepository productRepository,
            VerticalsConfigService verticalsConfigService,
            ReviewGenerationService reviewGenerationService) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.reviewGenerationService = reviewGenerationService;
    }

    /**
     * Trigger asynchronous AI review generation for the requested product.
     *
     * @param upc the product identifier used as review generation key
     * @return the UPC echoed back once the generation has been scheduled
     * @throws ResourceNotFoundException when the product does not exist in the repository
     */

    @PostMapping("/review/{id}")
    @Operation(summary = "Schedule AI review generation", description = "Launch the asynchronous AI review pipeline for the given UPC.",
            parameters = {
                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                            description = "Product UPC used to request review generation.",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0")),

// TODO(P1, security)  : enforce this parameter, tight to an admin role
                    @Parameter(name = "required", in = ParameterIn.PATH, required = false,
                    description = "Force the review generation, even if already processed",
                    schema = @Schema(type = "boolean", format = "bool", minimum = "0", defaultValue = "false")

                    		)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Generation scheduled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Long.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found")
            })
    public ResponseEntity<Long> generateReview(@PathVariable("id") long upc, @PathVariable( required = false, name = "force") boolean force, HttpServletRequest request) throws ResourceNotFoundException {
        Product product = productRepository.getById(upc);
        VerticalConfig verticalConfig = verticalsConfigService.getConfigById(product.getVertical());
        
        Map<String, String> headers = new HashMap<>();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            headers.put("User-Agent", userAgent);
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            headers.put("X-Forwarded-For", xForwardedFor);
        }
        String xForwardedIp = request.getHeader("X-Forwarded-IP"); // Also requested by user
        if (xForwardedIp != null) {
        	headers.put("X-Forwarded-IP", xForwardedIp);
        }
        
        long scheduledUpc = reviewGenerationService.generateReviewAsync(product, verticalConfig, CompletableFuture.completedFuture(null), force, headers);
        return ResponseEntity.ok(scheduledUpc);
    }

    /**
     * Retrieve the latest status of an AI review generation request.
     *
     * @param upc the product identifier used to track the process
     * @return the review generation status or {@code null} when no process is tracked for the UPC
     */
    @GetMapping("/review/{id}")
    @Operation(summary = "Get review generation status", description = "Return the latest status snapshot for the requested UPC.",
            parameters = {
                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                            description = "Product UPC used when the review generation was requested.",
                            schema = @Schema(type = "integer", format = "int64", minimum = "0"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReviewGenerationStatus.class))),
                    @ApiResponse(responseCode = "404", description = "Product not found", content = @Content())
            })
    public ResponseEntity<ReviewGenerationStatus> getReviewStatus(@PathVariable("id") long upc) {
        ReviewGenerationStatus status = reviewGenerationService.getProcessStatus(upc);
        return ResponseEntity.ok(status);
    }
}
