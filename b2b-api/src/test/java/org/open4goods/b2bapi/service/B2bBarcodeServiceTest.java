package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeMetadata;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeOptions;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;
import org.open4goods.b2bapi.exception.InvalidBarcodeException;
import org.open4goods.b2bapi.repository.BarcodeAssetRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;

class B2bBarcodeServiceTest {

    private B2bApiProperties properties;
    private BillingCatalogProperties billingCatalogProperties;
    private RedisMeteringService redisMeteringService;
    private CreditLedgerService creditLedgerService;
    private UsageStreamService usageStreamService;
    private BarcodeAssetRepository barcodeAssetRepository;
    private CreditBucketRepository creditBucketRepository;
    private Clock clock;
    private B2bBarcodeService barcodeService;

    private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");

    @BeforeEach
    void setUp() {
        properties = new B2bApiProperties();
        properties.getSecurity().setJwtSecret("my-super-secret-secure-jwt-key-with-32-bytes");
        properties.setPublicBaseUrl(java.net.URI.create("https://api.test.com"));

        billingCatalogProperties = mock(BillingCatalogProperties.class);
        redisMeteringService = mock(RedisMeteringService.class);
        creditLedgerService = mock(CreditLedgerService.class);
        usageStreamService = mock(UsageStreamService.class);
        barcodeAssetRepository = mock(BarcodeAssetRepository.class);
        creditBucketRepository = mock(CreditBucketRepository.class);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);

        barcodeService = new B2bBarcodeService(
                properties,
                billingCatalogProperties,
                redisMeteringService,
                creditLedgerService,
                usageStreamService,
                barcodeAssetRepository,
                creditBucketRepository,
                clock
        );
    }

    @Test
    void generatesAndVerifiesSignedToken() {
        B2bBarcodeRenderRequest req = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"),
                new B2bBarcodeMetadata("Copy", "Auth", "Desc")
        );

        String token = barcodeService.generateSignedToken(req);
        assertThat(token).isNotEmpty();

        B2bBarcodeRenderRequest parsed = barcodeService.parseSignedToken(token);
        assertThat(parsed.type()).isEqualTo(req.type());
        assertThat(parsed.data()).isEqualTo(req.data());
        assertThat(parsed.format()).isEqualTo(req.format());
        assertThat(parsed.width()).isEqualTo(req.width());
        assertThat(parsed.height()).isEqualTo(req.height());
        assertThat(parsed.foreground()).isEqualTo(req.foreground());
        assertThat(parsed.background()).isEqualTo(req.background());
        assertThat(parsed.rotation()).isEqualTo(req.rotation());
        assertThat(parsed.showText()).isEqualTo(req.showText());
        assertThat(parsed.quietZone()).isEqualTo(req.quietZone());
        assertThat(parsed.options().dpi()).isEqualTo(req.options().dpi());
        assertThat(parsed.options().moduleWidthMm()).isEqualTo(req.options().moduleWidthMm());
        assertThat(parsed.metadata().copyright()).isEqualTo(req.metadata().copyright());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ean8", "ean13", "upca", "upce", "code128", "gs1-128", "itf14", "qr", "aztec", "datamatrix", "pdf417"})
    void generatesBarcodeBytesForFormats(String type) {
        String data = "1234567";
        if (type.equals("ean13")) {
            data = "4006381333931";
        } else if (type.equals("upca")) {
            data = "012345678905";
        } else if (type.equals("ean8")) {
            data = "40063812";
        } else if (type.equals("upce")) {
            data = "01234565";
        } else if (type.equals("itf14")) {
            data = "12345678901231";
        } else if (type.equals("gs1-128")) {
            data = "0104006381333931";
        }

        B2bBarcodeRenderRequest req = new B2bBarcodeRenderRequest(
                type, data, "png", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"),
                new B2bBarcodeMetadata("Copy", "Auth", "Desc")
        );

        byte[] bytes = barcodeService.generateBarcodeBytes(req);
        assertThat(bytes).isNotEmpty();

        B2bBarcodeRenderRequest reqSvg = new B2bBarcodeRenderRequest(
                type, data, "svg", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"),
                new B2bBarcodeMetadata("Copy", "Auth", "Desc")
        );
        byte[] svgBytes = barcodeService.generateBarcodeBytes(reqSvg);
        assertThat(svgBytes).isNotEmpty();
        String svgStr = new String(svgBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(svgStr).contains("<svg").contains("Copy").contains("Auth").contains("Desc");
    }

    @Test
    void rejectsInvalidEanChecksum() {
        B2bBarcodeRenderRequest req = new B2bBarcodeRenderRequest(
                "ean13", "4006381333930", "png", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"), null
        );
        assertThatThrownBy(() -> barcodeService.generateBarcodeBytes(req))
                .isInstanceOf(InvalidBarcodeException.class);
    }

    @Test
    void pruneExpiredAssetsDeletesExpired() {
        barcodeService.pruneExpiredAssets();
        verify(barcodeAssetRepository, times(1)).deleteExpired(NOW);
    }
}
