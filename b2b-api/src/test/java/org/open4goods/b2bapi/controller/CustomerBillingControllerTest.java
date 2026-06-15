package org.open4goods.b2bapi.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.billing.B2bBalanceResponseDto;
import org.open4goods.b2bapi.dto.billing.B2bInvoiceDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.service.BillingCatalogService;
import org.open4goods.b2bapi.service.CustomerBillingService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.open4goods.b2bapi.service.StripeBillingService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CustomerBillingControllerTest {

    private final BillingCatalogService billingCatalogService = mock(BillingCatalogService.class);
    private final StripeBillingService stripeBillingService = mock(StripeBillingService.class);
    private final CustomerBillingService customerBillingService = mock(CustomerBillingService.class);

    private MockMvc mockMvc;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        final CustomerBillingController controller = new CustomerBillingController(
                billingCatalogService,
                stripeBillingService,
                customerBillingService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Setup security context with mock authentication principal
        final DashboardPrincipal principal = new DashboardPrincipal(
                UUID.randomUUID(),
                orgId,
                "owner@example.com",
                false,
                OrganizationRole.OWNER
        );
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getBalanceReturnsCorrectBalanceAndBuckets() throws Exception {
        final B2bBalanceResponseDto.B2bBucketDetailDto bucket = new B2bBalanceResponseDto.B2bBucketDetailDto(
                UUID.randomUUID().toString(),
                "SUBSCRIPTION",
                12000,
                10000,
                Instant.parse("2026-07-15T18:00:00Z"),
                "starter"
        );
        final B2bBalanceResponseDto balanceResponse = new B2bBalanceResponseDto(10000L, List.of(bucket));

        when(customerBillingService.getBalance(orgId)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/customer/billing/balance")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditsRemaining").value(10000))
                .andExpect(jsonPath("$.buckets[0].kind").value("SUBSCRIPTION"))
                .andExpect(jsonPath("$.buckets[0].creditsRemaining").value(10000))
                .andExpect(jsonPath("$.buckets[0].catalogId").value("starter"));
    }

    @Test
    void getTransactionsReturnsLedger() throws Exception {
        final B2bTransactionDto tx = new B2bTransactionDto(
                UUID.randomUUID(),
                "DEBIT",
                -5,
                "product.price",
                "0885909950805",
                "pdreq_123",
                "Lookup",
                Instant.parse("2026-06-15T18:00:00Z")
        );

        when(customerBillingService.getTransactions(orgId, 50)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/customer/billing/transactions")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .param("limit", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEBIT"))
                .andExpect(jsonPath("$[0].credits").value(-5))
                .andExpect(jsonPath("$[0].facetId").value("product.price"));
    }

    @Test
    void getInvoicesReturnsList() throws Exception {
        final B2bInvoiceDto invoice = new B2bInvoiceDto(
                UUID.randomUUID(),
                "in_123",
                2900,
                "eur",
                "paid",
                "https://invoice.stripe.com/i/123",
                12000L,
                Instant.parse("2026-06-15T18:00:00Z")
        );

        when(customerBillingService.getInvoices(orgId)).thenReturn(List.of(invoice));

        mockMvc.perform(get("/api/v1/customer/billing/invoices")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stripeInvoiceId").value("in_123"))
                .andExpect(jsonPath("$[0].amountCents").value(2900))
                .andExpect(jsonPath("$[0].status").value("paid"));
    }
}
