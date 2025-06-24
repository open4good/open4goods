package org.open4goods.nudgerfrontapi.controller;

import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.HomeCompositeResponse;
import org.open4goods.nudgerfrontapi.service.HomeService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint providing aggregated data for the home page.
 */
@RestController
public class HomeController {

    private static final long TTL_SECONDS = 300;

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/home")
    public ResponseEntity<HomeCompositeResponse> home() {
        HomeCompositeResponse resp = homeService.getHome();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(resp);
    }
}
