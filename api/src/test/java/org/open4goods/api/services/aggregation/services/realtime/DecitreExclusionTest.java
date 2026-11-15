package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-2264 (GOU-2262): the Decitre keys are removed from a product without vertical, with the real default config.
 */
class DecitreExclusionTest {

	private static final List<String> DECITRE_KEYS = List.of("CLASSIFICATION DECITRE 1", "CLASSIFICATION DECITRE 2",
			"CLASSIFICATION DECITRE 3", "ATTRIBUTS", "LIVRES ORIGINE", "PROPRIETAIRE", "PROPRIÉTAIRE");
	private static final List<String> KEPT_KEYS = List.of("EDITEUR", "FORMAT", "NB DE PAGES");

	private static VerticalsConfigService configService;

	@BeforeAll
	static void load() {
		configService = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
				new PathMatchingResourcePatternResolver());
	}

	@Test
	void decitreKeysAreRemovedFromAProductWithoutVertical() throws Exception {
		// Product without vertical: the default config is the one applied
		VerticalConfig config = configService.getConfigByIdOrDefault(null);
		AttributeRealtimeAggregationService service = new AttributeRealtimeAggregationService(configService,
				mock(BrandService.class), LoggerFactory.getLogger(DecitreExclusionTest.class),
				mock(IcecatFeatureResolver.class));

		Product product = new Product(9782017148654L);
		DECITRE_KEYS.forEach(k -> put(product, k));
		KEPT_KEYS.forEach(k -> put(product, k));

		service.onProduct(product, config);

		assertThat(product.getAttributes().getAll().keySet()).doesNotContainAnyElementsOf(DECITRE_KEYS);
		assertThat(product.getAttributes().getAll().keySet()).containsAll(KEPT_KEYS);
		KEPT_KEYS.forEach(k -> assertThat(product.getAttributes().getAll().get(k).getValue()).isEqualTo("v-" + k));
	}

	private static void put(Product product, String key) {
		ProductAttribute attribute = new ProductAttribute();
		attribute.setName(key);
		attribute.setValue("v-" + key);
		product.getAttributes().getAll().put(key, attribute);
	}
}
