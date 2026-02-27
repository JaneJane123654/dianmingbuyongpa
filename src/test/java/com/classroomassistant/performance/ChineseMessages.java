package com.classroomassistant.performance;

/**
 * 中文性能测试消息 (Chinese Performance Test Messages)
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class ChineseMessages implements PerformanceTestMessages {

    @Override
    public String circularBufferWritePerformance() {
        return "环形缓冲区写入性能测试";
    }

    @Override
    public String circularBufferReadPerformance() {
        return "环形缓冲区读取性能测试";
    }

    @Override
    public String circularBufferConcurrentPerformance() {
        return "环形缓冲区并发读写性能测试";
    }

    @Override
    public String audioFormatConversionPerformance() {
        return "音频格式转换性能测试";
    }

    @Override
    public String promptTemplateBuildPerformance() {
        return "提示词模板构建性能测试";
    }

    @Override
    public String circuitBreakerStateTransitionPerformance() {
        return "熔断器状态转换性能测试";
    }

    @Override
    public String llmClientMockPerformance() {
        return "LLM 客户端模拟调用性能测试";
    }

    @Override
    public String speechRecognitionMockPerformance() {
        return "语音识别模拟调用性能测试";
    }

    @Override
    public String testStarted(String testName) {
        return "开始测试: " + testName;
    }

    @Override
    public String testCompleted(String testName, long durationMs) {
        return "测试完成: " + testName + "，耗时: " + durationMs + " 毫秒";
    }

    @Override
    public String iterationsCompleted(int count) {
        return "完成迭代次数: " + count;
    }

    @Override
    public String averageTime(double avgMs) {
        return String.format("平均耗时: %.3f 毫秒", avgMs);
    }

    @Override
    public String throughput(double opsPerSecond) {
        return String.format("吞吐量: %.2f 次/秒", opsPerSecond);
    }

    @Override
    public String memoryUsed(long bytes) {
        return String.format("内存使用: %.2f MB", bytes / 1024.0 / 1024.0);
    }

    @Override
    public String shouldCompleteWithinMs(long expectedMs) {
        return "应在 " + expectedMs + " 毫秒内完成";
    }

    @Override
    public String shouldHandleOperationsPerSecond(long minOps) {
        return "应达到至少 " + minOps + " 次/秒";
    }

    @Override
    public String shouldNotExceedMemory(long maxBytes) {
        return String.format("内存使用不应超过 %.2f MB", maxBytes / 1024.0 / 1024.0);
    }

    @Override
    public String performanceReport() {
        return "性能测试报告";
    }

    @Override
    public String operation() {
        return "操作";
    }

    @Override
    public String iterations() {
        return "迭代次数";
    }

    @Override
    public String totalTime() {
        return "总耗时";
    }

    @Override
    public String averageTimeTitle() {
        return "平均耗时";
    }

    @Override
    public String throughputTitle() {
        return "吞吐量";
    }
}
