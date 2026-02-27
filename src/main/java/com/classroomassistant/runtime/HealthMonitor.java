package com.classroomassistant.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统健康监控器 (Health Monitor)
 *
 * <p>负责监控系统关键链路的运行状态。主要功能包括：
 * <ul>
 *   <li>音频心跳监控（Watchdog）：检测音频输入流是否中断</li>
 *   <li>自动恢复机制：当检测到异常时尝试自动恢复</li>
 *   <li>连续失败追踪：达到阈值后进入错误状态</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class HealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);

    /** 音频心跳超时阈值（秒） */
    private static final int AUDIO_TIMEOUT_SECONDS = 5;

    /** 最大连续恢复尝试次数 */
    private static final int MAX_RECOVERY_ATTEMPTS = 3;

    private final TaskScheduler taskScheduler;
    private final AtomicReference<Instant> lastAudioFrameAt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<HealthState> healthState = new AtomicReference<>(HealthState.HEALTHY);

    private ScheduledFuture<?> watchTask;
    private Runnable recoveryCallback;
    private Runnable errorCallback;

    /**
     * 健康状态枚举
     */
    public enum HealthState {
        /** 健康运行中 */
        HEALTHY,
        /** 正在恢复中 */
        RECOVERING,
        /** 错误状态（需要人工干预） */
        ERROR
    }

    /**
     * 构造健康监控器
     *
     * @param taskScheduler 用于执行定时检查任务的调度器
     */
    public HealthMonitor(TaskScheduler taskScheduler) {
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "任务调度器不能为空");
    }

    /**
     * 标记收到一个新的音频帧
     * <p>由音频采集模块调用，更新最近一次活跃时间戳，并重置失败计数。
     */
    public void markAudioFrameReceived() {
        lastAudioFrameAt.set(Instant.now());
        if (consecutiveFailures.get() > 0) {
            consecutiveFailures.set(0);
            healthState.set(HealthState.HEALTHY);
            logger.info("音频流已恢复正常");
        }
    }

    /**
     * 获取最近一次音频帧接收的时刻
     *
     * @return 包含最近活跃时间的 {@link Instant} 对象
     */
    public Instant getLastAudioFrameAt() {
        return lastAudioFrameAt.get();
    }

    /**
     * 获取当前健康状态
     *
     * @return 当前的 {@link HealthState}
     */
    public HealthState getHealthState() {
        return healthState.get();
    }

    /**
     * 获取连续失败次数
     *
     * @return 连续失败计数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * 设置恢复回调函数
     * <p>当检测到音频超时时，会调用此回调尝试恢复录音链路。
     *
     * @param callback 恢复回调函数
     */
    public void setRecoveryCallback(Runnable callback) {
        this.recoveryCallback = callback;
    }

    /**
     * 设置错误回调函数
     * <p>当多次恢复失败后进入错误状态时调用。
     *
     * @param callback 错误回调函数
     */
    public void setErrorCallback(Runnable callback) {
        this.errorCallback = callback;
    }

    /**
     * 启动看门狗定时检查任务
     * <p>以 5 秒为周期检查音频流健康状态。
     */
    public void startWatchdog() {
        if (watchTask != null && !watchTask.isCancelled()) {
            return;
        }
        consecutiveFailures.set(0);
        healthState.set(HealthState.HEALTHY);
        watchTask = taskScheduler.scheduleAtFixedRate(this::checkAudioHealth, 
            Duration.ofSeconds(AUDIO_TIMEOUT_SECONDS), Duration.ofSeconds(AUDIO_TIMEOUT_SECONDS));
        logger.debug("健康监控看门狗已启动");
    }

    /**
     * 停止看门狗检查任务
     */
    public void stopWatchdog() {
        if (watchTask != null) {
            watchTask.cancel(true);
            watchTask = null;
        }
        consecutiveFailures.set(0);
        healthState.set(HealthState.HEALTHY);
        logger.debug("健康监控看门狗已停止");
    }

    /**
     * 重置健康状态
     * <p>用于从错误状态恢复后重新开始监控。
     */
    public void resetState() {
        consecutiveFailures.set(0);
        healthState.set(HealthState.HEALTHY);
        lastAudioFrameAt.set(Instant.now());
        logger.info("健康监控状态已重置");
    }

    /**
     * 执行音频健康检查逻辑
     * <p>对比当前时间与最近一次音频帧到达时间。如果间隔过大，则尝试恢复。
     */
    private void checkAudioHealth() {
        Instant last = lastAudioFrameAt.get();
        if (last.equals(Instant.EPOCH)) {
            return;
        }

        Duration gap = Duration.between(last, Instant.now());
        if (gap.toSeconds() < AUDIO_TIMEOUT_SECONDS) {
            return;
        }

        int failures = consecutiveFailures.incrementAndGet();
        logger.warn("音频帧心跳超时: {} 秒，连续失败次数: {}", gap.toSeconds(), failures);

        if (failures >= MAX_RECOVERY_ATTEMPTS) {
            handleMaxFailuresReached();
        } else {
            attemptRecovery();
        }
    }

    /**
     * 尝试恢复录音链路
     */
    private void attemptRecovery() {
        if (recoveryCallback == null) {
            logger.warn("未设置恢复回调，无法自动恢复");
            return;
        }

        healthState.set(HealthState.RECOVERING);
        logger.info("正在尝试自动恢复录音链路（第 {} 次）", consecutiveFailures.get());

        try {
            recoveryCallback.run();
        } catch (Exception e) {
            logger.error("恢复过程中发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理达到最大失败次数
     */
    private void handleMaxFailuresReached() {
        healthState.set(HealthState.ERROR);
        logger.error("连续 {} 次恢复尝试失败，进入错误状态，需要用户干预", MAX_RECOVERY_ATTEMPTS);

        if (errorCallback != null) {
            try {
                errorCallback.run();
            } catch (Exception e) {
                logger.error("错误回调执行失败: {}", e.getMessage(), e);
            }
        }
    }
}
