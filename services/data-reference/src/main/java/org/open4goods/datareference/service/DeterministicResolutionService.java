package org.open4goods.datareference.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.normalization.NormalizationRequest;
import org.open4goods.datareference.model.normalization.NormalizationResult;
import org.open4goods.datareference.model.normalization.NormalizationStatus;
import org.open4goods.datareference.model.resolution.Correction;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolutionRule;
import org.open4goods.datareference.model.resolution.ResolutionRuleRegistry;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.CorrectionsPort;
import org.open4goods.datareference.port.NormalizationPort;
import org.open4goods.datareference.port.ResolutionPort;

/**
 * Resolves source evidence only after policy, lifecycle and attachment gates.
 *
 * <p>Every ordering coordinate is explicit and stable. In particular, source
 * arrival order is never read, and sources absent from a per-field rule are
 * excluded before they can affect a conflict flag.
 */
public final class DeterministicResolutionService implements ResolutionPort {

    private final SourceUsagePolicyRegistry policies;
    private final NormalizationPort normalization;
    private final CorrectionsPort corrections;
    private final ResolutionRuleRegistry rules;
    private final Function<SourceRecordHead, Locale> sourceLocale;
    private final Function<SourceRecordHead, String> sourceLanguage;

    /**
     * Creates a resolver using root locale and no provider language when an
     * adapter has no more specific normalization coordinates.
     */
    public DeterministicResolutionService(SourceUsagePolicyRegistry policies, NormalizationPort normalization,
            CorrectionsPort corrections, ResolutionRuleRegistry rules) {
        this(policies, normalization, corrections, rules, head -> Locale.ROOT, head -> null);
    }

