package org.open4goods.services.prompt.dto;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an asynchronous batch prompt response.
 *
 * @param <T> the type of the response body
 */
public record BatchPromptResponse<T>(
        String jobId,
        CompletableFuture<PromptResponse<T>> futureResponse
) { }
