package org.open4goods.b2bapi.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApiCallCountingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiCallCountingInterceptor.class);
    private final AtomicLong callCount = new AtomicLong(0);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long n = callCount.incrementAndGet();
        log.info("API Call #{} - {} {}", n, request.getMethod(), request.getRequestURI());
        response.setHeader("X-Total-Calls", String.valueOf(n));
        return true;
    }
}
