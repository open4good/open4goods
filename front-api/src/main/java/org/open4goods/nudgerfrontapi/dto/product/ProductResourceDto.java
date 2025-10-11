package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO mirroring {@link org.open4goods.model.resource.Resource} content for API consumption.
 */
public record ProductResourceDto(
        @Schema(description = "Resource URL", example = "https://cdn.example.org/images/1.jpg")
        String url,
        @Schema(description = "Detected MIME type", example = "image/jpeg")
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
        @Schema(description = "Size in bytes when known", example = "102400")
        Long fileSize,
        @Schema(description = "File name extracted from the URL", example = "image.jpg")
        String fileName,
        @Schema(description = "File extension", example = "jpg")
        String extension,
        @Schema(description = "MD5 checksum when available")
        String md5,
        @Schema(description = "Resource type")
        ResourceType resourceType,
        @Schema(description = "Additional image metadata", nullable = true)
        ProductResourceImageInfoDto imageInfo,
        @Schema(description = "Additional PDF metadata", nullable = true)
        ProductResourcePdfInfoDto pdfInfo,
        @Schema(description = "Group identifier used to cluster similar images", nullable = true)
        Integer group,
        @Schema(description = "Datasource providing the resource", example = "amazon.fr")
        String datasourceName,
        @Schema(description = "Tags describing the resource")
        Set<String> tags,
        @Schema(description = "Hard tags describing the resource nature")
        Set<ResourceTag> hardTags
) {
}
