package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * 模型加载异常
 */
public class ModelLoadException extends AppException {

    public ModelLoadException(String message) {
        super(ErrorCode.MODEL_LOAD_FAILED, message);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(ErrorCode.MODEL_LOAD_FAILED, message, cause);
    }
}
