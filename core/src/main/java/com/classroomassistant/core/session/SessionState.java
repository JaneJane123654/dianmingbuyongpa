package com.classroomassistant.core.session;

/**
 * 会话状态
 */
public enum SessionState {
    /** 空闲状态：系统未启动录音或监测任务 */
    IDLE,

    /** 监测中：正在实时采集音频并检测唤醒词 */
    MONITORING,

    /** 处理中：已检测到唤醒词或静音超时，正在进行语音识别或 AI 回答生成 */
    TRIGGER_HANDLING,

    /** 错误状态：系统发生不可恢复的故障，需重置或人工干预 */
    ERROR
}
