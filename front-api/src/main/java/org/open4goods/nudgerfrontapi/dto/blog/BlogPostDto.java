package org.open4goods.nudgerfrontapi.dto.blog;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing blog post information for the frontend API.
 */
public record BlogPostDto(
        @Schema(description = "URL slug identifying the post", example = "my-first-post")
        String url,
        @Schema(description = "Post title", example = "My first post")
        String title,
        @Schema(description = "Author full name", example = "John Doe", nullable = true)
        String author,
        @Schema(description = "Post summary", example = "Short introduction", nullable = true)
        String summary,
        @Schema(description = "HTML body", nullable = true)
        String body,
        @Schema(description = "Post categories", example = "[\"eco\"]", nullable = true)
        List<String> category,
        @Schema(description = "Cover image URL", example = "/blog/Post/image.jpg", nullable = true)
        String image,
        @Schema(description = "Direct edit link", example = "https://wiki/edit/Blog/Post", nullable = true)
        String editLink,
        @Schema(description = "Creation timestamp ms", example = "1690972800000", nullable = true)
        Long createdMs,
        @Schema(description = "Modification timestamp ms", example = "1690972900000", nullable = true)
        Long modifiedMs
) {}
