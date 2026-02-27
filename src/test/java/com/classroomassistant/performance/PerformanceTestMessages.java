package com.classroomassistant.performance;

/**
 * 性能测试消息接口 (Performance Test Messages Interface)
 *
 * <p>定义性能测试中使用的所有文本消息，支持中英双语切换。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public interface PerformanceTestMessages {

    // ================== 测试描述 ==================
    String circularBufferWritePerformance();
    String circularBufferReadPerformance();
    String circularBufferConcurrentPerformance();
    String audioFormatConversionPerformance();
    String promptTemplateBuildPerformance();
    String circuitBreakerStateTransitionPerformance();
    String llmClientMockPerformance();
    String speechRecognitionMockPerformance();

    // ================== 日志消息 ==================
    String testStarted(String testName);
    String testCompleted(String testName, long durationMs);
    String iterationsCompleted(int count);
    String averageTime(double avgMs);
    String throughput(double opsPerSecond);
    String memoryUsed(long bytes);
    
    // ================== 断言消息 ==================
    String shouldCompleteWithinMs(long expectedMs);
    String shouldHandleOperationsPerSecond(long minOps);
    String shouldNotExceedMemory(long maxBytes);

    // ================== 性能指标标题 ==================
    String performanceReport();
    String operation();
    String iterations();
    String totalTime();
    String averageTimeTitle();
    String throughputTitle();
}
