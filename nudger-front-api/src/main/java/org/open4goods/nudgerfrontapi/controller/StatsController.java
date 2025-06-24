package org.open4goods.nudgerfrontapi.controller;

import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.StatsDto;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint exposing statistics.
 */
@RestController
public class StatsController {

    private static final long TTL_SECONDS = 300;

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDto> stats() {
        StatsDto dto = statsService.fetchStats();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(dto);
    }
}
