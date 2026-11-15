package org.open4goods.api.services;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * EPREL match rate per vertical, measured after each batch (GOU-1893).
 *
 * <p>The population is the products with a brand, a model and at least one offer (the ones EPREL can be searched
 * for). Among them, those carrying EPREL data are split into matched by GTIN (the EPREL GTIN equals the product
 * GTIN) and matched by brand and model (every other match). The rate and its variation since the previous batch of
 * the same vertical (kept in memory: the first batch after a restart has no variation) are logged on one line and
 * published as Micrometer gauges {@code eprel.match.*{vertical}} (actuator {@code /actuator/metrics}).
 */
public class EprelMatchMetrics {

	private static final Logger logger = LoggerFactory.getLogger(EprelMatchMetrics.class);

	/**
	 * Counts of one batch.
	 *
	 * @param eligible products with brand, model and offer
	 * @param byGtin eligible products matched by GTIN
	 * @param byModel eligible products matched by brand and model
	 */
	public record Result(int eligible, int byGtin, int byModel) {

		/** @return all matched products */
		public int matched() {
			return byGtin + byModel;
		}

		/** @return matched / eligible, 0 when nothing is eligible */
		public double rate() {
			return eligible == 0 ? 0.0 : matched() / (double) eligible;
		}
	}

	private final MeterRegistry meterRegistry;
	private final Map<String, Result> previous = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

	/**
	 * @param meterRegistry optional registry, null to only log
	 */
	public EprelMatchMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Measures, logs and publishes the match of a vertical.
	 *
	 * @param verticalId the vertical id
	 * @param products the products of the batch
	 * @return the counts
	 */
	public Result record(String verticalId, Collection<Product> products) {
		Result result = measure(products);
		Result before = previous.put(verticalId, result);
		String variation = before == null ? "n/a"
				: String.format("%+.2f", (result.rate() - before.rate()) * 100);
		logger.info("eprel_match vertical={} eligible={} matched={} byGtin={} byModel={} rate={}% variationPts={}",
				verticalId, result.eligible(), result.matched(), result.byGtin(), result.byModel(),
				String.format("%.2f", result.rate() * 100), variation);
		publish(verticalId, result, before);
		return result;
	}

	/**
	 * @param products the products to look at
	 * @return the counts
	 */
	static Result measure(Collection<Product> products) {
		int eligible = 0;
		int byGtin = 0;
		int byModel = 0;
		for (Product p : products) {
			if (p.getOffersCount() == null || p.getOffersCount() <= 0 || StringUtils.isBlank(p.brand())
					|| StringUtils.isBlank(p.model())) {
				continue;
			}
			eligible++;
			EprelProduct eprel = p.getEprelDatas();
			if (eprel == null || eprel.isWithdrawn()) {
				continue;
			}
			if (matchesGtin(p.gtin(), eprel)) {
				byGtin++;
			} else {
				byModel++;
			}
		}
		return new Result(eligible, byGtin, byModel);
	}

	private static boolean matchesGtin(String gtin, EprelProduct eprel) {
		Long numeric = numeric(gtin);
		return numeric != null && (numeric.equals(eprel.getNumericGtin()) || numeric.equals(numeric(eprel.getGtinIdentifier())));
	}

	private static Long numeric(String value) {
		if (StringUtils.isBlank(value) || !value.trim().chars().allMatch(Character::isDigit)) {
			return null;
		}
		try {
			return Long.valueOf(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void publish(String verticalId, Result r, Result before) {
		if (meterRegistry == null) {
			return;
		}
		set("eligible", verticalId, r.eligible());
		set("matched", verticalId, r.matched());
		set("matched.gtin", verticalId, r.byGtin());
		set("matched.model", verticalId, r.byModel());
		// rates are stored in basis points (1 % = 100) so a long gauge is enough
		set("rate.bp", verticalId, Math.round(r.rate() * 10_000));
		set("rate.variation.bp", verticalId, before == null ? 0 : Math.round((r.rate() - before.rate()) * 10_000));
	}

	private void set(String name, String verticalId, long value) {
		gauges.computeIfAbsent(name + "|" + verticalId, k -> {
			AtomicLong g = new AtomicLong();
			meterRegistry.gauge("eprel.match." + name, Tags.of("vertical", verticalId), g);
			return g;
		}).set(value);
	}
}
