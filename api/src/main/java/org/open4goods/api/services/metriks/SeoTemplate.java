package org.open4goods.api.services.metriks;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Page templates ("gabarits") of nudger.fr as seen from Search Console, classified from the URL path alone.
 * <p>
 * The patterns mirror the Nuxt routing of the frontend: a product page is {@code /<category>/<gtin>-<slug>}
 * (or {@code /<gtin>-<slug>}), a category page is a single letters-and-hyphens segment, and a two-segment path
 * {@code /<category>/<slug>} that is not a product is a sub-category page. Sub-category pages and per-category
 * buying guides share that second shape, so they cannot be told apart without the vertical configuration:
 * both count as {@link #SOUS_CATEGORIE}; the editorial {@code /guides/**} pages are {@link #GUIDE}.
 */
public enum SeoTemplate {
    /** Product page. */
    FICHE("fiche"),
    /** Vertical (category) page. */
    CATEGORIE("categorie"),
    /** Sub-category page (or per-category guide, see class comment). */
    SOUS_CATEGORIE("sous-categorie"),
    /** Blog article or blog index. */
    BLOG("blog"),
    /** Editorial buying guide under {@code /guides}. */
    GUIDE("guide"),
    /** Open data pages. */
    OPENDATA("opendata"),
    /** PDF documents. */
    PDF("pdf"),
    /** Anything else (home page, static pages, unparsable URL). */
    AUTRE("autre");

    private static final Pattern PRODUCT_SEGMENT = Pattern.compile("^\\d{6,}-.+$");
    private static final Pattern CATEGORY_SEGMENT = Pattern.compile("^[a-z]+(?:-[a-z]+)*$");

    private final String id;

    SeoTemplate(String id) {
        this.id = id;
    }

    /** @return stable identifier used in Metriks event ids and params */
    public String id() {
        return id;
    }

    /**
     * Classifies a Search Console page URL (absolute, or a bare path).
     *
     * @param pageUrl the page key returned by the Search Analytics {@code page} dimension
     * @return the template, {@link #AUTRE} when nothing matches
     */
    public static SeoTemplate classify(String pageUrl) {
        String path = pathOf(pageUrl);
        if (path.endsWith(".pdf")) {
            return PDF;
        }
        String[] segments = path.isEmpty() ? new String[0] : path.substring(1).split("/");
        if (segments.length == 0) {
            return AUTRE;
        }
        String first = segments[0];
        if ("blog".equals(first)) {
            return BLOG;
        }
        if ("guides".equals(first)) {
            return GUIDE;
        }
        if ("opendata".equals(first)) {
            return OPENDATA;
        }
        for (String segment : segments) {
            if (PRODUCT_SEGMENT.matcher(segment).matches()) {
                return FICHE;
            }
        }
        if (segments.length == 1 && CATEGORY_SEGMENT.matcher(first).matches()) {
            return CATEGORIE;
        }
        if (segments.length == 2 && CATEGORY_SEGMENT.matcher(first).matches()) {
            return SOUS_CATEGORIE;
        }
        return AUTRE;
    }

    /** Lower-cased path of the URL without query, fragment or trailing slash ({@code ""} for the root). */
    static String pathOf(String pageUrl) {
        if (pageUrl == null) {
            return "";
        }
        String path;
        try {
            path = URI.create(pageUrl.trim()).getPath();
        } catch (IllegalArgumentException e) {
            path = pageUrl.trim();
        }
        if (path == null) {
            return "";
        }
        path = path.toLowerCase(Locale.ROOT);
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
