package org.open4goods.icecat.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.util.IcecatConstants;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves local attribute names against the Icecat feature index.
 *
 * <p>This service deliberately uses Elasticsearch as the source of truth instead of
 * the startup feature-loader maps. Lookups are cached by normalized attribute name
 * to avoid repeated Elasticsearch queries during product aggregation.
 */
public class IcecatFeatureResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatFeatureResolver.class);

    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "default", IcecatConstants.LANG_ID_ENGLISH,
            "en", IcecatConstants.LANG_ID_ENGLISH,
            "fr", 3);

    private final IcecatIndexService indexService;
    private final Map<String, Set<Integer>> featureIdsByNormalizedName = new ConcurrentHashMap<>();
    private final Map<Integer, IcecatFeatureDocument> featuresById = new ConcurrentHashMap<>();

    public IcecatFeatureResolver(IcecatIndexService indexService) {
        this.indexService = indexService;
    }

    /**
     * Resolves an attribute name to Icecat feature IDs using the Elasticsearch
     * {@code normalizedNames} field.
     *
     * @param featureName local or provider attribute name
     * @return matching Icecat feature IDs, or an empty set when unresolved
     */
    public Set<Integer> resolveFeatureName(String featureName) {
        String normalizedName = IdHelper.normalizeAttributeName(featureName);
        if (normalizedName == null || normalizedName.isBlank()) {
            return Set.of();
        }
        return featureIdsByNormalizedName.computeIfAbsent(normalizedName, this::resolveNormalizedName);
    }

    private Set<Integer> resolveNormalizedName(String normalizedName) {
        List<IcecatFeatureDocument> docs = indexService.findFeaturesByNormalizedName(normalizedName);
        for (IcecatFeatureDocument doc : docs) {
            if (doc.getId() != null) {
                featuresById.put(doc.getId(), doc);
            }
        }
        Set<Integer> ids = docs.stream()
                .map(IcecatFeatureDocument::getId)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.debug("Resolved Icecat normalized feature name {} to {}", normalizedName, ids);
        return ids;
    }

    /**
     * Returns a localized Icecat feature name from the Elasticsearch document.
     *
     * @param featureId Icecat feature ID
     * @param language language code, currently {@code default}, {@code en}, or {@code fr}
     * @return localized name, English fallback, or an unresolved marker
     */
    public String getFeatureName(Integer featureId, String language) {
        IcecatFeatureDocument doc = featureDocument(featureId);
        if (doc == null) {
            return "Unsolved : " + featureId + "," + languageId(language);
        }

        String localized = localizedName(doc, languageId(language));
        if (localized != null) {
            return localized;
        }
        if (doc.getEnglishName() != null) {
            return doc.getEnglishName();
        }
        return "Unsolved : " + featureId + "," + languageId(language);
    }

    /**
     * Resolves an attribute name to its canonical English Icecat name.
     *
     * @param name attribute name
     * @param vc optional vertical used to narrow ambiguous matches
     * @return canonical English name, or the original name when unresolved/ambiguous
     */
    public String getOriginalEnglishName(String name, VerticalConfig vc) {
        Set<Integer> featureIds = new HashSet<>(resolveFeatureName(name));

        if (featureIds.isEmpty()) {
            LOGGER.warn("No icecat name found for {}", name);
            return name;
        }

        if (vc != null && vc.getId() != null) {
            featureIds.retainAll(featuresId(vc));
            if (featureIds.isEmpty()) {
                LOGGER.warn("No icecat featureID for {}, after filtering on id's for vertical {}", name, vc);
                return name;
            }
        }

        if (featureIds.size() == 1) {
            String resolvedName = getFeatureName(featureIds.iterator().next(), "en");
            LOGGER.info("Resolved feature name : {}->{}", name, resolvedName);
            return resolvedName;
        }

        Set<String> attrNames = featureIds.stream()
                .map(id -> id + ":" + getFeatureName(id, "en"))
                .collect(Collectors.toSet());
        LOGGER.warn("Conflict! attr {} can be resolved to {}", name, attrNames);
        return name;
    }

    private IcecatFeatureDocument featureDocument(Integer featureId) {
        if (featureId == null) {
            return null;
        }
        IcecatFeatureDocument cached = featuresById.get(featureId);
        if (cached != null) {
            return cached;
        }
        return indexService.findFeature(featureId)
                .map(doc -> {
                    featuresById.put(featureId, doc);
                    return doc;
                })
                .orElse(null);
    }

    private Set<Integer> featuresId(VerticalConfig vertical) {
        Set<Integer> ret = new HashSet<>();
        if (vertical != null) {
            for (org.open4goods.model.vertical.FeatureGroup fg : vertical.getFeatureGroups()) {
                ret.addAll(fg.getFeaturesId());
            }
        }
        return ret;
    }

    private String localizedName(IcecatFeatureDocument doc, int languageId) {
        if (doc.getLangNames() == null) {
            return null;
        }
        String prefix = languageId + ":";
        return doc.getLangNames().stream()
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.substring(prefix.length()))
                .findFirst()
                .orElse(null);
    }

    private int languageId(String language) {
        return LANGUAGE_IDS.getOrDefault(language, IcecatConstants.LANG_ID_ENGLISH);
    }
}
