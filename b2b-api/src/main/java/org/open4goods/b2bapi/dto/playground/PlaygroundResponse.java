package org.open4goods.b2bapi.dto.playground;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Response structure for the playground price API proxy")
public record PlaygroundResponse(
        @Schema(description = "Masked request details")
        PlaygroundRequestDetails request,

        @Schema(description = "API response details")
        PlaygroundResponseDetails response,

        @Schema(description = "Metering outcome")
        PlaygroundMeteringDetails metering
) {
    public record PlaygroundRequestDetails(
            @Schema(description = "HTTP Method", example = "GET")
            String method,

            @Schema(description = "HTTP Request path", example = "/api/v1/products/0885909950805/price?language=en")
            String path,

            @Schema(description = "Masked HTTP headers")
            Map<String, String> headers
    ) {}

    public record PlaygroundResponseDetails(
            @Schema(description = "HTTP status code", example = "200")
            int status,

            @Schema(description = "HTTP headers")
            Map<String, String> headers,

            @Schema(description = "HTTP response body payload")
            Object body
    ) {}

    public record PlaygroundMeteringDetails(
            @Schema(description = "Whether the request was billed", example = "true")
            boolean billable,

            @Schema(description = "Credits consumed by this request", example = "5")
            long creditsConsumed,

            @Schema(description = "Durable/Redis credits remaining for the organization", example = "2495")
            long creditsRemaining,

            @Schema(description = "Metering reason or outcome code", example = "fresh-offer")
            String reason
    ) {}
}
