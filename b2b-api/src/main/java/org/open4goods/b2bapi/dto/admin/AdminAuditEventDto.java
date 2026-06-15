package org.open4goods.b2bapi.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Platform-admin audit log entry")
public record AdminAuditEventDto(
        @Schema(description = "Event UUID")
        UUID id,
        @Schema(description = "Actor user ID who performed the action")
        UUID actorUserId,
        @Schema(description = "Actor user email")
        String actorUserEmail,
        @Schema(description = "Audit action name", example = "CREDIT_MANUAL_GRANT")
        String action,
        @Schema(description = "Target organization ID if applicable")
        UUID targetOrganizationId,
        @Schema(description = "Target organization name if applicable")
        String targetOrganizationName,
        @Schema(description = "Target reference (e.g. key or grant ID)")
        String targetRef,
        @Schema(description = "Additional context details in JSON format")
        Map<String, Object> detail,
        @Schema(description = "Timestamp of event")
        Instant createdAt
) {}
