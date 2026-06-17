package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;
import org.open4goods.b2bapi.exception.InvalidBarcodeException;

@Schema(description = "Request body for rendering a barcode.")
public record B2bBarcodeRenderRequest(
    @Schema(description = "Barcode symbology type.", example = "ean13", required = true)
    String type,
    @Schema(description = "Data string to encode.", example = "4006381333931", required = true)
    String data,
    @Schema(description = "Output format ('png' or 'svg').", example = "png")
    String format,
    @Schema(description = "Target image width in pixels.", example = "200")
    Integer width,
    @Schema(description = "Target image height in pixels.", example = "100")
    Integer height,
    @Schema(description = "Foreground hex color for bars/text.", example = "#000000")
    String foreground,
    @Schema(description = "Background hex color.", example = "#ffffff")
    String background,
    @Schema(description = "Rotation in degrees (0, 90, 180, 270).", example = "0")
    Integer rotation,
    @Schema(description = "Whether to show the human-readable text.", example = "true")
    Boolean showText,
    @Schema(description = "Whether to include a quiet zone around the barcode.", example = "true")
    Boolean quietZone,
    @Schema(description = "Advanced symbology-specific configuration options.")
    B2bBarcodeOptions options,
    @Schema(description = "Custom copyright and creator metadata to embed in the image output.")
    B2bBarcodeMetadata metadata
) {
    public B2bBarcodeRenderRequest {
        if (type == null || type.isBlank()) {
            throw new InvalidBarcodeException("Barcode type is required");
        }
        if (data == null || data.isBlank()) {
            throw new InvalidBarcodeException("Barcode data is required");
        }
        if (format == null) {
            format = "png";
        } else {
            format = format.toLowerCase();
            if (!format.equals("png") && !format.equals("svg")) {
                throw new InvalidBarcodeException("Unsupported format: " + format);
            }
        }
        if (width == null) width = 200;
        if (height == null) height = 100;
        if (width <= 0 || height <= 0) {
            throw new InvalidBarcodeException("Width and height must be positive integers");
        }
        if (foreground == null || foreground.isBlank()) {
            foreground = "#000000";
        } else {
            foreground = normalizeHexColor(foreground);
        }
        if (background == null || background.isBlank()) {
            background = "#ffffff";
        } else {
            background = normalizeHexColor(background);
        }
        if (rotation == null) {
            rotation = 0;
        } else if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            throw new InvalidBarcodeException("Rotation must be 0, 90, 180, or 270");
        }
        if (showText == null) showText = true;
        if (quietZone == null) quietZone = true;
        if (options == null) options = new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe");
    }

    private static String normalizeHexColor(String color) {
        String hex = color.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("^[0-9a-fA-F]{3,8}$")) {
            throw new InvalidBarcodeException("Invalid hex color code: " + color);
        }
        if (hex.length() == 3) {
            StringBuilder sb = new StringBuilder();
            for (char c : hex.toCharArray()) {
                sb.append(c).append(c);
            }
            hex = sb.toString();
        }
        return "#" + hex.toLowerCase();
    }
}
