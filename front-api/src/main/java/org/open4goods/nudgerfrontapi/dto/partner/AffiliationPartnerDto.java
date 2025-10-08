package org.open4goods.nudgerfrontapi.dto.partner;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object representing an affiliation partner exposed to the
 * frontend. Contains additional computed asset URLs consumed by the Nuxt
 * application.
 */
public record AffiliationPartnerDto(
        @Schema(description = "Stable identifier of the partner.", example = "leroy-merlin")
        String id,

        @Schema(description = "Human readable partner name.", example = "Leroy Merlin")
        String name,

        @Schema(description = "Landing page URL used for affiliation redirection.",
                example = "https://track.example.com/redirect")
        String affiliationLink,

        @Schema(description = "Public portal URL of the partner.",
                example = "https://www.leroymerlin.fr")
        String portalUrl,

        @Schema(description = "Computed logo URL served by the front API.",
                example = "https://cdn.open4goods.org/logo/Leroy Merlin", format = "uri")
        String logoUrl,

        @Schema(description = "Computed favicon URL served by the front API.",
                example = "https://cdn.open4goods.org/favicon?url=Leroy Merlin", format = "uri")
        String faviconUrl,

        @Schema(description = "ISO 3166-1 alpha-2 country codes where the partner operates.",
                example = "[\"FR\", \"BE\"]")
        List<String> countryCodes
) {
}

