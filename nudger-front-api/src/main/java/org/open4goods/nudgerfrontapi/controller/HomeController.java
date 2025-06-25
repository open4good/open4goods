package org.open4goods.nudgerfrontapi.controller;

import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.HomeCompositeResponse;
import org.open4goods.nudgerfrontapi.service.HomeService;
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
 * REST endpoint providing aggregated data for the home page.
 */
@RestController
@Tag(name = "Home", description = "Aggregated data for the home page")
public class HomeController {

    private static final long TTL_SECONDS = 300;

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/home")
    @Operation(
            summary = "Get home data",
            description = "Return aggregated statistics and partners used on the home page.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HomeCompositeResponse.class)))
    })
    public ResponseEntity<HomeCompositeResponse> home() {
        HomeCompositeResponse resp = homeService.getHome();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(resp);
    }
}
