package org.open4goods.datareference.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.EvaluationInput;
import org.open4goods.datareference.model.projection.EvaluationRefreshTrigger;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.DomainSliceComposer;
import org.open4goods.datareference.port.EvaluationBridge;
import org.open4goods.datareference.port.OfferSummaryPort;
import org.open4goods.datareference.port.ProjectionWritePort;
import org.open4goods.datareference.port.ResolutionPort;
import org.open4goods.datareference.port.SearchSummaryPort;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * The sole writer that rebuilds a complete, three-surface GTIN projection.
 *
 * <p>Every slice crosses a typed port. In particular the evaluation and search
 * producers receive only resolved values, so they cannot acquire a hidden
 * legacy-product fallback or republish denied assertions.
 */
public final class ProjectionAssemblyService {

    private final SourceRecordHeadStore sourceHeads;
    private final ResolutionPort resolution;
    private final OfferSummaryPort offers;
    private final EvaluationBridge evaluation;
    private final SearchSummaryPort search;
    private final DomainSliceComposer composer;
    private final ProjectionWritePort writer;
    private final Clock clock;

    /** Creates an assembler with an explicit operational clock. */
    public ProjectionAssemblyService(SourceRecordHeadStore sourceHeads, ResolutionPort resolution,
            OfferSummaryPort offers, EvaluationBridge evaluation, SearchSummaryPort search,
            DomainSliceComposer composer, ProjectionWritePort writer, Clock clock) {
        this.sourceHeads = Objects.requireNonNull(sourceHeads, "sourceHeads must not be null");
        this.resolution = Objects.requireNonNull(resolution, "resolution must not be null");
        this.offers = Objects.requireNonNull(offers, "offers must not be null");
        this.evaluation = Objects.requireNonNull(evaluation, "evaluation must not be null");
        this.search = Objects.requireNonNull(search, "search must not be null");
        this.composer = Objects.requireNonNull(composer, "composer must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Resolves, evaluates and writes every surface for a GTIN in one envelope.
     *
     * @param gtin product to rebuild
     * @param replayInputs fixed versions and decision instant
     * @param trigger reason evaluation is refreshed
     * @return the stored envelope
     */
    public ProductReferenceProjectionEnvelope rebuild(Gtin gtin, ProjectionReplayInputs replayInputs,
            EvaluationRefreshTrigger trigger) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(replayInputs, "replayInputs must not be null");
        Objects.requireNonNull(trigger, "trigger must not be null");
        List<org.open4goods.datareference.model.SourceRecordHead> heads = sourceHeads.findByGtin(gtin);
        Map<ProjectionSurface, ProductReferenceProjection> components = new EnumMap<>(ProjectionSurface.class);
        Instant builtAt = clock.instant();
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            List<ResolvedValue> values = resolution.resolve(gtin, heads, surface, replayInputs.evaluationInstant());
            var offerSummary = offers.summarize(gtin, surface, replayInputs);
            var evaluationSummary = evaluation.evaluate(new EvaluationInput(gtin, surface, replayInputs, values,
                    offerSummary, trigger));
            var searchSummary = search.summarize(gtin, surface, replayInputs, values);
            components.put(surface, composer.compose(gtin, surface, replayInputs, values, offerSummary,
                    evaluationSummary, searchSummary, builtAt));
        }
        ProductReferenceProjectionEnvelope envelope = new ProductReferenceProjectionEnvelope(gtin, components);
        writer.write(envelope);
        return envelope;
    }
}
