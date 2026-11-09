package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.ImpactScoreGuardProperties;
import org.open4goods.api.services.ImpactScoreBatchGuard.Report;
import org.open4goods.api.services.ImpactScoreBatchGuard.Violation;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.boot.health.contributor.Status;

/**
 * Rules of {@link ImpactScoreBatchGuard} (GOU-1895), one test per rule, plus the capped "air conditioners" batch.
 */
class ImpactScoreBatchGuardTest {

	private final ImpactScoreBatchGuard guard = new ImpactScoreBatchGuard(new ImpactScoreGuardProperties(), null);

	@Test
	void healthyBatchRaisesNothing() {
		Report report = guard.verify("tv", cohort(100, i -> i * 0.19, 0.8));
		assertThat(report.violations()).isEmpty();
		assertThat(report.blocking()).isFalse();
	}

	@Test
	void scoreOutsideBoundsBlocks() {
		List<Product> cohort = cohort(100, i -> i * 0.19, 0.8);
		cohort.get(3).ecoscore().setValue(21.0);
		assertThat(rules(guard.verify("tv", cohort))).contains("BOUNDS");
		assertThat(guard.verify("tv", cohort).blocking()).isTrue();
	}

	@Test
	void relativDifferentFromValueOverFourBlocks() {
		List<Product> cohort = cohort(100, i -> i * 0.19, 0.8);
		cohort.get(3).ecoscore().getRelativ().setValue(4.9);
		assertThat(rules(guard.verify("tv", cohort))).contains("RELATIV");
	}

	@Test
	void compositeImpactScoreBlocks() {
		List<Product> cohort = cohort(100, i -> i * 0.19, 0.8);
		cohort.get(0).ecoscore().setAggregates(Map.of("GWP", 1.0));
		Report report = guard.verify("tv", cohort);
		assertThat(rules(report)).contains("COMPOSITE");
		assertThat(report.blocking()).isTrue();
	}

	@Test
	void compressedScaleBlocks() {
		Report report = guard.verify("tv", cohort(100, i -> 4.0 + i * 0.002, 0.8));
		assertThat(rules(report)).contains("SPREAD");
		assertThat(report.blocking()).isTrue();
	}

	@Test
	void smallCohortSkipsTheDistributionCheck() {
		assertThat(guard.verify("tv", cohort(5, i -> 4.0, 0.8)).violations()).isEmpty();
	}

	@Test
	void medianShiftWarnsWithoutBlocking() {
		guard.verify("tv", cohort(100, i -> i * 0.1, 0.8));
		Report report = guard.verify("tv", cohort(100, i -> 5.0 + i * 0.1, 0.8));
		assertThat(rules(report)).containsExactly("MEDIAN_SHIFT");
		assertThat(report.blocking()).isFalse();
	}

	@Test
	void coverageShiftWarns() {
		guard.verify("tv", cohort(100, i -> i * 0.19, 0.8));
		assertThat(rules(guard.verify("tv", cohort(100, i -> i * 0.19, 0.5)))).containsExactly("COVERAGE_SHIFT");
	}

	@Test
	void ratedAndScoredDropWarn() {
		guard.verify("tv", cohort(100, i -> i * 0.19, 0.8));
		Report report = guard.verify("tv", cohort(60, i -> i * 0.19 * 100 / 60, 0.8));
		assertThat(rules(report)).contains("RATED_DROP", "SCORED_DROP");
		assertThat(report.blocking()).isFalse();
	}

	@Test
	void eprelDropWarns() {
		List<Product> first = cohort(100, i -> i * 0.19, 0.8);
		first.forEach(p -> p.setEprelDatas(new EprelProduct()));
		guard.verify("tv", first);
		List<Product> second = cohort(100, i -> i * 0.19, 0.8);
		second.subList(0, 50).forEach(p -> p.setEprelDatas(new EprelProduct()));
		assertThat(rules(guard.verify("tv", second))).containsExactly("EPREL_DROP");
	}

	@Test
	void verticalOverrideReplacesTheDefault() {
		ImpactScoreGuardProperties props = new ImpactScoreGuardProperties();
		ImpactScoreGuardProperties.Thresholds override = new ImpactScoreGuardProperties.Thresholds();
		override.setMaxMedianShift(10.0);
		props.setVerticals(Map.of("tv", override));
		ImpactScoreBatchGuard custom = new ImpactScoreBatchGuard(props, null);
		custom.verify("tv", cohort(100, i -> i * 0.1, 0.8));
		assertThat(custom.verify("tv", cohort(100, i -> 5.0 + i * 0.1, 0.8)).violations()).isEmpty();
	}

