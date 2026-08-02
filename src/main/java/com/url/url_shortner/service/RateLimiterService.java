package com.url.url_shortner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-key token-bucket rate limiter.
 *
 * Each key (e.g. a username) gets a bucket that starts full with {@code capacity}
 * tokens and refills continuously at {@code capacity / windowSeconds} tokens per
 * second. Each allowed request consumes one token; when the bucket is empty the
 * request is rejected. This gives a burst allowance of {@code capacity} plus a
 * steady sustained rate — the classic token-bucket behaviour.
 *
 * State is held in memory, which is fine for a single instance. A multi-instance
 * deployment would back this with Redis instead.
 */
@Service
public class RateLimiterService {

    private final int capacity;
    private final double refillTokensPerSecond;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${ratelimit.shorten.capacity:10}") int capacity,
            @Value("${ratelimit.shorten.window-seconds:60}") int windowSeconds) {
        this.capacity = capacity;
        this.refillTokensPerSecond = (double) capacity / windowSeconds;
    }

    /** @return true if the request is allowed (a token was consumed), false if rate-limited. */
    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity))
                .tryConsume(refillTokensPerSecond, capacity);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        Bucket(int capacity) {
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume(double refillPerSecond, int capacity) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
