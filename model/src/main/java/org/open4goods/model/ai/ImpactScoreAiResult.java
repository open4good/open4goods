package org.open4goods.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents the structured result of the AI generation for Impact Score configuration.
 * This class matches the JSON structure defined in the 'impactscore-generation.yml' prompt.
 */
public class ImpactScoreAiResult {

    @JsonProperty(value = "use_case", required = true)
    @AiGeneratedField(instruction = "Le cas d'usage (ex: consumer_comparison)")
    private String useCase;

    @JsonProperty(value = "geo_scope", required = true)
    @AiGeneratedField(instruction = "Le périmètre géographique (ex: EU/France)")
    private String geoScope;

    @JsonProperty(value = "vertical", required = true)
    @AiGeneratedField(instruction = "Le nom de la verticale")
    private String vertical;

    @JsonProperty(value = "available_criterias", required = true)
    @AiGeneratedField(instruction = "La liste des critères disponibles")
    private List<String> availableCriterias;

    @JsonProperty(value = "positioning", required = true)
    @AiGeneratedField(instruction = "Positionnement du score (relatif vs absolu)")
    private Positioning positioning;

    @JsonProperty(value = "reference_lifetime", required = true)
    @AiGeneratedField(instruction = "Durée de vie de référence du produit")
    private ReferenceLifetime referenceLifetime;

    @JsonProperty(value = "gap_analysis_absolute_vs_relative", required = true)
    @AiGeneratedField(instruction = "Analyse des écarts entre ACV absolue et score relatif")
    private GapAnalysis gapAnalysis;

    @JsonProperty(value = "weighting_method", required = true)
    @AiGeneratedField(instruction = "Méthodologie de pondération utilisée")
    private WeightingMethod weightingMethod;

    @JsonProperty(value = "criteria_weights", required = true)
    @AiGeneratedField(instruction = "Liste des pondérations pour chaque critère")
    private List<CriteriaWeight> criteriaWeights;

    @JsonProperty(value = "normalization_notes", required = true)
    @AiGeneratedField(instruction = "Notes sur la normalisation des scores")
    private NormalizationNotes normalizationNotes;

    @JsonProperty(value = "quality_checks", required = true)
    @AiGeneratedField(instruction = "Vérifications de cohérence (somme des poids, exhaustivité)")
    private QualityChecks qualityChecks;

    @JsonProperty(value = "materiality_summary", required = true)
    @AiGeneratedField(instruction = "Synthèse de la matérialité environnementale")
    private MaterialitySummary materialitySummary;

    @JsonProperty(value = "sources", required = true)
    @AiGeneratedField(instruction = "Liste exhaustive des sources consultées")
    private List<Source> sources;

    @JsonProperty(value = "search_log", required = true)
    @AiGeneratedField(instruction = "Journal des recherches effectuées")
    private List<SearchLog> searchLog;

    // Getters and Setters

    public String getUseCase() { return useCase; }
    public void setUseCase(String useCase) { this.useCase = useCase; }

    public String getGeoScope() { return geoScope; }
    public void setGeoScope(String geoScope) { this.geoScope = geoScope; }

    public String getVertical() { return vertical; }
    public void setVertical(String vertical) { this.vertical = vertical; }

    public List<String> getAvailableCriterias() { return availableCriterias; }
    public void setAvailableCriterias(List<String> availableCriterias) { this.availableCriterias = availableCriterias; }

    public Positioning getPositioning() { return positioning; }
    public void setPositioning(Positioning positioning) { this.positioning = positioning; }

    public ReferenceLifetime getReferenceLifetime() { return referenceLifetime; }
    public void setReferenceLifetime(ReferenceLifetime referenceLifetime) { this.referenceLifetime = referenceLifetime; }

    public GapAnalysis getGapAnalysis() { return gapAnalysis; }
    public void setGapAnalysis(GapAnalysis gapAnalysis) { this.gapAnalysis = gapAnalysis; }

    public WeightingMethod getWeightingMethod() { return weightingMethod; }
    public void setWeightingMethod(WeightingMethod weightingMethod) { this.weightingMethod = weightingMethod; }

    public List<CriteriaWeight> getCriteriaWeights() { return criteriaWeights; }
    public void setCriteriaWeights(List<CriteriaWeight> criteriaWeights) { this.criteriaWeights = criteriaWeights; }

    public NormalizationNotes getNormalizationNotes() { return normalizationNotes; }
    public void setNormalizationNotes(NormalizationNotes normalizationNotes) { this.normalizationNotes = normalizationNotes; }

    public QualityChecks getQualityChecks() { return qualityChecks; }
    public void setQualityChecks(QualityChecks qualityChecks) { this.qualityChecks = qualityChecks; }

    public MaterialitySummary getMaterialitySummary() { return materialitySummary; }
    public void setMaterialitySummary(MaterialitySummary materialitySummary) { this.materialitySummary = materialitySummary; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }

    public List<SearchLog> getSearchLog() { return searchLog; }
    public void setSearchLog(List<SearchLog> searchLog) { this.searchLog = searchLog; }


    // Nested Classes

