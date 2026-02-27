package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;
import java.util.Objects;

/**
 * 应用异常基类 (Application Base Exception)
 *
 * <p>该类是应用中所有自定义业务异常的顶级父类，继承自 {@link RuntimeException}。
 * 它强制要求关联一个 {@link ErrorCode}，以便于在全局异常处理逻辑中能够根据错误码进行分类处理、
 * 国际化提示或日志归档。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 构造一个新的应用异常
     *
     * @param errorCode 错误码，用于标识异常类别，不能为 null
     * @param message   详细的错误描述信息
     */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "错误码不能为空");
    }

    /**
     * 构造一个包含原因的应用异常
     *
     * @param errorCode 错误码，不能为 null
     * @param message   详细的错误描述信息
     * @param cause     原始异常原因
     */
    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "错误码不能为空");
    }

    /**
     * 获取关联的错误码
     *
     * @return {@link ErrorCode}
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
