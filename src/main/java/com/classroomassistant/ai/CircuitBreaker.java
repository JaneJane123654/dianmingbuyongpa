package com.classroomassistant.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器 (Circuit Breaker)
 *
 * <p>用于控制连续失败时快速失败，避免资源耗尽。
 * 该实现支持三种状态：CLOSED（闭合）、OPEN（开启）、HALF_OPEN（半开）。
 * 当失败次数达到阈值时，状态从 CLOSED 变为 OPEN，并在指定时间后尝试进入 HALF_OPEN 状态。
 *
 * <p>使用示例：
 * <pre>
 * CircuitBreaker cb = new CircuitBreaker(5, Duration.ofSeconds(30));
 * if (cb.allowRequest()) {
 *     try {
 *         // 执行请求
 *         cb.recordSuccess();
 *     } catch (Exception e) {
 *         cb.recordFailure();
 *     }
 * }
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class CircuitBreaker {

    /**
     * 熔断器状态枚举
     */
    public enum State {
        /** 正常状态，允许请求 */
        CLOSED,
        /** 开启状态，拒绝请求 */
        OPEN,
        /** 半开状态，尝试允许部分请求以验证服务恢复情况 */
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> openedAt = new AtomicReference<>(Instant.EPOCH);

    /**
     * 构造一个新的熔断器
     *
     * @param failureThreshold 失败阈值，连续失败多少次后触发熔断
     * @param openDuration     熔断持续时间，即从 OPEN 到 HALF_OPEN 的等待时间
     * @throws IllegalArgumentException 如果 failureThreshold <= 0
     */
    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("失败阈值必须大于 0");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration == null ? Duration.ofSeconds(10) : openDuration;
    }

    /**
     * 判断当前是否允许执行请求
     * <p>在 HALF_OPEN 状态下，仅允许第一个请求通过进行探测，其他请求将被拒绝。
     *
     * @return true 表示允许执行，false 表示当前处于熔断状态
     */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (Instant.now().isAfter(openedAt.get().plus(openDuration))) {
                // 使用 CAS 确保只有一个线程能够进入 HALF_OPEN 状态进行探测
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    return true;
                }
                // CAS 失败说明其他线程已切换状态，重新检查
                return state.get() == State.CLOSED;
            }
            return false;
        }
        // HALF_OPEN 状态下，拒绝新请求，只允许已获得探测权的请求执行
        return false;
    }

    /**
     * 记录一次成功的调用，重置失败计数并关闭熔断器
     */
    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    /**
     * 记录一次失败的调用，如果失败次数达到阈值则触发熔断
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold) {
            state.set(State.OPEN);
            openedAt.set(Instant.now());
        }
    }

    /**
     * 获取当前熔断器状态
     *
     * @return 当前状态 {@link State}
     */
    public State getState() {
        return state.get();
    }
}
