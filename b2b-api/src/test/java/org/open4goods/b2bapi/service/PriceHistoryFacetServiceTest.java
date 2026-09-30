package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.product.B2bPriceHistoryDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.b2bapi.exception.InvalidPriceHistoryQueryException;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicy;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.provider.PublicProviderLabelRegistry;
import org.open4goods.pricehistory.model.DailyProviderRollup;
import org.open4goods.pricehistory.model.DailyRollupKey;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.PriceChangeEvent;
import org.open4goods.pricehistory.model.PriceChangeKind;
import org.open4goods.pricehistory.model.PriceHistoryPage;
import org.open4goods.pricehistory.model.PriceHistoryQuery;
import org.open4goods.pricehistory.port.PriceHistoryQueryPort;
import org.open4goods.services.productrepository.services.ProductRepository;

/**
 * Verifies the {@code product.price-history} facet workflow: parameter validation, the 8-credit
 * reserve/settle billing decision, source-usage-policy filtering, and redaction (GOU-28 AC1-AC6, AC8).
 */
class PriceHistoryFacetServiceTest {

    // Must postdate the real merchant-feed/legacy-product-backup policy effectiveFrom (2026-09-12)
    // so the default SourceUsagePolicyRegistry fixtures used by these tests are actually in force.
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID KEY_ID = UUID.randomUUID();
    private static final String GTIN = "00012345678905";
    private static final long PRODUCT_ID = 12345678905L;

    private RedisMeteringService redisMeteringService;
    private CreditLedgerService creditLedgerService;
    private CreditBucketRepository creditBucketRepository;
    private GtinNormalizationService gtinNormalizationService;
    private ProductRepository productRepository;
    private PriceHistoryQueryPort priceHistoryQueryPort;
    private UsageStreamService usageStreamService;
    private PublicProviderLabelRegistry publicProviderLabelRegistry;
    private SourceUsagePolicyRegistry sourceUsagePolicyRegistry;
    private PriceHistoryFacetService service;
    private ApiKeyPrincipal principal;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        redisMeteringService = mock(RedisMeteringService.class);
        creditLedgerService = mock(CreditLedgerService.class);
        creditBucketRepository = mock(CreditBucketRepository.class);
        gtinNormalizationService = mock(GtinNormalizationService.class);
        productRepository = mock(ProductRepository.class);
        priceHistoryQueryPort = mock(PriceHistoryQueryPort.class);
        usageStreamService = mock(UsageStreamService.class);
        publicProviderLabelRegistry = PublicProviderLabelRegistry.loadDefault();
        sourceUsagePolicyRegistry = SourceUsagePolicyRegistry.loadDefault();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        principal = new ApiKeyPrincipal(ORG_ID, KEY_ID);

        when(gtinNormalizationService.normalize(GTIN)).thenReturn(new NormalizedGtin(GTIN, PRODUCT_ID, null));
        when(productRepository.getByIdWithoutEmbedding(PRODUCT_ID)).thenReturn(mock(Product.class));
        when(redisMeteringService.reserveCredits(ORG_ID, 8L)).thenReturn(new RedisBalanceResult(RedisBalanceStatus.RESERVED, 992L));
        when(redisMeteringService.refundCredits(any(), anyLong())).thenReturn(new RedisBalanceResult(RedisBalanceStatus.UPDATED, 1000L));
        when(creditBucketRepository.sumLiveCredits(ORG_ID)).thenReturn(1000L);

        final BillingCatalogProperties catalog = new BillingCatalogProperties();
        final BillingCatalogProperties.Facet facet = new BillingCatalogProperties.Facet();
        facet.setPath("/api/v1/products/{gtin}/price/history");
        facet.setCredits(8);
        facet.setDoc("products/price-history");
        facet.setBillableWhen("has-history");
        catalog.setFacets(java.util.Map.of("product.price-history", facet));

