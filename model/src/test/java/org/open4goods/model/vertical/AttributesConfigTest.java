package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.vertical.referential.IcecatFeatureReferential;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lookup indexes of {@link AttributesConfig}: resolution order, rebuild after a setter, and first lookups from
 * several threads on a fresh config (GOU-2279).
 */
class AttributesConfigTest {

    private static final int THREADS = 16;

    @Test
    void resolvesByIcecatFeatureIdOnFirstCall() {
        AttributeConfig weight = attribute("WEIGHT", 1766, Map.of());
        AttributesConfig config = new AttributesConfig(List.of(weight));

        // No synonym matches this name: only the Icecat feature id can resolve it.
        ProductAttribute attr = productAttribute("POIDS_NET", "icecat.biz", 1766);

        assertThat(config.resolveFromProductAttribute(attr)).isSameAs(weight);
    }

    @Test
    void icecatFeatureIdTakesPrecedenceOverSynonyms() {
        AttributeConfig weight = attribute("WEIGHT", 1766, Map.of());
        AttributeConfig colour = attribute("COLOR", null, Map.of("all", Set.of("POIDS_NET")));
        AttributesConfig config = new AttributesConfig(List.of(weight, colour));

        assertThat(config.resolveFromProductAttribute(productAttribute("POIDS_NET", "icecat.biz", 1766)))
                .isSameAs(weight);
    }

    @Test
    void resolvesByDatasourceSynonymsThenByAllSynonyms() {
        AttributeConfig weight = attribute("WEIGHT", null, Map.of("darty.com", Set.of("POIDS")));
        AttributeConfig colour = attribute("COLOR", null, Map.of("all", Set.of("COULEUR")));
        AttributesConfig config = new AttributesConfig(List.of(weight, colour));

        assertThat(config.resolveFromProductAttribute(productAttribute("POIDS", "darty.com", null)))
                .isSameAs(weight);
        assertThat(config.resolveFromProductAttribute(productAttribute("COULEUR", "fnac.com", null)))
                .isSameAs(colour);
        // A datasource synonym does not leak to another datasource.
        assertThat(config.resolveFromProductAttribute(productAttribute("POIDS", "fnac.com", null))).isNull();
    }

    @Test
    void unknownOrNullLookupsReturnNullWithoutThrowing() {
        AttributesConfig config = new AttributesConfig(
                List.of(attribute("WEIGHT", 1766, Map.of("all", Set.of("POIDS")))));

        assertThat(config.getAttributeConfigByKey(null)).isNull();
        assertThat(config.getAttributeConfigByKey("UNKNOWN")).isNull();
        assertThat(config.getKeyForValue(null)).isNull();
        assertThat(config.resolveFromProductAttribute(productAttribute("UNKNOWN", null, null))).isNull();
    }

    @Test
    void keyForValueMapsSynonymsAndKeysOfAttributesWithSynonyms() {
        AttributesConfig config = new AttributesConfig(List.of(
                attribute("WEIGHT", null, Map.of("all", Set.of("POIDS"))),
                attribute("COLOR", null, Map.of())));

        assertThat(config.getKeyForValue("POIDS")).isEqualTo("WEIGHT");
        assertThat(config.getKeyForValue("WEIGHT")).isEqualTo("WEIGHT");
        // Unchanged behaviour: a key maps to itself only when the attribute declares synonyms.
        assertThat(config.getKeyForValue("COLOR")).isNull();
    }

    @Test
    void lastAttributeWinsOnDuplicateSynonymOrIcecatId() {
        AttributeConfig first = attribute("FIRST", 42, Map.of("all", Set.of("SHARED")));
        AttributeConfig second = attribute("SECOND", 42, Map.of("all", Set.of("SHARED")));
        AttributesConfig config = new AttributesConfig(List.of(first, second));

        assertThat(config.resolveFromProductAttribute(productAttribute("OTHER", "icecat.biz", 42))).isSameAs(second);
        assertThat(config.resolveFromProductAttribute(productAttribute("SHARED", "darty.com", null)))
                .isSameAs(second);
    }

    @Test
    void attributeWithNullSynonymsDoesNotBreakOtherLookups() {
        AttributeConfig noSynonyms = attribute("BRAND", null, Map.of());
        noSynonyms.setSynonyms(null);
        Map<String, Set<String>> nullValues = new HashMap<>();
        nullValues.put("darty.com", null);
        AttributeConfig nullSynonymValues = attribute("MODEL", null, nullValues);
        AttributeConfig weight = attribute("WEIGHT", null, Map.of("all", Set.of("POIDS")));
        AttributesConfig config = new AttributesConfig(
                Arrays.asList(null, noSynonyms, nullSynonymValues, weight));

        assertThat(config.getAttributeConfigByKey("BRAND")).isSameAs(noSynonyms);
        assertThat(config.getAttributeConfigByKey("MODEL")).isSameAs(nullSynonymValues);
        assertThat(config.resolveFromProductAttribute(productAttribute("POIDS", "darty.com", null))).isSameAs(weight);
    }

    @Test
    void nullConfigsGiveEmptyIndexes() {
        AttributesConfig config = new AttributesConfig(null);

        assertThat(config.getAttributeConfigByKey("WEIGHT")).isNull();
        assertThat(config.synonyms()).isEmpty();
        assertThat(config.resolveFromProductAttribute(productAttribute("WEIGHT", "icecat.biz", 1766))).isNull();
    }

