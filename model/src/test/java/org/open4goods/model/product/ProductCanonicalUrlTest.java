package org.open4goods.model.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;

class ProductCanonicalUrlTest {

	private static final long GTIN = 8690842771118L;

	@Test
	void productInVerticalUsesVerticalHomeUrlAndSlug() {
		Product product = product("washing-machine", "8690842771118-lave-linge-beko");
		VerticalConfig washingMachines = vertical("washing-machine", "lave-linges", "washing-machines");

		assertThat(ProductCanonicalUrl.fullSlug(product, washingMachines, DomainLanguage.fr))
				.isEqualTo("/lave-linges/8690842771118-lave-linge-beko");
		assertThat(ProductCanonicalUrl.fullSlug(product, washingMachines, DomainLanguage.en))
				.isEqualTo("/washing-machines/8690842771118-lave-linge-beko");
	}

	@Test
	void productWithoutVerticalUsesRootSlug() {
		Product product = product(null, "8690842771118-lave-linge-beko");

		assertThat(ProductCanonicalUrl.fullSlug(product, null, DomainLanguage.fr))
				.isEqualTo("/8690842771118-lave-linge-beko");
		// A vertical config passed for a product without vertical is ignored.
		assertThat(ProductCanonicalUrl.fullSlug(product, vertical("dishwasher", "lave-vaisselles", null),
				DomainLanguage.fr))
				.isEqualTo("/8690842771118-lave-linge-beko");
	}

	@Test
	void productThatChangedVerticalFollowsItsCurrentVertical() {
		Product product = product("washing-machine", "8690842771118-lave-linge-beko");
		VerticalConfig dishwashers = vertical("dishwasher", "lave-vaisselles", "dishwashers");

		product.setVertical("dishwasher");

		assertThat(ProductCanonicalUrl.fullSlug(product, dishwashers, DomainLanguage.fr))
				.isEqualTo("/lave-vaisselles/8690842771118-lave-linge-beko");
	}

	@Test
	void renamedSlugFollowsCurrentNamesUrl() {
		Product product = product("tv", "8806094831603-television-samsung-qe32ls03cbu");
		VerticalConfig tvs = vertical("tv", "televiseurs", "tvs");

		product.getNames().getUrl().put("fr", "8806094831603-smart-tv-qe32ls03cbu-32-pouces-full-hd-qledlcd");

		assertThat(ProductCanonicalUrl.fullSlug(product, tvs, DomainLanguage.fr))
				.isEqualTo("/televiseurs/8806094831603-smart-tv-qe32ls03cbu-32-pouces-full-hd-qledlcd");
	}

	@Test
	void slugFallsBackToDefaultThenToAnyValue() {
		Product product = new Product(GTIN);
		product.setVertical("tv");
		product.getNames().getUrl().put("default", "default-slug");
		product.getNames().getUrl().put("de", "german-slug");

		assertThat(ProductCanonicalUrl.slug(product, DomainLanguage.fr)).isEqualTo("default-slug");

		product.getNames().getUrl().remove("default");
		assertThat(ProductCanonicalUrl.slug(product, DomainLanguage.fr)).isEqualTo("german-slug");
	}

	@Test
	void missingSlugHasNoCanonicalPath() {
		Product product = new Product(GTIN);
		product.setVertical("tv");

		assertThat(ProductCanonicalUrl.fullSlug(product, vertical("tv", "televiseurs", null), DomainLanguage.fr))
				.isNull();
		assertThat(ProductCanonicalUrl.fullSlug(null, null, DomainLanguage.fr)).isNull();
	}

	@Test
	void pathTrimsSlashesAndWhitespace() {
		assertThat(ProductCanonicalUrl.path(" /slug/ ", "/televiseurs/")).isEqualTo("/televiseurs/slug");
		assertThat(ProductCanonicalUrl.path("slug", " ")).isEqualTo("/slug");
		assertThat(ProductCanonicalUrl.path(" ", "televiseurs")).isNull();
	}

	@Test
	void verticalHomeUrlSkipsBlankLocalisedValue() {
		VerticalConfig config = vertical("tv", " ", "tvs");

		assertThat(ProductCanonicalUrl.verticalHomeUrl(config, DomainLanguage.fr)).isEqualTo("tvs");
	}

	private static Product product(String vertical, String frSlug) {
		Product product = new Product(GTIN);
		product.setVertical(vertical);
		product.getNames().getUrl().put("fr", frSlug);
		return product;
	}

	private static VerticalConfig vertical(String id, String frHomeUrl, String defaultHomeUrl) {
		VerticalConfig config = new VerticalConfig();
		config.setId(id);
		Map<String, ProductI18nElements> i18n = new HashMap<>();
		i18n.put("fr", i18n(frHomeUrl));
		if (defaultHomeUrl != null) {
			i18n.put("default", i18n(defaultHomeUrl));
		}
		config.setI18n(i18n);
		return config;
	}

	private static ProductI18nElements i18n(String verticalHomeUrl) {
		ProductI18nElements elements = new ProductI18nElements();
		elements.setVerticalHomeUrl(verticalHomeUrl);
		return elements;
	}
}
