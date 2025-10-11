package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PDF metadata exported with the resource facet.
 */
public record ProductResourcePdfInfoDto(
        @Schema(description = "Title extracted from the PDF metadata", example = "User manual")
        String metadataTitle,
        @Schema(description = "Title extracted from the PDF content", example = "Operating instructions")
        String extractedTitle,
        @Schema(description = "Number of pages contained in the document", example = "48")
        Integer numberOfPages,
        @Schema(description = "Author declared in the metadata", example = "Example Corp")
        String author,
        @Schema(description = "Subject declared in the metadata")
        String subject,
        @Schema(description = "Keywords declared in the metadata")
        String keywords,
        @Schema(description = "Creation timestamp in epoch milliseconds")
        Long creationDate,
        @Schema(description = "Last modification timestamp in epoch milliseconds")
        Long modificationDate,
        @Schema(description = "Producer declared in the metadata")
        String producer,
        @Schema(description = "Detected language", example = "fr")
        String language,
        @Schema(description = "Confidence score associated with the detected language", example = "0.95")
        Double languageConfidence
) {
}
