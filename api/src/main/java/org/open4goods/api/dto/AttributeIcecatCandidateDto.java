package org.open4goods.api.dto;

/**
 * Candidate Icecat feature returned by the attribute-level reconciliation endpoint.
 *
 * @param featureId    Icecat feature identifier (e.g. 1464)
 * @param englishName  English label as exported by Icecat
 * @param featureType  Icecat feature data type (e.g. "numerical", "y_n", "dropdown")
 * @param categoryId   Icecat category in which the feature was observed
 * @param categoryName English name of the category
 * @param confidence   score in [0.0, 1.0] derived from name overlap
 * @param matchSource  short tag describing how the candidate was found ("exact-name",
 *                     "synonym-match", "name-overlap")
 */
public record AttributeIcecatCandidateDto(
        Integer featureId,
        String englishName,
        String featureType,
        Integer categoryId,
        String categoryName,
        double confidence,
        String matchSource)
{
}
