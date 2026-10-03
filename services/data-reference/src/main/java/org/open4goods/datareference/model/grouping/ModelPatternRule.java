package org.open4goods.datareference.model.grouping;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.RuleVersion;

/**
 * Git-authored, reviewed regex rule confirming a {@code FAMILY} group within
 * one canonical brand and O4G class.
 *
 * <p>Distinct from the unreviewed common-prefix similarity used for candidate
 * search: a {@code ModelPatternRule} only exists in this registry because it
 * went through Git review on merge, so every rule this type can even
 * construct is, by definition, the reviewed branch of ADR-0010's family
 * grouping. Pattern matching never produces a bare candidate -- an engine
 * matching this rule confirms a {@link GroupType#FAMILY} {@link GroupId}
 * outright; a weaker, unreviewed similarity signal is a separate mechanism
 * that must never be expressed as a {@code ModelPatternRule}.
 *
 * <p>The compact constructor compiles the pattern, rejects a pattern with a
 * nested-quantifier shape (see {@link NestedQuantifierGuard}), requires a
 * named {@code family} capture group, and replays every example and
 * counterexample so a rule that cannot prove itself against its own fixtures
 * never enters the registry. That rejection is a narrow structural check, not
 * a general ReDoS guarantee: {@link ModelPatternRuleRegistry} additionally
 * bounds the time any match against untrusted provider text may take, which
 * is the only guard that covers every exponential shape.
 *
 * @param version effective, monotonic rule coordinate
 * @param canonicalBrand normalized brand slug this rule is scoped to
 * @param canonicalClass O4G class this rule is scoped to
 * @param pattern regex source carrying a named {@code family} capture group
 * @param examples raw provider model texts that must match and yield a
 *     non-blank family capture
 * @param counterexamples raw provider model texts that must never yield a
 *     non-blank family capture, guarding against an overly broad pattern
 * @param reviewer identifier of the human who reviewed and approved this rule
 */
public record ModelPatternRule(
        RuleVersion version,
        String canonicalBrand,
        CanonicalClassId canonicalClass,
        String pattern,
        List<String> examples,
        List<String> counterexamples,
        String reviewer) {

    /** Name of the mandatory named capture group carrying the family fragment. */
    public static final String FAMILY_GROUP = "family";

    /** Validates the rule and proves it against its own examples and counterexamples. */
    public ModelPatternRule {
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(canonicalBrand, "canonicalBrand must not be null");
        canonicalBrand = ModelTextNormalizer.normalize(canonicalBrand);
        if (canonicalBrand.isEmpty()) {
            throw new ModelPatternRuleValidationException("canonicalBrand must carry alphanumeric content");
        }
        Objects.requireNonNull(canonicalClass, "canonicalClass must not be null");
        Objects.requireNonNull(pattern, "pattern must not be null");
        if (pattern.isBlank()) {
            throw new ModelPatternRuleValidationException("pattern must not be blank");
        }
        examples = List.copyOf(Objects.requireNonNull(examples, "examples must not be null"));
        if (examples.isEmpty()) {
            throw new ModelPatternRuleValidationException("a pattern rule must carry at least one example: " + version);
        }
        counterexamples = List.copyOf(Objects.requireNonNull(counterexamples, "counterexamples must not be null"));
        Objects.requireNonNull(reviewer, "reviewer must not be null");
        reviewer = reviewer.trim();
        if (reviewer.isBlank()) {
            throw new ModelPatternRuleValidationException("reviewer must not be blank: " + version);
        }

        if (NestedQuantifierGuard.hasNestedQuantifier(pattern)) {
            throw new ModelPatternRuleValidationException(
                    "pattern rejected for a nested-quantifier shape known to cause catastrophic backtracking: "
                            + version + " -> " + pattern);
        }
        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern);
        } catch (PatternSyntaxException exception) {
            throw new ModelPatternRuleValidationException(
                    "invalid pattern for rule " + version + ": " + exception.getMessage(), exception);
        }
        if (!compiled.namedGroups().containsKey(FAMILY_GROUP)) {
            throw new ModelPatternRuleValidationException(
                    "pattern must declare a named '" + FAMILY_GROUP + "' capture group: " + version);
        }
        for (String example : examples) {
            String family = family(compiled, example);
            if (family == null) {
                throw new ModelPatternRuleValidationException(
                        "example does not match pattern for rule " + version + ": " + example);
            }
            if (family.isBlank()) {
                throw new ModelPatternRuleValidationException(
                        "pattern produces an empty family capture for example in rule " + version + ": " + example);
            }
        }
        for (String counterexample : counterexamples) {
            String family = family(compiled, counterexample);
            if (family != null && !family.isBlank()) {
                throw new ModelPatternRuleValidationException(
                        "pattern incorrectly matches counterexample for rule " + version + ": " + counterexample);
            }
        }
    }

    /**
     * Matches raw provider model text against this rule's compiled pattern.
     *
     * @param compiled this rule's compiled pattern
     * @param text raw provider model text
     * @return the captured family fragment, or {@code null} when the pattern
     *     does not match
     */
    private static String family(Pattern compiled, String text) {
        Matcher matcher = compiled.matcher(text);
        return matcher.find() ? matcher.group(FAMILY_GROUP) : null;
    }
}
