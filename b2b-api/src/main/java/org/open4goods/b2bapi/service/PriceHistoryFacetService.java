package org.open4goods.b2bapi.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.product.B2bCoverageMeta;
import org.open4goods.b2bapi.dto.product.B2bFacetMeta;
import org.open4goods.b2bapi.dto.product.B2bMeta;
import org.open4goods.b2bapi.dto.product.B2bPriceHistoryChangePointDto;
import org.open4goods.b2bapi.dto.product.B2bPriceHistoryDayPointDto;
import org.open4goods.b2bapi.dto.product.B2bPriceHistoryDto;
import org.open4goods.b2bapi.dto.product.B2bPriceHistorySeriesDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.b2bapi.exception.InvalidPriceHistoryQueryException;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.service.pricehistory.PriceHistoryPublicCursor;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicy;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.model.provider.PublicProviderLabel;
import org.open4goods.model.provider.PublicProviderLabelRegistry;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceHistoryGranularity;
import org.open4goods.pricehistory.model.PriceHistoryPage;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.open4goods.pricehistory.port.PriceHistoryQueryPort;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the {@code product.price-history} B2B facet: parameter validation, GTIN/product
 * resolution, the reserve/settle credit workflow, source-usage-policy filtering, and mapping to the
 * sanitized public payload.
 *
 * <p>Mirrors the {@code product.price} workflow in {@link B2bProductService#getProductPrice} but
 * bills only when at least one policy-allowed price-history point is served (GOU-28 AC5).
 */
@Service
public class PriceHistoryFacetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceHistoryFacetService.class);
    private static final String FACET_PRICE_HISTORY = "product.price-history";
    private static final Duration DEFAULT_WINDOW = Duration.ofDays(30);
    private static final Duration MAX_DAY_WINDOW = Duration.ofDays(1826); // ~5 years, leap-year inclusive
    private static final Duration MAX_CHANGE_WINDOW = Duration.ofDays(31);
    private static final int DEFAULT_LIMIT = 100;

    private final B2bApiProperties b2bApiProperties;
    private final BillingCatalogProperties billingCatalogProperties;
    private final RedisMeteringService redisMeteringService;
    private final CreditLedgerService creditLedgerService;
    private final CreditBucketRepository creditBucketRepository;
    private final GtinNormalizationService gtinNormalizationService;
    private final ProductRepository productRepository;
    private final PriceHistoryQueryPort priceHistoryQueryPort;
    private final SourceUsagePolicyRegistry sourceUsagePolicyRegistry;
    private final PublicProviderLabelRegistry publicProviderLabelRegistry;
    private final UsageStreamService usageStreamService;
    private final Clock clock;

    @Autowired
    public PriceHistoryFacetService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties billingCatalogProperties,
            final RedisMeteringService redisMeteringService,
            final CreditLedgerService creditLedgerService,
            final CreditBucketRepository creditBucketRepository,
            final GtinNormalizationService gtinNormalizationService,
            final ProductRepository productRepository,
            final PriceHistoryQueryPort priceHistoryQueryPort,
            final SourceUsagePolicyRegistry sourceUsagePolicyRegistry,
            final PublicProviderLabelRegistry publicProviderLabelRegistry,
            final UsageStreamService usageStreamService) {
        this(b2bApiProperties, billingCatalogProperties, redisMeteringService, creditLedgerService,
                creditBucketRepository, gtinNormalizationService, productRepository, priceHistoryQueryPort,
                sourceUsagePolicyRegistry, publicProviderLabelRegistry, usageStreamService, Clock.systemUTC());
    }

    PriceHistoryFacetService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties billingCatalogProperties,
            final RedisMeteringService redisMeteringService,
            final CreditLedgerService creditLedgerService,
            final CreditBucketRepository creditBucketRepository,
            final GtinNormalizationService gtinNormalizationService,
            final ProductRepository productRepository,
            final PriceHistoryQueryPort priceHistoryQueryPort,
            final SourceUsagePolicyRegistry sourceUsagePolicyRegistry,
            final PublicProviderLabelRegistry publicProviderLabelRegistry,
            final UsageStreamService usageStreamService,
            final Clock clock) {
        this.b2bApiProperties = b2bApiProperties;
        this.billingCatalogProperties = billingCatalogProperties;
        this.redisMeteringService = redisMeteringService;
        this.creditLedgerService = creditLedgerService;
        this.creditBucketRepository = creditBucketRepository;
        this.gtinNormalizationService = gtinNormalizationService;
        this.productRepository = productRepository;
        this.priceHistoryQueryPort = priceHistoryQueryPort;
        this.sourceUsagePolicyRegistry = sourceUsagePolicyRegistry;
        this.publicProviderLabelRegistry = publicProviderLabelRegistry;
        this.usageStreamService = usageStreamService;
        this.clock = clock;
    }

    /**
     * Serves the price-history facet, billing 8 credits only when at least one policy-allowed point
     * is returned.
     *
     * @param rawGtin raw GTIN input
     * @param language requested response language
     * @param rawFrom inclusive UTC start (ISO-8601 instant), or {@code null} for the default window
     * @param rawTo exclusive UTC end (ISO-8601 instant), or {@code null} for the default window
     * @param rawGranularity {@code DAY} or {@code CHANGE}, or {@code null} to default to {@code DAY}
     * @param rawProvider public provider selector, or {@code null}
     * @param rawCondition {@code NEW}/{@code OCCASION}/{@code UNKNOWN}, or {@code null}
     * @param rawCurrency ISO 4217 currency code, or {@code null}
     * @param rawLimit page size, or {@code null} for the default
     * @param rawCursor opaque continuation from a previous page, or {@code null}
     * @param principal authenticated API key principal
     * @param request servlet request
     * @param response servlet response
     * @return standard B2B response envelope
     */
    public B2bResponse<B2bPriceHistoryDto> getProductPriceHistory(
            final String rawGtin,
            final String language,
            final String rawFrom,
            final String rawTo,
            final String rawGranularity,
            final String rawProvider,
            final String rawCondition,
            final String rawCurrency,
            final Integer rawLimit,
            final String rawCursor,
            final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final long startTime = clock.millis();
        final UUID orgId = principal.organizationId();
        final UUID keyId = principal.apiKeyId();

        redisMeteringService.checkRateLimit(keyId);

        final NormalizedGtin normalizedGtin;
        try {
            normalizedGtin = gtinNormalizationService.normalize(rawGtin);
        } catch (final InvalidGtinException ex) {
            emitNoPay(orgId, keyId, rawGtin, request, response, startTime, 400, "invalid-gtin");
            throw ex;
        }
        final String gtin = normalizedGtin.value();

        final ParsedRequest parsed;
        try {
            parsed = parseAndValidate(gtin, rawFrom, rawTo, rawGranularity, rawProvider, rawCondition, rawCurrency,
                    rawLimit, rawCursor);
        } catch (final InvalidPriceHistoryQueryException ex) {
            emitNoPay(orgId, keyId, gtin, request, response, startTime, 400, "invalid-parameter");
            throw ex;
        }

        final int maxCost = getFacetCreditsPrice();
        RedisBalanceResult reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        if (reserveResult.status() == RedisBalanceStatus.BALANCE_NOT_LOADED) {
            final long dbBalance = creditBucketRepository.sumLiveCredits(orgId);
            redisMeteringService.reconcileBalance(orgId, dbBalance);
            reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        }

        boolean reserved;
        long currentRedisBalance;
        if (reserveResult.status() == RedisBalanceStatus.RESERVED) {
            reserved = true;
            currentRedisBalance = reserveResult.balance();
        } else if (reserveResult.status() == RedisBalanceStatus.INSUFFICIENT_CREDITS) {
            emitNoPay(orgId, keyId, gtin, request, response, startTime, 402, "insufficient-credits");
            throw new InsufficientCreditsException("Insufficient credits to perform request.");
        } else {
            throw new RedisUnavailableException("Redis is unavailable or organization balance cannot be loaded.");
        }

        final String requestId = resolveOrCreateRequestId(request);
        boolean billable = false;
        long actualCost = 0;
        String noPayReason = null;
        int httpStatus = 200;
        B2bPriceHistoryDto data = null;
        long remainingBalance = currentRedisBalance;

        try {
            Product product;
            try {
                product = productRepository.getByIdWithoutEmbedding(normalizedGtin.productId());
            } catch (final org.open4goods.model.exceptions.ResourceNotFoundException ex) {
                httpStatus = 404;
                noPayReason = "not-found";
                throw new org.open4goods.b2bapi.exception.ResourceNotFoundException("Product not found.");
            }
            Objects.requireNonNull(product, "productRepository must not return a null product without throwing");

            data = queryAndMap(gtin, parsed);
            final boolean served = !data.series().isEmpty();
            if (served) {
                billable = true;
                actualCost = maxCost;
            } else {
                noPayReason = "no-price-history";
            }
        } catch (final Throwable t) {
            if (!(t instanceof org.open4goods.b2bapi.exception.ResourceNotFoundException)) {
                httpStatus = 500;
                noPayReason = "internal-error";
            }
            throw t;
        } finally {
            if (reserved) {
                if (actualCost == 0) {
                    final RedisBalanceResult refundResult = redisMeteringService.refundCredits(orgId, maxCost);
                    remainingBalance = refundResult.status() == RedisBalanceStatus.UPDATED
                            ? refundResult.balance()
                            : creditBucketRepository.sumLiveCredits(orgId);
                } else {
                    try {
                        final CreditSettlementResult settlementResult = creditLedgerService.settleDebit(
                                orgId, requestId, FACET_PRICE_HISTORY, gtin, actualCost);
                        remainingBalance = settlementResult.durableBalance();
                        if (settlementResult.idempotentReplay()) {
                            actualCost = 0;
                        }
                        final long refund = maxCost - actualCost;
                        if (refund > 0) {
                            redisMeteringService.refundCredits(orgId, refund);
                        }
                        redisMeteringService.reconcileBalance(orgId, remainingBalance);
                    } catch (final InsufficientCreditsException ex) {
                        redisMeteringService.refundCredits(orgId, maxCost);
                        httpStatus = 402;
                        billable = false;
                        actualCost = 0;
                        noPayReason = "insufficient-credits";
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                        emitUsage(orgId, keyId, gtin, requestId, request, response, startTime, 402, false, 0L,
                                "insufficient-credits", remainingBalance);
                        throw ex;
                    }
                }
            }
            emitUsage(orgId, keyId, gtin, requestId, request, response, startTime, httpStatus, billable, actualCost,
                    noPayReason, remainingBalance);
        }

        final long totalDuration = clock.millis() - startTime;
        final B2bFacetMeta facetMeta = new B2bFacetMeta(FACET_PRICE_HISTORY, maxCost, !data.series().isEmpty(), billable);
        final B2bCoverageMeta coverageMeta = new B2bCoverageMeta(FACET_PRICE_HISTORY, true);
        final B2bMeta meta = new B2bMeta(
                requestId,
                Instant.now(clock),
                language != null ? language : "en",
                actualCost,
                remainingBalance,
                billable,
                0,
                totalDuration,
                List.of(facetMeta),
                List.of(coverageMeta));

        return new B2bResponse<>(data, meta);
    }

    private ParsedRequest parseAndValidate(
            final String gtin,
            final String rawFrom,
            final String rawTo,
            final String rawGranularity,
            final String rawProvider,
            final String rawCondition,
            final String rawCurrency,
            final Integer rawLimit,
            final String rawCursor) {

        final Instant now = Instant.now(clock);

        final PriceHistoryGranularity granularity = parseGranularity(rawGranularity);

        final Instant to = parseInstant("to", rawTo, now);
        final Instant from = parseInstant("from", rawFrom, to.minus(DEFAULT_WINDOW));

        if (!from.isBefore(to)) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_DATE_RANGE,
                    "'from' must be strictly before 'to' (half-open range)");
        }
        if (!from.isBefore(now)) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_DATE_RANGE,
                    "the requested range must not lie entirely in the future");
        }
        final Duration window = Duration.between(from, to);
        if (granularity == PriceHistoryGranularity.DAY && window.compareTo(MAX_DAY_WINDOW) > 0) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_DATE_RANGE,
                    "DAY granularity may query at most five years");
        }
        if (granularity == PriceHistoryGranularity.CHANGE && window.compareTo(MAX_CHANGE_WINDOW) > 0) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_DATE_RANGE,
                    "CHANGE granularity may query at most 31 days");
        }

        final int limit = rawLimit == null ? DEFAULT_LIMIT : rawLimit;
        if (limit < PriceHistoryQuery.MIN_PAGE_SIZE || limit > PriceHistoryQuery.MAX_PAGE_SIZE) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_PARAMETER,
                    "'limit' must be between " + PriceHistoryQuery.MIN_PAGE_SIZE + " and " + PriceHistoryQuery.MAX_PAGE_SIZE);
        }

        String providerSourceId = null;
        if (rawProvider != null && !rawProvider.isBlank()) {
            providerSourceId = publicProviderLabelRegistry.findSourceId(rawProvider)
                    .orElseThrow(() -> new InvalidPriceHistoryQueryException(ErrorCode.INVALID_PARAMETER,
                            "'provider' must be a reviewed public provider selector"));
        }

        OfferCondition condition = null;
        if (rawCondition != null && !rawCondition.isBlank()) {
            try {
                condition = OfferCondition.valueOf(rawCondition.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ex) {
                throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_PARAMETER,
                        "'condition' must be one of NEW, OCCASION, UNKNOWN");
            }
        }

        Currency currency = null;
        if (rawCurrency != null && !rawCurrency.isBlank()) {
            try {
                currency = Currency.getInstance(rawCurrency.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ex) {
                throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_PARAMETER,
                        "'currency' must be a valid ISO 4217 code");
            }
        }

        final String fingerprint = PriceHistoryPublicCursor.fingerprint(
                gtin, granularity, providerSourceId, condition == null ? null : condition.name(),
                currency == null ? null : currency.getCurrencyCode(), from, to, limit);

        Optional<String> innerCursor = Optional.empty();
        if (rawCursor != null && !rawCursor.isBlank()) {
            innerCursor = PriceHistoryPublicCursor.decode(rawCursor, fingerprint);
            if (innerCursor.isEmpty()) {
                throw new InvalidPriceHistoryQueryException(ErrorCode.CURSOR_MISMATCH,
                        "cursor does not match the current request parameters");
            }
        }

        final PriceHistoryQuery query;
        try {
            query = new PriceHistoryQuery(
                    from, to, Optional.of(granularity),
                    Optional.of(new org.open4goods.datareference.model.Gtin(gtin)),
                    Optional.ofNullable(providerSourceId).map(SourceId::new),
                    Optional.ofNullable(condition),
                    Optional.ofNullable(currency),
                    innerCursor,
                    limit,
                    false);
        } catch (final IllegalArgumentException ex) {
            throw new InvalidPriceHistoryQueryException(ex.getMessage());
        }

        return new ParsedRequest(query, fingerprint);
    }

    private PriceHistoryGranularity parseGranularity(final String raw) {
        if (raw == null || raw.isBlank()) {
            return PriceHistoryGranularity.DAY;
        }
        try {
            return PriceHistoryGranularity.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_PARAMETER,
                    "'granularity' must be DAY or CHANGE");
        }
    }

    private Instant parseInstant(final String field, final String raw, final Instant fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(raw);
        } catch (final DateTimeParseException ex) {
            throw new InvalidPriceHistoryQueryException(ErrorCode.INVALID_DATE_RANGE,
                    "'" + field + "' must be an ISO-8601 UTC instant");
        }
    }

    private B2bPriceHistoryDto queryAndMap(final String gtin, final ParsedRequest parsed) {
        final PriceHistoryQuery query = parsed.query();
        final PriceHistoryGranularity granularity = query.effectiveGranularity();
        final Instant now = Instant.now(clock);

        final Map<SeriesKey, List<B2bPriceHistoryDayPointDto>> dayPoints = new LinkedHashMap<>();
        final Map<SeriesKey, List<B2bPriceHistoryChangePointDto>> changePoints = new LinkedHashMap<>();
        String nextInnerCursor = null;

        if (granularity == PriceHistoryGranularity.DAY) {
            final PriceHistoryPage<DailyProviderRollup> page = priceHistoryQueryPort.queryDaily(query);
            for (final DailyProviderRollup rollup : page.values()) {
                if (!isDailyProviderAllowed(rollup, now)) {
                    continue;
                }
                final PublicProviderLabel label = publicProviderLabelRegistry.find(rollup.key().providerId().value()).orElse(null);
                if (label == null) {
                    continue;
                }
                final SeriesKey key = new SeriesKey(label.label(), rollup.key().condition(), rollup.key().currency());
                dayPoints.computeIfAbsent(key, k -> new ArrayList<>()).add(new B2bPriceHistoryDayPointDto(
                        rollup.day(), rollup.minimumAmount(), rollup.maximumAmount(), rollup.closeAmount(),
                        rollup.observedOfferCount()));
            }
            nextInnerCursor = page.nextCursor().orElse(null);
        } else {
            final PriceHistoryPage<PriceChangeEvent> page = priceHistoryQueryPort.queryChanges(query);
            for (final PriceChangeEvent event : page.values()) {
                if (!sourceUsagePolicyRegistry.allows(event.key().providerId(), event.policyRef(),
                        SourceContentType.PRICE, ProjectionSurface.B2B_API, now)) {
                    continue;
                }
                final PublicProviderLabel label = publicProviderLabelRegistry.find(event.key().providerId().value()).orElse(null);
                if (label == null) {
                    continue;
                }
                final SeriesKey key = new SeriesKey(label.label(), event.condition(), event.currency());
                changePoints.computeIfAbsent(key, k -> new ArrayList<>()).add(new B2bPriceHistoryChangePointDto(
                        event.observedAt(), event.amount(), event.availability()));
            }
            nextInnerCursor = page.nextCursor().orElse(null);
        }

        final List<B2bPriceHistorySeriesDto> series = new ArrayList<>();
        for (final Map.Entry<SeriesKey, List<B2bPriceHistoryDayPointDto>> entry : dayPoints.entrySet()) {
            series.add(new B2bPriceHistorySeriesDto(entry.getKey().provider(), entry.getKey().condition(),
                    entry.getKey().currency(), entry.getValue(), List.of()));
        }
        for (final Map.Entry<SeriesKey, List<B2bPriceHistoryChangePointDto>> entry : changePoints.entrySet()) {
            series.add(new B2bPriceHistorySeriesDto(entry.getKey().provider(), entry.getKey().condition(),
                    entry.getKey().currency(), List.of(), entry.getValue()));
        }

        final String nextCursor = nextInnerCursor == null
                ? null
                : PriceHistoryPublicCursor.encode(nextInnerCursor, parsed.fingerprint());

        return new B2bPriceHistoryDto(gtin, query.from(), query.to(), granularity, series, nextCursor);
    }

    /**
     * Daily rollups carry no stamped {@code SourceUsagePolicyRef} (the time-series template has no
     * such field), so DAY-granularity filtering checks whether any currently reviewed policy version
     * for the provider allows {@code PRICE} on {@code B2B_API}, rather than binding to the exact
     * version recorded at ingestion time (as CHANGE events do via {@link PriceChangeEvent#policyRef()}).
     */
    private boolean isDailyProviderAllowed(final DailyProviderRollup rollup, final Instant now) {
        final SourceId providerId = rollup.key().providerId();
        for (final SourceUsagePolicy policy : sourceUsagePolicyRegistry.policies()) {
            if (policy.sourceId().equals(providerId) && policy.allows(SourceContentType.PRICE, ProjectionSurface.B2B_API, now)) {
                return true;
            }
        }
        return false;
    }

    private int getFacetCreditsPrice() {
        final BillingCatalogProperties.Facet facet = billingCatalogProperties.getFacets().get(FACET_PRICE_HISTORY);
        return facet == null ? 8 : facet.getCredits();
    }

    private String resolveOrCreateRequestId(final HttpServletRequest request) {
        if (request != null) {
            final String headerId = request.getHeader("X-Request-Id");
            if (headerId != null && !headerId.isBlank()) {
                return headerId;
            }
        }
        return b2bApiProperties.getRequestIds().getPrefix() + UUID.randomUUID().toString().replace("-", "");
    }

    private void emitNoPay(
            final UUID orgId, final UUID keyId, final String gtin, final HttpServletRequest request,
            final HttpServletResponse response, final long startTime, final int httpStatus, final String noPayReason) {
        final long duration = clock.millis() - startTime;
        final String requestId = resolveOrCreateRequestId(request);
        long remaining = 0;
        try {
            remaining = creditBucketRepository.sumLiveCredits(orgId);
        } catch (final Exception e) {
            LOGGER.warn("Failed to retrieve durable credit balance for orgId={}", orgId, e);
        }
        setHeadersAndAttributes(request, response, requestId, 0L, remaining, duration);
        usageStreamService.emit(new UsageStreamEvent(orgId, keyId, FACET_PRICE_HISTORY, gtin, requestId, httpStatus,
                false, 0L, noPayReason, (int) duration, Instant.now(clock)));
    }

    private void emitUsage(
            final UUID orgId, final UUID keyId, final String gtin, final String requestId,
            final HttpServletRequest request, final HttpServletResponse response, final long startTime,
            final int httpStatus, final boolean billable, final long actualCost, final String noPayReason,
            final long remainingBalance) {
        final long duration = clock.millis() - startTime;
        setHeadersAndAttributes(request, response, requestId, actualCost, remainingBalance, duration);
        usageStreamService.emit(new UsageStreamEvent(orgId, keyId, FACET_PRICE_HISTORY, gtin, requestId, httpStatus,
                billable, actualCost, noPayReason, (int) duration, Instant.now(clock)));
    }

    private void setHeadersAndAttributes(
            final HttpServletRequest request, final HttpServletResponse response, final String requestId,
            final long creditsConsumed, final long creditsRemaining, final long responseTimeMs) {
        if (request != null) {
            request.setAttribute("X-Request-Id", requestId);
        }
        if (response != null) {
            response.setHeader("X-Request-Id", requestId);
            response.setHeader("X-Credits-Consumed", String.valueOf(creditsConsumed));
            response.setHeader("X-Credits-Remaining", String.valueOf(creditsRemaining));
            response.setHeader("X-Response-Time-Ms", String.valueOf(responseTimeMs));
        }
    }

    private record ParsedRequest(PriceHistoryQuery query, String fingerprint) {
    }

    private record SeriesKey(String provider, OfferCondition condition, Currency currency) {
    }
}
