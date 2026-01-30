package com.classroomassistant.storage;

import java.util.List;

/**
 * 模型检查结果（值对象）
 */
public record ModelCheckResult(
    boolean ready,
    List<String> missingItems
) {
}

