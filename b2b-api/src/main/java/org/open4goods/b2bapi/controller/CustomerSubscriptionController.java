package org.open4goods.b2bapi.controller;

import java.util.List;
import org.open4goods.b2bapi.dto.billing.B2bSubscriptionDto;
import org.open4goods.b2bapi.service.CustomerBillingService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for B2B customer subscription endpoints.
 */
@RestController
@RequestMapping("/api/v1/customer/subscriptions")
@ConditionalOnBean(name = "entityManagerFactory")
public class CustomerSubscriptionController {

    private final CustomerBillingService customerBillingService;

    public CustomerSubscriptionController(final CustomerBillingService customerBillingService) {
        this.customerBillingService = customerBillingService;
    }

    /**
     * Get mirrored Stripe subscriptions for the active organization.
     */
    @GetMapping
    @PreAuthorize("@organizationRbacService.canReadBilling(authentication)")
    public List<B2bSubscriptionDto> getSubscriptions(final Authentication authentication) {
        return customerBillingService.getSubscriptions(principal(authentication).organizationId());
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            throw new AccessDeniedException("Dashboard authentication is required.");
        }
        return principal;
    }
}
