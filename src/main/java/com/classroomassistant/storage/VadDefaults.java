package com.classroomassistant.storage;

/**
 * VAD 默认配置（值对象）
 */
public record VadDefaults(
    boolean enabledDefault,
    int quietThresholdSecondsDefault
) {
}