    @Test
    void setConfigsAfterFirstLookupRebuildsIndexes() {
        AttributeConfig weight = attribute("WEIGHT", 1766, Map.of("all", Set.of("POIDS")));
        AttributesConfig config = new AttributesConfig(List.of(weight));
        assertThat(config.getAttributeConfigByKey("WEIGHT")).isSameAs(weight);
        assertThat(config.synonyms().get("all")).containsEntry("POIDS", "WEIGHT");

        // Same sequence as the vertical loading: defaults merged, then key-only attributes resolved.
        AttributeConfig colour = attribute("COLOR", 1234, Map.of("all", Set.of("COULEUR")));
        config.setConfigs(List.of(colour));

        assertThat(config.getAttributeConfigByKey("COLOR")).isSameAs(colour);
        assertThat(config.getAttributeConfigByKey("WEIGHT")).isNull();
        assertThat(config.getKeyForValue("COULEUR")).isEqualTo("COLOR");
        assertThat(config.getKeyForValue("POIDS")).isNull();
        assertThat(config.resolveFromProductAttribute(productAttribute("X", "icecat.biz", 1234))).isSameAs(colour);
        assertThat(config.resolveFromProductAttribute(productAttribute("X", "icecat.biz", 1766))).isNull();
    }

    @Test
    void serialisedCopyCarriesNoIndexAndMergedConfigsAreIndexed() {
        // Same settings as the YAML mapper of SerialisationService, which reads private fields.
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY,
                        JsonInclude.Include.NON_EMPTY))
                .changeDefaultVisibility(vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .build();
        AttributesConfig defaults = new AttributesConfig(
                new ArrayList<>(List.of(attribute("WEIGHT", 1766, Map.of("all", Set.of("POIDS"))))));
        assertThat(defaults.getAttributeConfigByKey("WEIGHT")).isNotNull();

        // VerticalsConfigService.getConfig: copy of the defaults through YAML, then the vertical file merged in place
        // (@JsonMerge appends to the list without calling the setter).
        String json = mapper.writeValueAsString(defaults);
        assertThat(json).doesNotContain("\"index\"", "indexLock");
        AttributesConfig copy = mapper.readValue(json, AttributesConfig.class);
        mapper.readerForUpdating(copy).readValue("""
                {"configs": [{"key": "COLOR", "synonyms": {"all": ["COULEUR"]}}]}
                """);

        assertThat(copy.getConfigs()).extracting(AttributeConfig::getKey).containsExactly("WEIGHT", "COLOR");
        assertThat(copy.getAttributeConfigByKey("WEIGHT")).isNotNull();
        assertThat(copy.getAttributeConfigByKey("COLOR")).isNotNull();
        assertThat(copy.getKeyForValue("COULEUR")).isEqualTo("COLOR");
        assertThat(copy.resolveFromProductAttribute(productAttribute("X", "icecat.biz", 1766)).getKey())
                .isEqualTo("WEIGHT");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void concurrentFirstLookupsOnFreshConfigNeverReturnNull() throws Exception {
        int attributes = 3000;
        List<AttributeConfig> configs = new ArrayList<>(attributes);
        for (int i = 0; i < attributes; i++) {
            configs.add(attribute("ATTR_" + i, 100_000 + i, Map.of("all", Set.of("NAME_" + i))));
        }
        int last = attributes - 1;
        ProductAttribute byIcecat = productAttribute("UNMAPPED", "icecat.biz", 100_000 + last);
        ProductAttribute bySynonym = productAttribute("NAME_" + last, "darty.com", null);

        AtomicInteger nullLookups = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 0; round < 30; round++) {
                AttributesConfig config = new AttributesConfig(configs);
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(THREADS);
                for (int t = 0; t < THREADS; t++) {
                    int lane = t;
                    executor.execute(() -> {
                        try {
                            start.await();
                            // Each lane starts on a different index so that every index is built under contention.
                            Object found = switch (lane % 3) {
                                case 0 -> config.getAttributeConfigByKey("ATTR_" + last);
                                case 1 -> config.resolveFromProductAttribute(byIcecat);
                                default -> config.resolveFromProductAttribute(bySynonym);
                            };
                            if (found == null || config.getKeyForValue("NAME_" + last) == null) {
                                nullLookups.incrementAndGet();
                            }
                        } catch (Throwable e) {
                            errors.add(e);
                        } finally {
                            done.countDown();
                        }
                    });
                }
                start.countDown();
                assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(errors).isEmpty();
        assertThat(nullLookups).hasValue(0);
    }

    private static AttributeConfig attribute(String key, Integer icecatFeatureId, Map<String, Set<String>> synonyms) {
        AttributeConfig config = new AttributeConfig();
        config.setKey(key);
        config.setSynonyms(synonyms);
        if (icecatFeatureId != null) {
            config.getReferentials().getIcecat().add(new IcecatFeatureReferential(icecatFeatureId, key, null));
        }
        return config;
    }

    private static ProductAttribute productAttribute(String name, String datasource, Integer icecatTaxonomyId) {
        SourcedAttribute source = new SourcedAttribute();
        source.setName(name);
        source.setValue("value");
        source.setDataSourcename(datasource);
        source.setIcecatTaxonomyId(icecatTaxonomyId);

        ProductAttribute attr = new ProductAttribute();
        attr.addSourceAttribute(source);
        return attr;
    }
}
