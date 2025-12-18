package org.open4goods.nudgerfrontapi.service.share;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.nudgerfrontapi.config.properties.ShareResolutionProperties;
import org.open4goods.nudgerfrontapi.dto.share.ShareExtractionDto;
import org.open4goods.nudgerfrontapi.service.http.SimpleHttpFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Best-effort extractor for shared URLs.
 */
@Service
public class ShareExtractionService {

    private static final Pattern GTIN_PATTERN = Pattern.compile("\\b(\\d{8,14})\\b");

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareExtractionService.class);

    private final ShareResolutionProperties properties;
    private final SimpleHttpFetcher httpFetcher;

    public ShareExtractionService(ShareResolutionProperties properties, SimpleHttpFetcher httpFetcher) {
        this.properties = properties;
        this.httpFetcher = httpFetcher;
    }

    /**
     * Extract a GTIN or fallback query from the URL and optional content.
     *
     * @param url   origin URL
     * @param title optional title
     * @param text  optional text
     * @return optional extraction result
     */
    public Optional<ShareExtractionDto> extract(String url, String title, String text) {
        String content = null;
        if (properties.isCrawlerExtractionEnabled()) {
            content = httpFetcher.fetch(url);
        }

        String gtin = firstMatch(url, content, title, text);
        String query = buildQuery(url, title, text);

        if (!StringUtils.hasText(gtin) && !StringUtils.hasText(query)) {
            return Optional.empty();
        }

        return Optional.of(new ShareExtractionDto(gtin, query));
    }

    /**
     * Identify the first GTIN-like segment across all provided content fragments.
     *
     * @param url     shared URL
     * @param content body fetched from the crawler when enabled
     * @param title   optional title
     * @param text    optional text
     * @return matched GTIN or {@code null}
     */
    private String firstMatch(String url, String content, String title, String text) {
        Matcher matcher = GTIN_PATTERN.matcher(concatenate(url, content, title, text));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Merge all content sources into a single searchable string.
     *
     * @param url     shared URL
     * @param content fetched body
     * @param title   optional title
     * @param text    optional text
     * @return concatenated representation
     */
    private String concatenate(String url, String content, String title, String text) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(url)) {
            builder.append(url).append(' ');
        }
        if (StringUtils.hasText(content)) {
            builder.append(content).append(' ');
        }
        if (StringUtils.hasText(title)) {
            builder.append(title).append(' ');
        }
        if (StringUtils.hasText(text)) {
            builder.append(text);
        }
        return builder.toString();
    }

    /**
     * Build a search query using user provided hints or a slug fallback.
     *
     * @param url   shared URL
     * @param title optional title
     * @param text  optional text
     * @return derived query or {@code null}
     */
    private String buildQuery(String url, String title, String text) {
        if (StringUtils.hasText(title)) {
            return title;
        }
        if (StringUtils.hasText(text)) {
            return text;
        }
        return deriveSlugQuery(url);
    }

    /**
     * Derive a readable query from the last URL segment.
     *
     * @param url shared URL
     * @return slug-based query
     */
    private String deriveSlugQuery(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            String[] segments = path.split("/");
            for (int i = segments.length - 1; i >= 0; i--) {
                String segment = segments[i];
                if (StringUtils.hasText(segment)) {
                    return segment.replace('-', ' ').replace('_', ' ').trim();
                }
            }
            return null;
        } catch (URISyntaxException e) {
            LOGGER.warn("Failed to derive slug query from URL {}: {}", url, e.getMessage());
            return null;
        }
    }
}
