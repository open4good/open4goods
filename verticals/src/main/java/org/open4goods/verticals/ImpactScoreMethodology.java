package org.open4goods.verticals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScorePositionLabel;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Rules of the ImpactScore methodology version of a vertical: parsing of the environment override
 * {@code impactscore.methodology-version-overrides} and validation of the version and of the {@code v2} block
 * (design {@code docs/impactscore/v2-design.md}, sections 1.2 and 5.2). Used at load time by
 * {@link VerticalsConfigService} and by {@code VerticalYamlValidationTest}.
 */
public final class ImpactScoreMethodology {

	/** Value of the override that forces nothing, so that a rendered override can be cleared. */
	public static final String NO_OVERRIDE = "none";

	/** Tolerance on the sum of the V2 matrix. */
	static final double WEIGHT_SUM_TOLERANCE = 1e-6;

	private static final String DATA_QUALITY = "DATA_QUALITY";
	private static final String ECOSCORE = "ECOSCORE";

	private ImpactScoreMethodology() {
	}

	/**
	 * Parses the override: comma-separated {@code <vertical>:<version>} pairs, for example {@code tv:2} or
	 * {@code tv:2,oven:1}. Blank or {@value #NO_OVERRIDE}: no override.
	 *
	 * @param raw the property value, may be {@code null}
	 * @return the forced version per vertical id, in declaration order
	 * @throws IllegalStateException when a pair is malformed, repeated, or forces a version outside {1, 2}
	 */
	public static Map<String, Integer> parseOverrides(String raw) {
		Map<String, Integer> overrides = new LinkedHashMap<>();
		if (StringUtils.isBlank(raw) || NO_OVERRIDE.equalsIgnoreCase(raw.trim())) {
			return overrides;
		}
		for (String pair : raw.split(",")) {
			String[] parts = pair.trim().split(":", -1);
			if (parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
				throw new IllegalStateException("impactscore.methodology-version-overrides: '" + pair.trim()
						+ "' is not <vertical>:<version>");
			}
			String vertical = parts[0].trim();
			int version;
			try {
				version = Integer.parseInt(parts[1].trim());
			} catch (NumberFormatException e) {
				throw new IllegalStateException("impactscore.methodology-version-overrides: version '" + parts[1].trim()
						+ "' of " + vertical + " is not a number", e);
			}
			if (!ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS.contains(version)) {
				throw new IllegalStateException("impactscore.methodology-version-overrides: version " + version + " of "
						+ vertical + " is not one of " + ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS);
			}
			if (overrides.put(vertical, version) != null) {
				throw new IllegalStateException("impactscore.methodology-version-overrides: " + vertical
						+ " is listed twice");
			}
		}
		return overrides;
	}

	/**
	 * Errors that make the effective methodology unusable: a declared or forced version outside {1, 2}, version 2
	 * without a {@code v2} block, or an invalid {@code v2} block while V2 is active. The service does not start with
	 * any of them.
	 *
	 * @param config   the ImpactScore configuration
	 * @param vertical the vertical, for the criteria check
	 * @return the errors, empty when the effective methodology is usable
	 */
	public static List<String> blockingErrors(ImpactScoreConfig config, VerticalConfig vertical) {
		List<String> errors = new ArrayList<>(versionErrors(config));
		if (errors.isEmpty() && config.isV2Active()) {
			if (config.getV2() == null) {
				errors.add("methodology version 2 requires a v2 block");
			} else {
				errors.addAll(v2Errors(config.getV2(), vertical));
			}
		}
		return errors;
	}

	/**
	 * Every rule of the design, section 1.2: {@link #blockingErrors} plus the validity of a {@code v2} block not yet
	 * active, so that a block merged dark is valid before the switch.
	 *
	 * @param config   the ImpactScore configuration
	 * @param vertical the vertical, for the criteria check
	 * @return the errors, empty when the configuration is valid
	 */
	public static List<String> validate(ImpactScoreConfig config, VerticalConfig vertical) {
		List<String> errors = new ArrayList<>(blockingErrors(config, vertical));
		if (!config.isV2Active() && config.getV2() != null) {
			errors.addAll(v2Errors(config.getV2(), vertical));
		}
		return errors;
	}

	private static List<String> versionErrors(ImpactScoreConfig config) {
		List<String> errors = new ArrayList<>();
		Integer declared = config.getMethodologyVersion();
		if (declared != null && !ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS.contains(declared)) {
			errors.add("methodologyVersion " + declared + " is not one of "
					+ ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS);
		}
		Integer forced = config.getMethodologyVersionOverride();
		if (forced != null && !ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS.contains(forced)) {
			errors.add("forced methodology version " + forced + " is not one of "
					+ ImpactScoreConfig.SUPPORTED_METHODOLOGY_VERSIONS);
		}
		return errors;
	}

