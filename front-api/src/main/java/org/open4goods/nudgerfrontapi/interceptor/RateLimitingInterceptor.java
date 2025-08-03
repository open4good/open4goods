package org.open4goods.nudgerfrontapi.interceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.commons.helper.IpHelper;
import org.open4goods.nudgerfrontapi.config.RateLimitProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that enforces per-user or per-IP request limits.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static class RequestCounter {
        int count;
        long windowStart;
    }

    private final RateLimitProperties properties;
    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    public RateLimitingInterceptor(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = resolveKey(request);
        RequestCounter counter = counters.computeIfAbsent(key, k -> {
            RequestCounter rc = new RequestCounter();
            rc.windowStart = System.currentTimeMillis();
            return rc;
        });
        int limit = key.startsWith("ip:") ? properties.getAnonymous() : properties.getAuthenticated();
        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStart >= 60_000) {
                counter.count = 0;
                counter.windowStart = now;
            }
            if (counter.count < limit) {
                counter.count++;
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        return false;
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "auth:" + auth.getName();
        }
        return "ip:" + IpHelper.getIp(request);
    }
}
