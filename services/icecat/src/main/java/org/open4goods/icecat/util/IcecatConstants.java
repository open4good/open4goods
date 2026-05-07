package org.open4goods.icecat.util;

/**
 * Shared constants for the Icecat integration.
 */
public final class IcecatConstants {

    /** Icecat language ID for English (used in all language-keyed lookups). */
    public static final int LANG_ID_ENGLISH = 1;

    /** Icecat language ID for French. */
    public static final int LANG_ID_FRENCH = 3;

    /** Default datasource name used in DataFragment and datasourceCodes map. */
    public static final String DATASOURCE_NAME = "icecat.biz";

    /** Live API URL prefix; append the GTIN and pass the language parameter. */
    public static final String LIVE_API_URL_PREFIX =
            "https://live.icecat.biz/api?UserName=openIcecat-live&Language={lang}&GTIN=";

    private IcecatConstants() {
    }
}
