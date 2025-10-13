package org.open4goods.nudgerfrontapi.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link PaginationLinkAdvice}.
 */
class PaginationLinkAdviceTest {

    private final PaginationLinkAdvice advice = new PaginationLinkAdvice();

    @Test
    void shouldAddPaginationLinksForSearchResponse() {
        ProductSearchResponseDto body = new ProductSearchResponseDto(
                new PageDto<>(new PageMetaDto(1, 20, 100, 5), List.of()),
                List.of());

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/products");
        servletRequest.setScheme("http");
        servletRequest.setServerName("localhost");
        servletRequest.setServerPort(80);
        servletRequest.setQueryString("pageNumber=1&pageSize=20");

        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        Object written = advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON, null, request, response);

        assertThat(written).isSameAs(body);

        List<String> linkHeaders = response.getHeaders().get(HttpHeaders.LINK);
        assertThat(linkHeaders).isNotNull();
        assertThat(String.join(",", linkHeaders))
                .contains("rel=\"first\"")
                .contains("rel=\"last\"")
                .contains("rel=\"prev\"")
                .contains("rel=\"next\"");
    }
}
