package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a product PDF resource exposed to the frontend.
 */
public record ProductPdfDto(
        @Schema(description = "Resource URL", example = "https://cdn.example.org/pdfs/product_abc.pdf")
        String url,
        @Schema(description = "Detected MIME type", example = "application/pdf")
        String mimeType,
        @Schema(description = "Last update timestamp in epoch milliseconds")
        Long timeStamp,
        @Schema(description = "Cache key used by the media pipeline")
        String cacheKey,
        @Schema(description = "Whether the resource has been evicted from the catalogue")
        boolean evicted,
        @Schema(description = "Whether the resource has been processed")
        boolean processed,
        @Schema(description = "Status assigned by the ingestion pipeline", nullable = true)
        ResourceStatus status,
        @Schema(description = "Size in bytes when known", example = "512000")
        Long fileSize,
        @Schema(description = "File name extracted from the URL", example = "product")
        String fileName,
        @Schema(description = "File extension", example = "pdf")
        String extension,
        @Schema(description = "MD5 checksum when available")
        String md5,
        @Schema(description = "Resource type")
        ResourceType resourceType,
        @Schema(description = "Group identifier used to cluster similar resources", nullable = true)
        Integer group,
        @Schema(description = "Datasource providing the resource", example = "manufacturer")
        String datasourceName,
        @Schema(description = "Tags describing the resource")
        Set<String> tags,
        @Schema(description = "Hard tags describing the resource nature")
        Set<ResourceTag> hardTags,
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
