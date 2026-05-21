package org.open4goods.api.dto;

/**
 * Candidate Google Product Taxonomy entry returned by the referential resolution endpoint.
 *
 * @param id         numeric Google taxonomy identifier
 * @param path       full taxonomy path (e.g. "Home &amp; Garden > ... > Air Conditioners")
 * @param confidence score in [0.0, 1.0] derived from keyword overlap with the vertical names
 * @param source     how the candidate was found (e.g. "keyword-match:air conditioner")
 */
public record GoogleCandidateDto(Integer id, String path, double confidence, String source)
{
}
