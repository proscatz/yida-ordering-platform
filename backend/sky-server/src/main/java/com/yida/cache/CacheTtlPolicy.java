package com.yida.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CacheTtlPolicy {
    private static final long NORMAL_SECONDS = 30 * 60;
    private static final long NORMAL_JITTER_SECONDS = 5 * 60;
    private static final long EMPTY_SECONDS = 60;
    private static final long EMPTY_JITTER_SECONDS = 30;
    private static final long SHOP_SECONDS = 60 * 60;
    private static final long SHOP_JITTER_SECONDS = 5 * 60;

    public Duration normal() {
        return withJitter(NORMAL_SECONDS, NORMAL_JITTER_SECONDS);
    }

    public Duration empty() {
        return withJitter(EMPTY_SECONDS, EMPTY_JITTER_SECONDS);
    }

    public Duration shopStatus() {
        return withJitter(SHOP_SECONDS, SHOP_JITTER_SECONDS);
    }

    private Duration withJitter(long baseSeconds, long maxJitterSeconds) {
        return Duration.ofSeconds(baseSeconds + ThreadLocalRandom.current().nextLong(maxJitterSeconds + 1));
    }
}