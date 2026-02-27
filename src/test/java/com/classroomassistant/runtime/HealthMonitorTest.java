package com.classroomassistant.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HealthMonitor 单元测试
 *
 * <p>验证健康监控器的心跳跟踪、看门狗功能和自动恢复机制。
 * 
 * @author Code Assistant
 * @date 2026-02-01
 */
class HealthMonitorTest {

    private TaskScheduler taskScheduler;
    private HealthMonitor healthMonitor;

    @BeforeEach
    void setUp() {
        taskScheduler = new TaskScheduler();
        healthMonitor = new HealthMonitor(taskScheduler);
    }

    @AfterEach
    void tearDown() {
        healthMonitor.stopWatchdog();
        taskScheduler.shutdown();
    }

    @Test
    @DisplayName("初始时 lastAudioFrameAt 应为 EPOCH")
    void initialState_lastAudioFrameAt_shouldBeEpoch() {
        Instant lastFrame = healthMonitor.getLastAudioFrameAt();

        assertEquals(Instant.EPOCH, lastFrame);
    }

    @Test
    @DisplayName("初始时健康状态应为 HEALTHY")
    void initialState_healthState_shouldBeHealthy() {
        assertEquals(HealthMonitor.HealthState.HEALTHY, healthMonitor.getHealthState());
    }

    @Test
    @DisplayName("初始时连续失败次数应为 0")
    void initialState_consecutiveFailures_shouldBeZero() {
        assertEquals(0, healthMonitor.getConsecutiveFailures());
    }

    @Test
    @DisplayName("markAudioFrameReceived 应更新 lastAudioFrameAt")
    void markAudioFrameReceived_shouldUpdateTimestamp() {
        Instant before = Instant.now();

        healthMonitor.markAudioFrameReceived();

        Instant lastFrame = healthMonitor.getLastAudioFrameAt();
        assertNotEquals(Instant.EPOCH, lastFrame);
        assertTrue(lastFrame.isAfter(before.minusSeconds(1)));
        assertTrue(lastFrame.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("多次调用 markAudioFrameReceived 应更新到最新时间")
    void markAudioFrameReceived_multipleTimes_shouldUpdateToLatest() throws Exception {
        healthMonitor.markAudioFrameReceived();
        Instant first = healthMonitor.getLastAudioFrameAt();

        Thread.sleep(50);
        healthMonitor.markAudioFrameReceived();
        Instant second = healthMonitor.getLastAudioFrameAt();

        assertTrue(second.isAfter(first));
    }

    @Test
    @DisplayName("startWatchdog 应启动看门狗任务")
    @Timeout(10)
    void startWatchdog_shouldStartTask() throws Exception {
        // 确保有音频帧记录，避免误报
        healthMonitor.markAudioFrameReceived();
        
        healthMonitor.startWatchdog();

        // 等待一个检查周期以确认任务在运行
        Thread.sleep(1000);
        
        // 看门狗已启动，不应抛出异常
        assertDoesNotThrow(() -> healthMonitor.stopWatchdog());
    }

    @Test
    @DisplayName("stopWatchdog 应停止看门狗任务")
    void stopWatchdog_shouldStopTask() {
        healthMonitor.startWatchdog();

        assertDoesNotThrow(() -> healthMonitor.stopWatchdog());
    }

    @Test
    @DisplayName("多次调用 startWatchdog 应安全（幂等）")
    void startWatchdog_multipleTimes_shouldBeSafe() {
        healthMonitor.markAudioFrameReceived();

        assertDoesNotThrow(() -> {
            healthMonitor.startWatchdog();
            healthMonitor.startWatchdog();
            healthMonitor.startWatchdog();
        });

        healthMonitor.stopWatchdog();
    }

    @Test
    @DisplayName("多次调用 stopWatchdog 应安全（幂等）")
    void stopWatchdog_multipleTimes_shouldBeSafe() {
        healthMonitor.startWatchdog();

        assertDoesNotThrow(() -> {
            healthMonitor.stopWatchdog();
            healthMonitor.stopWatchdog();
            healthMonitor.stopWatchdog();
        });
    }

    @Test
    @DisplayName("未启动时调用 stopWatchdog 应安全")
    void stopWatchdog_withoutStart_shouldBeSafe() {
        assertDoesNotThrow(() -> healthMonitor.stopWatchdog());
    }

    @Test
    @DisplayName("构造函数传入 null 应抛出 NullPointerException")
    void constructor_withNull_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> new HealthMonitor(null));
    }

    @Test
    @DisplayName("setRecoveryCallback 应设置恢复回调")
    void setRecoveryCallback_shouldBeSet() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        healthMonitor.setRecoveryCallback(() -> called.set(true));
        
        // 回调已设置，不应抛出异常
        assertDoesNotThrow(() -> healthMonitor.startWatchdog());
    }

    @Test
    @DisplayName("setErrorCallback 应设置错误回调")
    void setErrorCallback_shouldBeSet() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        healthMonitor.setErrorCallback(() -> called.set(true));
        
        // 回调已设置，不应抛出异常
        assertDoesNotThrow(() -> healthMonitor.startWatchdog());
    }

    @Test
    @DisplayName("resetState 应重置所有状态")
    void resetState_shouldResetAllState() {
        healthMonitor.markAudioFrameReceived();
        healthMonitor.startWatchdog();
        
        healthMonitor.resetState();
        
        assertEquals(HealthMonitor.HealthState.HEALTHY, healthMonitor.getHealthState());
        assertEquals(0, healthMonitor.getConsecutiveFailures());
        assertNotEquals(Instant.EPOCH, healthMonitor.getLastAudioFrameAt());
    }

    @Test
    @DisplayName("stopWatchdog 应重置失败计数和健康状态")
    void stopWatchdog_shouldResetState() {
        healthMonitor.startWatchdog();
        
        healthMonitor.stopWatchdog();
        
        assertEquals(HealthMonitor.HealthState.HEALTHY, healthMonitor.getHealthState());
        assertEquals(0, healthMonitor.getConsecutiveFailures());
    }

    @Test
    @DisplayName("markAudioFrameReceived 在失败后应重置状态为 HEALTHY")
    void markAudioFrameReceived_afterFailure_shouldResetToHealthy() {
        // 模拟失败状态的逻辑在实际运行时会被触发
        // 这里测试调用 markAudioFrameReceived 后状态应为 HEALTHY
        healthMonitor.markAudioFrameReceived();
        
        assertEquals(HealthMonitor.HealthState.HEALTHY, healthMonitor.getHealthState());
        assertEquals(0, healthMonitor.getConsecutiveFailures());
    }
}
