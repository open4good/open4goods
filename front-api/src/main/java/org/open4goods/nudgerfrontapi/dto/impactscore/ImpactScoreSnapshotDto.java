package org.open4goods.nudgerfrontapi.dto.impactscore;

import java.util.List;
import java.util.Map;

import org.open4goods.model.impactscore.ImpactScoreSnapshot;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Monthly aggregated picture of the ImpactScore of one vertical. Aggregates only, no personal data.
 */
@Schema(name = "ImpactScoreSnapshot", description = "Monthly aggregated ImpactScore of a vertical: counts, score percentiles (0 to 20), coverage, EPREL matching and top GTINs.")
public record ImpactScoreSnapshotDto(
        @Schema(description = "Vertical identifier.", example = "tv") String verticalId,
        @Schema(description = "Month of the snapshot, yyyy-MM.", example = "2026-11") String month,
        @Schema(description = "Instant of the snapshot, epoch milliseconds.") long snapshotAt,
        @Schema(description = "Methodology version applied to the vertical.", example = "2") int methodologyVersion,
        @Schema(description = "Fingerprint of the active criteria matrix.") String methodologyRevision,
        @Schema(description = "Products having offers: rated + notRated + excluded.") int cohort,
        @Schema(description = "Products holding an ImpactScore.") int rated,
        @Schema(description = "Assessed products without ImpactScore.") int notRated,
        @Schema(description = "Not rated products per reason.") Map<String, Integer> notRatedByReason,
        @Schema(description = "Products excluded from the vertical.") int excluded,
        @Schema(description = "Excluded products per cause (a product with several causes counts in each).") Map<String, Integer> excludedByCause,
        @Schema(description = "Percentiles p0..p100 of the rated products' score, 0 to 20. Empty when no product is rated.") Map<String, Double> scorePercentiles,
        @Schema(description = "Mean share of the criteria weight actually filled in, 0 to 1. Absent when no product was assessed.", nullable = true) Double avgRealWeightCoverage,
        @Schema(description = "Per criterion, share of the assessed products having it filled in, 0 to 1.") Map<String, Double> criteriaCoverage,
        @Schema(description = "Products matched with an EPREL record.") int eprelMatched,
        @Schema(description = "eprelMatched / cohort, 0 to 1.", nullable = true) Double eprelMatchedShare,
        @Schema(description = "Best rated products, best first.") List<TopEntryDto> top) {

    /** One row of the top. */
    @Schema(name = "ImpactScoreSnapshotTopEntry")
    public record TopEntryDto(@Schema(description = "Barcode.") String gtin,
            @Schema(description = "ImpactScore, 0 to 20.") double score) {
    }

    /**
     * Maps a stored snapshot.
     *
     * @param s the stored snapshot
     * @return the DTO
     */
    public static ImpactScoreSnapshotDto from(ImpactScoreSnapshot s) {
        return new ImpactScoreSnapshotDto(s.verticalId(), s.month(), s.snapshotAt(), s.methodologyVersion(),
                s.methodologyRevision(), s.cohort(), s.rated(), s.notRated(), s.notRatedByReason(), s.excluded(),
                s.excludedByCause(), s.scorePercentiles(), s.avgRealWeightCoverage(), s.criteriaCoverage(),
                s.eprelMatched(), s.eprelMatchedShare(),
                s.top() == null ? List.of() : s.top().stream().map(t -> new TopEntryDto(t.gtin(), t.score())).toList());
    }
}
