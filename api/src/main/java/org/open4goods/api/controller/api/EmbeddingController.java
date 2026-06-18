package org.open4goods.api.controller.api;

import org.open4goods.embedding.service.TextEmbeddingService;
import org.open4goods.model.RolesConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoint for generating semantic text embeddings, used by {@code front-api} for search.
 */
@RestController
@ConditionalOnBean(TextEmbeddingService.class)
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Tag(name = "Embedding", description = "Generate dense vector embeddings for semantic (neural) search. "
        + "Only available when a TextEmbeddingService bean is configured.")
public class EmbeddingController {

    private final TextEmbeddingService textEmbeddingService;

    public EmbeddingController(TextEmbeddingService textEmbeddingService) {
        this.textEmbeddingService = textEmbeddingService;
    }

    @GetMapping(path = "/embedding")
    @Operation(
            summary = "Generate a semantic embedding for a text",
            description = "Passes the input text through the configured embedding model and returns the resulting "
                    + "float vector. The vector dimensionality depends on the model in use. "
                    + "The front-api embedding proxy calls this endpoint to embed user search queries "
                    + "before performing k-NN retrieval in Elasticsearch.")
    @ApiResponse(responseCode = "200", description = "Float array representing the dense vector embedding")
    public float[] getEmbedding(
            @Parameter(description = "Text to embed — should be a product name, category label or search query", required = true)
            @RequestParam @NotBlank final String text) {
        return textEmbeddingService.embed(text);
    }
}
