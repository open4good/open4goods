package org.open4goods.nudgerfrontapi.localization;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration listing the supported domain languages exposed by the front API.
 * <p>
 * Values follow ISO 639-1 / IETF BCP 47 conventions so clients can safely map
 * them to standard locale tags when localisation is eventually implemented.
 * </p>
 */
@Schema(description = "Domain language hint used to drive localisation of responses.",
        example = "FR",
        allowableValues = {"FR", "EN"})
public enum DomainLanguage {

    /** French content (language tag {@code fr-FR}). */
    FR("fr-FR"),

    /** English content (language tag {@code en-US}). */
    EN("en-US");

    private final String languageTag;

    DomainLanguage(String languageTag) {
        this.languageTag = languageTag;
    }

    /**
     * Return the canonical IETF BCP 47 language tag associated with the enum value.
     *
     * @return language tag such as {@code fr-FR}
     */
    public String languageTag() {
        return languageTag;
    }
}
