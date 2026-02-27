﻿package com.classroomassistant.storage;

import java.util.Objects;

/**
 * 全局音频配置 (Global Audio Configuration)
 *
 * <p>作为一个不可变的值对象，它统一管理应用中音频采集、缓冲区分配及处理所需的各项物理参数。
 * 使用统一的配置对象有助于避免在不同模块中硬编码"魔法常量"，确保音频处理链路的一致性。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class AudioConfig {

    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;
    private final int frameMillis;
    private final int defaultLookbackSeconds;

    /**
     * 构造全局音频配置
     *
     * @param sampleRate             音频采样率（如 16000 Hz），决定了音频的频带宽度
     * @param channels               声道数（1 为单声道，2 为双声道）
     * @param bitsPerSample          每个采样的位数（通常为 16-bit PCM）
     * @param frameMillis            单帧音频的时长（毫秒），影响实时处理的粒度
     * @param defaultLookbackSeconds 默认的环形缓冲区回溯时长（秒），用于获取历史音频
     */
    public AudioConfig(int sampleRate, int channels, int bitsPerSample, int frameMillis, int defaultLookbackSeconds) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.frameMillis = frameMillis;
        this.defaultLookbackSeconds = defaultLookbackSeconds;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public int channels() {
        return channels;
    }

    public int bitsPerSample() {
        return bitsPerSample;
    }

    public int frameMillis() {
        return frameMillis;
    }

    public int defaultLookbackSeconds() {
        return defaultLookbackSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AudioConfig)) return false;
        AudioConfig that = (AudioConfig) o;
        return sampleRate == that.sampleRate
                && channels == that.channels
                && bitsPerSample == that.bitsPerSample
                && frameMillis == that.frameMillis
                && defaultLookbackSeconds == that.defaultLookbackSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleRate, channels, bitsPerSample, frameMillis, defaultLookbackSeconds);
    }

    @Override
    public String toString() {
        return "AudioConfig{" +
                "sampleRate=" + sampleRate +
                ", channels=" + channels +
                ", bitsPerSample=" + bitsPerSample +
                ", frameMillis=" + frameMillis +
                ", defaultLookbackSeconds=" + defaultLookbackSeconds +
                '}';
    }
}