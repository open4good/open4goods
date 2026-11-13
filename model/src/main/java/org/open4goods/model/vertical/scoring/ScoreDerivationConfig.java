package org.open4goods.model.vertical.scoring;

/**
 * How a derived score combines its source attributes ({@code AttributeConfig.scoreDerivation}). Without this block
 * a derived score uses {@link ScoreDerivationMethod#FIRST_AVAILABLE}.
 *
 * <p>For {@link ScoreDerivationMethod#SCREEN_AREA_DENSITY} the screen is a rectangle of the configured aspect ratio
 * (16:9 by default): for a diagonal {@code d} in inches, its area is {@code d² × w × h / (w² + h²)} square inches,
 * that is {@code d² × 144 / 337} in 16:9, and one square inch is {@link #SQUARE_DECIMETRES_PER_SQUARE_INCH} dm².
 */
public class ScoreDerivationConfig {

    /** Square decimetres in one square inch (2.54 cm squared). */
    public static final double SQUARE_DECIMETRES_PER_SQUARE_INCH = 0.064516;

    /** Default aspect ratio width, 16:9 being the format of the televisions sold today. */
    public static final double DEFAULT_ASPECT_RATIO_WIDTH = 16.0;

    /** Default aspect ratio height. */
    public static final double DEFAULT_ASPECT_RATIO_HEIGHT = 9.0;

    private ScoreDerivationMethod method = ScoreDerivationMethod.FIRST_AVAILABLE;

    private Double aspectRatioWidth = DEFAULT_ASPECT_RATIO_WIDTH;

    private Double aspectRatioHeight = DEFAULT_ASPECT_RATIO_HEIGHT;

    /**
     * Screen area of a diagonal, with the configured aspect ratio.
     *
     * @param diagonalInches screen diagonal in inches
     * @return the screen area in square decimetres
     */
    public double screenAreaSquareDecimetres(double diagonalInches) {
        double width = aspectRatioWidth;
        double height = aspectRatioHeight;
        double squareInches = diagonalInches * diagonalInches * width * height / (width * width + height * height);
        return squareInches * SQUARE_DECIMETRES_PER_SQUARE_INCH;
    }

    public ScoreDerivationMethod getMethod() {
        return method;
    }

    public void setMethod(ScoreDerivationMethod method) {
        this.method = method == null ? ScoreDerivationMethod.FIRST_AVAILABLE : method;
    }

    public Double getAspectRatioWidth() {
        return aspectRatioWidth;
    }

    public void setAspectRatioWidth(Double aspectRatioWidth) {
        this.aspectRatioWidth = aspectRatioWidth == null ? DEFAULT_ASPECT_RATIO_WIDTH : aspectRatioWidth;
    }

    public Double getAspectRatioHeight() {
        return aspectRatioHeight;
    }

    public void setAspectRatioHeight(Double aspectRatioHeight) {
        this.aspectRatioHeight = aspectRatioHeight == null ? DEFAULT_ASPECT_RATIO_HEIGHT : aspectRatioHeight;
    }
}
