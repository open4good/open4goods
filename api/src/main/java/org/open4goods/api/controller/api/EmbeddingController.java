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
 * Endpoint for generating semantic text embeddings, used by {@code front-api} for search.
 */
@RestController
@ConditionalOnBean(TextEmbeddingService.class)
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class EmbeddingController {

    private final TextEmbeddingService textEmbeddingService;

    public EmbeddingController(TextEmbeddingService textEmbeddingService) {
        this.textEmbeddingService = textEmbeddingService;
    }

    @GetMapping(path = "/embedding")
    @Operation(summary = "Generate a semantic embedding for a given text")
    public float[] getEmbedding(@RequestParam @NotBlank final String text) {
        return textEmbeddingService.embed(text);
    }
}
