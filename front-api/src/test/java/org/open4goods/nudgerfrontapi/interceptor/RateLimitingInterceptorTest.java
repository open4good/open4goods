package org.open4goods.nudgerfrontapi.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.RateLimitProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.benmanes.caffeine.cache.Ticker;

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

    @Test
    void multipleIpsAreTrackedIndependently() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(requestForIp("10.0.0.1"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestForIp("10.0.0.1"), response, new Object())).isFalse();

        MockHttpServletResponse otherResponse = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(requestForIp("10.0.0.2"), otherResponse, new Object())).isTrue();
        assertThat(interceptor.preHandle(requestForIp("10.0.0.2"), otherResponse, new Object())).isFalse();

        assertThat(interceptor.getActiveCounterCount()).isEqualTo(2);
    }

    @Test
    void countersAreEvictedAfterConfiguredTtl() {
        RateLimitProperties props = new RateLimitProperties();
        props.setAnonymous(1);
        props.setAuthenticated(2);
        props.setCounterTtl(Duration.ofSeconds(2));
        MutableTicker ticker = new MutableTicker();
        RateLimitingInterceptor shortLivedInterceptor = new RateLimitingInterceptor(props, ticker);

        for (int i = 0; i < 5; i++) {
            assertThat(shortLivedInterceptor.preHandle(requestForIp("10.0.0." + i), new MockHttpServletResponse(), new Object())).isTrue();
        }
        assertThat(shortLivedInterceptor.getActiveCounterCount()).isEqualTo(5);

        ticker.advance(Duration.ofSeconds(3));
        assertThat(shortLivedInterceptor.getActiveCounterCount()).isZero();

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = requestForIp("10.0.0.1");
        assertThat(shortLivedInterceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(shortLivedInterceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    private static MockHttpServletRequest requestForIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }

    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
