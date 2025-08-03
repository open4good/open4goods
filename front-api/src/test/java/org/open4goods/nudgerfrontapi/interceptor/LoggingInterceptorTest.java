package org.open4goods.nudgerfrontapi.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link LoggingInterceptor}.
 */
class LoggingInterceptorTest {

    @Test
    void preHandleIncrementsRequestCounter() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoggingInterceptor interceptor = new LoggingInterceptor(registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        Counter counter = registry.find("front.api.requests").tag("uri", "/test").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void afterCompletionIncrementsErrorCounter() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LoggingInterceptor interceptor = new LoggingInterceptor(registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, new Object(), null);

        Counter counter = registry.find("front.api.errors").tag("uri", "/test").tag("status", "500").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }
}

