package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** Tests of {@link EprelMatchMetrics} (GOU-1893). */
class EprelMatchMetricsTest {

	private static Product product(long gtin, String brand, String model, int offers, EprelProduct eprel) {
		Product p = new Product(gtin);
		if (brand != null) {
			p.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, brand);
		}
		if (model != null) {
			p.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, model);
		}
		p.setOffersCount(offers);
		p.setEprelDatas(eprel);
		return p;
	}

	private static EprelProduct eprel(Long numericGtin) {
		EprelProduct e = new EprelProduct();
		e.setNumericGtin(numericGtin);
		return e;
	}

	@Test
	void countsEligibleProductsAndSplitsGtinFromModelMatches() {
		List<Product> products = List.of(
				product(1001L, "A", "m1", 2, eprel(1001L)), // by GTIN
				product(1002L, "A", "m2", 1, eprel(9L)), // by brand and model
				product(1003L, "A", "m3", 1, null), // eligible, unmatched
				product(1004L, "A", "m4", 0, eprel(1004L)), // no offer: out
				product(1005L, null, "m5", 3, eprel(1005L)), // no brand: out
				product(1006L, "A", null, 3, eprel(1006L))); // no model: out

		EprelMatchMetrics.Result r = EprelMatchMetrics.measure(products);

		assertThat(r.eligible()).isEqualTo(3);
		assertThat(r.byGtin()).isEqualTo(1);
		assertThat(r.byModel()).isEqualTo(1);
		assertThat(r.rate()).isEqualTo(2 / 3.0);
	}

	@Test
	void noEligibleProductGivesRateZeroWithoutFailing() {
		assertThat(EprelMatchMetrics.measure(List.of()).rate()).isZero();
	}

	@Test
	void publishesGaugesAndVariationSincePreviousBatch() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		EprelMatchMetrics metrics = new EprelMatchMetrics(registry);

		metrics.record("tv", List.of(product(1L, "A", "m", 1, null), product(2L, "A", "n", 1, eprel(2L))));
		assertThat(registry.get("eprel.match.rate.bp").tag("vertical", "tv").gauge().value()).isEqualTo(5000);
		assertThat(registry.get("eprel.match.rate.variation.bp").tag("vertical", "tv").gauge().value()).isZero();

		metrics.record("tv", List.of(product(1L, "A", "m", 1, eprel(1L)), product(2L, "A", "n", 1, eprel(2L))));
		assertThat(registry.get("eprel.match.rate.bp").tag("vertical", "tv").gauge().value()).isEqualTo(10000);
		assertThat(registry.get("eprel.match.rate.variation.bp").tag("vertical", "tv").gauge().value()).isEqualTo(5000);
		assertThat(registry.get("eprel.match.matched.gtin").tag("vertical", "tv").gauge().value()).isEqualTo(2);
	}
}