	@Test
	void rejectedBatchDoesNotBecomeTheBaseline() {
		guard.verify("tv", cohort(100, i -> i * 0.19, 0.8));
		guard.verify("tv", cohort(100, i -> 4.0, 0.8)); // blocked
		assertThat(guard.verify("tv", cohort(100, i -> i * 0.19, 0.8)).violations()).isEmpty();
	}

	@Test
	void virtualImpactScoreDoesNotBlock() {
		List<Product> cohort = cohort(100, i -> i * 0.19, 0.8);
		Product virtual = cohort.get(7);
		virtual.ecoscore().setVirtual(true);
		virtual.ecoscore().getRelativ().setValue(2.5); // sigma-relativised, not value / 4
		Report report = guard.verify("tv", cohort);
		assertThat(report.violations()).isEmpty();
		assertThat(report.snapshot().rated()).isEqualTo(99);
	}

	@Test
	void referenceSurvivesARestart(@TempDir Path folder) {
		new ImpactScoreBatchGuard(new ImpactScoreGuardProperties(), null, folder).verify("tv",
				cohort(100, i -> i * 0.1, 0.8));
		ImpactScoreBatchGuard restarted = new ImpactScoreBatchGuard(new ImpactScoreGuardProperties(), null, folder);
		assertThat(rules(restarted.verify("tv", cohort(100, i -> 5.0 + i * 0.1, 0.8)))).containsExactly("MEDIAN_SHIFT");
	}

	@Test
	void unreadableReferenceIsIgnored(@TempDir Path folder) throws Exception {
		java.nio.file.Files.writeString(folder.resolve("tv.json"), "{not json");
		ImpactScoreBatchGuard g = new ImpactScoreBatchGuard(new ImpactScoreGuardProperties(), null, folder);
		assertThat(g.verify("tv", cohort(100, i -> i * 0.19, 0.8)).violations()).isEmpty();
	}

	@Test
	void healthExposesTheAlertWithoutBeingDown() {
		guard.verify("tv", cohort(100, i -> 4.0, 0.8));
		assertThat(guard.health().getStatus()).isEqualTo(ImpactScoreBatchGuard.STATUS_ALERT);
		assertThat(guard.health().getStatus()).isNotEqualTo(Status.DOWN);
	}

	@Test
	void cappedAirConditionersBatchIsNotIndexed() {
		// Defect B1: every score squashed under 5/20
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("air-conditioners");
		List<Product> capped = cohort(100, i -> 4.9 + i * 0.0005, 0.8);
		ProductRepository repository = mock(ProductRepository.class);
		when(repository.getProductsMatchingVerticalId(vertical)).thenAnswer(i -> capped.stream());
		AggregationFacadeService aggregation = mock(AggregationFacadeService.class);
		BatchService batch = new BatchService(aggregation, mock(CompletionFacadeService.class),
				mock(VerticalsConfigService.class), repository, mock(FeedIndexingService.class),
				mock(FeedService.class), mock(SerialisationService.class), mock(ResourceService.class),
				new ApiProperties());
		batch.batch(vertical);
		verify(repository, never()).addToFullindexationQueue(anyCollection());
		verify(repository, never()).applyNewerOfferStates(anyCollection());
	}

	@Test
	void healthyBatchIsIndexed() {
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("tv");
		List<Product> healthy = cohort(100, i -> i * 0.19, 0.8);
		ProductRepository repository = mock(ProductRepository.class);
		when(repository.getProductsMatchingVerticalId(vertical)).thenAnswer(i -> healthy.stream());
		BatchService batch = new BatchService(mock(AggregationFacadeService.class), mock(CompletionFacadeService.class),
				mock(VerticalsConfigService.class), repository, mock(FeedIndexingService.class),
				mock(FeedService.class), mock(SerialisationService.class), mock(ResourceService.class),
				new ApiProperties());
		batch.batch(vertical);
		verify(repository).addToFullindexationQueue(anyCollection());
	}

	private static List<String> rules(Report report) {
		return report.violations().stream().map(Violation::rule).toList();
	}

	private static List<Product> cohort(int size, IntToDoubleFunction value, double coverage) {
		List<Product> out = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			Product p = new Product(1000L + i);
			p.setVertical("tv");
			p.setOffersCount(1);
			double v = value.applyAsDouble(i);
			Score score = new Score("ECOSCORE", v);
			Cardinality relativ = new Cardinality();
			relativ.setValue(v / 4.0);
			score.setRelativ(relativ);
			p.getScores().put("ECOSCORE", score);
			ImpactScoreMeta meta = new ImpactScoreMeta();
			meta.setStatus(ImpactScoreMeta.Status.RATED.name());
			meta.setRealWeightCoverage(coverage);
			p.setImpactScoreMeta(meta);
			out.add(p);
		}
		return out;
	}
}
