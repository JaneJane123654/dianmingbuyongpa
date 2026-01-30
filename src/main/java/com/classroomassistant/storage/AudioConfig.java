package com.classroomassistant.storage;

/**
 * 音频配置（值对象）
 *
 * <p>统一采样率等参数，避免散落的魔法常量。
 */
public record AudioConfig(
    int sampleRate,
    int channels,
    int bitsPerSample,
    int frameMillis,
    int defaultLookbackSeconds
) {
}

