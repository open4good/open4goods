package org.open4goods.embeddinggateway.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jakarta.validation.Valid;

import org.open4goods.embeddinggateway.config.EmbeddingGatewayProperties;
import org.open4goods.embeddinggateway.dto.EmbeddingResponse;
import org.open4goods.embeddinggateway.dto.ImageEmbeddingRequest;
import org.open4goods.embeddinggateway.service.ImageEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing image embeddings based on remote URLs.
 */
@RestController
@RequestMapping("/embeddings/image")
@Validated
public class ImageEmbeddingController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEmbeddingController.class);

    private final ImageEmbeddingService imageEmbeddingService;
    private final Semaphore semaphore;
    private final Duration timeout;

    public ImageEmbeddingController(ImageEmbeddingService imageEmbeddingService, EmbeddingGatewayProperties properties)
    {
        this.imageEmbeddingService = imageEmbeddingService;
        this.timeout = properties.getImageRequestTimeout();
        this.semaphore = new Semaphore(properties.getMaxConcurrentImageRequests());
    }

    @PostMapping
    public ResponseEntity<EmbeddingResponse> embed(@Valid @RequestBody ImageEmbeddingRequest request)
            throws Exception
    {
        if (!semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS))
        {
            LOGGER.warn("Rejecting image embedding request because concurrency limit is reached");
            return ResponseEntity.status(429).build();
        }

        try
        {
            URL imageUrl = toUrl(request.url());
            float[] embedding = imageEmbeddingService.embed(imageUrl);
            return ResponseEntity.ok(new EmbeddingResponse(embedding));
        }
        finally
        {
            semaphore.release();
        }
    }

    private URL toUrl(String url) throws MalformedURLException
    {
        return new URL(url);
    }
}
