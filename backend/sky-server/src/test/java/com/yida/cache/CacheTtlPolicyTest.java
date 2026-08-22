package com.yida.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTtlPolicyTest {
    private final CacheTtlPolicy policy = new CacheTtlPolicy();

    @Test
    void normalTtlContainsBoundedRandomJitter() {
        assertRange(policy.normal(), 1800, 2100);
    }

    @Test
    void emptyTtlIsShortAndContainsJitter() {
        assertRange(policy.empty(), 60, 90);
    }

    @Test
    void shopStatusTtlContainsBoundedRandomJitter() {
        assertRange(policy.shopStatus(), 3600, 3900);
    }

    private void assertRange(Duration duration, long min, long max) {
        assertTrue(duration.getSeconds() >= min);
        assertTrue(duration.getSeconds() <= max);
    }
}