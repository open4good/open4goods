package org.open4goods.nudgerfrontapi.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a contact form submission.
 */
public record ContactResponseDto(
        @Schema(description = "Indicates whether the submission has been processed successfully.", example = "true")
        boolean success
      ) {
}
