package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Administrative endpoints to manage runtime behaviours of the frontend API.
 */
@RestController
@RequestMapping("/admin")
@Validated
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Administration", description = "Runtime administrative operations")
public class AdminController {

    private final CacheManager cacheManager;

    /**
     * Create an administrative controller backed by the provided cache manager.
     *
     * @param cacheManager Spring cache manager used to access configured caches
     */
    public AdminController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Reset all configured Spring caches in the application context.
     *
     * @return {@link ResponseEntity} with no content once caches are cleared
     */
    @PostMapping("/cache/reset")
    @Operation(
            summary = "Reset every Spring cache",
            description = "Clear the content of all caches configured for the frontend API.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Caches cleared"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated")
            }
    )
    public ResponseEntity<Void> resetCache() {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(cache -> cache != null)
                .forEach(Cache::clear);
        return ResponseEntity.noContent().build();
    }
}
