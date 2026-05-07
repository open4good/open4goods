package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.profile.ProfileDashboardDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProfileService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints exposing authenticated user profile dashboards.
 */
@RestController
@RequestMapping("/profile")
@Tag(name = "Profile", description = "Authenticated profile dashboards")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Return profile dashboard metrics and statuses for the authenticated user.
     *
     * @param domainLanguage requested domain language
     * @param authentication current authenticated principal
     * @return profile dashboard payload
     */
    @GetMapping("/dashboard")
    @Operation(
            summary = "Get profile dashboard",
            description = "Return account metrics, trust status and profile KPIs for the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile dashboard resolved"),
                    @ApiResponse(responseCode = "401", description = "Authentication required")
            })
    public ResponseEntity<ProfileDashboardDto> getDashboard(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(profileService.getDashboard(authentication, domainLanguage));
    }
}
