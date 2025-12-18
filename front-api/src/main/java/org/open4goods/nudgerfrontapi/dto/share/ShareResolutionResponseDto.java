package org.open4goods.nudgerfrontapi.dto.share;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Snapshot of a share resolution execution.
 */
public record ShareResolutionResponseDto(
        @Schema(description = "Token assigned to the resolution request", example = "0df7ce2f-3f9e-44d5-a0c4-5f3f0f7d31a7")
        String token,
        @Schema(description = "Current status of the resolution", implementation = ShareResolutionStatus.class)
        ShareResolutionStatus status,
        @Schema(description = "Original URL provided by the user", example = "https://shop.example.org/product/fairphone-4")
        String originUrl,
        @Schema(description = "Timestamp when the resolution started", example = "2024-06-05T12:00:00Z")
        Instant startedAt,
        @Schema(description = "Timestamp when the resolution completed", example = "2024-06-05T12:00:02Z", nullable = true)
        Instant resolvedAt,
        @Schema(description = "Best-effort extraction result", implementation = ShareExtractionDto.class, nullable = true)
        ShareExtractionDto extracted,
        @ArraySchema(arraySchema = @Schema(description = "List of matching candidates"),
                schema = @Schema(implementation = ShareCandidateDto.class))
        List<ShareCandidateDto> candidates,
        @Schema(description = "Optional diagnostic message", example = "Resolution timed out after 4 seconds", nullable = true)
        String message
) {
}
