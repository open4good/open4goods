package org.open4goods.nudgerfrontapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link SharedTokenFilter}.
 */
class SharedTokenFilterTest {

    @Test
    void shouldRejectWhenTokenMissing() throws ServletException, IOException {
        SecurityProperties props = new SecurityProperties();
        props.setSharedToken("secret");
        SharedTokenFilter filter = new SharedTokenFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldAllowWhenTokenMatches() throws ServletException, IOException {
        SecurityProperties props = new SecurityProperties();
        props.setSharedToken("secret");
        SharedTokenFilter filter = new SharedTokenFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/protected");
        request.addHeader(SharedTokenFilter.HEADER_NAME, "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
