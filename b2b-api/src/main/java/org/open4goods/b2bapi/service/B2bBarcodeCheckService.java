package org.open4goods.b2bapi.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.dto.barcode.check.BarcodeCheckResponse;
import org.open4goods.b2bapi.dto.barcode.check.BarcodeForensicsDto;
import org.open4goods.b2bapi.dto.barcode.check.ProductTeaserDto;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.product.BarcodeForensics;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementing the {@code barcode.check} facet.
 *
 * <p>Provides two entry points:
 * <ul>
 *   <li>{@link #checkPublic(String, String)} — unauthenticated, IP-rate-limited,
 *       no usage event (promo tool).</li>
 *   <li>{@link #check(String, ApiKeyPrincipal, HttpServletRequest, HttpServletResponse)} —
 *       API-key-authenticated, key-rate-limited, emits a zero-cost usage event.</li>
 * </ul>
 *
 * <p>This is a free facet (credits = 0, billable-when = never).
 * No credit reservation or settlement takes place.
 */
@Service
public class B2bBarcodeCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(B2bBarcodeCheckService.class);
    public static final String FACET_BARCODE_CHECK = "barcode.check";

    private static final String FLAG_URL_TEMPLATE = "/images/flags/%s.webp";
    private static final String PRODUCT_URL_TEMPLATE = "%s/fr/product/%s";

    private final B2bApiProperties b2bApiProperties;
    private final BarcodeForensicsService barcodeForensicsService;
    private final ProductRepository productRepository;
    private final RedisMeteringService redisMeteringService;
    private final UsageStreamService usageStreamService;
    private final Clock clock;

    @Autowired
    public B2bBarcodeCheckService(
            final B2bApiProperties b2bApiProperties,
            final BarcodeForensicsService barcodeForensicsService,
            final ObjectProvider<ProductRepository> productRepository,
            final ObjectProvider<RedisMeteringService> redisMeteringService,
            final ObjectProvider<UsageStreamService> usageStreamService) {
        this(
                b2bApiProperties,
                barcodeForensicsService,
                productRepository.getIfAvailable(),
                redisMeteringService.getIfAvailable(),
                usageStreamService.getIfAvailable(),
                Clock.systemUTC());
    }

    B2bBarcodeCheckService(
            final B2bApiProperties b2bApiProperties,
            final BarcodeForensicsService barcodeForensicsService,
            final ProductRepository productRepository,
            final RedisMeteringService redisMeteringService,
            final UsageStreamService usageStreamService,
            final Clock clock) {
        this.b2bApiProperties = b2bApiProperties;
        this.barcodeForensicsService = barcodeForensicsService;
        this.productRepository = productRepository;
        this.redisMeteringService = redisMeteringService;
        this.usageStreamService = usageStreamService;
        this.clock = clock;
    }

    /**
     * Checks a barcode without authentication. Rate-limited by IP address.
     * No usage event is emitted for the anonymous path.
     *
     * @param rawBarcode the barcode string to check
     * @param clientIp the caller's IP address (used for rate limiting)
     * @return check response containing forensics and an optional product teaser
     */
    public BarcodeCheckResponse checkPublic(final String rawBarcode, final String clientIp) {
        if (redisMeteringService != null) {
            redisMeteringService.checkRateLimitByIp(clientIp);
        }
        return buildResponse(rawBarcode);
    }

    /**
     * Checks a barcode for an authenticated API key. Rate-limited by key, emits a
     * zero-cost usage event, and writes metering response headers.
     *
     * @param rawBarcode the barcode string to check
     * @param principal authenticated API key principal
     * @param request servlet request (for request-id resolution)
     * @param response servlet response (for metering headers)
     * @return check response containing forensics and an optional product teaser
     */
    public BarcodeCheckResponse check(
            final String rawBarcode,
            final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final long startTime = clock.millis();
        final UUID orgId = principal.organizationId();
        final UUID keyId = principal.apiKeyId();

        if (redisMeteringService != null) {
            redisMeteringService.checkRateLimit(keyId);
        }

        final String requestId = resolveOrCreateRequestId(request);
        final BarcodeCheckResponse result = buildResponse(rawBarcode);
        final long duration = clock.millis() - startTime;

        setHeadersAndAttributes(request, response, requestId, 0L, duration);

        if (usageStreamService != null) {
            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_BARCODE_CHECK,
                    rawBarcode,
                    requestId,
                    200,
                    false,
                    0L,
                    null,
                    (int) duration,
                    Instant.now(clock)));
        }

        return result;
    }

    private BarcodeCheckResponse buildResponse(final String rawBarcode) {
        final BarcodeForensics forensics = barcodeForensicsService.analyze(rawBarcode);
        final BarcodeForensicsDto forensicsDto = toDto(forensics);

        ProductTeaserDto teaser = null;
        if (forensics.valid() && isNumericGtin(forensics.type())) {
            teaser = fetchTeaser(forensics, rawBarcode);
        }

        return new BarcodeCheckResponse(rawBarcode, forensicsDto, teaser);
    }

    private BarcodeForensicsDto toDto(final BarcodeForensics f) {
        final String flagUrl = f.issuingCountryCode() != null
                ? String.format(FLAG_URL_TEMPLATE, f.issuingCountryCode().toUpperCase(Locale.ROOT)) : null;
        final String countryName = resolveCountryName(f.issuingCountryCode());
        return new BarcodeForensicsDto(
                f.valid(),
                f.type(),
                f.gs1Prefix(),
                f.issuingCountryCode(),
                countryName,
                flagUrl,
                f.gs1Class(),
                gs1ClassLabel(f.gs1Class()),
                f.packagingIndicator(),
                f.isbnRegistrationGroup(),
                f.normalizedGtin14(),
                f.normalizedGtin13(),
                f.checkDigit());
    }

    private ProductTeaserDto fetchTeaser(final BarcodeForensics forensics, final String rawBarcode) {
        if (productRepository == null) {
            return null;
        }
        try {
            final String gtin = forensics.normalizedGtin13() != null
                    ? forensics.normalizedGtin13() : rawBarcode.trim();
            final Long productId = Long.parseLong(gtin);
            final Product product = productRepository.getByIdWithoutEmbedding(productId);

            final String title = resolveTitle(product);
            final String coverUrl = product.getCoverImagePath();
            final int offersCount = product.getOffersCount() != null ? product.getOffersCount() : 0;
            final AggregatedPrice bestPrice = product.bestPrice();
            final Double priceAmount = bestPrice != null ? bestPrice.getPrice() : null;
            final java.util.Currency currency = (bestPrice != null && bestPrice.getCurrency() != null) ? java.util.Currency.getInstance(bestPrice.getCurrency().name()) : null;
            final String productUrl = String.format(PRODUCT_URL_TEMPLATE,
                    b2bApiProperties.getPublicBaseUrl().toString().replace(
                            b2bApiProperties.getPublicBaseUrl().getPath(), ""),
                    gtin);
            // Use a hardcoded nudger.fr URL since it is the consumer frontend
            final String nudgerUrl = "https://www.nudger.fr/fr/product/" + gtin;

            return new ProductTeaserDto(gtin, title, coverUrl, offersCount, priceAmount, currency, nudgerUrl);
        } catch (final org.open4goods.model.exceptions.ResourceNotFoundException ex) {
            LOGGER.debug("No product found for barcode {}", rawBarcode);
            return null;
        } catch (final NumberFormatException ex) {
            LOGGER.debug("Cannot parse GTIN '{}' as Long", rawBarcode);
            return null;
        } catch (final Exception ex) {
            LOGGER.warn("Unexpected error fetching product teaser for barcode {}", rawBarcode, ex);
            return null;
        }
    }

    private String resolveTitle(final Product product) {
        if (product.getNames() == null) {
            return null;
        }
        final var displayName = product.getNames().getDisplayName();
        if (displayName != null) {
            String name = displayName.get("fr");
            if (name == null) {
                name = displayName.get("en");
            }
            if (name == null && !displayName.isEmpty()) {
                name = displayName.values().iterator().next();
            }
            return name;
        }
        return null;
    }

    private boolean isNumericGtin(final BarcodeType type) {
        return type == BarcodeType.GTIN_13 || type == BarcodeType.GTIN_12
                || type == BarcodeType.GTIN_8 || type == BarcodeType.GTIN_14
                || type == BarcodeType.ISBN_13;
    }

    private String gs1ClassLabel(final Gs1Class gs1Class) {
        if (gs1Class == null) {
            return null;
        }
        return switch (gs1Class) {
            case GTIN -> "Standard trade item";
            case ISBN_BOOKLAND -> "ISBN book (Bookland)";
            case ISMN_MUSIC -> "ISMN music publication";
            case ISSN_PERIODICAL -> "ISSN periodical / serial";
            case RESTRICTED_INTERNAL -> "Restricted / in-store / variable-weight";
            case COUPON -> "Coupon";
            case UNKNOWN -> "Unknown";
        };
    }

    private String resolveCountryName(final String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        try {
            return new Locale("", countryCode).getDisplayCountry(Locale.ENGLISH);
        } catch (final Exception ex) {
            return countryCode;
        }
    }

    private String resolveOrCreateRequestId(final HttpServletRequest request) {
        if (request != null) {
            final Object attributeId = request.getAttribute("X-Request-Id");
            if (attributeId instanceof String) {
                return (String) attributeId;
            }
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
            final long responseTimeMs) {
        if (request != null) {
            request.setAttribute("X-Request-Id", requestId);
        }
        if (response != null) {
            response.setHeader("X-Request-Id", requestId);
            response.setHeader("X-Credits-Consumed", String.valueOf(creditsConsumed));
            response.setHeader("X-Response-Time-Ms", String.valueOf(responseTimeMs));
        }
    }
}
