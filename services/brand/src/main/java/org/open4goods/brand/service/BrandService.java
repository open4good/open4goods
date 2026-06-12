package org.open4goods.brand.service;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.BrandReferential;
import org.open4goods.brand.model.BrandReferentialEntry;
import org.open4goods.brand.model.BrandSourceEvidence;
import org.open4goods.brand.model.BrandSuggestion;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.type.TypeReference;

/**
 * Service responsible for resolving brands and associating them with companies.
 */
public class BrandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandService.class);
    private static final String MAPPING_URL =
            "https://raw.githubusercontent.com/open4good/brands-company-mapping/refs/heads/main/brands-company-mapping.json";
    private static final Set<String> SUGGESTION_STOP_WORDS = Set.of(
            "FOR SAMSUNG", "OEM", "NO BRAND", "SANS MARQUE", "WITHOUT BRAND", "UNBRANDED", "GENERIC");

    private final SerialisationService serialisationService;
    private final BrandReferentialLoader referentialLoader;

    private final Map<String, Long> missCounter = new ConcurrentHashMap<>();
    private final Map<String, Brand> brandsByName = new ConcurrentHashMap<>();
    private final Map<String, String> canonicalByIndexedName = new ConcurrentHashMap<>();
    private final Map<String, List<BrandSourceEvidence>> evidenceByCanonicalName = new ConcurrentHashMap<>();
    private Map<String, String> brandsAlias = new HashMap<>();
    private BrandReferential referential = new BrandReferential();

    public BrandService(RemoteFileCachingService remoteFileCachingService, SerialisationService serialisationService)
            throws Exception {
        this(remoteFileCachingService, serialisationService,
                () -> IOUtils.toString(new URL(MAPPING_URL), Charset.defaultCharset()));
    }

    public BrandService(RemoteFileCachingService remoteFileCachingService, SerialisationService serialisationService,
            BrandReferentialLoader referentialLoader) throws Exception {
        this.serialisationService = serialisationService;
        this.referentialLoader = referentialLoader;
        loadBrandMappings();
    }

    /**
     * Loads the v2 remote referential. The in-memory index always resolves
     * canonical names and synonyms to the same {@link Brand} instance.
     */
    protected void loadBrandMappings() throws Exception {
        try {
            String mappingsStr = referentialLoader.load();
            BrandReferential loaded = parseReferential(mappingsStr);
            indexReferential(loaded);
        } catch (Exception e) {
            LOGGER.error("Error while loading brand mappings", e);
            throw e;
        }
    }

    /**
     * Resolves a raw brand name to the reviewed canonical brand, including known
     * synonyms.
     *
     * @param brandName raw brand text from a product or external referential
     * @return canonical brand, or an unknown brand carrying the normalized name
     */
    public Brand resolve(String brandName) {
        return resolve(brandName, brandsAlias);
    }

    /**
     * Resolves a brand using legacy vertical aliases before the central synonym
     * index. This keeps existing YAML overrides operational while centralising
     * final brand canonicalisation.
     *
     * @param brandName raw brand text
     * @param legacyAliases vertical-specific alias map
     * @return canonical brand, or an unknown brand carrying the normalized name
     */
    public Brand resolve(String brandName, Map<String, String> legacyAliases) {
        String input = sanitizeBrand(brandName);
        if (StringUtils.isBlank(input)) {
            return new Brand("");
        }

        String alias = resolveLegacyAlias(brandName, input, legacyAliases);
        if (StringUtils.isNotBlank(alias)) {
            input = sanitizeBrand(alias);
        }

        String canonicalName = canonicalByIndexedName.get(input);
        Brand resolved = canonicalName == null ? null : brandsByName.get(canonicalName);
        if (resolved == null) {
            resolved = new Brand(input);
            incrementUnknown(input);
            LOGGER.debug("Brand not found in companies mapping: {}", resolved);
        } else {
            LOGGER.debug("Brand found in companies mapping: {}", resolved);
        }
        return resolved;
    }

    /**
     * Normalizes a brand name for lookups and durable storage.
     *
     * @param name raw brand text
     * @return uppercase, accent-free, whitespace-normalized brand name
     */
    public String sanitizeBrand(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        return StringUtils.stripAccents(StringUtils.normalizeSpace(name).toUpperCase()).trim();
    }

    public void incrementUnknown(String brand) {
        String normalized = sanitizeBrand(brand);
        if (StringUtils.isNotBlank(normalized)) {
            missCounter.merge(normalized, 1L, Long::sum);
        }
    }

    /**
     * Adds external evidence for diagnostics and suggestion generation.
     *
     * @param rawName raw source brand name
     * @param source evidence source name
     * @param sourceId source-specific identifier
     */
    public void addSourceEvidence(String rawName, String source, String sourceId) {
        addSourceEvidence(rawName, source, sourceId, 1L);
    }

    /**
     * Adds counted external evidence for diagnostics and suggestion generation.
     *
     * @param rawName raw source brand name
     * @param source evidence source name
     * @param sourceId source-specific identifier
     * @param count number of matching source observations
     */
    public void addSourceEvidence(String rawName, String source, String sourceId, long count) {
        Brand brand = resolve(rawName);
        String canonical = brand.getBrandName();
        if (StringUtils.isBlank(canonical)) {
            return;
        }
        evidenceByCanonicalName.computeIfAbsent(canonical, ignored -> new ArrayList<>())
                .add(new BrandSourceEvidence(source, sourceId, rawName, count));
    }

    /**
     * Generates review candidates from unresolved names and source evidence.
     * No candidate returned here is automatically applied as a synonym.
     *
     * @return sorted review suggestions
     */
    public List<BrandSuggestion> generateSuggestions() {
        List<BrandSuggestion> suggestions = new ArrayList<>();
        missCounter.forEach((name, count) -> {
            if (!isNoisySuggestion(name)) {
                BrandSuggestion suggestion = new BrandSuggestion();
                suggestion.setRawName(name);
                suggestion.setNormalizedName(sanitizeBrand(name));
                suggestion.setSource("miss-counter");
                suggestion.setEvidenceCount(count);
                suggestion.setConfidence(0.2d);
                suggestion.setReason("Unresolved brand observed in product data; review before adding as synonym.");
                suggestions.add(suggestion);
            }
        });
        return suggestions.stream()
                .sorted(Comparator.comparingLong(BrandSuggestion::getEvidenceCount).reversed())
                .toList();
    }

    public Optional<Brand> findCanonical(String brandName) {
        Brand brand = resolve(brandName);
        if (StringUtils.isBlank(brand.getCompanyName())) {
            return Optional.empty();
        }
        return Optional.of(brand);
    }

    public boolean hasLogo(String upperCase) {
        return false;
    }

    public void setBrandsAlias(Map<String, String> brandsAlias) {
        this.brandsAlias = brandsAlias;
    }

    public InputStream getLogo(String upperCase) {
        return null;
    }

    public Map<String, Long> getMissCounter() {
        return missCounter;
    }

    public Map<String, List<BrandSourceEvidence>> getEvidenceByCanonicalName() {
        return evidenceByCanonicalName;
    }

    public BrandReferential getReferential() {
        return referential;
    }

    private BrandReferential parseReferential(String mappingsStr) throws Exception {
        Map<String, Object> raw = serialisationService.fromJsonTypeRef(mappingsStr,
                new TypeReference<Map<String, Object>>() {
                });
        if (!raw.containsKey("brands")) {
            throw new IllegalArgumentException("Brand referential must use the v2 schema with a top-level brands array");
        }

        BrandReferential parsed = serialisationService.jsonMapper().readValue(mappingsStr, BrandReferential.class);
        if (parsed.getVersion() != 2) {
            throw new IllegalArgumentException("Unsupported brand referential version: " + parsed.getVersion());
        }
        return parsed;
    }

    private void indexReferential(BrandReferential loaded) {
        referential = loaded;
        brandsByName.clear();
        canonicalByIndexedName.clear();
        evidenceByCanonicalName.clear();

        for (BrandReferentialEntry entry : loaded.getBrands()) {
            String canonical = sanitizeBrand(StringUtils.defaultIfBlank(entry.getCanonicalName(), entry.getNormalizedName()));
            if (StringUtils.isBlank(canonical)) {
                continue;
            }
            entry.setNormalizedName(sanitizeBrand(StringUtils.defaultIfBlank(entry.getNormalizedName(), canonical)));

            Brand brand = new Brand(canonical);
            brand.setCompanyName(entry.getCompanyName());
            brandsByName.put(canonical, brand);
            indexName(canonical, canonical);
            indexName(entry.getCanonicalName(), canonical);
            indexName(entry.getNormalizedName(), canonical);
            entry.getSynonyms().forEach(synonym -> indexName(synonym, canonical));
            if (!entry.getSources().isEmpty()) {
                evidenceByCanonicalName.put(canonical, new ArrayList<>(entry.getSources()));
            }
        }
        LOGGER.info("Loaded {} reviewed brand mappings and {} suggestions",
                brandsByName.size(), loaded.getSuggestions().size());
    }

    private void indexName(String name, String canonicalName) {
        String normalized = sanitizeBrand(name);
        if (StringUtils.isNotBlank(normalized)) {
            canonicalByIndexedName.put(normalized, canonicalName);
        }
    }

    private String resolveLegacyAlias(String rawInput, String normalizedInput, Map<String, String> legacyAliases) {
        if (legacyAliases == null || legacyAliases.isEmpty()) {
            return null;
        }
        Map<String, String> normalizedAliases = normalizeAliasMap(legacyAliases);
        String rawAlias = rawInput == null ? null : legacyAliases.get(rawInput);
        return StringUtils.defaultIfBlank(rawAlias, normalizedAliases.get(normalizedInput));
    }

    private Map<String, String> normalizeAliasMap(Map<String, String> aliases) {
        Map<String, String> normalized = new LinkedHashMap<>();
        aliases.forEach((key, value) -> normalized.put(sanitizeBrand(key), value));
        return normalized;
    }

    private boolean isNoisySuggestion(String normalizedName) {
        String normalized = sanitizeBrand(normalizedName);
        return SUGGESTION_STOP_WORDS.contains(normalized) || normalized.startsWith("FOR ");
    }

    public Map<String, Brand> getBrandsByName() {
        return Map.copyOf(brandsByName);
    }
}
