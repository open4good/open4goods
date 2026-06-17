package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Barcode image dimensions.")
public record B2bBarcodeDimensions(
    @Schema(description = "Width of the generated barcode image in pixels.", example = "200")
    int width,
    @Schema(description = "Height of the generated barcode image in pixels.", example = "100")
    int height,
    @Schema(description = "DPI of the generated barcode image.", example = "300")
    int dpi
) {
}
