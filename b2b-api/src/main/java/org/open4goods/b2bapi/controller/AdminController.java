package org.open4goods.b2bapi.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.admin.AdminAuditEventDto;
import org.open4goods.b2bapi.dto.admin.AdminCreditGrantResponseDto;
import org.open4goods.b2bapi.dto.admin.AdminManualGrantRequest;
import org.open4goods.b2bapi.dto.admin.AdminOrganizationDto;
import org.open4goods.b2bapi.dto.admin.AdminUsageEventDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.open4goods.b2bapi.service.AdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for B2B platform administrative endpoints.
 * Gated exclusively for platform administrators.
 */
@Tag(name = "Platform Administration", description = "Endpoints for B2B platform administrators")
@RestController
@RequestMapping("/api/v1/admin")
@ConditionalOnBean(name = "entityManagerFactory")
@PreAuthorize("@organizationRbacService.isPlatformAdmin(authentication)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(final AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/organizations")
    public List<AdminOrganizationDto> listOrganizations() {
        return adminService.listOrganizations();
    }

    @GetMapping("/organizations/{organizationId}")
    public AdminOrganizationDto getOrganization(@PathVariable final UUID organizationId) {
        return adminService.getOrganization(organizationId);
    }

    @GetMapping("/organizations/{organizationId}/transactions")
    public List<B2bTransactionDto> getOrganizationTransactions(
            @PathVariable final UUID organizationId,
            @RequestParam(defaultValue = "50") final int limit) {
        return adminService.getOrganizationTransactions(organizationId, limit);
    }

    @PostMapping("/organizations/{organizationId}/credits/grants")
    public AdminCreditGrantResponseDto grantManualCredits(
            @PathVariable final UUID organizationId,
            @Valid @RequestBody final AdminManualGrantRequest request,
            final Authentication authentication) {
        return adminService.grantManualCredits(organizationId, principal(authentication), request);
    }

    @GetMapping("/api-keys")
    public List<ApiKeyDto> listApiKeys() {
        return adminService.listApiKeys();
    }

    @PostMapping("/api-keys/{apiKeyId}/revoke")
    public ApiKeyDto revokeApiKey(
            @PathVariable final UUID apiKeyId,
            final Authentication authentication) {
        return adminService.revokeApiKey(principal(authentication), apiKeyId);
    }

    @GetMapping("/usage")
    public List<AdminUsageEventDto> listUsage(@RequestParam(defaultValue = "50") final int limit) {
        return adminService.listUsage(limit);
    }

    @GetMapping("/audit")
    public List<AdminAuditEventDto> listAuditEvents(@RequestParam(defaultValue = "50") final int limit) {
        return adminService.listAuditEvents(limit);
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            throw new AccessDeniedException("Dashboard authentication is required.");
        }
        return principal;
    }
}
