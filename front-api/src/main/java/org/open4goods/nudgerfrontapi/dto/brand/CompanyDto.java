package org.open4goods.nudgerfrontapi.dto.brand;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enriched company behind a brand: identity, manufacturing places, scores and
 * generic sourced facts.
 *
 * @param id company id
 * @param name legal name
 * @param parentCompanyId parent/holding company id, when known
 * @param hq headquarters location
 * @param manufacturing manufacturing sites relevant to the requested category
 * @param scores per-provider ESG / ethics scores
 * @param xmetas generic sourced facts (certifications, controversies, facts, news)
 */
@Schema(name = "Company", description = "Enriched company behind a brand.")
public record CompanyDto(
        @Schema(description = "Company id.", example = "apple-inc")
        String id,
        @Schema(description = "Legal name.", example = "Apple, Inc.")
        String name,
        @Schema(description = "Parent/holding company id.", nullable = true)
        String parentCompanyId,
        @Schema(description = "Headquarters location.", nullable = true)
        CompanyLocationDto hq,
        @Schema(description = "Manufacturing sites relevant to the requested category.")
        List<ManufacturingSiteDto> manufacturing,
        @Schema(description = "Per-provider ESG / ethics scores.")
        List<CompanyScoreDto> scores,
        @Schema(description = "Generic sourced facts.")
        List<XMetaDto> xmetas) {
}
