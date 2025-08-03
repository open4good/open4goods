package org.open4goods.nudgerfrontapi.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor logging request entry/exit and publishing simple metrics.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

    private final MeterRegistry meterRegistry;

    public LoggingInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String ip = request.getRemoteAddr();
        String user = resolveUser();
        LOG.info("Entering endpoint={} ip={} user={}", uri, ip, user);
        Counter.builder("front.api.requests")
                .tag("uri", uri)
                .register(meterRegistry)
                .increment();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String uri = request.getRequestURI();
        String ip = request.getRemoteAddr();
        String user = resolveUser();
        int status = response.getStatus();
        LOG.info("Exiting endpoint={} status={} ip={} user={}", uri, status, ip, user);
        if (status >= 400) {
            Counter.builder("front.api.errors")
                    .tag("uri", uri)
                    .tag("status", String.valueOf(status))
                    .register(meterRegistry)
                    .increment();
        }
    }

    private String resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Object subject = token.getToken().getClaims().get("sub");
            if (subject != null) {
                return subject.toString();
            }
        }
        return "anonymous";
    }
}

