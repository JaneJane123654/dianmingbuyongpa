package com.classroomassistant.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 健康监控器
 *
 * <p>用于记录关键链路心跳，支持后续扩展自动恢复策略。
 */
public class HealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);

    private final TaskScheduler taskScheduler;
    private final AtomicReference<Instant> lastAudioFrameAt = new AtomicReference<>(Instant.EPOCH);

    private ScheduledFuture<?> watchTask;

    public HealthMonitor(TaskScheduler taskScheduler) {
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "任务调度器不能为空");
    }

    /**
     * 标记收到音频帧
     */
    public void markAudioFrameReceived() {
        lastAudioFrameAt.set(Instant.now());
    }

    /**
     * 获取最近一次音频帧时间
     *
     * @return 时间戳
     */
    public Instant getLastAudioFrameAt() {
        return lastAudioFrameAt.get();
    }

    /**
     * 启动简单的健康检查任务
     */
    public void startWatchdog() {
        if (watchTask != null && !watchTask.isCancelled()) {
            return;
        }
        watchTask = taskScheduler.scheduleAtFixedRate(this::checkAudioHealth, Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    /**
     * 停止健康检查
     */
    public void stopWatchdog() {
        if (watchTask != null) {
            watchTask.cancel(true);
        }
    }

    private void checkAudioHealth() {
        Instant last = lastAudioFrameAt.get();
        if (last.equals(Instant.EPOCH)) {
            return;
        }
        Duration gap = Duration.between(last, Instant.now());
        if (gap.toSeconds() >= 5) {
            logger.warn("音频帧心跳超时: {} 秒", gap.toSeconds());
        }
    }
}