        service = new PriceHistoryFacetService(
                new B2bApiProperties(), catalog, redisMeteringService, creditLedgerService, creditBucketRepository,
                gtinNormalizationService, productRepository, priceHistoryQueryPort, sourceUsagePolicyRegistry,
                publicProviderLabelRegistry, usageStreamService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void defaultsToDayGranularityAndThirtyDayWindowAndBillsWhenServed() {
        DailyProviderRollup rollup = dailyRollup("merchant-feed", OfferCondition.NEW, "EUR",
                LocalDate.of(2026, 10, 10), "749.00", "819.99", "799.99", 3, 2);
        when(priceHistoryQueryPort.queryDaily(any())).thenReturn(new PriceHistoryPage<>(List.of(rollup), Optional.empty()));
        when(creditLedgerService.settleDebit(any(), any(), any(), any(), anyLong()))
                .thenReturn(new CreditSettlementResult(992L, 8L, false));

        B2bResponse<B2bPriceHistoryDto> response = call(null, null, null, null, null, null, null, null);

        assertThat(response.data().granularity()).isEqualTo(org.open4goods.pricehistory.model.PriceHistoryGranularity.DAY);
        assertThat(response.data().from()).isEqualTo(NOW.minus(java.time.Duration.ofDays(30)));
        assertThat(response.data().to()).isEqualTo(NOW);
        assertThat(response.data().series()).hasSize(1);
        assertThat(response.data().series().get(0).provider()).isEqualTo("Merchant Feed Partner");
        assertThat(response.data().series().get(0).dayPoints()).hasSize(1);
        assertThat(response.meta().billable()).isTrue();
        assertThat(response.meta().creditsConsumed()).isEqualTo(8);
        verify(creditLedgerService).settleDebit(org.mockito.ArgumentMatchers.eq(ORG_ID), any(),
                org.mockito.ArgumentMatchers.eq("product.price-history"),
                org.mockito.ArgumentMatchers.eq(GTIN), org.mockito.ArgumentMatchers.eq(8L));
    }

    @Test
    void explicitChangeGranularityWithinThirtyOneDaysMapsAmountAndState() {
        PriceChangeEvent event = changeEvent("merchant-feed", OfferCondition.NEW, "EUR", "799.99",
                OfferAvailability.AVAILABLE, NOW.minus(java.time.Duration.ofDays(1)));
        when(priceHistoryQueryPort.queryChanges(any())).thenReturn(new PriceHistoryPage<>(List.of(event), Optional.empty()));
        when(creditLedgerService.settleDebit(any(), any(), any(), any(), anyLong()))
                .thenReturn(new CreditSettlementResult(992L, 8L, false));

        B2bResponse<B2bPriceHistoryDto> response = call(null, null, "CHANGE", null, null, null, null, null);

        assertThat(response.data().granularity()).isEqualTo(org.open4goods.pricehistory.model.PriceHistoryGranularity.CHANGE);
        assertThat(response.data().series()).hasSize(1);
        assertThat(response.data().series().get(0).changePoints()).hasSize(1);
        assertThat(response.data().series().get(0).changePoints().get(0).state()).isEqualTo(OfferAvailability.AVAILABLE);
        assertThat(response.data().series().get(0).changePoints().get(0).amount()).isEqualByComparingTo("799.99");
    }

    @Test
    void rejectsInvertedRangeAndNeverReserves() {
        assertThatThrownBy(() -> call("2026-10-15T00:00:00Z", "2026-10-01T00:00:00Z", null, null, null, null, null, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
        verify(redisMeteringService, never()).reserveCredits(any(), anyLong());
    }

    @Test
    void rejectsFutureOnlyRange() {
        assertThatThrownBy(() -> call("2026-11-01T00:00:00Z", "2026-12-01T00:00:00Z", null, null, null, null, null, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
        verify(redisMeteringService, never()).reserveCredits(any(), anyLong());
    }

    @Test
    void rejectsDayWindowOverFiveYears() {
        assertThatThrownBy(() -> call("2020-01-01T00:00:00Z", "2026-06-15T00:00:00Z", "DAY", null, null, null, null, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class);
    }

    @Test
    void rejectsChangeWindowOverThirtyOneDays() {
        assertThatThrownBy(() -> call("2026-04-01T00:00:00Z", "2026-06-15T00:00:00Z", "CHANGE", null, null, null, null, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class);
    }

    @Test
    void rejectsLimitBelowMinimum() {
        assertThatThrownBy(() -> call(null, null, null, null, null, null, 0, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
    }

    @Test
    void rejectsLimitAboveMaximum() {
        assertThatThrownBy(() -> call(null, null, null, null, null, null, PriceHistoryQuery.MAX_PAGE_SIZE + 1, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class);
    }

    @Test
    void rejectsCursorThatDoesNotMatchTheCurrentQuery() {
        String cursorForADifferentRequest = org.open4goods.b2bapi.service.pricehistory.PriceHistoryPublicCursor.encode(
                new B2bApiProperties().getPriceHistory().getCursorSecret(), "inner",
                org.open4goods.b2bapi.service.pricehistory.PriceHistoryPublicCursor.fingerprint(
                        GTIN, org.open4goods.pricehistory.model.PriceHistoryGranularity.DAY, null, null, null,
                        NOW.minus(java.time.Duration.ofDays(99)), NOW, 100));

        assertThatThrownBy(() -> call(null, null, null, null, null, null, null, cursorForADifferentRequest))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.CURSOR_MISMATCH));
        verify(redisMeteringService, never()).reserveCredits(any(), anyLong());
    }

    /**
     * A cursor a client could plausibly tamper with or forge (garbage Base64, or a value re-signed
     * with an unkeyed hash the old implementation used) must still surface as a 400
     * {@code cursor-mismatch}, never bubble past decode as an unhandled 500 (GOU-28 review point 3).
     */
    @Test
    void rejectsAForgedOrGarbageCursorAsMismatchNeverAsAnInternalError() {
        assertThatThrownBy(() -> call(null, null, null, null, null, null, null, "not-valid-base64-at-all!!"))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.CURSOR_MISMATCH));

        assertThatThrownBy(() -> call(null, null, null, null, null, null, null,
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("short".getBytes())))
                .isInstanceOf(InvalidPriceHistoryQueryException.class)
                .satisfies(ex -> assertThat(((InvalidPriceHistoryQueryException) ex).errorCode()).isEqualTo(ErrorCode.CURSOR_MISMATCH));
        verify(redisMeteringService, never()).reserveCredits(any(), anyLong());
    }

    /**
     * The public cursor a page actually returns must be accepted as the very next page's request
     * cursor (round trip through the real AES-GCM encode/decode, not a mocked port).
     */
    @Test
    void nextCursorFromOnePageIsAcceptedToResumeTheNextPage() {
        DailyProviderRollup rollup = dailyRollup("merchant-feed", OfferCondition.NEW, "EUR",
                LocalDate.of(2026, 10, 10), "749.00", "819.99", "799.99", 3, 2);
        String innerCursor = org.open4goods.pricehistory.service.PriceHistoryCursor.after(NOW, "merchant-feed", "NEW", "EUR");
        when(priceHistoryQueryPort.queryDaily(any()))
                .thenReturn(new PriceHistoryPage<>(List.of(rollup), Optional.of(innerCursor)))
                .thenReturn(new PriceHistoryPage<>(List.of(rollup), Optional.empty()));
        when(creditLedgerService.settleDebit(any(), any(), any(), any(), anyLong()))
                .thenReturn(new CreditSettlementResult(992L, 8L, false));

        B2bResponse<B2bPriceHistoryDto> firstPage = call(null, null, null, null, null, null, null, null);
        assertThat(firstPage.data().nextCursor()).isNotBlank();

        B2bResponse<B2bPriceHistoryDto> secondPage = call(null, null, null, null, null, null, null,
                firstPage.data().nextCursor());
        assertThat(secondPage.data().nextCursor()).isNull();

        org.mockito.ArgumentCaptor<org.open4goods.pricehistory.model.PriceHistoryQuery> captor =
                org.mockito.ArgumentCaptor.forClass(org.open4goods.pricehistory.model.PriceHistoryQuery.class);
        verify(priceHistoryQueryPort, org.mockito.Mockito.times(2)).queryDaily(captor.capture());
        assertThat(captor.getAllValues().get(1).cursor()).contains(innerCursor);
    }

    @Test
    void rejectsUnknownProviderSelector() {
        assertThatThrownBy(() -> call(null, null, null, "not-a-reviewed-provider", null, null, null, null))
                .isInstanceOf(InvalidPriceHistoryQueryException.class);
    }

    @Test
    void policyDeniedProviderRemovesSeriesBeforeBillingAndRefunds() {
        // "icecat" is UNREVIEWED for PRICE/B2B_API in the default registry: denied by default.
        DailyProviderRollup denied = dailyRollup("icecat", OfferCondition.NEW, "EUR",
                LocalDate.of(2026, 6, 10), "1.00", "1.00", "1.00", 1, 0);
        when(priceHistoryQueryPort.queryDaily(any())).thenReturn(new PriceHistoryPage<>(List.of(denied), Optional.empty()));

        B2bResponse<B2bPriceHistoryDto> response = call(null, null, null, null, null, null, null, null);

        assertThat(response.data().series()).isEmpty();
        assertThat(response.meta().billable()).isFalse();
        assertThat(response.meta().creditsConsumed()).isZero();
        verify(creditLedgerService, never()).settleDebit(any(), any(), any(), any(), anyLong());
        verify(redisMeteringService).refundCredits(ORG_ID, 8L);
    }

    @Test
    void policyAllowedButUnlabeledProviderIsNeverExposed() throws Exception {
        // A source can be policy-reviewed and still carry no reviewed public label; AC4 requires
        // that series to be dropped rather than leaking the raw source id in its place.
        SourceUsagePolicy allowedButUnlabeled = new SourceUsagePolicy(
                "fixture-allowed", new SourceId("fixture-allowed"), "1",
                java.util.Map.of(org.open4goods.datareference.model.SourceContentType.PRICE,
                        java.util.Set.of(org.open4goods.datareference.model.ProjectionSurface.B2B_API)),
                Instant.parse("2026-01-01T00:00:00Z"), null, java.time.Duration.ofDays(30),
                org.open4goods.datareference.model.MediaCachePolicy.NONE,
                org.open4goods.datareference.model.AttributionRequirement.NONE,
                org.open4goods.datareference.model.RedistributionPolicy.ALLOWED,
                org.open4goods.datareference.model.DerivativeLicence.NONE, java.util.Set.of(),
                LocalDate.of(2026, 1, 1), List.of(java.net.URI.create("https://example.test/terms")));
        SourceUsagePolicyRegistry customPolicyRegistry = new SourceUsagePolicyRegistry(
                new org.open4goods.datareference.model.SourceUsagePolicyDocument(
                        org.open4goods.datareference.model.SourceUsagePolicyDocument.SCHEMA_VERSION,
                        List.of(allowedButUnlabeled)));
        PublicProviderLabelRegistry emptyLabelRegistry =
                new PublicProviderLabelRegistry(new org.open4goods.model.provider.PublicProviderLabelDocument(List.of()));

        PriceHistoryFacetService customService = new PriceHistoryFacetService(
                new B2bApiProperties(), billingCatalog(), redisMeteringService, creditLedgerService,
                creditBucketRepository, gtinNormalizationService, productRepository, priceHistoryQueryPort,
                customPolicyRegistry, emptyLabelRegistry, usageStreamService, Clock.fixed(NOW, ZoneOffset.UTC));

        DailyProviderRollup allowedNoLabel = dailyRollup("fixture-allowed", OfferCondition.NEW, "EUR",
                LocalDate.of(2026, 6, 10), "1.00", "1.00", "1.00", 1, 0);
        when(priceHistoryQueryPort.queryDaily(any())).thenReturn(new PriceHistoryPage<>(List.of(allowedNoLabel), Optional.empty()));

        B2bResponse<B2bPriceHistoryDto> result = customService.getProductPriceHistory(
                GTIN, "en", null, null, null, null, null, null, null, null, principal, request, response);

        assertThat(result.data().series()).isEmpty();
        assertThat(result.meta().billable()).isFalse();
    }

    private BillingCatalogProperties billingCatalog() {
        BillingCatalogProperties catalog = new BillingCatalogProperties();
        BillingCatalogProperties.Facet facet = new BillingCatalogProperties.Facet();
        facet.setPath("/api/v1/products/{gtin}/price/history");
        facet.setCredits(8);
        facet.setDoc("products/price-history");
        facet.setBillableWhen("has-history");
        catalog.setFacets(java.util.Map.of("product.price-history", facet));
        return catalog;
    }

    @Test
    void invalidGtinNeverReservesCredits() {
        when(gtinNormalizationService.normalize("bad")).thenThrow(new InvalidGtinException("bad"));

        assertThatThrownBy(() -> service.getProductPriceHistory("bad", "en", null, null, null, null, null, null,
                null, null, principal, request, response))
                .isInstanceOf(InvalidGtinException.class);
        verify(redisMeteringService, never()).reserveCredits(any(), anyLong());
    }

    @Test
    void unknownGtinReturns404AndRefundsReservation() throws Exception {
        when(productRepository.getByIdWithoutEmbedding(PRODUCT_ID)).thenThrow(new ResourceNotFoundException("missing"));

        assertThatThrownBy(() -> call(null, null, null, null, null, null, null, null))
                .isInstanceOf(org.open4goods.b2bapi.exception.ResourceNotFoundException.class);
        verify(redisMeteringService).refundCredits(ORG_ID, 8L);
        verify(creditLedgerService, never()).settleDebit(any(), any(), any(), any(), anyLong());
    }

    @Test
    void idempotentReplaySettlesZeroCostAndRefundsTheFullReservation() {
        DailyProviderRollup rollup = dailyRollup("merchant-feed", OfferCondition.NEW, "EUR",
                LocalDate.of(2026, 6, 10), "1.00", "1.00", "1.00", 1, 0);
        when(priceHistoryQueryPort.queryDaily(any())).thenReturn(new PriceHistoryPage<>(List.of(rollup), Optional.empty()));
        when(creditLedgerService.settleDebit(any(), any(), any(), any(), anyLong()))
                .thenReturn(new CreditSettlementResult(1000L, 0L, true));

        B2bResponse<B2bPriceHistoryDto> response = call(null, null, null, null, null, null, null, null);

        assertThat(response.meta().creditsConsumed()).isZero();
        verify(redisMeteringService).refundCredits(ORG_ID, 8L);
    }

    private B2bResponse<B2bPriceHistoryDto> call(
            String from, String to, String granularity, String provider, String condition, String currency,
            Integer limit, String cursor) {
        return service.getProductPriceHistory(GTIN, "en", from, to, granularity, provider, condition, currency,
                limit, cursor, principal, request, response);
    }

    private DailyProviderRollup dailyRollup(
            String providerId, OfferCondition condition, String currency, LocalDate day, String min, String max,
            String close, long offerCount, long changeCount) {
        DailyRollupKey key = new DailyRollupKey(new Gtin(GTIN), new SourceId(providerId), condition, Currency.getInstance(currency));
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DailyProviderRollup(key, day, new BigDecimal(min), new BigDecimal(max), new BigDecimal(close),
                offerCount, dayStart, dayStart.plusSeconds(3600), changeCount);
    }

    private PriceChangeEvent changeEvent(
            String providerId, OfferCondition condition, String currency, String amount,
            OfferAvailability availability, Instant observedAt) {
        OfferKey key = new OfferKey(new Gtin(GTIN), new SourceId(providerId), "offer-1");
        String eventId = "price-event:" + "a".repeat(64);
        return new PriceChangeEvent(eventId, key, PriceChangeKind.CHANGED, condition, Currency.getInstance(currency),
                new BigDecimal(amount), availability, observedAt, observedAt.plusNanos(1),
                new PayloadHash("SHA-256", "b".repeat(64)),
                new SourceUsagePolicyRef("merchant-feed", "1"));
    }
}
