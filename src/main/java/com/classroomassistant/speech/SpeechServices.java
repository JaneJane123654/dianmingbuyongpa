package com.classroomassistant.speech;

import java.util.Objects;

/**
 * 语音服务集合
 *
 * <p>统一持有唤醒词检测、安静检测与语音识别器实例。
 */
public class SpeechServices {

    private final WakeWordDetector wakeWordDetector;
    private final SilenceDetector silenceDetector;
    private final SpeechRecognizer speechRecognizer;

    public SpeechServices(
        WakeWordDetector wakeWordDetector,
        SilenceDetector silenceDetector,
        SpeechRecognizer speechRecognizer
    ) {
        this.wakeWordDetector = Objects.requireNonNull(wakeWordDetector, "唤醒词检测器不能为空");
        this.silenceDetector = Objects.requireNonNull(silenceDetector, "安静检测器不能为空");
        this.speechRecognizer = Objects.requireNonNull(speechRecognizer, "语音识别器不能为空");
    }

    public WakeWordDetector getWakeWordDetector() {
        return wakeWordDetector;
    }

    public SilenceDetector getSilenceDetector() {
        return silenceDetector;
    }

    public SpeechRecognizer getSpeechRecognizer() {
        return speechRecognizer;
    }
}
