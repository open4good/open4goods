package org.open4goods.nudgerfrontapi.service;

import java.util.Objects;

import org.open4goods.nudgerfrontapi.dto.profile.ProfileDashboardDto;
import org.open4goods.nudgerfrontapi.dto.profile.ProfileMetricsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Service exposing profile dashboard aggregates for authenticated users.
 */
@Service
public class ProfileService {

    private final AgentService agentService;

    public ProfileService(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Build the profile dashboard payload for the authenticated user.
     *
     * @param authentication current authentication principal
     * @param domainLanguage requested domain language
     * @return dashboard aggregate payload
     */
    public ProfileDashboardDto getDashboard(Authentication authentication, DomainLanguage domainLanguage) {
        String username = authentication != null ? authentication.getName() : "unknown";
        int configuredAgents = agentService.listTemplates(domainLanguage).stream()
                .filter(template -> template.allowedRoles() == null || template.allowedRoles().isEmpty()
                        || hasAnyRole(authentication, template.allowedRoles()))
                .toList()
                .size();

        ProfileMetricsDto metrics = new ProfileMetricsDto(
                configuredAgents,
                0,
                0,
                0L,
                0L,
                0L,
                0L,
                0.0,
                0.0,
                "EUR");

        return new ProfileDashboardDto(username, "basic", "pending", metrics);
    }

    private boolean hasAnyRole(Authentication authentication, java.util.List<String> allowedRoles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(allowedRoles::contains);
    }
}
