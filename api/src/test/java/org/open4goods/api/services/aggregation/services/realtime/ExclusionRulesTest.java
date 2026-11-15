package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * GOU-1881: exclusion rules, with the EPREL filter configurable per vertical (V2 only). GOU-2165: without EPREL, a
 * product enters only when its coverage reaches the V2 threshold; EPREL categories excluded per vertical.
 */
class ExclusionRulesTest {

	/** V2 matrix of the tests: {@link #HEAVY} 0.6, {@link #LIGHT} 0.4, threshold 0.6. */
	private static final String HEAVY = "HEAVY";
	private static final String LIGHT = "LIGHT";
	private static final double THRESHOLD = 0.6;

	private static VerticalConfig vertical(int methodologyVersion, Boolean eprelRequired) {
		return vertical(methodologyVersion, eprelRequired, List.of());
	}

	private static VerticalConfig vertical(int methodologyVersion, Boolean eprelRequired, List<String> excludedCategories) {
		VerticalConfig config = new VerticalConfig();
		config.setRequiredAttributes(Set.of("required_attr"));
		AttributesConfig attributesConfig = new AttributesConfig();
		attributesConfig.setConfigs(List.of(criterion(HEAVY), criterion(LIGHT)));
		config.setAttributesConfig(attributesConfig);
		config.getImpactScoreConfig().setCriteriasPonderation(Map.of(HEAVY, 0.5, LIGHT, 0.5));
		ImpactScoreV2Config v2 = new ImpactScoreV2Config();
		v2.setEprelRequired(eprelRequired);
		v2.setCriteriasPonderation(Map.of(HEAVY, 0.6, LIGHT, 0.4));
		v2.setMinRealWeightCoverage(THRESHOLD);
		v2.setExcludedEprelCategories(excludedCategories);
		config.getImpactScoreConfig().setV2(v2);
		config.getImpactScoreConfig().setMethodologyVersion(methodologyVersion);
		return config;
	}

	private static AttributeConfig criterion(String key) {
		AttributeConfig attribute = new AttributeConfig();
		attribute.setKey(key);
		attribute.setAsScore(true);
		attribute.setFilteringType(AttributeType.NUMERIC);
		return attribute;
	}

	/** A product with brand, model, EPREL and required attribute as asked, and the {@link #HEAVY} criterion. */
	private static Product product(boolean brand, boolean model, boolean eprel, boolean attr) {
		return with(bare(brand, model, eprel, attr), HEAVY, "10");
	}

