package org.open4goods.model.impactscore;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Monthly aggregated picture of the ImpactScore of one vertical, kept for the history of the methodology, the
 * quarterly barometer and the future open data set (GOU-1896).
 *
 * <p>Aggregates only: no personal data, and the only product identifiers are the GTIN of the top rows. One document
 * per vertical and month, its id is {@code <verticalId>_<yyyy-MM>}: writing the same month again replaces the
 * document instead of duplicating it. Built by {@link ImpactScoreSnapshotBuilder}.
 *
 * @param id                    {@code <verticalId>_<yyyy-MM>}
 * @param verticalId            the vertical
 * @param month                 {@code yyyy-MM}
 * @param snapshotAt            instant of the snapshot, epoch milliseconds
 * @param methodologyVersion    ImpactScore methodology version applied to the vertical
 * @param methodologyRevision   fingerprint of the active matrix
 * @param cohort                products of the vertical having offers: {@code rated + notRated + excluded}
 * @param rated                 products holding an ImpactScore
 * @param notRated              products assessed but without ImpactScore
 * @param notRatedByReason      {@code notRated} per reason ({@code LOW_COVERAGE}, {@code NO_REAL_CRITERIA},
 *                              {@code UNSPECIFIED} when the batch gave none)
 * @param excluded              products excluded from the vertical
 * @param excludedByCause       {@code excluded} per cause (a product with several causes counts in each)
 * @param scorePercentiles      percentiles ({@code p0}..{@code p100}) of the rated products' score, on a 0 to 20 scale
 * @param avgRealWeightCoverage mean weight coverage of the criteria actually filled in, over the assessed products
 *                              ({@code null} when no product was assessed)
 * @param criteriaCoverage      per criterion, share (0 to 1) of the assessed products having it filled in
 * @param eprelMatched          cohort products matched with an EPREL record
 * @param eprelMatchedShare     {@code eprelMatched / cohort} ({@code null} for an empty cohort)
 * @param top                   best rated products, best first
 */
@Document(indexName = ImpactScoreSnapshot.INDEX_NAME, createIndex = true)
public record ImpactScoreSnapshot(
		@Id @Field(type = FieldType.Keyword) String id,
		@Field(type = FieldType.Keyword) String verticalId,
		@Field(type = FieldType.Keyword) String month,
		@Field(type = FieldType.Long) long snapshotAt,
		@Field(type = FieldType.Integer) int methodologyVersion,
		@Field(type = FieldType.Keyword) String methodologyRevision,
		@Field(type = FieldType.Integer) int cohort,
		@Field(type = FieldType.Integer) int rated,
		@Field(type = FieldType.Integer) int notRated,
		Map<String, Integer> notRatedByReason,
		@Field(type = FieldType.Integer) int excluded,
		Map<String, Integer> excludedByCause,
		Map<String, Double> scorePercentiles,
		@Field(type = FieldType.Double) Double avgRealWeightCoverage,
		Map<String, Double> criteriaCoverage,
		@Field(type = FieldType.Integer) int eprelMatched,
		@Field(type = FieldType.Double) Double eprelMatchedShare,
		List<TopEntry> top) {

	/** Name of the Elasticsearch index. */
	public static final String INDEX_NAME = "impactscore-snapshots";

	/** Number of rows of {@link #top()}. */
	public static final int TOP_SIZE = 10;

	/**
	 * One row of the top.
	 *
	 * @param gtin  barcode of the product
	 * @param score ImpactScore on a 0 to 20 scale
	 */
	public record TopEntry(String gtin, double score) {
	}

	/**
	 * Id of the document of a vertical and a month.
	 *
	 * @param verticalId the vertical
	 * @param month      {@code yyyy-MM}
	 * @return the document id
	 */
	public static String idOf(String verticalId, String month) {
		return verticalId + "_" + month;
	}
}
