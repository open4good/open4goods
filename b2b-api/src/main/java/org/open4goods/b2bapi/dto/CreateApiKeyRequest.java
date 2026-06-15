package org.open4goods.b2bapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a Product Data API key.
 *
 * @param name human-readable key name
 */
public record CreateApiKeyRequest(
        @NotBlank @Size(max = 120) String name) {
}
