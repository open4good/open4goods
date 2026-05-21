package org.open4goods.api.dto;

/**
 * Candidate ETIM (European Technical Information Model) class returned by the
 * referential resolution endpoint.
 *
 * @param classId    ETIM class identifier (e.g. "EC011604")
 * @param className  English class label (e.g. "Portable air conditioner")
 * @param confidence score in [0.0, 1.0] reflecting resolution certainty
 */
public record EtimCandidateDto(String classId, String className, double confidence)
{
}
