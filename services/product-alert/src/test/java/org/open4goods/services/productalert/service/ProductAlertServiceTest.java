package org.open4goods.services.productalert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productalert.config.yml.ProductAlertProperties;
import org.open4goods.services.productalert.controller.ProductAlertBadRequestException;
import org.open4goods.services.productalert.dto.InternalPriceEventDto;
import org.open4goods.services.productalert.dto.PriceEventsIngestionRequest;
import org.open4goods.services.productalert.dto.PriceEventsIngestionResponse;
import org.open4goods.services.productalert.dto.SubscriptionDto;
import org.open4goods.services.productalert.dto.SubscriptionUpsertRequest;
import org.open4goods.services.productalert.model.ProductAlertNotificationCandidate;
import org.open4goods.services.productalert.model.ProductAlertSubscription;
import org.open4goods.services.productalert.repository.ProductAlertNotificationCandidateRepository;
import org.open4goods.services.productalert.repository.ProductAlertSubscriptionRepository;
import org.open4goods.services.productalert.repository.ProductAlertUserRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link ProductAlertService}.
 */
@ExtendWith(MockitoExtension.class)
class ProductAlertServiceTest
{
    @Mock
    private ProductAlertUserRepository userRepository;

    @Mock
    private ProductAlertSubscriptionRepository subscriptionRepository;

    @Mock
    private ProductAlertNotificationCandidateRepository candidateRepository;

    private ProductAlertService service;
    private Clock clock;

    @BeforeEach
    void setUp()
    {
        ProductAlertProperties properties = new ProductAlertProperties();
        properties.setDedupWindow(Duration.ofHours(24));

        clock = Clock.fixed(Instant.parse("2026-03-13T10:15:30Z"), ZoneOffset.UTC);

        service = new ProductAlertService(
                userRepository,
                subscriptionRepository,
                candidateRepository,
                new BarcodeValidationService(),
                properties,
                new SimpleMeterRegistry(),
                clock);
    }

    @Test
    void upsertSubscriptionNormalizesEmailAndGtin()
    {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findById("test@example.org#1234567890128#NEW")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionDto subscription = service.upsertSubscription(new SubscriptionUpsertRequest(
                " Test@Example.org ",
                "1234567890128",
                ProductCondition.NEW,
                99.99,
                true));

        assertThat(subscription.email()).isEqualTo("test@example.org");
        assertThat(subscription.gtin()).isEqualTo("1234567890128");
        assertThat(subscription.alertPrice()).isEqualTo(99.99);
    }

    @Test
    void upsertSubscriptionRejectsInvalidGtin()
    {
        assertThatThrownBy(() -> service.upsertSubscription(new SubscriptionUpsertRequest(
                "test@example.org",
                "abc",
                ProductCondition.NEW,
                null,
                true)))
                .isInstanceOf(ProductAlertBadRequestException.class)
                .hasMessageContaining("gtin");
    }

    @Test
    void ingestPriceEventsCreatesCandidateWhenSubscriptionMatches()
    {
        ProductAlertSubscription subscription = subscription("alice@example.org", 1234567890128L, ProductCondition.NEW);
        subscription.setAlertPrice(95d);

        when(subscriptionRepository.findByEnabledTrueAndGtinAndCondition(1234567890128L, ProductCondition.NEW))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceEventsIngestionResponse response = service.ingestPriceEvents(new PriceEventsIngestionRequest(List.of(
                new InternalPriceEventDto(
                        1234567890128L,
                        ProductCondition.NEW,
                        100d,
                        90d,
                        Instant.parse("2026-03-13T10:15:30Z")))));

        assertThat(response.receivedEvents()).isEqualTo(1);
        assertThat(response.matchedSubscriptions()).isEqualTo(1);
        assertThat(response.createdCandidates()).isEqualTo(1);

        ArgumentCaptor<ProductAlertNotificationCandidate> candidateCaptor = ArgumentCaptor.forClass(ProductAlertNotificationCandidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getEmail()).isEqualTo("alice@example.org");
        assertThat(candidateCaptor.getValue().getCurrentPrice()).isEqualTo(90d);
        assertThat(subscription.getLastTriggeredPrice()).isEqualTo(90d);
    }

    @Test
    void ingestPriceEventsSkipsDuplicateWithinDedupWindow()
    {
        ProductAlertSubscription subscription = subscription("alice@example.org", 1234567890128L, ProductCondition.NEW);
        subscription.setLastTriggeredPrice(90d);
        subscription.setLastTriggeredAt(Instant.parse("2026-03-13T08:15:30Z"));

        when(subscriptionRepository.findByEnabledTrueAndGtinAndCondition(1234567890128L, ProductCondition.NEW))
                .thenReturn(List.of(subscription));

        PriceEventsIngestionResponse response = service.ingestPriceEvents(new PriceEventsIngestionRequest(List.of(
                new InternalPriceEventDto(
                        1234567890128L,
                        ProductCondition.NEW,
                        100d,
                        90d,
                        Instant.parse("2026-03-13T10:15:30Z")))));

        assertThat(response.matchedSubscriptions()).isEqualTo(1);
        assertThat(response.createdCandidates()).isZero();
        verify(candidateRepository, never()).save(any());
    }

    private ProductAlertSubscription subscription(String email, Long gtin, ProductCondition condition)
    {
        ProductAlertSubscription subscription = new ProductAlertSubscription();
        subscription.setId(email + "#" + gtin + "#" + condition.name());
        subscription.setEmail(email);
        subscription.setGtin(gtin);
        subscription.setCondition(condition);
        subscription.setAlertOnDecrease(true);
        subscription.setEnabled(true);
        subscription.setCreatedAt(Instant.now(clock));
        subscription.setUpdatedAt(Instant.now(clock));
        return subscription;
    }
}
