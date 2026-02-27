package com.classroomassistant.speech;

import java.util.Objects;

/**
 * 语音服务容器 (Speech Services Container)
 *
 * <p>该类作为一个数据对象，统一持有并提供对以下三个核心语音组件的访问：
 * <ul>
 *   <li>{@link WakeWordDetector}: 唤醒词检测引擎</li>
 *   <li>{@link SilenceDetector}: 静音检测（VAD）引擎</li>
 *   <li>{@link SpeechRecognizer}: 语音识别（ASR）引擎</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SpeechServices {

    private final WakeWordDetector wakeWordDetector;
    private final SilenceDetector silenceDetector;
    private final SpeechRecognizer speechRecognizer;

    /**
     * 构造语音服务容器
     *
     * @param wakeWordDetector 唤醒词检测器实例
     * @param silenceDetector  静音检测器实例
     * @param speechRecognizer 语音识别器实例
     * @throws NullPointerException 如果任一参数为 null
     */
    public SpeechServices(
        WakeWordDetector wakeWordDetector,
        SilenceDetector silenceDetector,
        SpeechRecognizer speechRecognizer
    ) {
        this.wakeWordDetector = Objects.requireNonNull(wakeWordDetector, "唤醒词检测器不能为空");
        this.silenceDetector = Objects.requireNonNull(silenceDetector, "安静检测器不能为空");
        this.speechRecognizer = Objects.requireNonNull(speechRecognizer, "语音识别器不能为空");
    }

    /**
     * 获取唤醒词检测器
     *
     * @return {@link WakeWordDetector} 实例
     */
    public WakeWordDetector getWakeWordDetector() {
        return wakeWordDetector;
    }

    /**
     * 获取静音检测器
     *
     * @return {@link SilenceDetector} 实例
     */
    public SilenceDetector getSilenceDetector() {
        return silenceDetector;
    }

    /**
     * 获取语音识别器
     *
     * @return {@link SpeechRecognizer} 实例
     */
    public SpeechRecognizer getSpeechRecognizer() {
        return speechRecognizer;
    }
}
