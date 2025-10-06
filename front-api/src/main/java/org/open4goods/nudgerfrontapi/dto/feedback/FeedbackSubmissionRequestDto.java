package org.open4goods.nudgerfrontapi.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload used to create a new feedback entry (idea or bug).
 */
public record FeedbackSubmissionRequestDto(
        @NotNull
        @Schema(description = "Category of the feedback to submit.", example = "IDEA")
        FeedbackIssueType type,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Short title summarising the feedback.", example = "Ajouter un filtre par prix")
        String title,

        @NotBlank
        @Size(max = 5000)
        @Schema(description = "Detailed message explaining the bug or idea.",
                example = "Il serait pratique de pouvoir filtrer les offres par prix maximum.")
        String message,

        @Size(max = 2048)
        @Schema(description = "URL depuis laquelle l'utilisateur a soumis le feedback.",
                example = "https://nudger.fr/produits/1234", nullable = true, format = "uri")
        String url,

        @Size(max = 255)
        @Schema(description = "Nom affiché pour l'auteur du feedback.", example = "Jean Dupont", nullable = true)
        String author,

        @NotBlank
        @JsonProperty("h-captcha-response")
        @Schema(name = "h-captcha-response", description = "Jeton retourné par le widget hCaptcha.",
                example = "10000000-aaaa-bbbb-cccc-000000000001")
        String captchaResponse) {
}
