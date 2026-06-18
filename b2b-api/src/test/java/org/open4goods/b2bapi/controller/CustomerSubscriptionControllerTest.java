package org.open4goods.b2bapi.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.billing.B2bSubscriptionDto;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.service.CustomerBillingService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CustomerSubscriptionControllerTest {

    private final CustomerBillingService customerBillingService = mock(CustomerBillingService.class);
    private MockMvc mockMvc;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        final CustomerSubscriptionController controller = new CustomerSubscriptionController(customerBillingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSubscriptionsReturnsList() throws Exception {
        final B2bSubscriptionDto sub = new B2bSubscriptionDto(
                UUID.randomUUID(),
                "sub_123",
                "starter",
                "active",
                Instant.parse("2026-07-15T18:00:00Z"),
                null,
                Instant.parse("2026-06-15T18:00:00Z")
        );

        when(customerBillingService.getSubscriptions(orgId)).thenReturn(List.of(sub));

        mockMvc.perform(get("/api/v1/customer/subscriptions")
                        .principal(SecurityContextHolder.getContext().getAuthentication())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stripeSubscriptionId").value("sub_123"))
                .andExpect(jsonPath("$[0].catalogId").value("starter"))
                .andExpect(jsonPath("$[0].status").value("active"));
    }
}
