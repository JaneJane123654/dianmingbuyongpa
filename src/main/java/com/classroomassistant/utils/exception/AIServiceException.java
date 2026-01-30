package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * AI 服务异常
 */
public class AIServiceException extends AppException {

    public AIServiceException(String message) {
        super(ErrorCode.AI_SERVICE_FAILED, message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(ErrorCode.AI_SERVICE_FAILED, message, cause);
    }
}
