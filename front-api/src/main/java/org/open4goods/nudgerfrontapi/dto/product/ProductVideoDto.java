package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a product video resource exposed to the frontend.
 */
public record ProductVideoDto(
        @Schema(description = "Resource URL", example = "https://cdn.example.org/videos/product_abc.mp4")
        String url,
        @Schema(description = "Detected MIME type", example = "video/mp4")
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
        @Schema(description = "Size in bytes when known", example = "2048000")
        Long fileSize,
        @Schema(description = "File name extracted from the URL", example = "product")
        String fileName,
        @Schema(description = "File extension", example = "mp4")
        String extension,
        @Schema(description = "MD5 checksum when available")
        String md5,
        @Schema(description = "Resource type")
        ResourceType resourceType,
        @Schema(description = "Group identifier used to cluster similar resources", nullable = true)
        Integer group,
        @Schema(description = "Datasource providing the resource", example = "youtube")
        String datasourceName,
        @Schema(description = "Tags describing the resource")
        Set<String> tags,
        @Schema(description = "Hard tags describing the resource nature")
        Set<ResourceTag> hardTags
) {
}
