package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Pure rule deciding whether a product is excluded from the catalogue scoring.
 *
 * <p>Brand and model are always required. The EPREL association is required only when the vertical's
 * {@link ImpactScoreConfig#isEprelRequired()} says so (always {@code true} in V1). Required vertical attributes
 * are checked as before. Without vertical configuration the default one applies: the EPREL filter is kept.
 * Design: {@code docs/impactscore/v2-design.md}, section 3.
 */
public final class ExclusionRules {

	/** Cause: no brand. */
	public static final String MISSING_BRAND = "missing_brand";
	/** Cause: no model. */
	public static final String MISSING_MODEL = "missing_model";
	/** Cause: no EPREL association. */
	public static final String MISSING_EPREL = "missing_eprel";
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
		if (eprelRequired && product.getEprelDatas() == null) {
			causes.add(MISSING_EPREL);
		}

		if (vConf != null && vConf.getRequiredAttributes() != null) {
			Set<String> missing = new HashSet<>(vConf.getRequiredAttributes());
			missing.removeAll(product.getAttributes().getattributesAsStringKeys());
			missing.stream().sorted().forEach(e -> causes.add(MISSING_ATTR_PREFIX + e));
		}

		return new Result(!causes.isEmpty(), causes);
	}
}
