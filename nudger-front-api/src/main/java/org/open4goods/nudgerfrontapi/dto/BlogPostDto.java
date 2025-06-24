package org.open4goods.nudgerfrontapi.dto;

import java.util.Date;

/**
 * Minimal representation of a blog post.
 */
public record BlogPostDto(String slug,
                          String title,
                          String locale,
                          String summary,
                          String body,
                          Date created) {
}
