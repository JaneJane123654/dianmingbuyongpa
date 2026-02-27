package com.classroomassistant.storage;

import java.util.Objects;

/**
 * 静音检测默认配置 (VAD Default Configuration)
 *
 * <p>作为一个不可变的值对象，它封装了从系统配置文件加载的静音检测（Voice Activity Detection）默认参数。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class VadDefaults {

    private final boolean enabledDefault;
    private final int quietThresholdSecondsDefault;

    /**
     * 构造 VAD 默认配置
     *
     * @param enabledDefault               默认是否启用 VAD 功能
     * @param quietThresholdSecondsDefault 默认的触发静音判定所需的连续时长（秒）
     */
    public VadDefaults(boolean enabledDefault, int quietThresholdSecondsDefault) {
        this.enabledDefault = enabledDefault;
        this.quietThresholdSecondsDefault = quietThresholdSecondsDefault;
    }

    public boolean enabledDefault() {
        return enabledDefault;
    }

    public int quietThresholdSecondsDefault() {
        return quietThresholdSecondsDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VadDefaults)) return false;
        VadDefaults that = (VadDefaults) o;
        return enabledDefault == that.enabledDefault
                && quietThresholdSecondsDefault == that.quietThresholdSecondsDefault;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabledDefault, quietThresholdSecondsDefault);
    }

    @Override
    public String toString() {
        return "VadDefaults{" +
                "enabledDefault=" + enabledDefault +
                ", quietThresholdSecondsDefault=" + quietThresholdSecondsDefault +
                '}';
    }
}

