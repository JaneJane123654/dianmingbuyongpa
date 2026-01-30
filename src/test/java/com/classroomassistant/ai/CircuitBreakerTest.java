package com.classroomassistant.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {

    @Test
    void testOpenAfterFailures() {
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(1));
        assertTrue(breaker.allowRequest());
        breaker.recordFailure();
        assertTrue(breaker.allowRequest());
        breaker.recordFailure();
        assertFalse(breaker.allowRequest());
    }

    @Test
    void testRecoverAfterOpenDuration() throws InterruptedException {
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofMillis(100));
        breaker.recordFailure();
        assertFalse(breaker.allowRequest());
        Thread.sleep(120);
        assertTrue(breaker.allowRequest());
    }
}
