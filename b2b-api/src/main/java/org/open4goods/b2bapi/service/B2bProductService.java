package org.open4goods.b2bapi.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.product.B2bCoverageMeta;
import org.open4goods.b2bapi.dto.product.B2bFacetMeta;
import org.open4goods.b2bapi.dto.product.B2bMeta;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating the B2B product query workflow, billing reservation, Postgres settlement,
 * and usage event auditing.
 */
@Service
public class B2bProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(B2bProductService.class);
    private static final String FACET_PRICE = "product.price";

    private final B2bApiProperties b2bApiProperties;
    private final BillingCatalogProperties billingCatalogProperties;
    private final RedisMeteringService redisMeteringService;
    private final CreditLedgerService creditLedgerService;
    private final CreditBucketRepository creditBucketRepository;
    private final GtinNormalizationService gtinNormalizationService;
    private final ProductRepository productRepository;
    private final ProductPriceMappingService productPriceMappingService;
    private final UsageStreamService usageStreamService;
    private final Clock clock;

    @Autowired
    public B2bProductService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties billingCatalogProperties,
            final RedisMeteringService redisMeteringService,
            final CreditLedgerService creditLedgerService,
            final CreditBucketRepository creditBucketRepository,
            final GtinNormalizationService gtinNormalizationService,
            final ProductRepository productRepository,
            final ProductPriceMappingService productPriceMappingService,
            final UsageStreamService usageStreamService) {
        this(
                b2bApiProperties,
                billingCatalogProperties,
                redisMeteringService,
                creditLedgerService,
                creditBucketRepository,
                gtinNormalizationService,
                productRepository,
                productPriceMappingService,
                usageStreamService,
                Clock.systemUTC());
    }

    B2bProductService(
            final B2bApiProperties b2bApiProperties,
            final BillingCatalogProperties billingCatalogProperties,
            final RedisMeteringService redisMeteringService,
            final CreditLedgerService creditLedgerService,
            final CreditBucketRepository creditBucketRepository,
            final GtinNormalizationService gtinNormalizationService,
            final ProductRepository productRepository,
            final ProductPriceMappingService productPriceMappingService,
            final UsageStreamService usageStreamService,
            final Clock clock) {
        this.b2bApiProperties = b2bApiProperties;
        this.billingCatalogProperties = billingCatalogProperties;
        this.redisMeteringService = redisMeteringService;
        this.creditLedgerService = creditLedgerService;
        this.creditBucketRepository = creditBucketRepository;
        this.gtinNormalizationService = gtinNormalizationService;
        this.productRepository = productRepository;
        this.productPriceMappingService = productPriceMappingService;
        this.usageStreamService = usageStreamService;
        this.clock = clock;
    }

    /**
     * Retrieves the price facet of a product by its raw GTIN string, performs rate limiting,
     * credit checks, durable transaction settlement, and records usage telemetry.
     *
     * @param rawGtin raw GTIN input
     * @param language requested response language (e.g. "en", "fr")
     * @param principal authenticated API key principal
     * @param request servlet request object
     * @param response servlet response object
     * @return standard B2B response envelope containing mapped price details and metadata
     */
    public B2bResponse<B2bPriceDto> getProductPrice(
            final String rawGtin,
            final String language,
            final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final long startTime = clock.millis();
        final UUID orgId = principal.organizationId();
        final UUID keyId = principal.apiKeyId();

        // 1. Rate limiting check
        redisMeteringService.checkRateLimit(keyId);

        // 2. Validate GTIN before any credit reservation
        NormalizedGtin normalizedGtin;
        try {
            normalizedGtin = gtinNormalizationService.normalize(rawGtin);
        } catch (final InvalidGtinException ex) {
            final long duration = clock.millis() - startTime;
            final String requestId = resolveOrCreateRequestId(request);
            long remaining = 0;
            try {
                remaining = creditBucketRepository.sumLiveCredits(orgId);
            } catch (final Exception e) {
                LOGGER.warn("Failed to retrieve durable credit balance for orgId={}", orgId, e);
            }
            setHeadersAndAttributes(request, response, requestId, 0L, remaining, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_PRICE,
                    rawGtin,
                    requestId,
                    400,
                    false,
                    0L,
                    "invalid-gtin",
                    (int) duration,
                    Instant.now(clock)));
            throw ex;
        }

        final String gtin = normalizedGtin.value();
        final Long productId = normalizedGtin.productId();

        // 3. Max cost lookup from facet catalog
        final int maxCost = getFacetCreditsPrice();

        // 4. Reserve credits in Redis (retry once if balance not loaded)
        boolean reserved = false;
        long currentRedisBalance = 0;

        RedisBalanceResult reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        if (reserveResult.status() == RedisBalanceStatus.BALANCE_NOT_LOADED) {
            final long dbBalance = creditBucketRepository.sumLiveCredits(orgId);
            redisMeteringService.reconcileBalance(orgId, dbBalance);
            reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        }

        if (reserveResult.status() == RedisBalanceStatus.RESERVED) {
            reserved = true;
            currentRedisBalance = reserveResult.balance();
        } else if (reserveResult.status() == RedisBalanceStatus.INSUFFICIENT_CREDITS) {
            final long duration = clock.millis() - startTime;
            final String requestId = resolveOrCreateRequestId(request);
            long remaining = 0;
            try {
                remaining = creditBucketRepository.sumLiveCredits(orgId);
            } catch (final Exception e) {
                LOGGER.warn("Failed to retrieve durable credit balance for orgId={}", orgId, e);
            }
            setHeadersAndAttributes(request, response, requestId, 0L, remaining, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_PRICE,
                    gtin,
                    requestId,
                    402,
                    false,
                    0L,
                    "insufficient-credits",
                    (int) duration,
                    Instant.now(clock)));
            throw new InsufficientCreditsException("Insufficient credits to perform request.");
        } else {
            throw new RedisUnavailableException("Redis is unavailable or organization balance cannot be loaded.");
        }

        final String requestId = resolveOrCreateRequestId(request);
        boolean billable = false;
        long actualCost = 0;
        String noPayReason = null;
        int httpStatus = 200;

        Product product = null;
        B2bPriceDto data = null;
        long remainingBalance = currentRedisBalance;

        try {
            // 5. Retrieve product from Elasticsearch
            try {
                product = productRepository.getByIdWithoutEmbedding(productId);
            } catch (final org.open4goods.model.exceptions.ResourceNotFoundException ex) {
                httpStatus = 404;
                noPayReason = "not-found";
                throw new org.open4goods.b2bapi.exception.ResourceNotFoundException("Product not found.");
            }

            // 6. Map to sanitized DTO and determine freshness/served
            final int freshnessDays = b2bApiProperties.getPrice().getFreshnessDays();
            data = productPriceMappingService.map(product, gtin, freshnessDays);

            final boolean served = data.freshOffersCount() > 0;
            if (served) {
                billable = true;
                actualCost = maxCost;
            } else {
                noPayReason = "no-fresh-offer";
            }

        } catch (final Throwable t) {
            if (t instanceof org.open4goods.b2bapi.exception.ResourceNotFoundException) {
                // Handled successfully in terms of HTTP response mapping
            } else {
                httpStatus = 500;
                noPayReason = "internal-error";
            }
            throw t;
        } finally {
            // 7. Settle the actual debit or refund reservation
            if (reserved) {
                if (actualCost == 0) {
                    final RedisBalanceResult refundResult = redisMeteringService.refundCredits(orgId, maxCost);
                    if (refundResult.status() == RedisBalanceStatus.UPDATED) {
                        remainingBalance = refundResult.balance();
                    } else {
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                    }
                } else {
                    try {
                        final CreditSettlementResult settlementResult = creditLedgerService.settleDebit(
                                orgId,
                                requestId,
                                FACET_PRICE,
                                gtin,
                                actualCost);
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
                        // Rare: Buckets expired/changed between reservation and settlement
                        redisMeteringService.refundCredits(orgId, maxCost);
                        httpStatus = 402;
                        billable = false;
                        actualCost = 0;
                        noPayReason = "insufficient-credits";
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);

                        final long duration = clock.millis() - startTime;
                        setHeadersAndAttributes(request, response, requestId, 0L, remainingBalance, duration);

                        usageStreamService.emit(new UsageStreamEvent(
                                orgId,
                                keyId,
                                FACET_PRICE,
                                gtin,
                                requestId,
                                402,
                                false,
                                0L,
                                "insufficient-credits",
                                (int) duration,
                                Instant.now(clock)));
                        throw ex;
                    }
                }
            }

            final long duration = clock.millis() - startTime;
            setHeadersAndAttributes(request, response, requestId, actualCost, remainingBalance, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_PRICE,
                    gtin,
                    requestId,
                    httpStatus,
                    billable,
                    actualCost,
                    noPayReason,
                    (int) duration,
                    Instant.now(clock)));
        }

        final long totalDuration = clock.millis() - startTime;
        final B2bFacetMeta facetMeta = new B2bFacetMeta(FACET_PRICE, maxCost, data.freshOffersCount() > 0, billable);
        final B2bCoverageMeta coverageMeta = new B2bCoverageMeta(FACET_PRICE, true);

        final B2bMeta meta = new B2bMeta(
                requestId,
                Instant.now(clock),
                language != null ? language : "en",
                actualCost,
                remainingBalance,
                billable,
                b2bApiProperties.getPrice().getFreshnessDays(),
                totalDuration,
                List.of(facetMeta),
                List.of(coverageMeta));

        return new B2bResponse<>(data, meta);
    }

    private int getFacetCreditsPrice() {
        final BillingCatalogProperties.Facet facet = billingCatalogProperties.getFacets().get(FACET_PRICE);
        if (facet == null) {
            return 5; // Fallback default if not configured
        }
        return facet.getCredits();
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

    private void setHeadersAndAttributes(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String requestId,
            final long creditsConsumed,
            final long creditsRemaining,
            final long responseTimeMs) {
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
}
