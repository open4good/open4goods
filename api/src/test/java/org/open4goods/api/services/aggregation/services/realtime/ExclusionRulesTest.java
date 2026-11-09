package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * GOU-1881: exclusion rules, with the EPREL filter configurable per vertical (V2 only).
 */
class ExclusionRulesTest {

	private static VerticalConfig vertical(int methodologyVersion, Boolean eprelRequired) {
		VerticalConfig config = new VerticalConfig();
		config.setRequiredAttributes(Set.of("required_attr"));
		ImpactScoreV2Config v2 = new ImpactScoreV2Config();
		v2.setEprelRequired(eprelRequired);
		config.getImpactScoreConfig().setV2(v2);
		config.getImpactScoreConfig().setMethodologyVersion(methodologyVersion);
		return config;
	}

	private static Product product(boolean brand, boolean model, boolean eprel, boolean attr) {
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
}
