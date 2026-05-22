package org.open4goods.api.dto;

/**
 * Candidate Wikidata property returned by the attribute-level reconciliation endpoint.
 *
 * @param pid         Wikidata property identifier (e.g. "P2048")
 * @param label       English label
 * @param description English description
 * @param confidence  score in [0.0, 1.0] derived from label similarity
 */
public record AttributeWikidataPropertyCandidateDto(
        String pid,
        String label,
        String description,
        double confidence)
{
}
