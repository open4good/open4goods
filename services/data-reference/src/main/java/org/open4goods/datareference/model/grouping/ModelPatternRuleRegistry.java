package org.open4goods.datareference.model.grouping;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.datareference.model.CanonicalClassId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable, compiled lookup over Git-authored {@link ModelPatternRule}s,
 * scoped by canonical brand and O4G class.
 *
 * <p>Rejects two active rules in the same scope that are ambiguous: if either
 * rule's own examples also produce a non-blank family capture under the
 * other rule's pattern, an engine matching real provider text could pick
 * either one depending on rule order, which is exactly the silent
 * non-determinism ADR-0010 rules out for a confirmed {@code FAMILY} group.
 *
 * <p>{@link ModelPatternRule}'s own {@link NestedQuantifierGuard} check at
 * authoring time only rejects one exponential shape, nested quantifiers; it
 * cannot prove a pattern safe against every ReDoS shape (e.g. overlapping
 * alternation such as {@code (a|a)+}). Matching here against raw,
 * unbounded provider text is therefore itself time-bounded: a match that runs
 * past {@link #MATCH_TIMEOUT_NANOS} is aborted and treated as no match,
 * instead of being allowed to run unbounded.
 */
public final class ModelPatternRuleRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ModelPatternRuleRegistry.class);

    private static final long MATCH_TIMEOUT_NANOS = Duration.ofMillis(200).toNanos();

    private final Map<Scope, List<CompiledRule>> rulesByScope;

    /**
     * Compiles and cross-validates the authored rule set.
     *
     * @param authoredRules rules read from the versioned registry
     */
    public ModelPatternRuleRegistry(List<ModelPatternRule> authoredRules) {
        Objects.requireNonNull(authoredRules, "authoredRules must not be null");
        Map<Scope, List<CompiledRule>> indexed = new LinkedHashMap<>();
        for (ModelPatternRule rule : authoredRules) {
            Objects.requireNonNull(rule, "authoredRules must not contain null");
            Scope scope = new Scope(rule.canonicalBrand(), rule.canonicalClass());
            CompiledRule compiledRule = new CompiledRule(rule, Pattern.compile(rule.pattern()));
            indexed.computeIfAbsent(scope, unused -> new ArrayList<>()).add(compiledRule);
        }
        indexed.forEach((scope, rules) -> validateNoAmbiguousOverlap(scope, rules));
        Map<Scope, List<CompiledRule>> frozen = new LinkedHashMap<>();
        indexed.forEach((scope, rules) -> frozen.put(scope, List.copyOf(rules)));
        rulesByScope = Map.copyOf(frozen);
    }

    /**
     * Matches raw provider model text against every reviewed rule scoped to the
     * given brand and class, in authored order.
     *
     * @param canonicalBrand brand text, normalized the same way as a rule's own
     *     {@code canonicalBrand}
     * @param canonicalClass confirmed O4G class
     * @param modelText raw provider model text
     * @return the confirmed {@code FAMILY} group id, or empty when no rule in
     *     scope matches
     */
    public Optional<GroupId> matchFamily(String canonicalBrand, CanonicalClassId canonicalClass, String modelText) {
        Objects.requireNonNull(canonicalClass, "canonicalClass must not be null");
        Objects.requireNonNull(modelText, "modelText must not be null");
        String normalizedBrand = ModelTextNormalizer.normalize(Objects.requireNonNull(canonicalBrand, "canonicalBrand must not be null"));
        List<CompiledRule> candidates = rulesByScope.get(new Scope(normalizedBrand, canonicalClass));
        if (candidates == null) {
            return Optional.empty();
        }
        for (CompiledRule candidate : candidates) {
            Optional<String> family = candidate.family(modelText);
            if (family.isPresent()) {
                String slug = ModelTextNormalizer.joinSlug(canonicalClass.slug(), normalizedBrand,
                        ModelTextNormalizer.normalize(family.get()));
                return Optional.of(new GroupId(GroupType.FAMILY, slug));
            }
        }
        return Optional.empty();
    }

    private static void validateNoAmbiguousOverlap(Scope scope, List<CompiledRule> rules) {
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                CompiledRule first = rules.get(i);
                CompiledRule second = rules.get(j);
                checkOverlap(scope, first, second);
                checkOverlap(scope, second, first);
            }
        }
    }

    private static void checkOverlap(Scope scope, CompiledRule owner, CompiledRule other) {
        for (String example : owner.rule.examples()) {
            if (other.family(example).isPresent()) {
                throw new ModelPatternRuleValidationException(
                        "ambiguous overlapping pattern rules for " + scope + ": " + owner.rule.version()
                                + " and " + other.rule.version() + " both match example: " + example);
            }
        }
    }

    private record Scope(String canonicalBrand, CanonicalClassId canonicalClass) {

        @Override
        public String toString() {
            return canonicalClass.externalForm() + "/" + canonicalBrand;
        }
    }

    private record CompiledRule(ModelPatternRule rule, Pattern compiled) {

        Optional<String> family(String text) {
            Matcher matcher = compiled.matcher(new TimeBoundedCharSequence(text, MATCH_TIMEOUT_NANOS));
            try {
                if (!matcher.find()) {
                    return Optional.empty();
                }
            } catch (RegexMatchTimeoutException timeout) {
                LOG.warn("Pattern match for rule {} exceeded its {} ms time budget and was aborted; treating as no match",
                        rule.version(), Duration.ofNanos(MATCH_TIMEOUT_NANOS).toMillis());
                return Optional.empty();
            }
            String family = matcher.group(ModelPatternRule.FAMILY_GROUP);
            return family == null || family.isBlank() ? Optional.empty() : Optional.of(family);
        }
    }
}
