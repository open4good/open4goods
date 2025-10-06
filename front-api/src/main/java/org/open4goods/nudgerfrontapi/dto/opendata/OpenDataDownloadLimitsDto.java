package org.open4goods.nudgerfrontapi.dto.opendata;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing the download rate limits applied to the OpenData datasets.
 */
@Schema(description = "Download limits applied to OpenData dataset transfers.")
public record OpenDataDownloadLimitsDto(
        @Schema(description = "Maximum download speed per client expressed in kilobytes per second.", example = "256")
        int downloadSpeedKb,

        @Schema(description = "Human readable download speed formatted according to the requested locale.",
                example = "256\u00a0kB/s")
        String downloadSpeed,

        @Schema(description = "Maximum number of concurrent downloads accepted by the platform.", example = "4")
        int concurrentDownloads
) {
}
