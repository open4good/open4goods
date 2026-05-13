package org.open4goods.services.urlfetching.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.open4goods.services.urlfetching.dto.ExtractedMetadataAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Extracts schema.org JSON-LD, HTML meta, and itemprop metadata from fetched HTML.
 */
public final class StructuredMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(StructuredMetadataExtractor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> JSONLD_PRODUCT_KEYS = Set.of("name", "brand", "manufacturer", "model", "sku",
            "mpn", "gtin", "gtin8", "gtin12", "gtin13", "gtin14", "color", "material", "size", "weight", "height",
            "width", "depth", "description", "image");
    private static final Set<String> META_KEYS = Set.of("title", "description", "og:title", "og:description",
            "product:brand", "product:retailer_item_id", "product:price:amount", "product:price:currency",
            "twitter:title", "twitter:description", "og:image", "twitter:image");

    private StructuredMetadataExtractor() {
    }

    public static ExtractionResult extract(String html) {
        if (html == null || html.isBlank()) {
            return new ExtractionResult(List.of(), Set.of());
        }
        Document document = Jsoup.parse(html);
        List<ExtractedMetadataAttribute> attributes = new ArrayList<>();
        Set<String> gtins = new LinkedHashSet<>();

        extractJsonLd(document, attributes, gtins);
        extractMeta(document, attributes);
        extractItemProps(document, attributes, gtins);

        return new ExtractionResult(List.copyOf(attributes), Set.copyOf(gtins));
    }

    private static void extractJsonLd(Document document, List<ExtractedMetadataAttribute> attributes, Set<String> gtins) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            String json = script.data();
            if (json == null || json.isBlank()) {
                json = script.html();
            }
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                JsonNode root = OBJECT_MAPPER.readTree(json);
                walkJsonLd(root, attributes, gtins);
            } catch (Exception e) {
                logger.debug("Cannot parse JSON-LD metadata: {}", e.getMessage());
            }
        }
    }

    private static void walkJsonLd(JsonNode node, List<ExtractedMetadataAttribute> attributes, Set<String> gtins) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                walkJsonLd(child, attributes, gtins);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        JsonNode graph = node.get("@graph");
        if (graph != null) {
            walkJsonLd(graph, attributes, gtins);
        }

        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            String key = normalizeName(entry.getKey());
            JsonNode value = entry.getValue();
            if (value == null || value.isMissingNode() || value.isNull()) {
                continue;
            }
            if (isGtinKey(key)) {
                String gtin = scalarValue(value);
                if (gtin != null) {
                    gtins.add(normalizeGtin(gtin));
                    add(attributes, key, gtin, "jsonld", null);
                }
                continue;
            }
            if (JSONLD_PRODUCT_KEYS.contains(key)) {
                String scalar = scalarValue(value);
                if (scalar != null) {
                    add(attributes, key, scalar, "jsonld", null);
                } else if (key.equals("image") && value.isArray()) {
                    for (JsonNode child : value) {
                        String image = scalarValue(child);
                        if (image != null) {
                            add(attributes, key, image, "jsonld", null);
                        }
                    }
                }
            }
            if (value.isArray() || value.isObject()) {
                walkJsonLd(value, attributes, gtins);
            }
        }
    }

    private static void extractMeta(Document document, List<ExtractedMetadataAttribute> attributes) {
        for (Element meta : document.select("meta[name], meta[property]")) {
            String key = meta.hasAttr("property") ? meta.attr("property") : meta.attr("name");
            String value = meta.attr("content");
            key = normalizeName(key);
            if (META_KEYS.contains(key)) {
                add(attributes, key, value, "meta", null);
            }
        }
    }

    private static void extractItemProps(Document document, List<ExtractedMetadataAttribute> attributes, Set<String> gtins) {
        for (Element element : document.select("[itemprop]")) {
            String key = normalizeName(element.attr("itemprop"));
            if (!JSONLD_PRODUCT_KEYS.contains(key) && !isGtinKey(key)) {
                continue;
            }
            String value = element.hasAttr("content") ? element.attr("content") : element.text();
            if (isGtinKey(key)) {
                gtins.add(normalizeGtin(value));
            }
            add(attributes, key, value, "itemprop", null);
        }
    }

    private static String scalarValue(JsonNode node) {
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            JsonNode name = node.get("name");
            if (name != null && (name.isTextual() || name.isNumber())) {
                return name.asText();
            }
        }
        return null;
    }

    private static void add(List<ExtractedMetadataAttribute> attributes, String name, String value, String source,
            String language) {
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return;
        }
        attributes.add(new ExtractedMetadataAttribute(name, value.trim(), source, language));
    }

    private static boolean isGtinKey(String key) {
        return key != null && (key.equals("gtin") || key.equals("gtin8") || key.equals("gtin12")
                || key.equals("gtin13") || key.equals("gtin14"));
    }

    public static String normalizeGtin(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static String normalizeName(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public record ExtractionResult(List<ExtractedMetadataAttribute> attributes, Set<String> gtins) {
    }
}
