package org.open4goods.nudgerfrontapi.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

/**
 * Aspect implementing rate limiting using Bucket4j.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = pjp.getSignature().toLongString();
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(rateLimit));
        if (bucket.tryConsume(1)) {
            return pjp.proceed();
        }
        throw new RateLimitException("Too many requests");
    }

    private Bucket createBucket(RateLimit rl) {
        long refillTokens = rl.refillTokens() > 0 ? rl.refillTokens() : rl.capacity();
        Refill refill = Refill.greedy(refillTokens, Duration.ofSeconds(rl.refillSeconds()));
        Bandwidth limit = Bandwidth.classic(rl.capacity(), refill);
        return Bucket4j.builder().addLimit(limit).build();
    }
}
