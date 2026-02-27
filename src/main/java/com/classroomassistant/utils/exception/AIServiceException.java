package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * AI 服务相关异常 (AI Service Exception)
 *
 * <p>当调用大语言模型 (LLM) 接口、提示词生成、流式响应处理或熔断器触发时，
 * 如果发生无法恢复的错误，将抛出此异常。
 * 默认关联错误码：{@link ErrorCode#AI_SERVICE_FAILED}。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class AIServiceException extends AppException {

    /**
     * 构造 AI 服务异常
     *
     * @param message 错误描述信息
     */
    public AIServiceException(String message) {
        super(ErrorCode.AI_SERVICE_FAILED, message);
    }

    /**
     * 构造带原因的 AI 服务异常
     *
     * @param message 错误描述信息
     * @param cause   导致错误的原始异常
     */
    public AIServiceException(String message, Throwable cause) {
        super(ErrorCode.AI_SERVICE_FAILED, message, cause);
    }
}
