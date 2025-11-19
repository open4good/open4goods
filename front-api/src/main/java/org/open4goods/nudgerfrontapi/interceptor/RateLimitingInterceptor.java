package org.open4goods.nudgerfrontapi.interceptor;

import java.time.Duration;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.nudgerfrontapi.config.properties.RateLimitProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that enforces per-user or per-IP request limits.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private static class RequestCounter {
        int count;
        long windowStart;
    }

    private final RateLimitProperties properties;
    private final Cache<String, RequestCounter> counters;

    public RateLimitingInterceptor(RateLimitProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    RateLimitingInterceptor(RateLimitProperties properties, Ticker ticker) {
        this.properties = properties;
        Duration ttl = Objects.requireNonNull(properties.getCounterTtl(),
                "front.rate-limit.counter-ttl must not be null");
        this.counters = Caffeine.newBuilder()
                .expireAfterAccess(ttl)
                .ticker(ticker)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = resolveKey(request);
        RequestCounter counter = counters.get(key, k -> {
            RequestCounter rc = new RequestCounter();
            rc.windowStart = System.currentTimeMillis();
            return rc;
        });
        int limit = key.startsWith("ip:") ? properties.getAnonymous() : properties.getAuthenticated();
        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStart >= RATE_LIMIT_WINDOW.toMillis()) {
                counter.count = 0;
                counter.windowStart = now;
            }
            if (counter.count < limit) {
                counter.count++;
                return true;
            }
        }
        response.setStatus(429);
        return false;
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "auth:" + auth.getName();
        }
        return "ip:" + getIp(request);
    }



    /**
     * TODO : mutualize with Iphelper.getIp
     * @param request
     * @return
     */
        private String getIp(final HttpServletRequest request) {

                String ip = request.getHeader("X-Real-Ip");

                if (StringUtils.isEmpty(ip)) {
                        ip = request.getHeader("X-Forwarded-For");
                }

                if (StringUtils.isEmpty(ip)) {
                        ip = request.getRemoteAddr();
                }

                return ip;

        }

    long getActiveCounterCount() {
        counters.cleanUp();
        return counters.estimatedSize();
    }

}
