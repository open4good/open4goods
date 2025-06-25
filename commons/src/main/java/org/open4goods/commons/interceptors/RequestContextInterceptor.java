package org.open4goods.commons.interceptors;

import java.util.Enumeration;

import org.open4goods.commons.model.dto.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor populating the {@link RequestContext} with request headers.
 */
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestContextInterceptor.class);

    private final RequestContext requestContext;

    public RequestContextInterceptor(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestContext.clear();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getHeader(name);
                requestContext.getHeaders().put(name, value);
            }
        }
        logger.debug("Stored {} headers in RequestContext", requestContext.getHeaders().size());
        return true;
    }
}
