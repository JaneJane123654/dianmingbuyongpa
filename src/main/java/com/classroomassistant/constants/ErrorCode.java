package com.classroomassistant.constants;

/**
 * 系统统一错误码枚举 (Global Error Code)
 *
 * <p>用于标识系统中发生的各类异常情况。每个错误码对应一类特定的业务或技术故障，
 * 便于前端界面展示、日志检索以及监控报警。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public enum ErrorCode {
    /** 模型加载失败：找不到文件、版本不匹配或底层库初始化异常 */
    MODEL_LOAD_FAILED,

    /** 音频采集失败：麦克风访问受限、设备被占用或硬件故障 */
    AUDIO_CAPTURE_FAILED,

    /** AI 服务调用失败：网络超时、API Key 无效、配额耗尽或模型返回异常 */
    AI_SERVICE_FAILED,

    /** 配置无效：配置文件格式错误、关键参数缺失或参数范围越界 */
    CONFIG_INVALID,

    /** 未知错误：未捕获的运行时异常或系统底层崩溃 */
    UNKNOWN
}
