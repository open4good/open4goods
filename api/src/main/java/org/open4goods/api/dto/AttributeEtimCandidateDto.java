package org.open4goods.api.dto;

/**
 * Candidate ETIM feature returned by the attribute-level reconciliation endpoint.
 *
 * @param featureId   ETIM feature identifier (e.g. "EF000003")
 * @param featureName English label of the ETIM feature
 * @param classId     ETIM class scoping the feature (e.g. "EC001764")
 * @param className   English label of the ETIM class
 * @param confidence  score in [0.0, 1.0] derived from name overlap
 * @param matchSource short tag describing how the candidate was found
 */
public record AttributeEtimCandidateDto(
        String featureId,
        String featureName,
        String classId,
        String className,
        double confidence,
        String matchSource)
{
}
