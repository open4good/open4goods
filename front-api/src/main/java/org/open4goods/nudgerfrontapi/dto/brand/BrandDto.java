package org.open4goods.nudgerfrontapi.dto.brand;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A brand resolved to its owning company and enriched intelligence.
 *
 * @param brandName canonical brand name
 * @param companyName owning company name
 * @param officialDomains official brand domains
 * @param company enriched company details (manufacturing, scores, x-metas)
 */
@Schema(name = "Brand", description = "A brand resolved to its owning company and enriched intelligence.")
public record BrandDto(
        @Schema(description = "Canonical brand name.", example = "SAMSUNG")
        String brandName,
        @Schema(description = "Owning company name.", example = "Samsung Electronics Co., Ltd.", nullable = true)
        String companyName,
        @Schema(description = "Official brand domains.")
        List<String> officialDomains,
        @Schema(description = "Enriched company details.", nullable = true)
        CompanyDto company) {
}
