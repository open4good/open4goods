package org.open4goods.b2bapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.playground.PlaygroundBarcodeRequest;
import org.open4goods.b2bapi.dto.playground.PlaygroundRequest;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderResponse;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderMeta;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeDimensions;
import org.open4goods.b2bapi.dto.product.B2bCoverageMeta;
import org.open4goods.b2bapi.dto.product.B2bFacetMeta;
import org.open4goods.b2bapi.dto.product.B2bMeta;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.InvalidBarcodeException;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.open4goods.b2bapi.service.B2bBarcodeCheckService;
import org.open4goods.b2bapi.service.B2bBarcodeService;
import org.open4goods.b2bapi.service.B2bProductService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CustomerPlaygroundControllerTest {

    private final ApiKeyRepository apiKeyRepository = mock(ApiKeyRepository.class);
    private final B2bProductService b2bProductService = mock(B2bProductService.class);
    private final B2bBarcodeService b2bBarcodeService = mock(B2bBarcodeService.class);
    private final B2bBarcodeCheckService b2bBarcodeCheckService = mock(B2bBarcodeCheckService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private final UUID orgId = UUID.randomUUID();
    private final UUID apiKeyId = UUID.randomUUID();
    private ApiKey apiKey;

    @BeforeEach
    void setUp() {
        final CustomerPlaygroundController controller = new CustomerPlaygroundController(apiKeyRepository, b2bProductService, b2bBarcodeService, b2bBarcodeCheckService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        final DashboardPrincipal principal = new DashboardPrincipal(
                UUID.randomUUID(),
                orgId,
                "developer@example.com",
                false,
                OrganizationRole.DEVELOPER
        );
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Organization organization = new Organization("Workspace", "workspace");
        final User user = new User("user@example.com", org.open4goods.b2bapi.model.OidcProvider.GOOGLE, "subject");
        apiKey = new ApiKey(organization, user, "Test Key", "abcd", "hash123");
        apiKey.setId(apiKeyId);
    }

    @Test
    void proxyProductPriceSuccess() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));

        final B2bPriceDto priceDto = new B2bPriceDto(
                "0885909950805",
                "Product Name",
                "Brand",
                "Model",
                1,
                1,
                null,
                null,
                null,
                Map.of(),
                null,
                null,
                null,
                null
        );
        final B2bFacetMeta facetMeta = new B2bFacetMeta("product.price", 5L, true, true);
        final B2bCoverageMeta coverageMeta = new B2bCoverageMeta("product.price", true);
        final B2bMeta meta = new B2bMeta(
                "pdreq_123",
                Instant.now(),
                "en",
                5L,
                2495L,
                true,
                30,
                42L,
                List.of(facetMeta),
                List.of(coverageMeta)
        );
        final B2bResponse<B2bPriceDto> serviceResponse = new B2bResponse<>(priceDto, meta);

        when(b2bProductService.getProductPrice(
                eq("0885909950805"),
                eq("en"),
                eq(new ApiKeyPrincipal(orgId, apiKeyId)),
                any(),
                any()
        )).thenReturn(serviceResponse);

        final PlaygroundRequest requestBody = new PlaygroundRequest(apiKeyId, "0885909950805", "en");

        mockMvc.perform(post("/api/v1/customer/playground/products/price")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.method").value("GET"))
                .andExpect(jsonPath("$.request.path").value("/api/v1/products/0885909950805/price?language=en"))
                .andExpect(jsonPath("$.request.headers.Authorization").value("Bearer pdapi_abcd...masked"))
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.metering.billable").value(true))
                .andExpect(jsonPath("$.metering.creditsConsumed").value(5))
                .andExpect(jsonPath("$.metering.creditsRemaining").value(2495))
                .andExpect(jsonPath("$.metering.reason").value("fresh-offer"));
    }

    @Test
    void proxyProductPriceRevokedKeyReturns401() throws Exception {
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));

        final PlaygroundRequest requestBody = new PlaygroundRequest(apiKeyId, "0885909950805", "en");

        mockMvc.perform(post("/api/v1/customer/playground/products/price")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(401))
                .andExpect(jsonPath("$.metering.billable").value(false))
                .andExpect(jsonPath("$.metering.reason").value("invalid-credentials"));
    }

    @Test
    void proxyProductPriceInvalidGtinReturns400() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));
        when(b2bProductService.getProductPrice(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidGtinException("Invalid GTIN checksum."));

        final PlaygroundRequest requestBody = new PlaygroundRequest(apiKeyId, "12345", "en");

        mockMvc.perform(post("/api/v1/customer/playground/products/price")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(400))
                .andExpect(jsonPath("$.metering.reason").value("invalid-gtin"));
    }

    @Test
    void proxyProductPriceInsufficientCreditsReturns402() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));
        when(b2bProductService.getProductPrice(any(), any(), any(), any(), any()))
                .thenThrow(new InsufficientCreditsException("Insufficient credits."));

        final PlaygroundRequest requestBody = new PlaygroundRequest(apiKeyId, "0885909950805", "en");

        mockMvc.perform(post("/api/v1/customer/playground/products/price")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(402))
                .andExpect(jsonPath("$.metering.reason").value("insufficient-credits"));
    }

    @Test
    void proxyProductPriceNotFoundReturns404() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));
        when(b2bProductService.getProductPrice(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Product not found."));

        final PlaygroundRequest requestBody = new PlaygroundRequest(apiKeyId, "0885909950805", "en");

        mockMvc.perform(post("/api/v1/customer/playground/products/price")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(404))
                .andExpect(jsonPath("$.metering.reason").value("not-found"));
    }

    @Test
    void proxyBarcodeRenderSuccess() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));

        final B2bBarcodeRenderRequest renderRequest = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true, null, null
        );
        final B2bBarcodeRenderMeta renderMeta = new B2bBarcodeRenderMeta("pdreq_999", true, 1L);
        final B2bBarcodeDimensions dimensions = new B2bBarcodeDimensions(200, 100, 300);
        final B2bBarcodeRenderResponse renderResponse = new B2bBarcodeRenderResponse(
                renderMeta,
                "https://product-data-api.com/api/v1/barcodes/assets/tok_abc",
                Instant.now().plusSeconds(3600),
                dimensions,
                "image/png",
                List.of(),
                "sha256_abc"
        );

        when(b2bBarcodeService.renderBarcode(
                any(),
                eq(new ApiKeyPrincipal(orgId, apiKeyId)),
                any(),
                any()
        )).thenReturn(renderResponse);

        final PlaygroundBarcodeRequest requestBody = new PlaygroundBarcodeRequest(apiKeyId, renderRequest);

        mockMvc.perform(post("/api/v1/customer/playground/barcodes/render")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.method").value("POST"))
                .andExpect(jsonPath("$.request.path").value("/api/v1/barcodes/render"))
                .andExpect(jsonPath("$.request.headers.Authorization").value("Bearer pdapi_abcd...masked"))
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.body.assetUrl").value("https://product-data-api.com/api/v1/barcodes/assets/tok_abc"))
                .andExpect(jsonPath("$.metering.billable").value(true))
                .andExpect(jsonPath("$.metering.creditsConsumed").value(1))
                .andExpect(jsonPath("$.metering.reason").value("success"));
    }

    @Test
    void proxyBarcodeRenderInvalidBarcodeReturns400() throws Exception {
        when(apiKeyRepository.findByIdAndOrganizationId(apiKeyId, orgId)).thenReturn(Optional.of(apiKey));
        when(b2bBarcodeService.renderBarcode(any(), any(), any(), any()))
                .thenThrow(new InvalidBarcodeException("Barcode type is required"));

        // Use a valid structure to pass Jackson/validation, but trigger mock exception
        final B2bBarcodeRenderRequest renderRequest = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true, null, null
        );
        final PlaygroundBarcodeRequest requestBody = new PlaygroundBarcodeRequest(apiKeyId, renderRequest);

        mockMvc.perform(post("/api/v1/customer/playground/barcodes/render")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(400))
                .andExpect(jsonPath("$.metering.reason").value("invalid-input"));
    }
}
