package org.open4goods.services.wikidataservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.util.WikidataConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the raw Wikidata API response map into a typed {@link WikidataEntity}.
 *
 * <p>The Wikidata API returns a deeply nested structure; this class extracts only
 * the fields needed for product enrichment and is the single place where that
 * parsing logic lives. It is stateless and safe to share across threads.
 */
public class WikidataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataParser.class);

    private final List<String> languages;

    /**
     * Creates a parser that extracts data for the given language codes.
     *
     * @param languages BCP-47 language codes (e.g. "en", "fr")
     */
    public WikidataParser(List<String> languages) {
        this.languages = languages;
    }

    /**
     * Converts the raw entity map (as returned by {@code wbgetentities}) into a
     * {@link WikidataEntity}.
     *
     * @param qId the Q-identifier
     * @param entityMap the raw entity map for this Q-id
     * @return populated entity; never null but may be mostly empty on bad input
     */
    @SuppressWarnings("unchecked")
    public WikidataEntity parse(String qId, Map<String, Object> entityMap) {
        WikidataEntity entity = new WikidataEntity();
        entity.setQId(qId);
        entity.setLastFetchedAt(System.currentTimeMillis());

        if (entityMap == null || entityMap.isEmpty()) {
            return entity;
        }

        parseLabels(entity, entityMap);
        parseAliases(entity, entityMap);
        parseDescriptions(entity, entityMap);
        parseSitelinks(entity, entityMap);

        Object rawClaims = entityMap.get("claims");
        if (rawClaims instanceof Map<?, ?> claimsMap) {
            parseClaims(entity, (Map<String, Object>) claimsMap);
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    private void parseLabels(WikidataEntity entity, Map<String, Object> entityMap) {
        Object labelsObj = entityMap.get("labels");
        if (!(labelsObj instanceof Map<?, ?> labelsMap)) {
            return;
        }
        for (String lang : languages) {
            Object labelObj = labelsMap.get(lang);
            if (labelObj instanceof Map<?, ?> label) {
                Object value = label.get("value");
                if (value != null) {
                    entity.getLabels().add(lang + ":" + value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAliases(WikidataEntity entity, Map<String, Object> entityMap) {
        Object aliasesObj = entityMap.get("aliases");
        if (!(aliasesObj instanceof Map<?, ?> aliasesMap)) {
            return;
        }
        for (String lang : languages) {
            Object langAliases = aliasesMap.get(lang);
            if (langAliases instanceof List<?> aliasList) {
                for (Object aliasObj : aliasList) {
                    if (aliasObj instanceof Map<?, ?> alias) {
                        Object value = alias.get("value");
                        if (value != null) {
                            entity.getAliases().add(lang + ":" + value);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseDescriptions(WikidataEntity entity, Map<String, Object> entityMap) {
        Object descsObj = entityMap.get("descriptions");
        if (!(descsObj instanceof Map<?, ?> descsMap)) {
            return;
        }
        for (String lang : languages) {
            Object descObj = descsMap.get(lang);
            if (descObj instanceof Map<?, ?> desc) {
                Object value = desc.get("value");
                if (value != null) {
                    entity.getDescriptions().add(lang + ":" + value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseSitelinks(WikidataEntity entity, Map<String, Object> entityMap) {
        Object sitelinksObj = entityMap.get("sitelinks");
        if (!(sitelinksObj instanceof Map<?, ?> sitelinksMap)) {
            return;
        }
        for (String lang : languages) {
            String siteKey = lang + "wiki";
            Object sitelinkObj = sitelinksMap.get(siteKey);
            if (sitelinkObj instanceof Map<?, ?> sitelink) {
                Object title = sitelink.get("title");
                Object url = sitelink.get("url");
                if (url != null) {
                    entity.getWikipediaUrls().add(lang + ":" + url);
                } else if (title != null) {
                    String encoded = title.toString().replace(" ", "_");
                    entity.getWikipediaUrls().add(lang + ":https://" + lang + ".wikipedia.org/wiki/" + encoded);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseClaims(WikidataEntity entity, Map<String, Object> claims) {
        parseStringClaim(claims, WikidataConstants.P_GTIN, entity.getGtins());
        parseStringClaim(claims, WikidataConstants.P_WEBSITE, entity, "website");
        parseStringClaim(claims, WikidataConstants.P_IMAGE, entity.getImages());
        parseStringClaim(claims, WikidataConstants.P_VIDEO, entity.getVideos());

        parseDateClaim(claims, WikidataConstants.P_RELEASE_DATE, entity);

        parseEntityLabelClaim(claims, WikidataConstants.P_MANUFACTURER, entity.getBrandLabels());
        parseEntityLabelClaim(claims, WikidataConstants.P_BRAND, entity.getBrandLabels());

        parseQuantityClaim(claims, WikidataConstants.P_WIDTH, entity);
        parseQuantityClaim(claims, WikidataConstants.P_HEIGHT, entity);
        parseQuantityClaim(claims, WikidataConstants.P_MASS, entity);
        parseQuantityClaim(claims, WikidataConstants.P_DEPTH, entity);
    }

    @SuppressWarnings("unchecked")
    private void parseStringClaim(Map<String, Object> claims, String property, List<String> target) {
        Object statementsObj = claims.get(property);
        if (!(statementsObj instanceof List<?> statements)) {
            return;
        }
        for (Object statementObj : statements) {
            if (!(statementObj instanceof Map<?, ?> statement)) {
                continue;
            }
            String value = extractStringValue((Map<String, Object>) statement);
            if (value != null && !target.contains(value)) {
                target.add(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseStringClaim(Map<String, Object> claims, String property,
            WikidataEntity entity, String field) {
        Object statementsObj = claims.get(property);
        if (!(statementsObj instanceof List<?> statements) || statements.isEmpty()) {
            return;
        }
        Object first = statements.get(0);
        if (!(first instanceof Map<?, ?> statement)) {
            return;
        }
        String value = extractStringValue((Map<String, Object>) statement);
        if (value != null && "website".equals(field)) {
            entity.setWebsite(value);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseDateClaim(Map<String, Object> claims, String property, WikidataEntity entity) {
        Object statementsObj = claims.get(property);
        if (!(statementsObj instanceof List<?> statements) || statements.isEmpty()) {
            return;
        }
        Object first = statements.get(0);
        if (!(first instanceof Map<?, ?> statement)) {
            return;
        }
        Map<String, Object> mainsnak = extractMainsnak((Map<String, Object>) statement);
        if (mainsnak == null) {
            return;
        }
        Object datavalue = mainsnak.get("datavalue");
        if (!(datavalue instanceof Map<?, ?> dv)) {
            return;
        }
        Object value = dv.get("value");
        if (!(value instanceof Map<?, ?> timeMap)) {
            return;
        }
        Object time = timeMap.get("time");
        if (time != null) {
            String timeStr = time.toString();
            // Wikidata time format: +YYYY-MM-DDT...
            if (timeStr.length() >= 5) {
                String year = timeStr.startsWith("+") ? timeStr.substring(1, 5) : timeStr.substring(0, 4);
                entity.setReleaseYear(year);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseEntityLabelClaim(Map<String, Object> claims, String property, List<String> target) {
        Object statementsObj = claims.get(property);
        if (!(statementsObj instanceof List<?> statements)) {
            return;
        }
        for (Object statementObj : statements) {
            if (!(statementObj instanceof Map<?, ?> statement)) {
                continue;
            }
            Map<String, Object> mainsnak = extractMainsnak((Map<String, Object>) statement);
            if (mainsnak == null) {
                continue;
            }
            Object datavalue = mainsnak.get("datavalue");
            if (!(datavalue instanceof Map<?, ?> dv)) {
                continue;
            }
            Object value = dv.get("value");
            if (value instanceof Map<?, ?> entityValue) {
                Object id = entityValue.get("id");
                if (id != null) {
                    String label = id.toString();
                    if (!target.contains(label)) {
                        target.add(label);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseQuantityClaim(Map<String, Object> claims, String property, WikidataEntity entity) {
        Object statementsObj = claims.get(property);
        if (!(statementsObj instanceof List<?> statements) || statements.isEmpty()) {
            return;
        }
        Object first = statements.get(0);
        if (!(first instanceof Map<?, ?> statement)) {
            return;
        }
        Map<String, Object> mainsnak = extractMainsnak((Map<String, Object>) statement);
        if (mainsnak == null) {
            return;
        }
        Object datavalue = mainsnak.get("datavalue");
        if (!(datavalue instanceof Map<?, ?> dv)) {
            return;
        }
        Object value = dv.get("value");
        if (!(value instanceof Map<?, ?> quantityMap)) {
            return;
        }
        Object amount = quantityMap.get("amount");
        Object unit = quantityMap.get("unit");
        if (amount != null) {
            String amountStr = amount.toString().replaceFirst("^\\+", "");
            String unitStr = "";
            if (unit != null && !unit.toString().equals("1")) {
                String unitUri = unit.toString();
                int lastSlash = unitUri.lastIndexOf('/');
                if (lastSlash >= 0) {
                    unitStr = " " + unitUri.substring(lastSlash + 1);
                }
            }
            entity.getNumericClaims().put(property, amountStr + unitStr);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractStringValue(Map<String, Object> statement) {
        Map<String, Object> mainsnak = extractMainsnak(statement);
        if (mainsnak == null) {
            return null;
        }
        Object datavalue = mainsnak.get("datavalue");
        if (!(datavalue instanceof Map<?, ?> dv)) {
            return null;
        }
        Object value = dv.get("value");
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMainsnak(Map<String, Object> statement) {
        Object mainsnak = statement.get("mainsnak");
        if (!(mainsnak instanceof Map<?, ?> mainsnakMap)) {
            return null;
        }
        String snaktype = (String) mainsnakMap.get("snaktype");
        if (!"value".equals(snaktype)) {
            return null;
        }
        return (Map<String, Object>) mainsnakMap;
    }
}
