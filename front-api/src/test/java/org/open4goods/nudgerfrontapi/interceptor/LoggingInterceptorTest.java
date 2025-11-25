package org.open4goods.nudgerfrontapi.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link LoggingInterceptor}.
 */
class LoggingInterceptorTest {

    @Test
    void preHandleIncrementsRequestCounter() throws Exception {
        LoggingInterceptor interceptor = new LoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void afterCompletionIncrementsErrorCounter() throws Exception {
        LoggingInterceptor interceptor = new LoggingInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(response.getStatus()).isEqualTo(500);
    }
}