	private static Product bare(boolean brand, boolean model, boolean eprel, boolean attr) {
		Product product = new Product(1L);
		if (brand) {
			product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "Brand");
		}
		if (model) {
			product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "ModelX");
		}
		if (eprel) {
			product.setEprelDatas(new EprelProduct());
		}
		if (attr) {
			ProductAttribute attribute = new ProductAttribute();
			attribute.setName("required_attr");
			attribute.setValue("v");
			product.getAttributes().getAll().put("required_attr", attribute);
		}
		return product;
	}

	private static Product with(Product product, String key, String value) {
		product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, value));
		return product;
	}

	private static Product withCategory(String category) {
		Product product = product(true, true, true, true);
		product.getEprelDatas().setEprelCategories(category == null ? null : List.of(category));
		return product;
	}

	@Test
	void v2WithEprelNotRequiredKeepsProductWithoutEprel() {
		ExclusionRules.Result r = ExclusionRules.evaluate(product(true, true, false, true), vertical(2, false));
		assertThat(r.excluded()).isFalse();
		assertThat(r.excludedCauses()).isEmpty();
	}

	@Test
	void v2WithEprelRequiredExcludesProductWithoutEprel() {
		ExclusionRules.Result r = ExclusionRules.evaluate(product(true, true, false, true), vertical(2, true));
		assertThat(r.excluded()).isTrue();
		assertThat(r.excludedCauses()).containsExactly("missing_eprel");
	}

	@Test
	void v1IgnoresEprelRequiredFalse() {
		ExclusionRules.Result r = ExclusionRules.evaluate(product(true, true, false, true), vertical(1, false));
		assertThat(r.excludedCauses()).containsExactly("missing_eprel");
	}

	@Test
	void brandModelAndAttributeStayRequiredWhenEprelIsNotRequired() {
		ExclusionRules.Result r = ExclusionRules.evaluate(product(false, false, false, false), vertical(2, false));
		assertThat(r.excluded()).isTrue();
		assertThat(r.excludedCauses()).containsExactly("missing_brand", "missing_model", "missing_attr_required_attr");
	}

	@Test
	void missingBrandAloneExcludesWhenEprelIsNotRequired() {
		assertThat(ExclusionRules.evaluate(product(false, true, false, true), vertical(2, false)).excludedCauses())
				.containsExactly("missing_brand");
	}

	@Test
	void missingModelAloneExcludesWhenEprelIsRequired() {
		assertThat(ExclusionRules.evaluate(product(true, false, true, true), vertical(2, true)).excludedCauses())
				.containsExactly("missing_model");
	}

	@Test
	void allCausesWhenEverythingIsMissingAndEprelRequired() {
		assertThat(ExclusionRules.evaluate(product(false, false, false, false), vertical(2, true)).excludedCauses())
				.containsExactly("missing_brand", "missing_model", "missing_eprel", "missing_attr_required_attr");
	}

	@Test
	void completeProductIsNotExcluded() {
		assertThat(ExclusionRules.evaluate(product(true, true, true, true), vertical(1, null)).excluded()).isFalse();
	}

	@Test
	void productWithoutVerticalUsesDefaultConfigurationAndKeepsEprelFilter() {
		ExclusionRules.Result r = ExclusionRules.evaluate(product(true, true, false, true), null);
		assertThat(r.excluded()).isTrue();
		assertThat(r.excludedCauses()).containsExactly("missing_eprel");
		assertThat(ExclusionRules.evaluate(product(true, true, false, true), new VerticalConfig()).excludedCauses())
				.containsExactly("missing_eprel");
	}

	@Test
	void v2WithoutEprelBelowTheThresholdIsExcluded() {
		ExclusionRules.Result r = ExclusionRules.evaluate(with(bare(true, true, false, true), LIGHT, "3"),
				vertical(2, false));
		assertThat(r.excluded()).isTrue();
		assertThat(r.excludedCauses()).containsExactly("missing_eprel_low_coverage");
	}

	@Test
	void v2WithoutEprelNorCriterionIsExcluded() {
		assertThat(ExclusionRules.evaluate(bare(true, true, false, true), vertical(2, false)).excludedCauses())
				.containsExactly("missing_eprel_low_coverage");
	}

	@Test
	void v2WithoutEprelThresholdIsInclusive() {
		// HEAVY alone: C = 0.6, exactly the threshold
		assertThat(ExclusionRules.evaluate(product(true, true, false, true), vertical(2, false)).excluded()).isFalse();
		assertThat(ExclusionRules.evaluate(with(product(true, true, false, true), LIGHT, "3"), vertical(2, false))
				.excluded()).isFalse();
	}

	@Test
	void v2WithoutEprelCountsOnlyReadableValues() {
		Product unreadable = with(bare(true, true, false, true), HEAVY, "n/a");
		assertThat(ExclusionRules.evaluate(with(unreadable, LIGHT, "3"), vertical(2, false)).excludedCauses())
				.containsExactly("missing_eprel_low_coverage");
	}

	@Test
	void productWithEprelIsNotConcernedByTheCoverageRule() {
		assertThat(ExclusionRules.evaluate(bare(true, true, true, true), vertical(2, false)).excluded()).isFalse();
		assertThat(ExclusionRules.evaluate(bare(true, true, true, true), vertical(2, true)).excluded()).isFalse();
	}

	@Test
	void v1KeepsTheEprelFilterWhateverTheCoverage() {
		Product rated = with(product(true, true, false, true), LIGHT, "3");
		assertThat(ExclusionRules.evaluate(rated, vertical(1, false)).excludedCauses()).containsExactly("missing_eprel");
		assertThat(ExclusionRules.evaluate(bare(true, true, false, true), vertical(1, false)).excludedCauses())
				.containsExactly("missing_eprel");
	}

	@Test
	void v2ExcludesTheConfiguredEprelCategoriesIgnoringCase() {
		VerticalConfig tv = vertical(2, true, List.of("MONITOR", "professional_display", "SIGNAGE"));
		assertThat(ExclusionRules.evaluate(withCategory("MONITOR"), tv).excludedCauses())
				.containsExactly("eprel_category_MONITOR");
		assertThat(ExclusionRules.evaluate(withCategory("PROFESSIONAL_DISPLAY"), tv).excludedCauses())
				.containsExactly("eprel_category_PROFESSIONAL_DISPLAY");
		assertThat(ExclusionRules.evaluate(withCategory("signage"), tv).excludedCauses())
				.containsExactly("eprel_category_SIGNAGE");
	}

	@Test
	void v2KeepsTelevisionsOtherAndBlankEprelCategories() {
		VerticalConfig tv = vertical(2, true, List.of("MONITOR", "PROFESSIONAL_DISPLAY", "SIGNAGE"));
		assertThat(ExclusionRules.evaluate(withCategory("TELEVISION"), tv).excluded()).isFalse();
		assertThat(ExclusionRules.evaluate(withCategory("OTHER"), tv).excluded()).isFalse();
		assertThat(ExclusionRules.evaluate(withCategory(null), tv).excluded()).isFalse();
		assertThat(ExclusionRules.evaluate(withCategory(" "), tv).excluded()).isFalse();
	}

	@Test
	void v1IgnoresTheExcludedEprelCategories() {
		VerticalConfig tv = vertical(1, true, List.of("MONITOR"));
		assertThat(ExclusionRules.evaluate(withCategory("MONITOR"), tv).excluded()).isFalse();
	}
}
