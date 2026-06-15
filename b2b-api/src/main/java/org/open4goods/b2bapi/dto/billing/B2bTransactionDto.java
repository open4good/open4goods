package org.open4goods.b2bapi.dto.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Credit transaction record in the append-only ledger")
public record B2bTransactionDto(
        @Schema(description = "Transaction UUID", example = "5ea8b9cc-c4d6-4444-ac6b-9c7161b36fa1")
        UUID id,

        @Schema(description = "Transaction type (e.g. GRANT, DEBIT, REFUND, EXPIRE)", example = "DEBIT")
        String type,

        @Schema(description = "Credits affected by the transaction", example = "5")
        long credits,

        @Schema(description = "Facet ID associated with a DEBIT/REFUND transaction", example = "product.price")
        String facetId,

        @Schema(description = "GTIN associated with a DEBIT/REFUND transaction", example = "0885909950805")
        String gtin,

        @Schema(description = "Request ID of the operation, prefixed with pdreq_", example = "pdreq_01HF...")
        String requestId,

        @Schema(description = "Additional description or reason", example = "Product price query lookup")
        String note,

        @Schema(description = "Timestamp of transaction", example = "2026-06-15T18:22:25Z")
        Instant createdAt
) {}
