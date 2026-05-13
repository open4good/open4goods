package org.open4goods.services.urlfetching.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.open4goods.services.urlfetching.dto.ExtractedMetadataAttribute;
import org.open4goods.services.urlfetching.dto.ExtractedResource;
import org.open4goods.services.urlfetching.dto.ResourceType;

/**
 * Extracts downloadable and media resources from pages that look like official brand pages.
 */
public final class OfficialResourceExtractor {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> BRAND_KEYS = Set.of("brand", "manufacturer", "productbrand");
    private static final Set<String> IMAGE_KEYS = Set.of("image", "ogimage", "twitterimage");

    private OfficialResourceExtractor() {
    }

    /**
     * Extracts PDF, video, and only strongly signalled product image resources.
     *
     * @param pageUrl fetched page URL
     * @param html fetched HTML
     * @param metadata structured metadata extracted from the page
     * @return ordered unique resources, empty when the host does not look official
     */
    public static List<ExtractedResource> extract(String pageUrl, String html,
            List<ExtractedMetadataAttribute> metadata) {
        if (html == null || html.isBlank() || !isOfficialBrandHost(pageUrl, metadata)) {
            return List.of();
        }

        Document document = Jsoup.parse(html, pageUrl);
        Map<String, ExtractedResource> resources = new LinkedHashMap<>();
        addPdfResources(document, resources);
        addVideoResources(document, resources);
        addStructuredProductImages(document, metadata, resources);
        return List.copyOf(resources.values());
    }

    private static void addPdfResources(Document document, Map<String, ExtractedResource> resources) {
        for (Element link : document.select("a[href]")) {
            String absoluteUrl = link.absUrl("href");
            if (hasExtension(absoluteUrl, ".pdf")) {
                add(resources, absoluteUrl, ResourceType.PDF, "link", link.text());
            }
        }
    }

    private static void addVideoResources(Document document, Map<String, ExtractedResource> resources) {
        for (Element element : document.select("video[src], video source[src]")) {
            add(resources, element.absUrl("src"), ResourceType.VIDEO, "video", element.attr("title"));
        }
        for (Element link : document.select("a[href]")) {
            String absoluteUrl = link.absUrl("href");
            if (isVideoUrl(absoluteUrl)) {
                add(resources, absoluteUrl, ResourceType.VIDEO, "link", link.text());
            }
        }
        for (Element frame : document.select("iframe[src], embed[src]")) {
            String absoluteUrl = frame.absUrl("src");
            if (isVideoEmbedUrl(absoluteUrl)) {
                add(resources, absoluteUrl, ResourceType.VIDEO, "embed", frame.attr("title"));
            }
        }
    }

    private static void addStructuredProductImages(Document document, List<ExtractedMetadataAttribute> metadata,
            Map<String, ExtractedResource> resources) {
        if (metadata == null) {
            return;
        }
        for (ExtractedMetadataAttribute attribute : metadata) {
            if (!IMAGE_KEYS.contains(normalize(attribute.name()))) {
                continue;
            }
            String absoluteUrl = resolveUrl(document.baseUri(), attribute.value());
            if (isImageUrl(absoluteUrl)) {
                add(resources, absoluteUrl, ResourceType.IMAGE, attribute.source(), attribute.name());
            }
        }
    }

    private static boolean isOfficialBrandHost(String pageUrl, List<ExtractedMetadataAttribute> metadata) {
        String host = host(pageUrl);
        if (host.isBlank() || metadata == null || metadata.isEmpty()) {
            return false;
        }
        String normalizedHost = normalize(host);
        return brandValues(metadata).stream().anyMatch(brand -> hostContainsBrand(normalizedHost, brand));
    }

    private static List<String> brandValues(List<ExtractedMetadataAttribute> metadata) {
        List<String> brands = new ArrayList<>();
        for (ExtractedMetadataAttribute attribute : metadata) {
            if (BRAND_KEYS.contains(normalize(attribute.name())) && attribute.value() != null
                    && !attribute.value().isBlank()) {
                brands.add(attribute.value());
            }
        }
        return brands;
    }

    private static boolean hostContainsBrand(String normalizedHost, String brand) {
        String normalizedBrand = normalize(brand);
        if (normalizedBrand.length() >= 3 && normalizedHost.contains(normalizedBrand)) {
            return true;
        }
        for (String token : NON_ALPHANUMERIC.split(brand.toLowerCase(Locale.ROOT))) {
            if (token.length() >= 3 && normalizedHost.contains(token)) {
                return true;
            }
            if (token.length() == 2 && normalizedHost.startsWith(token)) {
                return true;
            }
        }
        return false;
    }

    private static void add(Map<String, ExtractedResource> resources, String url, ResourceType type, String source,
            String label) {
        if (url == null || url.isBlank()) {
            return;
        }
        resources.putIfAbsent(url, new ExtractedResource(url, type, source, blankToNull(label)));
    }

    private static boolean hasExtension(String url, String extension) {
        String path = path(url);
        return path.endsWith(extension);
    }

    private static boolean isVideoUrl(String url) {
        String path = path(url);
        return path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mov") || path.endsWith(".m4v");
    }

    private static boolean isVideoEmbedUrl(String url) {
        String normalized = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return normalized.contains("youtube.com/embed/") || normalized.contains("youtu.be/")
                || normalized.contains("player.vimeo.com/video/") || normalized.contains("dailymotion.com/embed/");
    }

    private static boolean isImageUrl(String url) {
        String path = path(url);
        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp")
                || path.endsWith(".avif");
    }

    private static String path(String url) {
        try {
            return URI.create(url).getPath().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private static String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host;
        } catch (Exception e) {
            return "";
        }
    }

    private static String resolveUrl(String baseUrl, String value) {
        if (value == null || value.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            return value;
        }
        try {
            return URI.create(baseUrl).resolve(value).toString();
        } catch (Exception e) {
            return value;
        }
    }

    private static String normalize(String raw) {
        return NON_ALPHANUMERIC.matcher(raw == null ? "" : raw.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }
}
