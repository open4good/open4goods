package org.open4goods.model.vertical.scoring;

/**
 * How a derived score ({@code AttributeConfig.scoreSources}) combines its source attributes.
 */
public enum ScoreDerivationMethod {

    /**
     * The raw value of the first source holding a parseable value, never an average (GOU-2041).
     */
    FIRST_AVAILABLE,

    /**
     * A value per square decimetre of screen (GOU-1884): the first source divided by the screen area computed from
     * the second source, a diagonal in inches, and the configured aspect ratio. Both sources are required.
     */
    SCREEN_AREA_DENSITY
}
