package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * 模型加载异常 (Model Load Exception)
 *
 * <p>当 AI 模型文件（如 ASR、KWS 或 VAD 的 ONNX 模型）不存在、版本不匹配、
 * 路径非法或 JNI 调用底层推理引擎初始化失败时抛出。
 * 默认关联错误码：{@link ErrorCode#MODEL_LOAD_FAILED}。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ModelLoadException extends AppException {

    /**
     * 构造模型加载异常
     *
     * @param message 错误描述信息
     */
    public ModelLoadException(String message) {
        super(ErrorCode.MODEL_LOAD_FAILED, message);
    }

    /**
     * 构造带原因的模型加载异常
     *
     * @param message 错误描述信息
     * @param cause   导致错误的原始异常
     */
    public ModelLoadException(String message, Throwable cause) {
        super(ErrorCode.MODEL_LOAD_FAILED, message, cause);
    }
}
