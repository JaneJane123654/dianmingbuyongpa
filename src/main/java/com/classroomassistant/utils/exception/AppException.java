package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;
import java.util.Objects;

/**
 * 应用异常基类
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "错误码不能为空");
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "错误码不能为空");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
