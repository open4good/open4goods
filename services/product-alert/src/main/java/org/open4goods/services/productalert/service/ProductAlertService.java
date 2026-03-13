package org.open4goods.services.productalert.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.services.productalert.config.yml.ProductAlertProperties;
import org.open4goods.services.productalert.controller.ProductAlertBadRequestException;
import org.open4goods.services.productalert.dto.InternalPriceEventDto;
import org.open4goods.services.productalert.dto.PriceEventsIngestionRequest;
import org.open4goods.services.productalert.dto.PriceEventsIngestionResponse;
import org.open4goods.services.productalert.dto.SubscriptionDto;
import org.open4goods.services.productalert.dto.SubscriptionUpsertRequest;
import org.open4goods.services.productalert.dto.UserDto;
import org.open4goods.services.productalert.dto.UserUpsertRequest;
import org.open4goods.services.productalert.model.NotificationCandidateStatus;
import org.open4goods.services.productalert.model.ProductAlertNotificationCandidate;
import org.open4goods.services.productalert.model.ProductAlertSubscription;
import org.open4goods.services.productalert.model.ProductAlertUser;
import org.open4goods.services.productalert.model.ProductAlertUserStatus;
import org.open4goods.services.productalert.repository.ProductAlertNotificationCandidateRepository;
import org.open4goods.services.productalert.repository.ProductAlertSubscriptionRepository;
import org.open4goods.services.productalert.repository.ProductAlertUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Main product alert business service.
 */
