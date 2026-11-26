package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A source contributing to the product, with the attribution it states (GOU-2302).
 * <p>
 * Listed once per product in {@link ProductDatasourcesDto#sources()}; the attribute sourcing
 * ({@link ProductSourcedAttributeDto#datasourceName()}) references it by {@link #name()} instead of repeating it.
 * The licence fields are {@code null} when the source does not state them: they are never guessed.
 * </p>
 *
 * @param name        datasource name, the key used by {@code datasourceCodes} and the attribute sourcing
 * @param portalUrl   portal URL of the source, or {@code null} when unknown
 * @param license     licence under which the source's data is reused, or {@code null} when not stated
 * @param licenseUrl  URL of the licence text, or {@code null} when not stated
 * @param attribution text crediting the source, or {@code null} when not stated
 */
public record ProductSourceDto(
        @Schema(description = "Datasource name, as used by datasourceCodes and by the attribute sourcing datasourceName",
                example = "fr-indice-durabilite")
        String name,
        @Schema(description = "Portal URL of the source, absent when unknown", format = "uri", nullable = true,
                example = "https://www.data.gouv.fr/fr/datasets/indice-de-durabilite/")
        String portalUrl,
        @Schema(description = "Licence under which the data of the source is reused, absent when the source does not "
                + "state one", nullable = true, example = "Licence Ouverte 2.0")
        String license,
        @Schema(description = "URL of the licence text, absent when the source does not state one", format = "uri",
                nullable = true, example = "https://www.etalab.gouv.fr/licence-ouverte-open-licence/")
        String licenseUrl,
        @Schema(description = "Text crediting the source, absent when the source does not state one", nullable = true,
                example = "Producteurs, via data.gouv.fr")
        String attribution
) {
}
