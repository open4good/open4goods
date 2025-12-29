package org.open4goods.nudgerfrontapi.dto.icecat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for Icecat retailer category data exposed to the frontend.
 *
 * @param id               the Icecat category ID
 * @param name             the category name
 * @param parentId         the parent category ID (null for root categories)
 * @param level            the hierarchy level (0 = root)
 * @param description      the category description
 * @param thumbnailUrl     the thumbnail image URL
 */
@Schema(description = "Icecat retailer category information")
public record IcecatRetailerCategoryDto(

        @Schema(description = "Icecat category ID", example = "1")
        Long id,

        @Schema(description = "Category name", example = "Electronics")
        String name,

        @Schema(description = "Parent category ID (null for root)", example = "null")
        Long parentId,

        @Schema(description = "Hierarchy level (0 = root)", example = "0")
        Integer level,

        @Schema(description = "Category description")
        String description,

        @Schema(description = "Thumbnail image URL")
        String thumbnailUrl

) {}
