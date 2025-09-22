package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localised textual information.
 */
public record ProductNamesDto(
        @Schema(description = "H1 title expected to align with the requested domainLanguage when localisation is available.")
        String h1Title,
        @Schema(description = "Meta description aligned to the requested domainLanguage once localisation is wired.")
        String metaDescription,
        @Schema(description = "OpenGraph title prepared for domainLanguage-driven localisation.")
        String ogTitle,
        @Schema(description = "OpenGraph description prepared for domainLanguage-driven localisation.")
        String ogDescription,
        @Schema(description = "Twitter title mirroring the requested domainLanguage in the future.")
        String twitterTitle,
        @Schema(description = "Twitter description mirroring the requested domainLanguage in the future.")
        String twitterDescription
) {}