@Service
public class ProductAlertService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAlertService.class);

    private final ProductAlertUserRepository userRepository;
    private final ProductAlertSubscriptionRepository subscriptionRepository;
    private final ProductAlertNotificationCandidateRepository notificationCandidateRepository;
    private final BarcodeValidationService barcodeValidationService;
    private final ProductAlertProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param userRepository user repository
     * @param subscriptionRepository subscription repository
     * @param notificationCandidateRepository notification candidate repository
     * @param barcodeValidationService GTIN validation service
     * @param properties service properties
     * @param meterRegistry metrics registry
     * @param clock application clock
     */
    public ProductAlertService(ProductAlertUserRepository userRepository,
            ProductAlertSubscriptionRepository subscriptionRepository,
            ProductAlertNotificationCandidateRepository notificationCandidateRepository,
            BarcodeValidationService barcodeValidationService,
            ProductAlertProperties properties,
            MeterRegistry meterRegistry,
            Clock clock)
    {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notificationCandidateRepository = notificationCandidateRepository;
        this.barcodeValidationService = barcodeValidationService;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    /**
     * Upserts a user from an email address.
     *
     * @param request user request
     * @return normalized user representation
     */
    public UserDto upsertUser(UserUpsertRequest request)
    {
        String normalizedEmail = normalizeEmail(request.email());
        Instant now = Instant.now(clock);

        ProductAlertUser user = userRepository.findById(normalizedEmail).orElseGet(ProductAlertUser::new);
        if (user.getCreatedAt() == null)
        {
            user.setCreatedAt(now);
        }
        user.setId(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setStatus(ProductAlertUserStatus.ACTIVE);
        user.setUpdatedAt(now);

        ProductAlertUser saved = userRepository.save(user);
        return toDto(saved);
    }

    /**
     * Upserts a subscription and auto-creates the user if needed.
     *
     * @param request subscription request
     * @return normalized subscription representation
     */
    public SubscriptionDto upsertSubscription(SubscriptionUpsertRequest request)
    {
        String normalizedEmail = normalizeEmail(request.email());
        Long normalizedGtin = normalizeGtin(request.gtin());
        Instant now = Instant.now(clock);

        upsertUser(new UserUpsertRequest(normalizedEmail));

        String subscriptionId = buildSubscriptionId(normalizedEmail, normalizedGtin, request.condition());
        ProductAlertSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseGet(ProductAlertSubscription::new);

        if (subscription.getCreatedAt() == null)
        {
            subscription.setCreatedAt(now);
        }

        if (request.alertPrice() != null && request.alertPrice() <= 0d)
        {
            throw new ProductAlertBadRequestException("alertPrice must be positive");
        }

        subscription.setId(subscriptionId);
        subscription.setEmail(normalizedEmail);
        subscription.setGtin(normalizedGtin);
        subscription.setCondition(request.condition());
        subscription.setAlertPrice(request.alertPrice());
        subscription.setAlertOnDecrease(request.alertOnDecrease() == null || request.alertOnDecrease());
        subscription.setEnabled(true);
        subscription.setUpdatedAt(now);

        ProductAlertSubscription saved = subscriptionRepository.save(subscription);
        return toDto(saved);
    }

    /**
     * Ingests a batch of internal price-drop events.
     *
     * @param request ingestion request
     * @return ingestion counters
     */
    public PriceEventsIngestionResponse ingestPriceEvents(PriceEventsIngestionRequest request)
    {
        int matchedSubscriptions = 0;
        int createdCandidates = 0;

        for (InternalPriceEventDto event : request.events())
        {
            validateInternalEvent(event);
            meterRegistry.counter("product.alert.events.received").increment();

            List<ProductAlertSubscription> subscriptions = subscriptionRepository
                    .findByEnabledTrueAndGtinAndCondition(event.gtin(), event.condition());

            for (ProductAlertSubscription subscription : subscriptions)
            {
                if (!matches(subscription, event))
                {
                    continue;
                }

                matchedSubscriptions++;
                meterRegistry.counter("product.alert.subscriptions.matched").increment();

                if (isDuplicate(subscription, event))
                {
                    LOGGER.info("Skipping duplicate notification candidate for subscription {} at price {}",
                            subscription.getId(), event.currentPrice());
                    continue;
                }

                ProductAlertNotificationCandidate candidate = new ProductAlertNotificationCandidate();
                candidate.setId(buildCandidateId(subscription, event));
                candidate.setSubscriptionId(subscription.getId());
                candidate.setEmail(subscription.getEmail());
                candidate.setGtin(subscription.getGtin());
                candidate.setCondition(subscription.getCondition());
                candidate.setPreviousPrice(event.previousPrice());
                candidate.setCurrentPrice(event.currentPrice());
                candidate.setEventTimestamp(event.eventTimestamp());
                candidate.setStatus(NotificationCandidateStatus.PENDING);
                candidate.setCreatedAt(Instant.now(clock));
                notificationCandidateRepository.save(candidate);

                subscription.setLastTriggeredAt(event.eventTimestamp());
                subscription.setLastTriggeredPrice(event.currentPrice());
                subscription.setUpdatedAt(Instant.now(clock));
                subscriptionRepository.save(subscription);

                createdCandidates++;
                meterRegistry.counter("product.alert.candidates.created").increment();

                LOGGER.info("Created notification candidate for email {} gtin {} condition {} currentPrice {}",
                        subscription.getEmail(), subscription.getGtin(), subscription.getCondition(), event.currentPrice());
            }
        }

        return new PriceEventsIngestionResponse(request.events().size(), matchedSubscriptions, createdCandidates);
    }

    private void validateInternalEvent(InternalPriceEventDto event)
    {
        if (event.currentPrice() >= event.previousPrice())
        {
            throw new ProductAlertBadRequestException("currentPrice must be lower than previousPrice");
        }
    }

    private boolean matches(ProductAlertSubscription subscription, InternalPriceEventDto event)
    {
        if (subscription.isAlertOnDecrease() && event.currentPrice() >= event.previousPrice())
        {
            return false;
        }

        return subscription.getAlertPrice() == null || event.currentPrice() <= subscription.getAlertPrice();
    }

    private boolean isDuplicate(ProductAlertSubscription subscription, InternalPriceEventDto event)
    {
        Instant lastTriggeredAt = subscription.getLastTriggeredAt();
        Double lastTriggeredPrice = subscription.getLastTriggeredPrice();
        if (lastTriggeredAt == null || lastTriggeredPrice == null)
        {
            return false;
        }

        if (!Objects.equals(lastTriggeredPrice, event.currentPrice()))
        {
            return false;
        }

        Instant dedupThreshold = lastTriggeredAt.plus(properties.getDedupWindow());
        return !event.eventTimestamp().isAfter(dedupThreshold);
    }

    private UserDto toDto(ProductAlertUser user)
    {
        return new UserDto(user.getEmail(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private SubscriptionDto toDto(ProductAlertSubscription subscription)
    {
        return new SubscriptionDto(
                subscription.getId(),
                subscription.getEmail(),
                String.valueOf(subscription.getGtin()),
                subscription.getCondition(),
                subscription.getAlertPrice(),
                subscription.isAlertOnDecrease(),
                subscription.isEnabled(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt(),
                subscription.getLastTriggeredAt(),
                subscription.getLastTriggeredPrice());
    }

    private String normalizeEmail(String email)
    {
        if (StringUtils.isBlank(email))
        {
            throw new ProductAlertBadRequestException("email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Long normalizeGtin(String gtin)
    {
        Entry<BarcodeType, String> sanitized = barcodeValidationService.sanitize(gtin);
        if (sanitized == null || sanitized.getKey() == BarcodeType.UNKNOWN || StringUtils.isBlank(sanitized.getValue()))
        {
            throw new ProductAlertBadRequestException("gtin is invalid");
        }
        try
        {
            return Long.parseLong(sanitized.getValue());
        }
        catch (NumberFormatException exception)
        {
            throw new ProductAlertBadRequestException("gtin is invalid");
        }
    }

    private String buildSubscriptionId(String email, Long gtin, Enum<?> condition)
    {
        return email + "#" + gtin + "#" + condition.name();
    }

    private String buildCandidateId(ProductAlertSubscription subscription, InternalPriceEventDto event)
    {
        return subscription.getId()
                + "#"
                + formatPrice(event.currentPrice())
                + "#"
                + event.eventTimestamp().toEpochMilli();
    }

    private String formatPrice(Double price)
    {
        return BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
    }
}
