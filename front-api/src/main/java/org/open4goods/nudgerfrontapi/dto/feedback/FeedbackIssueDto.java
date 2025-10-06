package org.open4goods.nudgerfrontapi.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight representation of a feedback issue enriched with the local vote count.
 */
public record FeedbackIssueDto(
        @Schema(description = "Identifiant technique de l'issue (valeur GitHub).", example = "128")
        String id,

        @Schema(description = "Numéro séquentiel de l'issue sur GitHub.", example = "128")
        int number,

        @Schema(description = "Titre de l'issue.", example = "Ajouter un filtre par prix")
        String title,

        @Schema(description = "Lien public vers l'issue GitHub.",
                example = "https://github.com/open4good/open4goods/issues/128", format = "uri")
        String url,

        @Schema(description = "Nombre total de votes enregistrés pour cette issue.", example = "12")
        int votes) {
}
