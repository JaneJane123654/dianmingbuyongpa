package com.classroomassistant.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入校验与规整工具
 *
 * <p>用于对用户输入与外部数据进行基本校验，避免非法输入导致状态异常。
 */
public final class Validator {

    private Validator() {
    }

    public static String normalizeKeywords(String keywordsInput) {
        if (keywordsInput == null) {
            return "";
        }
        String normalized = keywordsInput.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        String[] parts = normalized.split("[,，]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String value = part.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        return String.join(",", result);
    }

    public static void requireRange(int value, int minInclusive, int maxInclusive, String fieldName) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(fieldName + " 范围非法: " + value + "，要求 " + minInclusive + "-" + maxInclusive);
        }
    }
}

