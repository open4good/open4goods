package org.open4goods.nudgerfrontapi.controller;

import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.StatsDto;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoint exposing statistics.
 */
@RestController
@Tag(name = "Statistics", description = "Site usage and product statistics")
public class StatsController {

    private static final long TTL_SECONDS = 300;

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get statistics",
            description = "Return site statistics such as product and review counts.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StatsDto.class)))
    })
    public ResponseEntity<StatsDto> stats() {
        StatsDto dto = statsService.fetchStats();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(dto);
    }
}
