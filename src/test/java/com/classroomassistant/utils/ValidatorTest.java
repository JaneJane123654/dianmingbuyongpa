package com.classroomassistant.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validator 单元测试
 *
 * <p>验证输入校验与规整工具类的各项功能。
 * 
 * @author Code Assistant
 * @date 2026-02-01
 */
class ValidatorTest {

    // ========== normalizeKeywords 测试 ==========

    @Test
    @DisplayName("normalizeKeywords 对于 null 输入应返回空字符串")
    void normalizeKeywords_withNull_shouldReturnEmpty() {
        String result = Validator.normalizeKeywords(null);

        assertEquals("", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("normalizeKeywords 对于空白输入应返回空字符串")
    void normalizeKeywords_withBlank_shouldReturnEmpty(String input) {
        String result = Validator.normalizeKeywords(input);

        assertEquals("", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应正确处理单个关键词")
    void normalizeKeywords_singleKeyword_shouldReturnTrimmed() {
        String result = Validator.normalizeKeywords("  张三  ");

        assertEquals("张三", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应正确处理英文逗号分隔")
    void normalizeKeywords_withEnglishComma_shouldNormalize() {
        String result = Validator.normalizeKeywords("张三,李四,王五");

        assertEquals("张三,李四,王五", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应正确处理中文逗号分隔")
    void normalizeKeywords_withChineseComma_shouldNormalize() {
        String result = Validator.normalizeKeywords("张三，李四，王五");

        assertEquals("张三,李四,王五", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应正确处理混合逗号分隔")
    void normalizeKeywords_withMixedCommas_shouldNormalize() {
        String result = Validator.normalizeKeywords("张三,李四，王五,赵六");

        assertEquals("张三,李四,王五,赵六", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应去除每个关键词的首尾空格")
    void normalizeKeywords_shouldTrimEachKeyword() {
        String result = Validator.normalizeKeywords("  张三  ,  李四  ,  王五  ");

        assertEquals("张三,李四,王五", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应过滤空的关键词项")
    void normalizeKeywords_shouldFilterEmptyItems() {
        String result = Validator.normalizeKeywords("张三,,李四,,,王五");

        assertEquals("张三,李四,王五", result);
    }

    @Test
    @DisplayName("normalizeKeywords 应处理连续的中英文逗号")
    void normalizeKeywords_withConsecutiveCommas_shouldFilter() {
        String result = Validator.normalizeKeywords("张三,，,李四");

        assertEquals("张三,李四", result);
    }

    @ParameterizedTest
    @CsvSource({
        "'张三, 李四, 王五', '张三,李四,王五'",
        "'test1,test2', 'test1,test2'",
        "'老师，同学，朋友', '老师,同学,朋友'"
    })
    @DisplayName("normalizeKeywords 参数化测试")
    void normalizeKeywords_parameterized(String input, String expected) {
        String result = Validator.normalizeKeywords(input);

        assertEquals(expected, result);
    }

    // ========== requireRange 测试 ==========

    @Test
    @DisplayName("requireRange 值在范围内应不抛出异常")
    void requireRange_valueInRange_shouldNotThrow() {
        assertDoesNotThrow(() -> Validator.requireRange(5, 1, 10, "测试字段"));
    }

    @Test
    @DisplayName("requireRange 值等于最小值应不抛出异常")
    void requireRange_valueEqualsMin_shouldNotThrow() {
        assertDoesNotThrow(() -> Validator.requireRange(1, 1, 10, "测试字段"));
    }

    @Test
    @DisplayName("requireRange 值等于最大值应不抛出异常")
    void requireRange_valueEqualsMax_shouldNotThrow() {
        assertDoesNotThrow(() -> Validator.requireRange(10, 1, 10, "测试字段"));
    }

    @Test
    @DisplayName("requireRange 值小于最小值应抛出 IllegalArgumentException")
    void requireRange_valueBelowMin_shouldThrow() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Validator.requireRange(0, 1, 10, "回溯秒数")
        );

        assertTrue(exception.getMessage().contains("回溯秒数"));
        assertTrue(exception.getMessage().contains("0"));
        assertTrue(exception.getMessage().contains("1"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    @DisplayName("requireRange 值大于最大值应抛出 IllegalArgumentException")
    void requireRange_valueAboveMax_shouldThrow() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Validator.requireRange(15, 1, 10, "阈值")
        );

        assertTrue(exception.getMessage().contains("阈值"));
        assertTrue(exception.getMessage().contains("15"));
    }

    @Test
    @DisplayName("requireRange 负数范围应正确处理")
    void requireRange_negativeRange_shouldWork() {
        assertDoesNotThrow(() -> Validator.requireRange(-5, -10, 0, "温度"));
        assertThrows(IllegalArgumentException.class, 
            () -> Validator.requireRange(-15, -10, 0, "温度"));
    }

    @Test
    @DisplayName("requireRange 单值范围（min==max）应正确处理")
    void requireRange_singleValueRange_shouldWork() {
        assertDoesNotThrow(() -> Validator.requireRange(5, 5, 5, "固定值"));
        assertThrows(IllegalArgumentException.class, 
            () -> Validator.requireRange(4, 5, 5, "固定值"));
    }
}
