package com.classroomassistant.benchmark;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 性能测试消息国际化 (Performance Test Messages I18n)
 *
 * <p>支持中英双语的性能测试结果输出。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public final class BenchmarkMessages {

    private static final String BUNDLE_BASE_NAME = "benchmark_messages";
    private static ResourceBundle bundle;
    private static Locale currentLocale = Locale.CHINESE;

    private BenchmarkMessages() {
    }

    /**
     * 设置当前语言
     *
     * @param language 语言代码 ("zh" 或 "en")
     */
    public static void setLanguage(String language) {
        if ("en".equalsIgnoreCase(language)) {
            currentLocale = Locale.ENGLISH;
        } else {
            currentLocale = Locale.CHINESE;
        }
        bundle = null; // 强制重新加载
    }

    /**
     * 获取当前语言的消息
     *
     * @param key 消息键
     * @return 本地化消息
     */
    public static String get(String key) {
        if (bundle == null) {
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
            } catch (Exception e) {
                // 回退到默认消息
                return getDefaultMessage(key);
            }
        }
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return getDefaultMessage(key);
        }
    }

    /**
     * 获取格式化的消息
     *
     * @param key  消息键
     * @param args 格式化参数
     * @return 格式化后的消息
     */
    public static String format(String key, Object... args) {
        return String.format(get(key), args);
    }

    private static String getDefaultMessage(String key) {
        // 中文默认消息
        switch (key) {
            case "benchmark.title":
                return "=== 性能基准测试 ===";
            case "benchmark.audio.title":
                return "【音频模块性能测试】";
            case "benchmark.ai.title":
                return "【AI 模块性能测试】";
            case "benchmark.speech.title":
                return "【语音识别性能测试】";
            case "benchmark.buffer.write":
                return "环形缓冲区写入";
            case "benchmark.buffer.read":
                return "环形缓冲区读取";
            case "benchmark.wav.write":
                return "WAV 文件写入";
            case "benchmark.pcm.convert":
                return "PCM 转浮点数";
            case "benchmark.prompt.build":
                return "Prompt 模板构建";
            case "benchmark.circuit.check":
                return "熔断器状态检查";
            case "benchmark.iterations":
                return "迭代次数";
            case "benchmark.total.time":
                return "总耗时";
            case "benchmark.avg.time":
                return "平均耗时";
            case "benchmark.ops.per.sec":
                return "每秒操作数";
            case "benchmark.ms":
                return "毫秒";
            case "benchmark.us":
                return "微秒";
            case "benchmark.ns":
                return "纳秒";
            case "benchmark.complete":
                return "性能测试完成";
            case "benchmark.summary":
                return "测试摘要";
            case "benchmark.passed":
                return "通过";
            case "benchmark.failed":
                return "失败";
            default:
                return key;
        }
    }
}
