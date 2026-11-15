package org.open4goods.api.services.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Planning of the {@code nature} backfill (GOU-2234) with the real markers of {@code _default.yml}: the written
 * nature, the idempotence, and the dry-run report per threshold.
 */
class NatureBackfillOperationTest {

	private static final String SPARE = "PIECES DETACHEES LAVE-LINGE > JOINT HUBLOT";

	private static VerticalsConfigService verticals;

	@BeforeAll
	static void load() {
		verticals = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
				new PathMatchingResourcePatternResolver());
	}

	private static NatureBackfillOperation operation(double threshold) {
		return new NatureBackfillOperation(verticals, threshold, null, 2);
	}

	@Test
	void setsTheComputedNatureWhenItDiffers() {
		BackfillChange change = operation(0).plan("1", Map.of("datasourceCategories", List.of(SPARE))).orElseThrow();

		assertThat(change.script()).isEqualTo(NatureBackfillOperation.NATURE_SCRIPT);
		assertThat(change.params()).containsEntry("nature", "SPARE_PART").containsEntry("categories", List.of(SPARE));
		assertThat(change.backup()).isNull();
		assertThat(change.labels()).containsExactly("nature=SPARE_PART");
	}

	@Test
	void productAlreadyRightIsNotWritten() {
		assertThat(operation(0).plan("1", Map.of("datasourceCategories", List.of(SPARE), "nature", "SPARE_PART")))
				.isEmpty();
		assertThat(operation(0).plan("2", Map.of("datasourceCategories", List.of("MAISON > DIVERS")))).isEmpty();
	}

	@Test
	void staleNatureIsRemoved() {
		BackfillChange change = operation(0)
				.plan("1", Map.of("datasourceCategories", List.of("MAISON > DIVERS"), "nature", "ACCESSORY"))
				.orElseThrow();

		assertThat(change.params()).containsEntry("nature", null);
		assertThat(change.labels()).containsExactly("nature=absent");
	}

	@Test
	void writtenNatureFollowsTheThreshold() {
		Map<String, Object> oneOfFour = Map.of("datasourceCategories",
				List.of(SPARE, "MAISON > DIVERS", "BRICOLAGE > OUTILLAGE", "LIVRES > ROMANS"));

		assertThat(operation(0).plan("1", oneOfFour)).isPresent();
		assertThat(operation(0.5).plan("1", oneOfFour)).isEmpty();
	}

	@Test
	void dryRunDetailsCountEachReportThresholdAndGiveExamples() {
		NatureBackfillOperation operation = operation(0);
		operation.plan("1", Map.of("datasourceCategories", List.of(SPARE)));
		operation.plan("2", Map.of("datasourceCategories",
				List.of(SPARE, "MAISON > DIVERS", "BRICOLAGE > OUTILLAGE", "LIVRES > ROMANS"), "offerNames",
				List.of("Joint de hublot pour lave-linge", "Joint")));
		operation.plan("3", Map.of("datasourceCategories", List.of("ACCESSOIRES LAVE-LINGE")));

		Map<String, Object> details = operation.details();

		assertThat(details).containsEntry("threshold", 0d).containsEntry("lastChangeDays", 2);
		assertThat(details.get("markedByThreshold")).isEqualTo(Map.of(
				"0.0", Map.of("SPARE_PART", 2L, "ACCESSORY", 1L),
				"0.5", Map.of("SPARE_PART", 1L, "ACCESSORY", 1L)));
		assertThat(details.get("examplesMarkedOnlyAtLowestThreshold")).asList().singleElement()
				.isEqualTo(Map.of("gtin", "2", "name", "Joint", "nature", "SPARE_PART", "datasourceCategories",
						List.of(SPARE, "MAISON > DIVERS", "BRICOLAGE > OUTILLAGE", "LIVRES > ROMANS")));
	}

	@Test
	void scopeIsTheLiveProductsWithoutVertical() {
		String query = operation(0).query().toString();

		assertThat(query).contains("lastChange").contains("offersCount").contains("datasourceCategories")
				.contains("must_not").contains("vertical").as("one fixed window for the whole run").doesNotContain("now");
	}

	@Test
	void invalidParametersAreRefused() {
		assertThatThrownBy(() -> new NatureBackfillOperation(verticals, 1.5, null, 2))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NatureBackfillOperation(verticals, 0, List.of(-0.1), 2))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NatureBackfillOperation(verticals, 0, null, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NatureBackfillOperation(verticals, Double.NaN, null, 2))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
