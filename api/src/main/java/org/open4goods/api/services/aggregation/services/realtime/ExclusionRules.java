package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.services.batch.scores.ImpactScoreCoverage;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Pure rule deciding whether a product is excluded from the catalogue scoring.
 *
 * <p>Brand and model are always required. The EPREL association is required only when the vertical's
 * {@link ImpactScoreConfig#isEprelRequired()} says so (always {@code true} in V1). When it is not, a product without
 * EPREL data stays only if its coverage C, read from its attributes by {@link ImpactScoreCoverage}, reaches the
 * threshold of the vertical: entry only when the product can be rated (GOU-2165). A product whose EPREL category is
 * one of {@link ImpactScoreConfig#excludedEprelCategories()} (V2 only) is excluded. Required vertical attributes are
 * checked as before. Without vertical configuration the default one applies: the EPREL filter is kept.
 *
 * <p>The rule reads the product's attributes and EPREL data, never its scores: the batch clears the scores of an
 * excluded product, so a rule on them would keep it excluded, or switch it at every realtime and batch pass.
 * Design: {@code docs/impactscore/v2-design.md}, section 3.
 */
public final class ExclusionRules {

	/** Cause: no brand. */
	public static final String MISSING_BRAND = "missing_brand";
	/** Cause: no model. */
	public static final String MISSING_MODEL = "missing_model";
	/** Cause: no EPREL association. */
	public static final String MISSING_EPREL = "missing_eprel";
	/** Cause: no EPREL association, and a coverage below the V2 threshold (GOU-2165). */
	public static final String MISSING_EPREL_LOW_COVERAGE = "missing_eprel_low_coverage";
	/** Prefix of the cause for an EPREL category excluded from the vertical, followed by the category (GOU-2165). */
	public static final String EPREL_CATEGORY_PREFIX = "eprel_category_";
	/** Prefix of the cause for a missing required attribute. */
	public static final String MISSING_ATTR_PREFIX = "missing_attr_";

	private ExclusionRules() {
	}

	/**
	 * Result of the evaluation.
	 *
	 * @param excluded       {@code true} when at least one cause applies
	 * @param excludedCauses the causes, in a stable order
	 */
	public record Result(boolean excluded, Set<String> excludedCauses) {
	}

	/**
	 * Evaluates the exclusion rules. Does not modify the product.
	 *
	 * @param product the product
	 * @param vConf   the vertical configuration, may be {@code null} (default configuration)
	 * @return the exclusion state and its causes
	 */
	public static Result evaluate(Product product, VerticalConfig vConf) {
		Set<String> causes = new LinkedHashSet<>();

		if (StringUtils.isEmpty(product.brand())) {
			causes.add(MISSING_BRAND);
		}
		if (StringUtils.isEmpty(product.model())) {
			causes.add(MISSING_MODEL);
		}

		ImpactScoreConfig impactScoreConfig = vConf == null ? null : vConf.getImpactScoreConfig();
		boolean eprelRequired = impactScoreConfig == null || impactScoreConfig.isEprelRequired();
		if (product.getEprelDatas() == null) {
			if (eprelRequired) {
				causes.add(MISSING_EPREL);
			} else if (ImpactScoreCoverage.notRatedReasonFromAttributes(product, vConf) != null) {
				causes.add(MISSING_EPREL_LOW_COVERAGE);
			}
		} else if (impactScoreConfig != null) {
			String category = excludedEprelCategory(product.getEprelDatas(), impactScoreConfig);
			if (category != null) {
				causes.add(EPREL_CATEGORY_PREFIX + category);
			}
		}

		if (vConf != null && vConf.getRequiredAttributes() != null) {
			Set<String> missing = new HashSet<>(vConf.getRequiredAttributes());
			missing.removeAll(product.getAttributes().getattributesAsStringKeys());
			missing.stream().sorted().forEach(e -> causes.add(MISSING_ATTR_PREFIX + e));
		}

		return new Result(!causes.isEmpty(), causes);
	}

	/**
	 * The EPREL category of a product when the vertical excludes it, compared ignoring case.
	 *
	 * @return the category, upper case, {@code null} when it is blank or not excluded
	 */
	@SuppressWarnings("deprecation")
	private static String excludedEprelCategory(EprelProduct eprel, ImpactScoreConfig impactScoreConfig) {
		String category = StringUtils.trimToNull(eprel.getEprelCategory());
		if (category == null) {
			return null;
		}
		for (String excluded : impactScoreConfig.excludedEprelCategories()) {
			if (category.equalsIgnoreCase(StringUtils.trim(excluded))) {
				return category.toUpperCase(Locale.ROOT);
			}
		}
		return null;
	}
}
