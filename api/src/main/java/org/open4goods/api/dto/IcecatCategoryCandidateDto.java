package org.open4goods.api.dto;

import java.util.List;

/**
 * Candidate Icecat category for a vertical-to-Icecat taxonomy mapping workflow.
 */
public record IcecatCategoryCandidateDto(
        Integer id,
        String englishName,
        Integer parentId,
        Integer score,
        List<String> langNames,
        String source
) {}
