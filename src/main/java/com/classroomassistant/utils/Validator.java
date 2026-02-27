package com.classroomassistant.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入校验与规整工具 (Validator)
 *
 * <p>提供通用的参数校验、数据规整及业务逻辑约束检查方法。
 * 旨在通过前置校验，确保核心业务模块接收到的数据是合法且符合预期的。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class Validator {

    private Validator() {
        // 工具类禁止实例化
    }

    /**
     * 规整关键词输入字符串
     * <p>支持中英文逗号分隔，并会自动去除首尾空格、剔除空项，最后以标准英文逗号重新拼接。
     *
     * @param keywordsInput 原始输入的关键词字符串
     * @return 规整后的字符串，例如 "老师,点名"
     */
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

    /**
     * 校验数值是否在指定闭区间范围内
     *
     * @param value        待校验的值
     * @param minInclusive 最小值（包含）
     * @param maxInclusive 最大值（包含）
     * @param fieldName    字段名称，用于在异常信息中标识错误来源
     * @throws IllegalArgumentException 如果值不在范围内
     */
    public static void requireRange(int value, int minInclusive, int maxInclusive, String fieldName) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(fieldName + " 范围非法: " + value + "，要求 " + minInclusive + "-" + maxInclusive);
        }
    }
}

