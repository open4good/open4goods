package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.services.realtime.ExclusionRules;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyScore;
import org.open4goods.brand.model.ScoreScale;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Offline ImpactScore V1/V2 simulation on a production export (GOU-1887, design
 * {@code docs/impactscore/v2-design.md}, section 6). Never runs in CI: it needs the export, which stays outside the
 * repository, and writes nothing but CSV files.
 *
 * <p>For each {@code <vertical>.jsonl.gz} of {@code -Dimpactscore.simulation.input} (one product {@code _source} per
 * line), the real vertical configuration is loaded twice, forced to version 1 then 2 by
 * {@code impactscore.methodology-version-overrides}, and the real scoring chain runs in production order:
 * <ul>
 * <li>V1: cohort {@code offersCount > 0 && !excluded}, with the stored exclusion;</li>
 * <li>V2: exclusion recomputed by {@link ExclusionRules#evaluate} with the V2 configuration, same cohort rule;</li>
 * <li>brand scores: a stub {@link BrandService} returns the real {@code BRAND_*} scores read from the export;</li>
 * <li>each version scores its own copy of the products.</li>
 * </ul>
 * Optional {@code -Dimpactscore.simulation.params=<file.yml>}: per vertical id, fields replacing those of the
 * {@code v2} block (for example {@code dishwasher: {minRealWeightCoverage: 0.5}}), to compare candidate values.
 * Optional {@code -Dimpactscore.simulation.verticals=tv,oven} restricts the run.
 *
 * <p>Outputs in {@code -Dimpactscore.simulation.output} (default {@code target/impactscore-simulation}), read by
 * {@code scripts/impactscore/simulation_report.py}: {@code <vertical>-products.csv} (every product with offers, plus
 * every product holding a stored ImpactScore), {@code <vertical>-weights.csv} and {@code fidelity.csv}.
 *
 * <p>Fidelity check: a third run scores in V1 the cohort of the last batch (products whose
 * {@code impactScoreMeta.scoredAt} belongs to it, offers lost since or not), since the min-max scales depend on the
 * cohort. The run fails when it does not reproduce at least 99 % of the stored ImpactScores within 0.05, except for
 * the verticals of {@code -Dimpactscore.simulation.fidelity-waived=air-conditioner}: a V1 configuration changed on
 * purpose since the last batch (air conditioners, GOU-2137) cannot reproduce it, and the report states why.
 */
@EnabledIfSystemProperty(named = "impactscore.simulation.input", matches = ".+")
class ImpactScoreSimulationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactScoreSimulationTest.class);

	/** Tolerance and share of the fidelity check (issue GOU-1887). */
	private static final double FIDELITY_TOLERANCE = 0.05;
	private static final double FIDELITY_MIN_SHARE = 0.99;

	/** Products assessed less than this before the latest assessment belong to the last batch. */
	private static final long LAST_BATCH_WINDOW_MS = 6L * 3600 * 1000;

	private static final String ECOSCORE = "ECOSCORE";
	private static final String DATA_QUALITY = "DATA_QUALITY";

	private final SerialisationService serialisation = new SerialisationService();

	/**
	 * Reader of the export: the settings of {@link SerialisationService}, which cannot read a {@link Product} back.
	 * Jackson 3 detects parameter names and builds {@code SourcedAttribute} or {@code IndexedAttribute} through their
	 * copy constructors, which fail; without that detection it uses their no-argument constructors.
	 */
	private static final JsonMapper EXPORT_READER = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY,
					JsonInclude.Include.NON_EMPTY))
			.disable(MapperFeature.DETECT_PARAMETER_NAMES)
			.build();

	@Test
	void simulate() throws Exception {
		Path input = Path.of(System.getProperty("impactscore.simulation.input"));
		Path output = Path.of(System.getProperty("impactscore.simulation.output", "target/impactscore-simulation"));
		Files.createDirectories(output);
		Set<String> only = new LinkedHashSet<>();
		String restriction = System.getProperty("impactscore.simulation.verticals", "");
		for (String id : restriction.split(",")) {
			if (!id.isBlank()) {
				only.add(id.trim());
			}
		}

		List<String> ids = configs(null).stream().map(VerticalConfig::getId).toList();
		Map<String, VerticalConfig> v1 = byId(configs(overrides(ids, ImpactScoreConfig.METHODOLOGY_V1)));
		Map<String, VerticalConfig> v2 = byId(configs(overrides(ids, ImpactScoreConfig.METHODOLOGY_V2)));
		applyParams(v2);

		List<String> fidelity = new ArrayList<>();
		fidelity.add("vertical,last_batch,reproduced,share,max_gap,cohort_compared,cohort_reproduced,cohort_share,"
				+ "cohort_max_gap");
		Map<String, Double> shares = new TreeMap<>();
		for (String id : ids) {
			Path file = input.resolve(id + ".jsonl.gz");
			if ((!only.isEmpty() && !only.contains(id)) || !Files.exists(file)) {
				continue;
			}
			long start = System.nanoTime();
			String line = simulate(file, v1.get(id), v2.get(id), output);
			fidelity.add(line);
			shares.put(id, Double.parseDouble(line.split(",")[3]));
			LOGGER.info("Simulation of {} done in {} s : {}", id, (System.nanoTime() - start) / 1_000_000_000L, line);
		}
		Files.write(output.resolve("fidelity.csv"), fidelity, StandardCharsets.UTF_8);

		assertThat(shares).as("simulated verticals").isNotEmpty();
		Set<String> waived = Set.of(System.getProperty("impactscore.simulation.fidelity-waived", "").split(","));
		shares.forEach((id, share) -> {
			if (!waived.contains(id)) {
				assertThat(share).as("fidelity of the offline V1 on %s", id).isGreaterThanOrEqualTo(FIDELITY_MIN_SHARE);
			}
		});
	}

	/**
	 * Simulates one vertical and writes its CSV files.
	 *
	 * @return the fidelity line of the vertical
	 */
	private String simulate(Path file, VerticalConfig v1Conf, VerticalConfig v2Conf, Path output) throws Exception {
		List<Product> v1Products = new ArrayList<>();
		List<Product> v2Products = new ArrayList<>();
		List<Product> assessed = new ArrayList<>();
		Map<Long, Stored> stored = new LinkedHashMap<>();
		Map<String, BrandScores> brandScores = new HashMap<>();
		Set<String> keys = criterionKeys(v1Conf, v2Conf);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(Files.newInputStream(file), 1 << 16), StandardCharsets.UTF_8), 1 << 20)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				Product product = EXPORT_READER.readValue(line, Product.class);
				boolean offers = product.getOffersCount() != null && product.getOffersCount() > 0;
				Score ecoscore = product.getScores().get(ECOSCORE);
				if (!offers && ecoscore == null) {
					continue;
				}
				ExclusionRules.Result exclusion = ExclusionRules.evaluate(product, v2Conf);
				stored.put(product.getId(), Stored.of(product, keys, exclusion));
				collectBrandScores(product, brandScores);
				if (product.getImpactScoreMeta() != null && product.getImpactScoreMeta().getScoredAt() > 0) {
					assessed.add(EXPORT_READER.readValue(line, Product.class));
				}
				if (!offers) {
					continue;
				}

				// Each version scores its own copy: the V2 copy is read again from the export
				Product v2Product = EXPORT_READER.readValue(line, Product.class);
				v2Product.setExcluded(exclusion.excluded());
				v2Product.setExcludedCauses(new LinkedHashSet<>(exclusion.excludedCauses()));
				if (!v2Product.isExcluded()) {
					v2Products.add(v2Product);
				}
				if (!product.isExcluded()) {
					v1Products.add(product);
				}
			}
		}
		// The last batch's cohort: products it assessed, offers lost or not since. Its min-max scales depend on it.
		long last = assessed.stream().mapToLong(p -> p.getImpactScoreMeta().getScoredAt()).max().orElse(0);
		List<Product> lastBatch = assessed.stream()
				.filter(p -> p.getImpactScoreMeta().getScoredAt() >= last - LAST_BATCH_WINDOW_MS)
				.filter(p -> !p.isExcluded()).toList();
		assessed.clear();
		LOGGER.info("{} : {} rows kept, V1 cohort {}, V2 cohort {}, last batch {}, {} brands", v1Conf.getId(),
				stored.size(), v1Products.size(), v2Products.size(), lastBatch.size(), brandScores.size());

		BrandService brandService = brandService(brandScores);
		Path logs = Files.createTempDirectory("impactscore-simulation");
		aggregationFacadeService(logs, brandService).getScoringAggregator().score(lastBatch, v1Conf);
		aggregationFacadeService(logs, brandService).getScoringAggregator().score(v1Products, v1Conf);
		aggregationFacadeService(logs, brandService).getScoringAggregator().score(v2Products, v2Conf);

		Map<Long, Product> batchById = lastBatch.stream().collect(Collectors.toMap(Product::getId, p -> p));
		Map<Long, Product> v1ById = v1Products.stream().collect(Collectors.toMap(Product::getId, p -> p));
		Map<Long, Product> v2ById = v2Products.stream().collect(Collectors.toMap(Product::getId, p -> p));
		writeProducts(output.resolve(v1Conf.getId() + "-products.csv"), stored, keys, batchById, v1ById, v2ById,
				v2Conf);
		writeWeights(output.resolve(v1Conf.getId() + "-weights.csv"), v1Conf, v2Conf);
		return v1Conf.getId() + "," + fidelity(stored, batchById) + "," + fidelity(stored, v1ById);
	}

	/**
	 * Share of stored ImpactScores reproduced by an offline V1 run within {@link #FIDELITY_TOLERANCE}.
	 *
	 * @return {@code compared,reproduced,share,max_gap}
	 */
	private static String fidelity(Map<Long, Stored> stored, Map<Long, Product> offline) {
		int total = 0;
		int reproduced = 0;
		double maxGap = 0;
		for (Product product : offline.values()) {
			Stored s = stored.get(product.getId());
			if (s == null || s.storedValue == null || s.scoredAt <= 0) {
				continue;
			}
			total++;
			double gap = product.ecoscore() == null ? Double.MAX_VALUE
					: Math.abs(product.ecoscore().getValue() - s.storedValue);
			maxGap = Math.max(maxGap, gap);
			if (gap <= FIDELITY_TOLERANCE) {
				reproduced++;
			}
		}
		double share = total == 0 ? 0 : (double) reproduced / total;
		return String.format(java.util.Locale.ROOT, "%d,%d,%.4f,%.4f", total, reproduced, share, maxGap);
	}

	private void writeProducts(Path path, Map<Long, Stored> stored, Set<String> keys, Map<Long, Product> batchById,
			Map<Long, Product> v1ById, Map<Long, Product> v2ById, VerticalConfig v2Conf) throws IOException {
		List<String> header = new ArrayList<>(List.of("id", "brand", "model", "name", "offers_count", "last_change",
				"stored_excluded", "stored_causes", "stored_value", "stored_version", "stored_scored_at", "stored_ranked",
				"v2_excluded", "v2_causes", "v1_batch_value", "v1_value", "v1_ranking", "v1_position", "v1_count", "v2_status",
				"v2_reason", "v2_coverage", "v2_real", "v2_total", "v2_value", "v2_ranking", "v2_position", "v2_count",
				"v2_level"));
		for (String key : keys) {
			header.add("raw:" + key);
			header.add("v1:" + key);
			header.add("v1virtual:" + key);
			header.add("v2:" + key);
		}
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write(String.join(",", header));
			writer.write('\n');
			for (Stored s : stored.values()) {
				Product v1 = v1ById.get(s.id);
				Product v2 = v2ById.get(s.id);
				List<Object> row = new ArrayList<>(List.of(s.id, csv(s.brand), csv(s.model), csv(s.name), s.offersCount,
						s.lastChange, s.excluded, csv(String.join("|", s.causes)), str(s.storedValue), str(s.version),
						s.scoredAt, s.ranked, s.v2Excluded, csv(String.join("|", s.v2Causes))));
				Product batch = batchById.get(s.id);
				row.add(str(batch == null || batch.ecoscore() == null ? null : batch.ecoscore().getValue()));
				Score v1Score = v1 == null ? null : v1.ecoscore();
				row.add(str(v1Score == null ? null : v1Score.getValue()));
				row.add(str(v1Score == null ? null : v1Score.getRanking()));
				row.add(v1Score == null ? "" : v1.getRanking().getGlobalPosition());
				row.add(v1Score == null ? "" : v1.getRanking().getGlobalCount());
				ImpactScoreMeta meta = v2 == null ? null : v2.getImpactScoreMeta();
				Score v2Score = v2 == null ? null : v2.getScores().get(ECOSCORE);
				row.add(meta == null ? "" : meta.getStatus());
				row.add(meta == null ? "" : str(meta.getNotRatedReason()));
				row.add(meta == null ? "" : str(meta.getRealWeightCoverage()));
				row.add(meta == null ? "" : str(meta.getRealCriteriaCount()));
				row.add(meta == null ? "" : str(meta.getTotalCriteriaCount()));
				row.add(str(v2Score == null ? null : v2Score.getValue()));
				row.add(str(v2Score == null ? null : v2Score.getRanking()));
				row.add(v2Score == null ? "" : v2.getRanking().getGlobalPosition());
				row.add(v2Score == null ? "" : v2.getRanking().getGlobalCount());
				row.add(csv(level(v2Score, v2, v2Conf)));
				for (String key : keys) {
					row.add(csv(s.raw.get(key)));
					Score c1 = v1 == null ? null : v1.getScores().get(key);
					row.add(str(relative(c1)));
					row.add(c1 == null ? "" : String.valueOf(Boolean.TRUE.equals(c1.getVirtual())));
					row.add(str(relative(v2 == null ? null : v2.getScores().get(key))));
				}
				writer.write(row.stream().map(String::valueOf).collect(Collectors.joining(",")));
				writer.write('\n');
			}
		}
	}

	/** Position label of a V2 rated product, from its stored ranking (design 2.4, applied by the front-api). */
	private static String level(Score v2Score, Product v2, VerticalConfig v2Conf) {
		if (v2Score == null || v2Score.getRanking() == null) {
			return null;
		}
		long count = v2.getRanking().getGlobalCount();
		if (count < 2) {
			return null;
		}
		return v2Conf.getImpactScoreConfig().positionLevel(v2Score.getRanking() / (double) (count - 1), count);
	}

	private static void writeWeights(Path path, VerticalConfig v1Conf, VerticalConfig v2Conf) throws IOException {
		Map<String, Double> w1 = normalised(v1Conf.getImpactScoreConfig().activeCriteriasPonderation());
		Map<String, Double> w2 = normalised(v2Conf.getImpactScoreConfig().activeCriteriasPonderation());
		Set<String> keys = new java.util.TreeSet<>(w1.keySet());
		keys.addAll(w2.keySet());
		List<String> lines = new ArrayList<>();
		lines.add("criterion,v1_weight,v2_weight,v2_scoring_override,v2_threshold,v2_eprel_required");
		ImpactScoreConfig config = v2Conf.getImpactScoreConfig();
		for (String key : keys) {
			lines.add(key + "," + str(w1.get(key)) + "," + str(w2.get(key)) + "," + (config.criteriaScoring(key) != null)
					+ "," + str(config.minRealWeightCoverage())
					+ "," + config.isEprelRequired());
		}
		Files.write(path, lines, StandardCharsets.UTF_8);
	}

	private static Map<String, Double> normalised(Map<String, Double> weights) {
		double total = weights.values().stream().filter(w -> w != null && w > 0).mapToDouble(Double::doubleValue).sum();
		Map<String, Double> ret = new TreeMap<>();
		weights.forEach((key, w) -> {
			if (w != null && w > 0) {
				ret.put(key, w / total);
			}
		});
		return ret;
	}

	/** Keys of both matrices, the sources of derived criteria and {@code DATA_QUALITY}. */
	private static Set<String> criterionKeys(VerticalConfig v1Conf, VerticalConfig v2Conf) {
		Set<String> keys = new java.util.TreeSet<>(v1Conf.getImpactScoreConfig().activeCriteriasPonderation().keySet());
		keys.addAll(v2Conf.getImpactScoreConfig().activeCriteriasPonderation().keySet());
		keys.add(DATA_QUALITY);
		for (String key : List.copyOf(keys)) {
			AttributeConfig attribute = v2Conf.getAttributesConfig().getAttributeConfigByKey(key);
			if (attribute != null && attribute.isDerivedScore()) {
				keys.addAll(attribute.getScoreSources());
			}
		}
		return keys;
	}

	private static Double relative(Score score) {
		return score == null || score.getRelativ() == null ? null : score.getRelativ().getValue();
	}

	/**
	 * Keeps, for each brand, the real {@code BRAND_*} scores of its most recently assessed product: a product
	 * scored by an earlier batch may hold brand scores the last batch no longer gave.
	 */
	private static void collectBrandScores(Product product, Map<String, BrandScores> brandScores) {
		if (product.brand() == null) {
			return;
		}
		long scoredAt = product.getImpactScoreMeta() == null ? 0 : product.getImpactScoreMeta().getScoredAt();
		BrandScores known = brandScores.get(product.brand());
		if (known != null && known.scoredAt() >= scoredAt) {
			return;
		}
		Map<String, Double> scores = new TreeMap<>();
		for (Score score : product.getScores().values()) {
			if (score.getName() == null || !score.getName().startsWith("BRAND_") || Boolean.TRUE.equals(score.getVirtual())
					|| score.getAbsolute() == null || score.getAbsolute().getValue() == null) {
				continue;
			}
			String provider = score.getMetadatas() != null && score.getMetadatas().get(BrandScoresAggregationService.PROVIDER) != null
					? score.getMetadatas().get(BrandScoresAggregationService.PROVIDER)
					: score.getName().substring("BRAND_".length(), score.getName().length() - "_SCORING".length())
							.toLowerCase(java.util.Locale.ROOT);
			scores.put(provider, score.getAbsolute().getValue());
		}
		brandScores.put(product.brand(), new BrandScores(scoredAt, scores));
	}

	/** Real brand scores of the most recently assessed product of a brand, by provider. */
	private record BrandScores(long scoredAt, Map<String, Double> byProvider) {
	}

	/** Returns, for a brand, a company whose normalised scores are the stored {@code BRAND_*} values. */
	private static BrandService brandService(Map<String, BrandScores> brandScores) {
		BrandService brandService = mock(BrandService.class);
		when(brandService.resolve(anyString())).thenAnswer(invocation -> {
			String name = invocation.getArgument(0);
			BrandScores known = brandScores.get(name);
			if (known == null || known.byProvider().isEmpty()) {
				return null;
			}
			Map<String, Double> scores = known.byProvider();
			Company company = new Company(name, name);
			scores.forEach((provider, value) -> {
				CompanyScore score = new CompanyScore();
				score.setValue(value);
				score.setScale(new ScoreScale(0.0, 100.0, true));
				company.getScores().put(provider, score);
			});
			Brand brand = new Brand(name);
			brand.setCompanyName(name);
			brand.setCompany(company);
			return brand;
		});
		return brandService;
	}

	/**
	 * Applies {@code -Dimpactscore.simulation.params}: per vertical id, the given fields replace those of the
	 * {@code v2} block.
	 */
	private void applyParams(Map<String, VerticalConfig> v2) throws Exception {
		String params = System.getProperty("impactscore.simulation.params");
		if (params == null || params.isBlank()) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> byVertical = serialisation.fromYaml(Files.readString(Path.of(params)), Map.class);
		for (Map.Entry<String, Object> entry : byVertical.entrySet()) {
			VerticalConfig vertical = v2.get(entry.getKey());
			assertThat(vertical).as("vertical %s of %s", entry.getKey(), params).isNotNull();
			ImpactScoreConfig config = vertical.getImpactScoreConfig();
			Map<String, Object> merged = serialisation.fromJsonTypeRef(serialisation.toJson(config.getV2()),
					new TypeReference<Map<String, Object>>() {
					});
			@SuppressWarnings("unchecked")
			Map<String, Object> fields = (Map<String, Object>) entry.getValue();
			merged.putAll(fields);
			config.setV2(serialisation.fromJson(serialisation.toJson(merged), ImpactScoreV2Config.class));
			config.refreshMethodologyRevision();
			LOGGER.info("Simulation parameters of {} : {}", entry.getKey(), fields);
		}
	}

	private static String overrides(List<String> ids, int version) {
		return ids.stream().map(id -> id + ":" + version).collect(Collectors.joining(","));
	}

	private List<VerticalConfig> configs(String overrides) throws Exception {
		VerticalsConfigService service = new VerticalsConfigService(serialisation, mock(GoogleTaxonomyService.class),
				new PathMatchingResourcePatternResolver(), overrides);
		return new ArrayList<>(service.getConfigsWithoutDefault());
	}

	private static Map<String, VerticalConfig> byId(List<VerticalConfig> configs) {
		return configs.stream().collect(Collectors.toMap(VerticalConfig::getId, c -> c));
	}

	private static String str(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String csv(String value) {
		if (value == null) {
			return "";
		}
		String flat = value.replace('\n', ' ').replace('\r', ' ');
		return flat.contains(",") || flat.contains("\"") ? "\"" + flat.replace("\"", "\"\"") + "\"" : flat;
	}

	private static AggregationFacadeService aggregationFacadeService(Path logs, BrandService brandService) {
		ApiProperties apiProperties = new ApiProperties();
		apiProperties.setRootFolder(logs + "/");

		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		doAnswer(invocation -> null).when(beanFactory).autowireBean(any());

		return new AggregationFacadeService(mock(EvaluationService.class),
				mock(org.open4goods.model.StandardiserService.class), beanFactory, mock(ProductRepository.class),
				apiProperties, mock(Gs1PrefixService.class), mock(DataSourceConfigService.class),
				mock(VerticalsConfigService.class), mock(BarcodeValidationService.class), brandService,
				mock(BlablaService.class), mock(IcecatService.class), mock(IcecatFeatureResolver.class),
				mock(SerialisationService.class), mock(BarcodeForensicsService.class));
	}

	/** What the export holds for a product, read before any scoring. */
	private record Stored(long id, String brand, String model, String name, int offersCount, long lastChange,
			boolean excluded, Set<String> causes, Double storedValue, Integer version, long scoredAt, boolean ranked,
			boolean v2Excluded, Set<String> v2Causes, Map<String, String> raw) {

		static Stored of(Product product, Set<String> keys, ExclusionRules.Result v2Exclusion) {
			Score ecoscore = product.getScores().get(ECOSCORE);
			ImpactScoreMeta meta = product.getImpactScoreMeta();
			Map<String, String> raw = new HashMap<>();
			for (String key : keys) {
				IndexedAttribute attribute = product.getAttributes().getIndexed().get(key);
				if (attribute != null) {
					raw.put(key, attribute.getValue());
				}
			}
			return new Stored(product.getId(), product.brand(), product.model(), product.bestName(),
					product.getOffersCount() == null ? 0 : product.getOffersCount(), product.getLastChange(),
					product.isExcluded(), product.getExcludedCauses() == null ? Set.of() : product.getExcludedCauses(),
					ecoscore == null ? null : ecoscore.getValue(), meta == null ? null : meta.getMethodologyVersion(),
					meta == null ? 0 : meta.getScoredAt(),
					product.getRanking() != null && product.getRanking().getGlobalCount() > 0, v2Exclusion.excluded(),
					v2Exclusion.excludedCauses(), raw);
		}
	}
}
