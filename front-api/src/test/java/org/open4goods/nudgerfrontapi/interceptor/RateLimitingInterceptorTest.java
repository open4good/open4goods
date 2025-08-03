package org.open4goods.nudgerfrontapi.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.RateLimitProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests for {@link RateLimitingInterceptor}.
 */
class RateLimitingInterceptorTest {

    private RateLimitingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setAnonymous(1);
        props.setAuthenticated(2);
        interceptor = new RateLimitingInterceptor(props);
    }

    @Test
    void anonymousRequestsAreLimited() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void authenticatedRequestsAreLimited() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "ROLE_USER"));
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        SecurityContextHolder.clearContext();
    }
}
