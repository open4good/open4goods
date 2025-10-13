package org.open4goods.nudgerfrontapi.controller.advice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Advice that transforms {@link Page} responses to include RFC 8288 pagination
 * links and metadata.
 */
@RestControllerAdvice
public class PaginationLinkAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof ProductSearchResponseDto searchResponse) {
            addPaginationHeaders(searchResponse.products().page(), request, response);
            return body;
        }

        if (!(body instanceof Page<?> page)) {
            return body;
        }

        PageMetaDto meta = new PageMetaDto(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        addPaginationHeaders(meta, request, response);
        return new PageDto<>(meta, page.getContent());
    }

    private void addPaginationHeaders(PageMetaDto meta, ServerHttpRequest request, ServerHttpResponse response) {
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(servletRequest);

        int pageNumber = meta.number();
        int pageSize = meta.size();
        long totalPages = meta.totalPages();

        List<String> links = new ArrayList<>();
        links.add(buildLink(builder, 0, pageSize, "first"));
        if (totalPages > 0) {
            links.add(buildLink(builder, Math.max(0, totalPages - 1), pageSize, "last"));
        }
        if (pageNumber > 0) {
            links.add(buildLink(builder, pageNumber - 1, pageSize, "prev"));
        }
        if (pageNumber + 1 < totalPages) {
            links.add(buildLink(builder, pageNumber + 1, pageSize, "next"));
        }
        if (!links.isEmpty()) {
            response.getHeaders().add(HttpHeaders.LINK, String.join(", ", links));
        }
    }

    private String buildLink(UriComponentsBuilder builder, long number, int size, String rel) {
        String uri = builder.replaceQueryParam("pageNumber", number)
                .replaceQueryParam("pageSize", size)
                .toUriString();
        return "<" + uri + ">; rel=\"" + rel + "\"";
    }
}
