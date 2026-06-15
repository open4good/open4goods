package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Credit balance breakdown for an organization")
public record B2bBalanceResponseDto(
        @Schema(description = "Total active credit balance", example = "12500")
        long creditsRemaining,

        @Schema(description = "Breakdown of individual active credit buckets")
        List<B2bBucketDetailDto> buckets
) {
    @Schema(description = "Detail of a specific credit bucket")
    public record B2bBucketDetailDto(
            @Schema(description = "Bucket UUID", example = "6fa8b9cc-c4d6-4444-ac6b-9c7161b36fa2")
            String id,

            @Schema(description = "Source kind of the credits", example = "SUBSCRIPTION")
            String kind,

            @Schema(description = "Initial credit grant amount", example = "12000")
            long creditsTotal,

            @Schema(description = "Remaining credits in this bucket", example = "10000")
            long creditsRemaining,

            @Schema(description = "Expiration timestamp, if any", example = "2026-07-15T18:00:00Z")
            Instant expiresAt,

            @Schema(description = "Catalog reference ID associated with the plan", example = "starter")
            String catalogId
    ) {}
}
