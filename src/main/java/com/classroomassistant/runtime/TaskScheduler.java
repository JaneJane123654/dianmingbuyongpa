package com.classroomassistant.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务调度器
 *
 * <p>统一管理后台任务线程池，避免直接创建线程导致资源失控。
 */
public class TaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;

    public TaskScheduler() {
        this.ioExecutor = Executors.newFixedThreadPool(2, newNamedFactory("io-worker"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(newNamedFactory("scheduler"));
    }

    /**
     * 提交普通任务
     *
     * @param task 任务
     * @return Future
     */
    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task, "任务不能为空");
        return ioExecutor.submit(task);
    }

    /**
     * 提交带返回值的任务
     *
     * @param task 任务
     * @param <T> 返回类型
     * @return Future
     */
    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "任务不能为空");
        return ioExecutor.submit(task);
    }

    /**
     * 定时执行任务
     *
     * @param task 任务
     * @param delay 延迟时间
     * @return ScheduledFuture
     */
    public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        Objects.requireNonNull(task, "任务不能为空");
        Objects.requireNonNull(delay, "延迟不能为空");
        return scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 周期执行任务
     *
     * @param task 任务
     * @param initialDelay 首次延迟
     * @param period 周期
     * @return ScheduledFuture
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration initialDelay, Duration period) {
        Objects.requireNonNull(task, "任务不能为空");
        Objects.requireNonNull(initialDelay, "初始延迟不能为空");
        Objects.requireNonNull(period, "周期不能为空");
        return scheduler.scheduleAtFixedRate(task, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        ioExecutor.shutdown();
        scheduler.shutdown();
        try {
            if (!ioExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("任务调度器已关闭");
    }

    private static ThreadFactory newNamedFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        };
    }
}
