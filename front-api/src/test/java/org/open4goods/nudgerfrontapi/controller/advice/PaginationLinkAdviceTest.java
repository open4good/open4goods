package org.open4goods.nudgerfrontapi.controller.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
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

    private final PaginationLinkAdvice advice = new PaginationLinkAdvice(new ApiProperties());

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

    @Test
    void shouldNotAdvertisePagesBeyondResultWindow() {
        // 100 per page: 100 reachable pages (0..99), although ES holds 1M documents.
        ProductSearchResponseDto body = new ProductSearchResponseDto(
                new PageDto<>(new PageMetaDto(98, 100, 1_000_000, 10_000), List.of()),
                List.of());

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/products");
        servletRequest.setScheme("http");
        servletRequest.setServerName("localhost");
        servletRequest.setServerPort(80);
        servletRequest.setQueryString("pageNumber=98&pageSize=100");

        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        ProductSearchResponseDto written = (ProductSearchResponseDto) advice.beforeBodyWrite(body, null,
                MediaType.APPLICATION_JSON, null, request, response);

        assertThat(written.products().page().totalPages()).isEqualTo(100);
        String links = String.join(",", response.getHeaders().get(HttpHeaders.LINK));
        assertThat(links).contains("pageNumber=99&pageSize=100>; rel=\"last\"").contains("rel=\"next\"");

        // On the last reachable page there is no next link.
        servletRequest.setQueryString("pageNumber=99&pageSize=100");
        MockHttpServletResponse lastResponse = new MockHttpServletResponse();
        ServletServerHttpResponse last = new ServletServerHttpResponse(lastResponse);
        advice.beforeBodyWrite(new ProductSearchResponseDto(
                new PageDto<>(new PageMetaDto(99, 100, 1_000_000, 10_000), List.of()), List.of()),
                null, MediaType.APPLICATION_JSON, null, request, last);
        assertThat(String.join(",", last.getHeaders().get(HttpHeaders.LINK))).doesNotContain("rel=\"next\"");
    }
}
