package org.open4goods.nudgerfrontapi.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload submitted by the contact form.
 */
public record ContactRequestDto(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "Full name of the person submitting the form.", example = "Jean Dupont")
        String name,

        @NotBlank
        @Email
        @Schema(description = "Email address of the sender.", example = "jean.dupont@example.com", format = "email")
        String email,

        @NotBlank
        @Size(max = 5_000)
        @Schema(description = "Message written by the sender.", example = "Bonjour, j'aimerais en savoir plus sur vos actions.")
        String message,

        @NotBlank
        @JsonProperty("h-captcha-response")
        @Schema(name = "h-captcha-response", description = "Token returned by the hCaptcha widget.",
                example = "10000000-aaaa-bbbb-cccc-000000000001")
        String captchaResponse) {
}
