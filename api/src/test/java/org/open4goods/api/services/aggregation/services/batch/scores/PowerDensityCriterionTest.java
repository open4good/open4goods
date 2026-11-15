package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-1884: the tv V2 matrix rates the SDR power per square decimetre of screen ({@code POWER_DENSITY_SDR}) instead
 * of the raw watts and the diagonal. Runs the real scoring chain, in production order, on the real tv configuration.
 */
class PowerDensityCriterionTest {

    private static final String CRITERION = "POWER_DENSITY_SDR";
    private static final String WATTS = "POWER_CONSUMPTION_SDR";
    private static final String DIAGONAL = "DIAGONALE_POUCES";
    private static final String COMPOSITE = "ENERGY_CONSUMPTION";

    private static final long NO_DIAGONAL = 1L;
    private static final long NO_WATTS = 2L;
    private static final long ZERO_WATTS = 3L;
    private static final long TV_65 = 4L;
    private static final long TV_75 = 5L;

    /** The 27" television of the 08/11 audit, energy classes E (SDR) and G (HDR). */
    private static final long AUDIT_27_INCHES = 4719331852795L;

    private static final String FIXTURES = "impactscore/repairability-or-durability-products.properties";
    private static final String SAMPLE = "impactscore/tv-energy-sample.csv";
    private static final List<String> ENERGY_KEYS = List.of(DIAGONAL, WATTS, "POWER_CONSUMPTION_HDR",
            "CLASSE_ENERGY_SDR", "CLASSE_ENERGY_HDR");

    private static VerticalConfig tv;

    @BeforeAll
    static void loadTv() {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        tv = service.getConfigById("tv");
        assertThat(tv).isNotNull();
    }

    @Test
    void v2RatesTheSdrPowerPerSquareDecimetreOfScreen() throws Exception {
        Properties fixtures = fixtures();
        long realId = Long.parseLong(fixtures.getProperty("tv.gtin"));
        List<Product> products = new ArrayList<>();
        products.add(realProduct(fixtures, realId));
        products.add(with(realProduct(fixtures, NO_DIAGONAL), DIAGONAL, null));
        products.add(with(realProduct(fixtures, NO_WATTS), WATTS, null));
        products.add(with(realProduct(fixtures, ZERO_WATTS), WATTS, "0"));
        // Same energy efficiency index, regulation (EU) 2019/2013, on 65" and 75" screens
        products.add(with(with(realProduct(fixtures, TV_65), DIAGONAL, "65"), WATTS, String.valueOf(powerForEei(0.8, 65))));
        products.add(with(with(realProduct(fixtures, TV_75), DIAGONAL, "75"), WATTS, String.valueOf(powerForEei(0.8, 75))));

        withV2(() -> score(products));

        // 50", 53 W: 53 / (50² x 144 / 337 x 0.064516) W/dm²
        Product real = product(products, realId);
        Score density = real.getScores().get(CRITERION);
        assertThat(density).isNotNull();
        assertThat(density.getVirtual()).isNotEqualTo(Boolean.TRUE);
        assertThat(density.getAbsolute().getValue()).isCloseTo(53.0 / (2500.0 * 144.0 / 337.0 * 0.064516), within(1e-9));
        assertThat(density.getMetadatas()).containsEntry(Attribute2ScoreAggregationService.SCORE_SOURCE_METADATA,
                WATTS + "," + DIAGONAL);
        assertThat(real.getImpactScoreMeta().getRealCriteria()).contains(CRITERION, WATTS)
                .doesNotContain("POWER_CONSUMPTION_HDR", DIAGONAL);
        // GOU-2186 (option B): the raw SDR watts are back next to the density
        assertThat(real.getScores().get(COMPOSITE).getAggregates()).containsKeys(CRITERION, WATTS);

        // Without a real diagonal or real watts: absent, never imputed
        for (long id : List.of(NO_DIAGONAL, NO_WATTS, ZERO_WATTS)) {
            Product product = product(products, id);
            assertThat(product.getScores()).as("scores of %s", id).doesNotContainKey(CRITERION);
            assertThat(product.getImpactScoreMeta().getRealCriteria()).as("criteria of %s", id).doesNotContain(CRITERION);
        }

        // Same EEI: the raw watts differ by a fifth, the densities by less than a tenth
        double watts65 = powerForEei(0.8, 65);
        double watts75 = powerForEei(0.8, 75);
        assertThat(watts65 / watts75).isLessThan(0.82);
        double density65 = product(products, TV_65).getScores().get(CRITERION).getAbsolute().getValue();
        double density75 = product(products, TV_75).getScores().get(CRITERION).getAbsolute().getValue();
        assertThat(density75 / density65).isBetween(0.9, 1.0);
    }

    @Test
    void v1IsUnchanged() throws Exception {
        Properties fixtures = fixtures();
        long realId = Long.parseLong(fixtures.getProperty("tv.gtin"));
        List<Product> products = List.of(realProduct(fixtures, realId));
        assertThat(tv.getImpactScoreConfig().effectiveMethodologyVersion()).isEqualTo(1);

        score(products);

        Product real = products.getFirst();
        assertThat(real.getScores()).doesNotContainKey(CRITERION);
        assertThat(real.getImpactScoreMeta().getRealCriteria()).contains(WATTS, DIAGONAL).doesNotContain(CRITERION);
    }

