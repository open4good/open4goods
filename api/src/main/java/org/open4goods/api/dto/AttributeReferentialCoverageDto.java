package org.open4goods.api.dto;

import java.util.List;

/**
 * Per-attribute summary of the cross-referential coverage for a given vertical.
 *
 * @param attributeKey  Nudger attribute key (e.g. "HEIGHT")
 * @param coveredTaxonomies  taxonomies with at least one mapping (e.g. ["icecat", "etim"])
 * @param missingTaxonomies  taxonomies still unmapped (subset of the 4 supported ones)
 * @param icecatFeatureIds   Icecat feature IDs currently bound to the attribute
 * @param eprelFeatureNames  EPREL feature names currently bound to the attribute
 * @param etimFeatureIds     ETIM feature IDs currently bound to the attribute
 * @param wikidataPids       Wikidata property IDs currently bound to the attribute
 */
public record AttributeReferentialCoverageDto(
        String attributeKey,
        List<String> coveredTaxonomies,
        List<String> missingTaxonomies,
        List<Integer> icecatFeatureIds,
        List<String> eprelFeatureNames,
        List<String> etimFeatureIds,
        List<String> wikidataPids)
{
}
