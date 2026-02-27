package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * 音频采集异常 (Audio Capture Exception)
 *
 * <p>当系统无法访问麦克风、音频格式不受支持、目标数据线 (TargetDataLine) 无法开启，
 * 或者在音频数据循环读取过程中发生 IO 错误时抛出。
 * 默认关联错误码：{@link ErrorCode#AUDIO_CAPTURE_FAILED}。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class AudioCaptureException extends AppException {

    /**
     * 构造音频采集异常
     *
     * @param message 错误描述信息
     */
    public AudioCaptureException(String message) {
        super(ErrorCode.AUDIO_CAPTURE_FAILED, message);
    }

    /**
     * 构造带原因的音频采集异常
     *
     * @param message 错误描述信息
     * @param cause   导致错误的原始异常
     */
    public AudioCaptureException(String message, Throwable cause) {
        super(ErrorCode.AUDIO_CAPTURE_FAILED, message, cause);
    }
}
