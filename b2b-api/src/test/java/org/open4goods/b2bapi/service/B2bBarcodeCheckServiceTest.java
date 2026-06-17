package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.dto.barcode.check.BarcodeCheckResponse;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.BarcodeForensics;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class B2bBarcodeCheckServiceTest {

    private BarcodeForensicsService forensicsService;
    private ProductRepository productRepository;
    private RedisMeteringService redisMeteringService;
    private UsageStreamService usageStreamService;
    private B2bBarcodeCheckService service;

    private static final Instant NOW = Instant.parse("2026-06-16T10:00:00Z");
    private static final String VALID_BARCODE = "3017620422003";

    private static final BarcodeForensics VALID_FORENSICS = new BarcodeForensics(
            true, BarcodeType.GTIN_13, "301", "FR", Gs1Class.GTIN,
            null, null, "03017620422003", "3017620422003", 3);

    private static final BarcodeForensics INVALID_FORENSICS = new BarcodeForensics(
            false, BarcodeType.UNKNOWN, null, null, Gs1Class.UNKNOWN,
            null, null, null, null, 4);

    @BeforeEach
    void setUp() {
        forensicsService = mock(BarcodeForensicsService.class);
        productRepository = mock(ProductRepository.class);
        redisMeteringService = mock(RedisMeteringService.class);
        usageStreamService = mock(UsageStreamService.class);

        final B2bApiProperties props = new B2bApiProperties();
        props.getSecurity().setJwtSecret("my-super-secret-secure-jwt-key-with-32-bytes");
        props.setPublicBaseUrl(java.net.URI.create("https://api.test.com"));

        service = new B2bBarcodeCheckService(
                props,
                forensicsService,
                productRepository,
                redisMeteringService,
                usageStreamService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void checkPublicReturnsForensicsForValidBarcode() throws Exception {
        when(forensicsService.analyze(VALID_BARCODE)).thenReturn(VALID_FORENSICS);
        when(productRepository.getByIdWithoutEmbedding(3017620422003L))
                .thenThrow(new ResourceNotFoundException("not found"));

        final BarcodeCheckResponse response = service.checkPublic(VALID_BARCODE, "127.0.0.1");

        assertThat(response.barcode()).isEqualTo(VALID_BARCODE);
        assertThat(response.forensics().valid()).isTrue();
        assertThat(response.forensics().type()).isEqualTo(BarcodeType.GTIN_13);
        assertThat(response.forensics().gs1Prefix()).isEqualTo("301");
        assertThat(response.forensics().issuingCountryCode()).isEqualTo("FR");
        assertThat(response.forensics().gs1Class()).isEqualTo(Gs1Class.GTIN);
        assertThat(response.forensics().normalizedGtin13()).isEqualTo("3017620422003");
        assertThat(response.forensics().normalizedGtin14()).isEqualTo("03017620422003");
        assertThat(response.forensics().checkDigit()).isEqualTo(3);
        assertThat(response.product()).isNull();
    }

    @Test
    void checkPublicInvokesIpRateLimit() throws Exception {
        when(forensicsService.analyze(any())).thenReturn(VALID_FORENSICS);
        when(productRepository.getByIdWithoutEmbedding(anyLong()))
                .thenThrow(new ResourceNotFoundException("not found"));

        service.checkPublic(VALID_BARCODE, "10.0.0.1");

        verify(redisMeteringService, times(1)).checkRateLimitByIp("10.0.0.1");
    }

    @Test
    void checkPublicReturnsInvalidForensicsWhenChecksumFails() {
        when(forensicsService.analyze("9999999999999")).thenReturn(INVALID_FORENSICS);

        final BarcodeCheckResponse response = service.checkPublic("9999999999999", "127.0.0.1");

        assertThat(response.forensics().valid()).isFalse();
        assertThat(response.product()).isNull();
    }

    @Test
    void checkAuthenticatedEmitsUsageEvent() throws Exception {
        when(forensicsService.analyze(VALID_BARCODE)).thenReturn(VALID_FORENSICS);
        when(productRepository.getByIdWithoutEmbedding(anyLong()))
                .thenThrow(new ResourceNotFoundException("not found"));

        final UUID orgId = UUID.randomUUID();
        final UUID keyId = UUID.randomUUID();
        final ApiKeyPrincipal principal = new ApiKeyPrincipal(orgId, keyId);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.check(VALID_BARCODE, principal, request, response);

        verify(usageStreamService, times(1)).emit(any(UsageStreamEvent.class));
    }

    @Test
    void checkAuthenticatedSetsResponseHeaders() throws Exception {
        when(forensicsService.analyze(VALID_BARCODE)).thenReturn(VALID_FORENSICS);
        when(productRepository.getByIdWithoutEmbedding(anyLong()))
                .thenThrow(new ResourceNotFoundException("not found"));

        final ApiKeyPrincipal principal = new ApiKeyPrincipal(UUID.randomUUID(), UUID.randomUUID());
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.check(VALID_BARCODE, principal, request, response);

        assertThat(response.getHeader("X-Credits-Consumed")).isEqualTo("0");
        assertThat(response.getHeader("X-Request-Id")).isNotNull();
    }

    @Test
    void checkAuthenticatedInvokesKeyRateLimit() throws Exception {
        when(forensicsService.analyze(any())).thenReturn(VALID_FORENSICS);
        when(productRepository.getByIdWithoutEmbedding(anyLong()))
                .thenThrow(new ResourceNotFoundException("not found"));

        final UUID keyId = UUID.randomUUID();
        final ApiKeyPrincipal principal = new ApiKeyPrincipal(UUID.randomUUID(), keyId);

        service.check(VALID_BARCODE, principal, new MockHttpServletRequest(), new MockHttpServletResponse());

        verify(redisMeteringService, times(1)).checkRateLimit(keyId);
    }

    @Test
    void productTeaserIsNullWhenRepositoryIsAbsent() {
        // Service without productRepository (null)
        final B2bApiProperties props = new B2bApiProperties();
        props.getSecurity().setJwtSecret("my-super-secret-secure-jwt-key-with-32-bytes");
        props.setPublicBaseUrl(java.net.URI.create("https://api.test.com"));

        final B2bBarcodeCheckService noRepoService = new B2bBarcodeCheckService(
                props, forensicsService, null, redisMeteringService, usageStreamService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        when(forensicsService.analyze(VALID_BARCODE)).thenReturn(VALID_FORENSICS);

        final BarcodeCheckResponse response = noRepoService.checkPublic(VALID_BARCODE, "127.0.0.1");

        assertThat(response.forensics().valid()).isTrue();
        assertThat(response.product()).isNull();
    }

    @Test
    void productTeaserIsPopulatedWhenProductFound() throws Exception {
        when(forensicsService.analyze(VALID_BARCODE)).thenReturn(VALID_FORENSICS);

        final Product product = new Product();
        product.setOffersCount(5);
        when(productRepository.getByIdWithoutEmbedding(3017620422003L)).thenReturn(product);

        final BarcodeCheckResponse response = service.checkPublic(VALID_BARCODE, "127.0.0.1");

        assertThat(response.product()).isNotNull();
        assertThat(response.product().gtin()).isEqualTo("3017620422003");
        assertThat(response.product().offersCount()).isEqualTo(5);
        assertThat(response.product().productUrl()).contains("nudger.fr");
    }
}
