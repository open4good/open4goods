package org.open4goods.api.controller.api;

import org.open4goods.embedding.service.TextEmbeddingService;
import org.open4goods.model.RolesConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;



/**
 * Controller exposing text embedding generation for internal services
 * (e.g., front-api proxying).
 */
@RestController
@ConditionalOnBean(TextEmbeddingService.class)
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class EmbeddingController {

    private final TextEmbeddingService textEmbeddingService;

    public EmbeddingController(TextEmbeddingService textEmbeddingService) {
        this.textEmbeddingService = textEmbeddingService;
    }

    /**
     * Generate embedding for a single text.
     */
    @GetMapping(path = "/product/embedding")
    @Operation(summary = "Generate a semantic embedding for a given text")
    @PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
    public float[] getEmbedding(@RequestParam @NotBlank final String text) {
        return textEmbeddingService.embed(text);
    }
}
