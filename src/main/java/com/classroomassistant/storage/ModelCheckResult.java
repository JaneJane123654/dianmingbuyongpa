package com.classroomassistant.storage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 模型完整性检查结果 (Model Integrity Check Result)
 *
 * <p>作为一个不可变的值对象，它承载了对本地 AI 模型文件进行扫描后的最终状态。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class ModelCheckResult {

    private final boolean ready;
    private final List<String> missingItems;

    /**
     * 构造模型完整性检查结果
     *
     * @param ready        所有核心模型文件是否均已就绪。true 表示可以启动 AI 功能，false 表示存在缺失。
     * @param missingItems 缺失的文件或目录描述列表。如果 ready 为 true，则该列表应为空。
     */
    public ModelCheckResult(boolean ready, List<String> missingItems) {
        this.ready = ready;
        this.missingItems = missingItems == null ? Collections.emptyList() : Collections.unmodifiableList(missingItems);
    }

    public boolean ready() {
        return ready;
    }

    public List<String> missingItems() {
        return missingItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelCheckResult)) return false;
        ModelCheckResult that = (ModelCheckResult) o;
        return ready == that.ready && Objects.equals(missingItems, that.missingItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ready, missingItems);
    }

    @Override
    public String toString() {
        return "ModelCheckResult{" +
                "ready=" + ready +
                ", missingItems=" + missingItems +
                '}';
    }
}

