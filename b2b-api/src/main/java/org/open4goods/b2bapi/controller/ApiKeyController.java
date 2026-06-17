package org.open4goods.b2bapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.ApiKeySecretResponse;
import org.open4goods.b2bapi.dto.CreateApiKeyRequest;
import org.open4goods.b2bapi.service.ApiKeyService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer API key management endpoints.
 */
@RestController
@RequestMapping("/api/v1/customer/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(final ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("@organizationRbacService.canManageAnyApiKey(authentication)"
            + " or @organizationRbacService.canUsePlaygroundOrReadUsage(authentication)")
    public List<ApiKeyDto> list(final Authentication authentication) {
        return apiKeyService.list(principal(authentication));
    }

    @PostMapping
    @PreAuthorize("@organizationRbacService.canManageAnyApiKey(authentication)"
            + " or @organizationRbacService.canUsePlaygroundOrReadUsage(authentication)")
    public ApiKeySecretResponse create(@Valid @RequestBody final CreateApiKeyRequest request,
            final Authentication authentication) {
        return apiKeyService.create(principal(authentication), request.name());
    }

    @PostMapping("/{apiKeyId}/rotate")
    @PreAuthorize("@organizationRbacService.canManageAnyApiKey(authentication)"
            + " or @organizationRbacService.canUsePlaygroundOrReadUsage(authentication)")
    public ApiKeySecretResponse rotate(@PathVariable final UUID apiKeyId, final Authentication authentication) {
        return apiKeyService.rotate(principal(authentication), apiKeyId);
    }

    @PostMapping("/{apiKeyId}/revoke")
    @PreAuthorize("@organizationRbacService.canManageAnyApiKey(authentication)"
            + " or @organizationRbacService.canUsePlaygroundOrReadUsage(authentication)")
    public ApiKeyDto revoke(@PathVariable final UUID apiKeyId, final Authentication authentication) {
        return apiKeyService.revoke(principal(authentication), apiKeyId);
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            throw new AccessDeniedException("Dashboard authentication is required.");
        }
        return principal;
    }
}
