package org.open4goods.model.impactscore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;

class ImpactScoreSnapshotBuilderTest {

	private static final Instant AT = Instant.parse("2026-11-01T14:00:00Z");

	private static VerticalConfig vertical() {
		ImpactScoreConfig config = new ImpactScoreConfig();
		config.setCriteriasPonderation(Map.of("WARRANTY", 1.0, "REPAIR", 1.0));
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("tv");
		vertical.setImpactScoreConfig(config);
		return vertical;
	}

	private static Score score(String name, double relative) {
		Score s = new Score();
		s.setName(name);
		s.setVirtual(false);
		Cardinality c = new Cardinality();
		c.setValue(relative);
		s.setRelativ(c);
		return s;
	}

	private static Product rated(long gtin, double relative5, double coverage) {
		Product p = new Product(gtin);
		p.getScores().put("ECOSCORE", score("ECOSCORE", relative5));
		p.getScores().put("WARRANTY", score("WARRANTY", 3.0));
		ImpactScoreMeta meta = new ImpactScoreMeta();
		meta.setStatus("RATED");
		meta.setRealWeightCoverage(coverage);
		p.setImpactScoreMeta(meta);
		return p;
	}

	private static List<Product> cohort() {
		List<Product> products = new ArrayList<>(List.of(rated(1, 5.0, 1.0), rated(2, 2.5, 0.5), rated(3, 0.0, 0.5)));
		Product low = new Product(4L);
		ImpactScoreMeta meta = new ImpactScoreMeta();
		meta.setStatus("NOT_RATED");
		meta.setNotRatedReason("LOW_COVERAGE");
		low.setImpactScoreMeta(meta);
		products.add(low);
		Product excluded = new Product(5L);
		excluded.setExcluded(true);
		excluded.setExcludedCauses(Set.of("NO_EPREL"));
		excluded.setEprelDatas(new EprelProduct());
		products.add(excluded);
		return products;
	}

	@Test
	void countsPercentilesCoverageAndTop() {
		ImpactScoreSnapshot s = ImpactScoreSnapshotBuilder.build(vertical(), cohort(), YearMonth.of(2026, 11), AT);

		assertThat(s.id()).isEqualTo("tv_2026-11");
		assertThat(s.snapshotAt()).isEqualTo(AT.toEpochMilli());
		assertThat(s.cohort()).isEqualTo(5);
		assertThat(s.rated()).isEqualTo(3);
		assertThat(s.notRated()).isEqualTo(1);
		assertThat(s.notRatedByReason()).containsExactly(Map.entry("LOW_COVERAGE", 1));
		assertThat(s.excluded()).isEqualTo(1);
		assertThat(s.excludedByCause()).containsExactly(Map.entry("NO_EPREL", 1));
		assertThat(s.scorePercentiles()).containsEntry("p0", 0.0).containsEntry("p50", 10.0).containsEntry("p100", 20.0);
		assertThat(s.avgRealWeightCoverage()).isEqualTo((1.0 + 0.5 + 0.5) / 3);
		// assessed = 4 (3 rated + 1 not rated, the excluded one is not assessed); WARRANTY filled on 3 of them
		assertThat(s.criteriaCoverage()).containsEntry("WARRANTY", 0.75).containsEntry("REPAIR", 0.0);
		assertThat(s.eprelMatched()).isEqualTo(1);
		assertThat(s.eprelMatchedShare()).isEqualTo(0.2);
		assertThat(s.top()).extracting(ImpactScoreSnapshot.TopEntry::gtin).containsExactly("1", "2", "3");
		assertThat(s.top().get(0).score()).isEqualTo(20.0);
	}

	@Test
	void topIsLimitedToTen() {
		List<Product> products = new ArrayList<>();
		for (long i = 1; i <= 15; i++) {
			products.add(rated(i, 1.0 + i / 10.0, 1.0));
		}
		ImpactScoreSnapshot s = ImpactScoreSnapshotBuilder.build(vertical(), products, YearMonth.of(2026, 11), AT);
		assertThat(s.top()).hasSize(10);
		assertThat(s.top().get(0).gtin()).isEqualTo("15");
	}

	@Test
	void emptyCohortKeepsAbsenceAbsent() {
		ImpactScoreSnapshot s = ImpactScoreSnapshotBuilder.build(vertical(), List.of(), YearMonth.of(2026, 11), AT);
		assertThat(s.cohort()).isZero();
		assertThat(s.scorePercentiles()).isEmpty();
		assertThat(s.avgRealWeightCoverage()).isNull();
		assertThat(s.eprelMatchedShare()).isNull();
		assertThat(s.criteriaCoverage()).isEmpty();
		assertThat(s.top()).isEmpty();
	}

	@Test
	void sameInputGivesSameIdentity() {
		ImpactScoreSnapshot a = ImpactScoreSnapshotBuilder.build(vertical(), cohort(), YearMonth.of(2026, 11), AT);
		ImpactScoreSnapshot b = ImpactScoreSnapshotBuilder.build(vertical(), cohort(), YearMonth.of(2026, 11), AT.plusSeconds(60));
		assertThat(b.id()).isEqualTo(a.id());
		assertThat(ImpactScoreSnapshotBuilder.monthOf(AT)).isEqualTo(YearMonth.of(2026, 11));
	}
}
