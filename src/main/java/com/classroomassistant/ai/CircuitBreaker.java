package com.classroomassistant.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器
 *
 * <p>用于控制连续失败时快速失败，避免资源耗尽。
 */
public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> openedAt = new AtomicReference<>(Instant.EPOCH);

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("失败阈值必须大于 0");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration == null ? Duration.ofSeconds(10) : openDuration;
    }

    /**
     * 是否允许执行
     *
     * @return true 表示允许
     */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (Instant.now().isAfter(openedAt.get().plus(openDuration))) {
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * 记录成功
     */
    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    /**
     * 记录失败
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold) {
            state.set(State.OPEN);
            openedAt.set(Instant.now());
        }
    }

    public State getState() {
        return state.get();
    }
}
