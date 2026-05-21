package org.open4goods.api.dto;

/**
 * Candidate Wikidata entity returned by the referential resolution endpoint.
 *
 * @param qid         Wikidata Q-identifier (e.g. "Q174488")
 * @param label       English label from Wikidata
 * @param description English description from Wikidata
 * @param confidence  score in [0.0, 1.0] derived from label similarity to vertical names
 */
public record WikidataCandidateDto(String qid, String label, String description, double confidence)
{
}
