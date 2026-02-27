package com.classroomassistant.performance;

/**
 * English Performance Test Messages
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class EnglishMessages implements PerformanceTestMessages {

    @Override
    public String circularBufferWritePerformance() {
        return "Circular Buffer Write Performance Test";
    }

    @Override
    public String circularBufferReadPerformance() {
        return "Circular Buffer Read Performance Test";
    }

    @Override
    public String circularBufferConcurrentPerformance() {
        return "Circular Buffer Concurrent Read/Write Performance Test";
    }

    @Override
    public String audioFormatConversionPerformance() {
        return "Audio Format Conversion Performance Test";
    }

    @Override
    public String promptTemplateBuildPerformance() {
        return "Prompt Template Build Performance Test";
    }

    @Override
    public String circuitBreakerStateTransitionPerformance() {
        return "Circuit Breaker State Transition Performance Test";
    }

    @Override
    public String llmClientMockPerformance() {
        return "LLM Client Mock Call Performance Test";
    }

    @Override
    public String speechRecognitionMockPerformance() {
        return "Speech Recognition Mock Call Performance Test";
    }

    @Override
    public String testStarted(String testName) {
        return "Test started: " + testName;
    }

    @Override
    public String testCompleted(String testName, long durationMs) {
        return "Test completed: " + testName + ", duration: " + durationMs + " ms";
    }

    @Override
    public String iterationsCompleted(int count) {
        return "Iterations completed: " + count;
    }

    @Override
    public String averageTime(double avgMs) {
        return String.format("Average time: %.3f ms", avgMs);
    }

    @Override
    public String throughput(double opsPerSecond) {
        return String.format("Throughput: %.2f ops/sec", opsPerSecond);
    }

    @Override
    public String memoryUsed(long bytes) {
        return String.format("Memory used: %.2f MB", bytes / 1024.0 / 1024.0);
    }

    @Override
    public String shouldCompleteWithinMs(long expectedMs) {
        return "Should complete within " + expectedMs + " ms";
    }

    @Override
    public String shouldHandleOperationsPerSecond(long minOps) {
        return "Should handle at least " + minOps + " ops/sec";
    }

    @Override
    public String shouldNotExceedMemory(long maxBytes) {
        return String.format("Memory usage should not exceed %.2f MB", maxBytes / 1024.0 / 1024.0);
    }

    @Override
    public String performanceReport() {
        return "Performance Test Report";
    }

    @Override
    public String operation() {
        return "Operation";
    }

    @Override
    public String iterations() {
        return "Iterations";
    }

    @Override
    public String totalTime() {
        return "Total Time";
    }

    @Override
    public String averageTimeTitle() {
        return "Average Time";
    }

    @Override
    public String throughputTitle() {
        return "Throughput";
    }
}
