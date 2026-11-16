package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.CategoryVoteReplay.ExportedProduct;
import org.open4goods.verticals.CategoryVoteReplay.Population;
import org.open4goods.verticals.CategoryVoteReplay.ProductResult;
import org.open4goods.verticals.CategoryVoteReplay.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Before/after replay of the vote (GOU-2317), see {@link CategoryVoteReplay}.
 *
 * <p>In CI, the replay runs on the versioned sample of the card ({@value CategoryVoteTest#REAL_CATEGORIES}) and on
 * one product per population, and writes nothing. {@link #replayExport()} runs only with
 * {@code -Dcategoryvote.replay.input=<export.json>}: it replays that export and writes the Markdown report to
 * {@code -Dcategoryvote.replay.output} (default {@code target/category-vote-replay.md} of the module), and nowhere
 * else. Procedure: {@code verticals/README.md}.
 */
class CategoryVoteReplayTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryVoteReplayTest.class);

	private static CategoryVoteReplay replay;

	@BeforeAll
	static void setUp() {
		replay = new CategoryVoteReplay(historicalService());
	}

	/** The configuration as shipped, with the flag forced off: the historical side of the replay. */
	private static VerticalsConfigService historicalService() {
		VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
				mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
		service.getDefaultConfig().setCategoryVoteEnabled(false);
		return service;
	}

	private static ExportedProduct product(String eprelGroup, String... datasourceAndCategory) {
		Map<String, String> categories = new LinkedHashMap<>();
		for (int i = 0; i < datasourceAndCategory.length; i += 2) {
			categories.put(datasourceAndCategory[i], datasourceAndCategory[i + 1]);
		}
		return new ExportedProduct(categories, eprelGroup);
	}

	@Test
	void sampleOfTheCard() throws Exception {
		Report report = replay.replay(CategoryVoteTest.readRealCategories());

		assertThat(report.products()).hasSize(11);
		assertThat(report.unreadable()).isEmpty();
		assertThat(report.unclassified()).isZero();
		assertThat(report.eprelConfirmedLosses()).isEmpty();

		// The six false positives that had a vertical lose it, all in population (a); G.Skill had none already
		assertThat(report.differences()).extracting(ProductResult::id).containsExactlyInAnyOrder("3700755601091",
				"4718466015754", "4710483940316", "4710483944697", "5056547202259", "8800263650385");
		assertThat(report.differences()).allSatisfy(result -> {
			assertThat(result.vote()).isNull();
			assertThat(result.populations()).containsExactly(Population.A);
		});

		assertThat(report.byVertical()).containsOnlyKeys(CategoryVoteReplay.NO_VERTICAL, "dishwasher", "refrigerator",
				"smartphones", "tv");
		assertCounts(report, "smartphones", 4, 2, 2, 0);
		assertCounts(report, "tv", 3, 2, 1, 0);
		assertCounts(report, "refrigerator", 2, 0, 2, 0);
		assertCounts(report, "dishwasher", 1, 0, 1, 0);
		assertCounts(report, CategoryVoteReplay.NO_VERTICAL, 1, 7, 0, 6);
		assertThat(report.byVertical().get(CategoryVoteReplay.NO_VERTICAL).population(Population.A)).isEqualTo(6);

		String markdown = CategoryVoteReplay.toMarkdown(report, "sample");
		assertThat(markdown).contains("Products: 11. Unreadable: 0. Differences: 6 (unclassified: 0). "
				+ "EPREL-confirmed products losing their historical vertical: 0.")
				.contains("| smartphones | 4 | 2 | 2 | 0 | 2 | 0 | 0 | 0 | 0 |")
				.contains("| smartphones | (none) | 2 | 2 | 0 | 0 | 0 | 0 |")
				.contains("| 8800263650385 | refrigerator | (none) | (a) |");
	}

	/** One product per population of the README, built from real {@code matchingCategories} entries. */
	@Test
	void eachPopulationIsRecognised() {
		String dishwasher = "ELECTROMENAGER | ELECTROMENAGER > LAVAGE-SECHAGE > LAVE-VAISSELLE | LAVAGE-SECHAGE | "
				+ "LAVE-VAISSELLE";
		String smartphone = "TELEPHONIE ET MOBILITE / TELEPHONE MOBILE / SMARTPHONE | TELEPHONIE ET MOBILITE>"
				+ "TELEPHONE MOBILE>SMARTPHONE | TELEPHONE MOBILE | SMARTPHONE";
		String refrigerator = "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE | "
				+ "GROS ELECTROMENAGER / REFRIGERATEUR / REFRIGERATEUR 1 PORTE";
		Map<String, ExportedProduct> export = new LinkedHashMap<>();
		export.put("a", product(null, "Cdiscount FR", dishwasher,
				"ManoMano FR", "ELECTROMENAGER > PIECE DETACHEE LAVE-VAISSELLE > PANIER"));
		export.put("b", product(null, "Cdiscount FR", smartphone,
				"Fnac FR", "348 | MOBILE PHONES | SYSTEME PC & APPAREIL PORTABLE > APPAREIL MOBILE > TELEPHONE MOBILE",
				"rakuten.com-csv.yml", "PIECES DETACHEES TELEPHONE PORTABLE"));
		export.put("c", product(null, "Darty FR", refrigerator, "Cdiscount FR", dishwasher,
				"rakuten.com-csv.yml", "ELECTROMENAGER | LAVE-VAISSELLE"));
		export.put("d", product("washingmachines2019", "Cdiscount FR", dishwasher,
				"rakuten.com-csv.yml", "ELECTROMENAGER | LAVE-VAISSELLE"));
		export.put("same", product("electronicdisplays", "rakuten.com-csv.yml", "HIFI | TELEVISION"));
		export.put("unreadable", product(null, "Cdiscount FR", dishwasher, "Darty FR", null));

		Report report = replay.replay(export);

		assertThat(report.unreadable()).containsOnlyKeys("unreadable");
		assertThat(report.unclassified()).isZero();
		assertThat(report.eprelConfirmedLosses()).isEmpty();
		Map<String, ProductResult> byId = new LinkedHashMap<>();
		report.products().forEach(result -> byId.put(result.id(), result));
		assertResult(byId.get("a"), "dishwasher", null, Set.of(Population.A));
		assertResult(byId.get("b"), null, "smartphones", Set.of(Population.B));
		assertResult(byId.get("c"), "refrigerator", "dishwasher", Set.of(Population.C));
		assertResult(byId.get("d"), "dishwasher", null, Set.of(Population.D));
		assertResult(byId.get("same"), "tv", "tv", Set.of());
		assertThat(byId.get("same").differs()).isFalse();
	}

	@Test
	void replayRefusesAServiceWithTheVoteOn() {
		VerticalsConfigService service = historicalService();
		service.getDefaultConfig().setCategoryVoteEnabled(true);

		assertThatThrownBy(() -> new CategoryVoteReplay(service)).isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Replays an export outside CI: {@code -Dcategoryvote.replay.input=<export.json>}, optional
	 * {@code -Dcategoryvote.replay.output=<report.md>}. Writes the report only.
	 */
	@Test
	@EnabledIfSystemProperty(named = "categoryvote.replay.input", matches = ".+")
	void replayExport() throws Exception {
		Path input = Path.of(System.getProperty("categoryvote.replay.input"));
		Path output = Path.of(System.getProperty("categoryvote.replay.output", "target/category-vote-replay.md"));

		Map<String, ExportedProduct> export;
		try (InputStream in = Files.newInputStream(input)) {
			export = CategoryVoteReplay.readExport(in);
		}
		Report report = replay.replay(export);
		String markdown = CategoryVoteReplay.toMarkdown(report, input.getFileName().toString());
		Files.writeString(output, markdown, StandardCharsets.UTF_8);

		LOGGER.warn("Category vote replay of {} products: {} differences ({} unclassified), {} EPREL-confirmed losses, "
				+ "{} unreadable; report written to {}", report.products().size(), report.differences().size(),
				report.unclassified(), report.eprelConfirmedLosses().size(), report.unreadable().size(),
				output.toAbsolutePath());
		assertThat(report.products()).as("products replayed").isNotEmpty();
	}

	private static void assertCounts(Report report, String vertical, int historical, int vote, int lost, int gained) {
		CategoryVoteReplay.VerticalCounts counts = report.byVertical().get(vertical);
		assertThat(counts).as(vertical).isNotNull();
		assertThat(new int[] { counts.historical, counts.vote, counts.lost, counts.gained }).as(vertical)
				.containsExactly(historical, vote, lost, gained);
	}

	private static void assertResult(ProductResult result, String historical, String vote, Set<Population> populations) {
		assertThat(result).isNotNull();
		assertThat(result.historical()).as(result.id()).isEqualTo(historical);
		assertThat(result.vote()).as(result.id()).isEqualTo(vote);
		assertThat(result.populations()).as(result.id()).containsExactlyInAnyOrderElementsOf(populations);
	}
}
