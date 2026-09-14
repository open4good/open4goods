package org.open4goods.datareference.model.projection;

import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.RuleVersion;

/**
 * Lexical fields supplied to the consumer-search projection.
 *
 * @param ruleVersion version of the tokenization and field-selection rules
 * @param lexicalTerms ordered, normalized terms; vector or embedding fields are deliberately absent
 */
public record SearchSummary(RuleVersion ruleVersion, List<String> lexicalTerms) {

    /** Validates non-blank, deterministic lexical terms. */
    public SearchSummary {
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        lexicalTerms = List.copyOf(Objects.requireNonNull(lexicalTerms, "lexicalTerms must not be null"));
        if (lexicalTerms.stream().anyMatch(term -> term == null || term.isBlank())) {
            throw new IllegalArgumentException("lexical terms must not contain blanks");
        }
    }
}