	private static List<String> v2Errors(ImpactScoreV2Config v2, VerticalConfig vertical) {
		List<String> errors = new ArrayList<>();
		Map<String, Double> weights = v2.getCriteriasPonderation();
		if (weights == null || weights.isEmpty()) {
			errors.add("v2.criteriasPonderation is empty");
		} else {
			if (weights.values().stream().anyMatch(Objects::isNull)) {
				errors.add("v2.criteriasPonderation has a criterion without weight");
			}
			double sum = weights.values().stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
			if (Math.abs(sum - 1.0) > WEIGHT_SUM_TOLERANCE) {
				errors.add("v2.criteriasPonderation sums to " + sum + ", not 1");
			}
			if (weights.containsKey(DATA_QUALITY)) {
				errors.add("v2.criteriasPonderation cannot weight " + DATA_QUALITY);
			}
			if (weights.containsKey(ECOSCORE)) {
				errors.add("v2.criteriasPonderation cannot weight " + ECOSCORE);
			}
			for (String key : weights.keySet()) {
				if (DATA_QUALITY.equals(key) || ECOSCORE.equals(key)) {
					continue;
				}
				AttributeConfig attribute = vertical == null || vertical.getAttributesConfig() == null ? null
						: vertical.getAttributesConfig().getAttributeConfigByKey(key);
				if (attribute == null || !attribute.isAsScore()) {
					errors.add("v2.criteriasPonderation weights " + key + ", which is not an asScore attribute");
				} else if (attribute.isDerivedScore()) {
					errors.addAll(derivedCriterionErrors(attribute, weights.keySet(), vertical));
				}
			}
		}

		Double threshold = v2.getMinRealWeightCoverage();
		if (threshold == null || threshold <= 0.0 || threshold > 1.0) {
			errors.add("v2.minRealWeightCoverage must be in ]0, 1], got " + threshold);
		}

		Integer minRated = v2.getMinRatedForPositionLabel();
		if (minRated != null && minRated < 1) {
			errors.add("v2.minRatedForPositionLabel must be at least 1, got " + minRated);
		}

		List<ImpactScorePositionLabel> labels = v2.getPositionLabels();
		if (labels == null || labels.isEmpty()) {
			errors.add("v2.positionLabels is empty");
		} else {
			Double previous = null;
			for (ImpactScorePositionLabel label : labels) {
				if (label == null || StringUtils.isBlank(label.getLevel()) || label.getMinBetterThan() == null) {
					errors.add("v2.positionLabels has a label without level or minBetterThan");
					previous = null;
					continue;
				}
				double min = label.getMinBetterThan();
				if (min < 0.0 || min > 1.0) {
					errors.add("v2.positionLabels " + label.getLevel() + ": minBetterThan must be in [0, 1], got " + min);
				}
				if (previous != null && min >= previous) {
					errors.add("v2.positionLabels thresholds must strictly decrease, " + label.getLevel() + " has "
							+ min + " after " + previous);
				}
				previous = min;
			}
			ImpactScorePositionLabel last = labels.get(labels.size() - 1);
			if (last == null || last.getMinBetterThan() == null || last.getMinBetterThan() != 0.0) {
				errors.add("v2.positionLabels: the last threshold must be 0");
			}
		}
		return errors;
	}

	/**
	 * Errors of a derived criterion of the V2 matrix (GOU-2041): each source is an attribute of the vertical, not
	 * itself derived, and not weighted next to the criterion, which would count it twice.
	 */
	private static List<String> derivedCriterionErrors(AttributeConfig derived, Set<String> weighted,
			VerticalConfig vertical) {
		List<String> errors = new ArrayList<>();
		for (String source : derived.getScoreSources()) {
			AttributeConfig attribute = vertical.getAttributesConfig().getAttributeConfigByKey(source);
			if (attribute == null) {
				errors.add("v2 criterion " + derived.getKey() + " reads " + source + ", which is not an attribute");
			} else if (attribute.isDerivedScore()) {
				errors.add("v2 criterion " + derived.getKey() + " reads " + source + ", which is itself derived");
			}
			if (weighted.contains(source)) {
				errors.add("v2.criteriasPonderation weights both " + derived.getKey() + " and its source " + source);
			}
		}
		return errors;
	}
}
