package org.open4goods.services.wikidataservice.util;

/**
 * Wikidata property identifiers used by the wikidata-service.
 *
 * <p>Property names follow the format Pxxx where xxx is the numeric identifier.
 * See https://www.wikidata.org/wiki/Wikidata:List_of_properties for the full list.
 */
public final class WikidataConstants {

    private WikidataConstants() {
    }

    /** SPARQL endpoint URL. */
    public static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    /** Wikidata REST API base URL for entity retrieval. */
    public static final String REST_API_BASE = "https://www.wikidata.org/w/api.php";

    /** Global Trade Item Number (GTIN-13/EAN-13). */
    public static final String P_GTIN = "P3962";

    /** Manufacturer / maker. */
    public static final String P_MANUFACTURER = "P176";

    /** Brand. */
    public static final String P_BRAND = "P1716";

    /** Image (Commons file name). */
    public static final String P_IMAGE = "P18";

    /** Video (Commons file name). */
    public static final String P_VIDEO = "P10";

    /** Official website URL. */
    public static final String P_WEBSITE = "P856";

    /** Publication / release date. */
    public static final String P_RELEASE_DATE = "P577";

    /** Width (quantity). */
    public static final String P_WIDTH = "P2049";

    /** Height (quantity). */
    public static final String P_HEIGHT = "P2048";

    /** Mass / weight (quantity). */
    public static final String P_MASS = "P2067";

    /** Depth / length (quantity). */
    public static final String P_DEPTH = "P2660";

    /** Color. */
    public static final String P_COLOR = "P462";

    /** Country of origin. */
    public static final String P_COUNTRY_OF_ORIGIN = "P495";

    /** Commons category link. */
    public static final String P_COMMONS_CATEGORY = "P373";

    /** Wikimedia Commons URL prefix for file pages. */
    public static final String COMMONS_FILE_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    /** Default language to use when no other is specified. */
    public static final String LANG_DEFAULT = "en";

    /** Languages to retrieve from Wikidata labels/descriptions. */
    public static final String[] LANGUAGES = {"en", "fr", "de", "es", "it"};
}
