package org.open4goods.model.impactscore;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Computes the {@link ImpactScoreSnapshot} of a vertical from its products, once the batch has scored them.
 *
 * <p>Pure computation, no I/O. Absent stays absent: a product without {@link ImpactScoreMeta} is counted as not
 * rated, never as rated, and a missing average stays {@code null}.
 */
public final class ImpactScoreSnapshotBuilder {

	/** Percentiles published, in percent. */
	private static final int[] PERCENTILES = { 0, 10, 25, 50, 75, 90, 100 };

	/** Reason given to a not rated product whose batch gave none. */
	static final String UNSPECIFIED = "UNSPECIFIED";

	private ImpactScoreSnapshotBuilder() {
	}

	/**
	 * Builds the snapshot.
	 *
	 * @param vertical the vertical (active matrix and methodology)
	 * @param cohort   the vertical's products having offers, excluded ones included, scored by the batch
	 * @param month    the month of the snapshot
	 * @param at       instant of the snapshot
	 * @return the snapshot, never {@code null}
	 */
	public static ImpactScoreSnapshot build(VerticalConfig vertical, Collection<Product> cohort, YearMonth month, Instant at) {
		ImpactScoreConfig config = vertical.getImpactScoreConfig();
		Map<String, Integer> excludedByCause = new TreeMap<>();
		Map<String, Integer> notRatedByReason = new TreeMap<>();
		Map<String, Integer> filled = new TreeMap<>();
		List<Double> scores = new ArrayList<>();
		List<ImpactScoreSnapshot.TopEntry> ranked = new ArrayList<>();
		int excluded = 0;
		int notRated = 0;
		int assessed = 0;
		int eprel = 0;
		double coverageSum = 0.0;
		int coverageCount = 0;
		for (String criterion : config.activeCriteriasPonderation().keySet()) {
			filled.put(criterion, 0);
		}

		for (Product product : cohort) {
			if (product.getEprelDatas() != null) {
				eprel++;
			}
			if (product.isExcluded()) {
				excluded++;
				for (String cause : product.getExcludedCauses()) {
					excludedByCause.merge(cause, 1, Integer::sum);
				}
				continue;
			}
			ImpactScoreMeta meta = product.getImpactScoreMeta();
			Double score = ratedScore(product, meta);
			if (score != null) {
				scores.add(score);
				ranked.add(new ImpactScoreSnapshot.TopEntry(String.valueOf(product.getId()), score));
			} else {
				notRated++;
				String reason = meta == null || meta.getNotRatedReason() == null ? UNSPECIFIED : meta.getNotRatedReason();
				notRatedByReason.merge(reason, 1, Integer::sum);
			}
			if (meta != null) {
				assessed++;
				if (meta.getRealWeightCoverage() != null) {
					coverageSum += meta.getRealWeightCoverage();
					coverageCount++;
				}
				for (String criterion : filled.keySet()) {
					if (isFilled(product.getScores().get(criterion))) {
						filled.merge(criterion, 1, Integer::sum);
					}
				}
			}
		}

		Map<String, Double> criteriaCoverage = new TreeMap<>();
		if (assessed > 0) {
			for (Map.Entry<String, Integer> e : filled.entrySet()) {
				criteriaCoverage.put(e.getKey(), e.getValue() / (double) assessed);
			}
		}
		ranked.sort(Comparator.comparingDouble(ImpactScoreSnapshot.TopEntry::score).reversed()
				.thenComparing(ImpactScoreSnapshot.TopEntry::gtin));
		List<ImpactScoreSnapshot.TopEntry> top = List.copyOf(ranked.subList(0, Math.min(ImpactScoreSnapshot.TOP_SIZE, ranked.size())));
		String monthKey = month.toString();
		int size = cohort.size();
		return new ImpactScoreSnapshot(ImpactScoreSnapshot.idOf(vertical.getId(), monthKey), vertical.getId(), monthKey,
				at.toEpochMilli(), config.effectiveMethodologyVersion(), config.getMethodologyRevision(), size, scores.size(),
				notRated, notRatedByReason, excluded, excludedByCause, percentiles(scores),
				coverageCount > 0 ? coverageSum / coverageCount : null, criteriaCoverage, eprel,
				size > 0 ? eprel / (double) size : null, top);
	}

	/**
	 * The month a batch instant belongs to, in UTC.
	 *
	 * @param at the instant
	 * @return its month
	 */
	public static YearMonth monthOf(Instant at) {
		return YearMonth.from(at.atZone(ZoneOffset.UTC));
	}

	private static Double ratedScore(Product product, ImpactScoreMeta meta) {
		if (meta == null || !ImpactScoreMeta.Status.RATED.name().equals(meta.getStatus())) {
			return null;
		}
		Score ecoscore = product.ecoscore();
		if (ecoscore == null || Boolean.TRUE.equals(ecoscore.getVirtual()) || ecoscore.getRelativ() == null
				|| ecoscore.getRelativ().getValue() == null) {
			return null;
		}
		return ecoscore.getRelativ().getValue() * 20.0 / StandardiserService.DEFAULT_MAX_RATING;
	}

	private static boolean isFilled(Score score) {
		return score != null && !Boolean.TRUE.equals(score.getVirtual())
				&& (score.getRelativ() != null && score.getRelativ().getValue() != null
						|| score.getAbsolute() != null && score.getAbsolute().getValue() != null
						|| score.getValue() != null);
	}

	/** Linear-interpolation percentiles; empty when there is no score (nothing is invented). */
	private static Map<String, Double> percentiles(List<Double> scores) {
		Map<String, Double> ret = new TreeMap<>();
		if (scores.isEmpty()) {
			return ret;
		}
		List<Double> sorted = scores.stream().sorted().toList();
		for (int p : PERCENTILES) {
			double rank = p / 100.0 * (sorted.size() - 1);
			int low = (int) Math.floor(rank);
			int high = (int) Math.ceil(rank);
			double value = sorted.get(low) + (sorted.get(high) - sorted.get(low)) * (rank - low);
			ret.put("p" + p, value);
		}
		return ret;
	}
}
