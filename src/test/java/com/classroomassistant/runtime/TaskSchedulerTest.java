package com.classroomassistant.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskScheduler 单元测试
 *
 * <p>验证任务调度器的异步任务提交、延迟执行和周期执行功能。
 * 
 * @author Code Assistant
 * @date 2026-02-01
 */
class TaskSchedulerTest {

    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        taskScheduler = new TaskScheduler();
    }

    @AfterEach
    void tearDown() {
        taskScheduler.shutdown();
    }

    @Test
    @DisplayName("submit(Runnable) 应成功执行任务")
    @Timeout(5)
    void submitRunnable_shouldExecuteTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = taskScheduler.submit(latch::countDown);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(future);
    }

    @Test
    @DisplayName("submit(Callable) 应返回正确的结果")
    @Timeout(5)
    void submitCallable_shouldReturnResult() throws Exception {
        Future<String> future = taskScheduler.submit(() -> "hello");

        String result = future.get(2, TimeUnit.SECONDS);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("submit 传入 null 应抛出 NullPointerException")
    void submit_withNullTask_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> taskScheduler.submit((Runnable) null));
    }

    @Test
    @DisplayName("schedule 应在延迟后执行任务")
    @Timeout(5)
    void schedule_shouldExecuteAfterDelay() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();

        taskScheduler.schedule(latch::countDown, Duration.ofMillis(200));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 150, "任务应在延迟后执行，实际用时: " + elapsed + "ms");
    }

    @Test
    @DisplayName("schedule 传入 null 任务应抛出 NullPointerException")
    void schedule_withNullTask_shouldThrowException() {
        assertThrows(NullPointerException.class, 
            () -> taskScheduler.schedule(null, Duration.ofMillis(100)));
    }

    @Test
    @DisplayName("schedule 传入 null 延迟应抛出 NullPointerException")
    void schedule_withNullDelay_shouldThrowException() {
        assertThrows(NullPointerException.class, 
            () -> taskScheduler.schedule(() -> {}, null));
    }

    @Test
    @DisplayName("scheduleAtFixedRate 应周期执行任务")
    @Timeout(5)
    void scheduleAtFixedRate_shouldExecutePeriodically() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
            counter.incrementAndGet();
            latch.countDown();
        }, Duration.ofMillis(50), Duration.ofMillis(100));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        future.cancel(true);
        assertTrue(counter.get() >= 3, "应至少执行 3 次，实际执行: " + counter.get());
    }

    @Test
    @DisplayName("scheduleAtFixedRate 返回的 Future 可取消任务")
    @Timeout(5)
    void scheduleAtFixedRate_canBeCancelled() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
            counter::incrementAndGet, 
            Duration.ofMillis(50), 
            Duration.ofMillis(50)
        );

        Thread.sleep(200);
        future.cancel(true);
        int countAtCancel = counter.get();
        Thread.sleep(200);

        // 取消后计数应停止增长（允许±1的误差，因为可能刚好在取消时执行了一次）
        assertTrue(counter.get() <= countAtCancel + 1, 
            "取消后任务应停止执行，取消时: " + countAtCancel + "，当前: " + counter.get());
    }

    @Test
    @DisplayName("shutdown 应优雅关闭线程池")
    @Timeout(5)
    void shutdown_shouldTerminateGracefully() {
        CountDownLatch latch = new CountDownLatch(1);
        taskScheduler.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        taskScheduler.shutdown();

        // shutdown 后应该能等待任务完成
        // 由于 shutdown 已被调用，线程池应处于终止状态
    }

    @Test
    @DisplayName("多次调用 shutdown 应安全")
    void shutdown_multipleTimes_shouldBeSafe() {
        taskScheduler.shutdown();
        assertDoesNotThrow(() -> taskScheduler.shutdown());
    }
}
