package com.classroomassistant.audio;

import javax.sound.sampled.AudioFormat;
import java.util.Objects;

/**
 * 音频格式描述 (Audio Format Specification)
 *
 * <p>作为不可变的值对象，统一封装了音频采集和处理所需的各项参数（采样率、声道数、位深、帧长）。
 * 提供便捷的方法用于计算字节速率、帧大小，以及与 Java Sound API ({@link javax.sound.sampled.AudioFormat}) 的互操作。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class AudioFormatSpec {

    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;
    private final int frameMillis;

    /**
     * 构造音频格式描述
     *
     * @param sampleRate    采样率（如 16000）
     * @param channels      声道数（1 为单声道，2 为双声道）
     * @param bitsPerSample 每个采样的位数（通常为 16）
     * @param frameMillis   单帧时长（毫秒，影响实时性与系统开销的平衡）
     */
    public AudioFormatSpec(int sampleRate, int channels, int bitsPerSample, int frameMillis) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.frameMillis = frameMillis;
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

    /**
     * 转换为 Java 标准 Sound API 的 {@link AudioFormat} 对象
     *
     * @return 对应的 AudioFormat 实例（默认为有符号、小端序 PCM）
     */
    public AudioFormat toAudioFormat() {
        return new AudioFormat(sampleRate, bitsPerSample, channels, true, false);
    }

    /**
     * 计算每秒产生的字节数 (Byte Rate)
     *
     * @return 每秒字节数
     */
    public int bytesPerSecond() {
        return sampleRate * channels * bitsPerSample / 8;
    }

    /**
     * 计算单帧音频数据占用的字节数
     *
     * @return 单帧字节数（最小为 1 字节）
     */
    public int frameBytes() {
        int bytes = bytesPerSecond() * frameMillis / 1000;
        return Math.max(bytes, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AudioFormatSpec)) return false;
        AudioFormatSpec that = (AudioFormatSpec) o;
        return sampleRate == that.sampleRate
                && channels == that.channels
                && bitsPerSample == that.bitsPerSample
                && frameMillis == that.frameMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleRate, channels, bitsPerSample, frameMillis);
    }

    @Override
    public String toString() {
        return "AudioFormatSpec{" +
                "sampleRate=" + sampleRate +
                ", channels=" + channels +
                ", bitsPerSample=" + bitsPerSample +
                ", frameMillis=" + frameMillis +
                '}';
    }
}
