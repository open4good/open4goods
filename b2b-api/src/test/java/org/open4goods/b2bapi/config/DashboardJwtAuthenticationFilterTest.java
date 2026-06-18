package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.b2bapi.service.AuthTokenResolver;
import org.open4goods.b2bapi.service.DashboardAuthenticationService;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for dashboard JWT filter path selection.
 */
class DashboardJwtAuthenticationFilterTest {

    @Test
    void skipsPublicActuatorEndpointsUsedBySpringBootAdmin() {
        final DashboardJwtAuthenticationFilter filter = new DashboardJwtAuthenticationFilter(
                Mockito.mock(AuthTokenResolver.class),
                Mockito.mock(DashboardAuthenticationService.class));

        assertThat(filter.shouldNotFilter(request("/actuator/health"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/actuator/info"))).isTrue();
    }

    @Test
    void filtersAuthenticatedDashboardEndpoints() {
        final DashboardJwtAuthenticationFilter filter = new DashboardJwtAuthenticationFilter(
                Mockito.mock(AuthTokenResolver.class),
                Mockito.mock(DashboardAuthenticationService.class));

        assertThat(filter.shouldNotFilter(request("/api/v1/customer/api-keys"))).isFalse();
    }

    private MockHttpServletRequest request(final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
