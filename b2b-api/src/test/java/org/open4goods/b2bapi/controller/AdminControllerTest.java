package org.open4goods.b2bapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.ApiKeyDto;
import org.open4goods.b2bapi.dto.admin.AdminAuditEventDto;
import org.open4goods.b2bapi.dto.admin.AdminCreditGrantResponseDto;
import org.open4goods.b2bapi.dto.admin.AdminManualGrantRequest;
import org.open4goods.b2bapi.dto.admin.AdminOrganizationDto;
import org.open4goods.b2bapi.dto.admin.AdminUsageEventDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.service.AdminService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminControllerTest {

    private final AdminService adminService = mock(AdminService.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private MockMvc mockMvc;
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        final AdminController controller = new AdminController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Setup platform admin principal in context
        final DashboardPrincipal principal = new DashboardPrincipal(
                adminUserId,
                organizationId,
                "admin@platform.com",
                true, // platformAdmin = true
                OrganizationRole.OWNER
        );
        auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listOrganizationsReturnsList() throws Exception {
        final AdminOrganizationDto orgDto = new AdminOrganizationDto(
                organizationId,
                "Platform Org",
                "platform-org",
                "billing@platform.com",
                "en",
                OrganizationStatus.ACTIVE,
                true,
                5000L,
                Instant.parse("2026-06-15T18:00:00Z"),
                Instant.parse("2026-06-15T18:30:00Z")
        );

        when(adminService.listOrganizations()).thenReturn(List.of(orgDto));

        mockMvc.perform(get("/api/v1/admin/organizations")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(organizationId.toString()))
                .andExpect(jsonPath("$[0].name").value("Platform Org"))
                .andExpect(jsonPath("$[0].creditBalance").value(5000));
    }

    @Test
    void getOrganizationReturnsDetails() throws Exception {
        final AdminOrganizationDto orgDto = new AdminOrganizationDto(
                organizationId,
                "Platform Org",
                "platform-org",
                "billing@platform.com",
                "en",
                OrganizationStatus.ACTIVE,
                true,
                5000L,
                Instant.parse("2026-06-15T18:00:00Z"),
                Instant.parse("2026-06-15T18:30:00Z")
        );

        when(adminService.getOrganization(organizationId)).thenReturn(orgDto);

        mockMvc.perform(get("/api/v1/admin/organizations/{id}", organizationId)
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId.toString()))
                .andExpect(jsonPath("$.name").value("Platform Org"))
                .andExpect(jsonPath("$.creditBalance").value(5000));
    }

    @Test
    void getOrganizationTransactionsReturnsList() throws Exception {
        final B2bTransactionDto txDto = new B2bTransactionDto(
                UUID.randomUUID(),
                "GRANT",
                1000,
                null,
                null,
                null,
                "Manual grant",
                Instant.parse("2026-06-15T18:00:00Z")
        );

        when(adminService.getOrganizationTransactions(organizationId, 10)).thenReturn(List.of(txDto));

        mockMvc.perform(get("/api/v1/admin/organizations/{id}/transactions", organizationId)
                        .principal(auth)
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GRANT"))
                .andExpect(jsonPath("$[0].credits").value(1000));
    }

    @Test
    void grantManualCreditsExecutesGrantAndReturnsResult() throws Exception {
        final AdminManualGrantRequest request = new AdminManualGrantRequest(
                5000L,
                "Goodwill credits",
                Instant.parse("2026-12-31T23:59:59Z")
        );

        final AdminCreditGrantResponseDto responseDto = new AdminCreditGrantResponseDto(
                UUID.randomUUID(),
                5000L,
                7500L
        );

        when(adminService.grantManualCredits(eq(organizationId), any(DashboardPrincipal.class), any(AdminManualGrantRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/admin/organizations/{id}/credits/grants", organizationId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsGranted").value(5000))
                .andExpect(jsonPath("$.durableBalance").value(7500));
    }

    @Test
    void listApiKeysReturnsAllKeys() throws Exception {
        final ApiKeyDto apiKeyDto = new ApiKeyDto(
                UUID.randomUUID(),
                "Test key",
                "pdapi_abc",
                ApiKeyStatus.ACTIVE,
                UUID.randomUUID(),
                Instant.parse("2026-06-15T18:00:00Z"),
                null,
                null
        );

        when(adminService.listApiKeys()).thenReturn(List.of(apiKeyDto));

        mockMvc.perform(get("/api/v1/admin/api-keys")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test key"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void revokeApiKeyTriggersRevocation() throws Exception {
        final UUID apiKeyId = UUID.randomUUID();
        final ApiKeyDto revokedKey = new ApiKeyDto(
                apiKeyId,
                "Test key",
                "pdapi_abc",
                ApiKeyStatus.REVOKED,
                UUID.randomUUID(),
                Instant.parse("2026-06-15T18:00:00Z"),
                null,
                Instant.parse("2026-06-15T18:45:00Z")
        );

        when(adminService.revokeApiKey(any(DashboardPrincipal.class), eq(apiKeyId))).thenReturn(revokedKey);

        mockMvc.perform(post("/api/v1/admin/api-keys/{id}/revoke", apiKeyId)
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void listUsageReturnsUsageHistory() throws Exception {
        final AdminUsageEventDto usageDto = new AdminUsageEventDto(
                UUID.randomUUID(),
                organizationId,
                "Platform Org",
                UUID.randomUUID(),
                "pdapi_abc",
                "product.price",
                "0885909950805",
                "pdreq_123",
                (short) 200,
                true,
                5,
                null,
                42,
                Instant.parse("2026-06-15T18:22:25Z")
        );

        when(adminService.listUsage(10)).thenReturn(List.of(usageDto));

        mockMvc.perform(get("/api/v1/admin/usage")
                        .principal(auth)
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].httpStatus").value(200))
                .andExpect(jsonPath("$[0].creditsConsumed").value(5))
                .andExpect(jsonPath("$[0].facetId").value("product.price"));
    }

    @Test
    void listAuditEventsReturnsAuditLog() throws Exception {
        final AdminAuditEventDto auditDto = new AdminAuditEventDto(
                UUID.randomUUID(),
                adminUserId,
                "admin@platform.com",
                "API_KEY_REVOKE",
                organizationId,
                "Platform Org",
                UUID.randomUUID().toString(),
                Map.of("keyPrefix", "pdapi_abc"),
                Instant.parse("2026-06-15T18:45:00Z")
        );

        when(adminService.listAuditEvents(10)).thenReturn(List.of(auditDto));

        mockMvc.perform(get("/api/v1/admin/audit")
                        .principal(auth)
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("API_KEY_REVOKE"))
                .andExpect(jsonPath("$[0].actorUserEmail").value("admin@platform.com"));
    }
}
