package org.open4goods.nudgerfrontapi.dto;

import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Minimal representation of a blog post.
 */
public record BlogPostDto(
        @Schema(description = "URL slug of the post", example = "new-feature")
        String slug,

        @Schema(description = "Post title", example = "Introducing a new feature")
        String title,

        @Schema(description = "IETF BCP 47 language tag", example = "en")
        String locale,

        @Schema(description = "Short summary", example = "Highlights of our update")
        String summary,

        @Schema(description = "Full markdown body")
        String body,

        @Schema(description = "Creation date in epoch ms", example = "1690972800000")
        Date created) {
}
