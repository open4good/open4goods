package org.open4goods.b2bapi.dto.barcode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Advanced barcode rendering options.")
public record B2bBarcodeOptions(
    @Schema(description = "Output resolution in Dots Per Inch.", example = "300")
    Integer dpi,
    @Schema(description = "Width of the narrowest barcode module/bar in millimeters.", example = "0.33")
    Double moduleWidthMm,
    @Schema(description = "Height of the bars in millimeters.", example = "15.0")
    Double barHeightMm,
    @Schema(description = "Size of the human-readable text font in points.", example = "8.0")
    Double fontSize,
    @Schema(description = "Preconfigured optimization settings (e.g. 'print-safe').", example = "print-safe")
    String preset
) {
    public B2bBarcodeOptions {
        if (dpi == null) dpi = 300;
        if (moduleWidthMm == null) moduleWidthMm = 0.33;
        if (barHeightMm == null) barHeightMm = 15.0;
        if (fontSize == null) fontSize = 8.0;
    }
}
