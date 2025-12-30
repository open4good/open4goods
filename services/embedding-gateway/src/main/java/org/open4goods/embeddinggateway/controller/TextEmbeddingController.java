package org.open4goods.embeddinggateway.controller;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jakarta.validation.Valid;

import org.open4goods.commons.services.TextEmbeddingService;
import org.open4goods.embeddinggateway.config.EmbeddingGatewayProperties;
import org.open4goods.embeddinggateway.dto.EmbeddingResponse;
import org.open4goods.embeddinggateway.dto.TextEmbeddingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing text embedding generation.
 */
@RestController
@RequestMapping("/embeddings/text")
@Validated
public class TextEmbeddingController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TextEmbeddingController.class);

    private final TextEmbeddingService textEmbeddingService;
    private final Semaphore semaphore;
    private final Duration timeout;

    public TextEmbeddingController(TextEmbeddingService textEmbeddingService, EmbeddingGatewayProperties properties)
    {
        this.textEmbeddingService = textEmbeddingService;
        this.timeout = properties.getTextRequestTimeout();
        this.semaphore = new Semaphore(properties.getMaxConcurrentTextRequests());
    }

    @PostMapping
    public ResponseEntity<EmbeddingResponse> embed(@Valid @RequestBody TextEmbeddingRequest request)
            throws InterruptedException
    {
        if (!semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS))
        {
            LOGGER.warn("Rejecting text embedding request because concurrency limit is reached");
            return ResponseEntity.status(429).build();
        }

        try
        {
            float[] embedding = textEmbeddingService.embed(request.text());
            return ResponseEntity.ok(new EmbeddingResponse(embedding));
        }
        finally
        {
            semaphore.release();
        }
    }
}