    /** Creates a resolver with explicit source normalization coordinates. */
    public DeterministicResolutionService(SourceUsagePolicyRegistry policies, NormalizationPort normalization,
            CorrectionsPort corrections, ResolutionRuleRegistry rules,
            Function<SourceRecordHead, Locale> sourceLocale, Function<SourceRecordHead, String> sourceLanguage) {
        this.policies = Objects.requireNonNull(policies, "policies must not be null");
        this.normalization = Objects.requireNonNull(normalization, "normalization must not be null");
        this.corrections = Objects.requireNonNull(corrections, "corrections must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.sourceLocale = Objects.requireNonNull(sourceLocale, "sourceLocale must not be null");
        this.sourceLanguage = Objects.requireNonNull(sourceLanguage, "sourceLanguage must not be null");
    }

    @Override
    public List<ResolvedValue> resolve(Gtin gtin, List<SourceRecordHead> heads, ProjectionSurface surface, Instant at) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(heads, "heads must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(at, "at must not be null");

        Map<CanonicalAttributeId, List<Candidate>> candidates = eligibleCandidates(gtin, heads, surface, at);
        corrections.findByGtin(gtin).stream()
                .filter(correction -> correction.appliesTo(surface, at))
                .forEach(correction -> candidates.computeIfAbsent(correction.attribute(), ignored -> new ArrayList<>())
                        .add(Candidate.fromCorrection(correction)));

        return candidates.entrySet().stream()
                .map(entry -> resolveField(entry.getKey(), surface, entry.getValue()))
                .flatMap(result -> result.stream())
                .sorted(Comparator.comparing(resolved -> resolved.attribute().externalForm()))
                .toList();
    }

    private Map<CanonicalAttributeId, List<Candidate>> eligibleCandidates(
            Gtin gtin, List<SourceRecordHead> heads, ProjectionSurface surface, Instant at) {
        Map<CanonicalAttributeId, List<Candidate>> candidates = new java.util.HashMap<>();
        for (SourceRecordHead head : heads) {
            if (head == null || !head.isUsableAt(at)) {
                continue;
            }
            GtinLink link = head.gtinLinks().stream().filter(candidate -> gtin.equals(candidate.gtin()))
                    .findFirst().orElse(null);
            if (link == null || link.confidence() == GtinMatchConfidence.UNVERIFIED) {
                continue;
            }
            for (SourceAssertion assertion : head.assertions()) {
                if (!policies.allows(head.key().sourceId(), head.usagePolicyRef(), assertion.contentType(), surface, at)) {
                    continue;
                }
                NormalizationResult normalized = normalization.normalize(new NormalizationRequest(head.key().sourceId(), assertion,
                        sourceLocale.apply(head), sourceLanguage.apply(head), at.atZone(ZoneOffset.UTC).toLocalDate()));
                if (normalized.status() != NormalizationStatus.SUCCESS) {
                    continue;
                }
                var value = normalized.value();
                ResolutionRule rule = rules.find(value.attribute(), surface).orElse(null);
                if (rule == null || rule.rankOf(head.key().sourceId()) < 0) {
                    continue;
                }
                candidates.computeIfAbsent(value.attribute(), ignored -> new ArrayList<>())
                        .add(Candidate.source(value.sourceAssertionId(), value.value(), head, link.confidence(), rule));
            }
        }
        return candidates;
    }

    private java.util.Optional<ResolvedValue> resolveField(
            CanonicalAttributeId attribute, ProjectionSurface surface, List<Candidate> fieldCandidates) {
        ResolutionRule rule = rules.find(attribute, surface).orElse(null);
        if (rule == null) {
            return java.util.Optional.empty();
        }
        List<Candidate> candidates = fieldCandidates.stream()
                .filter(candidate -> candidate.correction || candidate.rule.equals(rule))
                .sorted(Candidate.ORDER)
                .toList();
        if (candidates.isEmpty()) {
            return java.util.Optional.empty();
        }
        Candidate winner = candidates.getFirst();
        List<AssertionId> ids = candidates.stream().map(Candidate::assertionId).distinct().sorted(Comparator.comparing(AssertionId::value))
                .toList();
        boolean conflict = candidates.stream().map(Candidate::value).distinct().count() > 1;
        return java.util.Optional.of(new ResolvedValue(attribute, winner.value(), winner.assertionId(), ids,
                rule.version(), reasonForWinner(winner, candidates), conflict));
    }

    /**
     * Reports the first ordering coordinate that made the selected candidate win.
     *
     * <p>The result is intentionally derived from the already eligible candidates.
     * A rejected or surface-disallowed assertion must not affect either the winning
     * value or its explanation.
     */
    private ResolutionReason reasonForWinner(Candidate winner, List<Candidate> candidates) {
        if (winner.correction()) {
            return ResolutionReason.O4G_CORRECTION;
        }
        if (winner.regulatory() && candidates.stream().anyMatch(candidate -> !candidate.regulatory())) {
            return ResolutionReason.SOURCE_AUTHORITY;
        }
        if (candidates.stream().map(Candidate::sourceRank).distinct().count() > 1) {
            return ResolutionReason.CONFIGURED_SOURCE_RANK;
        }
        if (candidates.stream().map(Candidate::confidence).distinct().count() > 1) {
            return ResolutionReason.HIGHER_CONFIDENCE;
        }
        if (candidates.stream().map(Candidate::observedAt).distinct().count() > 1) {
            return ResolutionReason.MORE_RECENT_OBSERVATION;
        }
        return ResolutionReason.STABLE_TIE_BREAK;
    }

    private record Candidate(AssertionId assertionId, org.open4goods.datareference.model.value.CanonicalValue value,
            boolean correction, boolean regulatory, int sourceRank, GtinMatchConfidence confidence,
            Instant observedAt, String stableSourceId, ResolutionRule rule) {

        private static final Comparator<Candidate> ORDER = Comparator.comparing(Candidate::correction).reversed()
                .thenComparing(Comparator.comparing(Candidate::regulatory).reversed())
                .thenComparingInt(Candidate::sourceRank)
                .thenComparing(Candidate::confidence, Comparator.comparingInt(DeterministicResolutionService::confidenceRank).reversed())
                .thenComparing(Candidate::observedAt, Comparator.reverseOrder())
                .thenComparing(Candidate::stableSourceId)
                .thenComparing(candidate -> candidate.assertionId().value());

        static Candidate source(AssertionId assertionId, org.open4goods.datareference.model.value.CanonicalValue value,
                SourceRecordHead head, GtinMatchConfidence confidence, ResolutionRule rule) {
            return new Candidate(assertionId, value, false, head.key().sourceId().equals(rule.regulatoryAuthority()),
                    rule.rankOf(head.key().sourceId()), confidence, head.observedAt(), head.key().sourceId().value(), rule);
        }

        static Candidate fromCorrection(Correction correction) {
            return new Candidate(correction.assertionId(), correction.value(), true, false, -1, GtinMatchConfidence.EXACT,
                    correction.reviewedAt(), "o4g", null);
        }
    }

    private static int confidenceRank(GtinMatchConfidence confidence) {
        return switch (confidence) {
            case EXACT -> 4;
            case STRONG -> 3;
            case WEAK -> 2;
            case UNVERIFIED -> 1;
        };
    }
}
