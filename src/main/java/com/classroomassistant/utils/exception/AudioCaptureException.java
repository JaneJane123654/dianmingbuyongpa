package com.classroomassistant.utils.exception;

import com.classroomassistant.constants.ErrorCode;

/**
 * 音频采集异常
 */
public class AudioCaptureException extends AppException {

    public AudioCaptureException(String message) {
        super(ErrorCode.AUDIO_CAPTURE_FAILED, message);
    }

    public AudioCaptureException(String message, Throwable cause) {
        super(ErrorCode.AUDIO_CAPTURE_FAILED, message, cause);
    }
}
