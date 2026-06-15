package org.open4goods.b2bapi.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.exception.B2bApiException;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.StripeCheckoutMode;
import org.open4goods.b2bapi.model.StripeCheckoutSession;
import org.open4goods.b2bapi.model.StripeCheckoutStatus;
import org.open4goods.b2bapi.model.StripeCustomer;
import org.open4goods.b2bapi.model.StripeEvent;
import org.open4goods.b2bapi.model.StripeSubscription;
import org.open4goods.b2bapi.repository.InvoiceRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.StripeCheckoutSessionRepository;
import org.open4goods.b2bapi.repository.StripeCustomerRepository;
import org.open4goods.b2bapi.repository.StripeEventRepository;
import org.open4goods.b2bapi.repository.StripeSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to process incoming Stripe Webhooks idempotently and dispatch to corresponding handlers.
 */
@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private static final Duration SUBSCRIPTION_GRACE = Duration.ofDays(7);

    private final B2bApiProperties b2bApiProperties;
    private final BillingCatalogProperties catalogProperties;
    private final StripeEventRepository stripeEventRepository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final StripeCheckoutSessionRepository stripeCheckoutSessionRepository;
    private final StripeSubscriptionRepository stripeSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final CreditGrantService creditGrantService;

    @Autowired
    public StripeWebhookService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties catalogProperties,
            final StripeEventRepository stripeEventRepository,
            final StripeCustomerRepository stripeCustomerRepository,
            final StripeCheckoutSessionRepository stripeCheckoutSessionRepository,
            final StripeSubscriptionRepository stripeSubscriptionRepository,
            final InvoiceRepository invoiceRepository,
            final OrganizationRepository organizationRepository,
            final CreditGrantService creditGrantService) {
        this.b2bApiProperties = b2bApiProperties;
        this.catalogProperties = catalogProperties;
        this.stripeEventRepository = stripeEventRepository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.stripeCheckoutSessionRepository = stripeCheckoutSessionRepository;
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
        this.creditGrantService = creditGrantService;
    }

    /**
     * Process an incoming Stripe Webhook.
     *
     * @param payload the raw request payload
     * @param sigHeader Stripe-Signature header
     */
    @Transactional
    public void processWebhook(final String payload, final String sigHeader) {
        final String webhookSecret = b2bApiProperties.getStripe().getWebhookSecret();
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (final SignatureVerificationException e) {
            log.warn("Stripe signature verification failed.", e);
            throw new B2bApiException(ErrorCode.INVALID_PARAMETER, "Invalid webhook signature", e);
        }

        // Idempotency check: process only if not already processed
        final String stripeEventId = event.getId();
        if (stripeEventRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.info("Stripe event already processed: {}", stripeEventId);
            return;
        }

        // Parse object
        final EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            log.warn("Stripe event data deserialization failed for event: {}", stripeEventId);
            throw new B2bApiException(ErrorCode.INVALID_PARAMETER, "Failed to deserialize event object");
        }
        final StripeObject stripeObject = deserializer.getObject().get();

        log.info("Processing Stripe webhook event: {} (type: {})", stripeEventId, event.getType());

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted((Session) stripeObject);
                    break;
                case "invoice.paid":
                    handleInvoicePaid((Invoice) stripeObject);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed((Invoice) stripeObject);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated((Subscription) stripeObject);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted((Subscription) stripeObject);
                    break;
                default:
                    log.info("Unhandled Stripe event type: {}", event.getType());
                    break;
            }
        } catch (final StripeException e) {
            log.error("Stripe SDK call failed during webhook processing of event: {}", stripeEventId, e);
            throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Stripe API interaction failed", e);
        }

        // Record the event to guarantee idempotency
        final StripeEvent stripeEvent = new StripeEvent(stripeEventId, event.getType());
        // Simple map storing event type/id for metadata
        stripeEvent.getPayload().put("id", stripeEventId);
        stripeEvent.getPayload().put("type", event.getType());
        stripeEventRepository.save(stripeEvent);
    }

    private void handleCheckoutSessionCompleted(final Session session) throws StripeException {
        final String stripeSessionId = session.getId();
        final Optional<StripeCheckoutSession> existingMirror = stripeCheckoutSessionRepository.findByStripeSessionId(stripeSessionId);

        final String orgIdStr = session.getClientReferenceId() != null
                ? session.getClientReferenceId()
                : session.getMetadata().get("organization_id");

        if (orgIdStr == null || orgIdStr.isBlank()) {
            log.warn("checkout.session.completed received without clientReferenceId or organization_id metadata: {}", stripeSessionId);
            return;
        }

        final UUID orgId = UUID.fromString(orgIdStr);
        final Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        final StripeCheckoutMode mode = StripeCheckoutMode.valueOf(session.getMode());
        final String catalogId = session.getMetadata().get("catalog_id");

        final StripeCheckoutSession mirror;
        if (existingMirror.isPresent()) {
            mirror = existingMirror.get();
            mirror.setStatus(StripeCheckoutStatus.COMPLETED);
            stripeCheckoutSessionRepository.save(mirror);
        } else {
            mirror = new StripeCheckoutSession(org, stripeSessionId, mode, catalogId);
            mirror.setStatus(StripeCheckoutStatus.COMPLETED);
            stripeCheckoutSessionRepository.save(mirror);
        }

        if (mode == StripeCheckoutMode.payment) {
            creditGrantService.grantPack(org.getId(), catalogId, stripeSessionId);
        } else if (mode == StripeCheckoutMode.subscription) {
            final String subId = session.getSubscription();
            if (subId != null && !subId.isBlank()) {
                Stripe.apiKey = b2bApiProperties.getStripe().getSecretKey();
                final Subscription stripeSub = Subscription.retrieve(subId);

                final Optional<StripeSubscription> existingSub = stripeSubscriptionRepository.findByStripeSubscriptionId(subId);
                final StripeSubscription subMirror;
                if (existingSub.isPresent()) {
                    subMirror = existingSub.get();
                    subMirror.setStatus(stripeSub.getStatus());
                    subMirror.setCurrentPeriodEnd(getSubscriptionCurrentPeriodEnd(stripeSub));
                    subMirror.setUpdatedAt(Instant.now());
                } else {
                    subMirror = new StripeSubscription(org, subId, catalogId, stripeSub.getStatus());
                    subMirror.setCurrentPeriodEnd(getSubscriptionCurrentPeriodEnd(stripeSub));
                }
                stripeSubscriptionRepository.save(subMirror);
            }
        }
    }

    private void handleInvoicePaid(final Invoice invoice) throws StripeException {
        final String stripeCustomerId = invoice.getCustomer();
        final StripeCustomer stripeCustomer = stripeCustomerRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new ResourceNotFoundException("Stripe customer not found in registry: " + stripeCustomerId));

        final Organization org = stripeCustomer.getOrganization();
        final Invoice.Parent parent = invoice.getParent();
        final String subId = (parent != null && parent.getSubscriptionDetails() != null)
                ? parent.getSubscriptionDetails().getSubscription()
                : null;

        if (subId != null && !subId.isBlank()) {
            // Subscription payment
            Stripe.apiKey = b2bApiProperties.getStripe().getSecretKey();
            final Subscription stripeSub = Subscription.retrieve(subId);

            final Optional<StripeSubscription> existingSub = stripeSubscriptionRepository.findByStripeSubscriptionId(subId);
            final StripeSubscription subMirror;
            final String catalogId;

            if (existingSub.isPresent()) {
                subMirror = existingSub.get();
                catalogId = subMirror.getCatalogId();
                subMirror.setStatus(stripeSub.getStatus());
                subMirror.setCurrentPeriodEnd(getSubscriptionCurrentPeriodEnd(stripeSub));
                subMirror.setUpdatedAt(Instant.now());
                stripeSubscriptionRepository.save(subMirror);
            } else {
                // Out-of-order execution lookup
                final String priceId = getInvoiceFirstLinePriceId(invoice);
                catalogId = findSubscriptionCatalogIdByPriceId(priceId);
                if (catalogId == null) {
                    throw new B2bApiException(ErrorCode.INTERNAL_ERROR, "Subscription price ID " + priceId + " not found in catalog");
                }
                subMirror = new StripeSubscription(org, subId, catalogId, "pending_activation");
                subMirror.setCurrentPeriodEnd(getSubscriptionCurrentPeriodEnd(stripeSub));
                stripeSubscriptionRepository.save(subMirror);
            }

            final BillingCatalogProperties.Subscription catalogSub = catalogProperties.getBilling().getSubscriptions().get(catalogId);
            final Instant expiresAt = getSubscriptionCurrentPeriodEnd(stripeSub).plus(SUBSCRIPTION_GRACE);

            creditGrantService.grantSubscription(org.getId(), catalogId, subId, expiresAt);

            // Save/update invoice record
            final Optional<org.open4goods.b2bapi.model.Invoice> existingInv = invoiceRepository.findByStripeInvoiceId(invoice.getId());
            final org.open4goods.b2bapi.model.Invoice invoiceMirror;
            if (existingInv.isPresent()) {
                invoiceMirror = existingInv.get();
                invoiceMirror.setStatus(invoice.getStatus());
                invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
                invoiceMirror.setCreditsGranted((long) catalogSub.getMonthlyCredits());
            } else {
                invoiceMirror = new org.open4goods.b2bapi.model.Invoice(org, invoice.getId(), invoice.getAmountPaid().intValue(), invoice.getStatus());
                invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
                invoiceMirror.setCreditsGranted((long) catalogSub.getMonthlyCredits());
            }
            invoiceRepository.save(invoiceMirror);
        } else {
            // One-off charge (e.g. prepaid pack)
            final Optional<org.open4goods.b2bapi.model.Invoice> existingInv = invoiceRepository.findByStripeInvoiceId(invoice.getId());
            final org.open4goods.b2bapi.model.Invoice invoiceMirror;
            if (existingInv.isPresent()) {
                invoiceMirror = existingInv.get();
                invoiceMirror.setStatus(invoice.getStatus());
                invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
            } else {
                invoiceMirror = new org.open4goods.b2bapi.model.Invoice(org, invoice.getId(), invoice.getAmountPaid().intValue(), invoice.getStatus());
                invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
            }
            invoiceRepository.save(invoiceMirror);
        }
    }

    private void handleInvoicePaymentFailed(final Invoice invoice) {
        final String stripeCustomerId = invoice.getCustomer();
        final Optional<StripeCustomer> stripeCustomer = stripeCustomerRepository.findByStripeCustomerId(stripeCustomerId);
        if (stripeCustomer.isEmpty()) {
            log.warn("Invoice payment failed event for unregistered customer: {}", stripeCustomerId);
            return;
        }

        final Organization org = stripeCustomer.get().getOrganization();
        final Optional<org.open4goods.b2bapi.model.Invoice> existingInv = invoiceRepository.findByStripeInvoiceId(invoice.getId());

        final org.open4goods.b2bapi.model.Invoice invoiceMirror;
        if (existingInv.isPresent()) {
            invoiceMirror = existingInv.get();
            invoiceMirror.setStatus(invoice.getStatus());
            invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
        } else {
            invoiceMirror = new org.open4goods.b2bapi.model.Invoice(org, invoice.getId(), invoice.getAmountPaid().intValue(), invoice.getStatus());
            invoiceMirror.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
        }
        invoiceRepository.save(invoiceMirror);
    }

    private void handleSubscriptionUpdated(final Subscription subscription) {
        final Optional<StripeSubscription> existingSub = stripeSubscriptionRepository.findByStripeSubscriptionId(subscription.getId());
        if (existingSub.isEmpty()) {
            log.warn("Subscription updated received for unknown subscription: {}", subscription.getId());
            return;
        }

        final StripeSubscription subMirror = existingSub.get();
        final String oldStatus = subMirror.getStatus();
        subMirror.setStatus(subscription.getStatus());
        subMirror.setCurrentPeriodEnd(getSubscriptionCurrentPeriodEnd(subscription));

        final Instant newCancelAt = subscription.getCancelAt() != null ? Instant.ofEpochSecond(subscription.getCancelAt()) : null;
        final Instant oldCancelAt = subMirror.getCancelAt();
        subMirror.setCancelAt(newCancelAt);
        subMirror.setUpdatedAt(Instant.now());
        stripeSubscriptionRepository.save(subMirror);

        // If cancelAt has been set (transition from non-canceled state to canceling)
        if (newCancelAt != null && oldCancelAt == null) {
            log.info("Subscription canceled, setting bucket expiration: {}", subscription.getId());
            creditGrantService.applySubscriptionCancellationExpiry(
                    subMirror.getOrganization().getId(),
                    subscription.getId(),
                    newCancelAt
            );
        }
    }

    private void handleSubscriptionDeleted(final Subscription subscription) {
        final Optional<StripeSubscription> existingSub = stripeSubscriptionRepository.findByStripeSubscriptionId(subscription.getId());
        if (existingSub.isEmpty()) {
            log.warn("Subscription deleted received for unknown subscription: {}", subscription.getId());
            return;
        }

        final StripeSubscription subMirror = existingSub.get();
        subMirror.setStatus("canceled");
        subMirror.setUpdatedAt(Instant.now());
        stripeSubscriptionRepository.save(subMirror);

        final Instant cancelTime = subscription.getEndedAt() != null
                ? Instant.ofEpochSecond(subscription.getEndedAt())
                : Instant.now();

        creditGrantService.applySubscriptionCancellationExpiry(
                subMirror.getOrganization().getId(),
                subscription.getId(),
                cancelTime
        );
    }

    private Instant getSubscriptionCurrentPeriodEnd(final Subscription subscription) {
        if (subscription != null && subscription.getItems() != null && subscription.getItems().getData() != null && !subscription.getItems().getData().isEmpty()) {
            final Long end = subscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (end != null) {
                return Instant.ofEpochSecond(end);
            }
        }
        return Instant.now();
    }

    private String getInvoiceFirstLinePriceId(final Invoice invoice) {
        if (invoice != null && invoice.getLines() != null && invoice.getLines().getData() != null && !invoice.getLines().getData().isEmpty()) {
            final InvoiceLineItem lineItem = invoice.getLines().getData().get(0);
            if (lineItem.getPricing() != null && lineItem.getPricing().getPriceDetails() != null) {
                return lineItem.getPricing().getPriceDetails().getPrice();
            }
        }
        return null;
    }

    private String findSubscriptionCatalogIdByPriceId(final String priceId) {
        for (final Map.Entry<String, BillingCatalogProperties.Subscription> entry : catalogProperties.getBilling().getSubscriptions().entrySet()) {
            if (priceId.equals(entry.getValue().getStripePriceId())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
