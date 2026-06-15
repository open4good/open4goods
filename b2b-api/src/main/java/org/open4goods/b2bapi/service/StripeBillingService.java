package org.open4goods.b2bapi.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Optional;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties.Pack;
import org.open4goods.b2bapi.config.BillingCatalogProperties.Subscription;
import org.open4goods.b2bapi.exception.B2bApiException;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.StripeCheckoutMode;
import org.open4goods.b2bapi.model.StripeCheckoutSession;
import org.open4goods.b2bapi.model.StripeCustomer;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.StripeCheckoutSessionRepository;
import org.open4goods.b2bapi.repository.StripeCustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for interfacing with Stripe for checkout and billing portal integrations.
 */
@Service
public class StripeBillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    private final B2bApiProperties b2bApiProperties;
    private final BillingCatalogProperties catalogProperties;
    private final OrganizationRepository organizationRepository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final StripeCheckoutSessionRepository stripeCheckoutSessionRepository;

    @Autowired
    public StripeBillingService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties catalogProperties,
            final OrganizationRepository organizationRepository,
            final StripeCustomerRepository stripeCustomerRepository,
            final StripeCheckoutSessionRepository stripeCheckoutSessionRepository) {
        this.b2bApiProperties = b2bApiProperties;
        this.catalogProperties = catalogProperties;
        this.organizationRepository = organizationRepository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.stripeCheckoutSessionRepository = stripeCheckoutSessionRepository;
    }

    /**
     * Initiates a Stripe Checkout Session for a prepaid credits pack.
     *
     * @param principal authenticated dashboard principal
     * @param catalogId internal identifier of the pack
     * @return CheckoutResponse containing the hosted checkout URL
     */
    @Transactional
    public String createPackCheckoutSession(final DashboardPrincipal principal, final String catalogId) {
        final Organization org = organizationRepository.findById(principal.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        final Pack pack = catalogProperties.getBilling().getPacks().get(catalogId);
        if (pack == null) {
            throw new B2bApiException(ErrorCode.INVALID_PARAMETER, "Pack not found in catalog: " + catalogId);
        }

        final String stripePriceId = pack.getStripePriceId();
        if (stripePriceId == null || stripePriceId.isBlank()) {
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe Price ID is not configured for pack: " + catalogId);
        }

        try {
            final String stripeCustomerId = getOrCreateStripeCustomerId(org, principal.email());
            final String checkoutUrl = createStripeCheckoutSession(org, stripeCustomerId, stripePriceId, catalogId, StripeCheckoutMode.payment);
            return checkoutUrl;
        } catch (final StripeException e) {
            log.error("Stripe Checkout Session creation failed for pack: {}", catalogId, e);
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe checkout initiation failed", e);
        }
    }

    /**
     * Initiates a Stripe Checkout Session for a subscription plan.
     *
     * @param principal authenticated dashboard principal
     * @param catalogId internal identifier of the subscription
     * @return CheckoutResponse containing the hosted checkout URL
     */
    @Transactional
    public String createSubscriptionCheckoutSession(final DashboardPrincipal principal, final String catalogId) {
        final Organization org = organizationRepository.findById(principal.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        final Subscription sub = catalogProperties.getBilling().getSubscriptions().get(catalogId);
        if (sub == null) {
            throw new B2bApiException(ErrorCode.INVALID_PARAMETER, "Subscription plan not found in catalog: " + catalogId);
        }

        final String stripePriceId = sub.getStripePriceId();
        if (stripePriceId == null || stripePriceId.isBlank()) {
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe Price ID is not configured for subscription: " + catalogId);
        }

        try {
            final String stripeCustomerId = getOrCreateStripeCustomerId(org, principal.email());
            final String checkoutUrl = createStripeCheckoutSession(org, stripeCustomerId, stripePriceId, catalogId, StripeCheckoutMode.subscription);
            return checkoutUrl;
        } catch (final StripeException e) {
            log.error("Stripe Checkout Session creation failed for subscription: {}", catalogId, e);
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe checkout initiation failed", e);
        }
    }

    /**
     * Initiates a Stripe Billing Portal session for the customer.
     *
     * @param principal authenticated dashboard principal
     * @return redirection URL to the Stripe Customer Portal
     */
    @Transactional(readOnly = true)
    public String createBillingPortalSession(final DashboardPrincipal principal) {
        final Organization org = organizationRepository.findById(principal.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        final StripeCustomer stripeCustomer = stripeCustomerRepository.findByOrganizationId(org.getId())
                .orElseThrow(() -> new B2bApiException(ErrorCode.INVALID_PARAMETER, "No Stripe customer found for this organization. Initiate a purchase first."));

        Stripe.apiKey = b2bApiProperties.getStripe().getSecretKey();

        final String returnUrl = b2bApiProperties.getPublicBaseUrl().toString() + "/billing";

        try {
            final com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(stripeCustomer.getStripeCustomerId())
                    .setReturnUrl(returnUrl)
                    .build();

            final com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
            return portalSession.getUrl();
        } catch (final StripeException e) {
            log.error("Stripe Billing Portal Session creation failed for customer: {}", stripeCustomer.getStripeCustomerId(), e);
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe portal initiation failed", e);
        }
    }

    private String getOrCreateStripeCustomerId(final Organization org, final String userEmail) throws StripeException {
        final Optional<StripeCustomer> existing = stripeCustomerRepository.findByOrganizationId(org.getId());
        if (existing.isPresent()) {
            return existing.get().getStripeCustomerId();
        }

        Stripe.apiKey = b2bApiProperties.getStripe().getSecretKey();

        final String email = (org.getBillingEmail() != null && !org.getBillingEmail().isBlank())
                ? org.getBillingEmail()
                : userEmail;

        final CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(org.getName())
                .putMetadata("organization_id", org.getId().toString())
                .build();

        final Customer customer = Customer.create(params);

        final StripeCustomer stripeCustomer = new StripeCustomer(org, customer.getId());
        stripeCustomerRepository.save(stripeCustomer);

        return customer.getId();
    }

    private String createStripeCheckoutSession(
            final Organization org,
            final String stripeCustomerId,
            final String stripePriceId,
            final String catalogId,
            final StripeCheckoutMode mode) throws StripeException {

        Stripe.apiKey = b2bApiProperties.getStripe().getSecretKey();

        final String successUrl = b2bApiProperties.getPublicBaseUrl().toString() + "/billing?success=true";
        final String cancelUrl = b2bApiProperties.getPublicBaseUrl().toString() + "/billing?cancel=true";

        final SessionCreateParams params = SessionCreateParams.builder()
                .setMode(mode == StripeCheckoutMode.payment ? SessionCreateParams.Mode.PAYMENT : SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomer(stripeCustomerId)
                .setClientReferenceId(org.getId().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(stripePriceId)
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("organization_id", org.getId().toString())
                .putMetadata("catalog_id", catalogId)
                .build();

        final Session session = Session.create(params);

        final StripeCheckoutSession checkoutSession = new StripeCheckoutSession(
                org,
                session.getId(),
                mode,
                catalogId
        );
        stripeCheckoutSessionRepository.save(checkoutSession);

        return session.getUrl();
    }
}
