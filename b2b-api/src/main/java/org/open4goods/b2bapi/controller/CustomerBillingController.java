package org.open4goods.b2bapi.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.open4goods.b2bapi.dto.billing.B2bBalanceResponseDto;
import org.open4goods.b2bapi.dto.billing.B2bInvoiceDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import org.open4goods.b2bapi.dto.billing.BillingCatalogDto;
import org.open4goods.b2bapi.dto.billing.CheckoutRequest;
import org.open4goods.b2bapi.dto.billing.CheckoutResponse;
import org.open4goods.b2bapi.service.BillingCatalogService;
import org.open4goods.b2bapi.service.CustomerBillingService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.open4goods.b2bapi.service.StripeBillingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for B2B customer billing endpoints.
 */
@RestController
@RequestMapping("/api/v1/customer/billing")
@ConditionalOnBean(name = "entityManagerFactory")
public class CustomerBillingController {

    private final BillingCatalogService billingCatalogService;
    private final StripeBillingService stripeBillingService;
    private final CustomerBillingService customerBillingService;

    public CustomerBillingController(
            final BillingCatalogService billingCatalogService,
            final StripeBillingService stripeBillingService,
            final CustomerBillingService customerBillingService) {
        this.billingCatalogService = billingCatalogService;
        this.stripeBillingService = stripeBillingService;
        this.customerBillingService = customerBillingService;
    }

    /**
     * Get the public-safe billing catalog of packs and subscriptions.
     *
     * @return complete billing catalog DTO
     */
    @GetMapping("/catalog")
    public BillingCatalogDto getCatalog() {
        return billingCatalogService.getCatalog();
    }

    /**
     * Initiates a Stripe Checkout Session for a prepaid credits pack.
     *
     * @param request request body containing the catalog ID
     * @param authentication active dashboard security authentication context
     * @return CheckoutResponse containing the hosted checkout URL
     */
    @PostMapping("/checkout/pack")
    @PreAuthorize("@organizationRbacService.canManageBilling(authentication)")
    public CheckoutResponse checkoutPack(
            @Valid @RequestBody final CheckoutRequest request,
            final Authentication authentication) {
        final String url = stripeBillingService.createPackCheckoutSession(principal(authentication), request.catalogId());
        return new CheckoutResponse(url);
    }

    /**
     * Initiates a Stripe Checkout Session for a subscription plan.
     *
     * @param request request body containing the catalog ID
     * @param authentication active dashboard security authentication context
     * @return CheckoutResponse containing the hosted checkout URL
     */
    @PostMapping("/checkout/subscription")
    @PreAuthorize("@organizationRbacService.canManageBilling(authentication)")
    public CheckoutResponse checkoutSubscription(
            @Valid @RequestBody final CheckoutRequest request,
            final Authentication authentication) {
        final String url = stripeBillingService.createSubscriptionCheckoutSession(principal(authentication), request.catalogId());
        return new CheckoutResponse(url);
    }

    /**
     * Initiates a Stripe Customer Portal Session for billing self-service.
     *
     * @param authentication active dashboard security authentication context
     * @return CheckoutResponse containing the Customer Portal URL
     */
    @PostMapping("/portal")
    @PreAuthorize("@organizationRbacService.canManageBilling(authentication)")
    public CheckoutResponse portal(final Authentication authentication) {
        final String url = stripeBillingService.createBillingPortalSession(principal(authentication));
        return new CheckoutResponse(url);
    }

    /**
     * Get credit balance and active bucket breakdown.
     */
    @GetMapping("/balance")
    @PreAuthorize("@organizationRbacService.canReadBilling(authentication)")
    public B2bBalanceResponseDto getBalance(final Authentication authentication) {
        return customerBillingService.getBalance(principal(authentication).organizationId());
    }

    /**
     * Get recent transactions ledger.
     */
    @GetMapping("/transactions")
    @PreAuthorize("@organizationRbacService.canReadBilling(authentication)")
    public List<B2bTransactionDto> getTransactions(
            @RequestParam(defaultValue = "50") final int limit,
            final Authentication authentication) {
        return customerBillingService.getTransactions(principal(authentication).organizationId(), limit);
    }

    /**
     * Get mirrored Stripe invoices.
     */
    @GetMapping("/invoices")
    @PreAuthorize("@organizationRbacService.canReadBilling(authentication)")
    public List<B2bInvoiceDto> getInvoices(final Authentication authentication) {
        return customerBillingService.getInvoices(principal(authentication).organizationId());
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            throw new AccessDeniedException("Dashboard authentication is required.");
        }
        return principal;
    }
}
