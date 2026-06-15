package org.open4goods.b2bapi.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response containing the result of a manual credit grant")
public record AdminCreditGrantResponseDto(
        @Schema(description = "ID of the created credit bucket")
        UUID bucketId,
        @Schema(description = "Amount of credits granted")
        long creditsGranted,
        @Schema(description = "Authoritative credit balance of the organization after the grant")
        long durableBalance
) {}
