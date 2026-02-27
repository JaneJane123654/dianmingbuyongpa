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
 * 任务调度器 (Task Scheduler)
 *
 * <p>系统后台任务的集中管理器。通过持有专用的线程池（ExecutorService 和 ScheduledExecutorService），
 * 实现了对异步 IO 任务、定时检查任务和周期性维护任务的统一调度。
 *
 * <p>该类保证了线程资源的合理分配，防止因盲目创建线程而导致的系统资源耗尽。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class TaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    private final ExecutorService ioExecutor;
    private final ScheduledExecutorService scheduler;

    /**
     * 初始化任务调度器
     * <p>创建固定大小为 2 的 IO 线程池和一个单线程的调度器线程池。
     */
    public TaskScheduler() {
        this.ioExecutor = Executors.newFixedThreadPool(2, newNamedFactory("io-worker"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(newNamedFactory("scheduler"));
    }

    /**
     * 提交一个无返回值的异步 Runnable 任务到 IO 线程池
     *
     * @param task 要执行的任务内容
     * @return {@link Future} 对象，可用于取消任务或检查执行状态
     * @throws NullPointerException 如果 task 为 null
     */
    public Future<?> submit(Runnable task) {
        Objects.requireNonNull(task, "任务不能为空");
        return ioExecutor.submit(task);
    }

    /**
     * 提交一个有返回值的异步 Callable 任务到 IO 线程池
     *
     * @param <T>  返回值的类型
     * @param task 要执行的任务内容
     * @return {@link Future} 对象，可用于获取执行结果
     * @throws NullPointerException 如果 task 为 null
     */
    public <T> Future<T> submit(Callable<T> task) {
        Objects.requireNonNull(task, "任务不能为空");
        return ioExecutor.submit(task);
    }

    /**
     * 延迟指定时间后执行一次性任务
     *
     * @param task  要执行的任务内容
     * @param delay 延迟的时长
     * @return {@link ScheduledFuture} 对象
     * @throws NullPointerException 如果任一参数为 null
     */
    public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        Objects.requireNonNull(task, "任务不能为空");
        Objects.requireNonNull(delay, "延迟不能为空");
        return scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 以固定频率周期性执行任务
     *
     * @param task         要执行的任务内容
     * @param initialDelay 首次执行前的延迟时长
     * @param period       两次执行之间的时间间隔
     * @return {@link ScheduledFuture} 对象
     * @throws NullPointerException 如果任一参数为 null
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration initialDelay, Duration period) {
        Objects.requireNonNull(task, "任务不能为空");
        Objects.requireNonNull(initialDelay, "初始延迟不能为空");
        Objects.requireNonNull(period, "周期不能为空");
        return scheduler.scheduleAtFixedRate(task, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 优雅关闭所有线程池
     *
     * <p>首先尝试正常关闭 (shutdown)，允许已提交的任务执行完毕。
     * 如果在 3 秒内未完成终止，则会强制中断剩余任务 (shutdownNow)。
     * 该方法应在应用退出或组件销毁时调用。
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

    /**
     * 创建一个命名的守护线程工厂
     *
     * @param prefix 线程名称前缀
     * @return 线程工厂实例
     */
    private static ThreadFactory newNamedFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        };
    }
}