    /**
     * Real energy values, every other criterion equal: the 27" E/G television of the audit is in the top 5 % in V1
     * (small screen, few watts) and loses ground in V2 (density and watts at equal weight).
     */
    @Test
    void auditTelevisionLeavesTheTopOfTheV2Ranking() throws Exception {
        List<Product> v1 = sample();
        score(v1);
        assertThat(betterThan(v1, AUDIT_27_INCHES)).as("V1 share of the cohort the 27\" beats").isGreaterThan(0.95);

        List<Product> v2 = sample();
        withV2(() -> score(v2));
        assertThat(product(v2, AUDIT_27_INCHES).getScores().get(CRITERION)).isNotNull();
        // GOU-2186 (option B): the raw watts, back at the same weight as the density, keep part of the small screen's edge
        double v2Share = betterThan(v2, AUDIT_27_INCHES);
        assertThat(v2Share).as("V2 share of the cohort the 27\" beats").isLessThan(0.8);
        assertThat(v2Share).isLessThan(betterThan(v1, AUDIT_27_INCHES) - 0.15);
    }

    /**
     * On-mode power, in watts, of a television of energy efficiency index {@code eei} (regulation (EU) 2019/2013,
     * annex III: EEI = (P + 1) / (3 x (90 x tanh(0.025 + 0.0035 x (A - 11)) + 4) + 3), A the screen area in dm²).
     */
    private static double powerForEei(double eei, double diagonalInches) {
        double area = diagonalInches * diagonalInches * 144.0 / 337.0 * 0.064516;
        return eei * (3.0 * (90.0 * Math.tanh(0.025 + 0.0035 * (area - 11.0)) + 4.0) + 3.0) - 1.0;
    }

    /** Share of the rest of the cohort with a strictly lower ImpactScore. */
    private static double betterThan(List<Product> products, long id) {
        double score = product(products, id).ecoscore().getValue();
        long beaten = products.stream().filter(product -> product.getId() != id)
                .filter(product -> product.ecoscore().getValue() < score).count();
        return (double) beaten / (products.size() - 1);
    }

    /** {@link #SAMPLE}: copies of the real product with the energy values of real televisions. */
    private static List<Product> sample() throws Exception {
        Properties fixtures = fixtures();
        List<Product> products = new ArrayList<>();
        try (InputStream input = PowerDensityCriterionTest.class.getClassLoader().getResourceAsStream(SAMPLE)) {
            assertThat(input).as(SAMPLE).isNotNull();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            List<String> lines = reader.lines().filter(line -> !line.startsWith("#")).skip(1).toList();
            for (String line : lines) {
                String[] cells = line.split(",", -1);
                Product product = realProduct(fixtures, Long.parseLong(cells[0]));
                for (int i = 0; i < ENERGY_KEYS.size(); i++) {
                    with(product, ENERGY_KEYS.get(i), cells[i + 1].isEmpty() ? null : cells[i + 1]);
                }
                products.add(product);
            }
        }
        return products;
    }

    private static Product realProduct(Properties fixtures, long id) {
        Product product = new Product(id);
        fixtures.stringPropertyNames().stream()
                .filter(name -> name.startsWith("tv.") && !name.equals("tv.gtin"))
                .forEach(name -> {
                    String key = name.substring("tv.".length());
                    product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, fixtures.getProperty(name)));
                });
        return product;
    }

    private static Product with(Product product, String key, String value) {
        Map<String, IndexedAttribute> indexed = product.getAttributes().getIndexed();
        indexed.remove(key);
        if (value != null) {
            indexed.put(key, new IndexedAttribute(key, value));
        }
        return product;
    }

    private static void score(List<Product> products) {
        Logger logger = LoggerFactory.getLogger(PowerDensityCriterionTest.class);
        List<AbstractAggregationService> services = new ArrayList<>();
        services.add(new CleanScoreAggregationService(logger));
        services.add(new Attribute2ScoreAggregationService(logger));
        services.add(new BrandScoresAggregationService(logger, mock(BrandService.class)));
        services.add(new DataCompletion2ScoreAggregationService(logger));
        services.add(new EcoScoreAggregationService(logger));
        services.add(new ParticipatingScoresAggregationService(logger));
        new ScoringBatchedAggregator(services).score(products, tv);
    }

    private static void withV2(Runnable body) {
        ImpactScoreConfig config = tv.getImpactScoreConfig();
        try {
            config.setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
            body.run();
        } finally {
            config.setMethodologyVersionOverride(null);
        }
    }

    private static Product product(List<Product> products, long id) {
        return products.stream().filter(product -> product.getId() == id).findFirst().orElseThrow();
    }

    private static Properties fixtures() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = PowerDensityCriterionTest.class.getClassLoader().getResourceAsStream(FIXTURES)) {
            assertThat(input).as(FIXTURES).isNotNull();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