    public static class Positioning {
        @JsonProperty(value = "claim", required = true)
        public String claim;
        @JsonProperty(value = "not_an_absolute_footprint", required = true)
        public boolean notAnAbsoluteFootprint;
        @JsonProperty(value = "what_it_means_for_users", required = true)
        public String whatItMeansForUsers;
    }

    public static class ReferenceLifetime {
        @JsonProperty(value = "lifetime_years_reference", required = true)
        public double lifetimeYearsReference;
        @JsonProperty(value = "lifetime_years_range", required = true)
        public LifetimeRange lifetimeYearsRange; // min, max
        @JsonProperty(value = "rationale", required = true)
        public String rationale;
        @JsonProperty(value = "citations", required = true)
        public List<String> citations;

        public static class LifetimeRange {
            @JsonProperty(value = "min", required = true)
            public double min;
            @JsonProperty(value = "max", required = true)
            public double max;
        }
    }

    public static class GapAnalysis {
        @JsonProperty(value = "what_is_missing_vs_full_lca", required = true)
        public String whatIsMissingVsFullLca;
        @JsonProperty(value = "why_relative_is_still_useful", required = true)
        public String whyRelativeIsStillUseful;
        @JsonProperty(value = "mitigations", required = true)
        public List<String> mitigations;
    }

    public static class WeightingMethod {
        @JsonProperty(value = "approach", required = true)
        public String approach;
        @JsonProperty(value = "key_principles", required = true)
        public List<String> keyPrinciples;
        @JsonProperty(value = "assumptions", required = true)
        public List<Assumption> assumptions;
        
        public static class Assumption {
            @JsonProperty(value = "assumption", required = true)
            public String assumption;
            @JsonProperty(value = "why_it_matters", required = true)
            public String whyItMatters;
            @JsonProperty(value = "citations", required = true)
            public List<String> citations;
        }
    }

    public static class CriteriaWeight {
        @JsonProperty(value = "criterion", required = true)
        public String criterion;
        @JsonProperty(value = "weight", required = true)
        public double weight;
        @JsonProperty(value = "directionality", required = true)
        public String directionality;
        @JsonProperty(value = "impact_dimensions", required = true)
        public List<String> impactDimensions;
        @JsonProperty(value = "rationale", required = true)
        public String rationale;
        @JsonProperty(value = "overlap_notes", required = true)
        public String overlapNotes;
        @JsonProperty(value = "guardrails", required = true)
        public String guardrails;
        @JsonProperty(value = "evidence_strength", required = true)
        public EvidenceStrength evidenceStrength;

        public static class EvidenceStrength {
            @JsonProperty(value = "level", required = true)
            public String level;
            @JsonProperty(value = "why", required = true)
            public String why;
        }
    }

    public static class NormalizationNotes {
        @JsonProperty(value = "relative_ranking_0_100", required = true)
        public String relativeRanking0100;
        @JsonProperty(value = "sigma_scoring_0_5", required = true)
        public String sigmaScoring05;
        @JsonProperty(value = "percentile_fallback", required = true)
        public String percentileFallback;
        @JsonProperty(value = "missing_value_virtualization", required = true)
        public String missingValueVirtualization;
    }

    public static class QualityChecks {
        @JsonProperty(value = "criteria_weights_complete", required = true)
        public boolean criteriaWeightsComplete;
        @JsonProperty(value = "weights_sum_to_1", required = true)
        public boolean weightsSumTo1;
        @JsonProperty(value = "sum_value", required = true)
        public double sumValue;
        @JsonProperty(value = "unused_or_zero_weight_criterias", required = true)
        public List<String> unusedOrZeroWeightCriterias;
        @JsonProperty(value = "renormalization_rule_if_needed", required = true)
        public String renormalizationRuleIfNeeded;
    }

    public static class MaterialitySummary {
        @JsonProperty(value = "dominant_lifecycle_stages_for_vertical", required = true)
        public List<String> dominantLifecycleStagesForVertical;
        @JsonProperty(value = "short_summary", required = true)
        public String shortSummary;
    }

    public static class Source {
        @JsonProperty(value = "id", required = true)
        public String id;
        @JsonProperty(value = "type", required = true)
        public String type;
        @JsonProperty(value = "title", required = true)
        public String title;
        @JsonProperty(value = "authors_or_org", required = true)
        public String authorsOrOrg;
        @JsonProperty(value = "year", required = true)
        public Integer year; // Can be null or integer
        @JsonProperty(value = "publisher", required = true)
        public String publisher;
        @JsonProperty(value = "url", required = true)
        public String url;
        @JsonProperty(value = "accessed_date", required = true)
        public String accessedDate;
        @JsonProperty(value = "reliability", required = true)
        public String reliability;
        @JsonProperty(value = "used", required = true)
        public boolean used;
        @JsonProperty(value = "used_for", required = true)
        public List<String> usedFor;
        @JsonProperty(value = "notes", required = true)
        public String notes;
    }

    public static class SearchLog {
        @JsonProperty(value = "query", required = true)
        public String query;
        @JsonProperty(value = "date", required = true)
        public String date;
        @JsonProperty(value = "notes", required = true)
        public String notes;
    }
}
