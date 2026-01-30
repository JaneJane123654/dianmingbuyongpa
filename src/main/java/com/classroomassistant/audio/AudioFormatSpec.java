package com.classroomassistant.audio;

import javax.sound.sampled.AudioFormat;

/**
 * 音频格式描述（值对象）
 *
 * <p>统一采样率、声道数等参数，并提供字节数计算方法。
 */
public record AudioFormatSpec(int sampleRate, int channels, int bitsPerSample, int frameMillis) {

    /**
     * 转换为 Java Sound API 的 AudioFormat
     *
     * @return AudioFormat 实例
     */
    public AudioFormat toAudioFormat() {
        return new AudioFormat(sampleRate, bitsPerSample, channels, true, false);
    }

    /**
     * 每秒字节数
     *
     * @return 每秒字节数
     */
    public int bytesPerSecond() {
        return sampleRate * channels * bitsPerSample / 8;
    }

    /**
     * 单帧字节数
     *
     * @return 单帧字节数
     */
    public int frameBytes() {
        int bytes = bytesPerSecond() * frameMillis / 1000;
        return Math.max(bytes, 1);
    }
}
