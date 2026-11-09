package org.open4goods.api.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.api.config.yml.ImpactScoreGuardProperties;
import org.open4goods.api.config.yml.ImpactScoreGuardProperties.Thresholds;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Automatic checks of the ImpactScore data after each vertical batch (GOU-1895).
 *
 * <p>The guard computes a {@link Snapshot} of the scored products of a vertical and compares it with the one of
 * the previous accepted batch (persisted as JSON per vertical, so the reference survives a restart; without a readable file the batch only gets the absolute checks).
 * Violations are either {@link Severity#BLOCKING} (the caller must not index) or {@link Severity#WARNING}.
 *
 * <ul>
 * <li>Blocking: ImpactScore outside [0, 20] or {@code relativ} different from {@code value / 4}; composite
 * ImpactScore (non-empty {@code aggregates}); compressed scale (p95 - p5 under the floor).</li>
 * <li>Warning: median shift, mean real-weight coverage shift (the imputed share is its complement), drop of the
 * rated or scored count, drop of the EPREL-matched count.</li>
 * </ul>
 *
 * <p>Alert channels: structured {@code ERROR} log (one line per violation), Micrometer gauges
 * {@code impactscore.guard.violations} tagged by vertical and severity, and this class as a
 * {@link HealthIndicator} (custom statuses {@code ALERT} / {@code WARN}, which never turn the aggregate health
 * DOWN) so Ops reads the state from {@code /actuator/health/impactScoreGuard}.
 */
public class ImpactScoreBatchGuard implements HealthIndicator {

	private static final Logger logger = LoggerFactory.getLogger(ImpactScoreBatchGuard.class);

	/** Bounds of the ImpactScore value. */
	static final double MIN_SCORE = 0.0;
	static final double MAX_SCORE = 20.0;
	/** The relative score is the value on a 0-5 scale: value / 4. */
	static final double RELATIV_DIVISOR = 4.0;
	private static final double EPSILON = 1e-6;

	static final Status STATUS_ALERT = new Status("ALERT");
	static final Status STATUS_WARN = new Status("WARN");

	/** Gravity of a violation. */
	public enum Severity {
		BLOCKING, WARNING
	}

	/** One failed check. */
	public record Violation(Severity severity, String rule, String message) {
	}

	/**
	 * Aggregates of one batch.
	 *
	 * @param scored number of products handed to scoring
	 * @param rated number of products carrying an ImpactScore
	 * @param median median ImpactScore, null when nothing is rated
	 * @param p5 5th percentile
	 * @param p95 95th percentile
	 * @param meanCoverage mean real-weight coverage over the products having it, null when none
	 * @param eprelMatched number of scored products matched with EPREL
	 */
	public record Snapshot(int scored, int rated, Double median, Double p5, Double p95, Double meanCoverage,
			int eprelMatched) {
	}

	/** Outcome of one verification. */
	public record Report(String vertical, Snapshot snapshot, List<Violation> violations) {

		/** @return true when at least one violation forbids the indexation */
		public boolean blocking() {
			return violations.stream().anyMatch(v -> v.severity() == Severity.BLOCKING);
		}
	}

	private final ImpactScoreGuardProperties properties;
	private final MeterRegistry meterRegistry;
	private final Path stateFolder;
	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<String, Snapshot> previous = new ConcurrentHashMap<>();
	private final Map<String, Report> lastReports = new ConcurrentHashMap<>();
	private final Map<String, AtomicInteger> gauges = new ConcurrentHashMap<>();

	/**
	 * @param properties thresholds, defaults are used when null
	 * @param meterRegistry optional metrics registry
	 */
	public ImpactScoreBatchGuard(ImpactScoreGuardProperties properties, MeterRegistry meterRegistry) {
		this(properties, meterRegistry, null);
	}

	/**
	 * @param properties thresholds, defaults are used when null
	 * @param meterRegistry optional metrics registry
	 * @param stateFolder folder holding the accepted snapshot of each vertical ({@code <verticalId>.json}), null to
	 *        keep the reference in memory only
	 */
	public ImpactScoreBatchGuard(ImpactScoreGuardProperties properties, MeterRegistry meterRegistry, Path stateFolder) {
		this.stateFolder = stateFolder;
		this.properties = properties == null ? new ImpactScoreGuardProperties() : properties;
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Checks the scored products of a vertical, logs and publishes the violations.
	 *
	 * <p>The snapshot becomes the reference of the next batch only when nothing blocks: a rejected batch must not
	 * become the baseline.
	 *
	 * @param verticalId the vertical id
	 * @param scored the products handed to scoring (after scoring)
	 * @return the report
	 */
	public Report verify(String verticalId, Collection<Product> scored) {
		if (!properties.isEnabled()) {
			return new Report(verticalId, new Snapshot(scored.size(), 0, null, null, null, null, 0), List.of());
		}
		Thresholds t = properties.forVertical(verticalId);
		Snapshot snapshot = snapshot(scored);
		List<Violation> violations = new ArrayList<>(checkAbsolute(scored, snapshot, t));
		Snapshot before = previous.computeIfAbsent(verticalId, this::load);
		if (before != null) {
			violations.addAll(checkTrend(before, snapshot, t));
		}
		Report report = new Report(verticalId, snapshot, List.copyOf(violations));
		lastReports.put(verticalId, report);
		publish(report);
		if (!report.blocking()) {
			previous.put(verticalId, snapshot);
			save(verticalId, snapshot);
		}
		return report;
	}

	private Path stateFile(String verticalId) {
		return stateFolder.resolve(verticalId.replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
	}

	/** Reads the persisted reference; a missing or unreadable file means no reference, never a failure. */
	private Snapshot load(String verticalId) {
		if (stateFolder == null) {
			return null;
		}
		Path file = stateFile(verticalId);
		if (!Files.exists(file)) {
			return null;
		}
		try {
			return mapper.readValue(file.toFile(), Snapshot.class);
		} catch (IOException | RuntimeException e) {
			logger.warn("impactscore_guard vertical={} unreadable reference {}: {}", verticalId, file, e.toString());
			return null;
		}
	}

	/** Writes the reference atomically (temporary file, then move); a failure is logged, never blocking. */
	private void save(String verticalId, Snapshot snapshot) {
		if (stateFolder == null) {
			return;
		}
		try {
			Files.createDirectories(stateFolder);
			Path file = stateFile(verticalId);
			Path tmp = Files.createTempFile(stateFolder, verticalId.replaceAll("[^A-Za-z0-9._-]", "_"), ".tmp");
			mapper.writeValue(tmp.toFile(), snapshot);
			try {
				Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException | RuntimeException e) {
			logger.warn("impactscore_guard vertical={} cannot persist the reference: {}", verticalId, e.toString());
		}
	}

	private List<Violation> checkAbsolute(Collection<Product> scored, Snapshot s, Thresholds t) {
		List<Violation> out = new ArrayList<>();
		List<String> outOfBounds = new ArrayList<>();
		List<String> badRelativ = new ArrayList<>();
		List<String> composites = new ArrayList<>();
		for (Product p : scored) {
			Score score = p.ecoscore();
			if (score == null) {
				continue;
			}
			boolean virtual = Boolean.TRUE.equals(score.getVirtual());
			if (score.getAggregates() != null && !score.getAggregates().isEmpty()) {
				composites.add(String.valueOf(p.getId()));
			}
			Double value = score.getValue();
			if (value == null || value.isNaN() || value < MIN_SCORE - EPSILON || value > MAX_SCORE + EPSILON) {
				outOfBounds.add(p.getId() + "=" + value);
			} else if (!virtual && (score.getRelativ() == null || score.getRelativ().getValue() == null
					|| Math.abs(score.getRelativ().getValue() - value / RELATIV_DIVISOR) > EPSILON)) {
				badRelativ.add(p.getId() + "=" + value + "/"
						+ (score.getRelativ() == null ? null : score.getRelativ().getValue()));
			}
		}
		if (!outOfBounds.isEmpty()) {
			out.add(blocking("BOUNDS", outOfBounds.size() + " ImpactScore outside [0, 20], e.g. " + sample(outOfBounds)));
		}
		if (!badRelativ.isEmpty()) {
			out.add(blocking("RELATIV", badRelativ.size() + " ImpactScore with relativ != value / 4, e.g. "
					+ sample(badRelativ)));
		}
		if (!composites.isEmpty()) {
			out.add(blocking("COMPOSITE", composites.size() + " ImpactScore with non-empty aggregates, e.g. "
					+ sample(composites)));
		}
		if (s.rated() >= t.getMinProductsForDistribution() && s.p5() != null && s.p95() != null
				&& s.p95() - s.p5() < t.getMinSpread()) {
			out.add(blocking("SPREAD", "p95 - p5 = " + round(s.p95() - s.p5()) + " < " + t.getMinSpread()
					+ " (p5=" + round(s.p5()) + ", p95=" + round(s.p95()) + "): compressed scale"));
		}
		return out;
	}

	private List<Violation> checkTrend(Snapshot b, Snapshot s, Thresholds t) {
		List<Violation> out = new ArrayList<>();
		if (b.median() != null && s.median() != null && Math.abs(s.median() - b.median()) > t.getMaxMedianShift()) {
			out.add(warning("MEDIAN_SHIFT", "median " + round(b.median()) + " -> " + round(s.median()) + " (max "
					+ t.getMaxMedianShift() + ")"));
		}
		if (b.meanCoverage() != null && s.meanCoverage() != null
				&& Math.abs(s.meanCoverage() - b.meanCoverage()) > t.getMaxCoverageShift()) {
			out.add(warning("COVERAGE_SHIFT", "mean real-weight coverage " + round(b.meanCoverage()) + " -> "
					+ round(s.meanCoverage()) + " (max " + t.getMaxCoverageShift() + ")"));
		}
		dropped("RATED_DROP", "rated products", b.rated(), s.rated(), t.getMaxCountDrop()).ifPresent(out::add);
		dropped("SCORED_DROP", "scored products", b.scored(), s.scored(), t.getMaxCountDrop()).ifPresent(out::add);
		dropped("EPREL_DROP", "EPREL-matched products", b.eprelMatched(), s.eprelMatched(), t.getMaxEprelDrop())
				.ifPresent(out::add);
		return out;
	}

	private static Optional<Violation> dropped(String rule, String what, int before, int now, double max) {
		if (before > 0 && (before - now) / (double) before > max) {
			return Optional.of(warning(rule, what + " " + before + " -> " + now + " (max drop "
					+ Math.round(max * 100) + " %)"));
		}
		return Optional.empty();
	}

	/**
	 * Builds the aggregates of a batch.
	 *
	 * @param scored the products handed to scoring
	 * @return the snapshot
	 */
	static Snapshot snapshot(Collection<Product> scored) {
		List<Double> values = new ArrayList<>();
		double coverageSum = 0.0;
		int coverageCount = 0;
		int eprel = 0;
		for (Product p : scored) {
			if (p.getEprelDatas() != null) {
				eprel++;
			}
			Score score = p.ecoscore();
			if (score != null && !Boolean.TRUE.equals(score.getVirtual()) && score.getValue() != null
					&& !score.getValue().isNaN()) {
				values.add(score.getValue());
			}
			ImpactScoreMeta meta = p.getImpactScoreMeta();
			if (meta != null && meta.getRealWeightCoverage() != null) {
				coverageSum += meta.getRealWeightCoverage();
				coverageCount++;
			}
		}
		values.sort(Double::compare);
		return new Snapshot(scored.size(), values.size(), percentile(values, 0.5), percentile(values, 0.05),
				percentile(values, 0.95), coverageCount == 0 ? null : coverageSum / coverageCount, eprel);
	}

	/** Linear-interpolated percentile of a sorted list, null when empty. */
	static Double percentile(List<Double> sorted, double q) {
		if (sorted.isEmpty()) {
			return null;
		}
		double pos = q * (sorted.size() - 1);
		int lo = (int) Math.floor(pos);
		int hi = (int) Math.ceil(pos);
		return sorted.get(lo) + (sorted.get(hi) - sorted.get(lo)) * (pos - lo);
	}

	private void publish(Report r) {
		for (Violation v : r.violations()) {
			if (v.severity() == Severity.BLOCKING) {
				logger.error("impactscore_guard vertical={} severity={} rule={} message=\"{}\" action=indexation_blocked",
						r.vertical(), v.severity(), v.rule(), v.message());
			} else {
				logger.error("impactscore_guard vertical={} severity={} rule={} message=\"{}\" action=none",
						r.vertical(), v.severity(), v.rule(), v.message());
			}
		}
		if (meterRegistry != null) {
			for (Severity sev : Severity.values()) {
				long count = r.violations().stream().filter(v -> v.severity() == sev).count();
				gauges.computeIfAbsent(r.vertical() + "|" + sev, k -> {
					AtomicInteger g = new AtomicInteger();
					meterRegistry.gauge("impactscore.guard.violations",
							Tags.of("vertical", r.vertical(), "severity",
									sev.name().toLowerCase()),
							g);
					return g;
				}).set((int) count);
			}
		}
	}

	@Override
	public Health health() {
		boolean blocking = false;
		boolean warning = false;
		Health.Builder builder = Health.up();
		for (Report r : lastReports.values()) {
			if (r.violations().isEmpty()) {
				continue;
			}
			blocking |= r.blocking();
			warning = true;
			builder.withDetail(r.vertical(), r.violations().stream()
					.map(v -> v.severity() + " " + v.rule() + ": " + v.message()).toList());
		}
		builder.withDetail("verticalsChecked", lastReports.size());
		if (blocking) {
			builder.status(STATUS_ALERT);
		} else if (warning) {
			builder.status(STATUS_WARN);
		}
		return builder.build();
	}

	private static Violation blocking(String rule, String message) {
		return new Violation(Severity.BLOCKING, rule, message);
	}

	private static Violation warning(String rule, String message) {
		return new Violation(Severity.WARNING, rule, message);
	}

	private static String sample(List<String> items) {
		return String.join(", ", items.subList(0, Math.min(3, items.size())));
	}

	private static double round(double d) {
		return Math.round(d * 100.0) / 100.0;
	}

	@Override
	public String toString() {
		return "ImpactScoreBatchGuard[" + Objects.toString(previous.keySet()) + "]";
	}
}
