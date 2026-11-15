package org.open4goods.api.services.backfill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.open4goods.model.product.ProductNature;
import org.open4goods.verticals.VerticalsConfigService;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Sets {@code nature} on the indexed products without vertical (GOU-2234, lot CSS 4e.2 of GOU-2226).
 *
 * <p>A feed row that did not change only triggers a partial update, and the nightly batch only loads the products of
 * the verticals: without this backfill, the products without vertical never get the {@code nature} computed by
 * {@code TaxonomyRealTimeAggregationService} (GOU-2233). The scope is the live products without vertical: no
 * {@code vertical}, {@code lastChange} within {@code lastChangeDays}, {@code offersCount} above 0, and
 * {@code datasourceCategories} present.
 *
 * <p>The nature comes from the same computation as the aggregation,
 * {@link VerticalsConfigService#getNatureForCategories(Collection, double)}, with the threshold
 * {@code natureMinMarkedCategoryShare} given to the run. A product whose stored nature already matches is not
 * written, so the backfill can be run again; a product that got a vertical meanwhile is left untouched by the
 * script. The previous value is recomputable, so no backup is written and there is no restore.
 *
 * <p>The {@link #details()} count, for each report threshold, the products that would be marked per nature value, and
 * give up to {@value #EXAMPLES_SIZE} examples marked at the lowest report threshold but not at the highest: the dry
 * run report the CTO uses to set the threshold (measure 7.4 of GOU-2226).
 */
public class NatureBackfillOperation implements BackfillOperation {

	/** Name of the operation, in the endpoint and the status. */
	public static final String NAME = "nature";

	/** Default report thresholds of the dry run (measure 7.4 of GOU-2226). */
	public static final List<Double> DEFAULT_REPORT_THRESHOLDS = List.of(0d, 0.5d);

	/** Default {@code lastChange} window, in days: the live products (measure 4 of GOU-2226). */
	public static final int DEFAULT_LAST_CHANGE_DAYS = 2;

	/** Number of examples reported. */
	static final int EXAMPLES_SIZE = 20;

	/**
	 * Sets or removes {@code nature}, unless the product got a vertical (it never has a nature then) or already holds
	 * the value.
	 */
	static final String NATURE_SCRIPT = "if (ctx._source.vertical != null) { ctx.op = 'noop'; } "
			+ "else if (params.nature == null) { if (ctx._source.containsKey('nature')) { ctx._source.remove('nature'); } else { ctx.op = 'noop'; } } "
			+ "else if (params.nature == ctx._source.nature) { ctx.op = 'noop'; } "
			+ "else { ctx._source.nature = params.nature; }";

	private final VerticalsConfigService verticalsConfigService;
	private final double threshold;
	private final List<Double> reportThresholds;
	private final int lastChangeDays;

	/** Marked products per report threshold, then per nature value. */
	private final Map<Double, Map<String, Long>> markedByThreshold = new TreeMap<>();
	private final List<Map<String, Object>> examples = new ArrayList<>();

	/**
	 * @param verticalsConfigService nature computation and its markers
	 * @param threshold              {@code natureMinMarkedCategoryShare} of the written nature, 0 to 1
	 * @param reportThresholds       thresholds counted in {@link #details()}, 0 to 1
	 * @param lastChangeDays         {@code lastChange} window, in days, at least 1
	 * @throws IllegalArgumentException when a threshold is outside 0 to 1 or the window below 1 day
	 */
	public NatureBackfillOperation(VerticalsConfigService verticalsConfigService, double threshold,
			List<Double> reportThresholds, int lastChangeDays) {
		this.verticalsConfigService = Objects.requireNonNull(verticalsConfigService, "verticalsConfigService");
		requireShare(threshold);
		List<Double> thresholds = reportThresholds == null || reportThresholds.isEmpty() ? DEFAULT_REPORT_THRESHOLDS
				: reportThresholds.stream().distinct().sorted().toList();
		thresholds.forEach(NatureBackfillOperation::requireShare);
		if (lastChangeDays < 1) {
			throw new IllegalArgumentException("lastChangeDays must be at least 1");
		}
		this.threshold = threshold;
		this.reportThresholds = thresholds;
		this.lastChangeDays = lastChangeDays;
		thresholds.forEach(t -> markedByThreshold.put(t, new TreeMap<>()));
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public Query query() {
		return Query.of(q -> q.bool(b -> b
				.filter(f -> f.range(r -> r.date(d -> d.field("lastChange").gte("now-" + lastChangeDays + "d"))))
				.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0d))))
				.filter(f -> f.exists(e -> e.field("datasourceCategories")))
				.mustNot(m -> m.exists(e -> e.field("vertical")))));
	}

	@Override
	public List<String> sourceFields() {
		return List.of("datasourceCategories", "nature", "offerNames");
	}

	@Override
	public Optional<BackfillChange> plan(String id, Map<String, Object> source) {
		List<String> categories = strings(source.get("datasourceCategories"));

		ProductNature lowest = null;
		ProductNature highest = null;
		for (Double reportThreshold : reportThresholds) {
			ProductNature nature = verticalsConfigService.getNatureForCategories(categories, reportThreshold);
			if (nature != null) {
				markedByThreshold.get(reportThreshold).merge(nature.name(), 1L, Long::sum);
			}
			if (reportThreshold.equals(reportThresholds.getFirst())) {
				lowest = nature;
			}
			highest = nature;
		}
		if (lowest != null && highest == null && reportThresholds.size() > 1 && examples.size() < EXAMPLES_SIZE) {
			Map<String, Object> example = new LinkedHashMap<>();
			example.put("gtin", id);
			example.put("name", strings(source.get("offerNames")).stream().min(Comparator.comparingInt(String::length))
					.orElse(null));
			example.put("nature", lowest.name());
			example.put("datasourceCategories", List.copyOf(categories));
			examples.add(example);
		}

		ProductNature target = verticalsConfigService.getNatureForCategories(categories, threshold);
		String targetValue = target == null ? null : target.name();
		Object current = source.get("nature");
		if (Objects.equals(targetValue, current)) {
			return Optional.empty();
		}
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("nature", targetValue);
		return Optional.of(new BackfillChange(NATURE_SCRIPT, params, null,
				List.of("nature=" + (targetValue == null ? "absent" : targetValue))));
	}

	@Override
	public String restoreScript() {
		return null;
	}

	@Override
	public Map<String, Object> parameters() {
		return Map.of("threshold", threshold, "reportThresholds", reportThresholds, "lastChangeDays", lastChangeDays);
	}

	/**
	 * @return {@code threshold}, {@code lastChangeDays}, {@code markedByThreshold} (threshold, then nature value, then
	 *         count) and {@code examplesMarkedOnlyAtLowestThreshold}, as an immutable snapshot
	 */
	@Override
	public Map<String, Object> details() {
		Map<String, Object> marked = new LinkedHashMap<>();
		markedByThreshold.forEach((t, counts) -> marked.put(String.valueOf(t), Map.copyOf(counts)));
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("threshold", threshold);
		details.put("lastChangeDays", lastChangeDays);
		details.put("markedByThreshold", marked);
		// An example may have no name: Map.copyOf refuses null values
		details.put("examplesMarkedOnlyAtLowestThreshold",
				examples.stream().map(e -> Collections.unmodifiableMap(new LinkedHashMap<>(e))).toList());
		return Collections.unmodifiableMap(details);
	}

	private static List<String> strings(Object value) {
		if (value instanceof Collection<?> collection) {
			return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		if (value instanceof String single) {
			return List.of(single);
		}
		return List.of();
	}

	private static void requireShare(double share) {
		if (share < 0 || share > 1) {
			throw new IllegalArgumentException("thresholds must be between 0 and 1");
		}
	}
}
