package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.exception.B2bApiException;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.StripeCheckoutSession;
import org.open4goods.b2bapi.model.StripeCheckoutStatus;
import org.open4goods.b2bapi.model.StripeCustomer;
import org.open4goods.b2bapi.model.StripeEvent;
import org.open4goods.b2bapi.model.StripeSubscription;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.InvoiceRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.StripeCheckoutSessionRepository;
import org.open4goods.b2bapi.repository.StripeCustomerRepository;
import org.open4goods.b2bapi.repository.StripeEventRepository;
import org.open4goods.b2bapi.repository.StripeSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration and unit tests for StripeWebhookService.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
class StripeWebhookServiceTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void postgresProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private StripeEventRepository stripeEventRepository;

    @Autowired
    private StripeCustomerRepository stripeCustomerRepository;

    @Autowired
    private StripeCheckoutSessionRepository stripeCheckoutSessionRepository;

    @Autowired
    private StripeSubscriptionRepository stripeSubscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    private Organization organization;
    private MockedStatic<Webhook> webhookMockedStatic;

    @BeforeEach
    void setUp() {
        webhookMockedStatic = mockStatic(Webhook.class);

        creditTransactionRepository.deleteAll();
        creditBucketRepository.deleteAll();
        invoiceRepository.deleteAll();
        stripeCheckoutSessionRepository.deleteAll();
        stripeSubscriptionRepository.deleteAll();
        stripeCustomerRepository.deleteAll();
        stripeEventRepository.deleteAll();
        organizationRepository.deleteAll();

        organization = new Organization("Stripe Org", "stripe-org");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization = organizationRepository.save(organization);
    }

    @AfterEach
    void tearDown() {
        if (webhookMockedStatic != null) {
            webhookMockedStatic.close();
        }
    }

    @Test
    void processWebhookSavesEventIdempotencyAndThrowsOnSignatureFailure() {
        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenThrow(new com.stripe.exception.SignatureVerificationException("sig failed", "sig"));

        assertThatThrownBy(() -> stripeWebhookService.processWebhook("payload", "sig"))
                .isInstanceOf(B2bApiException.class)
                .hasMessageContaining("Invalid webhook signature");

        assertThat(stripeEventRepository.findAll()).isEmpty();
    }

    @Test
    void processWebhookThrowsOnDeserializationFailure() {
        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getId()).thenReturn("evt_test1");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.empty());

        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);

        assertThatThrownBy(() -> stripeWebhookService.processWebhook("payload", "sig"))
                .isInstanceOf(B2bApiException.class)
                .hasMessageContaining("Failed to deserialize event object");

        assertThat(stripeEventRepository.findAll()).isEmpty();
    }

    @Test
    void processWebhookReturnsEarlyIfEventAlreadyProcessed() {
        final StripeEvent existing = new StripeEvent("evt_processed", "checkout.session.completed");
        stripeEventRepository.save(existing);

        final Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_processed");

        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);

        // Should return early and not throw/reprocess
        stripeWebhookService.processWebhook("payload", "sig");

        assertThat(stripeEventRepository.findByStripeEventId("evt_processed")).isPresent();
    }

    @Test
    void checkoutSessionCompletedPaymentProcessesPackGrant() {
        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        final Session session = mock(Session.class);

        when(event.getId()).thenReturn("evt_checkout_pack");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        when(session.getId()).thenReturn("cs_pack_123");
        when(session.getMode()).thenReturn("payment");
        when(session.getClientReferenceId()).thenReturn(organization.getId().toString());

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("catalog_id", "starter");
        when(session.getMetadata()).thenReturn(metadata);

        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);

        stripeWebhookService.processWebhook("payload", "sig");

        // Verify checkout session mirrored
        final Optional<StripeCheckoutSession> csOpt = stripeCheckoutSessionRepository.findByStripeSessionId("cs_pack_123");
        assertThat(csOpt).isPresent();
        assertThat(csOpt.get().getStatus()).isEqualTo(StripeCheckoutStatus.COMPLETED);

        // Verify credit bucket created
        final List<CreditBucket> buckets = creditBucketRepository.findAll();
        assertThat(buckets).filteredOn(b -> b.getOrganization().getId().equals(organization.getId()))
                .hasSize(1)
                .first()
                .satisfies(b -> {
                    assertThat(b.getKind()).isEqualTo(CreditBucketKind.PACK);
                    assertThat(b.getCreditsRemaining()).isEqualTo(10_000L);
                    assertThat(b.getSourceRef()).isEqualTo("cs_pack_123");
                });
    }

    @Test
    void checkoutSessionCompletedSubscriptionRetrievesSubscriptionAndCreatesMirror() throws Exception {
        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        final Session session = mock(Session.class);
        final Subscription stripeSub = mock(Subscription.class);

        when(event.getId()).thenReturn("evt_checkout_sub");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        when(session.getId()).thenReturn("cs_sub_123");
        when(session.getMode()).thenReturn("subscription");
        when(session.getClientReferenceId()).thenReturn(organization.getId().toString());
        when(session.getSubscription()).thenReturn("sub_stripe_123");

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("catalog_id", "starter");
        when(session.getMetadata()).thenReturn(metadata);

        // Mock static Subscription.retrieve(subId)
        try (MockedStatic<Subscription> subMock = mockStatic(Subscription.class)) {
            subMock.when(() -> Subscription.retrieve("sub_stripe_123"))
                    .thenReturn(stripeSub);
            when(stripeSub.getStatus()).thenReturn("active");
            
            final SubscriptionItemCollection items = mock(SubscriptionItemCollection.class);
            final SubscriptionItem item = mock(SubscriptionItem.class);
            when(stripeSub.getItems()).thenReturn(items);
            when(items.getData()).thenReturn(List.of(item));
            when(item.getCurrentPeriodEnd()).thenReturn(1770000000L); // Future date

            webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            stripeWebhookService.processWebhook("payload", "sig");
        }

        // Verify subscription mirrored
        final Optional<StripeSubscription> subOpt = stripeSubscriptionRepository.findByStripeSubscriptionId("sub_stripe_123");
        assertThat(subOpt).isPresent();
        assertThat(subOpt.get().getStatus()).isEqualTo("active");
        assertThat(subOpt.get().getCatalogId()).isEqualTo("starter");
        assertThat(subOpt.get().getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1770000000L));
    }

    @Test
    void invoicePaidSubscriptionGrantsCredits() throws Exception {
        // Prepare Stripe Customer link
        final StripeCustomer customer = new StripeCustomer(organization, "cus_stripe_123");
        stripeCustomerRepository.save(customer);

        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        final Invoice invoice = mock(Invoice.class);
        final Subscription stripeSub = mock(Subscription.class);

        when(event.getId()).thenReturn("evt_invoice_paid");
        when(event.getType()).thenReturn("invoice.paid");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));

        when(invoice.getId()).thenReturn("in_123");
        when(invoice.getCustomer()).thenReturn("cus_stripe_123");
        when(invoice.getAmountPaid()).thenReturn(2900L);
        when(invoice.getStatus()).thenReturn("paid");
        when(invoice.getHostedInvoiceUrl()).thenReturn("https://invoice.url");

        final Invoice.Parent parent = mock(Invoice.Parent.class);
        final Invoice.Parent.SubscriptionDetails details = mock(Invoice.Parent.SubscriptionDetails.class);
        when(invoice.getParent()).thenReturn(parent);
        when(parent.getSubscriptionDetails()).thenReturn(details);
        when(details.getSubscription()).thenReturn("sub_stripe_123");

        // Seed existing sub mirror
        final StripeSubscription existingSub = new StripeSubscription(organization, "sub_stripe_123", "starter", "active");
        stripeSubscriptionRepository.save(existingSub);

        try (MockedStatic<Subscription> subMock = mockStatic(Subscription.class)) {
            subMock.when(() -> Subscription.retrieve("sub_stripe_123"))
                    .thenReturn(stripeSub);
            when(stripeSub.getStatus()).thenReturn("active");

            final SubscriptionItemCollection items = mock(SubscriptionItemCollection.class);
            final SubscriptionItem item = mock(SubscriptionItem.class);
            when(stripeSub.getItems()).thenReturn(items);
            when(items.getData()).thenReturn(List.of(item));
            when(item.getCurrentPeriodEnd()).thenReturn(1770000000L);

            webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            stripeWebhookService.processWebhook("payload", "sig");
        }

        // Verify credit bucket from subscription created
        final List<CreditBucket> buckets = creditBucketRepository.findAll();
        assertThat(buckets).filteredOn(b -> b.getOrganization().getId().equals(organization.getId()))
                .hasSize(1)
                .first()
                .satisfies(b -> {
                    assertThat(b.getKind()).isEqualTo(CreditBucketKind.SUBSCRIPTION);
                    assertThat(b.getCreditsRemaining()).isEqualTo(12_000L);
                    assertThat(b.getSourceRef()).isEqualTo("sub_stripe_123");
                });

        // Verify invoice recorded
        final Optional<org.open4goods.b2bapi.model.Invoice> invOpt = invoiceRepository.findByStripeInvoiceId("in_123");
        assertThat(invOpt).isPresent();
        assertThat(invOpt.get().getStatus()).isEqualTo("paid");
        assertThat(invOpt.get().getAmountCents()).isEqualTo(2900);
        assertThat(invOpt.get().getCreditsGranted()).isEqualTo(12_000L);
    }

    @Test
    void invoicePaymentFailedUpdatesInvoiceMirror() {
        final StripeCustomer customer = new StripeCustomer(organization, "cus_stripe_123");
        stripeCustomerRepository.save(customer);

        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        final Invoice invoice = mock(Invoice.class);

        when(event.getId()).thenReturn("evt_invoice_failed");
        when(event.getType()).thenReturn("invoice.payment_failed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));

        when(invoice.getId()).thenReturn("in_fail_123");
        when(invoice.getCustomer()).thenReturn("cus_stripe_123");
        when(invoice.getAmountPaid()).thenReturn(0L);
        when(invoice.getStatus()).thenReturn("open");
        when(invoice.getHostedInvoiceUrl()).thenReturn("https://failed.invoice.url");

        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);

        stripeWebhookService.processWebhook("payload", "sig");

        final Optional<org.open4goods.b2bapi.model.Invoice> invOpt = invoiceRepository.findByStripeInvoiceId("in_fail_123");
        assertThat(invOpt).isPresent();
        assertThat(invOpt.get().getStatus()).isEqualTo("open");
    }

    @Test
    void subscriptionUpdatedWithCancelAtAppliesCancellationExpiry() {
        final StripeSubscription subMirror = new StripeSubscription(organization, "sub_cancel_123", "starter", "active");
        stripeSubscriptionRepository.save(subMirror);

        // Seed credit bucket for subscription
        final CreditBucket bucket = new CreditBucket(organization, CreditBucketKind.SUBSCRIPTION, 12000L, 12000L);
        bucket.setCatalogId("starter");
        bucket.setSourceRef("sub_cancel_123");
        bucket.setExpiresAt(Instant.now().plusSeconds(86400 * 30));
        creditBucketRepository.save(bucket);

        final Event event = mock(Event.class);
        final EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        final Subscription subscription = mock(Subscription.class);

        when(event.getId()).thenReturn("evt_sub_updated");
        when(event.getType()).thenReturn("customer.subscription.updated");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));

        when(subscription.getId()).thenReturn("sub_cancel_123");
        when(subscription.getStatus()).thenReturn("active");
        when(subscription.getCancelAt()).thenReturn(1770000000L); // Cancel timestamp

        final SubscriptionItemCollection items = mock(SubscriptionItemCollection.class);
        final SubscriptionItem item = mock(SubscriptionItem.class);
        when(subscription.getItems()).thenReturn(items);
        when(items.getData()).thenReturn(List.of(item));
        when(item.getCurrentPeriodEnd()).thenReturn(1770000000L);

        webhookMockedStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);

        stripeWebhookService.processWebhook("payload", "sig");

        // Verify bucket expiry updated (to cancel time + 30 days grace)
        final CreditBucket updatedBucket = creditBucketRepository.findAll().stream()
                .filter(b -> b.getSourceRef().equals("sub_cancel_123"))
                .findFirst().orElseThrow();
        assertThat(updatedBucket.getExpiresAt()).isEqualTo(Instant.ofEpochSecond(1770000000L).plusSeconds(30 * 86400));
    }
}
